import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexiones {
    private static final String URL = "jdbc:mysql://127.0.0.1:3306/Tienda_abarrotes?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";       // Cambia por tu usuario si difiere
    private static final String PASSWORD = "root";       // Cambia por tu contraseña de MySQL

    public static Connection getConexion() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}