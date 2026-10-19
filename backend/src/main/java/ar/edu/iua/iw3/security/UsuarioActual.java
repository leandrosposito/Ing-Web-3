package ar.edu.iua.iw3.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Nombre del usuario autenticado en el request actual (lo usan la auditoria de estados y las alarmas). */
public final class UsuarioActual {

    private UsuarioActual() {
    }

    public static String nombre() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth == null) ? "sistema" : auth.getName();
    }
}
