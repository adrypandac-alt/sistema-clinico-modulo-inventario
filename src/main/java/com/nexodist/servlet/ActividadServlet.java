package com.nexodist.servlet;

import java.io.IOException;
import java.util.List;

import com.nexodist.model.ActividadCuenta;
import com.nexodist.model.Producto;
import com.nexodist.model.Usuario;
import com.nexodist.service.DashboardService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/actividad")
public class ActividadServlet extends HttpServlet {

    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("usuario") == null) {
            resp.sendRedirect("index.jsp");
            return;
        }
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (DashboardService.esVendedor(usuario)) {
            resp.sendRedirect("venta");
            return;
        }

        List<ActividadCuenta> actividad =
                (List<ActividadCuenta>) getServletContext().getAttribute("actividadCuentas");
        List<Producto> productos =
                (List<Producto>) getServletContext().getAttribute("productos");

        req.setAttribute("actividad", actividad);
        req.setAttribute("alertasCount", DashboardService.contarAlertas(
                productos != null ? productos : List.of()));

        resp.setHeader("X-Modulo", "Actividad-Cuentas");
        resp.setHeader("X-Metodo-HTTP", "GET");
        resp.setHeader("X-Estado-Operacion", "Consulta-Correcta");
        resp.setStatus(HttpServletResponse.SC_OK);
        req.getRequestDispatcher("/actividad.jsp").forward(req, resp);
    }
}
