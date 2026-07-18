package com.nexodist.servlet;

import java.io.IOException;
import java.util.List;

import com.nexodist.model.Producto;
import com.nexodist.model.Usuario;
import com.nexodist.model.Venta;
import com.nexodist.service.DashboardService;
import com.nexodist.service.VentaService;
import com.nexodist.util.RequestValidator;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/factura")
public class FacturaServlet extends HttpServlet {

    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Usuario usuario = validarVendedor(req, resp);
        if (usuario == null) return;

        List<Venta> ventas = VentaService.ventasDelVendedor(getServletContext(), usuario.getCorreo());
        List<Producto> productos = (List<Producto>) getServletContext().getAttribute("productos");

        String idTexto = req.getParameter("id");
        if (idTexto != null) {
            try {
                Venta seleccionada = VentaService.buscarVenta(getServletContext(), Integer.parseInt(idTexto));
                if (seleccionada != null
                        && seleccionada.getVendedorCorreo().equalsIgnoreCase(usuario.getCorreo())) {
                    req.setAttribute("ventaSeleccionada", seleccionada);
                }
            } catch (IllegalArgumentException e) {
                req.setAttribute("mensajeError", e.getMessage());
            }
        }

        req.setAttribute("ventas", ventas);
        req.setAttribute("alertasCount", DashboardService.contarAlertas(productos));
        req.setAttribute("scPagina", "factura");

        resp.setHeader("X-Modulo", "Factura");
        req.getRequestDispatcher("/factura.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Usuario usuario = validarVendedor(req, resp);
        if (usuario == null) return;

        int ventaId;
        try {
            ventaId = RequestValidator.enteroPositivo(req.getParameter("ventaId"), "El identificador de la venta");
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }
        Venta venta = VentaService.buscarVenta(getServletContext(), ventaId);
        if (venta != null && venta.getVendedorCorreo().equalsIgnoreCase(usuario.getCorreo())) {
            synchronized (getServletContext()) {
                VentaService.marcarFacturada(getServletContext(), ventaId);
            }
        }
        resp.sendRedirect("factura?id=" + ventaId + "&impreso=1");
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
        if (!DashboardService.puedeVerPanel(usuario, "factura")) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Sin acceso a factura");
            return null;
        }
        return usuario;
    }
}
