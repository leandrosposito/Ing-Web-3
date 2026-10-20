package ar.edu.iua.iw3.controller;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Umbral y destinatarios editables desde el sistema (solo ADMIN). Los valores iniciales salen de las propiedades. */
@SpringBootTest
@AutoConfigureMockMvc
class ConfiguracionAlarmaTest {

    private static final String CONFIG = "/api/v1/alarmas/configuracion";
    private static final String ORIGINAL = "{\"temperaturaUmbral\":40,\"destinatarios\":[\"uno@test.local\",\"dos@test.local\"]}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    void laConfiguracionInicialSaleDeLasPropiedades() throws Exception {
        TestAuth.get(mvc, "admin", CONFIG)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temperaturaUmbral").value(40.0))
                .andExpect(jsonPath("$.destinatarios[0]").value("uno@test.local"))
                .andExpect(jsonPath("$.destinatarios[1]").value("dos@test.local"));
    }

    @Test
    void soloAdminPuedeVerlaYCambiarla() throws Exception {
        TestAuth.get(mvc, "operador", CONFIG).andExpect(status().isForbidden());
        TestAuth.put(mvc, "operador", CONFIG, ORIGINAL).andExpect(status().isForbidden());
        TestAuth.put(mvc, "carga", CONFIG, ORIGINAL).andExpect(status().isForbidden());
        // sin token: 401
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(CONFIG))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rechazaValoresInvalidos() throws Exception {
        TestAuth.put(mvc, "admin", CONFIG, "{\"temperaturaUmbral\":40,\"destinatarios\":[\"no-es-un-mail\"]}")
                .andExpect(status().isBadRequest());
        TestAuth.put(mvc, "admin", CONFIG, "{\"temperaturaUmbral\":40,\"destinatarios\":[]}")
                .andExpect(status().isBadRequest());
        TestAuth.put(mvc, "admin", CONFIG, "{\"temperaturaUmbral\":900,\"destinatarios\":[\"a@test.local\"]}")
                .andExpect(status().isBadRequest());
        TestAuth.put(mvc, "admin", CONFIG, "{\"destinatarios\":[\"a@test.local\"]}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void elCambioRigeEnElProximoDatoSinReiniciar() throws Exception {
        reset(mailSender);
        try {
            TestAuth.put(mvc, "admin", CONFIG,
                            "{\"temperaturaUmbral\":60,\"destinatarios\":[\"nuevo@test.local\"]}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.temperaturaUmbral").value(60.0))
                    .andExpect(jsonPath("$.actualizadoPor").value("admin"))
                    .andExpect(jsonPath("$.fechaActualizacion").isNotEmpty());
            TestAuth.get(mvc, "admin", CONFIG).andExpect(jsonPath("$.destinatarios[0]").value("nuevo@test.local"));

            TestAuth.ordenEnCarga(mvc, 8101, "CFG001", 10000);

            // 50 supera el umbral viejo (40) pero no el nuevo (60): sin alarma ni mail
            TestAuth.dato(mvc, 8101, 1000, 50, 3000).andExpect(status().isOk());
            verify(mailSender, after(300).never()).send(any(SimpleMailMessage.class));

            // 65 supera el nuevo umbral: 1 mail, solo al destinatario nuevo, con el umbral nuevo en la alarma
            TestAuth.dato(mvc, 8101, 2000, 65, 3000).andExpect(status().isOk());
            ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender, timeout(2000).times(1)).send(mail.capture()); // el mail sale en otro hilo (@Async)
            assertArrayEquals(new String[]{"nuevo@test.local"}, mail.getValue().getTo());
            TestAuth.get(mvc, "operador", "/api/v1/alarmas?estado=PENDIENTE")
                    .andExpect(jsonPath("$[?(@.numeroOrden==8101)].umbral", hasItem(60.0)));
        } finally {
            TestAuth.put(mvc, "admin", CONFIG, ORIGINAL).andExpect(status().isOk());
        }
    }
}
