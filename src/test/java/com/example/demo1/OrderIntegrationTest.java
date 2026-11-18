package com.example.demo1;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// Dùng H2 riêng trong bộ nhớ để chạy HTTP, Flyway và JPA mà không đụng database thật.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:orders_integration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.baseline-on-migrate=false"

})
class OrderIntegrationTest {
    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper json = new JsonMapper();

    @Test
    void createsReadsPaysAndCancelsOrderThroughHttpAndDatabase() throws Exception {
        String customerId = UUID.randomUUID().toString();
        String productId = UUID.randomUUID().toString();
        String body = """
                {"customerId":"%s","items":[{"productId":"%s","quantity":2,"priceVnd":15000}]}
                """.formatted(customerId, productId);
        HttpResponse<String> created = send("POST", "/api/v1/orders", body);
        assertEquals(201, created.statusCode());
        String id = json.readTree(created.body()).get("orderId").asString();
        assertNotNull(UUID.fromString(id));
        assertEquals("/api/v1/orders/" + id, URI.create(created.headers().firstValue("Location").orElseThrow()).getPath());

        HttpResponse<String> retrieved = send("GET", "/api/v1/orders/" + id, null);
        assertEquals(200, retrieved.statusCode());
        var initial = json.readTree(retrieved.body());
        assertEquals(customerId, initial.get("customerId").asString());
        assertEquals(productId, initial.get("items").get(0).get("productId").asString());
        assertEquals(2, initial.get("items").get(0).get("quantity").asInt());
        assertEquals(0, initial.get("totalPriceVnd").decimalValue().compareTo(new java.math.BigDecimal("30000")));
        assertEquals("PENDING", initial.get("status").asString());
        // Thời gian tạo phải giữ nguyên khi đơn được đọc và lưu lại qua các request.
        String createdAt = initial.get("createdAt").asString();

        assertEquals(204, send("POST", "/api/v1/orders/" + id + "/pay", null).statusCode());
        var paid = json.readTree(send("GET", "/api/v1/orders/" + id, null).body());
        assertEquals("PAID", paid.get("status").asString());
        assertEquals(createdAt, paid.get("createdAt").asString());

        assertEquals(204, send("POST", "/api/v1/orders/" + id + "/cancel", null).statusCode());
        var cancelled = json.readTree(send("GET", "/api/v1/orders/" + id, null).body());
        assertEquals("CANCELLED", cancelled.get("status").asString());
        assertEquals(createdAt, cancelled.get("createdAt").asString());
    }

    @Test
    void rejectsInvalidRequest() throws Exception {
        String body = """
                {"customerId":"%s","items":[{"productId":"%s","quantity":0,"priceVnd":15000}]}
                """.formatted(UUID.randomUUID(), UUID.randomUUID());
        assertEquals(400, send("POST", "/api/v1/orders", body).statusCode());
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void rejectsInvalidBodies(String body) throws Exception {
        assertEquals(400, send("POST", "/api/v1/orders", body).statusCode());
    }

    private static Stream<String> invalidRequests() {
        String customer = UUID.randomUUID().toString();
        String product = UUID.randomUUID().toString();
        return Stream.of(
                "{\"items\":[{\"productId\":\"" + product + "\",\"quantity\":1,\"priceVnd\":1}]}",
                "{\"customerId\":\"" + customer + "\",\"items\":[]}",
                "{\"customerId\":\"" + customer + "\",\"items\":[null]}",
                "{\"customerId\":\"" + customer + "\",\"items\":[{\"productId\":\"" + product + "\",\"quantity\":1,\"priceVnd\":-1}]}",
                "{\"customerId\":\"" + customer + "\",\"items\":[{\"productId\":\"" + product + "\",\"quantity\":1,\"priceVnd\":1.234}]}",
                "{\"customerId\":\"not-a-uuid\",\"items\":[]}",
                "{invalid json"
        );
    }

    @Test
    void returnsNotFoundForMissingOrderAndBadRequestForInvalidUuid() throws Exception {
        String missing = UUID.randomUUID().toString();
        assertEquals(404, send("GET", "/api/v1/orders/" + missing, null).statusCode());
        assertEquals(404, send("POST", "/api/v1/orders/" + missing + "/pay", null).statusCode());
        assertEquals(404, send("POST", "/api/v1/orders/" + missing + "/cancel", null).statusCode());
        assertEquals(400, send("GET", "/api/v1/orders/not-a-uuid", null).statusCode());
    }

    @Test
    void rejectsRepeatedPaymentAndPaymentAfterCancellation() throws Exception {
        String first = createOrderId();
        assertEquals(204, send("POST", "/api/v1/orders/" + first + "/pay", null).statusCode());
        assertEquals(409, send("POST", "/api/v1/orders/" + first + "/pay", null).statusCode());
        String second = createOrderId();
        assertEquals(204, send("POST", "/api/v1/orders/" + second + "/cancel", null).statusCode());
        assertEquals(409, send("POST", "/api/v1/orders/" + second + "/pay", null).statusCode());
        assertEquals("CANCELLED", json.readTree(send("GET", "/api/v1/orders/" + second, null).body()).get("status").asString());
    }

    @Test
    void persistsMultipleItemsWithDecimalPrices() throws Exception {
        String first = UUID.randomUUID().toString();
        String second = UUID.randomUUID().toString();
        String body = """
                {"customerId":"%s","items":[{"productId":"%s","quantity":2,"priceVnd":10.25},{"productId":"%s","quantity":3,"priceVnd":2.15}]}
                """.formatted(UUID.randomUUID(), first, second);
        var created = send("POST", "/api/v1/orders", body);
        assertEquals(201, created.statusCode());
        String id = json.readTree(created.body()).get("orderId").asString();
        var response = send("GET", "/api/v1/orders/" + id, null);
        assertEquals(200, response.statusCode());
        var result = json.readTree(response.body());
        assertEquals(0, result.get("totalPriceVnd").decimalValue().compareTo(new BigDecimal("26.95")));
        var items = result.get("items");
        assertEquals(2, items.size());
        // Đối chiếu theo productId vì thứ tự item từ database không được đảm bảo.
        java.util.Map<String, String> expected = java.util.Map.of(first, "2:10.25", second, "3:2.15");
        for (var item : items) {
            String actual = item.get("quantity").asInt() + ":" + item.get("unitPriceVnd").decimalValue().toPlainString();
            assertEquals(expected.get(item.get("productId").asString()), actual);
        }
    }

    private String createOrderId() throws Exception {
        String body = """
                {"customerId":"%s","items":[{"productId":"%s","quantity":1,"priceVnd":10}]}
                """.formatted(UUID.randomUUID(), UUID.randomUUID());
        var response = send("POST", "/api/v1/orders", body);
        assertEquals(201, response.statusCode());
        return json.readTree(response.body()).get("orderId").asString();
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if ("GET".equals(method)) {
            builder.GET();
        } else {
            builder.header("Content-Type", "application/json")
                    .POST(body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
