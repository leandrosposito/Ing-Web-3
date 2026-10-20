package ar.edu.iua.iw3.service;

import ar.edu.iua.iw3.dto.AceptarAlarmaRequest;
import ar.edu.iua.iw3.dto.AlarmaResponse;
import ar.edu.iua.iw3.exception.ConflictoException;
import ar.edu.iua.iw3.exception.RecursoNoEncontradoException;
import ar.edu.iua.iw3.model.Alarma;
import ar.edu.iua.iw3.model.ConfiguracionAlarma;
import ar.edu.iua.iw3.model.EstadoAlarma;
import ar.edu.iua.iw3.model.Orden;
import ar.edu.iua.iw3.repository.AlarmaRepository;
import ar.edu.iua.iw3.security.UsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Las alarmas quedan pendientes hasta que un usuario las acepta. */
@Service
@RequiredArgsConstructor
public class AlarmaService {

    private final AlarmaRepository alarmaRepository;
    private final ApplicationEventPublisher publisher;
    private final ConfiguracionAlarmaService configuracion;

    /**
     * Se llama con cada dato valido, dentro de la transaccion de CargaService. Si supera el umbral y la orden no
     * tiene una alarma pendiente, crea la alarma y publica el evento. La orden esta bloqueada, asi que dos datos
     * simultaneos no crean dos alarmas.
     */
    @Transactional
    public void evaluar(Orden orden, double temperatura) {
        ConfiguracionAlarma config = configuracion.obtener();
        double umbral = config.getTemperaturaUmbral();
        if (temperatura <= umbral) {
            return;
        }
        if (alarmaRepository.existsByOrdenAndEstado(orden, EstadoAlarma.PENDIENTE)) {
            return; // ya se aviso y nadie la acepto
        }
        Alarma alarma = alarmaRepository.save(new Alarma(orden, temperatura, umbral));
        publisher.publishEvent(new AlarmaCreada(alarma.getId(), orden.getNumeroOrden(), temperatura, umbral,
                List.of(config.getDestinatarios().split(","))));
    }

    @Transactional(readOnly = true)
    public List<AlarmaResponse> listar(EstadoAlarma estado) {
        List<Alarma> alarmas = (estado == null)
                ? alarmaRepository.findAllByOrderByFechaHoraDesc()
                : alarmaRepository.findByEstadoOrderByFechaHoraDesc(estado);
        return alarmas.stream().map(AlarmaResponse::desde).toList();
    }

    @Transactional
    public AlarmaResponse aceptar(Long id, AceptarAlarmaRequest req) {
        Alarma alarma = alarmaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la alarma " + id));
        if (alarma.getEstado() == EstadoAlarma.ACEPTADA) {
            throw new ConflictoException("La alarma " + id + " ya fue aceptada por " + alarma.getUsuarioAceptacion());
        }
        alarma.setEstado(EstadoAlarma.ACEPTADA);
        alarma.setUsuarioAceptacion(UsuarioActual.nombre());
        alarma.setFechaAceptacion(LocalDateTime.now());
        alarma.setObservacion(req == null ? null : req.observacion());
        return AlarmaResponse.desde(alarma);
    }
}
