package ar.edu.iua.iw3.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sin el perfil "h2" (por ejemplo, en un servidor con MySQL) la consola de H2 no se puede abrir. */
@SpringBootTest
@AutoConfigureMockMvc
class H2ConsolaCerradaTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    void sinTokenNoSePuedeAbrir() throws Exception {
        mvc.perform(get("/h2-console")).andExpect(status().isUnauthorized());
        mvc.perform(get("/h2-console/")).andExpect(status().isUnauthorized());
    }

    @Test
    void conTokenTampocoHayConsola() throws Exception {
        // el usuario esta autenticado, pero la consola no esta habilitada: no existe
        TestAuth.get(mvc, "admin", "/h2-console").andExpect(status().isNotFound());
    }
}
