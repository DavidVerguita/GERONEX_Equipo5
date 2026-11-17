-- =====================================================================
-- GERONEX - Esquema de base de datos, etapa "Negocio"
-- Alcance: catalogo (categorias, proveedores, servicios, disponibilidad),
--          agentes, solicitudes de viaje, cotizaciones y reservaciones.
-- Compatible con MySQL 8.0.x
--
-- REQUISITO: haber ejecutado antes 01_esquema_cuentas.sql.
-- Seguro de volver a ejecutar: no contiene DROP, usa IF NOT EXISTS.
-- Se ejecuta con root en Workbench.
--
-- Los nombres de tablas vienen del diagrama entidad-relacion del equipo.
-- Cambio respecto al E-R (opcion A acordada): agente tiene id_usuario,
-- para que cada agente sea una cuenta con rol AGENTE que puede iniciar sesion.
--
-- Se dejan para una fase siguiente: solicitud_cambio, reasignacion,
-- seguimiento_comercial, seguimiento_proveedor, auditoria, ajuste_cotizacion,
-- publico_objetivo, detalle_reservacion, permiso y rol_permiso.
-- =====================================================================

USE geronex;

-- ---------------------------------------------------------------------
-- CATEGORIA_SERVICIO: hotel, vuelo, transporte, paquete...
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categoria_servicio (
    id_categoria  INT          NOT NULL AUTO_INCREMENT,
    nombre        VARCHAR(50)  NOT NULL,
    descripcion   VARCHAR(255) NULL,
    CONSTRAINT pk_categoria_servicio PRIMARY KEY (id_categoria),
    CONSTRAINT uq_categoria_nombre   UNIQUE (nombre)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- PROVEEDOR: empresas externas (ficticias) que dan el servicio
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS proveedor (
    id_proveedor  INT          NOT NULL AUTO_INCREMENT,
    nombre        VARCHAR(100) NOT NULL,
    telefono      VARCHAR(10)  NULL,
    correo        VARCHAR(254) NULL,
    estatus       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT pk_proveedor         PRIMARY KEY (id_proveedor),
    CONSTRAINT uq_proveedor_nombre  UNIQUE (nombre),
    CONSTRAINT ck_proveedor_estatus CHECK (estatus IN ('ACTIVO', 'INACTIVO'))
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- SERVICIO: lo que el agente puede meter en una cotizacion
-- unidad: como se cobra el precio ('por persona', 'por noche', 'por viaje')
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS servicio (
    id_servicio   INT           NOT NULL AUTO_INCREMENT,
    id_categoria  INT           NOT NULL,
    id_proveedor  INT           NOT NULL,
    nombre        VARCHAR(100)  NOT NULL,
    descripcion   VARCHAR(500)  NULL,
    destino       VARCHAR(100)  NOT NULL,
    precio        DECIMAL(10,2) NOT NULL,
    unidad        VARCHAR(30)   NOT NULL,
    estatus       VARCHAR(20)   NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT pk_servicio           PRIMARY KEY (id_servicio),
    CONSTRAINT fk_servicio_categoria FOREIGN KEY (id_categoria) REFERENCES categoria_servicio (id_categoria),
    CONSTRAINT fk_servicio_proveedor FOREIGN KEY (id_proveedor) REFERENCES proveedor (id_proveedor),
    CONSTRAINT ck_servicio_precio    CHECK (precio >= 0),
    CONSTRAINT ck_servicio_estatus   CHECK (estatus IN ('ACTIVO', 'INACTIVO'))
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- DISPONIBILIDAD: cupo de cada servicio (lugares, habitaciones, cupos)
-- Se descuenta cuando el responsable CONFIRMA una reservacion
-- y se devuelve si el cliente cancela una reservacion confirmada.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS disponibilidad (
    id_disponibilidad INT NOT NULL AUTO_INCREMENT,
    id_servicio       INT NOT NULL,
    cupo_total        INT NOT NULL,
    cupo_disponible   INT NOT NULL,
    CONSTRAINT pk_disponibilidad          PRIMARY KEY (id_disponibilidad),
    CONSTRAINT uq_disponibilidad_servicio UNIQUE (id_servicio),
    CONSTRAINT fk_disponibilidad_servicio FOREIGN KEY (id_servicio) REFERENCES servicio (id_servicio),
    CONSTRAINT ck_disponibilidad_cupo     CHECK (cupo_disponible >= 0 AND cupo_disponible <= cupo_total)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- AGENTE: datos del agente de viajes (diagrama E-R)
-- id_usuario: su cuenta de acceso con rol AGENTE (1 a 1)
-- Baja logica: estatus INACTIVO (nunca se borra la fila)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS agente (
    id_agente   INT          NOT NULL AUTO_INCREMENT,
    id_usuario  INT          NOT NULL,
    nombre      VARCHAR(100) NOT NULL,
    correo      VARCHAR(254) NOT NULL,
    telefono    VARCHAR(10)  NULL,
    estatus     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT pk_agente          PRIMARY KEY (id_agente),
    CONSTRAINT uq_agente_usuario  UNIQUE (id_usuario),
    CONSTRAINT uq_agente_correo   UNIQUE (correo),
    CONSTRAINT fk_agente_usuario  FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT ck_agente_estatus  CHECK (estatus IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT ck_agente_telefono CHECK (telefono IS NULL OR telefono REGEXP '^[0-9]{10}$')
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- SOLICITUD_VIAJE: lo que pide el cliente
-- estatus: NUEVA (espera cotizacion) -> COTIZADA (hay una cotizacion enviada)
--          -> ACEPTADA (el cliente acepto). Si el cliente rechaza la cotizacion
--          o el responsable rechaza la reservacion, vuelve a NUEVA.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS solicitud_viaje (
    id_solicitud         INT           NOT NULL AUTO_INCREMENT,
    id_usuario           INT           NOT NULL COMMENT 'Cliente que pide el viaje',
    id_agente            INT           NULL     COMMENT 'Agente que la cotizo',
    destino              VARCHAR(100)  NOT NULL,
    fecha_inicio         DATE          NOT NULL,
    fecha_fin            DATE          NOT NULL,
    num_personas         INT           NOT NULL,
    presupuesto          DECIMAL(10,2) NULL,
    necesidades          VARCHAR(500)  NULL,
    estatus              VARCHAR(20)   NOT NULL DEFAULT 'NUEVA',
    fecha_creacion       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                                       ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_solicitud_viaje    PRIMARY KEY (id_solicitud),
    CONSTRAINT fk_solicitud_usuario  FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_solicitud_agente   FOREIGN KEY (id_agente)  REFERENCES agente (id_agente),
    CONSTRAINT ck_solicitud_fechas   CHECK (fecha_fin >= fecha_inicio),
    CONSTRAINT ck_solicitud_personas CHECK (num_personas BETWEEN 1 AND 50),
    CONSTRAINT ck_solicitud_estatus  CHECK (estatus IN ('NUEVA', 'COTIZADA', 'ACEPTADA'))
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- COTIZACION: propuesta del agente para una solicitud
-- estatus: ENVIADA -> ACEPTADA o RECHAZADA (lo decide el cliente)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cotizacion (
    id_cotizacion    INT           NOT NULL AUTO_INCREMENT,
    id_solicitud     INT           NOT NULL,
    id_agente        INT           NOT NULL,
    total            DECIMAL(12,2) NOT NULL,
    notas            VARCHAR(500)  NULL,
    estatus          VARCHAR(20)   NOT NULL DEFAULT 'ENVIADA',
    fecha_creacion   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_respuesta  DATETIME      NULL,
    CONSTRAINT pk_cotizacion           PRIMARY KEY (id_cotizacion),
    CONSTRAINT fk_cotizacion_solicitud FOREIGN KEY (id_solicitud) REFERENCES solicitud_viaje (id_solicitud),
    CONSTRAINT fk_cotizacion_agente    FOREIGN KEY (id_agente)    REFERENCES agente (id_agente),
    CONSTRAINT ck_cotizacion_estatus   CHECK (estatus IN ('ENVIADA', 'ACEPTADA', 'RECHAZADA'))
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- DETALLE_COTIZACION: cada servicio de la cotizacion.
-- precio_unitario guarda el precio DEL MOMENTO: si despues cambia el precio
-- del servicio, la cotizacion no se altera (trazabilidad comercial).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS detalle_cotizacion (
    id_detalle       INT           NOT NULL AUTO_INCREMENT,
    id_cotizacion    INT           NOT NULL,
    id_servicio      INT           NOT NULL,
    cantidad         INT           NOT NULL,
    precio_unitario  DECIMAL(10,2) NOT NULL,
    subtotal         DECIMAL(12,2) NOT NULL,
    CONSTRAINT pk_detalle_cotizacion PRIMARY KEY (id_detalle),
    CONSTRAINT fk_detalle_cotizacion FOREIGN KEY (id_cotizacion) REFERENCES cotizacion (id_cotizacion),
    CONSTRAINT fk_detalle_servicio   FOREIGN KEY (id_servicio)   REFERENCES servicio (id_servicio),
    CONSTRAINT uq_detalle_servicio   UNIQUE (id_cotizacion, id_servicio),
    CONSTRAINT ck_detalle_cantidad   CHECK (cantidad > 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- RESERVACION: nace cuando el cliente acepta una cotizacion.
-- estatus: POR_VALIDAR -> CONFIRMADA (el responsable valida disponibilidad)
--                      -> RECHAZADA  (no hay disponibilidad)
--          POR_VALIDAR o CONFIRMADA -> CANCELADA (el cliente, con 3 dias
--          de anticipacion; si estaba CONFIRMADA se registra un cargo del 5%)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reservacion (
    id_reservacion     INT           NOT NULL AUTO_INCREMENT,
    folio              VARCHAR(20)   NOT NULL,
    id_cotizacion      INT           NOT NULL,
    id_usuario         INT           NOT NULL COMMENT 'Cliente',
    id_responsable     INT           NULL     COMMENT 'Usuario que valido o rechazo',
    total              DECIMAL(12,2) NOT NULL,
    estatus            VARCHAR(20)   NOT NULL DEFAULT 'POR_VALIDAR',
    motivo_rechazo     VARCHAR(255)  NULL,
    cargo_cancelacion  DECIMAL(12,2) NULL,
    fecha_creacion     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_validacion   DATETIME      NULL,
    fecha_cancelacion  DATETIME      NULL,
    CONSTRAINT pk_reservacion             PRIMARY KEY (id_reservacion),
    CONSTRAINT uq_reservacion_folio       UNIQUE (folio),
    CONSTRAINT uq_reservacion_cotizacion  UNIQUE (id_cotizacion),
    CONSTRAINT fk_reservacion_cotizacion  FOREIGN KEY (id_cotizacion)  REFERENCES cotizacion (id_cotizacion),
    CONSTRAINT fk_reservacion_cliente     FOREIGN KEY (id_usuario)     REFERENCES usuario (id_usuario),
    CONSTRAINT fk_reservacion_responsable FOREIGN KEY (id_responsable) REFERENCES usuario (id_usuario),
    CONSTRAINT ck_reservacion_estatus     CHECK (estatus IN ('POR_VALIDAR', 'CONFIRMADA', 'RECHAZADA', 'CANCELADA'))
) ENGINE = InnoDB;

-- Comprobacion: deben aparecer las 12 tablas (3 de cuentas + 9 de negocio)
SHOW TABLES;
