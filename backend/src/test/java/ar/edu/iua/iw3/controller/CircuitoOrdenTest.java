package ar.edu.iua.iw3.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de integracion: levanta la aplicacion completa (con H2) y recorre el circuito de una orden
 * por HTTP, igual que lo harian SAP, TMS y el sistema de carga (cada uno con su usuario).
 */
@SpringBootTest
@AutoConfigureMockMvc
class CircuitoOrdenTest {

    private static final String BASE = "/api/v1/ordenes";

    @Autowired
    private MockMvc mvc;

    @Test
    void circuitoCompletoDeUnaOrden() throws Exception {
        long nro = 5001;

        // 1. SAP crea la orden -> estado 1
        postJson("sap", BASE, ordenJson(nro))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoNumero").value(1))
                .andExpect(jsonPath("$.camion.capacidadTotalLitros").value(15000.0));

        // 2. TMS registra la tara -> estado 2 + password
        String respTara = postJson("tms", BASE + "/" + nro + "/pesaje-inicial", "{\"pesaje\":15000}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CON_PESAJE_INICIAL"))
                .andReturn().getResponse().getContentAsString();
        int password = JsonPath.read(respTara, "$.password");

        // 3.1 El sistema de carga se habilita con la password y recibe el preset
        postJson("carga", BASE + "/" + nro + "/habilitar-carga", "{\"password\":" + password + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preset").value(20000.0));

        // 3.3 Datos en tiempo real: 3 validos y 1 descartado
        postJson("carga", BASE + "/" + nro + "/detalle", detalleJson(1000, 0.5, 10, 50000)).andExpect(jsonPath("$.aceptado").value(true));
        postJson("carga", BASE + "/" + nro + "/detalle", detalleJson(2000, 0.6, 20, 60000)).andExpect(jsonPath("$.aceptado").value(true));
        postJson("carga", BASE + "/" + nro + "/detalle", detalleJson(1500, 0.6, 20, 60000)) // masa menor a la anterior
                .andExpect(jsonPath("$.aceptado").value(false));
        postJson("carga", BASE + "/" + nro + "/detalle", detalleJson(3000, 0.7, 30, 70000)).andExpect(jsonPath("$.aceptado").value(true));

        mvc.perform(getAuth("operador", BASE + "/" + nro))
                .andExpect(jsonPath("$.ultimaMasaAcumulada").value(3000.0))
                .andExpect(jsonPath("$.ultimaTemperatura").value(30.0));

        // 4. Cierre -> estado 3; ya no se aceptan datos
        mvc.perform(post(BASE + "/" + nro + "/cierre").header(HttpHeaders.AUTHORIZATION, TestAuth.bearer(mvc, "carga")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoNumero").value(3));
        postJson("carga", BASE + "/" + nro + "/detalle", detalleJson(4000, 0.7, 30, 70000)).andExpect(status().isConflict());

        // 5. Pesaje final -> estado 4 + conciliacion
        postJson("tms", BASE + "/" + nro + "/pesaje-final", "{\"pesaje\":18100}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productoCargado").value(3000.0))
                .andExpect(jsonPath("$.netoPorBalanza").value(3100.0))
                .andExpect(jsonPath("$.diferenciaBalanzaCaudalimetro").value(100.0))
                .andExpect(jsonPath("$.promedioTemperatura").value(20.0))
                .andExpect(jsonPath("$.promedioCaudal").value(60000.0));

        mvc.perform(getAuth("operador", BASE + "/" + nro + "/conciliacion")).andExpect(status().isOk());

        // La auditoria guarda el usuario autenticado de cada cambio de estado
        mvc.perform(getAuth("operador", BASE + "/" + nro + "/historial-estados"))
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].usuario").value("sap"))
                .andExpect(jsonPath("$[1].usuario").value("tms"))
                .andExpect(jsonPath("$[2].usuario").value("carga"))
                .andExpect(jsonPath("$[3].usuario").value("tms"));
    }

    @Test
    void erroresDeNegocio() throws Exception {
        long nro = 5002;
        postJson("sap", BASE, ordenJson(nro)).andExpect(status().isCreated());

        postJson("sap", BASE, ordenJson(nro)).andExpect(status().isConflict());                       // duplicada
        mvc.perform(getAuth("operador", BASE + "/999999")).andExpect(status().isNotFound());           // inexistente
        mvc.perform(getAuth("operador", BASE + "/" + nro + "/conciliacion"))
                .andExpect(status().isConflict());                                                      // no finalizada
        postJson("carga", BASE + "/" + nro + "/habilitar-carga", "{\"password\":12345}")
                .andExpect(status().isConflict());                                                      // sin tara todavia

        postJson("tms", BASE + "/" + nro + "/pesaje-inicial", "{\"pesaje\":15000}").andExpect(status().isOk());
        postJson("carga", BASE + "/" + nro + "/habilitar-carga", "{\"password\":1}")
                .andExpect(status().isUnauthorized());                                                  // password incorrecta
        postJson("tms", BASE + "/" + nro + "/pesaje-inicial", "{\"pesaje\":-5}")
                .andExpect(status().isBadRequest());                                                    // validacion
    }

    /** POST autenticado con el usuario de demo indicado. */
    private ResultActions postJson(String usuario, String url, String body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body)
                .header(HttpHeaders.AUTHORIZATION, TestAuth.bearer(mvc, usuario)));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder getAuth(String usuario, String url) {
        return get(url).header(HttpHeaders.AUTHORIZATION, TestAuth.bearer(mvc, usuario));
    }

    private static String detalleJson(double masa, double densidad, double temperatura, double caudal) {
        return "{\"masaAcumulada\":%s,\"densidad\":%s,\"temperatura\":%s,\"caudal\":%s}"
                .formatted(masa, densidad, temperatura, caudal);
    }

    private static String ordenJson(long nro) {
        return """
                {"numeroOrden": %d,
                 "camion": {"codigoExterno": "CAM-T", "patente": "AA000AA",
                            "cisternas": [{"numero": 1, "capacidadLitros": 10000}, {"numero": 2, "capacidadLitros": 5000}]},
                 "chofer": {"codigoExterno": "CHO-T", "nombre": "Ana", "apellido": "Gomez"},
                 "cliente": {"codigoExterno": "CLI-T", "razonSocial": "Cliente Test"},
                 "producto": {"codigoExterno": "PRO-T", "nombre": "Butano"},
                 "fechaPrevistaCarga": "2026-11-10T08:00:00",
                 "preset": 20000}""".formatted(nro);
    }
}
