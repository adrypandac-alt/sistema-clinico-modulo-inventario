package com.nexodist.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/historial")
public class HistorialServlet extends HttpServlet {

    private static final String COOKIE_NAME = "historialClinico";
    private static final String COOKIE_SEPARATOR = ";;";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String historial = obtenerCookie(req, COOKIE_NAME);
        List<String> transacciones = new ArrayList<>();

        if (historial != null && !historial.isEmpty()) {
            String[] compras = historial.split(COOKIE_SEPARATOR);
            for (String compra : compras) {
                transacciones.add(compra);
            }
        }

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setHeader("X-Modulo", "Cookies-Historial");
        resp.setHeader("X-Cookie-Leida", COOKIE_NAME);
        req.setAttribute("transacciones", transacciones);
        req.getRequestDispatcher("/historial.jsp").forward(req, resp);
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
