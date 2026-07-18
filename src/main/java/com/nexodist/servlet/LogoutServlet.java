package com.nexodist.servlet;

import java.io.IOException;

import com.nexodist.model.ActividadCuenta.Tipo;
import com.nexodist.model.Usuario;
import com.nexodist.service.ActividadService;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (usuario != null) {
                ActividadService.registrar(getServletContext(), usuario,
                        Tipo.LOGOUT,
                        "Cierre de sesión",
                        ActividadService.obtenerIp(req));
            }
            session.invalidate();
        }

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setHeader("X-Modulo", "Autenticacion");
        resp.setHeader("X-Estado-Operacion", "Logout");
        resp.sendRedirect("index.jsp");
    }
}
