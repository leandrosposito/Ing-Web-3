package ar.edu.iua.iw3.controller;

import ar.edu.iua.iw3.service.MonitoreoService;
import ar.edu.iua.iw3.service.OrdenService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El listado de ordenes y el monitoreo no hacen una consulta por cada orden (problema N+1):
 * la cantidad de consultas SQL no depende de cuantas ordenes haya.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
class ConsultasSinN1Test {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private EntityManagerFactory emf;
    @Autowired
    private MonitoreoService monitoreoService;
    @Autowired
    private OrdenService ordenService;

    @MockitoBean
    private JavaMailSender mailSender;

    private Statistics estadisticas() {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        return stats;
    }

    @Test
    void monitoreoHaceUnaSolaConsultaAunConVariasOrdenes() throws Exception {
        for (int i = 0; i < 4; i++) {
            TestAuth.ordenEnCarga(mvc, 9100 + i, "N1M00" + i, 10000);
        }
        Statistics stats = estadisticas();

        int filas = monitoreoService.listar(null, "", "patente", false).size();

        assertTrue(filas >= 4);
        assertEquals(1, stats.getPrepareStatementCount(), "monitoreo: una consulta, no una por orden");
    }

    @Test
    void listadoDeOrdenesNoHaceUnaConsultaPorOrden() throws Exception {
        for (int i = 0; i < 4; i++) {
            TestAuth.ordenEnCarga(mvc, 9200 + i, "N1L00" + i, 10000);
        }
        Statistics stats = estadisticas();

        int filas = ordenService.listar(null).size();

        assertTrue(filas >= 4);
        // 1 consulta de ordenes (con camion, chofer, cliente y producto) + 1 para las cisternas (@BatchSize)
        assertTrue(stats.getPrepareStatementCount() <= 2,
                "listado: " + stats.getPrepareStatementCount() + " consultas para " + filas + " ordenes");
    }
}
