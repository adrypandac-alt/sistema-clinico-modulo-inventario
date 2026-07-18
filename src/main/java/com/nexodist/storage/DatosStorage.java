package com.nexodist.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.nexodist.model.Movimiento;
import com.nexodist.model.Pedido;
import com.nexodist.model.PedidoItem;
import com.nexodist.model.Producto;
import com.nexodist.model.Rol;
import com.nexodist.model.Usuario;
import com.nexodist.model.Venta;
import com.nexodist.model.VentaItem;
import com.nexodist.util.DataProtectionUtil;
import com.nexodist.util.PasswordUtil;

/**
 * En esta clase concentro el acceso JDBC y la inicialización del esquema MySQL.
 *
 * Cumplo la función de gateway de persistencia de la aplicación monolítica y
 * me comunico con modelos, servicios y listener de arranque. Si esta clase
 * falla, el sistema conserva parte del estado en memoria, pero no puede
 * garantizar persistencia ni trazabilidad después de reiniciar.
 */
public class DatosStorage {

    public static List<Rol> cargarRoles() {
        List<Rol> roles = new ArrayList<>();
        asegurarColumnasRol();
        String sql = "SELECT nombre, paneles_permitidos, puede_interactuar FROM rol WHERE activo = TRUE ORDER BY id_rol";
        try (Connection cn = abrirConexion(); PreparedStatement ps = cn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) roles.add(new Rol(rs.getString(1), rs.getString(2), rs.getBoolean(3)));
        } catch (SQLException e) { registrarError("cargar roles", e); }
        return roles;
    }

    public static boolean guardarRol(String original, Rol rol) {
        asegurarColumnasRol();
        String sql = original == null || original.isBlank()
                ? "INSERT INTO rol (nombre, descripcion, paneles_permitidos, puede_interactuar, activo) VALUES (?, ?, ?, ?, TRUE)"
                : "UPDATE rol SET nombre=?, descripcion=?, paneles_permitidos=?, puede_interactuar=? WHERE nombre=?";
        try (Connection cn = abrirConexion(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, rol.getNombre()); ps.setString(2, "Rol " + rol.getNombre());
            ps.setString(3, rol.getPanelesPermitidos()); ps.setBoolean(4, rol.isPuedeInteractuar());
            if (original != null && !original.isBlank()) ps.setString(5, original);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { registrarError("guardar rol", e); return false; }
    }

    public static boolean eliminarRol(String nombre) {
        try (Connection cn = abrirConexion(); PreparedStatement ps = cn.prepareStatement(
                "DELETE FROM rol WHERE nombre=? AND nombre <> 'Administrador' AND NOT EXISTS (SELECT 1 FROM usuario WHERE usuario.id_rol=rol.id_rol)")) {
            ps.setString(1, nombre); return ps.executeUpdate() > 0;
        } catch (SQLException e) { registrarError("eliminar rol", e); return false; }
    }

    private static void asegurarColumnasRol() {
        try (Connection cn = abrirConexion()) {
            asegurarColumna(cn, "rol", "paneles_permitidos", "VARCHAR(500) NULL");
            asegurarColumna(cn, "rol", "puede_interactuar", "BOOLEAN NOT NULL DEFAULT TRUE");
            try (Statement st = cn.createStatement()) {
                st.executeUpdate("UPDATE rol SET paneles_permitidos='dashboard,productos,proveedor,movimientos,alertas,usuarios,despachos' WHERE nombre='Administrador'");
                st.executeUpdate("UPDATE rol SET paneles_permitidos='dashboard,productos,movimientos,alertas,despachos' WHERE nombre='Operativo'");
                st.executeUpdate("UPDATE rol SET paneles_permitidos='dashboard', puede_interactuar=FALSE WHERE nombre='Visualizador' AND (paneles_permitidos IS NULL OR paneles_permitidos='')");
            }
        } catch (SQLException e) { registrarError("actualizar permisos de rol", e); }
    }

    private static void asegurarColumna(Connection cn, String tabla, String columna, String definicion) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND COLUMN_NAME=?")) {
            ps.setString(1, tabla); ps.setString(2, columna);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); if (rs.getInt(1) == 0) try (Statement st = cn.createStatement()) { st.executeUpdate("ALTER TABLE " + tabla + " ADD COLUMN " + columna + " " + definicion); } }
        }
    }

    private static final String DB_URL_DEFAULT = "jdbc:mysql://localhost:3306/ase_proyecto_integrador"
            + "?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USER_DEFAULT = "root";
    private static final String DB_PASSWORD_DEFAULT = "";
    private static volatile boolean baseDatosDisponible = true;
    private static volatile boolean avisoModoMemoria = false;

    public static void inicializarDirectorio() {
        inicializarBaseDatos();
    }

    public static String getRutaDatos() {
        if (!baseDatosDisponible) {
            return "Memoria local (MySQL no disponible)";
        }
        return obtenerUrl();
    }

    public static boolean verificarConexion() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection cn = DriverManager.getConnection(obtenerUrl(), obtenerUsuario(), obtenerClave())) {
                boolean disponible = cn.isValid(2);
                baseDatosDisponible = disponible;
                if (disponible) avisoModoMemoria = false;
                return disponible;
            }
        } catch (Exception e) {
            baseDatosDisponible = false;
            return false;
        }
    }

    public static void guardarProductos(List<Producto> productos) {
        String sql = "INSERT INTO producto (id_producto, id_categoria, codigo, id_proveedor, nombre, descripcion, "
                + "precio_compra, precio_venta, stock_minimo, stock_actual, activo) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE) "
                + "ON DUPLICATE KEY UPDATE id_categoria = VALUES(id_categoria), id_proveedor = VALUES(id_proveedor), "
                + "nombre = VALUES(nombre), descripcion = VALUES(descripcion), precio_venta = VALUES(precio_venta), "
                + "stock_minimo = VALUES(stock_minimo), stock_actual = VALUES(stock_actual), activo = TRUE";
        String sqlLote = "INSERT INTO lote_producto (id_producto, numero_lote, fecha_vencimiento, cantidad) "
                + "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE fecha_vencimiento = VALUES(fecha_vencimiento), "
                + "cantidad = VALUES(cantidad)";
        try (Connection cn = abrirConexion();
             PreparedStatement ps = cn.prepareStatement(sql);
             PreparedStatement psLote = cn.prepareStatement(sqlLote)) {
            for (Producto p : productos) {
                int categoriaId = asegurarCategoria(cn, p.getCategoria());
                int proveedorId = asegurarProveedor(cn, p.getProveedor());
                ps.setInt(1, p.getId());
                ps.setInt(2, categoriaId);
                ps.setString(3, p.getSku());
                ps.setInt(4, proveedorId);
                ps.setString(5, p.getNombre());
                ps.setString(6, p.getRegistroSanitario());
                ps.setDouble(7, p.getPrecio());
                ps.setDouble(8, p.getPrecio());
                ps.setInt(9, p.getStockMinimo());
                ps.setInt(10, p.getStock());
                ps.executeUpdate();

                psLote.setInt(1, p.getId());
                psLote.setString(2, valorPorDefecto(p.getLote(), "SIN-LOTE"));
                if (p.getFechaCaducidad() == null || p.getFechaCaducidad().isBlank()) {
                    psLote.setNull(3, java.sql.Types.DATE);
                } else {
                    psLote.setString(3, p.getFechaCaducidad());
                }
                psLote.setInt(4, p.getStock());
                psLote.executeUpdate();
            }
        } catch (SQLException e) {
            registrarError("guardar productos", e);
        }
    }

    public static List<Producto> cargarProductos() {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT p.id_producto, p.codigo, p.nombre, c.nombre AS categoria, pr.nombre AS proveedor, "
                + "p.precio_venta, p.stock_actual, p.stock_minimo, p.descripcion, "
                + "COALESCE(l.numero_lote, 'SIN-LOTE') AS lote, COALESCE(CAST(l.fecha_vencimiento AS CHAR), '') AS fecha_vencimiento "
                + "FROM producto p "
                + "JOIN categoria_producto c ON c.id_categoria = p.id_categoria "
                + "JOIN proveedor pr ON pr.id_proveedor = p.id_proveedor "
                + "LEFT JOIN lote_producto l ON l.id_lote = (SELECT lp.id_lote FROM lote_producto lp "
                + "WHERE lp.id_producto = p.id_producto ORDER BY lp.fecha_ingreso ASC, lp.id_lote ASC LIMIT 1) "
                + "WHERE p.activo = TRUE ORDER BY p.id_producto";
        try (Connection cn = abrirConexion();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new Producto(
                        rs.getInt("id_producto"),
                        rs.getString("codigo"),
                        rs.getString("nombre"),
                        rs.getString("categoria"),
                        rs.getString("proveedor"),
                        rs.getDouble("precio_venta"),
                        rs.getInt("stock_actual"),
                        rs.getInt("stock_minimo"),
                        "Bodega principal",
                        rs.getString("lote"),
                        rs.getString("fecha_vencimiento"),
                        valorPorDefecto(rs.getString("descripcion"), "No registrado")));
            }
        } catch (SQLException e) {
            registrarError("cargar productos", e);
        }
        return lista;
    }

    public static void guardarUsuarios(List<Usuario> usuarios) {
        asegurarColumnaPanelesUsuario();
        asegurarColumnaTelefonoUsuario();
        String sql = "INSERT INTO usuario (email, id_rol, password, nombre, activo, paneles_permitidos, telefono) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE id_rol = VALUES(id_rol), "
                + "password = VALUES(password), nombre = VALUES(nombre), activo = VALUES(activo), "
                + "paneles_permitidos = VALUES(paneles_permitidos), telefono=VALUES(telefono)";
        try (Connection cn = abrirConexion(); PreparedStatement ps = cn.prepareStatement(sql)) {
            for (Usuario u : usuarios) {
                int rolId = asegurarRol(cn, normalizarRolParaDb(u.getRol()));
                ps.setString(1, DataProtectionUtil.proteger(u.getCorreo().trim().toLowerCase()));
                ps.setInt(2, rolId);
                ps.setString(3, PasswordUtil.hashearSiNecesario(u.getClave()));
                ps.setString(4, DataProtectionUtil.proteger(u.getNombre().trim()));
                ps.setBoolean(5, u.isActivo());
                ps.setString(6, u.getPanelesPermitidos());
                ps.setString(7, DataProtectionUtil.proteger(u.getTelefono()));
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            registrarError("guardar usuarios", e);
        }
    }

    public static List<Usuario> cargarUsuarios() {
        List<Usuario> lista = new ArrayList<>();
        asegurarColumnaPanelesUsuario();
        asegurarColumnaTelefonoUsuario();
        migrarUsuariosCifrados();
        String sql = "SELECT u.nombre, u.email, u.password, r.nombre AS rol, u.activo, u.fecha_creacion, u.paneles_permitidos, u.telefono "
                + "FROM usuario u JOIN rol r ON r.id_rol = u.id_rol ORDER BY u.nombre";
        try (Connection cn = abrirConexion();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String rol = normalizarRolParaApp(rs.getString("rol"));
                Usuario cargado = new Usuario(
                        DataProtectionUtil.revelar(rs.getString("nombre")),
                        DataProtectionUtil.revelar(rs.getString("email")),
                        rs.getString("password"),
                        rol,
                        areaPorRol(rol),
                        rs.getBoolean("activo"),
                        rs.getTimestamp("fecha_creacion") == null ? "Sin accesos" : "Registrado",
                        rs.getString("paneles_permitidos"));
                cargado.setTelefono(DataProtectionUtil.revelar(rs.getString("telefono")));
                lista.add(cargado);
            }
        } catch (SQLException e) {
            registrarError("cargar usuarios", e);
        }
        return lista;
    }

    /** Migra en el mismo registro los datos personales legados en texto plano. */
    private static void migrarUsuariosCifrados() {
        String consulta = "SELECT id_usuario, email, nombre, telefono FROM usuario";
        String actualizacion = "UPDATE usuario SET email=?, nombre=?, telefono=? WHERE id_usuario=?";
        try (Connection cn = abrirConexion();
             PreparedStatement leer = cn.prepareStatement(consulta);
             ResultSet rs = leer.executeQuery();
             PreparedStatement guardar = cn.prepareStatement(actualizacion)) {
            while (rs.next()) {
                String email = rs.getString("email");
                String nombre = rs.getString("nombre");
                String telefono = rs.getString("telefono");
                if ((email != null && !email.startsWith("ENC1:"))
                        || (nombre != null && !nombre.startsWith("ENC1:"))
                        || (telefono != null && !telefono.isBlank() && !telefono.startsWith("ENC1:"))) {
                    guardar.setString(1, DataProtectionUtil.proteger(email == null ? "" : email.trim().toLowerCase()));
                    guardar.setString(2, DataProtectionUtil.proteger(nombre));
                    guardar.setString(3, DataProtectionUtil.proteger(telefono));
                    guardar.setInt(4, rs.getInt("id_usuario"));
                    guardar.executeUpdate();
                }
            }
        } catch (SQLException e) { registrarError("migrar cifrado de usuarios", e); }
    }

    public static void guardarMovimientos(List<Movimiento> movimientos) {
        String sqlMov = "INSERT INTO movimiento (id_movimiento, id_usuario, tipo_movimiento, fecha, observaciones) "
                + "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE id_usuario = VALUES(id_usuario), "
                + "tipo_movimiento = VALUES(tipo_movimiento), fecha = VALUES(fecha), observaciones = VALUES(observaciones)";
        String sqlDetalle = "INSERT INTO movimiento_detalle (id_producto, cantidad, precio_unitario, id_movimiento) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection cn = abrirConexion();
             PreparedStatement psMov = cn.prepareStatement(sqlMov);
             PreparedStatement psDetalle = cn.prepareStatement(sqlDetalle);
             PreparedStatement limpiar = cn.prepareStatement("DELETE FROM movimiento_detalle WHERE id_movimiento = ?")) {
            for (Movimiento m : movimientos) {
                psMov.setInt(1, m.getId());
                psMov.setInt(2, buscarUsuarioId(cn, m.getUsuario()));
                psMov.setString(3, normalizarTipoMovimiento(m.getTipo()));
                psMov.setTimestamp(4, timestampSeguro(m.getFecha()));
                psMov.setString(5, serializarObservacion(m));
                psMov.executeUpdate();

                limpiar.setInt(1, m.getId());
                limpiar.executeUpdate();
                if (existeProducto(cn, m.getProductoId())) {
                    psDetalle.setInt(1, m.getProductoId());
                    psDetalle.setInt(2, Math.max(1, m.getCantidad()));
                    psDetalle.setDouble(3, precioProducto(cn, m.getProductoId()));
                    psDetalle.setInt(4, m.getId());
                    psDetalle.executeUpdate();
                }
            }
        } catch (SQLException e) {
            registrarError("guardar movimientos", e);
        }
    }

    public static List<Movimiento> cargarMovimientos() {
        List<Movimiento> lista = new ArrayList<>();
        String sql = "SELECT m.id_movimiento, m.tipo_movimiento, m.fecha, m.observaciones, u.nombre AS usuario, "
                + "d.id_producto, d.cantidad, p.nombre AS producto "
                + "FROM movimiento m "
                + "JOIN usuario u ON u.id_usuario = m.id_usuario "
                + "LEFT JOIN movimiento_detalle d ON d.id_movimiento = m.id_movimiento "
                + "LEFT JOIN producto p ON p.id_producto = d.id_producto "
                + "ORDER BY m.id_movimiento";
        try (Connection cn = abrirConexion();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String obs = valorPorDefecto(rs.getString("observaciones"), "");
                lista.add(new Movimiento(
                        rs.getInt("id_movimiento"),
                        rs.getInt("id_producto"),
                        valorPorDefecto(rs.getString("producto"), "Producto no especificado"),
                        rs.getString("tipo_movimiento"),
                        rs.getInt("cantidad"),
                        extraerEntero(obs, "stockAntes"),
                        extraerEntero(obs, "stockDespues"),
                        extraerTexto(obs, "motivo"),
                        rs.getString("usuario"),
                        rs.getTimestamp("fecha") == null ? "" : rs.getTimestamp("fecha").toString()));
            }
        } catch (SQLException e) {
            registrarError("cargar movimientos", e);
        }
        return lista;
    }

    public static void guardarVentas(List<Venta> ventas) {
        String sqlVenta = "INSERT INTO venta (id_venta, id_cliente, fecha, total, estado, id_usuario) "
                + "VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE id_cliente = VALUES(id_cliente), "
                + "fecha = VALUES(fecha), total = VALUES(total), estado = VALUES(estado), id_usuario = VALUES(id_usuario)";
        String sqlDetalle = "INSERT INTO venta_detalle (id_producto, cantidad, precio_unitario, subtotal, id_venta) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection cn = abrirConexion();
             PreparedStatement psVenta = cn.prepareStatement(sqlVenta);
             PreparedStatement psDetalle = cn.prepareStatement(sqlDetalle);
             PreparedStatement limpiar = cn.prepareStatement("DELETE FROM venta_detalle WHERE id_venta = ?")) {
            for (Venta v : ventas) {
                int clienteId = asegurarCliente(cn, v);
                psVenta.setInt(1, v.getId());
                psVenta.setInt(2, clienteId);
                psVenta.setTimestamp(3, timestampSeguro(v.getFecha()));
                psVenta.setDouble(4, v.getTotal());
                psVenta.setString(5, v.isFacturada() ? "PAGADA" : "PENDIENTE");
                psVenta.setInt(6, buscarUsuarioId(cn, v.getVendedorCorreo()));
                psVenta.executeUpdate();

                limpiar.setInt(1, v.getId());
                limpiar.executeUpdate();
                for (VentaItem item : v.getItems()) {
                    psDetalle.setInt(1, item.getProductoId());
                    psDetalle.setInt(2, item.getCantidad());
                    psDetalle.setDouble(3, item.getPrecioUnitario());
                    psDetalle.setDouble(4, item.getSubtotal());
                    psDetalle.setInt(5, v.getId());
                    psDetalle.executeUpdate();
                }
            }
        } catch (SQLException e) {
            registrarError("guardar ventas", e);
        }
    }

    public static List<Venta> cargarVentas() {
        List<Venta> lista = new ArrayList<>();
        String sql = "SELECT v.id_venta, v.fecha, v.total, v.estado, c.nombre AS cliente_nombre, "
                + "c.documento, c.telefono, c.email AS cliente_email, c.direccion, "
                + "u.nombre AS vendedor_nombre, u.email AS vendedor_email "
                + "FROM venta v JOIN cliente c ON c.id_cliente = v.id_cliente "
                + "JOIN usuario u ON u.id_usuario = v.id_usuario ORDER BY v.id_venta";
        try (Connection cn = abrirConexion();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Venta v = new Venta();
                v.setId(rs.getInt("id_venta"));
                v.setVendedorNombre(rs.getString("vendedor_nombre"));
                v.setVendedorCorreo(rs.getString("vendedor_email"));
                v.setClienteNombre(rs.getString("cliente_nombre"));
                v.setClienteRuc(rs.getString("documento"));
                v.setClienteTelefono(rs.getString("telefono"));
                v.setClienteCorreo(rs.getString("cliente_email"));
                v.setClienteDireccion(rs.getString("direccion"));
                v.setTotal(rs.getDouble("total"));
                v.setFecha(rs.getTimestamp("fecha") == null ? "" : rs.getTimestamp("fecha").toString());
                v.setFacturada("PAGADA".equalsIgnoreCase(rs.getString("estado")));
                v.setItems(cargarItemsVenta(cn, v.getId()));
                lista.add(v);
            }
        } catch (SQLException e) {
            registrarError("cargar ventas", e);
        }
        return lista;
    }

    private static List<VentaItem> cargarItemsVenta(Connection cn, int ventaId) throws SQLException {
        List<VentaItem> items = new ArrayList<>();
        String sql = "SELECT d.id_producto, p.codigo, p.nombre, d.cantidad, d.precio_unitario "
                + "FROM venta_detalle d JOIN producto p ON p.id_producto = d.id_producto "
                + "WHERE d.id_venta = ? ORDER BY d.id_venta_detalle";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, ventaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new VentaItem(
                            rs.getInt("id_producto"),
                            rs.getString("codigo"),
                            rs.getString("nombre"),
                            rs.getInt("cantidad"),
                            rs.getDouble("precio_unitario")));
                }
            }
        }
        return items;
    }

    public static void guardarPedidos(List<Pedido> pedidos) {
        asegurarColumnasDespacho();
        String sqlPedido = "INSERT INTO pedido (id_pedido, id_usuario, rol_usuario, fecha, estado, clinica, solicitante, despachado_por, entregado_a) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE id_usuario=VALUES(id_usuario), rol_usuario=VALUES(rol_usuario), fecha=VALUES(fecha), estado=VALUES(estado), clinica=VALUES(clinica), solicitante=VALUES(solicitante), despachado_por=VALUES(despachado_por), entregado_a=VALUES(entregado_a)";
        String sqlDetalle = "INSERT INTO pedido_detalle "
                + "(id_pedido, id_producto, sku, nombre_producto, proveedor, cantidad, observacion) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection cn = abrirConexion();
             PreparedStatement psPedido = cn.prepareStatement(sqlPedido);
             PreparedStatement psDetalle = cn.prepareStatement(sqlDetalle);
             PreparedStatement limpiar = cn.prepareStatement("DELETE FROM pedido_detalle WHERE id_pedido = ?")) {
            for (Pedido pedido : pedidos) {
                psPedido.setInt(1, pedido.getId());
                psPedido.setInt(2, buscarUsuarioId(cn, pedido.getUsuario()));
                psPedido.setString(3, pedido.getRol());
                psPedido.setTimestamp(4, timestampSeguro(pedido.getFecha()));
                psPedido.setString(5, normalizarEstadoPedido(pedido.getEstado()));
                psPedido.setString(6, pedido.getClinica());
                psPedido.setString(7, pedido.getSolicitante());
                psPedido.setString(8, pedido.getDespachadoPor());
                psPedido.setString(9, pedido.getEntregadoA());
                psPedido.executeUpdate();

                limpiar.setInt(1, pedido.getId());
                limpiar.executeUpdate();
                for (PedidoItem item : pedido.getItems()) {
                    if (!existeProducto(cn, item.getProductoId())) continue;
                    psDetalle.setInt(1, pedido.getId());
                    psDetalle.setInt(2, item.getProductoId());
                    psDetalle.setString(3, item.getSku());
                    psDetalle.setString(4, item.getNombre());
                    psDetalle.setString(5, item.getProveedor());
                    psDetalle.setInt(6, item.getCantidad());
                    psDetalle.setString(7, item.getObservacion());
                    psDetalle.executeUpdate();
                }
            }
        } catch (SQLException e) {
            registrarError("guardar pedidos", e);
        }
    }

    public static List<Pedido> cargarPedidos() {
        asegurarColumnasDespacho();
        List<Pedido> lista = new ArrayList<>();
        String sql = "SELECT p.id_pedido, p.fecha, p.estado, p.rol_usuario, p.clinica, p.solicitante, p.despachado_por, p.entregado_a, u.nombre AS usuario "
                + "FROM pedido p JOIN usuario u ON u.id_usuario = p.id_usuario "
                + "ORDER BY p.id_pedido DESC";
        try (Connection cn = abrirConexion();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Pedido pedido = new Pedido();
                pedido.setId(rs.getInt("id_pedido"));
                pedido.setUsuario(rs.getString("usuario"));
                pedido.setRol(valorPorDefecto(rs.getString("rol_usuario"), "Inventario"));
                pedido.setFecha(rs.getTimestamp("fecha") == null ? "" : rs.getTimestamp("fecha").toString());
                pedido.setEstado(estadoPedidoParaApp(rs.getString("estado")));
                pedido.setClinica(rs.getString("clinica"));
                pedido.setSolicitante(rs.getString("solicitante"));
                pedido.setDespachadoPor(rs.getString("despachado_por"));
                pedido.setEntregadoA(rs.getString("entregado_a"));
                pedido.setItems(cargarItemsPedido(cn, pedido.getId()));
                lista.add(pedido);
            }
        } catch (SQLException e) {
            registrarError("cargar pedidos", e);
        }
        return lista;
    }

    private static void asegurarColumnasDespacho() {
        try (Connection cn = abrirConexion()) {
            asegurarColumna(cn, "pedido", "clinica", "VARCHAR(150) NULL");
            asegurarColumna(cn, "pedido", "solicitante", "VARCHAR(120) NULL");
            asegurarColumna(cn, "pedido", "despachado_por", "VARCHAR(120) NULL");
            asegurarColumna(cn, "pedido", "entregado_a", "VARCHAR(120) NULL");
        } catch (SQLException e) { registrarError("actualizar pedidos para despacho", e); }
    }

    private static List<PedidoItem> cargarItemsPedido(Connection cn, int pedidoId) throws SQLException {
        List<PedidoItem> items = new ArrayList<>();
        String sql = "SELECT d.id_producto, COALESCE(d.sku, p.codigo) AS sku, "
                + "COALESCE(d.nombre_producto, p.nombre) AS nombre_producto, "
                + "COALESCE(d.proveedor, pr.nombre) AS proveedor, d.cantidad, d.observacion "
                + "FROM pedido_detalle d "
                + "JOIN producto p ON p.id_producto = d.id_producto "
                + "JOIN proveedor pr ON pr.id_proveedor = p.id_proveedor "
                + "WHERE d.id_pedido = ? ORDER BY d.id_pedido_detalle";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, pedidoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PedidoItem item = new PedidoItem();
                    item.setProductoId(rs.getInt("id_producto"));
                    item.setSku(rs.getString("sku"));
                    item.setNombre(rs.getString("nombre_producto"));
                    item.setProveedor(rs.getString("proveedor"));
                    item.setCantidad(rs.getInt("cantidad"));
                    item.setObservacion(rs.getString("observacion"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    private static void inicializarBaseDatos() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("[DatosStorage] Driver MySQL no disponible: " + e.getMessage());
        }
        try (Connection cn = abrirConexion(); Statement st = cn.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS rol (id_rol INT AUTO_INCREMENT PRIMARY KEY, nombre VARCHAR(80) NOT NULL UNIQUE, descripcion VARCHAR(200), activo BOOLEAN NOT NULL DEFAULT TRUE, fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB");
            // Tablas de la antigua pantalla de módulos: se eliminan también en
            // instalaciones existentes para mantener el esquema actualizado.
            st.executeUpdate("DROP TABLE IF EXISTS rol_modulo");
            st.executeUpdate("DROP TABLE IF EXISTS modulo");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS usuario (id_usuario INT AUTO_INCREMENT PRIMARY KEY, email VARCHAR(512) NOT NULL UNIQUE, id_rol INT NOT NULL, password VARCHAR(255) NOT NULL, nombre VARCHAR(512) NOT NULL, activo BOOLEAN NOT NULL DEFAULT TRUE, fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (id_rol) REFERENCES rol(id_rol) ON DELETE RESTRICT) ENGINE=InnoDB");
            st.executeUpdate("ALTER TABLE usuario MODIFY email VARCHAR(512) NOT NULL, MODIFY nombre VARCHAR(512) NOT NULL, MODIFY password VARCHAR(255) NOT NULL, MODIFY telefono VARCHAR(255) NULL");
            asegurarColumnaPanelesUsuario(cn);
            st.executeUpdate("CREATE TABLE IF NOT EXISTS proveedor (id_proveedor INT AUTO_INCREMENT PRIMARY KEY, nombre VARCHAR(120) NOT NULL, contacto VARCHAR(100), telefono VARCHAR(20), email VARCHAR(120), direccion VARCHAR(200), activo BOOLEAN NOT NULL DEFAULT TRUE) ENGINE=InnoDB");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS categoria_producto (id_categoria INT AUTO_INCREMENT PRIMARY KEY, nombre VARCHAR(100) NOT NULL UNIQUE, descripcion VARCHAR(200), activo BOOLEAN NOT NULL DEFAULT TRUE) ENGINE=InnoDB");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS producto (id_producto INT AUTO_INCREMENT PRIMARY KEY, id_categoria INT NOT NULL, codigo VARCHAR(50) NOT NULL UNIQUE, id_proveedor INT NOT NULL, nombre VARCHAR(120) NOT NULL, descripcion VARCHAR(250), precio_compra DECIMAL(10,2) NOT NULL DEFAULT 0.00, precio_venta DECIMAL(10,2) NOT NULL DEFAULT 0.00, stock_minimo INT NOT NULL DEFAULT 0, stock_actual INT NOT NULL DEFAULT 0, activo BOOLEAN NOT NULL DEFAULT TRUE, FOREIGN KEY (id_categoria) REFERENCES categoria_producto(id_categoria) ON DELETE RESTRICT, FOREIGN KEY (id_proveedor) REFERENCES proveedor(id_proveedor) ON DELETE RESTRICT) ENGINE=InnoDB");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS movimiento (id_movimiento INT AUTO_INCREMENT PRIMARY KEY, id_usuario INT NOT NULL, tipo_movimiento VARCHAR(30) NOT NULL, fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, observaciones VARCHAR(250), FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE RESTRICT) ENGINE=InnoDB");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS movimiento_detalle (id_movimiento_detalle INT AUTO_INCREMENT PRIMARY KEY, id_producto INT NOT NULL, cantidad INT NOT NULL, precio_unitario DECIMAL(10,2) NOT NULL DEFAULT 0.00, id_movimiento INT NOT NULL, FOREIGN KEY (id_producto) REFERENCES producto(id_producto) ON DELETE RESTRICT, FOREIGN KEY (id_movimiento) REFERENCES movimiento(id_movimiento) ON DELETE CASCADE) ENGINE=InnoDB");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS lote_producto (id_lote INT AUTO_INCREMENT PRIMARY KEY, id_producto INT NOT NULL, numero_lote VARCHAR(60) NOT NULL, fecha_fabricacion DATE, fecha_vencimiento DATE, cantidad INT NOT NULL DEFAULT 0, id_movimiento_detalle INT, UNIQUE (id_producto, numero_lote), FOREIGN KEY (id_producto) REFERENCES producto(id_producto) ON DELETE RESTRICT, FOREIGN KEY (id_movimiento_detalle) REFERENCES movimiento_detalle(id_movimiento_detalle) ON DELETE SET NULL) ENGINE=InnoDB");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS pedido (id_pedido INT AUTO_INCREMENT PRIMARY KEY, id_usuario INT NOT NULL, rol_usuario VARCHAR(80), fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, estado VARCHAR(30) NOT NULL DEFAULT 'CONFIRMADO', FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE RESTRICT) ENGINE=InnoDB");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS pedido_detalle (id_pedido_detalle INT AUTO_INCREMENT PRIMARY KEY, id_pedido INT NOT NULL, id_producto INT NOT NULL, sku VARCHAR(50), nombre_producto VARCHAR(120), proveedor VARCHAR(120), cantidad INT NOT NULL, observacion VARCHAR(250), FOREIGN KEY (id_pedido) REFERENCES pedido(id_pedido) ON DELETE CASCADE, FOREIGN KEY (id_producto) REFERENCES producto(id_producto) ON DELETE RESTRICT) ENGINE=InnoDB");
            insertarDatosBase(cn);
        } catch (SQLException e) {
            registrarError("inicializar base de datos", e);
        }
    }

    private static void insertarDatosBase(Connection cn) throws SQLException {
        asegurarRol(cn, "Administrador");
        asegurarRol(cn, "Operativo");
        asegurarCategoria(cn, "Medicamentos");
        asegurarCategoria(cn, "Insumos médicos");
        asegurarCategoria(cn, "Cuidado personal");
        asegurarProveedor(cn, "Proveedor General");
        insertarUsuarioSiNoExiste(cn, "admin@ase.com", "admin123", "Administrador ASE", "Administrador");
    }

    private static Connection abrirConexion() throws SQLException {
        if (!baseDatosDisponible) {
            throw new SQLException("Base de datos no disponible; usando memoria local");
        }
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL no disponible", e);
        }
        return DriverManager.getConnection(obtenerUrl(), obtenerUsuario(), obtenerClave());
    }

    /**
     * Versión de acceso de paquete para uso interno en storage (SeguridadStorage).
     * Misma lógica que abrirConexion() pero accesible desde el mismo paquete.
     */
    static Connection abrirConexionPublica() throws SQLException {
        return abrirConexion();
    }

    private static void asegurarColumnaPanelesUsuario() {
        try (Connection cn = abrirConexion()) {
            asegurarColumnaPanelesUsuario(cn);
        } catch (SQLException e) {
            registrarError("asegurar paneles de usuario", e);
        }
    }

    private static void asegurarColumnaTelefonoUsuario() {
        try (Connection cn = abrirConexion()) { asegurarColumna(cn, "usuario", "telefono", "VARCHAR(25) NULL"); }
        catch (SQLException e) { registrarError("asegurar teléfono de usuario", e); }
    }

    private static void asegurarColumnaPanelesUsuario(Connection cn) throws SQLException {
        try (Statement st = cn.createStatement()) {
            st.executeUpdate("ALTER TABLE usuario ADD COLUMN paneles_permitidos VARCHAR(250)");
        } catch (SQLException e) {
            if (!"42S21".equals(e.getSQLState()) && e.getErrorCode() != 1060) {
                throw e;
            }
        }
    }

    private static int asegurarRol(Connection cn, String nombre) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("INSERT IGNORE INTO rol (nombre, descripcion) VALUES (?, ?)")) {
            ps.setString(1, nombre);
            ps.setString(2, "Rol " + nombre);
            ps.executeUpdate();
        }
        return buscarId(cn, "SELECT id_rol FROM rol WHERE nombre = ?", nombre);
    }

    private static int asegurarCategoria(Connection cn, String nombre) throws SQLException {
        String valor = valorPorDefecto(nombre, "Medicamentos");
        try (PreparedStatement ps = cn.prepareStatement("INSERT IGNORE INTO categoria_producto (nombre, descripcion) VALUES (?, ?)")) {
            ps.setString(1, valor);
            ps.setString(2, "Categoria " + valor);
            ps.executeUpdate();
        }
        return buscarId(cn, "SELECT id_categoria FROM categoria_producto WHERE nombre = ?", valor);
    }

    private static int asegurarProveedor(Connection cn, String nombre) throws SQLException {
        String valor = valorPorDefecto(nombre, "Proveedor General");
        try (PreparedStatement ps = cn.prepareStatement("SELECT id_proveedor FROM proveedor WHERE nombre = ? LIMIT 1")) {
            ps.setString(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        try (PreparedStatement ps = cn.prepareStatement("INSERT INTO proveedor (nombre, contacto, telefono, email, direccion) VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, valor);
            ps.setString(2, "Contacto principal");
            ps.setString(3, "0999999999");
            ps.setString(4, "proveedor@example.com");
            ps.setString(5, "Direccion no especificada");
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return buscarId(cn, "SELECT id_proveedor FROM proveedor WHERE nombre = ?", valor);
    }

    private static void insertarUsuarioSiNoExiste(Connection cn, String email, String clave, String nombre, String rol) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("INSERT IGNORE INTO usuario (email, id_rol, password, nombre) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, DataProtectionUtil.proteger(email.trim().toLowerCase()));
            ps.setInt(2, asegurarRol(cn, rol));
            ps.setString(3, PasswordUtil.hashearSiNecesario(clave));
            ps.setString(4, DataProtectionUtil.proteger(nombre));
            ps.executeUpdate();
        }
    }

    private static int asegurarCliente(Connection cn, Venta venta) throws SQLException {
        return asegurarClienteBasico(cn,
                valorPorDefecto(venta.getClienteNombre(), "Consumidor final"),
                valorPorDefecto(venta.getClienteRuc(), "9999999999"),
                venta.getClienteTelefono(),
                venta.getClienteCorreo(),
                venta.getClienteDireccion());
    }

    private static int asegurarClienteBasico(Connection cn, String nombre, String documento, String telefono,
                                             String email, String direccion) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("INSERT INTO cliente (nombre, documento, telefono, email, direccion) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), telefono = VALUES(telefono), email = VALUES(email), direccion = VALUES(direccion)")) {
            ps.setString(1, nombre);
            ps.setString(2, documento);
            ps.setString(3, telefono);
            ps.setString(4, email);
            ps.setString(5, direccion);
            ps.executeUpdate();
        }
        return buscarId(cn, "SELECT id_cliente FROM cliente WHERE documento = ?", documento);
    }

    private static int buscarUsuarioId(Connection cn, String datoUsuario) throws SQLException {
        String dato = datoUsuario == null ? "" : datoUsuario.trim();
        int corte = dato.indexOf(" (");
        if (corte > 0) dato = dato.substring(0, corte);
        try (PreparedStatement ps = cn.prepareStatement("SELECT id_usuario FROM usuario WHERE email = ? OR nombre = ? LIMIT 1")) {
            ps.setString(1, DataProtectionUtil.proteger(dato.toLowerCase()));
            ps.setString(2, DataProtectionUtil.proteger(dato));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        insertarUsuarioSiNoExiste(cn, "admin@ase.com", "admin123", "Administrador ASE", "Administrador");
        return buscarId(cn, "SELECT id_usuario FROM usuario WHERE email = ?",
                DataProtectionUtil.proteger("admin@ase.com"));
    }

    private static boolean existeProducto(Connection cn, int id) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("SELECT 1 FROM producto WHERE id_producto = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static double precioProducto(Connection cn, int id) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("SELECT precio_venta FROM producto WHERE id_producto = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        }
    }

    private static int buscarId(Connection cn, String sql, String valor) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static Timestamp timestampSeguro(String valor) {
        if (valor == null || valor.isBlank()) return new Timestamp(System.currentTimeMillis());
        try {
            return Timestamp.valueOf(valor.length() == 16 ? valor + ":00" : valor);
        } catch (IllegalArgumentException e) {
            return new Timestamp(System.currentTimeMillis());
        }
    }

    private static String serializarObservacion(Movimiento m) {
        return "stockAntes=" + m.getStockAntes()
                + ";stockDespues=" + m.getStockDespues()
                + ";motivo=" + valorPorDefecto(m.getMotivo(), "");
    }

    private static int extraerEntero(String texto, String clave) {
        String valor = extraerTexto(texto, clave);
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String extraerTexto(String texto, String clave) {
        if (texto == null) return "";
        String prefijo = clave + "=";
        int inicio = texto.indexOf(prefijo);
        if (inicio < 0) return texto;
        inicio += prefijo.length();
        int fin = texto.indexOf(';', inicio);
        return fin < 0 ? texto.substring(inicio) : texto.substring(inicio, fin);
    }

    private static String normalizarTipoMovimiento(String tipo) {
        if ("PEDIDO".equalsIgnoreCase(tipo)) return "PEDIDO";
        if ("SALIDA".equalsIgnoreCase(tipo)) return "SALIDA";
        if ("AJUSTE".equalsIgnoreCase(tipo)) return "AJUSTE";
        return "ENTRADA";
    }

    private static String normalizarEstadoPedido(String estado) {
        if ("PENDIENTE".equalsIgnoreCase(estado)) return "PENDIENTE";
        if ("PREPARANDO".equalsIgnoreCase(estado)) return "PREPARANDO";
        if ("DESPACHADO".equalsIgnoreCase(estado)) return "DESPACHADO";
        if ("BORRADOR".equalsIgnoreCase(estado)) return "BORRADOR";
        if ("RECIBIDO".equalsIgnoreCase(estado)) return "RECIBIDO";
        if ("ANULADO".equalsIgnoreCase(estado)) return "ANULADO";
        return "CONFIRMADO";
    }

    private static String estadoPedidoParaApp(String estado) {
        if ("PENDIENTE".equalsIgnoreCase(estado)) return "Pendiente";
        if ("PREPARANDO".equalsIgnoreCase(estado)) return "Preparando";
        if ("DESPACHADO".equalsIgnoreCase(estado)) return "Despachado";
        if ("BORRADOR".equalsIgnoreCase(estado)) return "Borrador";
        if ("RECIBIDO".equalsIgnoreCase(estado)) return "Recibido";
        if ("ANULADO".equalsIgnoreCase(estado)) return "Anulado";
        return "Confirmado";
    }

    private static String normalizarRolParaDb(String rol) {
        if ("Operador".equalsIgnoreCase(rol) || "Bodega".equalsIgnoreCase(rol)) return "Operativo";
        if ("Vendedor".equalsIgnoreCase(rol) || "Ventas".equalsIgnoreCase(rol)) return "Farmacia";
        return valorPorDefecto(rol, "Operativo");
    }

    private static String normalizarRolParaApp(String rol) {
        if ("Bodega".equalsIgnoreCase(rol) || "Operativo".equalsIgnoreCase(rol)) return "Operativo";
        if ("Vendedor".equalsIgnoreCase(rol) || "Ventas".equalsIgnoreCase(rol)
                || "Farmacia".equalsIgnoreCase(rol)) return "Farmacia";
        return valorPorDefecto(rol, "Operativo");
    }

    private static String areaPorRol(String rol) {
        if ("Administrador".equalsIgnoreCase(rol)) return "Administracion";
        if ("Ventas".equalsIgnoreCase(rol) || "Farmacia".equalsIgnoreCase(rol)) return "Farmacia";
        return "Inventario";
    }

    private static String obtenerUrl() {
        String url = valorConfigurado("sistema.db.url", "SISTEMA_DB_URL", DB_URL_DEFAULT);
        if (url.startsWith("jdbc:mysql:") && !url.contains("connectTimeout=")) {
            url += (url.contains("?") ? "&" : "?") + "connectTimeout=1200&socketTimeout=1200";
        }
        return url;
    }

    private static String obtenerUsuario() {
        return valorConfigurado("sistema.db.user", "SISTEMA_DB_USER", DB_USER_DEFAULT);
    }

    private static String obtenerClave() {
        return valorConfigurado("sistema.db.password", "SISTEMA_DB_PASSWORD", DB_PASSWORD_DEFAULT);
    }

    private static String valorConfigurado(String propiedad, String variable, String defecto) {
        String valor = System.getProperty(propiedad);
        if (valor != null && !valor.isBlank()) return valor;
        valor = System.getenv(variable);
        return valor == null || valor.isBlank() ? defecto : valor;
    }

    private static String valorPorDefecto(String valor, String defecto) {
        return valor == null || valor.isBlank() ? defecto : valor;
    }

    private static void registrarError(String operacion, SQLException e) {
        if (esErrorConexion(e)) {
            baseDatosDisponible = false;
            if (!avisoModoMemoria) {
                avisoModoMemoria = true;
                System.err.println("[DatosStorage] MySQL no disponible. La aplicacion continuara en modo memoria local.");
            }
            return;
        }
        if (!baseDatosDisponible) {
            return;
        }
        System.err.println("[DatosStorage] Error al " + operacion + " en MySQL ASE: " + e.getMessage());
    }

    private static boolean esErrorConexion(SQLException e) {
        String estado = e.getSQLState();
        String mensaje = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        return (estado != null && estado.startsWith("08"))
                || mensaje.contains("communications link failure")
                || mensaje.contains("base de datos no disponible")
                || mensaje.contains("connection refused")
                || mensaje.contains("connect timed out");
    }
}
