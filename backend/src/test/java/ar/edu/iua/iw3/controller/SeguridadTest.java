package ar.edu.iua.iw3.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Login, 401 (sin token) y 403 (rol equivocado) de cada grupo de endpoints. */
@SpringBootTest
@AutoConfigureMockMvc
class SeguridadTest {

    private static final String BASE = "/api/v1/ordenes";

    @Autowired
    private MockMvc mvc;

    @Test
    void loginCorrectoDevuelveTokenYRol() throws Exception {
        mvc.perform(login("tms", "demo1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.rol").value("TMS"));
    }

    @Test
    void loginIncorrectoDa401() throws Exception {
        mvc.perform(login("tms", "mala")).andExpect(status().isUnauthorized());
        mvc.perform(login("noexiste", "demo1234")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sinTokenDa401ConElFormatoDeError() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value(BASE));
    }

    @Test
    void tokenAdulteradoDa401() throws Exception {
        // Se pisan los ultimos caracteres de la firma: ya no coincide con el contenido del token
        String valido = TestAuth.bearer(mvc, "admin");
        String adulterado = valido.substring(0, valido.length() - 5) + "AAAAA";
        mvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, adulterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cadaRolSoloUsaSusEndpoints() throws Exception {
        // SAP no puede cargar datos ni hacer pesajes
        mvc.perform(conRol("sap", post(BASE + "/1/detalle"))).andExpect(status().isForbidden());
        mvc.perform(conRol("sap", post(BASE + "/1/pesaje-inicial"))).andExpect(status().isForbidden());
        // TMS no puede crear ordenes ni cerrarlas
        mvc.perform(conRol("tms", post(BASE))).andExpect(status().isForbidden());
        mvc.perform(conRol("tms", post(BASE + "/1/cierre"))).andExpect(status().isForbidden());
        // El sistema de carga no puede hacer pesajes
        mvc.perform(conRol("carga", post(BASE + "/1/pesaje-final"))).andExpect(status().isForbidden());
        // El operador solo consulta
        mvc.perform(conRol("operador", post(BASE))).andExpect(status().isForbidden());
        mvc.perform(conRol("operador", post(BASE + "/1/habilitar-carga"))).andExpect(status().isForbidden());
    }

    @Test
    void consultasLasPuedeUsarCualquierUsuarioLogueado() throws Exception {
        for (String usuario : new String[]{"sap", "tms", "carga", "operador", "admin"}) {
            mvc.perform(conRol(usuario, get(BASE))).andExpect(status().isOk());
        }
    }

    @Test
    void adminPuedeUsarLosEndpointsDeEscritura() throws Exception {
        // Pasa la seguridad: la orden no existe, asi que llega al service y responde 404 (no 403)
        mvc.perform(conRol("admin", post(BASE + "/999999/cierre"))).andExpect(status().isNotFound());
    }

    private MockHttpServletRequestBuilder login(String usuario, String password) {
        return post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(usuario, password));
    }

    private MockHttpServletRequestBuilder conRol(String usuario, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, TestAuth.bearer(mvc, usuario));
    }
}
