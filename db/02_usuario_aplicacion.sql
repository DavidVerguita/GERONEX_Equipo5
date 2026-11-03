-- =====================================================================
-- GERONEX - Usuario de MySQL para el backend (NO usar root en la app)
--
-- ANTES DE EJECUTAR: cambia 'CAMBIA_ESTA_CLAVE' por una clave tuya.
-- No subas este archivo a Git con tu clave real ni la compartas por chat.
--
-- Permisos minimos: solo leer, insertar y actualizar en la base geronex.
-- No puede crear/borrar tablas ni tocar otras bases de datos.
-- =====================================================================

CREATE USER IF NOT EXISTS 'geronex_app'@'localhost' IDENTIFIED BY 'CAMBIA_ESTA_CLAVE';

GRANT SELECT, INSERT, UPDATE ON geronex.* TO 'geronex_app'@'localhost';

-- Comprobar los permisos concedidos:
SHOW GRANTS FOR 'geronex_app'@'localhost';
