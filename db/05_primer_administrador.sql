-- =====================================================================
-- GERONEX - Convertir una cuenta en el PRIMER ADMINISTRADOR
--
-- El registro publico siempre crea clientes (por seguridad). El primer
-- administrador se crea asi:
--   1. Registrate normal desde la web (por ejemplo con el usuario "admin").
--   2. Cambia 'admin' abajo por ese nombre de usuario y ejecuta este script.
--   3. Cierra sesion en la web y vuelve a entrar (el rol va dentro del token).
-- Despues, ese administrador crea las cuentas de agentes y responsables
-- desde la pagina admin.html.
-- =====================================================================

USE geronex;

UPDATE usuario
SET id_rol = (SELECT id_rol FROM (SELECT id_rol FROM rol WHERE nombre = 'ADMINISTRADOR') AS r)
WHERE nombre_usuario = 'admin';

-- Comprobacion: debe salir tu usuario con rol ADMINISTRADOR
SELECT u.id_usuario, u.nombre_usuario, r.nombre AS rol, u.estatus
FROM usuario u JOIN rol r ON r.id_rol = u.id_rol
ORDER BY u.id_usuario;
