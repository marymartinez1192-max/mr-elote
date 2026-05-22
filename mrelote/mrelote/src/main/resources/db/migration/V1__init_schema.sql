-- MrElote initial schema (Supabase Postgres)
-- Auth is delegated to Supabase. Local `usuarios` row mirrors the Supabase user
-- via supabase_user_id (= JWT `sub`). No password column.

CREATE TABLE usuarios (
    id                BIGSERIAL PRIMARY KEY,
    supabase_user_id  UUID NOT NULL UNIQUE,
    nombre            VARCHAR(255) NOT NULL,
    telefono          VARCHAR(50)  NOT NULL,
    correo            VARCHAR(255) NOT NULL UNIQUE,
    direccion         VARCHAR(255) NOT NULL,
    rol               VARCHAR(32)  NOT NULL
);

CREATE TABLE categorias (
    id     BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE productos (
    id           BIGSERIAL PRIMARY KEY,
    nombre       VARCHAR(255)   NOT NULL,
    descripcion  TEXT,
    precio       NUMERIC(19, 2) NOT NULL,
    imagen_url   VARCHAR(1024),
    disponible   BOOLEAN        NOT NULL,
    categoria_id BIGINT         NOT NULL REFERENCES categorias(id)
);

CREATE TABLE carritos (
    id                    BIGSERIAL PRIMARY KEY,
    usuario_id            BIGINT    NOT NULL UNIQUE REFERENCES usuarios(id),
    fecha_creacion        TIMESTAMP NOT NULL,
    ultima_actualizacion  TIMESTAMP NOT NULL
);

CREATE TABLE items_carrito (
    id              BIGSERIAL PRIMARY KEY,
    carrito_id      BIGINT         NOT NULL REFERENCES carritos(id),
    producto_id     BIGINT         NOT NULL REFERENCES productos(id),
    cantidad        INTEGER        NOT NULL,
    precio_unitario NUMERIC(19, 2) NOT NULL,
    subtotal        NUMERIC(19, 2) NOT NULL
);

CREATE TABLE pedidos (
    id              BIGSERIAL PRIMARY KEY,
    usuario_id      BIGINT         NOT NULL REFERENCES usuarios(id),
    estado          VARCHAR(32)    NOT NULL,
    tarifa_envio    NUMERIC(19, 2) NOT NULL,
    total           NUMERIC(19, 2) NOT NULL,
    fecha_creacion  TIMESTAMP      NOT NULL
);

CREATE TABLE items_pedido (
    id              BIGSERIAL PRIMARY KEY,
    pedido_id       BIGINT         NOT NULL REFERENCES pedidos(id),
    producto_id     BIGINT         NOT NULL REFERENCES productos(id),
    cantidad        INTEGER        NOT NULL,
    precio_unitario NUMERIC(19, 2) NOT NULL,
    subtotal        NUMERIC(19, 2) NOT NULL
);

CREATE TABLE config_negocio (
    id               BIGSERIAL PRIMARY KEY,
    horario_apertura TIME    NOT NULL,
    horario_cierre   TIME    NOT NULL,
    cerrado_manual   BOOLEAN NOT NULL
);
