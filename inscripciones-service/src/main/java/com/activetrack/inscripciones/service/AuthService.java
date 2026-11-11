package com.activetrack.inscripciones.service;

import com.activetrack.inscripciones.model.Usuario;
import com.activetrack.inscripciones.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Login de organizadores y jueces: valida la clave (BCrypt) y emite un JWT con su rol. */
@Service
public class AuthService {

    public record Sesion(String token, String rol, String nombre, Instant expiraEn) {
    }

    public static class CredencialesInvalidasException extends RuntimeException {
        public CredencialesInvalidasException() {
            super("Usuario o clave incorrectos");
        }
    }

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final Duration validez;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder, Clock clock,
                       @Value("${seguridad.jwt.horas-validez}") long horasValidez) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.validez = Duration.ofHours(horasValidez);
    }

    public Sesion login(String usuario, String clave) {
        Usuario u = usuarios.findByUsuario(usuario)
                .filter(encontrado -> passwordEncoder.matches(clave, encontrado.getClaveHash()))
                .orElseThrow(CredencialesInvalidasException::new);
        Instant ahora = clock.instant();
        Instant expira = ahora.plus(validez);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("activetrack")
                .subject(u.getUsuario())
                .issuedAt(ahora)
                .expiresAt(expira)
                .claim("roles", List.of(u.getRol()))
                .claim("nombre", u.getNombre())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new Sesion(token, u.getRol(), u.getNombre(), expira);
    }
}
