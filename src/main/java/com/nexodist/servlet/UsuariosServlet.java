package com.nexodist.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.nexodist.model.ActividadCuenta.Tipo;
import com.nexodist.model.Producto;
import com.nexodist.model.Rol;
import com.nexodist.model.Usuario;
import com.nexodist.service.ActividadService;
import com.nexodist.service.DashboardService;
import com.nexodist.storage.DatosStorage;
import com.nexodist.storage.SeguridadStorage;
import com.nexodist.util.PasswordUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/usuarios")
public class UsuariosServlet extends HttpServlet {

    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Usuario usuario = validarAdmin(req, resp);
        if (usuario == null) return;

        List<Usuario> usuarios = (List<Usuario>) getServletContext().getAttribute("usuarios");
        List<Producto> productos = (List<Producto>) getServletContext().getAttribute("productos");
        req.setAttribute("usuariosLista", usuarios);
        List<Rol> roles = construirRolesDisponibles(usuarios);
        req.setAttribute("roles", roles);
        req.setAttribute("rolesDisponibles", roles.stream().map(Rol::getNombre).toList());
        req.setAttribute("alertasCount", DashboardService.contarAlertas(productos));

        resp.setHeader("X-Modulo", "Usuarios");
        req.getRequestDispatcher("/usuarios.jsp").forward(req, resp);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Usuario usuario = validarAdmin(req, resp);
        if (usuario == null) return;

        req.setCharacterEncoding("UTF-8");

        String accion = req.getParameter("accion");
        String correo = req.getParameter("correo");

        synchronized (getServletContext()) {
            List<Usuario> usuarios = (List<Usuario>) getServletContext().getAttribute("usuarios");

            if ("crearRol".equals(accion) || "modificarRol".equals(accion)) {
                String nombre = normalizarRol(req.getParameter("nombreRol"));
                String original = "modificarRol".equals(accion) ? req.getParameter("rolOriginal") : null;
                String panelesRol = normalizarPaneles(req.getParameterValues("panelesRol"));
                boolean interactua = req.getParameter("puedeInteractuar") != null;
                if (nombre == null) {
                    resp.sendRedirect("usuarios?error=rol"); return;
                }
                Rol rolGuardado = new Rol(nombre, panelesRol, interactua);
                DatosStorage.guardarRol(original, rolGuardado);
                guardarRolEnMemoria(original, rolGuardado);
                if (original != null && !original.equalsIgnoreCase(nombre)) {
                    for (Usuario u : usuarios) if (original.equalsIgnoreCase(u.getRol())) u.setRol(nombre);
                    DatosStorage.guardarUsuarios(usuarios);
                }
                resp.sendRedirect("usuarios?rolGuardado=1"); return;
            }
            if ("eliminarRol".equals(accion)) {
                boolean eliminado = DatosStorage.eliminarRol(req.getParameter("nombreRol"));
                resp.sendRedirect("usuarios?" + (eliminado ? "rolEliminado=1" : "error=rolEnUso")); return;
            }

            if ("crear".equals(accion)) {
                String nombre = nombreCompleto(req);
                String clave = req.getParameter("clave");
                String claveConfirmacion = req.getParameter("claveConfirmacion");
                String rol = normalizarRol(req.getParameter("rol"));
                String area = req.getParameter("area");
                String paneles = normalizarPaneles(req.getParameterValues("paneles"));

                if (nombre == null || nombre.isBlank() || correo == null || correo.isBlank()
                        || clave == null || clave.isBlank() || rol == null
                        || !clave.equals(claveConfirmacion)) {
                    resp.sendRedirect("usuarios?error=campos");
                    return;
                }
                if (clave.length() < PasswordUtil.LONGITUD_MINIMA) {
                    resp.sendRedirect("usuarios?error=clavecorta");
                    return;
                }
                boolean existe = usuarios.stream()
                        .anyMatch(u -> u.getCorreo().equalsIgnoreCase(correo.trim()));
                if (existe) {
                    resp.sendRedirect("usuarios?error=correo");
                    return;
                }

                // Hashear contraseña antes de guardar
                String claveHash = PasswordUtil.hashear(clave.trim());

                Usuario nuevo = new Usuario(nombre.trim(), correo.trim(), claveHash, rol,
                        normalizarArea(area, rol), true, "Sin accesos", paneles);
                nuevo.setTelefono(limpiarTelefono(req.getParameter("telefono")));
                usuarios.add(nuevo);
                DatosStorage.guardarUsuarios(usuarios);
                ActividadService.registrar(getServletContext(), usuario, Tipo.ACCION,
                        "Usuario creado: " + nuevo.getNombre() + " - " + nuevo.getRol(),
                        ActividadService.obtenerIp(req));
                resp.sendRedirect("usuarios?creado=1");
                return;
            }

            if ("modificar".equals(accion)) {
                String correoOriginal = req.getParameter("correoOriginal");
                String nombre = nombreCompleto(req);
                String nuevoCorreo = req.getParameter("correo");
                String clave = req.getParameter("clave");
                String claveConfirmacion = req.getParameter("claveConfirmacion");
                String rol = normalizarRol(req.getParameter("rol"));
                String area = req.getParameter("area");
                String paneles = normalizarPaneles(req.getParameterValues("paneles"));
                boolean activo = Boolean.parseBoolean(req.getParameter("activo"));

                if (correoOriginal == null || correoOriginal.isBlank()
                        || nombre == null || nombre.isBlank()
                        || nuevoCorreo == null || nuevoCorreo.isBlank()
                        || clave == null || clave.isBlank()
                        || rol == null
                        || !clave.equals(claveConfirmacion)) {
                    resp.sendRedirect("usuarios?error=campos");
                    return;
                }

                boolean correoDuplicado = usuarios.stream()
                        .anyMatch(u -> !u.getCorreo().equalsIgnoreCase(correoOriginal.trim())
                                && u.getCorreo().equalsIgnoreCase(nuevoCorreo.trim()));
                if (correoDuplicado) {
                    resp.sendRedirect("usuarios?error=correo");
                    return;
                }

                for (Usuario u : usuarios) {
                    if (u.getCorreo().equalsIgnoreCase(correoOriginal.trim())) {
                        String resumenAnterior = u.getNombre() + " - " + u.getRol();
                        u.setNombre(nombre.trim());
                        u.setTelefono(limpiarTelefono(req.getParameter("telefono")));
                        u.setCorreo(nuevoCorreo.trim());
                        u.setClave(clave.trim());
                        u.setRol(rol);
                        u.setArea(normalizarArea(area, rol));
                        u.setPanelesPermitidos(paneles);
                        u.setActivo(activo);

                        if (usuario.getCorreo().equalsIgnoreCase(correoOriginal.trim())) {
                            req.getSession().setAttribute("usuario", u);
                        }
                        ActividadService.registrar(getServletContext(), usuario, Tipo.ACCION,
                                "Usuario modificado: " + resumenAnterior + " -> " + u.getNombre() + " - " + u.getRol(),
                                ActividadService.obtenerIp(req));
                        break;
                    }
                }
                DatosStorage.guardarUsuarios(usuarios);
                resp.sendRedirect("usuarios?modificado=1");
                return;
            }

            Usuario usuarioParaEliminar = null;
            for (Usuario u : usuarios) {
                if (u.getCorreo().equals(correo)) {
                    if ("activar".equals(accion)) {
                        u.setActivo(true);
                        ActividadService.registrar(getServletContext(), usuario, Tipo.ACCION,
                                "Usuario activado: " + u.getNombre() + " (" + u.getCorreo() + ")",
                                ActividadService.obtenerIp(req));
                    } else if ("desactivar".equals(accion)) {
                        u.setActivo(false);
                        ActividadService.registrar(getServletContext(), usuario, Tipo.ACCION,
                                "Usuario desactivado: " + u.getNombre() + " (" + u.getCorreo() + ")",
                                ActividadService.obtenerIp(req));
                    } else if ("rol".equals(accion)) {
                        String nuevoRol = normalizarRol(req.getParameter("rol"));
                        if (nuevoRol != null) {
                            String rolAnterior = u.getRol();
                            u.setRol(nuevoRol);
                            ActividadService.registrar(getServletContext(), usuario, Tipo.ACCION,
                                    "Cambio de rol: " + u.getNombre() + " → " + rolAnterior + " ➜ " + nuevoRol,
                                    ActividadService.obtenerIp(req));
                        }
                    } else if ("eliminar".equals(accion)
                            && !u.getCorreo().equalsIgnoreCase(usuario.getCorreo())) {
                        ActividadService.registrar(getServletContext(), usuario, Tipo.ACCION,
                                "Usuario eliminado: " + u.getNombre() + " (" + u.getCorreo() + ")",
                                ActividadService.obtenerIp(req));
                        usuarioParaEliminar = u;
                    }
                    break;
                }
            }
            if (usuarioParaEliminar != null) {
                usuarios.remove(usuarioParaEliminar);
            }
            DatosStorage.guardarUsuarios(usuarios);
        }

        resp.sendRedirect("usuarios");
    }

    private String normalizarRol(String rol) {
        if (rol == null) return null;
        String valor = rol.trim();
        if (valor.isBlank()) return null;
        if ("Administrador".equalsIgnoreCase(valor)) return "Administrador";
        if ("Operador".equalsIgnoreCase(valor) || "Operativo".equalsIgnoreCase(valor)) return "Operativo";
        if ("Visualizador".equalsIgnoreCase(valor)) return "Visualizador";
        return valor.length() > 80 ? valor.substring(0, 80) : valor;
    }

    private String normalizarPaneles(String[] paneles) {
        if (paneles == null || paneles.length == 0) return "";
        Set<String> permitidos = new LinkedHashSet<>();
        for (String panel : paneles) {
            if (panel == null) continue;
            String valor = panel.trim().toLowerCase();
            if (List.of("dashboard", "productos", "proveedor", "movimientos", "alertas", "usuarios", "despachos")
                    .contains(valor)) {
                permitidos.add(valor);
            }
        }
        return String.join(",", permitidos);
    }

    private List<String> obtenerRolesDisponibles(List<Usuario> usuarios) {
        Set<String> roles = new LinkedHashSet<>(List.of("Administrador", "Operativo", "Visualizador"));
        if (usuarios != null) {
            for (Usuario u : usuarios) {
                if (u.getRol() != null && !u.getRol().isBlank()) roles.add(u.getRol());
            }
        }
        return new ArrayList<>(roles);
    }

    @SuppressWarnings("unchecked")
    private List<Rol> construirRolesDisponibles(List<Usuario> usuarios) {
        List<Rol> resultado = new ArrayList<>();
        agregarRolSiFalta(resultado, new Rol("Administrador",
                "dashboard,productos,proveedor,movimientos,alertas,usuarios,despachos", true));
        agregarRolSiFalta(resultado, new Rol("Operativo",
                "dashboard,productos,movimientos,alertas,despachos", true));
        agregarRolSiFalta(resultado, new Rol("Visualizador", "dashboard", false));

        for (Rol rol : DatosStorage.cargarRoles()) {
            if (!esRolEliminado(rol.getNombre())) agregarRolSiFalta(resultado, rol);
        }
        List<Rol> rolesMemoria = (List<Rol>) getServletContext().getAttribute("rolesMemoria");
        if (rolesMemoria != null) for (Rol rol : rolesMemoria) agregarRolSiFalta(resultado, rol);
        if (usuarios != null) for (Usuario u : usuarios) {
            if (u.getRol() != null && !u.getRol().isBlank() && !esRolEliminado(u.getRol())) {
                agregarRolSiFalta(resultado, new Rol(u.getRol(), u.getPanelesPermitidos(), true));
            }
        }
        return resultado;
    }

    private void agregarRolSiFalta(List<Rol> roles, Rol nuevo) {
        for (Rol rol : roles) if (rol.getNombre().equalsIgnoreCase(nuevo.getNombre())) return;
        roles.add(nuevo);
    }

    private boolean esRolEliminado(String nombre) {
        return "Farmacia".equalsIgnoreCase(nombre) || "Ventas".equalsIgnoreCase(nombre)
                || "Vendedor".equalsIgnoreCase(nombre);
    }

    @SuppressWarnings("unchecked")
    private void guardarRolEnMemoria(String original, Rol nuevo) {
        List<Rol> roles = (List<Rol>) getServletContext().getAttribute("rolesMemoria");
        if (roles == null) {
            roles = new ArrayList<>();
            getServletContext().setAttribute("rolesMemoria", roles);
        }
        if (original != null) roles.removeIf(r -> r.getNombre().equalsIgnoreCase(original));
        roles.removeIf(r -> r.getNombre().equalsIgnoreCase(nuevo.getNombre()));
        roles.add(nuevo);
    }

    private String normalizarArea(String area, String rol) {
        if (area == null || area.isBlank()) {
            return "Operativo".equalsIgnoreCase(rol) ? "Bodega" : "Administración";
        }
        if ("Administración".equalsIgnoreCase(area) || "Administracion".equalsIgnoreCase(area)) return "Administración";
        if ("Bodega".equalsIgnoreCase(area)) return "Bodega";
        return "Bodega";
    }

    private String nombreCompleto(HttpServletRequest req) {
        String nombres = req.getParameter("nombres");
        String apellidos = req.getParameter("apellidos");
        if (nombres == null) nombres = req.getParameter("nombre");
        return ((nombres == null ? "" : nombres.trim()) + " " + (apellidos == null ? "" : apellidos.trim())).trim();
    }

    private String limpiarTelefono(String telefono) {
        if (telefono == null) return "";
        String valor = telefono.replaceAll("[^0-9+() -]", "").trim();
        return valor.length() > 25 ? valor.substring(0, 25) : valor;
    }

    private Usuario validarAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("usuario") == null) {
            resp.sendRedirect("index.jsp");
            return null;
        }
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (!DashboardService.puedeGestionarUsuarios(usuario)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Sin acceso a usuarios");
            return null;
        }
        return usuario;
    }
}
