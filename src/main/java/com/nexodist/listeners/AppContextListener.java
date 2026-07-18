package com.nexodist.listeners;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.nexodist.model.Movimiento;
import com.nexodist.model.Pedido;
import com.nexodist.model.Producto;
import com.nexodist.model.Usuario;
import com.nexodist.model.Venta;
import com.nexodist.storage.DatosStorage;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

@WebListener
public class AppContextListener implements ServletContextListener, HttpSessionListener {

    public static final String ATRIBUTO_VISITAS = "visitas";
    public static final String ATRIBUTO_INICIO = "fechaInicio";
    public static final String ATRIBUTO_PRODUCTOS = "productos";
    public static final String ATRIBUTO_USUARIOS = "usuarios";

    private static final List<Usuario> USUARIOS_DEFAULT = List.of(
        new Usuario("Carlos Mendoza", "admin@sistema-clinico.local", "admin123", "Administrador",
                "Administración", true, "Hoy, 08:32"),
        new Usuario("Adrian", "acarvajal@sistema.com", "adrian1234", "Administrador",
                "Administración", true, "Cuenta permanente"),
        new Usuario("Laura Ríos", "operativo@sistema-clinico.local", "operativo123", "Operativo",
                "Bodega", true, "Hoy, 09:15")
    );

    private static final List<Producto> PRODUCTOS_DEFAULT = List.of(
        new Producto(1, "MED-001", "Paracetamol 500 mg", "Medicamentos", "FarmaAndes S.A.",
                3.80, 142, 80, "FAR-A-01", "PCT-2601", "2027-03-31", "RS-EC-2025-001"),
        new Producto(2, "MED-002", "Amoxicilina 500 mg", "Medicamentos", "Laboratorios Vida",
                8.50, 18, 25, "FAR-A-02", "AMX-2512", "2026-08-15", "RS-EC-2025-014"),
        new Producto(3, "MED-003", "Losartán 50 mg", "Medicamentos", "Medifarma Ecuador",
                6.25, 35, 20, "FAR-A-03", "LST-2602", "2027-01-30", "RS-EC-2024-087"),
        new Producto(4, "MED-004", "Insulina glargina", "Medicamentos", "BioSalud",
                24.90, 9, 15, "REF-02", "INS-2509", "2026-07-10", "RS-EC-2025-122"),
        new Producto(5, "VAC-001", "Vacuna influenza", "Vacunas", "InmunoLab",
                18.00, 28, 20, "REF-01", "VIF-2506", "2026-05-30", "RS-EC-2025-190"),
        new Producto(6, "INS-001", "Jeringa estéril 5 ml", "Insumos médicos", "MediSupply",
                0.35, 210, 100, "BOD-B-01", "JER-2603", "2029-03-31", "DM-EC-2025-211"),
        new Producto(7, "INS-002", "Guantes de nitrilo caja", "Insumos médicos", "SafeHands",
                7.90, 12, 20, "BOD-B-02", "GNT-2601", "2028-12-31", "DM-EC-2025-314"),
        new Producto(8, "INS-003", "Suero fisiológico 500 ml", "Soluciones", "FarmaAndes S.A.",
                2.10, 13, 20, "FAR-C-01", "SUE-2511", "2026-09-05", "RS-EC-2025-240"),
        new Producto(9, "EQU-001", "Tensiómetro digital", "Equipos médicos", "TecnoMed",
                39.50, 22, 15, "BOD-D-01", "TEN-2601", "2031-01-01", "DM-EC-2025-401"),
        new Producto(10, "EQU-002", "Oxímetro de pulso", "Equipos médicos", "TecnoMed",
                22.75, 45, 20, "BOD-D-02", "OXI-2602", "2031-01-01", "DM-EC-2025-402"),
        new Producto(11, "CUR-001", "Gasa estéril 10x10", "Curación", "MediSupply",
                0.45, 5, 20, "BOD-E-01", "GAS-2601", "2028-06-30", "DM-EC-2025-455"),
        new Producto(12, "CUR-002", "Alcohol antiséptico 1 L", "Curación", "BioClean Medical",
                4.20, 38, 25, "BOD-E-02", "ALC-2510", "2026-10-20", "RS-EC-2025-477")
    );

    private static final List<Movimiento> MOVIMIENTOS_DEFAULT = List.of(
        new Movimiento(1, 1, "Paracetamol 500 mg", "ENTRADA", 50, 92, 142,
                "Recepción lote PCT-2601", "Carlos Mendoza", "2026-06-15 14:32"),
        new Movimiento(2, 3, "Losartán 50 mg", "SALIDA", 7, 42, 35,
                "Venta médica #4521", "Ana Torres", "2026-06-16 13:18"),
        new Movimiento(3, 4, "Insulina glargina", "ENTRADA", 5, 4, 9,
                "Ingreso cadena de frío", "Laura Ríos", "2026-06-16 11:45"),
        new Movimiento(4, 6, "Jeringa estéril 5 ml", "ENTRADA", 80, 130, 210,
                "Recepción proveedor", "Laura Ríos", "2026-06-17 16:20"),
        new Movimiento(5, 2, "Amoxicilina 500 mg", "SALIDA", 3, 21, 18,
                "Venta médica #4518", "Ana Torres", "2026-06-17 10:05"),
        new Movimiento(6, 11, "Gasa estéril 10x10", "AJUSTE", 2, 7, 5,
                "Conteo físico de emergencia", "Carlos Mendoza", "2026-06-18 09:12")
    );

    private static List<Venta> createDefaultVentas() {
        List<Venta> lista = new ArrayList<>();
        Venta v1 = new Venta();
        v1.setId(1);
        v1.setVendedorNombre("Farmacia");
        v1.setVendedorCorreo("farmacia@sistema-clinico.local");
        v1.setClienteNombre("Clínica Central S.A.");
        v1.setClienteRuc("1791234567001");
        v1.setClienteTelefono("022123456");
        v1.setClienteCorreo("compras@distcentral.com");
        v1.setClienteDireccion("Av. de los Shyris y Naciones Unidas");
        v1.setFecha("2026-06-08 14:30");
        v1.setFacturada(true);
        v1.getItems().add(new com.nexodist.model.VentaItem(1, "MED-001", "Paracetamol 500 mg", 5, 3.80));
        v1.getItems().add(new com.nexodist.model.VentaItem(2, "MED-002", "Amoxicilina 500 mg", 10, 8.50));
        v1.recalcularTotal();
        lista.add(v1);

        Venta v2 = new Venta();
        v2.setId(2);
        v2.setVendedorNombre("Farmacia");
        v2.setVendedorCorreo("farmacia@sistema-clinico.local");
        v2.setClienteNombre("Consultorio San Gabriel");
        v2.setClienteRuc("1715432198");
        v2.setClienteTelefono("0998765432");
        v2.setClienteCorreo("cruiz@email.com");
        v2.setClienteDireccion("Calle Secundaria 456");
        v2.setFecha("2026-06-09 10:15");
        v2.setFacturada(false);
        v2.getItems().add(new com.nexodist.model.VentaItem(4, "MED-004", "Insulina glargina", 2, 24.90));
        v2.recalcularTotal();
        lista.add(v2);

        return lista;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext contexto = sce.getServletContext();
        Map<String, Integer> visitas = new ConcurrentHashMap<>();
        DatosStorage.inicializarDirectorio();

        List<Usuario> usuarios = DatosStorage.cargarUsuarios();
        usuarios.removeIf(u -> "Farmacia".equalsIgnoreCase(u.getRol()) || "Ventas".equalsIgnoreCase(u.getRol()) || "Vendedor".equalsIgnoreCase(u.getRol()));
        if (usuarios.isEmpty()) {
            usuarios = new ArrayList<>(USUARIOS_DEFAULT);
            DatosStorage.guardarUsuarios(usuarios);
        } else {
            usuarios.removeIf(u -> "viewer@Sistema Clínico.com".equalsIgnoreCase(u.getCorreo())
                    || "ana@Sistema Clínico.com".equalsIgnoreCase(u.getCorreo()));
            for (Usuario u : usuarios) {
                u.setArea(normalizarArea(u.getArea(), u.getRol()));
            }
            asegurarUsuariosBase(usuarios);
            DatosStorage.guardarUsuarios(usuarios);
        }

        List<Producto> productos = DatosStorage.cargarProductos();
        if (productos.isEmpty()) {
            productos = new ArrayList<>(PRODUCTOS_DEFAULT);
            DatosStorage.guardarProductos(productos);
        } else if (esCatalogoAnterior(productos)) {
            productos = new ArrayList<>(PRODUCTOS_DEFAULT);
            DatosStorage.guardarProductos(productos);
        }

        List<Movimiento> movimientos = DatosStorage.cargarMovimientos();
        if (movimientos.isEmpty() || esHistorialAnterior(movimientos)) {
            movimientos = new ArrayList<>(MOVIMIENTOS_DEFAULT);
            DatosStorage.guardarMovimientos(movimientos);
        }

        List<Venta> ventas = new ArrayList<>();

        List<Pedido> pedidos = DatosStorage.cargarPedidos();

        List<String> historiaEventos = new ArrayList<>();
        historiaEventos.add(LocalDateTime.now() + " - Aplicacion iniciada correctamente");
        historiaEventos.add(LocalDateTime.now() + " - Datos cargados desde: " + DatosStorage.getRutaDatos());

        contexto.setAttribute("NombreAplicacion", "Sistema Clínico");
        contexto.setAttribute(ATRIBUTO_INICIO, LocalDateTime.now().toString());
        contexto.setAttribute("usuariosActivos", 0);
        contexto.setAttribute("sesionesActivas", 0);
        contexto.setAttribute("totalPeticiones", 0);
        contexto.setAttribute("totalMovimientos", movimientos.size());
        contexto.setAttribute("historiaEventos", historiaEventos);
        contexto.setAttribute(ATRIBUTO_USUARIOS, usuarios);
        contexto.setAttribute(ATRIBUTO_PRODUCTOS, productos);
        contexto.setAttribute(ATRIBUTO_VISITAS, visitas);
        contexto.setAttribute("movimientos", movimientos);
        contexto.setAttribute("ventas", ventas);
        contexto.setAttribute("pedidos", pedidos);

        System.out.println("-----------------------------");
        System.out.println("Sistema Clinico iniciado");
        System.out.println("Datos en: " + DatosStorage.getRutaDatos());
        System.out.println("Productos: " + productos.size());
        System.out.println("Movimientos: " + movimientos.size());
        System.out.println("-----------------------------");
    }

    private void asegurarUsuariosBase(List<Usuario> usuarios) {
        for (Usuario usuarioDefault : USUARIOS_DEFAULT) {
            boolean existe = usuarios.stream()
                    .anyMatch(u -> u.getCorreo().equalsIgnoreCase(usuarioDefault.getCorreo()));
            if (!existe) {
                usuarios.add(usuarioDefault);
            }
        }
    }

    private String normalizarArea(String area, String rol) {
        if (area == null || area.isBlank()) {
            if ("Farmacia".equalsIgnoreCase(rol) || "Ventas".equalsIgnoreCase(rol)) return "Farmacia";
            return "Operativo".equalsIgnoreCase(rol) ? "Bodega" : "Administración";
        }
        String valor = area.toLowerCase();
        if (valor.contains("farmacia")) return "Farmacia";
        if (valor.contains("admin") || valor.contains("dirección") || valor.contains("direccion")
                || valor.contains("venta") || valor.contains("devol")) {
            return "Administración";
        }
        return "Bodega";
    }

    private boolean esCatalogoAnterior(List<Producto> productos) {
        if (productos == null || productos.isEmpty()) return false;
        for (Producto producto : productos) {
            String sku = producto.getSku() == null ? "" : producto.getSku();
            if (sku.startsWith("ELC-") || sku.startsWith("INF-") || sku.startsWith("OFI-")
                    || sku.startsWith("HER-") || sku.startsWith("LIM-")
                    || producto.getNombre().contains("Router industrial")) {
                return true;
            }
        }
        return false;
    }

    private boolean esHistorialAnterior(List<Movimiento> movimientos) {
        for (Movimiento movimiento : movimientos) {
            String nombre = movimiento.getProductoNombre();
            if (nombre != null && (nombre.contains("Cargador USB") || nombre.contains("Monitor 27")
                    || nombre.contains("Router Wi-Fi"))) {
                return true;
            }
        }
        return false;
    }

    private boolean esVentasAnteriores(List<Venta> ventas) {
        for (Venta venta : ventas) {
            if (venta.getItems() == null) continue;
            for (com.nexodist.model.VentaItem item : venta.getItems()) {
                String sku = item.getSku() == null ? "" : item.getSku();
                if (sku.startsWith("ELC-") || sku.startsWith("INF-")) return true;
            }
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContext contexto = sce.getServletContext();
        List<Producto> productos = (List<Producto>) contexto.getAttribute("productos");
        if (productos != null) {
            DatosStorage.guardarProductos(productos);
        }
        List<Movimiento> movimientos = (List<Movimiento>) contexto.getAttribute("movimientos");
        if (movimientos != null) {
            DatosStorage.guardarMovimientos(movimientos);
        }
        List<Venta> ventas = (List<Venta>) contexto.getAttribute("ventas");
        if (ventas != null) {
            DatosStorage.guardarVentas(ventas);
        }
        List<Pedido> pedidos = (List<Pedido>) contexto.getAttribute("pedidos");
        if (pedidos != null) {
            DatosStorage.guardarPedidos(pedidos);
        }
        agregarEvento(contexto, "Aplicacion finalizada correctamente");
        contexto.removeAttribute(ATRIBUTO_VISITAS);
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        actualizarSesionesActivas(se, 1);
        agregarEvento(se.getSession().getServletContext(), "Sesion iniciada: " + se.getSession().getId());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        actualizarSesionesActivas(se, -1);
        agregarEvento(se.getSession().getServletContext(), "Sesion cerrada: " + se.getSession().getId());
    }

    private void actualizarSesionesActivas(HttpSessionEvent se, int cambio) {
        ServletContext contexto = se.getSession().getServletContext();
        synchronized (contexto) {
            Integer sesionesActivas = (Integer) contexto.getAttribute("sesionesActivas");
            int total = sesionesActivas == null ? 0 : sesionesActivas;
            int nuevoTotal = Math.max(0, total + cambio);
            contexto.setAttribute("sesionesActivas", nuevoTotal);
            contexto.setAttribute("usuariosActivos", nuevoTotal);
        }
    }

    @SuppressWarnings("unchecked")
    private void agregarEvento(ServletContext contexto, String evento) {
        synchronized (contexto) {
            List<String> historiaEventos = (List<String>) contexto.getAttribute("historiaEventos");
            if (historiaEventos == null) {
                historiaEventos = new ArrayList<>();
                contexto.setAttribute("historiaEventos", historiaEventos);
            }
            historiaEventos.add(LocalDateTime.now() + " - " + evento);
        }
    }
}
