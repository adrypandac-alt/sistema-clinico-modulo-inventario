package com.nexodist.servlet;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.nexodist.model.ActividadCuenta.Tipo;
import com.nexodist.model.Usuario;
import com.nexodist.service.ActividadService;
import com.nexodist.service.DashboardService;
import com.nexodist.storage.DatosStorage;
import com.nexodist.storage.SeguridadStorage;
import com.nexodist.util.PasswordUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final String COOKIE_ULTIMO_USUARIO = "ultimoUsuarioSistemaClinico";
    private static final int COOKIE_MAX_AGE = 7 * 24 * 60 * 60;

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String correo = req.getParameter("usuario");
        if (esVacio(correo)) correo = req.getParameter("correo");
        String clave = req.getParameter("clave");
        String ip    = ActividadService.obtenerIp(req);
        String agente = req.getHeader("User-Agent");

        // ── Validación básica de campos ──────────────────────────────
        if (esVacio(correo) || esVacio(clave)) {
            req.setAttribute("error", "Complete usuario y contraseña para iniciar sesión.");
            req.getRequestDispatcher("/index.jsp").forward(req, resp);
            return;
        }

        final String correoLimpio = correo.trim();

        // ── Bloqueo por fuerza bruta ─────────────────────────────────
        // Si la IP o el correo acumularon >= MAX_INTENTOS_FALLIDOS en los
        // últimos VENTANA_INTENTOS_MINUTOS minutos, bloqueamos sin intentar.
        if (SeguridadStorage.estaBloqueado(ip, correoLimpio)) {
            ActividadService.registrarError(getServletContext(), correoLimpio,
                    "Acceso bloqueado temporalmente — demasiados intentos fallidos", ip);
            req.setAttribute("error",
                    "Acceso bloqueado temporalmente por múltiples intentos fallidos. " +
                    "Espere " + SeguridadStorage.VENTANA_INTENTOS_MINUTOS + " minutos e intente de nuevo.");
            req.setAttribute("bloqueado", true);
            req.getRequestDispatcher("/index.jsp").forward(req, resp);
            return;
        }

        // ── Buscar usuario y verificar contraseña ────────────────────
        List<Usuario> usuarios = (List<Usuario>) getServletContext().getAttribute("usuarios");
        Usuario usuarioEncontrado = buscarUsuario(usuarios, correoLimpio, clave.trim());

        if (usuarioEncontrado == null) {
            // Registrar intento fallido en BD y en auditoría de actividad
            SeguridadStorage.registrarIntento(correoLimpio, ip, agente, false);
            ActividadService.registrarError(getServletContext(), correoLimpio,
                    "Intento de login fallido — credenciales incorrectas", ip);

            int restantes = SeguridadStorage.MAX_INTENTOS_FALLIDOS
                    - SeguridadStorage.contarIntentosFallidos(ip, correoLimpio);
            String msgRestantes = !SeguridadStorage.bloqueoIntentosHabilitado()
                    ? ""
                    : restantes > 0
                        ? " Intentos restantes antes del bloqueo: " + restantes + "."
                        : " Acceso bloqueado. Espere " + SeguridadStorage.VENTANA_INTENTOS_MINUTOS + " min.";

            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setHeader("X-Estado-Operacion", "Login-Fallido");
            req.setAttribute("error", "Credenciales incorrectas." + msgRestantes);
            req.getRequestDispatcher("/index.jsp").forward(req, resp);
            return;
        }

        // ── Login exitoso ────────────────────────────────────────────
        SeguridadStorage.registrarIntento(correoLimpio, ip, agente, true);

        // Actualizar último acceso
        synchronized (getServletContext()) {
            for (Usuario u : usuarios) {
                if (u.getCorreo().equalsIgnoreCase(usuarioEncontrado.getCorreo())) {
                    u.setUltimoAcceso("Hoy, " + java.time.LocalTime.now().format(
                            java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
                    DatosStorage.guardarUsuarios(usuarios);
                    usuarioEncontrado = u;
                    break;
                }
            }
        }

        // Invalidar sesión anterior e iniciar una nueva
        // (previene session fixation attacks)
        HttpSession sesionAnterior = req.getSession(false);
        if (sesionAnterior != null) {
            SeguridadStorage.invalidarSesion(sesionAnterior.getId());
            sesionAnterior.invalidate();
        }
        HttpSession session = req.getSession(true);
        session.setAttribute("usuario", usuarioEncontrado);

        // Registrar sesión en la tabla sesion_usuario
        // (id_usuario = 0 en modo memoria, la tabla lo filtra silenciosamente)
        SeguridadStorage.registrarSesion(session.getId(), 0, ip, agente);

        // Cookie de último usuario (solo correo, HttpOnly)
        Cookie ultimoUsuario = new Cookie(COOKIE_ULTIMO_USUARIO,
                URLEncoder.encode(usuarioEncontrado.getCorreo(), StandardCharsets.UTF_8));
        ultimoUsuario.setMaxAge(COOKIE_MAX_AGE);
        ultimoUsuario.setPath(req.getContextPath().isEmpty() ? "/" : req.getContextPath());
        ultimoUsuario.setHttpOnly(true);
        resp.addCookie(ultimoUsuario);

        // Registrar en auditoría de actividad
        ActividadService.registrar(getServletContext(), usuarioEncontrado,
                Tipo.LOGIN, "Inicio de sesión exitoso", ip);

        // Limpieza periódica de intentos antiguos (cada login exitoso)
        SeguridadStorage.limpiarIntentosAntiguos();

        resp.setStatus(HttpServletResponse.SC_SEE_OTHER);
        resp.setHeader("X-Modulo", "Autenticacion");
        resp.setHeader("X-Estado-Operacion", "Login-Correcto");
        resp.setHeader("Location",
                req.getContextPath() + "/" + DashboardService.rutaInicio(usuarioEncontrado));
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "Método no permitido para login.");
    }

    /**
     * Busca el usuario por correo y verifica la contraseña.
     * Soporta tanto contraseñas en texto plano (legacy/demo)
     * como hashes BCrypt (modo seguro).
     */
    private Usuario buscarUsuario(List<Usuario> usuarios, String correo, String clave) {
        if (usuarios == null) return null;
        for (Usuario u : usuarios) {
            if (!u.isActivo()) continue;
            if (!u.getCorreo().equalsIgnoreCase(correo)) continue;

            String hashAlmacenado = u.getClave();
            boolean autenticado;

            if (PasswordUtil.esHash(hashAlmacenado)) {
                // Contraseña hasheada con BCrypt — comparación segura
                autenticado = PasswordUtil.verificar(clave, hashAlmacenado);
            } else {
                // Contraseña en texto plano (demo / migración) — comparación directa
                // y migración automática al hash en la misma operación
                autenticado = hashAlmacenado != null && hashAlmacenado.equals(clave);
                if (autenticado) {
                    // Migrar silenciosamente a BCrypt
                    u.setClave(PasswordUtil.hashear(clave));
                }
            }

            if (autenticado) return u;
        }
        return null;
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
