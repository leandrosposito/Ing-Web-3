package ar.edu.iua.iw3.controller;

import ar.edu.iua.iw3.dto.ErrorResponse;
import ar.edu.iua.iw3.dto.LoginRequest;
import ar.edu.iua.iw3.dto.LoginResponse;
import ar.edu.iua.iw3.security.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "0. Autenticacion", description = "Login: devuelve el token JWT para usar en el resto de las APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Con usuario y contrasena devuelve un token. Enviarlo en cada request como 'Authorization: Bearer <token>'.")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "Faltan datos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Usuario o contrasena incorrectos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }
}
