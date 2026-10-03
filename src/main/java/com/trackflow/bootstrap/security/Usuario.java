package com.trackflow.bootstrap.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Usuario del sistema (HU-08). Solo guarda el hash BCrypt de la contraseña, nunca
 * la contraseña: BCrypt incluye la sal en el propio hash, así que dos usuarios con
 * la misma clave tienen hashes distintos.
 *
 * Crear, editar y desactivar usuarios desde la API es la HU-12; por ahora se crean
 * al arrancar (ver UsuariosIniciales).
 */
@Entity
@Table(name = "auth_users")
public class Usuario {

    @Id
    @Column(length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String roles;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Usuario() {
    }

    public Usuario(String username, String passwordHash, String roles, boolean active, Instant createdAt) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.roles = roles;
        this.active = active;
        this.createdAt = createdAt;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRoles() {
        return roles;
    }

    public boolean isActive() {
        return active;
    }

    void cambiarPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
