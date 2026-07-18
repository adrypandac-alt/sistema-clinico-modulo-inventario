package com.nexodist.servlet;

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

import java.io.IOException;
import java.util.List;

@WebServlet("/ventas-admin")
public class VentasAdminServlet extends HttpServlet {

    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Usuario usuario = validarAdmin(request, response);
        if (usuario == null) return;

        List<Venta> ventas = (List<Venta>) getServletContext().getAttribute("ventas");
        List<Producto> productos = (List<Producto>) getServletContext().getAttribute("productos");

        request.setAttribute("ventas", VentaService.ultimasVentas(ventas, Integer.MAX_VALUE));
        request.setAttribute("ventasTotal", VentaService.totalVentas(ventas));
        request.setAttribute("alertasCount", DashboardService.contarAlertas(productos));
        request.setAttribute("scPagina", "ventas-admin");
        response.setHeader("X-Modulo", "Ventas-Administracion");
        request.getRequestDispatcher("/ventas-admin.jsp").forward(request, response);
    }

    private Usuario validarAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuario") == null) {
            response.sendRedirect("index.jsp");
            return null;
        }
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (!DashboardService.esAdministrador(usuario)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso solo para administradores");
            return null;
        }
        return usuario;
    }
}
