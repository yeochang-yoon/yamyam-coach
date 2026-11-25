package dev.yamyam;

import dev.yamyam.service.CoachService;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "MYSQL_TEST_URL", matches = "jdbc:mysql://.+")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=${MYSQL_TEST_URL}",
    "spring.datasource.username=${MYSQL_TEST_USER}",
    "spring.datasource.password=${MYSQL_TEST_PASSWORD}"
})
class LiveApiTest {

    private static final String RICE = "D101-050000000-0001";
    private static final String JUICE = "D220-716000000-0068";
    private static final String PASSWORD = "MealTest123!";

    @LocalServerPort
    private int port;

    @Autowired
    private CoachService coach;

    private final JsonMapper json = JsonMapper.builder().build();
    private final HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();

    @Test
    void searchesFoodsAndAnalyzesSeedDataForTheSignedInMember() throws Exception {
        assertEquals(401, send("GET", "/api/meals", null, null).statusCode());
        assertEquals(RICE, body(200, send("GET", "/api/foods?q=" + RICE.toLowerCase(), null, null))
            .get(0).get("foodCode").asString());
        String prefix = URLEncoder.encode("현미", StandardCharsets.UTF_8);
        JsonNode foods = body(200, send("GET", "/api/foods?mode=prefix&q=" + prefix, null, null));
        assertTrue(foods.get(0).get("name").asString().startsWith("현미"));
        assertEquals(1, body(200, send("GET", "/api/foods?page=2&size=1", null, null)).size());
        assertEquals(0, body(200, send("GET", "/api/foods?q=%25", null, null)).size());

        login("demo01", "DemoMeal01!");
        assertEquals(21, body(200, send("GET", "/api/meals", null, null)).size());
        assertEquals(3, body(200, send("GET", "/api/meals?date=2025-04-21", null, null)).size());
        assertEquals(3, body(200, send("GET", "/api/meals/1", null, null)).get("items").size());
        JsonNode daily = body(200, send("GET", "/api/meals/analysis?date=2025-04-21", null, null));
        assertEquals(1528.05, daily.get("total").get("calories").asDouble(), 0.000001);
        assertEquals(3, daily.get("mealCount").asInt());
        assertEquals(204, send("DELETE", "/api/session", null, null).statusCode());
        assertEquals(401, send("GET", "/api/session", null, null).statusCode());
        login("demo02", "DemoMeal02!");
        assertEquals(404, send("GET", "/api/meals/1", null, null).statusCode());
    }

    @Test
    void managesMembershipAndMealsThroughMysql() throws Exception {
        assertTrue(AopUtils.isAopProxy(coach));
        String username = "api_" + UUID.randomUUID().toString().substring(0, 8);
        String registration = """
            {"username":"%s","password":"%s","name":"검증 회원","height":170,"weight":65.5}
            """.formatted(username, PASSWORD);
        String withdraw = "{\"password\":\"" + PASSWORD + "\"}";
        JsonNode user = body(201, send("POST", "/api/users", registration, null));
        assertFalse(user.has("password"));
        assertFalse(user.has("passwordHash"));
        try {
            assertEquals(409, send("POST", "/api/users", registration, null).statusCode());
            login(username, PASSWORD);
            String profile = "{\"name\":\"변경된 이름\",\"height\":172,\"weight\":66,\"conditions\":\"테스트\"}";
            assertEquals("변경된 이름", body(200, send("PUT", "/api/users/me", profile, null)).get("name").asString());
            assertEquals("테스트", body(200, send("GET", "/api/users/me", null, null)).get("conditions").asString());

            String meal = """
                {"date":"2025-04-22","type":"점심","note":"등록", "items":[
                {"foodCode":"%s","amount":150,"unit":"g"},
                {"foodCode":"%s","amount":1,"unit":"g"},
                {"foodCode":"%s","amount":200,"unit":"mL"}]}
                """.formatted(RICE, RICE, JUICE);
            HttpResponse<String> creation = send("POST", "/api/meals", meal, null);
            String location = creation.headers().firstValue("Location").orElseThrow();
            assertEquals(3, body(201, creation).get("items").size());
            JsonNode detail = body(200, send("GET", location, null, null));
            assertEquals(3, detail.get("items").size());
            assertEquals("mL", detail.get("items").get(2).get("unit").asString());
            assertEquals(359.72, detail.get("nutrition").get("calories").asDouble(), 0.000001);

            assertEquals(400, send("PUT", location, meal.replace(JUICE, "UNKNOWN"), null).statusCode());
            assertEquals("등록", body(200, send("GET", location, null, null)).get("note").asString());
            assertEquals("2025-04-23", body(200, send("PUT", location,
                meal.replace("2025-04-22", "2025-04-23"), null)).get("date").asString());
            assertEquals(0, body(200, send("GET", "/api/meals?date=2025-04-22", null, null)).size());
            assertEquals(204, send("DELETE", location, null, null).statusCode());
            assertEquals(404, send("GET", location, null, null).statusCode());

            assertEquals(400, send("DELETE", "/api/users/me", "{\"password\":\"wrong\"}", null).statusCode());
            assertEquals(204, send("DELETE", "/api/users/me", withdraw, null).statusCode());
            assertEquals(401, send("GET", "/api/users/me", null, null).statusCode());
            assertEquals(401, send("POST", "/api/session",
                "{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}", null).statusCode());
        } finally {
            String login = "{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}";
            if (send("POST", "/api/session", login, null).statusCode() == 201) {
                send("DELETE", "/api/users/me", withdraw, null);
            }
        }
    }

    @Test
    void exposesSwaggerAndHandlesCorsAndValidation() throws Exception {
        JsonNode openApi = body(200, send("GET", "/v3/api-docs", null, null));
        assertTrue(openApi.get("paths").get("/api/meals").get("post").get("responses").has("201"));
        assertTrue(openApi.get("paths").get("/api/session").get("delete").get("responses").has("204"));
        assertEquals(200, send("GET", "/swagger-ui/index.html", null, null).statusCode());
        HttpResponse<String> preflight = send("OPTIONS", "/api/meals", null, "http://localhost:5500");
        assertEquals(200, preflight.statusCode());
        assertEquals("true", preflight.headers().firstValue("Access-Control-Allow-Credentials").orElseThrow());
        assertEquals(403, send("OPTIONS", "/api/meals", null, "https://example.com").statusCode());

        assertEquals(400, body(400, send("GET", "/api/foods?page=wrong", null, null)).get("status").asInt());
        assertEquals(400, body(400, send("POST", "/api/users", "{", null)).get("status").asInt());
        assertEquals(404, body(404, send("GET", "/api/missing", null, null)).get("status").asInt());
    }

    private void login(String username, String password) throws Exception {
        String request = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        assertEquals(username, body(201, send("POST", "/api/session", request, null)).get("username").asString());
    }

    private JsonNode body(int expected, HttpResponse<String> response) {
        assertEquals(expected, response.statusCode(), response.body());
        return json.readTree(response.body());
    }

    private HttpResponse<String> send(String method, String path, String body, String origin) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
            .timeout(Duration.ofSeconds(15));
        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json");
            request.method(method, HttpRequest.BodyPublishers.ofString(body));
        }
        if (origin != null) {
            request.header("Origin", origin).header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type");
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
