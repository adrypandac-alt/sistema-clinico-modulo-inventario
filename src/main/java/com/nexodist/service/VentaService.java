package com.nexodist.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.nexodist.model.Producto;
import com.nexodist.model.Usuario;
import com.nexodist.model.Venta;
import com.nexodist.model.VentaItem;
import com.nexodist.storage.DatosStorage;

import jakarta.servlet.ServletContext;

public final class VentaService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private VentaService() {
    }

    public static double totalVentas(List<Venta> ventas) {
        if (ventas == null) return 0;
        double total = 0;
        for (Venta venta : ventas) total += venta.getTotal();
        return total;
    }

    public static List<Venta> ultimasVentas(List<Venta> ventas, int limite) {
        if (ventas == null) return List.of();
        List<Venta> copia = new ArrayList<>(ventas);
        copia.sort(Comparator.comparingInt(Venta::getId).reversed());
        return copia.size() > limite ? copia.subList(0, limite) : copia;
    }

    @SuppressWarnings("unchecked")
    public static List<Venta> ventasDelVendedor(ServletContext ctx, String correo) {
        List<Venta> ventas = (List<Venta>) ctx.getAttribute("ventas");
        if (ventas == null) return List.of();
        List<Venta> filtradas = new ArrayList<>();
        for (Venta v : ventas) {
            if (v.getVendedorCorreo() != null && v.getVendedorCorreo().equalsIgnoreCase(correo)) {
                filtradas.add(v);
            }
        }
        filtradas.sort(Comparator.comparingInt(Venta::getId).reversed());
        return filtradas;
    }

    @SuppressWarnings("unchecked")
    public static Venta registrarVenta(ServletContext ctx, Usuario vendedor, Venta venta,
                                       List<VentaItem> carrito) throws IllegalStateException {
        if (carrito == null || carrito.isEmpty()) {
            throw new IllegalStateException("Agregue al menos un producto a la venta.");
        }

        List<Producto> productos = (List<Producto>) ctx.getAttribute("productos");
        List<Venta> ventas = (List<Venta>) ctx.getAttribute("ventas");
        if (productos == null || ventas == null) {
            throw new IllegalStateException("Datos de inventario no disponibles.");
        }

        for (VentaItem item : carrito) {
            Producto p = buscarProducto(productos, item.getProductoId());
            if (p == null) {
                throw new IllegalStateException("Producto no encontrado: " + item.getSku());
            }
            if (p.isCaducado()) {
                throw new IllegalStateException("No se puede vender un producto caducado: " + p.getNombre()
                        + " (lote " + p.getLote() + ")");
            }
            if (item.getCantidad() > p.getStock()) {
                throw new IllegalStateException("Stock insuficiente para " + p.getNombre());
            }
        }

        int nuevoId = ventas.size() + 1;
        Venta nueva = new Venta();
        nueva.setId(nuevoId);
        nueva.setVendedorNombre(vendedor.getNombre());
        nueva.setVendedorCorreo(vendedor.getCorreo());
        nueva.setClienteNombre(venta.getClienteNombre());
        nueva.setClienteRuc(venta.getClienteRuc());
        nueva.setClienteTelefono(venta.getClienteTelefono());
        nueva.setClienteCorreo(venta.getClienteCorreo());
        nueva.setClienteDireccion(venta.getClienteDireccion());
        nueva.setFecha(LocalDateTime.now().format(FMT));
        nueva.setFacturada(false);

        List<VentaItem> copiaItems = new ArrayList<>();
        for (VentaItem item : carrito) {
            copiaItems.add(new VentaItem(item.getProductoId(), item.getSku(),
                    item.getNombre(), item.getCantidad(), item.getPrecioUnitario()));
            Producto p = buscarProducto(productos, item.getProductoId());
            String motivo = "Venta #" + nuevoId + " — " + venta.getClienteNombre();
            DashboardService.registrarMovimiento(ctx, item.getProductoId(), "SALIDA",
                    item.getCantidad(), motivo,
                    vendedor.getNombre() + " (" + DashboardService.rolVisible(vendedor) + ")");
        }
        nueva.setItems(copiaItems);
        nueva.recalcularTotal();
        ventas.add(nueva);

        DatosStorage.guardarProductos(productos);
        List<com.nexodist.model.Movimiento> movimientos =
                (List<com.nexodist.model.Movimiento>) ctx.getAttribute("movimientos");
        DatosStorage.guardarMovimientos(movimientos);
        DatosStorage.guardarVentas(ventas);

        return nueva;
    }

    private static Producto buscarProducto(List<Producto> productos, int id) {
        for (Producto p : productos) {
            if (p.getId() == id) return p;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static Venta buscarVenta(ServletContext ctx, int id) {
        List<Venta> ventas = (List<Venta>) ctx.getAttribute("ventas");
        if (ventas == null) return null;
        for (Venta v : ventas) {
            if (v.getId() == id) return v;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static void marcarFacturada(ServletContext ctx, int ventaId) {
        List<Venta> ventas = (List<Venta>) ctx.getAttribute("ventas");
        if (ventas == null) return;
        for (Venta v : ventas) {
            if (v.getId() == ventaId) {
                v.setFacturada(true);
                DatosStorage.guardarVentas(ventas);
                break;
            }
        }
    }
}
