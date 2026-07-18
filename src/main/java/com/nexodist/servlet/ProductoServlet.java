package com.nexodist.servlet;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.nexodist.model.Producto;
import com.nexodist.model.Usuario;
import com.nexodist.service.DashboardService;
import com.nexodist.storage.DatosStorage;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/productos")
public class ProductoServlet extends HttpServlet {

    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<Producto> productos = (List<Producto>) getServletContext().getAttribute("productos");

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setHeader("X-Modulo", "Gestion-Productos");
        resp.setHeader("X-Metodo-HTTP", "GET");
        req.setAttribute("productos", productos);
        req.getRequestDispatcher("/productos.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Usuario usuario = req.getSession(false) == null ? null
                : (Usuario) req.getSession(false).getAttribute("usuario");
        if (!DashboardService.puedeModificarInventario(usuario)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Usuario de solo lectura");
            return;
        }
        procesarProducto(req, resp, "POST");
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Usuario usuario = req.getSession(false) == null ? null
                : (Usuario) req.getSession(false).getAttribute("usuario");
        if (!DashboardService.puedeModificarInventario(usuario)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Usuario de solo lectura");
            return;
        }
        procesarProducto(req, resp, "PUT");
    }

    @SuppressWarnings("unchecked")
    private void procesarProducto(HttpServletRequest req, HttpServletResponse resp, String metodoHttp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html;charset=UTF-8");

        resp.setHeader("X-Modulo", "Gestion-Productos");
        resp.setHeader("X-Metodo-HTTP", metodoHttp);
        resp.setHeader("X-Servidor", "Sistema Clínico");
        resp.setHeader("X-Tipo-Operacion", "Registro producto");

        String nombre = req.getParameter("nombre");
        String categoria = req.getParameter("categoria");
        String precioTexto = req.getParameter("precio");
        String stockTexto = req.getParameter("stock");
        String deposito = req.getParameter("deposito");

        String error = validarDatos(nombre, categoria, precioTexto, stockTexto, deposito);

        if (error != null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setHeader("X-Estado-Operacion", "Error-Validacion");
            req.setAttribute("mensajeError", error);
            req.setAttribute("codigoEstado", HttpServletResponse.SC_BAD_REQUEST);
            req.getRequestDispatcher("/productoError.jsp").forward(req, resp);
            return;
        }

        List<Producto> productos = (List<Producto>) getServletContext().getAttribute("productos");
        if (productos == null) {
            productos = new ArrayList<>();
            getServletContext().setAttribute("productos", productos);
        }

        double precio = Double.parseDouble(precioTexto);
        int stock = Integer.parseInt(stockTexto);
        int nuevoId = productos.size() + 1;
        Producto producto = new Producto(nuevoId, "SKU-" + nuevoId, nombre.trim(), categoria.trim(),
                "Proveedor general", precio, stock, Math.max(10, stock / 3), deposito.trim());

        synchronized (getServletContext()) {
            productos.add(producto);
            DatosStorage.guardarProductos(productos);
            agregarEvento("Producto registrado: " + producto.getNombre() + " $" + producto.getPrecio());
        }

        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.setHeader("X-Estado-Operacion", "Producto-Creado");
        resp.setHeader("X-Nombre", producto.getNombre());
        resp.setHeader("X-Categoria-Producto", producto.getCategoria());

        req.setAttribute("producto", producto);
        req.setAttribute("codigoEstado", HttpServletResponse.SC_CREATED);
        req.setAttribute("mensaje", "Producto registrado correctamente");
        req.getRequestDispatcher("/productoresultado.jsp").forward(req, resp);
    }

    private String validarDatos(String nombre, String categoria, String precioTexto, String stockTexto, String deposito) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return "El nombre del producto es obligatorio";
        }
        if (categoria == null || categoria.trim().isEmpty()) {
            return "Debe seleccionar una categoria";
        }
        if (precioTexto == null || precioTexto.trim().isEmpty()) {
            return "El precio en dolares es obligatorio";
        }
        if (stockTexto == null || stockTexto.trim().isEmpty()) {
            return "El stock es obligatorio";
        }
        if (deposito == null || deposito.trim().isEmpty()) {
            return "Debe indicar el deposito";
        }

        try {
            double precio = Double.parseDouble(precioTexto);
            if (precio <= 0) {
                return "El precio debe ser mayor que 0 dolares";
            }
        } catch (NumberFormatException e) {
            return "El precio debe ser un numero valido";
        }

        try {
            int stock = Integer.parseInt(stockTexto);
            if (stock < 0) {
                return "El stock no puede ser negativo";
            }
        } catch (NumberFormatException e) {
            return "El stock debe ser un numero entero valido";
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private void agregarEvento(String evento) {
        List<String> historiaEventos = (List<String>) getServletContext().getAttribute("historiaEventos");
        if (historiaEventos == null) {
            historiaEventos = new ArrayList<>();
            getServletContext().setAttribute("historiaEventos", historiaEventos);
        }
        historiaEventos.add(LocalDateTime.now() + " - " + evento);
    }
}
