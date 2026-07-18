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

@WebServlet("/mis-ventas")
public class MisVentasServlet extends HttpServlet {

    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Usuario usuario = validarVendedor(req, resp);
        if (usuario == null) return;

        List<Venta> ventas = VentaService.ventasDelVendedor(getServletContext(), usuario.getCorreo());
        List<Producto> productos = (List<Producto>) getServletContext().getAttribute("productos");

        req.setAttribute("ventas", ventas);
        req.setAttribute("alertasCount", DashboardService.contarAlertas(productos));
        req.setAttribute("scPagina", "mis-ventas");

        resp.setHeader("X-Modulo", "Mis-Ventas");
        req.getRequestDispatcher("/mis-ventas.jsp").forward(req, resp);
    }

    private Usuario validarVendedor(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("usuario") == null) {
            resp.sendRedirect("index.jsp");
            return null;
        }
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (!DashboardService.esVendedor(usuario)) {
            resp.sendRedirect(DashboardService.rutaInicio(usuario));
            return null;
        }
        if (!DashboardService.puedeVerPanel(usuario, "mis-ventas")) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Sin acceso a mis ventas");
            return null;
        }
        return usuario;
    }
}
