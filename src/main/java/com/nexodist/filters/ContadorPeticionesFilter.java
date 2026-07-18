package com.nexodist.filters;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.nexodist.listeners.AppContextListener;

@WebFilter("/*")
public class ContadorPeticionesFilter implements Filter {

    @Override
    @SuppressWarnings("unchecked")
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        ServletContext contexto = request.getServletContext();

        synchronized (contexto) {
            Integer totalPeticiones = (Integer) contexto.getAttribute("totalPeticiones");
            int nuevoTotal = totalPeticiones == null ? 1 : totalPeticiones + 1;
            contexto.setAttribute("totalPeticiones", nuevoTotal);

            List<String> historiaEventos = (List<String>) contexto.getAttribute("historiaEventos");
            if (historiaEventos == null) {
                historiaEventos = new ArrayList<>();
                contexto.setAttribute("historiaEventos", historiaEventos);
            }

            if (request instanceof HttpServletRequest httpRequest) {
                historiaEventos.add(LocalDateTime.now() + " - Peticion " + nuevoTotal + " " + httpRequest.getMethod() + " " + httpRequest.getRequestURI());

                Map<String, Integer> visitas = obtenerVisitas(contexto);
                if (visitas != null) {
                    visitas.merge(httpRequest.getRequestURI(), 1, Integer::sum);
                }
            }
        }

        if (response instanceof HttpServletResponse httpResponse) {
            httpResponse.setHeader("X-App", "Sistema Clínico-Inventario");
            httpResponse.setHeader("X-Filtro", "ContadorPeticionesFilter");
        }

        chain.doFilter(request, response);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> obtenerVisitas(ServletContext contexto) {
        return (Map<String, Integer>) contexto.getAttribute(AppContextListener.ATRIBUTO_VISITAS);
    }
}
