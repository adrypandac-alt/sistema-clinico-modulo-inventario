package com.nexodist.servlet;

import java.io.IOException;
import java.util.List;

import com.nexodist.model.Producto;
import com.nexodist.model.Usuario;
import com.nexodist.model.Venta;
import com.nexodist.service.DashboardService;
import com.nexodist.service.VentaService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/panel")
public class PanelServlet extends HttpServlet {

    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("usuario") == null) {
            resp.sendRedirect("index.jsp");
            return;
        }
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (!DashboardService.puedeVerPanel(usuario, "dashboard")) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Sin acceso al dashboard");
            return;
        }
        List<Producto> productos = (List<Producto>) getServletContext().getAttribute("productos");
        req.setAttribute("alertasCount", DashboardService.contarAlertas(productos));
        req.setAttribute("caducadosCount", DashboardService.contarCaducados(productos));
        req.setAttribute("proximosCaducarCount", DashboardService.contarProximosCaducar(productos));
        req.setAttribute("fechaResumen", DashboardService.fechaHoyFormateada());
        List<Venta> ventas = (List<Venta>) getServletContext().getAttribute("ventas");
        req.setAttribute("ventasCount", ventas == null ? 0 : ventas.size());
        req.setAttribute("ventasTotal", VentaService.totalVentas(ventas));
        req.setAttribute("ultimasVentas", VentaService.ultimasVentas(ventas, 5));

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setHeader("X-Modulo", "Panel-Inventario");
        resp.setHeader("X-Metodo-HTTP", "GET");
        req.getRequestDispatcher("/panel.jsp").forward(req, resp);
    }
}
