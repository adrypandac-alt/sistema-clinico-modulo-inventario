package com.nexodist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.nexodist.model.Producto;
import com.nexodist.model.Usuario;
import com.nexodist.model.Venta;
import com.nexodist.model.VentaItem;
import com.nexodist.service.DashboardService;
import com.nexodist.service.VentaService;

@DisplayName("Pruebas de caja negra")
class CajaNegraTest {

    @Test
    @DisplayName("El dashboard reporta alertas segun stock y caducidad")
    void dashboardReportaAlertasSegunReglasDeNegocio() {
        List<Producto> productos = List.of(
                producto(1, "Normal", 20, 10, LocalDate.now().plusDays(120)),
                producto(2, "Stock bajo", 3, 10, LocalDate.now().plusDays(120)),
                producto(3, "Proximo a caducar", 20, 10, LocalDate.now().plusDays(30)),
                producto(4, "Caducado", 20, 10, LocalDate.now().minusDays(1)));

        assertEquals(3, DashboardService.contarAlertas(productos));
        assertEquals(1, DashboardService.contarCaducados(productos));
        assertEquals(1, DashboardService.contarProximosCaducar(productos));
        assertEquals(63, DashboardService.totalUnidades(productos));
    }

    @Test
    @DisplayName("Una venta calcula subtotales y total desde los items")
    void ventaCalculaTotalDesdeItems() {
        Venta venta = new Venta();
        venta.setItems(List.of(
                new VentaItem(1, "SKU-001", "Alcohol", 2, 5.50),
                new VentaItem(2, "SKU-002", "Gasas", 3, 2.00)));

        venta.recalcularTotal();

        assertEquals(17.00, venta.getTotal(), 0.001);
        assertEquals(34.00, VentaService.totalVentas(List.of(venta, venta)), 0.001);
    }

    @Test
    @DisplayName("Los roles publicos habilitan solo los paneles esperados")
    void rolesHabilitanPanelesEsperados() {
        Usuario farmacia = new Usuario("Eva Venta", "eva@ase.com", "123", "Farmacia");
        Usuario admin = new Usuario("Ada Admin", "admin@ase.com", "123", "Administrador");
        Usuario custom = new Usuario("Uma User", "uma@ase.com", "123", "Operativo",
                "Inventario", true, "Hoy", "usuarios,factura");

        assertTrue(DashboardService.puedeRegistrarVentas(farmacia));
        assertFalse(DashboardService.puedeGestionarUsuarios(farmacia));
        assertTrue(DashboardService.puedeGestionarUsuarios(admin));
        assertTrue(DashboardService.puedeVerPanel(custom, "usuarios"));
        assertTrue(DashboardService.puedeVerPanel(custom, "factura"));
        assertFalse(DashboardService.puedeVerPanel(custom, "productos"));
    }

    private static Producto producto(int id, String nombre, int stock, int minimo, LocalDate caducidad) {
        return new Producto(id, "SKU-" + id, nombre, "Medicamentos", "Proveedor",
                10.0, stock, minimo, "Bodega", "L-" + id, caducidad.toString(), "RS-" + id);
    }
}
