package com.trackflow.bootstrap.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Token cuya sesión se cerró antes de vencer (HU-08). Solo se guarda su jti, nunca
 * el token: con el identificador basta para rechazarlo y no hay nada que robar.
 */
@Entity
@Table(name = "auth_revoked_tokens")
public class TokenRevocado {

    @Id
    @Column(length = 64)
    private String jti;

    @Column(nullable = false, length = 100)
    private String subject;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant revokedAt;

    protected TokenRevocado() {
    }

    public TokenRevocado(String jti, String subject, Instant expiresAt, Instant revokedAt) {
        this.jti = jti;
        this.subject = subject;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
    }
}
