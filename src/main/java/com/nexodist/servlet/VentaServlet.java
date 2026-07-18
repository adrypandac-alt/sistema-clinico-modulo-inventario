package com.nexodist.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.nexodist.model.Producto;
import com.nexodist.model.Usuario;
import com.nexodist.model.Venta;
import com.nexodist.model.VentaItem;
import com.nexodist.service.DashboardService;
import com.nexodist.service.VentaService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/venta")
public class VentaServlet extends HttpServlet {

    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Usuario usuario = validarVendedor(req, resp);
        if (usuario == null) return;

        List<Producto> productos = (List<Producto>) getServletContext().getAttribute("productos");
        productos = productosDisponibles(productos);
        List<VentaItem> carrito = obtenerCarrito(req.getSession());

        req.setAttribute("productos", productos);
        req.setAttribute("carrito", carrito);
        req.setAttribute("alertasCount", DashboardService.contarAlertas(productos));
        req.setAttribute("scPagina", "venta");

        resp.setHeader("X-Modulo", "Panel-Venta");
        req.getRequestDispatcher("/venta.jsp").forward(req, resp);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Usuario usuario = validarVendedor(req, resp);
        if (usuario == null) return;

        req.setCharacterEncoding("UTF-8");
        String accion = req.getParameter("accion");
        HttpSession session = req.getSession();
        List<VentaItem> carrito = obtenerCarrito(session);

        if ("agregar".equals(accion)) {
            int productoId = Integer.parseInt(req.getParameter("productoId"));
            int cantidad = Integer.parseInt(req.getParameter("cantidad"));
            List<Producto> productos = (List<Producto>) getServletContext().getAttribute("productos");
            Producto p = null;
            for (Producto prod : productos) {
                if (prod.getId() == productoId) { p = prod; break; }
            }
            if (p != null && cantidad > 0) {
                boolean existe = false;
                for (VentaItem item : carrito) {
                    if (item.getProductoId() == productoId) {
                        item.setCantidad(item.getCantidad() + cantidad);
                        existe = true;
                        break;
                    }
                }
                if (!existe) {
                    carrito.add(new VentaItem(p.getId(), p.getSku(), p.getNombre(), cantidad, p.getPrecio()));
                }
                session.setAttribute("carritoVenta", carrito);
            }
            resp.sendRedirect("venta");
            return;
        }

        if ("eliminar".equals(accion)) {
            int productoId = Integer.parseInt(req.getParameter("productoId"));
            carrito.removeIf(i -> i.getProductoId() == productoId);
            session.setAttribute("carritoVenta", carrito);
            resp.sendRedirect("venta");
            return;
        }

        if ("actualizar".equals(accion)) {
            int productoId = Integer.parseInt(req.getParameter("productoId"));
            int cantidad = Integer.parseInt(req.getParameter("cantidad"));
            for (VentaItem item : carrito) {
                if (item.getProductoId() == productoId) {
                    item.setCantidad(Math.max(1, cantidad));
                    break;
                }
            }
            session.setAttribute("carritoVenta", carrito);
            resp.sendRedirect("venta");
            return;
        }

        if ("registrar".equals(accion)) {
            Venta datos = new Venta();
            datos.setClienteNombre(req.getParameter("clienteNombre"));
            datos.setClienteRuc(req.getParameter("clienteRuc"));
            datos.setClienteTelefono(req.getParameter("clienteTelefono"));
            datos.setClienteCorreo(req.getParameter("clienteCorreo"));
            datos.setClienteDireccion(req.getParameter("clienteDireccion"));

            List<String> errores = validarCliente(datos);
            if (!errores.isEmpty()) {
                req.setAttribute("errores", errores);
                req.setAttribute("clienteNombre", datos.getClienteNombre());
                req.setAttribute("clienteRuc", datos.getClienteRuc());
                req.setAttribute("clienteTelefono", datos.getClienteTelefono());
                req.setAttribute("clienteCorreo", datos.getClienteCorreo());
                req.setAttribute("clienteDireccion", datos.getClienteDireccion());
                List<Producto> productos = (List<Producto>) getServletContext().getAttribute("productos");
                req.setAttribute("productos", productos);
                req.setAttribute("carrito", carrito);
                req.setAttribute("alertasCount", DashboardService.contarAlertas(productos));
                req.setAttribute("scPagina", "venta");
                req.getRequestDispatcher("/venta.jsp").forward(req, resp);
                return;
            }

            try {
                synchronized (getServletContext()) {
                    VentaService.registrarVenta(getServletContext(), usuario, datos, carrito);
                }
                session.removeAttribute("carritoVenta");
                resp.sendRedirect("mis-ventas?ok=1");
            } catch (IllegalStateException e) {
                req.setAttribute("errores", List.of(e.getMessage()));
                req.setAttribute("clienteNombre", datos.getClienteNombre());
                req.setAttribute("clienteRuc", datos.getClienteRuc());
                req.setAttribute("clienteTelefono", datos.getClienteTelefono());
                req.setAttribute("clienteCorreo", datos.getClienteCorreo());
                req.setAttribute("clienteDireccion", datos.getClienteDireccion());
                List<Producto> productos = (List<Producto>) getServletContext().getAttribute("productos");
                req.setAttribute("productos", productos);
                req.setAttribute("carrito", carrito);
                req.setAttribute("alertasCount", DashboardService.contarAlertas(productos));
                req.setAttribute("scPagina", "venta");
                req.getRequestDispatcher("/venta.jsp").forward(req, resp);
            }
            return;
        }

        resp.sendRedirect("venta");
    }

    private List<String> validarCliente(Venta datos) {
        List<String> errores = new ArrayList<>();
        if (datos.getClienteNombre() == null || datos.getClienteNombre().trim().isEmpty()) {
            errores.add("El nombre del cliente es obligatorio.");
        }
        if (datos.getClienteRuc() == null || datos.getClienteRuc().trim().isEmpty()) {
            errores.add("El RUC o cédula es obligatorio.");
        }
        return errores;
    }

    private List<Producto> productosDisponibles(List<Producto> productos) {
        List<Producto> disponibles = new ArrayList<>();
        if (productos == null) return disponibles;
        for (Producto producto : productos) {
            if (!producto.isCaducado() && producto.getStock() > 0) disponibles.add(producto);
        }
        return disponibles;
    }

    @SuppressWarnings("unchecked")
    private List<VentaItem> obtenerCarrito(HttpSession session) {
        List<VentaItem> carrito = (List<VentaItem>) session.getAttribute("carritoVenta");
        if (carrito == null) {
            carrito = new ArrayList<>();
            session.setAttribute("carritoVenta", carrito);
        }
        return carrito;
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
        if (!DashboardService.puedeVerPanel(usuario, "venta")) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Sin acceso al panel de venta");
            return null;
        }
        return usuario;
    }
}
