package com.nexodist.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.nexodist.model.Movimiento;
import com.nexodist.model.Producto;
import com.nexodist.service.DashboardService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api/dashboard")
public class ApiDashboardServlet extends HttpServlet {

    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setHeader("X-Modulo", "API-Dashboard");
        resp.setHeader("Cache-Control", "no-cache");

        List<Producto> productos = (List<Producto>) getServletContext().getAttribute("productos");
        List<Movimiento> movimientos = (List<Movimiento>) getServletContext().getAttribute("movimientos");

        if (productos == null) productos = List.of();
        if (movimientos == null) movimientos = List.of();

        int alertas = DashboardService.contarAlertas(productos);
        int[] entradas = DashboardService.movimientosSemanaEntradas(movimientos);
        int[] salidas = DashboardService.movimientosSemanaSalidas(movimientos);
        Map<String, Integer> porCategoria = DashboardService.stockPorCategoria(productos);
        List<Movimiento> ultimos = DashboardService.ultimosMovimientos(movimientos, 5);
        List<Producto> alertasList = DashboardService.productosConAlerta(productos);
        List<Map<String, String>> recomendaciones = DashboardService.generarRecomendaciones(productos);

        double valorTotal = DashboardService.valorTotal(productos);

        PrintWriter out = resp.getWriter();
        out.print("{");
        out.print("\"timestamp\":\"" + LocalDateTime.now().toString() + "\",");
        out.print("\"alertasCount\":" + alertas + ",");
        out.print("\"fechaCorta\":\"" + esc(DashboardService.fechaCorta()) + "\",");
        out.print("\"kpis\":{");
        out.print("\"totalProductos\":" + productos.size() + ",");
        out.print("\"unidadesStock\":" + DashboardService.totalUnidades(productos) + ",");
        out.print("\"valorTotal\":" + (long) valorTotal + ",");
        out.print("\"valorTotalK\":\"" + String.format(Locale.US, "%.0f", valorTotal / 1000) + "\",");
        out.print("\"bajoStock\":" + alertas + ",");
        out.print("\"criticos\":" + DashboardService.contarCriticos(productos) + ",");
        out.print("\"bajos\":" + DashboardService.contarBajos(productos) + ",");
        out.print("\"caducados\":" + DashboardService.contarCaducados(productos) + ",");
        out.print("\"proximosCaducar\":" + DashboardService.contarProximosCaducar(productos) + ",");
        out.print("\"normales\":" + DashboardService.contarNormales(productos));
        out.print("},");

        out.print("\"entradasSemana\":[");
        for (int i = 0; i < entradas.length; i++) {
            out.print(entradas[i]);
            if (i < entradas.length - 1) out.print(",");
        }
        out.print("],");
        out.print("\"salidasSemana\":[");
        for (int i = 0; i < salidas.length; i++) {
            out.print(salidas[i]);
            if (i < salidas.length - 1) out.print(",");
        }
        out.print("],");

        out.print("\"categorias\":[");
        int ci = 0;
        for (Map.Entry<String, Integer> e : porCategoria.entrySet()) {
            if (ci > 0) out.print(",");
            out.print("{\"nombre\":\"" + esc(e.getKey()) + "\",\"unidades\":" + e.getValue() + "}");
            ci++;
        }
        out.print("],");

        out.print("\"ultimosMovimientos\":[");
        for (int i = 0; i < ultimos.size(); i++) {
            Movimiento m = ultimos.get(i);
            if (i > 0) out.print(",");
            out.print(movimientoJson(m));
        }
        out.print("],");

        out.print("\"alertasStock\":[");
        for (int i = 0; i < alertasList.size(); i++) {
            Producto p = alertasList.get(i);
            if (i > 0) out.print(",");
            out.print(productoAlertaJson(p));
        }
        out.print("],");

        out.print("\"recomendaciones\":[");
        for (int i = 0; i < recomendaciones.size(); i++) {
            Map<String, String> r = recomendaciones.get(i);
            if (i > 0) out.print(",");
            out.print("{\"nivel\":\"" + esc(r.get("nivel")) + "\",");
            out.print("\"titulo\":\"" + esc(r.get("titulo")) + "\",");
            out.print("\"texto\":\"" + esc(r.get("texto")) + "\",");
            out.print("\"ref\":\"" + esc(r.get("ref")) + "\"}");
        }
        out.print("]}");
    }

    private String movimientoJson(Movimiento m) {
        String signo = "ENTRADA".equals(m.getTipo()) ? "+" : "-";
        return "{\"producto\":\"" + esc(m.getProductoNombre()) + "\","
                + "\"tipo\":\"" + esc(m.getTipo()) + "\","
                + "\"motivo\":\"" + esc(m.getMotivo()) + "\","
                + "\"cantidad\":\"" + signo + m.getCantidad() + "\","
                + "\"stockAntes\":" + m.getStockAntes() + ","
                + "\"stockDespues\":" + m.getStockDespues() + ","
                + "\"usuario\":\"" + esc(m.getUsuario()) + "\","
                + "\"fecha\":\"" + esc(m.getFecha()) + "\"}";
    }

    private String productoAlertaJson(Producto p) {
        String nivel = p.isCaducado() || p.esStockCritico() ? "critico" : "bajo";
        String tipo = p.isCaducado() ? "caducado" : (p.isProximoCaducar() ? "proximo" : "stock");
        return "{\"nombre\":\"" + esc(p.getNombre()) + "\","
                + "\"sku\":\"" + esc(p.getSku()) + "\","
                + "\"categoria\":\"" + esc(p.getCategoria()) + "\","
                + "\"proveedor\":\"" + esc(p.getProveedor()) + "\","
                + "\"ubicacion\":\"" + esc(p.getUbicacion()) + "\","
                + "\"lote\":\"" + esc(p.getLote()) + "\","
                + "\"fechaCaducidad\":\"" + esc(p.getFechaCaducidad()) + "\","
                + "\"tipoAlerta\":\"" + tipo + "\","
                + "\"stock\":" + p.getStock() + ","
                + "\"minimo\":" + p.getStockMinimo() + ","
                + "\"nivel\":\"" + nivel + "\"}";
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
