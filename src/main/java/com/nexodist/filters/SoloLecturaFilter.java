package com.nexodist.filters;

import java.io.IOException;
import com.nexodist.model.Rol;
import com.nexodist.model.Usuario;
import com.nexodist.storage.DatosStorage;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;

@WebFilter("/*")
public class SoloLecturaFilter implements Filter {
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        HttpSession sesion = req.getSession(false);
        Usuario usuario = sesion == null ? null : (Usuario) sesion.getAttribute("usuario");
        boolean interactua = true;
        if (usuario != null) {
            for (Rol rol : DatosStorage.cargarRoles()) if (rol.getNombre().equalsIgnoreCase(usuario.getRol())) {
                interactua = rol.isPuedeInteractuar(); break;
            }
            req.setAttribute("puedeInteractuar", interactua);
        }
        if (usuario != null && !interactua && !"GET".equalsIgnoreCase(req.getMethod())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Este rol tiene acceso de solo lectura"); return;
        }
        chain.doFilter(request, response);
    }
}
