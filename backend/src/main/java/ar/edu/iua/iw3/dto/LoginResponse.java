package ar.edu.iua.iw3.dto;

import ar.edu.iua.iw3.model.Rol;

/** Lo que recibe el cliente al loguearse: el token para mandar en cada request y quien es. */
public record LoginResponse(
        String token,
        String username,
        Rol rol) {
}
