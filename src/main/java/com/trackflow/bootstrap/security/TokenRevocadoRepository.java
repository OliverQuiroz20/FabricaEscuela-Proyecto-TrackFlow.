package com.trackflow.bootstrap.security;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface TokenRevocadoRepository extends JpaRepository<TokenRevocado, String> {

    /** Los vencidos ya los rechaza la validación de fecha; guardarlos no aporta nada. */
    @Transactional
    @Modifying
    @Query("delete from TokenRevocado t where t.expiresAt < :ahora")
    int borrarVencidos(Instant ahora);
}
