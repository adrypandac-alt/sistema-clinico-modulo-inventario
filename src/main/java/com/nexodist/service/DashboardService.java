package com.nexodist.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.nexodist.model.Movimiento;
import com.nexodist.model.Producto;
import com.nexodist.model.Usuario;

import jakarta.servlet.ServletContext;

public final class DashboardService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String[] DIAS = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Hoy"};

    private DashboardService() {
    }

    public static int contarAlertas(List<Producto> productos) {
        if (productos == null) return 0;
        int count = 0;
        for (Producto p : productos) {
            if ((p.getStockMinimo() > 0 && p.getStock() < p.getStockMinimo())
                    || p.isAlertaCaducidad()) {
                count++;
            }
        }
        return count;
    }

    public static int contarCaducados(List<Producto> productos) {
        if (productos == null) return 0;
        int count = 0;
        for (Producto p : productos) if (p.isCaducado()) count++;
        return count;
    }

    public static int contarProximosCaducar(List<Producto> productos) {
        if (productos == null) return 0;
        int count = 0;
        for (Producto p : productos) if (p.isProximoCaducar()) count++;
        return count;
    }

    public static int contarCriticos(List<Producto> productos) {
        int count = 0;
        for (Producto p : productos) {
            if (p.esStockCritico()) count++;
        }
        return count;
    }

    public static int contarBajos(List<Producto> productos) {
        int count = 0;
        for (Producto p : productos) {
            if (p.esStockBajo()) count++;
        }
        return count;
    }

    public static int contarNormales(List<Producto> productos) {
        int count = 0;
        for (Producto p : productos) {
            if (p.getStockMinimo() > 0 && p.getStock() >= p.getStockMinimo()) {
                count++;
            }
        }
        return count;
    }

    public static int totalUnidades(List<Producto> productos) {
        int total = 0;
        for (Producto p : productos) {
            total += p.getStock();
        }
        return total;
    }

    public static double valorTotal(List<Producto> productos) {
        double total = 0;
        for (Producto p : productos) {
            total += p.getPrecio() * p.getStock();
        }
        return total;
    }

    public static Map<String, Integer> stockPorCategoria(List<Producto> productos) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (Producto p : productos) {
            map.merge(p.getCategoria(), p.getStock(), Integer::sum);
        }
        return map;
    }

    public static List<Movimiento> ultimosMovimientos(List<Movimiento> movimientos, int limite) {
        List<Movimiento> copia = new ArrayList<>(movimientos);
        copia.sort((a, b) -> b.getFecha().compareTo(a.getFecha()));
        if (copia.size() > limite) {
            return copia.subList(0, limite);
        }
        return copia;
    }

    public static List<Producto> productosConAlerta(List<Producto> productos) {
        List<Producto> alertas = new ArrayList<>();
        if (productos == null) return alertas;
        for (Producto p : productos) {
            if ((p.getStockMinimo() > 0 && p.getStock() < p.getStockMinimo())
                    || p.isAlertaCaducidad()) {
                alertas.add(p);
            }
        }
        alertas.sort((a, b) -> {
            int prioA = prioridadAlerta(a);
            int prioB = prioridadAlerta(b);
            if (prioA != prioB) return prioA - prioB;
            return Long.compare(a.getDiasParaCaducar(), b.getDiasParaCaducar());
        });
        return alertas;
    }

    private static int prioridadAlerta(Producto producto) {
        if (producto.isCaducado()) return 0;
        if (producto.isProximoCaducar()) return 1;
        if (producto.esStockCritico()) return 2;
        return 3;
    }

    public static List<Map<String, String>> generarRecomendaciones(List<Producto> productos) {
        List<Map<String, String>> recs = new ArrayList<>();

        for (Producto p : productos) {
            if (p.isCaducado()) {
                recs.add(mapRec("critico", p.getNombre(),
                        "Bloquear lote " + p.getLote() + " y retirar del inventario dispensable.",
                        p.getSku()));
            } else if (p.isProximoCaducar()) {
                recs.add(mapRec("bajo", p.getNombre(),
                        "El lote " + p.getLote() + " caduca en " + p.getDiasParaCaducar()
                                + " dias. Mantener alerta de caducidad sin alterar el orden FIFO.", p.getSku()));
            } else if (p.esStockCritico()) {
                int pedido = p.getStockMinimo() * 2 - p.getStock();
                recs.add(mapRec("critico", p.getNombre(),
                        "Pedir " + pedido + " unidades a " + p.getProveedor()
                                + " — stock crítico (" + p.getStock() + "/" + p.getStockMinimo() + " mín.)",
                        p.getSku()));
            } else if (p.esStockBajo()) {
                int pedido = p.getStockMinimo() - p.getStock() + 5;
                recs.add(mapRec("bajo", p.getNombre(),
                        "Reponer " + pedido + " unidades para alcanzar nivel óptimo",
                        p.getSku()));
            }
        }

        Map<String, Integer> porCat = stockPorCategoria(productos);
        String catMax = null;
        int maxStock = 0;
        for (Map.Entry<String, Integer> e : porCat.entrySet()) {
            if (e.getValue() > maxStock) {
                maxStock = e.getValue();
                catMax = e.getKey();
            }
        }
        if (catMax != null && maxStock > 200) {
            recs.add(mapRec("info", "Balance de inventario",
                    "La categoría " + catMax + " concentra " + maxStock
                            + " unidades. Revise rotación para evitar sobre-stock.",
                    "CAT"));
        }

        if (recs.isEmpty()) {
            recs.add(mapRec("ok", "Inventario saludable",
                    "Todos los productos están por encima del stock mínimo. Mantenga el monitoreo activo.",
                    "OK"));
        }

        return recs;
    }

    private static Map<String, String> mapRec(String nivel, String titulo, String texto, String ref) {
        Map<String, String> m = new HashMap<>();
        m.put("nivel", nivel);
        m.put("titulo", titulo);
        m.put("texto", texto);
        m.put("ref", ref);
        return m;
    }

    public static int[] movimientosSemanaEntradas(List<Movimiento> movimientos) {
        return movimientosSemanaPorTipo(movimientos, "ENTRADA");
    }

    public static int[] movimientosSemanaSalidas(List<Movimiento> movimientos) {
        return movimientosSemanaPorTipo(movimientos, "SALIDA");
    }

    private static int[] movimientosSemanaPorTipo(List<Movimiento> movimientos, String tipo) {
        int[] datos = new int[7];
        LocalDate hoy = LocalDate.now();

        for (Movimiento m : movimientos) {
            if (!m.getTipo().equals(tipo)) continue;
            try {
                LocalDateTime dt = LocalDateTime.parse(m.getFecha(), FMT);
                LocalDate fecha = dt.toLocalDate();
                long diff = hoy.toEpochDay() - fecha.toEpochDay();
                if (diff >= 0 && diff < 7) {
                    int idx = 6 - (int) diff;
                    datos[idx] += m.getCantidad();
                }
            } catch (Exception ignored) {
            }
        }
        return datos;
    }

    public static String[] etiquetasSemana() {
        return DIAS;
    }

    public static String fechaHoyFormateada() {
        LocalDate hoy = LocalDate.now();
        String dia = hoy.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
        String mes = hoy.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
        return dia + ", " + hoy.getDayOfMonth() + " de " + mes + " de " + hoy.getYear();
    }

    public static String fechaCorta() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("d/M/yyyy"));
    }

    @SuppressWarnings("unchecked")
    public static Movimiento registrarMovimiento(ServletContext ctx, int productoId, String tipo,
                                                  int cantidad, String motivo, String usuarioNombre) {
        List<Producto> productos = (List<Producto>) ctx.getAttribute("productos");
        List<Movimiento> movimientos = (List<Movimiento>) ctx.getAttribute("movimientos");

        if (productos == null || movimientos == null) return null;

        Producto producto = null;
        for (Producto p : productos) {
            if (p.getId() == productoId) {
                producto = p;
                break;
            }
        }
        if (producto == null) return null;

        int stockAntes = producto.getStock();
        int stockDespues = stockAntes;

        if ("ENTRADA".equals(tipo)) {
            stockDespues = stockAntes + cantidad;
        } else if ("SALIDA".equals(tipo)) {
            if (cantidad > stockAntes) return null;
            stockDespues = stockAntes - cantidad;
        } else if ("AJUSTE".equals(tipo)) {
            stockDespues = cantidad;
            cantidad = Math.abs(stockDespues - stockAntes);
        } else {
            return null;
        }

        producto.setStock(stockDespues);

        int nuevoId = movimientos.size() + 1;
        Movimiento mov = new Movimiento(
                nuevoId, productoId, producto.getNombre(), tipo, cantidad,
                stockAntes, stockDespues, motivo, usuarioNombre,
                LocalDateTime.now().format(FMT)
        );
        movimientos.add(mov);

        Integer totalMov = (Integer) ctx.getAttribute("totalMovimientos");
        ctx.setAttribute("totalMovimientos", totalMov == null ? 1 : totalMov + 1);

        return mov;
    }

    @SuppressWarnings("unchecked")
    public static Movimiento registrarMovimientoInformativo(ServletContext ctx, int productoId, String tipo,
                                                            int cantidad, String motivo, String usuarioNombre) {
        List<Producto> productos = (List<Producto>) ctx.getAttribute("productos");
        List<Movimiento> movimientos = (List<Movimiento>) ctx.getAttribute("movimientos");
        if (productos == null || movimientos == null) return null;

        Producto producto = null;
        for (Producto p : productos) {
            if (p.getId() == productoId) {
                producto = p;
                break;
            }
        }
        if (producto == null) return null;

        int nuevoId = movimientos.size() + 1;
        Movimiento mov = new Movimiento(
                nuevoId, productoId, producto.getNombre(), tipo, cantidad,
                producto.getStock(), producto.getStock(), motivo, usuarioNombre,
                LocalDateTime.now().format(FMT)
        );
        movimientos.add(mov);

        Integer totalMov = (Integer) ctx.getAttribute("totalMovimientos");
        ctx.setAttribute("totalMovimientos", totalMov == null ? 1 : totalMov + 1);
        return mov;
    }

    public static boolean esAdministrador(Usuario usuario) {
        return usuario != null && "Administrador".equalsIgnoreCase(usuario.getRol());
    }

    public static boolean esOperador(Usuario usuario) {
        return usuario != null && ("Operador".equalsIgnoreCase(usuario.getRol())
                || "Operativo".equalsIgnoreCase(usuario.getRol()));
    }

    public static boolean esVendedor(Usuario usuario) {
        return usuario != null && ("Vendedor".equalsIgnoreCase(usuario.getRol())
                || "Ventas".equalsIgnoreCase(usuario.getRol())
                || "Farmacia".equalsIgnoreCase(usuario.getRol()));
    }

    public static boolean esVisualizador(Usuario usuario) {
        return usuario != null && "Visualizador".equalsIgnoreCase(usuario.getRol());
    }

    public static boolean puedeModificarInventario(Usuario usuario) {
        if (tienePanelesPersonalizados(usuario)) {
            return esAdministrador(usuario) || usuario.tienePanelAsignado("productos")
                    || usuario.tienePanelAsignado("movimientos");
        }
        return esAdministrador(usuario) || esOperador(usuario);
    }

    public static boolean puedeRegistrarVentas(Usuario usuario) {
        if (tienePanelesPersonalizados(usuario)) {
            return esAdministrador(usuario) || usuario.tienePanelAsignado("venta")
                    || usuario.tienePanelAsignado("ventas")
                    || usuario.tienePanelAsignado("farmacia");
        }
        return esAdministrador(usuario) || esVendedor(usuario);
    }

    public static boolean puedeGestionarUsuarios(Usuario usuario) {
        if (tienePanelesPersonalizados(usuario)) {
            return esAdministrador(usuario) || usuario.tienePanelAsignado("usuarios");
        }
        return esAdministrador(usuario);
    }

    public static boolean puedeVerPanel(Usuario usuario, String panel) {
        if (usuario == null || panel == null) return false;
        if (esAdministrador(usuario)) return true;
        String clave = panel.toLowerCase(Locale.ROOT);
        if (tienePanelesPersonalizados(usuario)) {
            return usuario.tienePanelAsignado(clave);
        }
        if (esOperador(usuario)) {
            return List.of("dashboard", "productos", "inventario", "entradas", "salidas", "movimientos", "alertas", "despachos")
                    .contains(clave);
        }
        if (esVendedor(usuario)) {
            return List.of("venta", "ventas", "farmacia", "mis-ventas", "alertas-venta", "factura").contains(clave);
        }
        return false;
    }

    private static boolean tienePanelesPersonalizados(Usuario usuario) {
        return usuario != null && usuario.getPanelesPermitidos() != null
                && !usuario.getPanelesPermitidos().isBlank();
    }

    public static String rolVisible(Usuario usuario) {
        if (usuario == null || usuario.getRol() == null) return "";
        if (esOperador(usuario)) return "Operativo";
        if (esVendedor(usuario)) return "Farmacia";
        return usuario.getRol();
    }

    public static String rutaInicio(Usuario usuario) {
        // La pantalla de selección de módulos fue retirada. Cada usuario entra
        // directamente a la primera sección operativa que tiene autorizada.
        if (puedeVerPanel(usuario, "dashboard")) return "panel";
        if (puedeVerPanel(usuario, "productos")) return "inventario";
        if (puedeVerPanel(usuario, "movimientos")) return "movimientos";
        if (puedeVerPanel(usuario, "despachos")) return "pedidos";
        if (puedeVerPanel(usuario, "alertas")) return "alertas";
        if (esVendedor(usuario)) {
            if (puedeVerPanel(usuario, "venta")) return "venta";
            if (puedeVerPanel(usuario, "mis-ventas")) return "mis-ventas";
            if (puedeVerPanel(usuario, "alertas-venta")) return "alertas-venta";
            if (puedeVerPanel(usuario, "factura")) return "factura";
        }
        return "inventario";
    }
}
