package com.nexodist.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.nexodist.model.ActividadCuenta;
import com.nexodist.model.ActividadCuenta.Tipo;
import com.nexodist.model.Usuario;

import jakarta.servlet.ServletContext;

/**
 * Registra y consulta la actividad de cuentas de usuario.
 * Todos los métodos son thread-safe vía synchronized sobre el ServletContext.
 */
public final class ActividadService {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String ATTR = "actividadCuentas";
    private static final int MAX_REGISTROS = 500;

    private ActividadService() {}

    public static void registrar(ServletContext ctx, Usuario usuario,
                                 Tipo tipo, String descripcion, String ip) {
        if (ctx == null || usuario == null) return;
        synchronized (ctx) {
            List<ActividadCuenta> lista = obtenerLista(ctx);
            int nuevoId = lista.isEmpty() ? 1 : lista.get(lista.size() - 1).getId() + 1;
            ActividadCuenta reg = new ActividadCuenta(
                    nuevoId,
                    usuario.getNombre(),
                    usuario.getCorreo(),
                    usuario.getRol(),
                    tipo,
                    descripcion,
                    ip != null ? ip : "—",
                    LocalDateTime.now().format(FMT)
            );
            lista.add(reg);
            // Limitar tamaño para no crecer infinito
            if (lista.size() > MAX_REGISTROS) {
                lista.remove(0);
            }
        }
    }

    public static void registrarAccion(ServletContext ctx, Usuario usuario,
                                       String descripcion, String ip) {
        registrar(ctx, usuario, Tipo.ACCION, descripcion, ip);
    }

    public static void registrarError(ServletContext ctx, String correoIntento,
                                      String descripcion, String ip) {
        if (ctx == null) return;
        synchronized (ctx) {
            List<ActividadCuenta> lista = obtenerLista(ctx);
            int nuevoId = lista.isEmpty() ? 1 : lista.get(lista.size() - 1).getId() + 1;
            Usuario dummy = new Usuario("(desconocido)", correoIntento, "", "—");
            ActividadCuenta reg = new ActividadCuenta(
                    nuevoId, "(desconocido)", correoIntento, "—",
                    Tipo.ERROR, descripcion,
                    ip != null ? ip : "—",
                    LocalDateTime.now().format(FMT)
            );
            lista.add(reg);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<ActividadCuenta> obtenerLista(ServletContext ctx) {
        List<ActividadCuenta> lista = (List<ActividadCuenta>) ctx.getAttribute(ATTR);
        if (lista == null) {
            lista = new ArrayList<>();
            ctx.setAttribute(ATTR, lista);
        }
        return lista;
    }

    @SuppressWarnings("unchecked")
    public static List<ActividadCuenta> obtener(ServletContext ctx) {
        if (ctx == null) return new ArrayList<>();
        List<ActividadCuenta> lista = (List<ActividadCuenta>) ctx.getAttribute(ATTR);
        return lista != null ? lista : new ArrayList<>();
    }

    public static String obtenerIp(jakarta.servlet.http.HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = req.getRemoteAddr();
        // Si viene una lista de IPs tomar la primera
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        return ip;
    }
}
