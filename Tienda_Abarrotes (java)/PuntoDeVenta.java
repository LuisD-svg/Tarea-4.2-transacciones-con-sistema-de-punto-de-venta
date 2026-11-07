import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class PuntoDeVenta extends JFrame {

    private JTextField txtCodigo;
    private JTable tablaProductos;
    private DefaultTableModel modeloTabla;
    private JLabel lblSubtotalValor;
    private JLabel lblIvaValor;
    private JLabel lblTotalValor;

    private static final double TASA_IVA = 0.16; // 16% de IVA

    public PuntoDeVenta() {
        setTitle("Sistema Punto de Venta - Tienda de Abarrotes");
        setSize(850, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // --- Panel Superior: Escaneo de Código de Barras ---
        JPanel panelNorte = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        panelNorte.setBackground(new Color(245, 245, 245));
        
        JLabel lblCodigo = new JLabel("Código de Barras:");
        lblCodigo.setFont(new Font("Arial", Font.BOLD, 14));
        txtCodigo = new JTextField(20);
        txtCodigo.setFont(new Font("Arial", Font.PLAIN, 14));

        panelNorte.add(lblCodigo);
        panelNorte.add(txtCodigo);
        add(panelNorte, BorderLayout.NORTH);

        // --- Panel Central: Tabla (GRID) ---
        String[] columnas = {"CLAVE", "PRODUCTO", "PRECIO", "CANTIDAD", "IMPORTE"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Solo lectura en la tabla directa
            }
        };
        tablaProductos = new JTable(modeloTabla);
        tablaProductos.setRowHeight(25);
        tablaProductos.setFont(new Font("Arial", Font.PLAIN, 13));
        tablaProductos.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));

        JScrollPane scrollTabla = new JScrollPane(tablaProductos);
        add(scrollTabla, BorderLayout.CENTER);

        // --- Panel Inferior:  ---
        JPanel panelSur = new JPanel(new BorderLayout(10, 10));
        panelSur.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        JButton btnGuardar = new JButton("GUARDAR VENTA");
        btnGuardar.setFont(new Font("Arial", Font.BOLD, 16));
        btnGuardar.setBackground(new Color(46, 125, 50));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setFocusPainted(false);

        JButton btnEliminar = new JButton("Eliminar Fila");
        btnEliminar.setFont(new Font("Arial", Font.PLAIN, 12));

        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelAcciones.add(btnGuardar);
        panelAcciones.add(btnEliminar);

        JPanel panelTotales = new JPanel(new GridLayout(3, 2, 10, 5));
        JLabel lblSubtotal = new JLabel("SUBTOTAL: $", SwingConstants.RIGHT);
        lblSubtotalValor = new JLabel("0.00", SwingConstants.LEFT);
        JLabel lblIva = new JLabel("IVA (16%): $", SwingConstants.RIGHT);
        lblIvaValor = new JLabel("0.00", SwingConstants.LEFT);
        JLabel lblTotal = new JLabel("TOTAL: $", SwingConstants.RIGHT);
        lblTotalValor = new JLabel("0.00", SwingConstants.LEFT);

        Font fuenteTotales = new Font("Arial", Font.BOLD, 14);
        lblSubtotal.setFont(fuenteTotales);
        lblSubtotalValor.setFont(fuenteTotales);
        lblIva.setFont(fuenteTotales);
        lblIvaValor.setFont(fuenteTotales);
        
        lblTotal.setFont(new Font("Arial", Font.BOLD, 18));
        lblTotalValor.setFont(new Font("Arial", Font.BOLD, 18));
        lblTotalValor.setForeground(new Color(183, 28, 28));

        panelTotales.add(lblSubtotal);
        panelTotales.add(lblSubtotalValor);
        panelTotales.add(lblIva);
        panelTotales.add(lblIvaValor);
        panelTotales.add(lblTotal);
        panelTotales.add(lblTotalValor);

        panelSur.add(panelAcciones, BorderLayout.WEST);
        panelSur.add(panelTotales, BorderLayout.EAST);
        add(panelSur, BorderLayout.SOUTH);

        // Escanear código (al presionar Enter en el JTextField)
        txtCodigo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String codigo = txtCodigo.getText().trim();
                if (!codigo.isEmpty()) {
                    procesarCodigoBarras(codigo);
                    txtCodigo.setText("");
                }
            }
        });

        // Eliminar fila seleccionada
        btnEliminar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int filaSeleccionada = tablaProductos.getSelectedRow();
                if (filaSeleccionada != -1) {
                    modeloTabla.removeRow(filaSeleccionada);
                    recalcularTotales();
                } else {
                    JOptionPane.showMessageDialog(PuntoDeVenta.this, "Selecciona una fila de la tabla para eliminar.");
                }
            }
        });

        // Guardar venta (Transacción)
        btnGuardar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                guardarVentaTransaccional();
            }
        });
    }

    private void procesarCodigoBarras(String codigo) {
        // Verificar si el producto ya está en la tabla para incrementar cantidad
        for (int i = 0; i < modeloTabla.getRowCount(); i++) {
            String codigoFila = modeloTabla.getValueAt(i, 0).toString();
            if (codigoFila.equals(codigo)) {
                int cantidadActual = Integer.parseInt(modeloTabla.getValueAt(i, 3).toString());
                double precio = Double.parseDouble(modeloTabla.getValueAt(i, 2).toString());
                int nuevaCantidad = cantidadActual + 1;
                double nuevoImporte = nuevaCantidad * precio;

                modeloTabla.setValueAt(nuevaCantidad, i, 3);
                modeloTabla.setValueAt(String.format("%.2f", nuevoImporte), i, 4);
                recalcularTotales();
                return;
            }
        }

        // Si no está en la tabla, lo consulta en la base
        try (Connection conn = Conexiones.getConexion();
             PreparedStatement ps = conn.prepareStatement("SELECT descripcion, precio, stock FROM productos WHERE codigo_barras = ?")) {

            ps.setString(1, codigo);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String descripcion = rs.getString("descripcion");
                double precio = rs.getDouble("precio");
                int stock = rs.getInt("stock");

                if (stock <= 0) {
                    JOptionPane.showMessageDialog(this, "El producto '" + descripcion + "' está agotado.", "Sin Stock", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                int cantidadInicial = 1;
                double importe = precio * cantidadInicial;

                modeloTabla.addRow(new Object[]{
                    codigo,
                    descripcion,
                    String.format("%.2f", precio),
                    cantidadInicial,
                    String.format("%.2f", importe)
                });

                recalcularTotales();
            } else {
                JOptionPane.showMessageDialog(this, "Producto no registrado con código: " + codigo, "No encontrado", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al buscar producto: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void recalcularTotales() {
        double sumaTotal = 0.0;
        for (int i = 0; i < modeloTabla.getRowCount(); i++) {
            double importe = Double.parseDouble(modeloTabla.getValueAt(i, 4).toString().replace(",", "."));
            sumaTotal += importe;
        }

        // Desglose de Subtotal e IVA según el estándar comercial
        double subtotal = sumaTotal / (1.0 + TASA_IVA);
        double iva = sumaTotal - subtotal;

        lblSubtotalValor.setText(String.format("%.2f", subtotal));
        lblIvaValor.setText(String.format("%.2f", iva));
        lblTotalValor.setText(String.format("%.2f", sumaTotal));
    }

    private void guardarVentaTransaccional() {
        if (modeloTabla.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay productos en la venta actual.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double subtotal = Double.parseDouble(lblSubtotalValor.getText().replace(",", "."));
        double iva = Double.parseDouble(lblIvaValor.getText().replace(",", "."));
        double total = Double.parseDouble(lblTotalValor.getText().replace(",", "."));

        List<VentaDAO.ItemVenta> items = new ArrayList<>();
        for (int i = 0; i < modeloTabla.getRowCount(); i++) {
            String codigo = modeloTabla.getValueAt(i, 0).toString();
            String descripcion = modeloTabla.getValueAt(i, 1).toString();
            double precio = Double.parseDouble(modeloTabla.getValueAt(i, 2).toString().replace(",", "."));
            int cantidad = Integer.parseInt(modeloTabla.getValueAt(i, 3).toString());
            double importe = Double.parseDouble(modeloTabla.getValueAt(i, 4).toString().replace(",", "."));

            items.add(new VentaDAO.ItemVenta(codigo, descripcion, precio, cantidad, importe));
        }

        try {
            boolean exitoso = VentaDAO.registrarVenta(subtotal, iva, total, items);
            if (exitoso) {
                JOptionPane.showMessageDialog(this, "¡Venta y transacción completadas con éxito!", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                modeloTabla.setRowCount(0);
                recalcularTotales();
                txtCodigo.requestFocus();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Transacción revertida (Rollback). Razón:\n" + ex.getMessage(), "Error en Transacción", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new PuntoDeVenta().setVisible(true);
            }
        });
    }
}