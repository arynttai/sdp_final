package com.restaurant.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.restaurant.Main;
import com.restaurant.builder.application.OrderService;
import com.restaurant.builder.application.dto.*;
import com.restaurant.builder.product.Fulfillment;
import com.restaurant.builder.repository.OrderBook;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = Main.class)
@AutoConfigureMockMvc
class RestaurantApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private ObjectNode request() {
        ObjectNode request = json.createObjectNode();
        request.put("customerName", "Mukhammad").put("fulfillment", "TAKEAWAY").put("discountCode", "STUDENT")
            .put("note", "Please slice for sharing").put("requestId", UUID.randomUUID().toString());
        var item = request.putArray("items").addObject();
        item.put("menuItemId", "margherita").put("quantity", 2).putArray("toppings").add("cheese");
        return request;
    }
    private JsonNode place(ObjectNode request, MockHttpSession session) throws Exception {
        return json.readTree(mvc.perform(post("/api/orders").session(session).contentType(MediaType.APPLICATION_JSON)
            .content(request.toString())).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }
    @Test void menuProvidesAllServerControlledOptions() throws Exception {
        mvc.perform(get("/api/menu")).andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(6)).andExpect(jsonPath("$.toppings.length()").value(3))
            .andExpect(jsonPath("$.discounts.length()").value(3)).andExpect(jsonPath("$.currency").value("KZT"))
            .andExpect(jsonPath("$.statuses[0]").value("PLACED")).andExpect(jsonPath("$.statuses[3]").value("COMPLETED"));
    }
    @Test void decoratedQuotePlacementAndBridgeReceiptAgree() throws Exception {
        ObjectNode request = request();
        ObjectNode quoteRequest = request.deepCopy();
        quoteRequest.remove(List.of("customerName", "tableNumber", "note", "requestId"));
        JsonNode quote = json.readTree(mvc.perform(post("/api/orders/quote").contentType(MediaType.APPLICATION_JSON)
            .content(quoteRequest.toString())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        MockHttpSession session = new MockHttpSession();
        JsonNode order = place(request, session);
        for (String field : List.of("subtotal", "discount", "total", "currency")) assertThat(order.get(field)).isEqualTo(quote.get(field));
        assertThat(order.get("subtotal").decimalValue()).isEqualByComparingTo("4700.00");
        assertThat(order.get("discount").decimalValue()).isEqualByComparingTo("470.00");
        assertThat(order.get("total").decimalValue()).isEqualByComparingTo("4230.00");
        String receipt = mvc.perform(get("/api/orders/" + order.get("id").asText() + "/receipt").session(session).param("format", "text"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(receipt).contains("4230.00 KZT", "Extra cheese");
    }
    @Test void retriesAreIdempotentAndChangedPayloadConflicts() throws Exception {
        MockHttpSession session = new MockHttpSession();
        ObjectNode request = request();
        JsonNode order = place(request, session);
        assertThat(place(request, session)).isEqualTo(order);
        mvc.perform(get("/api/orders").session(session)).andExpect(jsonPath("$.length()").value(1));
        assertThat(order.get("events")).hasSize(2);
        request.put("note", "Different payload");
        mvc.perform(post("/api/orders").session(session).contentType(MediaType.APPLICATION_JSON).content(request.toString()))
            .andExpect(status().isConflict());
    }
    @Test void onlySuccessfulTransitionsNotifyAndRepeatedTargetsAreIdempotent() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String id = place(request(), session).get("id").asText();
        String preparing = "{\"status\":\"PREPARING\"}";
        for (int retry = 0; retry < 2; retry++) {
            mvc.perform(post("/api/orders/" + id + "/status").session(session).contentType(MediaType.APPLICATION_JSON).content(preparing))
                .andExpect(status().isOk()).andExpect(jsonPath("$.events.length()").value(4));
        }
        mvc.perform(post("/api/orders/" + id + "/status").session(session).contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"PLACED\"}")).andExpect(status().isConflict());
        mvc.perform(post("/api/orders/" + id + "/advance").session(session).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("READY")).andExpect(jsonPath("$.events.length()").value(6));
        mvc.perform(post("/api/orders/" + id + "/advance").session(session).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED")).andExpect(jsonPath("$.events.length()").value(8));
        mvc.perform(post("/api/orders/" + id + "/advance").session(session).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isConflict());
        mvc.perform(get("/api/orders/" + id).session(session)).andExpect(jsonPath("$.events.length()").value(8));
    }
    @Test void anotherSessionCannotReadModifyOrRenderOrders() throws Exception {
        MockHttpSession owner = new MockHttpSession();
        String id = place(request(), owner).get("id").asText();
        MockHttpSession other = new MockHttpSession();
        mvc.perform(get("/api/orders").session(other)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/orders/" + id).session(other)).andExpect(status().isNotFound());
        mvc.perform(get("/api/orders/" + id + "/receipt").session(other)).andExpect(status().isNotFound());
        mvc.perform(post("/api/orders/" + id + "/advance").session(other).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isNotFound());
    }
    @Test void allReceiptModesWorkAndHtmlEscapesPrivateInput() throws Exception {
        MockHttpSession session = new MockHttpSession();
        ObjectNode request = request().put("customerName", "<script>name</script>").put("note", "<img src=x>");
        String id = place(request, session).get("id").asText();
        for (String format : List.of("html", "text")) for (String type : List.of("customer", "kitchen")) {
            String body = mvc.perform(get("/api/orders/" + id + "/receipt").session(session).param("format", format).param("type", type))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsString();
            assertThat(body).contains("Margherita");
            if (format.equals("html")) assertThat(body).doesNotContain("<script>", "<img ").contains("&lt;img");
            if (type.equals("kitchen")) assertThat(body).doesNotContain("4230.00");
        }
        mvc.perform(get("/api/orders/" + id + "/receipt").session(session).param("format", "pdf")).andExpect(status().isBadRequest());
    }
    @Test void receiptThemesPreserveContentEscapingAndSecurityHeaders() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String id = place(request().put("customerName", "<script>name</script>").put("note", "<img src=x>"), session).get("id").asText();
        String path = "/api/orders/" + id + "/receipt";
        for (String type : List.of("customer", "kitchen")) {
            String defaultBody = mvc.perform(get(path).session(session).param("type", type))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            String expectedContent = defaultBody.substring(defaultBody.indexOf("<body>") + 6, defaultBody.indexOf("</body>"));
            for (String theme : List.of("dark", "light")) {
                String html = mvc.perform(get(path).session(session).param("type", type).param("theme", theme))
                    .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; frame-ancestors 'none'"))
                    .andReturn().getResponse().getContentAsString();
                assertThat(html).contains("<title>ITALIANO — Receipt</title>", "html{color-scheme:" + theme,
                    "@media print{html{color-scheme:light;background:white;color:black}", "&lt;img src=x&gt;")
                    .doesNotContain("<script>", "<img ");
                assertThat(html.substring(html.indexOf("<body>") + 6, html.indexOf("</body>"))).isEqualTo(expectedContent);
                if (theme.equals("dark")) assertThat(html).isEqualTo(defaultBody).contains("background:#171410");
                else assertThat(html).contains("background:#f7f2e9");
            }
            String text = mvc.perform(get(path).session(session).param("type", type).param("format", "text"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            mvc.perform(get(path).session(session).param("type", type).param("format", "text").param("theme", "light"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(text));
        }
    }
    @Test void invalidReceiptThemesAreRejectedWithoutReflectingMarkup() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String id = place(request(), session).get("id").asText();
        for (String theme : List.of("sepia", "DARK", "light;background:url(https://example.com)", "\"><script>alert(1)</script>")) {
            mvc.perform(get("/api/orders/" + id + "/receipt").session(session).param("theme", theme))
                .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Receipt theme must be dark or light"));
        }
    }
    static Stream<String> invalidToppings() {
        return Stream.of("null", "[null]", "[\"\"]", "[\"unknown\"]", "[\"cheese\",\"cheese\"]", "[\"cheese\",\"mushrooms\",\"olives\",\"other\"]");
    }
    @ParameterizedTest @MethodSource("invalidToppings")
    void invalidToppingsAreClientErrors(String toppings) throws Exception {
        ObjectNode request = request();
        ((ObjectNode) request.at("/items/0")).set("toppings", json.readTree(toppings));
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(request.toString())).andExpect(status().isBadRequest());
    }
    @Test void drinksAndIneligibleDiscountsAreRejectedWithoutStoringOrder() throws Exception {
        MockHttpSession session = new MockHttpSession();
        ObjectNode request = request();
        ((ObjectNode) request.at("/items/0")).put("menuItemId", "lemonade");
        mvc.perform(post("/api/orders").session(session).contentType(MediaType.APPLICATION_JSON).content(request.toString())).andExpect(status().isBadRequest());
        request = request().put("fulfillment", "DINE_IN").put("tableNumber", 1).put("discountCode", "PICKUP");
        mvc.perform(post("/api/orders").session(session).contentType(MediaType.APPLICATION_JSON).content(request.toString())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/orders").session(session)).andExpect(jsonPath("$.length()").value(0));
    }
    @Test void sessionsHaveBoundedOrderStorage() throws Exception {
        MockHttpSession session = new MockHttpSession();
        for (int i = 0; i < 20; i++) place(request(), session);
        mvc.perform(post("/api/orders").session(session).contentType(MediaType.APPLICATION_JSON).content(request().toString()))
            .andExpect(status().isConflict());
    }
    @Test void concurrentRetriesPublishOnePlacement() throws Exception {
        OrderService service = new OrderService();
        OrderBook book = new OrderBook();
        OrderRequest request = new OrderRequest(List.of(new ItemRequest("margherita", 2, List.of("cheese"))),
            Fulfillment.TAKEAWAY, "STUDENT", "Guest", null, "", UUID.randomUUID());
        try (var executor = Executors.newFixedThreadPool(8)) {
            List<Callable<UUID>> calls = IntStream.range(0, 24).mapToObj(i -> (Callable<UUID>) () -> service.place(request, book).id()).toList();
            var results = executor.invokeAll(calls);
            for (var result : results) assertThat(result.get()).isEqualTo(results.getFirst().get());
        }
        assertThat(service.list(book)).hasSize(1);
        assertThat(service.list(book).getFirst().events()).hasSize(2);
    }
}
