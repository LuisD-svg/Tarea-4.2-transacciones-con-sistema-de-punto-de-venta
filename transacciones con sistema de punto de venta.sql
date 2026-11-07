CREATE DATABASE IF NOT EXISTS Tienda_abarrotes CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE Tienda_abarrotes;

-- 1. Tabla de Productos
CREATE TABLE IF NOT EXISTS productos (
    codigo_barras VARCHAR(50) PRIMARY KEY,
    descripcion VARCHAR(150) NOT NULL,
    precio DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

-- 2. Tabla de Ventas
CREATE TABLE IF NOT EXISTS ventas (
    id_venta INT AUTO_INCREMENT PRIMARY KEY,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    subtotal DECIMAL(10,2) NOT NULL,
    iva DECIMAL(10,2) NOT NULL,
    total DECIMAL(10,2) NOT NULL
) ENGINE=InnoDB;

-- 3. Tabla de Detalles de Venta
CREATE TABLE IF NOT EXISTS detalles_venta (
    id_detalle INT AUTO_INCREMENT PRIMARY KEY,
    id_venta INT NOT NULL,
    codigo_producto VARCHAR(50) NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    importe DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (id_venta) REFERENCES ventas(id_venta) ON DELETE CASCADE,
    FOREIGN KEY (codigo_producto) REFERENCES productos(codigo_barras)
) ENGINE=InnoDB;

-- Datos de prueba para escanear
INSERT INTO productos (codigo_barras, descripcion, precio, stock) VALUES
('6928804011128', 'COCA COLA 600ML', 23.00, 50),
('7506129441548', 'DORITOS NACHO 58G', 20.00, 35),
('7702111299894', 'Hojas 100u', 18.50, 40)
ON DUPLICATE KEY UPDATE descripcion=VALUES(descripcion);

SELECT codigo_barras, descripcion, stock FROM productos;

-- Ver Ventas registrada
SELECT * FROM ventas;

-- Ver los detalles de la venta
SELECT * FROM detalles_venta;

-- Ver cómo disminuyó el stock 
SELECT codigo_barras, descripcion, stock FROM productos;