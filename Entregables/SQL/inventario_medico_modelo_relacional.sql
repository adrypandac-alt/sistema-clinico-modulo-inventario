-- ============================================================
-- Proyecto: Inventario Medico
-- Motor: MySQL / MariaDB
-- Modelo corregido desde DBML
-- ============================================================

DROP DATABASE IF EXISTS ase_proyecto_integrador;
CREATE DATABASE ase_proyecto_integrador
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_spanish_ci;

USE ase_proyecto_integrador;

-- ============================================================
-- UBICACION
-- ============================================================

CREATE TABLE pais (
  id_pais INT AUTO_INCREMENT,
  nombre VARCHAR(100) NOT NULL,
  CONSTRAINT pk_pais PRIMARY KEY (id_pais),
  CONSTRAINT uq_pais_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

CREATE TABLE provincia (
  id_provincia INT AUTO_INCREMENT,
  id_pais INT NOT NULL,
  nombre VARCHAR(100) NOT NULL,
  CONSTRAINT pk_provincia PRIMARY KEY (id_provincia),
  CONSTRAINT uq_provincia UNIQUE (id_pais, nombre),
  CONSTRAINT fk_provincia_pais
    FOREIGN KEY (id_pais) REFERENCES pais (id_pais)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE ciudad (
  id_ciudad INT AUTO_INCREMENT,
  id_provincia INT NOT NULL,
  nombre VARCHAR(100) NOT NULL,
  CONSTRAINT pk_ciudad PRIMARY KEY (id_ciudad),
  CONSTRAINT uq_ciudad UNIQUE (id_provincia, nombre),
  CONSTRAINT fk_ciudad_provincia
    FOREIGN KEY (id_provincia) REFERENCES provincia (id_provincia)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ============================================================
-- SEGURIDAD Y USUARIOS
-- ============================================================

CREATE TABLE rol (
  id_rol INT AUTO_INCREMENT,
  nombre VARCHAR(80) NOT NULL,
  descripcion VARCHAR(200),
  paneles_permitidos VARCHAR(250),
  puede_interactuar BOOLEAN NOT NULL DEFAULT TRUE,
  fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT pk_rol PRIMARY KEY (id_rol),
  CONSTRAINT uq_rol_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

CREATE TABLE tipo_usuario (
  id_tipo_usuario INT AUTO_INCREMENT,
  nombre VARCHAR(80) NOT NULL,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT pk_tipo_usuario PRIMARY KEY (id_tipo_usuario),
  CONSTRAINT uq_tipo_usuario_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

CREATE TABLE usuario (
  id_usuario INT AUTO_INCREMENT,
  id_rol INT NOT NULL,
  id_tipo_usuario INT NULL,
  identificacion VARCHAR(20) NULL,
  nombres VARCHAR(120) NULL,
  nombre VARCHAR(120) NOT NULL,
  password VARCHAR(255) NOT NULL,
  email VARCHAR(120) NOT NULL,
  telefono VARCHAR(25),
  paneles_permitidos VARCHAR(250),
  fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
  CONSTRAINT uq_usuario_identificacion UNIQUE (identificacion),
  CONSTRAINT uq_usuario_email UNIQUE (email),
  CONSTRAINT fk_usuario_rol
    FOREIGN KEY (id_rol) REFERENCES rol (id_rol)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,
  CONSTRAINT fk_usuario_tipo_usuario
    FOREIGN KEY (id_tipo_usuario) REFERENCES tipo_usuario (id_tipo_usuario)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ============================================================
-- SEGURIDAD — AUTENTICACION Y SESIONES
-- ============================================================

-- Registra cada intento de login (exitoso o fallido).
-- Permite detectar y bloquear ataques de fuerza bruta por IP o correo.
CREATE TABLE intentos_login (
  id_intento    INT AUTO_INCREMENT,
  correo        VARCHAR(120)         NOT NULL,
  ip            VARCHAR(45)          NOT NULL,
  fecha         TIMESTAMP            NOT NULL DEFAULT CURRENT_TIMESTAMP,
  exitoso       BOOLEAN              NOT NULL DEFAULT FALSE,
  agente        VARCHAR(255),
  CONSTRAINT pk_intentos_login PRIMARY KEY (id_intento),
  INDEX idx_intentos_correo (correo),
  INDEX idx_intentos_ip     (ip),
  INDEX idx_intentos_fecha  (fecha)
) ENGINE=InnoDB COMMENT='Auditoria de intentos de inicio de sesion';

-- Controla las sesiones activas de cada usuario.
-- Permite al administrador invalidar una sesion en tiempo real
-- y detectar accesos simultaneos desde distintas IPs.
CREATE TABLE sesion_usuario (
  id_sesion     VARCHAR(128)         NOT NULL,
  id_usuario    INT                  NOT NULL,
  ip            VARCHAR(45)          NOT NULL,
  agente        VARCHAR(255),
  fecha_inicio  TIMESTAMP            NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_expira  TIMESTAMP            NOT NULL,
  activa        BOOLEAN              NOT NULL DEFAULT TRUE,
  CONSTRAINT pk_sesion_usuario PRIMARY KEY (id_sesion),
  INDEX idx_sesion_usuario  (id_usuario),
  INDEX idx_sesion_activa   (activa, fecha_expira),
  CONSTRAINT fk_sesion_usuario
    FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
    ON UPDATE CASCADE
    ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Sesiones activas de usuarios autenticados';

-- Guarda los ultimos N hashes de contrasena por usuario.
-- Impide que un usuario reutilice contrasenas recientes al cambiarla.
CREATE TABLE historial_password (
  id_historial  INT AUTO_INCREMENT,
  id_usuario    INT                  NOT NULL,
  hash          VARCHAR(255)         NOT NULL,
  fecha         TIMESTAMP            NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_historial_password PRIMARY KEY (id_historial),
  INDEX idx_historial_usuario (id_usuario),
  CONSTRAINT fk_historial_password_usuario
    FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
    ON UPDATE CASCADE
    ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Historial de contrasenas hasheadas para evitar reutilizacion';

-- ============================================================
-- INVENTARIO
-- ============================================================

CREATE TABLE categoria_producto (
  id_categoria INT AUTO_INCREMENT,
  nombre VARCHAR(100) NOT NULL,
  descripcion VARCHAR(200),
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT pk_categoria_producto PRIMARY KEY (id_categoria),
  CONSTRAINT uq_categoria_producto_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

CREATE TABLE producto (
  id_producto INT AUTO_INCREMENT,
  id_categoria INT NOT NULL,
  sku VARCHAR(50) NULL,
  codigo VARCHAR(50) NOT NULL,
  id_proveedor INT NOT NULL,
  nombre VARCHAR(140) NOT NULL,
  descripcion VARCHAR(250),
  precio_compra DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  precio_venta DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  stock_actual INT NOT NULL DEFAULT 0,
  stock_minimo INT NOT NULL DEFAULT 0,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT pk_producto PRIMARY KEY (id_producto),
  CONSTRAINT uq_producto_sku UNIQUE (sku),
  CONSTRAINT uq_producto_codigo UNIQUE (codigo),
  CONSTRAINT chk_producto_precio_venta CHECK (precio_venta >= 0),
  CONSTRAINT chk_producto_stock_actual CHECK (stock_actual >= 0),
  CONSTRAINT chk_producto_stock_minimo CHECK (stock_minimo >= 0),
  CONSTRAINT fk_producto_categoria
    FOREIGN KEY (id_categoria) REFERENCES categoria_producto (id_categoria)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE bodega (
  id_bodega INT AUTO_INCREMENT,
  nombre VARCHAR(100) NOT NULL,
  id_ciudad INT NOT NULL,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT pk_bodega PRIMARY KEY (id_bodega),
  CONSTRAINT uq_bodega_nombre UNIQUE (nombre),
  CONSTRAINT fk_bodega_ciudad
    FOREIGN KEY (id_ciudad) REFERENCES ciudad (id_ciudad)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE tipo_movimiento (
  id_tipo_movimiento INT AUTO_INCREMENT,
  nombre VARCHAR(80) NOT NULL,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT pk_tipo_movimiento PRIMARY KEY (id_tipo_movimiento),
  CONSTRAINT uq_tipo_movimiento_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

-- ============================================================
-- PROVEEDOR Y CONTACTO
-- ============================================================

CREATE TABLE proveedor (
  id_proveedor INT AUTO_INCREMENT,
  ruc VARCHAR(20) NULL,
  nombre VARCHAR(160) NOT NULL,
  razon_social VARCHAR(160),
  contacto VARCHAR(120),
  telefono VARCHAR(30),
  email VARCHAR(120),
  direccion VARCHAR(220),
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT pk_proveedor PRIMARY KEY (id_proveedor),
  CONSTRAINT uq_proveedor_ruc UNIQUE (ruc),
  CONSTRAINT uq_proveedor_email UNIQUE (email)
) ENGINE=InnoDB;

ALTER TABLE producto ADD CONSTRAINT fk_producto_proveedor_directo
  FOREIGN KEY (id_proveedor) REFERENCES proveedor (id_proveedor)
  ON UPDATE CASCADE ON DELETE RESTRICT;

CREATE TABLE proveedor_direccion (
  id_direccion INT AUTO_INCREMENT,
  ruc VARCHAR(20) NOT NULL,
  id_ciudad INT NOT NULL,
  id_tipo_telefono INT,
  direccion VARCHAR(220) NOT NULL,
  telefono VARCHAR(30),
  principal BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT pk_proveedor_direccion PRIMARY KEY (id_direccion),
  CONSTRAINT fk_proveedor_direccion_proveedor
    FOREIGN KEY (ruc) REFERENCES proveedor (ruc)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  CONSTRAINT fk_proveedor_direccion_ciudad
    FOREIGN KEY (id_ciudad) REFERENCES ciudad (id_ciudad)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE tipo_telefono (
  id_tipo_telefono INT AUTO_INCREMENT,
  nombre VARCHAR(80) NOT NULL,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT pk_tipo_telefono PRIMARY KEY (id_tipo_telefono),
  CONSTRAINT uq_tipo_telefono_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

ALTER TABLE proveedor_direccion
  ADD CONSTRAINT fk_proveedor_direccion_tipo_telefono
  FOREIGN KEY (id_tipo_telefono) REFERENCES tipo_telefono (id_tipo_telefono)
  ON UPDATE CASCADE ON DELETE RESTRICT;

CREATE TABLE producto_proveedor (
  id_producto_proveedor INT AUTO_INCREMENT,
  id_producto INT NOT NULL,
  ruc VARCHAR(20) NOT NULL,
  precio_compra DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT pk_producto_proveedor PRIMARY KEY (id_producto_proveedor),
  CONSTRAINT uq_producto_proveedor UNIQUE (id_producto, ruc),
  CONSTRAINT chk_producto_proveedor_precio CHECK (precio_compra >= 0),
  CONSTRAINT fk_producto_proveedor_producto
    FOREIGN KEY (id_producto) REFERENCES producto (id_producto)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,
  CONSTRAINT fk_producto_proveedor_proveedor
    FOREIGN KEY (ruc) REFERENCES proveedor (ruc)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ============================================================
-- COMPRAS Y LOTES
-- ============================================================

CREATE TABLE pedido (
  id_pedido INT AUTO_INCREMENT,
  id_usuario INT NOT NULL,
  ruc VARCHAR(20) NULL,
  numero_factura VARCHAR(60),
  rol_usuario VARCHAR(80),
  fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  estado VARCHAR(40),
  clinica VARCHAR(150),
  solicitante VARCHAR(120),
  despachado_por VARCHAR(120),
  entregado_a VARCHAR(120),
  CONSTRAINT pk_pedido PRIMARY KEY (id_pedido),
  CONSTRAINT fk_pedido_usuario
    FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,
  CONSTRAINT fk_pedido_proveedor
    FOREIGN KEY (ruc) REFERENCES proveedor (ruc)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE pedido_detalle (
  id_pedido_detalle INT AUTO_INCREMENT,
  id_pedido INT NOT NULL,
  id_producto INT NOT NULL,
  sku VARCHAR(50),
  nombre_producto VARCHAR(120),
  proveedor VARCHAR(120),
  cantidad INT NOT NULL,
  observacion VARCHAR(250),
  precio_unitario DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  CONSTRAINT pk_pedido_detalle PRIMARY KEY (id_pedido_detalle),
  CONSTRAINT chk_pedido_detalle_cantidad CHECK (cantidad > 0),
  CONSTRAINT chk_pedido_detalle_precio CHECK (precio_unitario >= 0),
  CONSTRAINT fk_pedido_detalle_pedido
    FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  CONSTRAINT fk_pedido_detalle_producto
    FOREIGN KEY (id_producto) REFERENCES producto (id_producto)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE lote_producto (
  id_lote INT AUTO_INCREMENT,
  fecha_ingreso TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  id_producto INT NOT NULL,
  ruc VARCHAR(20) NULL,
  numero_lote VARCHAR(80),
  fecha_caducidad DATE,
  fecha_vencimiento DATE,
  cantidad_lote INT NOT NULL DEFAULT 0,
  cantidad INT NOT NULL DEFAULT 0,
  fecha_fabricacion DATE,
  id_movimiento_detalle INT NULL,
  CONSTRAINT pk_lote_producto PRIMARY KEY (id_lote),
  CONSTRAINT uq_lote_producto UNIQUE (id_producto, numero_lote),
  CONSTRAINT chk_lote_producto_cantidad CHECK (cantidad_lote >= 0),
  CONSTRAINT fk_lote_producto_producto
    FOREIGN KEY (id_producto) REFERENCES producto (id_producto)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,
  CONSTRAINT fk_lote_producto_proveedor
    FOREIGN KEY (ruc) REFERENCES proveedor (ruc)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ============================================================
-- SALIDAS Y KARDEX
-- ============================================================

CREATE TABLE despacho (
  id_despacho INT AUTO_INCREMENT,
  id_usuario INT NOT NULL,
  fecha_despacho DATE,
  estado VARCHAR(40),
  CONSTRAINT pk_despacho PRIMARY KEY (id_despacho),
  CONSTRAINT fk_despacho_usuario
    FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE despacho_detalle (
  id_despacho_detalle INT AUTO_INCREMENT,
  id_despacho INT NOT NULL,
  id_producto INT NOT NULL,
  id_lote INT NOT NULL,
  cantidad INT NOT NULL,
  CONSTRAINT pk_despacho_detalle PRIMARY KEY (id_despacho_detalle),
  CONSTRAINT chk_despacho_detalle_cantidad CHECK (cantidad > 0),
  CONSTRAINT fk_despacho_detalle_despacho
    FOREIGN KEY (id_despacho) REFERENCES despacho (id_despacho)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  CONSTRAINT fk_despacho_detalle_producto
    FOREIGN KEY (id_producto) REFERENCES producto (id_producto)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,
  CONSTRAINT fk_despacho_detalle_lote
    FOREIGN KEY (id_lote) REFERENCES lote_producto (id_lote)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE movimiento (
  id_movimiento INT AUTO_INCREMENT,
  id_usuario INT NOT NULL,
  id_tipo_movimiento INT NULL,
  tipo_movimiento VARCHAR(30) NOT NULL,
  id_despacho INT NULL,
  fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  estado VARCHAR(40),
  observaciones VARCHAR(250),
  CONSTRAINT pk_movimiento PRIMARY KEY (id_movimiento),
  CONSTRAINT fk_movimiento_usuario
    FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,
  CONSTRAINT fk_movimiento_tipo
    FOREIGN KEY (id_tipo_movimiento) REFERENCES tipo_movimiento (id_tipo_movimiento)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,
  CONSTRAINT fk_movimiento_despacho
    FOREIGN KEY (id_despacho) REFERENCES despacho (id_despacho)
    ON UPDATE CASCADE
    ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE movimiento_detalle (
  id_movimiento_detalle INT AUTO_INCREMENT,
  id_movimiento INT NOT NULL,
  id_producto INT NOT NULL,
  id_lote INT NULL,
  cantidad INT NOT NULL,
  precio_unitario DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  CONSTRAINT pk_movimiento_detalle PRIMARY KEY (id_movimiento_detalle),
  CONSTRAINT chk_movimiento_detalle_cantidad CHECK (cantidad > 0),
  CONSTRAINT fk_movimiento_detalle_movimiento
    FOREIGN KEY (id_movimiento) REFERENCES movimiento (id_movimiento)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  CONSTRAINT fk_movimiento_detalle_producto
    FOREIGN KEY (id_producto) REFERENCES producto (id_producto)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,
  CONSTRAINT fk_movimiento_detalle_lote
    FOREIGN KEY (id_lote) REFERENCES lote_producto (id_lote)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ============================================================
-- DATOS BASE
-- ============================================================

INSERT INTO pais (nombre) VALUES ('Ecuador');
INSERT INTO provincia (id_pais, nombre) VALUES (1, 'Pichincha');
INSERT INTO ciudad (id_provincia, nombre) VALUES (1, 'Quito');

INSERT INTO rol (nombre) VALUES
('Administrador'),
('Operativo'),
('Visualizador');

INSERT INTO tipo_usuario (nombre) VALUES
('Administrativo'),
('Cliente'),
('Proveedor interno'),
('Paciente');

INSERT INTO categoria_producto (nombre) VALUES
('Medicamentos'),
('Vacunas'),
('Insumos medicos'),
('Equipos medicos'),
('Soluciones'),
('Curacion');

INSERT INTO tipo_movimiento (nombre) VALUES
('Ingreso'),
('Salida'),
('Ajuste'),
('Devolucion');

INSERT INTO tipo_telefono (nombre) VALUES
('Celular'),
('Convencional'),
('Whatsapp'),
('Oficina');

INSERT INTO usuario
  (id_rol, id_tipo_usuario, identificacion, nombres, nombre, password, email)
VALUES
  (1, 1, '9999999999', 'Administrador General', 'Administrador General', 'admin123', 'admin@sistema-clinico.local'),
  (2, 1, '0999999998', 'Operativo Inventario', 'Operativo Inventario', 'operativo123', 'operativo@sistema-clinico.local');

INSERT INTO bodega (nombre, id_ciudad) VALUES
('Bodega principal', 1);

INSERT INTO proveedor (ruc, nombre, razon_social, contacto, email, telefono, direccion) VALUES
('1790012345001', 'Distribuidora Medica Andina S.A.', 'Distribuidora Medica Andina S.A.', 'Laura Paredes', 'contacto@medicaandina.local', '022345678', 'Av. Amazonas N34-120'),
('1790098765001', 'Insumos Hospitalarios Quito Cia. Ltda.', 'Insumos Hospitalarios Quito Cia. Ltda.', 'Carlos Mena', 'contacto@insumosquito.local', '0987654321', 'Av. 10 de Agosto N45-80');

INSERT INTO proveedor_direccion
  (ruc, id_ciudad, id_tipo_telefono, direccion, telefono, principal) VALUES
('1790012345001', 1, 4, 'Av. Amazonas N34-120 y Naciones Unidas', '022345678', TRUE),
('1790098765001', 1, 1, 'Av. 10 de Agosto N45-80 y Rio Coca', '0987654321', TRUE);

INSERT INTO producto
  (id_categoria, id_proveedor, sku, codigo, nombre, precio_venta, stock_actual, stock_minimo)
VALUES
  (1, 1, 'MED-PAR-500', 'MED-PAR-500', 'Paracetamol 500 mg', 0.12, 300, 100),
  (3, 2, 'INS-GUA-NIT-M', 'INS-GUA-NIT-M', 'Guantes nitrilo talla M', 8.50, 45, 20),
  (5, 1, 'SOL-NA-09-500', 'SOL-NA-09-500', 'Solucion salina 0.9% 500 ml', 1.75, 80, 30);

INSERT INTO producto_proveedor (id_producto, ruc, precio_compra) VALUES
(1, '1790012345001', 0.08),
(2, '1790098765001', 6.25),
(3, '1790012345001', 1.15);

INSERT INTO lote_producto
  (id_producto, ruc, numero_lote, fecha_caducidad, cantidad_lote)
VALUES
  (1, '1790012345001', 'PAR-2026-01', '2028-01-15', 300),
  (2, '1790098765001', 'GUA-2026-03', '2029-03-10', 45),
  (3, '1790012345001', 'SAL-2026-02', '2027-02-20', 80);

INSERT INTO pedido (id_usuario, ruc, numero_factura, fecha, estado) VALUES
(2, '1790012345001', 'FAC-001-001-000000123', CURRENT_DATE, 'Pendiente');

INSERT INTO pedido_detalle (id_pedido, id_producto, cantidad, precio_unitario) VALUES
(1, 1, 200, 0.08),
(1, 3, 50, 1.15);

-- ============================================================
-- VERIFICACION
-- ============================================================

SELECT 'Base de datos de inventario medico creada correctamente' AS mensaje;
SHOW TABLES;
