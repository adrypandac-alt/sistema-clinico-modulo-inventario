package com.nexodist.servlet;

import com.nexodist.model.CabeceraInfo;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/estado")
public class EstadoCabeceraServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        String tipo = request.getParameter("tipo");
        String acceptEncoding = request.getHeader("Accept-Encoding");
        boolean aceptaGzip = acceptEncoding != null && acceptEncoding.toLowerCase().contains("gzip");

        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Vary", "Accept-Encoding");
        response.setHeader("X-Compresion-Soportada", aceptaGzip ? "gzip" : "no-detectada");
        response.setHeader("X-Modulo", "Estatus-y-Cabeceras");
        response.setHeader("X-Empresa", "Sistema-Clinico");

        prepararEstado(tipo, request, response);

        List<CabeceraInfo> entradas = new ArrayList<>();
        entradas.add(new CabeceraInfo("Accept-Encoding", valorSeguro(acceptEncoding),
                "Indica los algoritmos de compresion aceptados por el cliente HTTP."));
        entradas.add(new CabeceraInfo("User-Agent", valorSeguro(request.getHeader("User-Agent")),
                "Identifica de forma general el navegador que realiza la peticion."));
        entradas.add(new CabeceraInfo("Accept-Language", valorSeguro(request.getHeader("Accept-Language")),
                "Indica los idiomas preferidos por el navegador."));

        request.setAttribute("entradas", entradas);
        request.setAttribute("aceptaGzip", aceptaGzip);
        request.getRequestDispatcher("/estado.jsp").forward(request, response);
    }

    private void prepararEstado(String tipo, HttpServletRequest request, HttpServletResponse response) {
        if ("error".equalsIgnoreCase(tipo)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setHeader("X-Estado-Operacion", "Error-Controlado");
            request.setAttribute("codigoEstado", HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("mensaje", "Solicitud incorrecta generada para probar HTTP 400");
            return;
        }

        if ("no-encontrado".equalsIgnoreCase(tipo)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setHeader("X-Estado-Operacion", "Recurso-No-Encontrado");
            request.setAttribute("codigoEstado", HttpServletResponse.SC_NOT_FOUND);
            request.setAttribute("mensaje", "Recurso no encontrado generado para probar HTTP 404");
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("X-Estado-Operacion", "Consulta-Correcta");
        request.setAttribute("codigoEstado", HttpServletResponse.SC_OK);
        request.setAttribute("mensaje", "Respuesta correcta generada para probar HTTP 200");
    }

    private String valorSeguro(String valor) {
        return valor == null || valor.isBlank() ? "No enviada" : valor;
    }
}
