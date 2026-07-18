package com.nexodist.servlet;

import java.io.IOException;
import java.util.List;

import com.nexodist.model.Producto;
import com.nexodist.model.Usuario;
import com.nexodist.service.DashboardService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/alertas-venta")
public class VendedorAlertasServlet extends HttpServlet {

    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Usuario usuario = validarVendedor(req, resp);
        if (usuario == null) return;

        List<Producto> productos = (List<Producto>) getServletContext().getAttribute("productos");
        req.setAttribute("alertas", DashboardService.productosConAlerta(productos));
        req.setAttribute("criticos", DashboardService.contarCriticos(productos));
        req.setAttribute("bajos", DashboardService.contarBajos(productos));
        req.setAttribute("normales", DashboardService.contarNormales(productos));
        req.setAttribute("caducados", DashboardService.contarCaducados(productos));
        req.setAttribute("proximosCaducar", DashboardService.contarProximosCaducar(productos));
        req.setAttribute("alertasCount", DashboardService.contarAlertas(productos));
        req.setAttribute("scPagina", "alertas-venta");

        resp.setHeader("X-Modulo", "Alertas-Venta");
        req.getRequestDispatcher("/alertas-vendedor.jsp").forward(req, resp);
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
        if (!DashboardService.puedeVerPanel(usuario, "alertas-venta")) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Sin acceso a alertas de farmacia");
            return null;
        }
        return usuario;
    }
}
