import java.sql.*;
import java.util.List;

public class VentaDAO {

    public static class ItemVenta {
        public String codigo;
        public String descripcion;
        public double precio;
        public int cantidad;
        public double importe;

        public ItemVenta(String codigo, String descripcion, double precio, int cantidad, double importe) {
            this.codigo = codigo;
            this.descripcion = descripcion;
            this.precio = precio;
            this.cantidad = cantidad;
            this.importe = importe;
        }
    }

    public static boolean registrarVenta(double subtotal, double iva, double total, List<ItemVenta> items) throws Exception {
        Connection conn = null;
        PreparedStatement psVenta = null;
        PreparedStatement psDetalle = null;
        PreparedStatement psStockCheck = null;
        PreparedStatement psStockUpdate = null;
        ResultSet rsKeys = null;

        String sqlVenta = "INSERT INTO ventas (subtotal, iva, total) VALUES (?, ?, ?)";
        String sqlDetalle = "INSERT INTO detalles_venta (id_venta, codigo_producto, cantidad, precio_unitario, importe) VALUES (?, ?, ?, ?, ?)";
        String sqlStockCheck = "SELECT stock FROM productos WHERE codigo_barras = ? FOR UPDATE";
        String sqlStockUpdate = "UPDATE productos SET stock = stock - ? WHERE codigo_barras = ?";

        try {
            conn = Conexiones.getConexion();
            // INICIO DE LA TRANSACCIÓN
            conn.setAutoCommit(false);

            // 1. Insertar la venta
            psVenta = conn.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS);
            psVenta.setDouble(1, subtotal);
            psVenta.setDouble(2, iva);
            psVenta.setDouble(3, total);
            psVenta.executeUpdate();

            rsKeys = psVenta.getGeneratedKeys();
            int idVentaGenerado = 0;
            if (rsKeys.next()) {
                idVentaGenerado = rsKeys.getInt(1);
            } else {
                throw new SQLException("No se pudo obtener el ID de la venta generada.");
            }

            // Preparar statements para detalles y stock
            psStockCheck = conn.prepareStatement(sqlStockCheck);
            psStockUpdate = conn.prepareStatement(sqlStockUpdate);
            psDetalle = conn.prepareStatement(sqlDetalle);

            for (ItemVenta item : items) {
                // Verificar stock disponible antes de restar
                psStockCheck.setString(1, item.codigo);
                ResultSet rsStock = psStockCheck.executeQuery();
                if (rsStock.next()) {
                    int stockActual = rsStock.getInt("stock");
                    if (stockActual < item.cantidad) {
                        throw new Exception("Stock insuficiente para: " + item.descripcion + " (Disponible: " + stockActual + ")");
                    }
                } else {
                    throw new Exception("Producto no encontrado en inventario: " + item.codigo);
                }
                rsStock.close();

                // 2. Actualizar stock del producto
                psStockUpdate.setInt(1, item.cantidad);
                psStockUpdate.setString(2, item.codigo);
                psStockUpdate.executeUpdate();

                // 3. Insertar detalle de venta
                psDetalle.setInt(1, idVentaGenerado);
                psDetalle.setString(2, item.codigo);
                psDetalle.setInt(3, item.cantidad);
                psDetalle.setDouble(4, item.precio);
                psDetalle.setDouble(5, item.importe);
                psDetalle.executeUpdate();
            }

            // Si todo salió bien, confirmamos la transacción
            conn.commit();
            return true;

        } catch (Exception e) {
            // Si ocurre cualquier error, deshacer todas las operaciones
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e; // Relanzar excepción para mostrarla en la interfaz
        } finally {
            if (rsKeys != null) rsKeys.close();
            if (psVenta != null) psVenta.close();
            if (psDetalle != null) psDetalle.close();
            if (psStockCheck != null) psStockCheck.close();
            if (psStockUpdate != null) psStockUpdate.close();
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }
}