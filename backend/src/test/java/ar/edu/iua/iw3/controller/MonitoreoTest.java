package ar.edu.iua.iw3.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Monitoreo: columnas calculadas, filtros y ordenamiento (los hace el backend). */
@SpringBootTest
@AutoConfigureMockMvc
class MonitoreoTest {

    private static final String URL = "/api/v1/monitoreo";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JavaMailSender mailSender; // los datos de estos tests no superan el umbral, pero no queremos SMTP real

    @Test
    void calculaEtaYTiempoSoloEnEstado2() throws Exception {
        TestAuth.ordenEnCarga(mvc, 7001, "MON001", 10000);
        TestAuth.dato(mvc, 7001, 4000, 20, 3000).andExpect(status().isOk());

        TestAuth.get(mvc, "operador", URL + "?patente=MON001")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numeroOrden").value(7001))
                .andExpect(jsonPath("$[0].estadoNumero").value(2))
                .andExpect(jsonPath("$[0].ultimaMasaAcumulada").value(4000.0))
                .andExpect(jsonPath("$[0].etaHoras").value(2.0))
                .andExpect(jsonPath("$[0].tiempoTranscurridoSegundos").isNumber());

        // Al cerrarla (estado 3) ya no se informan
        TestAuth.post(mvc, "carga", "/api/v1/ordenes/7001/cierre", "{}").andExpect(status().isOk());
        TestAuth.get(mvc, "operador", URL + "?patente=MON001")
                .andExpect(jsonPath("$[0].estadoNumero").value(3))
                .andExpect(jsonPath("$[0].etaHoras").value(nullValue()))
                .andExpect(jsonPath("$[0].tiempoTranscurridoSegundos").value(nullValue()));
    }

    @Test
    void filtraPorEstadoYPatente() throws Exception {
        TestAuth.ordenEnCarga(mvc, 7011, "FIL777", 10000);   // queda en estado 2
        TestAuth.ordenEnCarga(mvc, 7012, "FIL888", 10000);
        TestAuth.post(mvc, "carga", "/api/v1/ordenes/7012/cierre", "{}").andExpect(status().isOk()); // estado 3

        // por patente (sin distinguir mayusculas, "contiene")
        TestAuth.get(mvc, "operador", URL + "?patente=fil7")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].numeroOrden").value(7011));

        // por estado: todas las de estado 3 lo son, y la 7012 esta entre ellas
        TestAuth.get(mvc, "operador", URL + "?estado=CERRADA_PARA_CARGA")
                .andExpect(jsonPath("$[*].estadoNumero", everyItem(is(3))))
                .andExpect(jsonPath("$[*].numeroOrden", hasItem(7012)))
                .andExpect(jsonPath("$[*].numeroOrden", not(hasItem(7011))));
    }

    @Test
    void ordenaPorLaColumnaPedida() throws Exception {
        TestAuth.ordenEnCarga(mvc, 7021, "ORD021", 10000);
        TestAuth.ordenEnCarga(mvc, 7022, "ORD022", 10000);
        TestAuth.dato(mvc, 7021, 1000, 20, 3000);
        TestAuth.dato(mvc, 7022, 2000, 20, 3000);

        TestAuth.get(mvc, "operador", URL + "?patente=ORD02&ordenarPor=masa&direccion=desc")
                .andExpect(jsonPath("$[0].numeroOrden").value(7022))
                .andExpect(jsonPath("$[1].numeroOrden").value(7021));
        TestAuth.get(mvc, "operador", URL + "?patente=ORD02&ordenarPor=masa&direccion=asc")
                .andExpect(jsonPath("$[0].numeroOrden").value(7021));
        // una columna que no esta en la lista no rompe: se ordena por numero de orden
        TestAuth.get(mvc, "operador", URL + "?patente=ORD02&ordenarPor=cualquier;cosa")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numeroOrden").value(7021));
    }

    @Test
    void requiereEstarLogueado() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(URL))
                .andExpect(status().isUnauthorized());
    }
}
