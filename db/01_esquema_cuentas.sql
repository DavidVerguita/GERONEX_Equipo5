-- =====================================================================
-- GERONEX - Esquema de base de datos, etapa "Cuentas"
-- Alcance: registro, login, consulta/edicion de cuenta, cambio de contrasena
-- Compatible con MySQL 8.0.x
--
-- Seguro de volver a ejecutar: no contiene DROP, usa IF NOT EXISTS
-- e INSERT IGNORE. Si las tablas ya existen, no las modifica.
-- Se ejecuta con un usuario administrador (por ejemplo root) en Workbench.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS geronex
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE geronex;

-- ---------------------------------------------------------------------
-- ROL: los 7 roles del sistema (del diagrama de clases y del E-R)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rol (
    id_rol       INT          NOT NULL AUTO_INCREMENT,
    nombre       VARCHAR(50)  NOT NULL,
    descripcion  VARCHAR(255) NULL,
    CONSTRAINT pk_rol        PRIMARY KEY (id_rol),
    CONSTRAINT uq_rol_nombre UNIQUE (nombre)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- USUARIO: cuenta de acceso. Una sola tabla para clientes y empleados;
-- lo que los distingue es su rol.
-- contrasena guarda SOLO el hash (BCrypt), nunca el texto de la contrasena.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS usuario (
    id_usuario           INT          NOT NULL AUTO_INCREMENT,
    nombre_usuario       VARCHAR(15)  NOT NULL,
    contrasena           VARCHAR(100) NOT NULL COMMENT 'Hash BCrypt, nunca texto plano',
    correo               VARCHAR(254) NOT NULL,
    id_rol               INT          NOT NULL,
    estatus              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                      ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_usuario          PRIMARY KEY (id_usuario),
    CONSTRAINT uq_usuario_nombre   UNIQUE (nombre_usuario),
    CONSTRAINT uq_usuario_correo   UNIQUE (correo),
    CONSTRAINT fk_usuario_rol      FOREIGN KEY (id_rol) REFERENCES rol (id_rol),
    CONSTRAINT ck_usuario_estatus  CHECK (estatus IN ('ACTIVO', 'INACTIVO', 'BLOQUEADO'))
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- PERFIL: datos personales editables, 1 a 1 con usuario (diagrama de clases).
-- El nombre puede estar vacio al registrarse y se completa en "Mi cuenta".
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS perfil (
    id_perfil   INT          NOT NULL AUTO_INCREMENT,
    id_usuario  INT          NOT NULL,
    nombre      VARCHAR(100) NULL,
    CONSTRAINT pk_perfil          PRIMARY KEY (id_perfil),
    CONSTRAINT uq_perfil_usuario  UNIQUE (id_usuario),
    CONSTRAINT fk_perfil_usuario  FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
        ON DELETE CASCADE
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- DATOS INICIALES: los 7 roles (sin el prefijo ROLE_, Spring lo agrega)
-- ---------------------------------------------------------------------
INSERT IGNORE INTO rol (nombre, descripcion) VALUES
    ('ADMINISTRADOR',            'Gestiona cuentas, roles y consulta reportes'),
    ('SUPERVISOR',               'Asigna operaciones y valida reservaciones'),
    ('AGENTE',                   'Atiende solicitudes y elabora cotizaciones'),
    ('RESPONSABLE_RESERVACIONES','Valida disponibilidad y gestiona reservaciones'),
    ('RESPONSABLE_PROVEEDORES',  'Registra proveedores, servicios y precios'),
    ('AUDITOR',                  'Consulta y audita la operacion'),
    ('CLIENTE',                  'Cliente de la agencia (rol del registro publico)');
