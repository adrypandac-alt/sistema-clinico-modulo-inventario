package com.nexodist.servlet;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.nexodist.model.Movimiento;
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

@WebServlet("/movimientos")
public class MovimientosServlet extends HttpServlet {

    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Usuario usuario = validarSesion(req, resp);
        if (usuario == null) return;
        if (!DashboardService.puedeVerPanel(usuario, "movimientos")) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Sin acceso a movimientos");
            return;
        }

        List<Movimiento> movimientos = (List<Movimiento>) getServletContext().getAttribute("movimientos");
        List<Usuario> usuarios = (List<Usuario>) getServletContext().getAttribute("usuarios");
        Map<String, String> rolesPorUsuario = construirRolesPorUsuario(usuarios);
        String usuarioFiltro = req.getParameter("usuario");
        if (!DashboardService.esAdministrador(usuario)) {
            usuarioFiltro = usuario.getNombre();
        }
        List<Movimiento> movimientosFiltrados = new ArrayList<>();
        Set<String> usuariosMovimiento = new LinkedHashSet<>();
        if (!DashboardService.esAdministrador(usuario)) {
            usuariosMovimiento.add(usuario.getNombre());
        }
        if (movimientos != null) {
            for (Movimiento movimiento : movimientos) {
                String nombreMovimiento = nombreUsuarioMovimiento(movimiento.getUsuario());
                if (DashboardService.esAdministrador(usuario) || usuario.getNombre().equalsIgnoreCase(nombreMovimiento)) {
                    usuariosMovimiento.add(nombreMovimiento);
                }
                boolean coincideUsuario = usuarioFiltro == null || usuarioFiltro.isBlank()
                        || usuarioFiltro.equalsIgnoreCase(nombreMovimiento)
                        || nombreMovimiento.toLowerCase().startsWith(usuarioFiltro.toLowerCase());
                if (coincideUsuario) {
                    movimientosFiltrados.add(movimiento);
                }
            }
        }
        req.setAttribute("movimientos", movimientosFiltrados);
        req.setAttribute("usuariosMovimiento", usuariosMovimiento);
        req.setAttribute("rolesMovimiento", rolesPorUsuario);
        req.setAttribute("usuarioFiltro", usuarioFiltro == null ? "" : usuarioFiltro);
        req.setAttribute("puedeVerTodosUsuarios", DashboardService.esAdministrador(usuario));
        req.setAttribute("puedeRegistrarMovimiento", DashboardService.puedeModificarInventario(usuario));
        List<Producto> productos = (List<Producto>) getServletContext().getAttribute("productos");
        req.setAttribute("alertasCount", DashboardService.contarAlertas(productos));

        resp.setHeader("X-Modulo", "Movimientos");
        req.getRequestDispatcher("/movimientos.jsp").forward(req, resp);
    }

    private Map<String, String> construirRolesPorUsuario(List<Usuario> usuarios) {
        Map<String, String> roles = new LinkedHashMap<>();
        if (usuarios == null) return roles;
        for (Usuario u : usuarios) {
            roles.put(u.getNombre(), DashboardService.rolVisible(u));
        }
        return roles;
    }

    private String nombreUsuarioMovimiento(String usuarioMovimiento) {
        if (usuarioMovimiento == null || usuarioMovimiento.isBlank()) {
            return "Sin usuario";
        }
        int rolInicio = usuarioMovimiento.lastIndexOf("(");
        if (rolInicio > 0) {
            return usuarioMovimiento.substring(0, rolInicio).trim();
        }
        return usuarioMovimiento.trim();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Usuario usuario = validarSesion(req, resp);
        if (usuario == null) return;
        if (!DashboardService.puedeVerPanel(usuario, "movimientos")) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Sin acceso a movimientos");
            return;
        }
        if (!DashboardService.puedeModificarInventario(usuario)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Usuario de solo lectura");
            return;
        }

        req.setCharacterEncoding("UTF-8");

        int productoId = Integer.parseInt(req.getParameter("productoId"));
        String tipo = req.getParameter("tipo");
        int cantidad = Integer.parseInt(req.getParameter("cantidad"));
        String motivo = req.getParameter("motivo");

        synchronized (getServletContext()) {
            Movimiento mov = DashboardService.registrarMovimiento(
                    getServletContext(), productoId, tipo, cantidad, motivo,
                    usuario.getNombre() + " (" + DashboardService.rolVisible(usuario) + ")");

            if (mov == null) {
                resp.sendRedirect("movimientos?error=1");
                return;

            }
            List<Producto> productos = (List<Producto>) getServletContext().getAttribute("productos");
            List<Movimiento> movimientos = (List<Movimiento>) getServletContext().getAttribute("movimientos");
            DatosStorage.guardarProductos(productos);
            DatosStorage.guardarMovimientos(movimientos);
        }

        resp.sendRedirect("movimientos");
    }

    private Usuario validarSesion(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("usuario") == null) {
            resp.sendRedirect("index.jsp");
            return null;
        }
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        return usuario;
    }
}
