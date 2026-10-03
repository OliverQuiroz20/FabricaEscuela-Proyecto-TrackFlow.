package com.trackflow.bootstrap.security;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SesionRepository extends JpaRepository<Sesion, String> {

    /** Las vencidas ya las rechaza la validación de fecha del token; guardarlas no aporta. */
    @Modifying
    @Query("delete from Sesion s where s.expiresAt < :ahora")
    int borrarVencidas(Instant ahora);
}
