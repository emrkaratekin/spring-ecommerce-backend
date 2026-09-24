package com.ecommerce.integration;

import com.ecommerce.TestcontainersConfiguration;
import com.ecommerce.payment.PaymentEvent;
import com.ecommerce.payment.PaymentGateway;
import com.ecommerce.payment.PaymentIntentResult;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests through the real HTTP layer, security filters, services, Flyway schema and PostgreSQL.
 * Only the external payment provider is replaced by a mock, so the tests never call Stripe.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class CheckoutFlowIntegrationTest {

    private static final String ADDRESS_JSON = """
            {"shippingAddress": {
                "recipientName": "Emir Karatekin", "phoneNumber": "+48 600 123 456",
                "street": "ul. Marszalkowska 1", "city": "Warszawa",
                "postalCode": "00-001", "country": "Poland"}}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentGateway paymentGateway;

    private String adminToken;
    private String userToken;
    private long productId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = login("admin@ecommerce.local", "Admin12345");
        userToken = registerNewUser();

        String unique = UUID.randomUUID().toString().substring(0, 8);
        long categoryId = idOf(perform(post("/api/v1/categories"), adminToken,
                "{\"name\":\"Books " + unique + "\"}").andExpect(status().isCreated()), "$.id");
        productId = idOf(perform(post("/api/v1/products"), adminToken, """
                {"name":"Clean Code","sku":"CC-%s","price":39.90,"stockQuantity":5,"categoryId":%d}
                """.formatted(unique, categoryId)).andExpect(status().isCreated()), "$.id");
    }

    @Test
    @DisplayName("Cart -> order -> cancellation keeps stock consistent")
    void checkoutAndCancellation() throws Exception {
        addToCart(2);

        String orderNumber = JsonPath.read(perform(post("/api/v1/orders"), userToken, ADDRESS_JSON)
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(79.80))
                .andReturn().getResponse().getContentAsString(), "$.orderNumber");

        expectStock(3);
        perform(get("/api/v1/cart"), userToken, null).andExpect(jsonPath("$.items").isEmpty());

        perform(post("/api/v1/orders/" + orderNumber + "/cancellation"), userToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        expectStock(5);
    }

    @Test
    @DisplayName("A signed payment webhook marks the order as PAID")
    void paymentWebhookMarksOrderPaid() throws Exception {
        addToCart(1);
        String orderNumber = JsonPath.read(perform(post("/api/v1/orders"), userToken, ADDRESS_JSON)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.orderNumber");

        String intentId = "pi_test_" + UUID.randomUUID().toString().replace("-", "");
        when(paymentGateway.createPaymentIntent(eq(orderNumber), anyLong(), eq("pln"), anyString()))
                .thenReturn(new PaymentIntentResult(intentId, intentId + "_secret", "requires_payment_method"));

        perform(post("/api/v1/orders/" + orderNumber + "/payment"), userToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientSecret").value(intentId + "_secret"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        when(paymentGateway.parseWebhookEvent(anyString(), eq("valid-signature")))
                .thenReturn(new PaymentEvent(PaymentEvent.Type.PAYMENT_SUCCEEDED,
                        "payment_intent.succeeded", intentId, null));

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "valid-signature")
                        .content("{}"))
                .andExpect(status().isOk());

        perform(get("/api/v1/orders/" + orderNumber), userToken, null)
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    @DisplayName("Security rules: 401 without token, 403 for customers on admin operations")
    void securityRules() throws Exception {
        mockMvc.perform(get("/api/v1/cart")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/products")).andExpect(status().isOk());

        perform(post("/api/v1/products"), userToken, "{}").andExpect(status().isForbidden());
        perform(delete("/api/v1/products/" + productId), userToken, null).andExpect(status().isForbidden());
        perform(get("/api/v1/admin/orders"), userToken, null).andExpect(status().isForbidden());
        perform(get("/api/v1/admin/orders"), adminToken, null).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Ordering more than the available stock is rejected")
    void insufficientStockIsRejected() throws Exception {
        perform(post("/api/v1/cart/items"), userToken,
                "{\"productId\":" + productId + ",\"quantity\":6}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only 5 units of 'Clean Code' are available in stock"));
    }

    // ---------------------------------------------------------------- helpers

    private void addToCart(int quantity) throws Exception {
        perform(post("/api/v1/cart/items"), userToken,
                "{\"productId\":" + productId + ",\"quantity\":" + quantity + "}")
                .andExpect(status().isOk());
    }

    private void expectStock(int expected) throws Exception {
        mockMvc.perform(get("/api/v1/products/" + productId))
                .andExpect(jsonPath("$.stockQuantity").value(expected));
    }

    private String login(String email, String password) throws Exception {
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        return JsonPath.read(perform(post("/api/v1/auth/login"), null, body)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.accessToken");
    }

    private String registerNewUser() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        String body = """
                {"firstName":"Test","lastName":"User","email":"%s","password":"Secret123"}
                """.formatted(email);
        return JsonPath.read(perform(post("/api/v1/auth/register"), null, body)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.accessToken");
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, String token, String json) throws Exception {
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (json != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mockMvc.perform(request);
    }

    private long idOf(ResultActions result, String path) throws Exception {
        Number id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
        return id.longValue();
    }
}
