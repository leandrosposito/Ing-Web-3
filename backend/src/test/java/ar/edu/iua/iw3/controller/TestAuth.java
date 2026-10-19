package ar.edu.iua.iw3.controller;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    /** POST con JSON autenticado como ese usuario de demo. */
    static ResultActions post(MockMvc mvc, String usuario, String url, String json) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(url).contentType(MediaType.APPLICATION_JSON).content(json)
                .header(HttpHeaders.AUTHORIZATION, bearer(mvc, usuario)));
    }

    /** GET autenticado como ese usuario de demo. */
    static ResultActions get(MockMvc mvc, String usuario, String url) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.get(url).header(HttpHeaders.AUTHORIZATION, bearer(mvc, usuario)));
    }

    /** Crea una orden (SAP) con la temperatura y preset dados y la deja en estado 2 (TMS), lista para cargar. */
    static void ordenEnCarga(MockMvc mvc, long numeroOrden, String patente, double preset) throws Exception {
        String orden = """
                {"numeroOrden": %d,
                 "camion": {"codigoExterno": "CAM-%d", "patente": "%s",
                            "cisternas": [{"numero": 1, "capacidadLitros": 10000}]},
                 "chofer": {"codigoExterno": "CHO-M", "nombre": "Ana", "apellido": "Gomez"},
                 "cliente": {"codigoExterno": "CLI-M", "razonSocial": "Cliente Test"},
                 "producto": {"codigoExterno": "PRO-M", "nombre": "Butano"},
                 "fechaPrevistaCarga": "2026-11-10T08:00:00",
                 "preset": %s}""".formatted(numeroOrden, numeroOrden, patente, preset);
        post(mvc, "sap", "/api/v1/ordenes", orden).andExpect(status().isCreated());
        post(mvc, "tms", "/api/v1/ordenes/" + numeroOrden + "/pesaje-inicial", "{\"pesaje\":15000}")
                .andExpect(status().isOk());
    }

    /** Envia un dato de carga como el sistema de carga. */
    static ResultActions dato(MockMvc mvc, long numeroOrden, double masa, double temperatura, double caudal) throws Exception {
        return post(mvc, "carga", "/api/v1/ordenes/" + numeroOrden + "/detalle",
                "{\"masaAcumulada\":%s,\"densidad\":0.5,\"temperatura\":%s,\"caudal\":%s}".formatted(masa, temperatura, caudal));
    }

    private static String login(MockMvc mvc, String username) {
        try {
            String body = mvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, PASSWORD_DEMO)))
                    .andReturn().getResponse().getContentAsString();
            return JsonPath.read(body, "$.token");
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo hacer login con " + username, e);
        }
    }
}
