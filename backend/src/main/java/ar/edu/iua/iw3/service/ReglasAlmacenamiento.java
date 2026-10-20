package ar.edu.iua.iw3.service;

import java.time.Duration;
import java.time.LocalDateTime;

/** Cada cuanto se guarda un detalle de carga (sin dependencias, para testearla sola). */
public final class ReglasAlmacenamiento {

    private ReglasAlmacenamiento() {
    }

    /** ultimoGuardado es null si todavia no se guardo ninguno; frecuencia 0 = guardar todos. */
    public static boolean debeAlmacenar(LocalDateTime ultimoGuardado, LocalDateTime ahora, long frecuenciaSegundos) {
        if (frecuenciaSegundos <= 0 || ultimoGuardado == null) {
            return true;
        }
        return Duration.between(ultimoGuardado, ahora).toSeconds() >= frecuenciaSegundos;
    }
}
