package com.nilambar.erp;

import static org.assertj.core.api.Assertions.assertThat;

import com.nilambar.erp.dto.dashboard.DashboardSnapshot;
import com.nilambar.erp.repository.EmployeeRepository;
import com.nilambar.erp.repository.OrderRepository;
import com.nilambar.erp.repository.StockMovementRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Renders the dashboard over real HTTP against real MySQL and Kafka so the seeded demo data, the
 * KPI services and the JSP (including its EL) are all exercised together.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class DashboardIT {

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

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Test
    void demoDataIsSeededSoTheKpiServicesHaveHistory() {
        assertThat(employeeRepository.count()).isPositive();
        assertThat(orderRepository.count()).isPositive();
        assertThat(stockMovementRepository.count()).isPositive();
    }

    @Test
    void dashboardPageRendersEveryKpiGroup() throws Exception {
        String html = get("/dashboard?days=30");

        assertThat(html).contains("AI-powered business dashboard");
        assertThat(html).contains("Inventory", "Employees &amp; Resources", "Sales &amp; Business",
                "Customers &amp; Vendors", "Risk &amp; Operations", "AI-Driven Insights");
        assertThat(html).contains("Total Stock Available", "Inventory Turnover Rate", "Attendance Rate",
                "Total Revenue", "Business Risk Index", "Revenue Prediction");
        // A rendering failure surfaces as the Tomcat/JSP error page rather than a 500 alone.
        assertThat(html).doesNotContain("Exception", "jakarta.el");
    }

    @Test
    void jsonApiExposesTheSameSnapshot() {
        DashboardSnapshot snapshot = restTemplate.getForObject(
                "http://localhost:" + port + "/dashboard/api?days=90", DashboardSnapshot.class);

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.windowDays()).isEqualTo(90);
        assertThat(snapshot.groups()).hasSize(6);
        assertThat(snapshot.groups()).allSatisfy(group -> assertThat(group.cards()).isNotEmpty());
        assertThat(snapshot.revenueTrend()).isNotEmpty();
        assertThat(snapshot.revenueForecast()).isNotEmpty();
        assertThat(snapshot.aiNarrative()).isNotBlank();
    }

    @Test
    void windowSizeIsClampedToTheSupportedRange() {
        DashboardSnapshot narrow = restTemplate.getForObject(
                "http://localhost:" + port + "/dashboard/api?days=1", DashboardSnapshot.class);
        DashboardSnapshot wide = restTemplate.getForObject(
                "http://localhost:" + port + "/dashboard/api?days=9999", DashboardSnapshot.class);

        assertThat(narrow).isNotNull();
        assertThat(wide).isNotNull();
        assertThat(narrow.windowDays()).isEqualTo(7);
        assertThat(wide.windowDays()).isEqualTo(180);
    }

    private String get(String path) throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return response.body();
    }
}
