package com.nexodist.servlet;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.nexodist.model.Movimiento;
import com.nexodist.model.Pedido;
import com.nexodist.model.PedidoItem;
import com.nexodist.model.Producto;
import com.nexodist.model.Usuario;
import com.nexodist.service.DashboardService;
import com.nexodist.storage.DatosStorage;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/pedidos")
public class PedidoServlet extends HttpServlet {
    private static final String CARRITO_PEDIDO = "pedidoCarrito";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Usuario usuario = validarSesion(req, resp);
        if (usuario == null) return;

        req.setAttribute("scPagina", "despachos");
        req.setAttribute("productos", getServletContext().getAttribute("productos"));
        req.setAttribute("pedidos", obtenerPedidos());
        req.setAttribute("carritoPedido", obtenerCarrito(req.getSession()));
        req.setAttribute("pedidoNombres", valorSesion(req.getSession(), "pedidoNombres"));
        req.setAttribute("pedidoApellidos", valorSesion(req.getSession(), "pedidoApellidos"));
        req.setAttribute("alertasCount", DashboardService.contarAlertas(
                (List<Producto>) getServletContext().getAttribute("productos")));
        resp.setHeader("X-Modulo", "Pedidos-Inventario");
        req.getRequestDispatcher("/pedidos.jsp").forward(req, resp);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Usuario usuario = validarSesion(req, resp);
        if (usuario == null) return;
        if (!DashboardService.puedeModificarInventario(usuario)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Usuario de solo lectura");
            return;
        }

        req.setCharacterEncoding("UTF-8");
        String accion = req.getParameter("accion");
        if ("agregar".equals(accion)) {
            agregarItem(req, resp);
            return;
        }
        if ("quitar".equals(accion)) {
            quitarItem(req, resp);
            return;
        }
        if ("revisar".equals(accion)) {
            List<PedidoItem> carrito = obtenerCarrito(req.getSession());
            if (carrito.isEmpty()) {
                resp.sendRedirect("pedidos?error=vacio");
                return;
            }
            req.setAttribute("scPagina", "inventario");
            req.setAttribute("carritoPedido", carrito);
            req.setAttribute("pedidoSolicitante", nombreSolicitante(req.getSession()));
            req.setAttribute("alertasCount", DashboardService.contarAlertas(
                    (List<Producto>) getServletContext().getAttribute("productos")));
            req.getRequestDispatcher("/pedido-confirmar.jsp").forward(req, resp);
            return;
        }
        if ("confirmar".equals(accion)) {
            confirmarPedido(req, resp, usuario);
            return;
        }
        if ("iniciarDespacho".equals(accion)) {
            cambiarEstadoDespacho(req, resp, usuario, false); return;
        }
        if ("completarDespacho".equals(accion)) {
            cambiarEstadoDespacho(req, resp, usuario, true); return;
        }
        if ("cancelar".equals(accion)) {
            req.getSession().removeAttribute(CARRITO_PEDIDO);
            req.getSession().removeAttribute("pedidoNombres");
            req.getSession().removeAttribute("pedidoApellidos");
            resp.sendRedirect("pedidos");
            return;
        }
        resp.sendRedirect("pedidos");
    }

    @SuppressWarnings("unchecked")
    private void agregarItem(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int productoId = Integer.parseInt(req.getParameter("id"));
        int cantidad = Integer.parseInt(req.getParameter("cantidadPedido"));
        String observacion = limpiar(req.getParameter("observacionPedido"));
        String nombres = limpiar(req.getParameter("nombresSolicitante"));
        String apellidos = limpiar(req.getParameter("apellidosSolicitante"));
        if (nombres.isBlank() || apellidos.isBlank()) {
            resp.sendRedirect("pedidos?error=solicitante"); return;
        }
        req.getSession().setAttribute("pedidoNombres", nombres);
        req.getSession().setAttribute("pedidoApellidos", apellidos);

        List<Producto> productos = (List<Producto>) getServletContext().getAttribute("productos");
        Producto producto = buscarProducto(productos, productoId);
        if (producto == null || cantidad <= 0) {
            resp.sendRedirect("pedidos?error=producto");
            return;
        }

        List<PedidoItem> carrito = obtenerCarrito(req.getSession());
        PedidoItem existente = buscarItem(carrito, productoId);
        if (existente == null) {
            carrito.add(new PedidoItem(producto, cantidad, observacion));
        } else {
            existente.setCantidad(existente.getCantidad() + cantidad);
            if (!observacion.isBlank()) existente.setObservacion(observacion);
        }
        resp.sendRedirect("pedidos?carrito=1");
    }

    private void quitarItem(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int productoId = Integer.parseInt(req.getParameter("id"));
        List<PedidoItem> carrito = obtenerCarrito(req.getSession());
        carrito.removeIf(item -> item.getProductoId() == productoId);
        resp.sendRedirect("pedidos?carrito=1");
    }

    @SuppressWarnings("unchecked")
    private void confirmarPedido(HttpServletRequest req, HttpServletResponse resp, Usuario usuario) throws IOException {
        List<PedidoItem> carrito = obtenerCarrito(req.getSession());
        if (carrito.isEmpty()) {
            resp.sendRedirect("pedidos?error=vacio");
            return;
        }

        synchronized (getServletContext()) {
            List<Pedido> pedidos = obtenerPedidos();
            List<PedidoItem> copia = new ArrayList<>(carrito);
            String solicitante = nombreSolicitante(req.getSession());
            if (solicitante.isBlank()) { resp.sendRedirect("pedidos?error=datos"); return; }
            Pedido pedido = new Pedido(pedidos.stream().mapToInt(Pedido::getId).max().orElse(0) + 1, usuario.getNombre(),
                    "Pedido", LocalDateTime.now().format(FMT), "Pendiente", copia);
            pedido.setClinica(""); pedido.setSolicitante(solicitante);
            pedidos.add(0, pedido);
            getServletContext().setAttribute("pedidos", pedidos);

            for (PedidoItem item : copia) {
                String detalle = item.getObservacion() == null || item.getObservacion().isBlank()
                        ? "Pedido confirmado #" + pedido.getId()
                        : "Pedido confirmado #" + pedido.getId() + ": " + item.getObservacion();
                DashboardService.registrarMovimientoInformativo(getServletContext(), item.getProductoId(), "PEDIDO",
                        item.getCantidad(), detalle,
                        usuario.getNombre() + " (" + DashboardService.rolVisible(usuario) + ")");
            }
            DatosStorage.guardarMovimientos((List<Movimiento>) getServletContext().getAttribute("movimientos"));
            DatosStorage.guardarPedidos(pedidos);
        }

        req.getSession().removeAttribute(CARRITO_PEDIDO);
        req.getSession().removeAttribute("pedidoNombres");
        req.getSession().removeAttribute("pedidoApellidos");
        resp.sendRedirect("pedidos?ok=1");
    }

    @SuppressWarnings("unchecked")
    private void cambiarEstadoDespacho(HttpServletRequest req, HttpServletResponse resp, Usuario usuario, boolean completar) throws IOException {
        int id;
        try { id = Integer.parseInt(req.getParameter("id")); } catch (Exception e) { resp.sendRedirect("pedidos?error=pedido"); return; }
        synchronized (getServletContext()) {
            Pedido pedido = obtenerPedidos().stream().filter(p -> p.getId() == id).findFirst().orElse(null);
            if (pedido == null) { resp.sendRedirect("pedidos?error=pedido"); return; }
            if (!completar) {
                if (!"Pendiente".equalsIgnoreCase(pedido.getEstado())) { resp.sendRedirect("pedidos?error=estado"); return; }
                pedido.setEstado("Preparando"); pedido.setDespachadoPor(usuario.getNombre());
            } else {
                if (!"Preparando".equalsIgnoreCase(pedido.getEstado())) { resp.sendRedirect("pedidos?error=estado"); return; }
                String entregadoA = limpiar(req.getParameter("entregadoA"));
                if (entregadoA.isBlank()) { resp.sendRedirect("pedidos?error=entrega&id=" + id); return; }
                List<Producto> productos = (List<Producto>) getServletContext().getAttribute("productos");
                for (PedidoItem item : pedido.getItems()) {
                    Producto p = buscarProducto(productos, item.getProductoId());
                    if (p == null || p.getStock() < item.getCantidad()) { resp.sendRedirect("pedidos?error=stock&id=" + id); return; }
                }
                for (PedidoItem item : pedido.getItems()) DashboardService.registrarMovimiento(getServletContext(), item.getProductoId(), "SALIDA", item.getCantidad(),
                        "Despacho #" + id + " entregado a " + entregadoA, usuario.getNombre() + " (" + DashboardService.rolVisible(usuario) + ")");
                pedido.setEstado("Despachado"); pedido.setDespachadoPor(usuario.getNombre()); pedido.setEntregadoA(entregadoA);
                DatosStorage.guardarProductos(productos);
                DatosStorage.guardarMovimientos((List<Movimiento>) getServletContext().getAttribute("movimientos"));
            }
            DatosStorage.guardarPedidos(obtenerPedidos());
        }
        resp.sendRedirect("pedidos?" + (completar ? "despachado=1" : "iniciado=1"));
    }

    @SuppressWarnings("unchecked")
    private List<Pedido> obtenerPedidos() {
        List<Pedido> pedidos = (List<Pedido>) getServletContext().getAttribute("pedidos");
        if (pedidos == null) {
            pedidos = new ArrayList<>();
            getServletContext().setAttribute("pedidos", pedidos);
        }
        return pedidos;
    }

    @SuppressWarnings("unchecked")
    private List<PedidoItem> obtenerCarrito(HttpSession session) {
        List<PedidoItem> carrito = (List<PedidoItem>) session.getAttribute(CARRITO_PEDIDO);
        if (carrito == null) {
            carrito = new ArrayList<>();
            session.setAttribute(CARRITO_PEDIDO, carrito);
        }
        return carrito;
    }

    private Producto buscarProducto(List<Producto> productos, int id) {
        if (productos == null) return null;
        for (Producto producto : productos) {
            if (producto.getId() == id) return producto;
        }
        return null;
    }

    private PedidoItem buscarItem(List<PedidoItem> carrito, int productoId) {
        for (PedidoItem item : carrito) {
            if (item.getProductoId() == productoId) return item;
        }
        return null;
    }

    private String limpiar(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private String valorSesion(HttpSession session, String clave) {
        Object valor = session.getAttribute(clave);
        return valor == null ? "" : valor.toString();
    }

    private String nombreSolicitante(HttpSession session) {
        return (valorSesion(session, "pedidoNombres") + " " + valorSesion(session, "pedidoApellidos")).trim();
    }

    private Usuario validarSesion(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("usuario") == null) {
            resp.sendRedirect("index.jsp");
            return null;
        }
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (!DashboardService.puedeVerPanel(usuario, "despachos")) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Sin acceso al panel de despachos");
            return null;
        }
        return usuario;
    }
}
