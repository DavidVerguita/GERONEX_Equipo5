-- =====================================================================
-- GERONEX - Datos de ejemplo del catalogo (administrados por el equipo)
-- REQUISITO: haber ejecutado 03_esquema_negocio.sql.
-- Seguro de volver a ejecutar: usa ids fijos con INSERT IGNORE.
-- Los proveedores y precios son ficticios.
-- =====================================================================

USE geronex;

INSERT IGNORE INTO categoria_servicio (id_categoria, nombre, descripcion) VALUES
    (1, 'HOTEL',      'Hospedaje; se cobra por noche'),
    (2, 'VUELO',      'Boletos de avion; se cobra por persona'),
    (3, 'TRANSPORTE', 'Autobuses, trenes y traslados; se cobra por persona o por viaje'),
    (4, 'PAQUETE',    'Paquetes turisticos y tours; se cobra por persona');

INSERT IGNORE INTO proveedor (id_proveedor, nombre, telefono, correo) VALUES
    (1, 'Hoteles Caribe Azul',   '9981234567', 'reservas@caribeazul.example'),
    (2, 'AeroMex Ficticia',      '5512345678', 'ventas@aeromex.example'),
    (3, 'Autobuses Rutas del Sol','5598765432', 'contacto@rutasdelsol.example'),
    (4, 'Tours Raices de Mexico','9511234567', 'hola@raices.example'),
    (5, 'Hoteles Ciudad Viva',   '5543219876', 'reservas@ciudadviva.example');

INSERT IGNORE INTO servicio
    (id_servicio, id_categoria, id_proveedor, nombre, descripcion, destino, precio, unidad) VALUES
    ( 1, 1, 1, 'Hotel Caribe Azul Todo Incluido', 'Habitacion doble frente al mar, alimentos y bebidas incluidos', 'Cancún',      2450.00, 'por noche'),
    ( 2, 1, 1, 'Hotel Laguna Bonita',             'Habitacion estandar con vista a la laguna',                    'Cancún',      1380.00, 'por noche'),
    ( 3, 1, 5, 'Hotel Centro Histórico',          'Habitacion doble a dos cuadras del Zócalo',                    'CDMX',        1150.00, 'por noche'),
    ( 4, 1, 5, 'Hotel Casa Oaxaqueña',            'Hotel boutique con desayuno incluido',                         'Oaxaca',      1290.00, 'por noche'),
    ( 5, 1, 1, 'Resort Arco Dorado',              'Suite con alberca privada',                                    'Los Cabos',   3900.00, 'por noche'),
    ( 6, 2, 2, 'Vuelo CDMX - Cancún',             'Viaje redondo, maleta documentada de 25 kg',                   'Cancún',      3200.00, 'por persona'),
    ( 7, 2, 2, 'Vuelo CDMX - Los Cabos',          'Viaje redondo, maleta documentada de 25 kg',                   'Los Cabos',   3850.00, 'por persona'),
    ( 8, 2, 2, 'Vuelo Guadalajara - Cancún',      'Viaje redondo, equipaje de mano',                              'Cancún',      2900.00, 'por persona'),
    ( 9, 3, 3, 'Autobús de lujo CDMX - Oaxaca',   'Viaje redondo en asiento ejecutivo',                           'Oaxaca',       980.00, 'por persona'),
    (10, 3, 3, 'Traslado aeropuerto - hotel',     'Camioneta privada hasta 6 personas, ida y vuelta',             'Cancún',       850.00, 'por viaje'),
    (11, 3, 3, 'Tren Turístico Chepe Express',    'Recorrido Barrancas del Cobre, clase turista',                 'Chihuahua',   4200.00, 'por persona'),
    (12, 4, 4, 'Tour Chichén Itzá',               'Día completo con guía, comida y cenote',                       'Cancún',      1650.00, 'por persona'),
    (13, 4, 4, 'Tour Gastronómico Oaxaca',        'Mercados, mezcal y clase de cocina',                           'Oaxaca',      1100.00, 'por persona'),
    (14, 4, 4, 'Recorrido Teotihuacán',           'Transporte, guía y entrada a la zona arqueológica',            'CDMX',         890.00, 'por persona');

INSERT IGNORE INTO disponibilidad (id_servicio, cupo_total, cupo_disponible) VALUES
    ( 1, 20, 20), ( 2, 15, 15), ( 3, 12, 12), ( 4,  8,  8), ( 5,  4,  4),
    ( 6, 40, 40), ( 7, 30, 30), ( 8, 30, 30), ( 9, 35, 35), (10, 10, 10),
    (11, 25, 25), (12, 30, 30), (13, 12, 12), (14, 25, 25);

-- Comprobacion
SELECT s.id_servicio, c.nombre AS categoria, s.nombre, s.destino, s.precio, d.cupo_disponible
FROM servicio s
JOIN categoria_servicio c ON c.id_categoria = s.id_categoria
JOIN disponibilidad d     ON d.id_servicio  = s.id_servicio
ORDER BY s.id_servicio;
