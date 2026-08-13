package com.nilambar.erp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.nilambar.erp.domain.CustomerOrder;
import com.nilambar.erp.domain.FulfilmentType;
import com.nilambar.erp.domain.OrderStatus;
import com.nilambar.erp.domain.Product;
import com.nilambar.erp.repository.AddressRepository;
import com.nilambar.erp.repository.CartRepository;
import com.nilambar.erp.repository.OrderFeedbackRepository;
import com.nilambar.erp.repository.OrderRepository;
import com.nilambar.erp.repository.ProductRepository;
import com.nilambar.erp.repository.UserRepository;
import com.nilambar.erp.service.otp.OtpSender;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Drives the full registration -> address -> cart -> checkout -> order path over real HTTP against
 * real MySQL and Kafka containers. Real requests are used rather than MockMvc so that every JSP is
 * actually compiled and rendered; EL mistakes only surface at request time.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(RegistrationToOrderIT.RecordingSenderConfig.class)
class RegistrationToOrderIT {

    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");

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
        registry.add("erp.dashboard.seed-demo-data", () -> "false");
    }

    @LocalServerPort
    private int port;

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

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private OrderFeedbackRepository feedbackRepository;

    @Test
    void addressInsideRadiusCanCheckOutWithHomeDelivery() throws Exception {
        Browser browser = new Browser();
        String mobile = "9800000001";
        signIn(browser, mobile);
        completeProfile(browser, "Nilambar", "Home", "24 Church Street", "Bengaluru", "560001",
                "12.9750", "77.6070");

        Product product = productRepository.findAll().get(0);
        int stockBefore = product.getStockQuantity();

        assertThat(browser.get("/products")).contains("Page 1 of");
        assertThat(browser.get("/products?q=" + URLEncoder.encode(product.getName(), StandardCharsets.UTF_8)))
                .contains(product.getName());
        assertThat(browser.get("/products/" + product.getId())).contains("Add to cart");

        browser.post("/cart/add", Map.of("productId", product.getId().toString(), "quantity", "2"));
        assertThat(browser.get("/cart")).contains(product.getName());

        String checkout = browser.get("/checkout");
        assertThat(checkout).contains("Home delivery available");
        assertThat(checkout).contains("11540334561").contains("SBIN0007021");
        assertThat(browser.getBytes("/checkout/payment-qr.png?addressId=" + addressId(mobile)))
                .hasSizeGreaterThan(100);

        browser.post("/checkout/place", Map.of("addressId", addressId(mobile).toString(),
                "fulfilmentType", "HOME_DELIVERY"));

        CustomerOrder order = onlyOrderOf(mobile);
        assertThat(order.getFulfilmentType()).isEqualTo(FulfilmentType.HOME_DELIVERY);
        assertThat(order.getItems()).hasSize(1);
        BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(2));
        assertThat(order.getDeliveryFee()).isEqualByComparingTo("30.00");
        assertThat(order.getTotal()).isEqualByComparingTo(subtotal.add(order.getDeliveryFee()));
        assertThat(order.getDistanceKm()).isLessThanOrEqualTo(1.5);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity())
                .isEqualTo(stockBefore - 2);
        assertThat(cartRepository.findByUserId(order.getUser().getId()).orElseThrow().getItems()).isEmpty();

        assertThat(browser.get("/orders")).contains(order.getOrderNumber());
        String detail = browser.get("/orders/" + order.getId());
        assertThat(detail).contains(product.getName());
        assertThat(detail).contains("11540334561").contains("once it is delivered");
        assertThat(browser.getBytes("/orders/" + order.getId() + "/payment-qr.png")).hasSizeGreaterThan(100);

        // The Kafka consumer advances the order out of PLACED once it receives order.placed.
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                        .isEqualTo(OrderStatus.CONFIRMED));

        markDelivered(order.getId());
        browser.get("/orders/" + order.getId());
        browser.post("/orders/" + order.getId() + "/feedback",
                Map.of("rating", "5", "comment", "Delivered on time"));
        assertThat(feedbackRepository.findByOrderId(order.getId()).orElseThrow().getRating()).isEqualTo(5);
        assertThat(browser.get("/orders/" + order.getId())).contains("Delivered on time");
    }

    private void markDelivered(Long orderId) {
        CustomerOrder order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);
    }

    @Test
    void addressBeyondRadiusIsRefusedHomeDeliveryEvenIfThePostSaysOtherwise() throws Exception {
        Browser browser = new Browser();
        String mobile = "9800000002";
        signIn(browser, mobile);
        completeProfile(browser, "Far Away", "Farm", "Outskirts", "Bengaluru Rural", "562123",
                "13.2000", "77.8000");

        Product product = productRepository.findAll().get(1);
        browser.post("/cart/add", Map.of("productId", product.getId().toString(), "quantity", "1"));

        String checkout = browser.get("/checkout");
        assertThat(checkout).contains("Outside the home-delivery radius");
        assertThat(checkout).contains("Store pickup");

        Long addressId = addressId(mobile);
        browser.post("/checkout/place", Map.of("addressId", addressId.toString(),
                "fulfilmentType", "HOME_DELIVERY"));
        assertThat(ordersOf(mobile)).isEmpty();

        browser.post("/checkout/place", Map.of("addressId", addressId.toString(),
                "fulfilmentType", "STORE_PICKUP"));
        CustomerOrder order = onlyOrderOf(mobile);
        assertThat(order.getFulfilmentType()).isEqualTo(FulfilmentType.STORE_PICKUP);
        assertThat(order.getDistanceKm()).isGreaterThan(5);
    }

    private void signIn(Browser browser, String mobile) throws Exception {
        assertThat(browser.get("/auth/login")).contains("Mobile number");
        browser.post("/auth/otp/request", Map.of("mobile", mobile));
        assertThat(browser.get("/auth/verify?mobile=" + mobile)).contains("OTP");
        browser.post("/auth/otp/verify", Map.of("mobile", mobile, "code", otpSender.lastCode()));
        assertThat(userRepository.findByMobile(mobile)).isPresent();
    }

    private void completeProfile(Browser browser, String name, String label, String line1, String city,
            String pincode, String latitude, String longitude) throws Exception {
        assertThat(browser.get("/profile/complete")).contains("Complete your profile");
        Map<String, String> form = new LinkedHashMap<>();
        form.put("name", name);
        form.put("address.label", label);
        form.put("address.line1", line1);
        form.put("address.city", city);
        form.put("address.state", "Karnataka");
        form.put("address.pincode", pincode);
        form.put("address.latitude", latitude);
        form.put("address.longitude", longitude);
        browser.post("/profile/complete", form);
        assertThat(browser.get("/profile/addresses")).contains(line1);
    }

    private Long addressId(String mobile) {
        Long userId = userRepository.findByMobile(mobile).orElseThrow().getId();
        return addressRepository.findByUserIdOrderByDefaultAddressDescIdAsc(userId).get(0).getId();
    }

    private List<CustomerOrder> ordersOf(String mobile) {
        Long userId = userRepository.findByMobile(mobile).orElseThrow().getId();
        return orderRepository.findAll().stream()
                .filter(order -> order.getUser().getId().equals(userId))
                .collect(Collectors.toList());
    }

    private CustomerOrder onlyOrderOf(String mobile) {
        List<CustomerOrder> orders = ordersOf(mobile);
        assertThat(orders).hasSize(1);
        return orders.get(0);
    }

    /** Cookie-aware HTTP client that scrapes the CSRF token out of the rendered page. */
    private class Browser {

        private final HttpClient client;
        private String csrfToken;

        Browser() {
            CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
            this.client = HttpClient.newBuilder().cookieHandler(cookies)
                    .followRedirects(HttpClient.Redirect.NORMAL).build();
        }

        byte[] getBytes(String path) throws IOException, InterruptedException {
            HttpResponse<byte[]> response = client.send(
                    HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            assertThat(response.statusCode()).as("GET %s", path).isEqualTo(200);
            return response.body();
        }

        String get(String path) throws IOException, InterruptedException {
            HttpResponse<String> response = client.send(
                    HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).as("GET %s", path).isEqualTo(200);
            Matcher matcher = CSRF.matcher(response.body());
            if (matcher.find()) {
                csrfToken = matcher.group(1);
            }
            return response.body();
        }

        void post(String path, Map<String, String> form) throws IOException, InterruptedException {
            Map<String, String> body = new LinkedHashMap<>(form);
            body.put("_csrf", csrfToken);
            HttpResponse<String> response = client.send(HttpRequest.newBuilder(uri(path))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(encode(body)))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).as("POST %s", path).isEqualTo(200);
            Matcher matcher = CSRF.matcher(response.body());
            if (matcher.find()) {
                csrfToken = matcher.group(1);
            }
        }

        private URI uri(String path) {
            return URI.create("http://localhost:" + port + path);
        }

        private String encode(Map<String, String> form) {
            return form.entrySet().stream()
                    .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "="
                            + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));
        }
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
