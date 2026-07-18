-- =====================================================================
-- SCRIPT DE DIAGNÓSTICO Y CORRECCIÓN CONTROLADA DE TIPOS
-- Motor REAL detectado: MySQL 8 / MariaDB. NO ejecutar en Oracle.
-- No elimino columnas ni convierto datos automáticamente.
-- Primero diagnostico y creo copias dentro de una transacción controlada.
-- =====================================================================

USE ase_proyecto_integrador;

-- 1. DIAGNÓSTICO
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
ORDER BY TABLE_NAME, ORDINAL_POSITION;

SELECT id_producto, codigo, precio_venta, stock_actual, stock_minimo
FROM producto
WHERE precio_venta < 0 OR stock_actual < 0 OR stock_minimo < 0;

SELECT id_movimiento_detalle, cantidad, precio_unitario
FROM movimiento_detalle
WHERE cantidad <= 0 OR precio_unitario < 0;

SELECT id_pedido_detalle, cantidad
FROM pedido_detalle
WHERE cantidad <= 0;

SELECT id_usuario
FROM usuario
WHERE nombre IS NULL OR TRIM(nombre) = '' OR email IS NULL OR TRIM(email) = ''
   OR password IS NULL OR CHAR_LENGTH(password) < 60;

-- 2. COPIA DE SEGURIDAD
-- Cambio el sufijo por la fecha real antes de ejecutar una nueva migración.
CREATE TABLE IF NOT EXISTS respaldo_producto_20260717 LIKE producto;
INSERT IGNORE INTO respaldo_producto_20260717 SELECT * FROM producto;
CREATE TABLE IF NOT EXISTS respaldo_movimiento_detalle_20260717 LIKE movimiento_detalle;
INSERT IGNORE INTO respaldo_movimiento_detalle_20260717 SELECT * FROM movimiento_detalle;
CREATE TABLE IF NOT EXISTS respaldo_pedido_detalle_20260717 LIKE pedido_detalle;
INSERT IGNORE INTO respaldo_pedido_detalle_20260717 SELECT * FROM pedido_detalle;

-- 3. LIMPIEZA DIAGNÓSTICA
-- Reviso estos resultados manualmente. No corrijo importes o cantidades sin
-- conocer el valor válido de negocio.
SELECT id_producto, codigo FROM producto WHERE codigo <> TRIM(codigo);
SELECT id_lote, numero_lote FROM lote_producto WHERE numero_lote <> TRIM(numero_lote);

-- 4. TRANSFORMACIONES PROPUESTAS (NO EJECUTADAS AUTOMÁTICAMENTE)
-- UPDATE producto SET codigo = TRIM(codigo), nombre = TRIM(nombre)
-- WHERE codigo <> TRIM(codigo) OR nombre <> TRIM(nombre);

-- 5. CAMBIOS DE TIPOS PROPUESTOS
-- El SQL ya usa DECIMAL para dinero. La aplicación Java todavía usa double;
-- primero debo migrar los modelos a BigDecimal y sus pruebas.
-- ALTER TABLE producto MODIFY precio_compra DECIMAL(12,2) NOT NULL DEFAULT 0.00;
-- ALTER TABLE producto MODIFY precio_venta  DECIMAL(12,2) NOT NULL DEFAULT 0.00;

-- 6. RESTRICCIONES. Ejecutar una por una solo si los diagnósticos no devuelven filas.
-- ALTER TABLE producto ADD CONSTRAINT chk_producto_precios
--   CHECK (precio_compra >= 0 AND precio_venta >= 0);
-- ALTER TABLE producto ADD CONSTRAINT chk_producto_stock
--   CHECK (stock_actual >= 0 AND stock_minimo >= 0);
-- ALTER TABLE movimiento_detalle ADD CONSTRAINT chk_movimiento_detalle_cantidad
--   CHECK (cantidad > 0 AND precio_unitario >= 0);
-- ALTER TABLE pedido_detalle ADD CONSTRAINT chk_pedido_detalle_cantidad CHECK (cantidad > 0);
-- ALTER TABLE lote_producto ADD CONSTRAINT chk_lote_cantidad CHECK (cantidad >= 0);
-- ALTER TABLE pedido ADD CONSTRAINT chk_pedido_estado
--   CHECK (UPPER(estado) IN ('PENDIENTE','PREPARANDO','DESPACHADO','CANCELADO','CONFIRMADO'));

-- 7. ÍNDICES DIAGNÓSTICOS
SHOW INDEX FROM producto;
SHOW INDEX FROM lote_producto;
SHOW INDEX FROM movimiento;
SHOW INDEX FROM pedido;
-- CREATE INDEX idx_lote_caducidad_cantidad ON lote_producto(fecha_vencimiento, cantidad);

-- 8. VERIFICACIÓN POSTERIOR
SELECT COUNT(*) AS productos_invalidos FROM producto
WHERE precio_venta < 0 OR stock_actual < 0 OR stock_minimo < 0;
SELECT COUNT(*) AS movimientos_invalidos FROM movimiento_detalle WHERE cantidad <= 0;
SELECT COUNT(*) AS pedidos_invalidos FROM pedido_detalle WHERE cantidad <= 0;

-- 9. ROLLBACK / RESTAURACIÓN MANUAL
-- MySQL realiza commit implícito en ALTER TABLE. Por eso conservo las tablas de
-- respaldo y restauro únicamente después de validar claves foráneas y detener
-- la aplicación. No incluyo DROP ni reemplazos destructivos automáticos.
-- Ejemplo controlado (NO EJECUTAR sin autorización):
-- START TRANSACTION;
-- DELETE FROM producto;
-- INSERT INTO producto SELECT * FROM respaldo_producto_20260717;
-- COMMIT;

-- NOTA ORACLE
-- Si se aprueba una migración real a Oracle, se debe crear otro script con
-- VARCHAR2, NUMBER, secuencias/IDENTITY y NUMBER(1) para booleanos. Este script
-- no mezcla sintaxis Oracle porque dañaría el motor actualmente configurado.

