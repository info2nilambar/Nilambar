package com.nilambar.erp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nilambar.erp.domain.CustomerOrder;
import com.nilambar.erp.domain.FulfilmentType;
import com.nilambar.erp.domain.OrderStatus;
import com.nilambar.erp.domain.Product;
import com.nilambar.erp.repository.CartRepository;
import com.nilambar.erp.repository.OrderRepository;
import com.nilambar.erp.repository.ProductRepository;
import com.nilambar.erp.repository.UserRepository;
import com.nilambar.erp.service.delivery.DeliveryQuote;
import com.nilambar.erp.service.otp.OtpSender;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Exercises the full registration -> address -> cart -> checkout -> order path against real MySQL
 * and Kafka containers.
 */
@SpringBootTest
@Testcontainers
@Import(RegistrationToOrderIT.RecordingSenderConfig.class)
class RegistrationToOrderIT {

    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("erpdb");

    private static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.4.4"));

    static {
        MYSQL.start();
        KAFKA.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("erp.fulfilment.simulate", () -> "false");
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RecordingOtpSender otpSender;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Test
    void registeredUserInsideRadiusCanPlaceAHomeDeliveryOrder() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        MockHttpSession session = new MockHttpSession();
        String mobile = "9800000001";

        mockMvc.perform(post("/auth/otp/request").with(csrf()).session(session).param("mobile", mobile))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/auth/otp/verify").with(csrf()).session(session)
                        .param("mobile", mobile)
                        .param("code", otpSender.lastCode()))
                .andExpect(redirectedUrl("/profile/complete"));

        mockMvc.perform(post("/profile/complete").with(csrf()).session(session)
                        .param("name", "Nilambar")
                        .param("email", "nilambar@example.com")
                        .param("address.label", "Home")
                        .param("address.line1", "24 Church Street")
                        .param("address.city", "Bengaluru")
                        .param("address.state", "Karnataka")
                        .param("address.pincode", "560001")
                        .param("address.latitude", "12.9750")
                        .param("address.longitude", "77.6070"))
                .andExpect(redirectedUrl("/products"));

        assertThat(userRepository.findByMobile(mobile)).get()
                .extracting("profileCompleted").isEqualTo(true);

        Product product = productRepository.findAll().get(0);
        int stockBefore = product.getStockQuantity();

        mockMvc.perform(post("/cart/add").with(csrf()).session(session)
                        .param("productId", product.getId().toString())
                        .param("quantity", "2"))
                .andExpect(redirectedUrl("/cart"));

        DeliveryQuote quote = (DeliveryQuote) mockMvc.perform(get("/checkout").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("quote"))
                .andReturn().getModelAndView().getModel().get("quote");
        assertThat(quote.homeDeliveryAvailable()).isTrue();

        Long addressId = orderAddressId(session, mockMvc);

        mockMvc.perform(post("/checkout/place").with(csrf()).session(session)
                        .param("addressId", addressId.toString())
                        .param("fulfilmentType", "HOME_DELIVERY"))
                .andExpect(redirectedUrl("/orders"));

        List<CustomerOrder> orders = orderRepository.findAll();
        assertThat(orders).hasSize(1);
        CustomerOrder order = orders.get(0);
        assertThat(order.getFulfilmentType()).isEqualTo(FulfilmentType.HOME_DELIVERY);
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getTotal()).isEqualByComparingTo(product.getPrice().multiply(java.math.BigDecimal.valueOf(2)));
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity())
                .isEqualTo(stockBefore - 2);
        assertThat(cartRepository.findByUserId(order.getUser().getId()).orElseThrow().getItems()).isEmpty();

        // The Kafka consumer advances the order out of PLACED once it receives order.placed.
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                        .isEqualTo(OrderStatus.CONFIRMED));
    }

    @Test
    void addressBeyondTheRadiusCannotUseHomeDelivery() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        MockHttpSession session = new MockHttpSession();
        String mobile = "9800000002";

        mockMvc.perform(post("/auth/otp/request").with(csrf()).session(session).param("mobile", mobile));
        mockMvc.perform(post("/auth/otp/verify").with(csrf()).session(session)
                .param("mobile", mobile).param("code", otpSender.lastCode()));

        mockMvc.perform(post("/profile/complete").with(csrf()).session(session)
                        .param("name", "Far Away")
                        .param("address.label", "Farm")
                        .param("address.line1", "Outskirts")
                        .param("address.city", "Bengaluru Rural")
                        .param("address.state", "Karnataka")
                        .param("address.pincode", "562123")
                        .param("address.latitude", "13.2000")
                        .param("address.longitude", "77.8000"))
                .andExpect(redirectedUrl("/products"));

        Product product = productRepository.findAll().get(1);
        mockMvc.perform(post("/cart/add").with(csrf()).session(session)
                .param("productId", product.getId().toString())
                .param("quantity", "1"));

        DeliveryQuote quote = (DeliveryQuote) mockMvc.perform(get("/checkout").session(session))
                .andReturn().getModelAndView().getModel().get("quote");
        assertThat(quote.homeDeliveryAvailable()).isFalse();
        assertThat(quote.fulfilmentType()).isEqualTo(FulfilmentType.STORE_PICKUP);

        Long addressId = orderAddressId(session, mockMvc);

        // The browser is not trusted: posting HOME_DELIVERY anyway must be rejected.
        mockMvc.perform(post("/checkout/place").with(csrf()).session(session)
                        .param("addressId", addressId.toString())
                        .param("fulfilmentType", "HOME_DELIVERY"))
                .andExpect(status().is3xxRedirection());

        Long userId = userRepository.findByMobile(mobile).orElseThrow().getId();
        assertThat(orderRepository.findAll().stream()
                .filter(order -> order.getUser().getId().equals(userId)))
                .isEmpty();
    }

    private Long orderAddressId(MockHttpSession session, MockMvc mockMvc) throws Exception {
        Object addresses = mockMvc.perform(get("/checkout").session(session))
                .andReturn().getModelAndView().getModel().get("selectedAddress");
        return ((com.nilambar.erp.domain.Address) addresses).getId();
    }

    @TestConfiguration
    static class RecordingSenderConfig {

        @Bean
        @Primary
        RecordingOtpSender recordingOtpSender() {
            return new RecordingOtpSender();
        }
    }

    static class RecordingOtpSender implements OtpSender {

        private volatile String lastCode;

        @Override
        public void send(String mobile, String code) {
            this.lastCode = code;
        }

        @Override
        public String channel() {
            return "TEST";
        }

        String lastCode() {
            return lastCode;
        }
    }
}
