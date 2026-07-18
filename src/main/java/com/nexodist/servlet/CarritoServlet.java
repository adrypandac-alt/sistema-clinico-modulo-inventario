package com.nexodist.servlet;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.nexodist.model.CarritoItem;
import com.nexodist.model.Producto;
import com.nexodist.model.Usuario;
import com.nexodist.service.DashboardService;
import com.nexodist.storage.DatosStorage;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/carrito")
public class CarritoServlet extends HttpServlet {

    private static final String COOKIE_NAME = "historialClinico";
    private static final String COOKIE_SEPARATOR = ";;";
    private static final int COOKIE_MAX_AGE = 15 * 24 * 60 * 60;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setHeader("X-Modulo", "Carrito-Stock");
        resp.setHeader("X-Metodo-HTTP", "GET");
        req.getRequestDispatcher("/carrito.jsp").forward(req, resp);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Usuario usuario = req.getSession(false) == null ? null
                : (Usuario) req.getSession(false).getAttribute("usuario");
        if (!DashboardService.puedeModificarInventario(usuario)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Usuario de solo lectura");
            return;
        }
        int idProducto;
        int cantidad;

        try {
            idProducto = Integer.parseInt(req.getParameter("idProducto"));
            cantidad = Integer.parseInt(req.getParameter("cantidad"));
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            req.setAttribute("mensajeError", "El producto y la cantidad deben ser valores validos");
            req.setAttribute("codigoEstado", HttpServletResponse.SC_BAD_REQUEST);
            req.getRequestDispatcher("/productoError.jsp").forward(req, resp);
            return;
        }

        List<Producto> productos = (List<Producto>) getServletContext().getAttribute("productos");
        Producto productoSeleccionado = buscarProducto(productos, idProducto);

        if (productoSeleccionado == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.setHeader("X-Estado-Operacion", "Producto-No-Encontrado");
            req.setAttribute("mensajeError", "Producto no encontrado en inventario");
            req.setAttribute("codigoEstado", HttpServletResponse.SC_NOT_FOUND);
            req.getRequestDispatcher("/productoError.jsp").forward(req, resp);
            return;
        }

        if (cantidad <= 0 || cantidad > productoSeleccionado.getStock()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setHeader("X-Estado-Operacion", "Stock-Insuficiente");
            req.setAttribute("mensajeError", "Cantidad invalida o stock insuficiente");
            req.setAttribute("codigoEstado", HttpServletResponse.SC_BAD_REQUEST);
            req.getRequestDispatcher("/productoError.jsp").forward(req, resp);
            return;
        }

        HttpSession session = req.getSession();
        List<CarritoItem> carrito = (List<CarritoItem>) session.getAttribute("carrito");

        if (carrito == null) {
            carrito = new ArrayList<>();
        }

        boolean existente = false;
        for (CarritoItem item : carrito) {
            if (item.getProducto().getId() == idProducto) {
                item.setCantidad(item.getCantidad() + cantidad);
                existente = true;
                break;
            }
        }

        if (!existente) {
            carrito.add(new CarritoItem(productoSeleccionado, cantidad));
        }

        session.setAttribute("carrito", carrito);

        synchronized (getServletContext()) {
            if (DashboardService.registrarMovimiento(getServletContext(), idProducto, "SALIDA", cantidad,
                    "Salida automática desde carrito interno",
                    usuario.getNombre() + " (" + DashboardService.rolVisible(usuario) + ")") == null) {
                resp.sendError(HttpServletResponse.SC_CONFLICT, "No se pudo registrar el movimiento");
                return;
            }
            List<Producto> todosProductos = (List<Producto>) getServletContext().getAttribute("productos");
            if (todosProductos != null) {
                DatosStorage.guardarProductos(todosProductos);
            }
            List<com.nexodist.model.Movimiento> movimientos =
                    (List<com.nexodist.model.Movimiento>) getServletContext().getAttribute("movimientos");
            DatosStorage.guardarMovimientos(movimientos);
        }

        guardarHistorial(req, resp, productoSeleccionado, cantidad);

        resp.setStatus(HttpServletResponse.SC_SEE_OTHER);
        resp.setHeader("X-Modulo", "Carrito-Stock");
        resp.setHeader("X-Estado-Operacion", "Movimiento-Creado");
        resp.setHeader("Location", req.getContextPath() + "/carrito");
    }

    private Producto buscarProducto(List<Producto> productos, int idProducto) {
        if (productos == null) {
            return null;
        }

        for (Producto producto : productos) {
            if (producto.getId() == idProducto) {
                return producto;
            }
        }

        return null;
    }

    private void guardarHistorial(HttpServletRequest req, HttpServletResponse resp, Producto producto, int cantidad) {
        String historialAnterior = obtenerCookie(req, COOKIE_NAME);
        String movimiento = LocalDateTime.now() + " - " + producto.getNombre() + " x" + cantidad + " $" + producto.getPrecio();
        String historialNuevo = historialAnterior == null || historialAnterior.isEmpty()
                ? movimiento
                : historialAnterior + COOKIE_SEPARATOR + movimiento;

        Cookie cookie = new Cookie(COOKIE_NAME, URLEncoder.encode(historialNuevo, StandardCharsets.UTF_8));
        cookie.setMaxAge(COOKIE_MAX_AGE);
        cookie.setPath(req.getContextPath().isEmpty() ? "/" : req.getContextPath());
        cookie.setHttpOnly(true);
        resp.addCookie(cookie);
    }

    private String obtenerCookie(HttpServletRequest req, String nombreCookie) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(nombreCookie)) {
                return URLDecoder.decode(cookie.getValue(), StandardCharsets.UTF_8);
            }
        }

        return null;
    }

}
