package com.nexodist.servlet;

import java.io.IOException;
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
import jakarta.servlet.http.HttpSession;

@WebServlet("/inventario")
public class InventarioServlet extends HttpServlet {

    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Usuario usuario = validarSesion(req, resp);
        if (usuario == null) return;
        if (!DashboardService.puedeVerPanel(usuario, "productos")) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Sin acceso a productos");
            return;
        }

        List<Producto> productos = (List<Producto>) getServletContext().getAttribute("productos");
        req.setAttribute("productos", productos);
        req.setAttribute("categorias", obtenerCategorias(productos));
        req.setAttribute("alertasCount", DashboardService.contarAlertas(productos));

        resp.setHeader("X-Modulo", "Inventario");
        req.getRequestDispatcher("/inventario.jsp").forward(req, resp);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Usuario usuario = validarSesion(req, resp);
        if (usuario == null) return;
        if (!DashboardService.puedeVerPanel(usuario, "productos")) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Sin acceso a productos");
            return;
        }
        if (!DashboardService.puedeModificarInventario(usuario)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Usuario de solo lectura");
            return;
        }

        req.setCharacterEncoding("UTF-8");
        String accion = req.getParameter("accion");
        if ("crearCategoria".equals(accion)) {
            crearCategoria(req, resp);
            return;
        }
        if ("eliminar".equals(accion)) {
            if (!DashboardService.esAdministrador(usuario)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Solo el administrador puede eliminar productos");
                return;
            }
            eliminarProducto(req, resp, usuario);
            return;
        }

        String nombre = req.getParameter("nombre");
        String sku = req.getParameter("sku");
        String categoria = req.getParameter("categoria");
        String proveedor = req.getParameter("proveedor");
        String lote = req.getParameter("lote");
        String fechaCaducidad = req.getParameter("fechaCaducidad");
        String registroSanitario = req.getParameter("registroSanitario");

        // Validar campos de texto obligatorios
        if (esVacio(nombre) || esVacio(categoria) || esVacio(proveedor)
                || esVacio(lote) || esVacio(fechaCaducidad) || esVacio(registroSanitario)) {
            resp.sendRedirect("inventario?error=camposObligatorios");
            return;
        }

        // Parsear numéricos con manejo de error
        double precio;
        int stock;
        int stockMinimo;
        try {
            precio = Double.parseDouble(req.getParameter("precio"));
            stock = Integer.parseInt(req.getParameter("stock"));
            stockMinimo = Integer.parseInt(req.getParameter("stockMinimo"));
            if (precio < 0 || stock < 0 || stockMinimo < 1) {
                resp.sendRedirect("inventario?error=valoresNegativos");
                return;
            }
        } catch (NumberFormatException e) {
            resp.sendRedirect("inventario?error=formatoNumerico");
            return;
        }

        // Validar que la fecha de caducidad no sea pasada (solo al crear)
        if (!"editar".equals(req.getParameter("accion"))) {
            try {
                java.time.LocalDate fechaParsed = java.time.LocalDate.parse(fechaCaducidad.trim());
                if (fechaParsed.isBefore(java.time.LocalDate.now())) {
                    resp.sendRedirect("inventario?error=fechaPasada");
                    return;
                }
            } catch (java.time.format.DateTimeParseException e) {
                resp.sendRedirect("inventario?error=fechaInvalida");
                return;
            }
        }

        String ubicacion = "No aplica";

        synchronized (getServletContext()) {
            List<Producto> productos = (List<Producto>) getServletContext().getAttribute("productos");
            if (productos == null) {
                productos = new ArrayList<>();
                getServletContext().setAttribute("productos", productos);
            }

            if ("editar".equals(accion)) {
                int productoId = Integer.parseInt(req.getParameter("id"));
                Producto producto = buscarProducto(productos, productoId);
                if (producto == null) {
                    resp.sendRedirect("inventario?error=producto");
                    return;
                }
                // Validar SKU duplicado al editar (ignorar el mismo producto)
                String skuEditar = (sku == null || sku.isBlank()) ? producto.getSku() : sku.trim();
                for (Producto otro : productos) {
                    if (otro.getId() != productoId && otro.getSku().equalsIgnoreCase(skuEditar)) {
                        resp.sendRedirect("inventario?error=skuDuplicado");
                        return;
                    }
                }
                int stockAnterior = producto.getStock();
                producto.setNombre(nombre.trim());
                producto.setSku(skuEditar);
                producto.setCategoria(categoria.trim());
                agregarCategoriaSiNueva(categoria);
                producto.setProveedor(proveedor.trim());
                producto.setPrecio(precio);
                producto.setStockMinimo(stockMinimo);
                producto.setUbicacion(ubicacion);
                producto.setLote(lote.trim());
                producto.setFechaCaducidad(fechaCaducidad.trim());
                producto.setRegistroSanitario(registroSanitario.trim());
                if (stock != stockAnterior) {
                    DashboardService.registrarMovimiento(getServletContext(), productoId, "AJUSTE", stock,
                            "Ajuste por modificación de producto",
                            usuario.getNombre() + " (" + DashboardService.rolVisible(usuario) + ")");
                }
                DatosStorage.guardarProductos(productos);
                List<com.nexodist.model.Movimiento> movimientos =
                        (List<com.nexodist.model.Movimiento>) getServletContext().getAttribute("movimientos");
                DatosStorage.guardarMovimientos(movimientos);
                resp.sendRedirect("inventario");
                return;
            }

            int nuevoId = productos.size() + 1;
            if (sku == null || sku.isBlank()) {
                sku = "SKU-" + nuevoId;
            }

            // Validar SKU duplicado al crear
            final String skuFinal = sku.trim();
            for (Producto otro : productos) {
                if (otro.getSku().equalsIgnoreCase(skuFinal)) {
                    resp.sendRedirect("inventario?error=skuDuplicado");
                    return;
                }
            }

            Producto producto = new Producto(nuevoId, sku.trim(), nombre.trim(), categoria.trim(),
                    proveedor.trim(), precio, 0, stockMinimo, ubicacion.trim(), lote.trim(),
                    fechaCaducidad.trim(), registroSanitario.trim());
            agregarCategoriaSiNueva(categoria);
            productos.add(producto);

            if (stock > 0) {
                DashboardService.registrarMovimiento(getServletContext(), nuevoId, "ENTRADA", stock,
                        "Alta automática de medicamento - lote " + lote.trim(),
                        usuario.getNombre() + " (" + DashboardService.rolVisible(usuario) + ")");
            }
            DatosStorage.guardarProductos(productos);
            List<com.nexodist.model.Movimiento> movimientos =
                    (List<com.nexodist.model.Movimiento>) getServletContext().getAttribute("movimientos");
            DatosStorage.guardarMovimientos(movimientos);
        }

        resp.sendRedirect("inventario");
    }

    @SuppressWarnings("unchecked")
    private void crearCategoria(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String nombre = req.getParameter("nombreCategoria");
        if (nombre == null || nombre.isBlank()) {
            resp.sendRedirect("inventario?categoriaError=vacia");
            return;
        }

        synchronized (getServletContext()) {
            List<String> categorias = (List<String>) getServletContext().getAttribute("categoriasInventario");
            if (categorias == null) {
                categorias = obtenerCategorias((List<Producto>) getServletContext().getAttribute("productos"));
            }
            String nuevaCategoria = nombre.trim();
            for (String categoria : categorias) {
                if (categoria.equalsIgnoreCase(nuevaCategoria)) {
                    getServletContext().setAttribute("categoriasInventario", categorias);
                    resp.sendRedirect("inventario?categoriaError=duplicada");
                    return;
                }
            }
            categorias.add(nuevaCategoria);
            getServletContext().setAttribute("categoriasInventario", categorias);
        }

        resp.sendRedirect("inventario?categoriaCreada=1");
    }

    @SuppressWarnings("unchecked")
    private void eliminarProducto(HttpServletRequest req, HttpServletResponse resp, Usuario usuario) throws IOException {
        int productoId = Integer.parseInt(req.getParameter("id"));
        synchronized (getServletContext()) {
            List<Producto> productos = (List<Producto>) getServletContext().getAttribute("productos");
            Producto producto = buscarProducto(productos, productoId);
            if (producto == null) {
                resp.sendRedirect("inventario?error=producto");
                return;
            }
            DashboardService.registrarMovimientoInformativo(getServletContext(), productoId, "ELIMINACION", 0,
                    "Eliminación de producto: " + producto.getNombre(),
                    usuario.getNombre() + " (" + DashboardService.rolVisible(usuario) + ")");
            productos.remove(producto);
            DatosStorage.guardarProductos(productos);
            List<com.nexodist.model.Movimiento> movimientos =
                    (List<com.nexodist.model.Movimiento>) getServletContext().getAttribute("movimientos");
            DatosStorage.guardarMovimientos(movimientos);
        }
        resp.sendRedirect("inventario");
    }

    private Producto buscarProducto(List<Producto> productos, int id) {
        if (productos == null) return null;
        for (Producto producto : productos) {
            if (producto.getId() == id) return producto;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> obtenerCategorias(List<Producto> productos) {
        List<String> categorias = (List<String>) getServletContext().getAttribute("categoriasInventario");
        if (categorias == null) {
            categorias = new ArrayList<>();
            for (String base : List.of("Medicamentos", "Vacunas", "Insumos médicos", "Equipos médicos", "Curación")) {
                categorias.add(base);
            }
        }
        if (productos != null) {
            for (Producto producto : productos) {
                String categoria = producto.getCategoria();
                if (categoria != null && !categoria.isBlank()) {
                    boolean existe = false;
                    for (String actual : categorias) {
                        if (actual.equalsIgnoreCase(categoria.trim())) {
                            existe = true;
                            break;
                        }
                    }
                    if (!existe) categorias.add(categoria.trim());
                }
            }
        }
        getServletContext().setAttribute("categoriasInventario", categorias);
        return categorias;
    }

    @SuppressWarnings("unchecked")
    private void agregarCategoriaSiNueva(String nombre) {
        if (nombre == null || nombre.isBlank()) return;
        List<String> categorias = (List<String>) getServletContext().getAttribute("categoriasInventario");
        if (categorias == null) {
            categorias = obtenerCategorias((List<Producto>) getServletContext().getAttribute("productos"));
        }
        for (String categoria : categorias) {
            if (categoria.equalsIgnoreCase(nombre.trim())) return;
        }
        categorias.add(nombre.trim());
        getServletContext().setAttribute("categoriasInventario", categorias);
    }

    private Usuario validarSesion(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("usuario") == null) {
            resp.sendRedirect("index.jsp");
            return null;
        }
        return (Usuario) session.getAttribute("usuario");
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
