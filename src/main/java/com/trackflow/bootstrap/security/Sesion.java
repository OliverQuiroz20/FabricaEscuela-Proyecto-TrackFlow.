package com.trackflow.bootstrap.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;

/**
 * Sesión abierta con un token (HU-08). Se identifica por el jti del token: el token
 * nunca se guarda, con su identificador basta y así no hay nada que robar.
 */
@Entity
@Table(name = "auth_sessions")
public class Sesion {

    @Id
    @Column(length = 64)
    private String jti;

    @Column(nullable = false, length = 50, updatable = false)
    private String username;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant lastActivityAt;

    @Column(nullable = false, updatable = false)
    private Instant expiresAt;

    private Instant closedAt;

    protected Sesion() {
    }

    public Sesion(String jti, String username, Instant createdAt, Instant expiresAt) {
        this.jti = jti;
        this.username = username;
        this.createdAt = createdAt;
        this.lastActivityAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getUsername() {
        return username;
    }

    boolean abierta() {
        return closedAt == null;
    }

    boolean inactivaDesde(Instant ahora, Duration limite) {
        return lastActivityAt.plus(limite).isBefore(ahora);
    }

    void cerrar(Instant ahora) {
        if (closedAt == null) {
            closedAt = ahora;
        }
    }

    /**
     * Registra actividad. Solo escribe si pasó más de un minuto desde la última,
     * para no hacer un UPDATE por cada petición: para una inactividad de 30 minutos,
     * un minuto de imprecisión no cambia nada.
     *
     * @return true si cambió y hay que guardarla
     */
    boolean registrarActividad(Instant ahora) {
        if (lastActivityAt.plus(Duration.ofMinutes(1)).isAfter(ahora)) {
            return false;
        }
        lastActivityAt = ahora;
        return true;
    }
}
