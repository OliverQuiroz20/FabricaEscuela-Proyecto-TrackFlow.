-- HU-08. Un JWT no se puede "borrar": sigue siendo válido hasta que vence. Para que
-- cerrar sesión sea real, se guarda el identificador (jti) de cada token cerrado y
-- el decodificador rechaza los que estén aquí. Basta con conservarlos hasta su
-- vencimiento: después de esa fecha el token ya lo rechaza la validación normal.
create table auth_revoked_tokens (
    jti varchar(64) primary key,
    subject varchar(100) not null,
    expires_at timestamp(6) with time zone not null,
    revoked_at timestamp(6) with time zone not null
);

create index idx_auth_revoked_tokens_expires_at on auth_revoked_tokens (expires_at);
