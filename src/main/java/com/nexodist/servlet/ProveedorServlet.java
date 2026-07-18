package com.nexodist.servlet;

import com.nexodist.model.Producto;
import com.nexodist.model.Proveedor;
import com.nexodist.model.Usuario;
import com.nexodist.service.DashboardService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/proveedor")
public class ProveedorServlet extends HttpServlet {

    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!puedeEditar(request, response)) return;

        List<Proveedor> proveedores = obtenerProveedores();
        List<Producto> productos = (List<Producto>) getServletContext().getAttribute("productos");
        request.setAttribute("proveedores", proveedores);
        request.setAttribute("alertasCount", DashboardService.contarAlertas(productos));
        request.getRequestDispatcher("/proveedor.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!puedeEditar(request, response)) return;

        request.setCharacterEncoding("UTF-8");
        String accion = request.getParameter("accion");
        String ruc = request.getParameter("ruc");

        synchronized (getServletContext()) {
            List<Proveedor> proveedores = obtenerProveedores();

            if ("eliminar".equals(accion)) {
                proveedores.removeIf(p -> p.getRuc().equals(ruc));
                response.sendRedirect("proveedor?eliminado=1");
                return;
            }

            String nombre = request.getParameter("nombre");
            String contacto = request.getParameter("contacto");
            String telefono = request.getParameter("telefono");
            String correo = request.getParameter("correo");
            String direccion = request.getParameter("direccion");
            boolean activo = "Activo".equalsIgnoreCase(request.getParameter("estado"));
            String observaciones = request.getParameter("observaciones");

            String error = validar(ruc, nombre, contacto, telefono, correo);
            if (error != null) {
                request.setAttribute("mensajeError", error);
                request.setAttribute("proveedores", proveedores);
                request.getRequestDispatcher("/proveedor.jsp").forward(request, response);
                return;
            }

            Proveedor existente = buscarProveedor(proveedores, ruc.trim());
            if ("editar".equals(accion)) {
                if (existente != null) {
                    existente.setNombre(nombre.trim());
                    existente.setContacto(contacto.trim());
                    existente.setTelefono(telefono.trim());
                    existente.setCorreo(correo.trim());
                    existente.setDireccion(valorOpcional(direccion));
                    existente.setActivo(activo);
                    existente.setObservaciones(valorOpcional(observaciones));
                }
                response.sendRedirect("proveedor?actualizado=1");
                return;
            }

            if (existente != null) {
                request.setAttribute("mensajeError", "El proveedor ya existe.");
                request.setAttribute("proveedores", proveedores);
                request.getRequestDispatcher("/proveedor.jsp").forward(request, response);
                return;
            }

            proveedores.add(new Proveedor(ruc.trim(), nombre.trim(), contacto.trim(), telefono.trim(),
                    correo.trim(), valorOpcional(direccion), activo, valorOpcional(observaciones)));
        }

        response.sendRedirect("proveedor?creado=1");
    }

    @SuppressWarnings("unchecked")
    private List<Proveedor> obtenerProveedores() {
        List<Proveedor> proveedores = (List<Proveedor>) getServletContext().getAttribute("proveedores");
        if (proveedores == null) {
            proveedores = new ArrayList<>();
            proveedores.add(new Proveedor("1790012345001", "MediSupply Ecuador", "Paula Ortega",
                    "0991234567", "proveedores@medisupply.ec", "Av. Amazonas N34-120", true,
                    "Proveedor principal de insumos médicos."));
            proveedores.add(new Proveedor("1790098765001", "BioSalud Laboratorios", "Andrés Molina",
                    "0987654321", "contacto@biosalud.ec", "", true,
                    "Medicamentos y cadena de frío."));
            getServletContext().setAttribute("proveedores", proveedores);
        }
        return proveedores;
    }

    private Proveedor buscarProveedor(List<Proveedor> proveedores, String ruc) {
        for (Proveedor proveedor : proveedores) {
            if (proveedor.getRuc().equals(ruc)) return proveedor;
        }
        return null;
    }

    private String validar(String ruc, String nombre, String contacto, String telefono, String correo) {
        if (estaVacio(ruc) || estaVacio(nombre) || estaVacio(contacto)
                || estaVacio(telefono) || estaVacio(correo)) {
            return "Complete los campos obligatorios.";
        }
        if (!ruc.matches("\\d{10}|\\d{13}")) {
            return "El RUC debe tener 10 o 13 digitos numericos.";
        }
        if (!telefono.matches("\\d{7,10}")) {
            return "El telefono debe tener entre 7 y 10 digitos numericos.";
        }
        if (!correo.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            return "El correo no tiene un formato valido.";
        }
        return null;
    }

    private boolean estaVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private String valorOpcional(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private boolean puedeEditar(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getSession(false) == null
                || request.getSession(false).getAttribute("usuario") == null) {
            response.sendRedirect("index.jsp");
            return false;
        }
        Usuario usuario = (Usuario) request.getSession(false).getAttribute("usuario");
        if (!DashboardService.puedeVerPanel(usuario, "proveedor")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Sin acceso a proveedores");
            return false;
        }
        return true;
    }
}
