package ar.edu.iua.iw3.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Alarma de temperatura (umbral 40 en los tests): un solo mail hasta que alguien la acepta. */
@SpringBootTest
@AutoConfigureMockMvc
class AlarmasTest {

    private static final String ALARMAS = "/api/v1/alarmas";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    void sinSuperarElUmbralNoHayAlarmaNiMail() throws Exception {
        reset(mailSender);
        TestAuth.ordenEnCarga(mvc, 8001, "ALA001", 10000);

        TestAuth.dato(mvc, 8001, 1000, 40, 3000).andExpect(status().isOk()); // 40 no supera 40

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        TestAuth.get(mvc, "operador", ALARMAS).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.numeroOrden==8001)]").isEmpty());
    }

    @Test
    void superarElUmbralMandaUnSoloMailHastaQueSeAcepta() throws Exception {
        reset(mailSender);
        TestAuth.ordenEnCarga(mvc, 8002, "ALA002", 10000);

        // 1. Primer dato sobre el umbral: alarma pendiente + 1 mail a los dos destinatarios
        TestAuth.dato(mvc, 8002, 1000, 45.5, 3000).andExpect(jsonPath("$.aceptado").value(true));
        ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(mail.capture());
        assertArrayEquals(new String[]{"uno@test.local", "dos@test.local"}, mail.getValue().getTo());
        assertTrue(mail.getValue().getText().contains("8002"));

        // 2. Mas datos sobre el umbral: no hay otra alarma ni otro mail
        TestAuth.dato(mvc, 8002, 2000, 50, 3000);
        TestAuth.dato(mvc, 8002, 3000, 55, 3000);
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));

        String pendientes = TestAuth.get(mvc, "operador", ALARMAS + "?estado=PENDIENTE")
                .andExpect(jsonPath("$[?(@.numeroOrden==8002)]", hasSize(1)))
                .andReturn().getResponse().getContentAsString();
        int id = ((java.util.List<Integer>) JsonPath.read(pendientes, "$[?(@.numeroOrden==8002)].id")).get(0);

        // 3. Un rol sin permiso no puede aceptarla
        TestAuth.post(mvc, "tms", ALARMAS + "/" + id + "/aceptar", "{}").andExpect(status().isForbidden());

        // 4. El operador la acepta: queda registrado quien, cuando y la observacion
        TestAuth.post(mvc, "operador", ALARMAS + "/" + id + "/aceptar", "{\"observacion\":\"Se reviso el camion\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACEPTADA"))
                .andExpect(jsonPath("$.usuarioAceptacion").value("operador"))
                .andExpect(jsonPath("$.fechaAceptacion").isNotEmpty())
                .andExpect(jsonPath("$.observacion").value("Se reviso el camion"));

        // 5. Aceptarla otra vez da 409; una inexistente, 404
        TestAuth.post(mvc, "operador", ALARMAS + "/" + id + "/aceptar", "{}").andExpect(status().isConflict());
        TestAuth.post(mvc, "operador", ALARMAS + "/999999/aceptar", "{}").andExpect(status().isNotFound());

        // 6. Despues de aceptada, un nuevo dato sobre el umbral vuelve a alarmar (2do mail)
        TestAuth.dato(mvc, 8002, 4000, 60, 3000);
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
        TestAuth.get(mvc, "operador", ALARMAS + "?estado=PENDIENTE")
                .andExpect(jsonPath("$[?(@.numeroOrden==8002)]", hasSize(1)));
    }

    @Test
    void siFallaElMailLaAlarmaIgualQuedaGuardadaYElDatoSeAcepta() throws Exception {
        reset(mailSender);
        doThrow(new MailSendException("SMTP caido")).when(mailSender).send(any(SimpleMailMessage.class));
        TestAuth.ordenEnCarga(mvc, 8003, "ALA003", 10000);

        TestAuth.dato(mvc, 8003, 1000, 70, 3000)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aceptado").value(true));

        TestAuth.get(mvc, "operador", ALARMAS + "?estado=PENDIENTE")
                .andExpect(jsonPath("$[?(@.numeroOrden==8003)]", hasSize(1)));
    }

    @Test
    void unDatoDescartadoNoGeneraAlarma() throws Exception {
        reset(mailSender);
        TestAuth.ordenEnCarga(mvc, 8004, "ALA004", 10000);

        // caudal 0: se descarta aunque la temperatura sea altisima
        TestAuth.dato(mvc, 8004, 1000, 90, 0).andExpect(jsonPath("$.aceptado").value(false));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
