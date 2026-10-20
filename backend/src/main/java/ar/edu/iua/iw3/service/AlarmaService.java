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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Alarmas de temperatura: se crean al superar el umbral y quedan pendientes hasta que un usuario las acepta. */
@Service
public class AlarmaService {

    private final AlarmaRepository alarmaRepository;
    private final ApplicationEventPublisher publisher;

    /** Umbral y destinatarios vigentes (se editan desde el sistema, por eso se leen en cada evaluacion). */
    private final ConfiguracionAlarmaService configuracion;

    public AlarmaService(AlarmaRepository alarmaRepository, ApplicationEventPublisher publisher,
                         ConfiguracionAlarmaService configuracion) {
        this.alarmaRepository = alarmaRepository;
        this.publisher = publisher;
        this.configuracion = configuracion;
    }

    /**
     * Se llama con cada dato de carga valido (dentro de la transaccion de CargaService).
     * Si supera el umbral y la orden no tiene ya una alarma pendiente, crea la alarma y avisa por evento.
     * Como la orden esta bloqueada (SELECT ... FOR UPDATE), dos datos simultaneos no crean dos alarmas.
     */
    @Transactional
    public void evaluar(Orden orden, double temperatura) {
        ConfiguracionAlarma config = configuracion.obtener();
        double umbral = config.getTemperaturaUmbral();
        if (temperatura <= umbral) {
            return;
        }
        if (alarmaRepository.existsByOrdenAndEstado(orden, EstadoAlarma.PENDIENTE)) {
            return; // ya se aviso y nadie la acepto: no se vuelve a mandar
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

    /** Acepta la alarma registrando quien (usuario del token), cuando y la observacion. */
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
