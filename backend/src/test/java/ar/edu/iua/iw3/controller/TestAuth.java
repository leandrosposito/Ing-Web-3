package ar.edu.iua.iw3.controller;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Ayuda para los tests: hace login con un usuario de demo y devuelve el header "Authorization". */
final class TestAuth {

    private static final String PASSWORD_DEMO = "demo1234";
    private static final Map<String, String> TOKENS = new ConcurrentHashMap<>();

    private TestAuth() {
    }

    /** Header listo para usar: "Bearer <token>". El token se pide una sola vez por usuario. */
    static String bearer(MockMvc mvc, String username) {
        return "Bearer " + TOKENS.computeIfAbsent(username, u -> login(mvc, u));
    }

    private static String login(MockMvc mvc, String username) {
        try {
            String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, PASSWORD_DEMO)))
                    .andReturn().getResponse().getContentAsString();
            return JsonPath.read(body, "$.token");
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo hacer login con " + username, e);
        }
    }
}
