-- HU-08. Usuarios y sesiones.
--
-- Hasta ahora las credenciales vivían solo en variables de entorno y se comparaban
-- en texto plano. Ahora cada usuario tiene su contraseña guardada con BCrypt (que
-- incluye la sal en el propio hash) y un indicador de activo: a un usuario se le
-- desactiva, no se le borra, para no perder la trazabilidad de lo que hizo.
create table auth_users (
    username varchar(50) primary key,
    password_hash varchar(100) not null,
    -- Separados por espacio, como viajan en el claim "roles" del token.
    roles varchar(100) not null,
    active boolean not null default true,
    created_at timestamp(6) with time zone not null
);

-- Un JWT no se puede "borrar": sigue siendo válido hasta que vence. Para que cerrar
-- sesión sea real y para cerrar las sesiones inactivas, cada token emitido tiene su
-- sesión, identificada por el jti, y el servidor la consulta en cada petición.
create table auth_sessions (
    jti varchar(64) primary key,
    username varchar(50) not null references auth_users (username),
    created_at timestamp(6) with time zone not null,
    last_activity_at timestamp(6) with time zone not null,
    expires_at timestamp(6) with time zone not null,
    -- Null mientras la sesión esté abierta.
    closed_at timestamp(6) with time zone
);

create index idx_auth_sessions_expires_at on auth_sessions (expires_at);
