package ar.edu.iua.iw3.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Con el perfil "h2" la consola se abre sin token y deja usar frames del mismo origen; la API sigue protegida. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class H2ConsolaPerfilH2Test {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    void seAbreSinTokenYPermiteFramesDelMismoOrigen() throws Exception {
        int status = mvc.perform(get("/h2-console/"))
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"))
                .andReturn().getResponse().getStatus();
        assertNotEquals(401, status);
        assertNotEquals(403, status);
    }

    @Test
    void laApiSigueProtegida() throws Exception {
        mvc.perform(get("/api/v1/ordenes")).andExpect(status().isUnauthorized());
    }
}
