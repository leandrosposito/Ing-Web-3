package ar.edu.iua.iw3.service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Regla de frecuencia de almacenamiento del detalle de carga (punto 3). Es una clase sin dependencias para
 * poder testearla sola, igual que ReglasDetalleCarga.
 */
public final class ReglasAlmacenamiento {

    private ReglasAlmacenamiento() {
    }

    /**
     * @param ultimoGuardado        cuando se guardo el ultimo detalle (null si todavia no se guardo ninguno)
     * @param ahora                 momento en que llega el dato
     * @param frecuenciaSegundos    cada cuantos segundos se guarda un detalle (0 o menos = guardar todos)
     * @return true si este dato se tiene que guardar como detalle
     */
    public static boolean debeAlmacenar(LocalDateTime ultimoGuardado, LocalDateTime ahora, long frecuenciaSegundos) {
        if (frecuenciaSegundos <= 0 || ultimoGuardado == null) {
            return true;
        }
        return Duration.between(ultimoGuardado, ahora).toSeconds() >= frecuenciaSegundos;
    }
}
