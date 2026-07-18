package com.nexodist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.nexodist.model.Movimiento;
import com.nexodist.model.Producto;
import com.nexodist.model.Usuario;
import com.nexodist.model.Venta;
import com.nexodist.model.VentaItem;
import com.nexodist.service.DashboardService;
import com.nexodist.service.VentaService;

import jakarta.servlet.ServletContext;

@DisplayName("Pruebas de caja blanca")
class CajaBlancaTest {

    @Test
    @DisplayName("registrarMovimiento cubre entradas, salidas, ajustes y ramas invalidas")
    void registrarMovimientoCubreRamasPrincipales() {
        Producto producto = producto(1, "Paracetamol", 10, 5, LocalDate.now().plusDays(180));
        List<Movimiento> movimientos = new ArrayList<>();
        ServletContext ctx = contextoCon(Map.of(
                "productos", List.of(producto),
                "movimientos", movimientos,
                "totalMovimientos", 0));

        Movimiento entrada = DashboardService.registrarMovimiento(ctx, 1, "ENTRADA", 5,
                "Reposicion", "Admin");
        assertNotNull(entrada);
        assertEquals(10, entrada.getStockAntes());
        assertEquals(15, producto.getStock());

        Movimiento salida = DashboardService.registrarMovimiento(ctx, 1, "SALIDA", 4,
                "Venta", "Farmacia");
        assertNotNull(salida);
        assertEquals(15, salida.getStockAntes());
        assertEquals(11, producto.getStock());

        Movimiento ajuste = DashboardService.registrarMovimiento(ctx, 1, "AJUSTE", 7,
                "Conteo fisico", "Operativo");
        assertNotNull(ajuste);
        assertEquals(4, ajuste.getCantidad());
        assertEquals(7, producto.getStock());

        assertNull(DashboardService.registrarMovimiento(ctx, 1, "SALIDA", 99,
                "Sin stock", "Farmacia"));
        assertEquals(7, producto.getStock());

        assertNull(DashboardService.registrarMovimiento(ctx, 1, "DESCONOCIDO", 1,
                "Tipo invalido", "Admin"));
        assertEquals(3, movimientos.size());
    }

    @Test
    @DisplayName("productosConAlerta ordena por prioridad interna de riesgo")
    void productosConAlertaOrdenaPorPrioridadInterna() {
        Producto bajo = producto(1, "Stock bajo", 7, 10, LocalDate.now().plusDays(180));
        Producto critico = producto(2, "Stock critico", 5, 10, LocalDate.now().plusDays(180));
        Producto proximo = producto(3, "Proximo", 20, 10, LocalDate.now().plusDays(20));
        Producto caducado = producto(4, "Caducado", 20, 10, LocalDate.now().minusDays(1));

        List<Producto> alertas = DashboardService.productosConAlerta(List.of(bajo, critico, proximo, caducado));

        assertEquals(List.of(caducado, proximo, critico, bajo), alertas);
    }

    @Test
    @DisplayName("registrarVenta rechaza carrito vacio antes de tocar inventario")
    void registrarVentaRechazaCarritoVacio() {
        ServletContext ctx = contextoCon(new HashMap<>());
        Usuario vendedor = new Usuario("Eva Venta", "eva@ase.com", "123", "Farmacia");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> VentaService.registrarVenta(ctx, vendedor, new Venta(), List.of()));

        assertTrue(error.getMessage().contains("Agregue"));
    }

    @Test
    @DisplayName("registrarVenta valida producto inexistente, caducado y stock insuficiente")
    void registrarVentaValidaRamasDeInventario() {
        Producto disponible = producto(1, "Alcohol", 3, 1, LocalDate.now().plusDays(180));
        Producto caducado = producto(2, "Jarabe", 10, 1, LocalDate.now().minusDays(1));
        List<Venta> ventas = new ArrayList<>();
        ServletContext ctx = contextoCon(Map.of(
                "productos", List.of(disponible, caducado),
                "ventas", ventas));
        Usuario vendedor = new Usuario("Eva Venta", "eva@ase.com", "123", "Farmacia");

        assertThrows(IllegalStateException.class, () -> VentaService.registrarVenta(ctx, vendedor,
                cliente(), List.of(new VentaItem(99, "SKU-99", "Fantasma", 1, 1.0))));

        IllegalStateException errorCaducado = assertThrows(IllegalStateException.class,
                () -> VentaService.registrarVenta(ctx, vendedor, cliente(),
                        List.of(new VentaItem(2, "SKU-2", "Jarabe", 1, 8.0))));
        assertTrue(errorCaducado.getMessage().contains("caducado"));

        IllegalStateException errorStock = assertThrows(IllegalStateException.class,
                () -> VentaService.registrarVenta(ctx, vendedor, cliente(),
                        List.of(new VentaItem(1, "SKU-1", "Alcohol", 4, 5.0))));
        assertTrue(errorStock.getMessage().contains("Stock insuficiente"));
        assertEquals(0, ventas.size());
    }

    private static ServletContext contextoCon(Map<String, Object> atributosIniciales) {
        Map<String, Object> atributos = new HashMap<>(atributosIniciales);
        return (ServletContext) Proxy.newProxyInstance(
                ServletContext.class.getClassLoader(),
                new Class<?>[] {ServletContext.class},
                (proxy, method, args) -> {
                    if ("getAttribute".equals(method.getName())) {
                        return atributos.get((String) args[0]);
                    }
                    if ("setAttribute".equals(method.getName())) {
                        atributos.put((String) args[0], args[1]);
                        return null;
                    }
                    if ("getContextPath".equals(method.getName())) {
                        return "";
                    }
                    Class<?> retorno = method.getReturnType();
                    if (retorno.equals(boolean.class)) return false;
                    if (retorno.equals(int.class)) return 0;
                    if (retorno.equals(long.class)) return 0L;
                    if (retorno.equals(double.class)) return 0.0;
                    return null;
                });
    }

    private static Venta cliente() {
        Venta venta = new Venta();
        venta.setClienteNombre("Consumidor final");
        venta.setClienteRuc("9999999999");
        return venta;
    }

    private static Producto producto(int id, String nombre, int stock, int minimo, LocalDate caducidad) {
        Producto producto = new Producto(id, "SKU-" + id, nombre, "Medicamentos", "Proveedor",
                10.0, stock, minimo, "Bodega", "L-" + id, caducidad.toString(), "RS-" + id);
        return producto;
    }
}
