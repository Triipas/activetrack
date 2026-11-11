package com.activetrack.inscripciones.controller;

import com.activetrack.inscripciones.service.AuthService;
import com.activetrack.inscripciones.service.AuthService.Sesion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    public record LoginRequest(@NotBlank String usuario, @NotBlank String clave) {
    }

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/api/auth/login")
    public Sesion login(@Valid @RequestBody LoginRequest r) {
        return auth.login(r.usuario(), r.clave());
    }
}
