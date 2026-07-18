<%@ page import="com.nexodist.model.Usuario" %>
<%@ page import="com.nexodist.model.Producto" %>
<%@ page import="com.nexodist.model.CarritoItem" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuario");
    if (usuario == null) {
        response.sendRedirect("index.jsp");
        return;
    }

    List<Producto> productos = (List<Producto>) application.getAttribute("productos");
    List<CarritoItem> carrito = (List<CarritoItem>) session.getAttribute("carrito");
    double total = 0;
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Carrito</title>
    <link rel="stylesheet" href="css/estilos.css">
    <link rel="stylesheet" href="css/accessibility.css">
    <script src="js/accessibility.js"></script>
</head>
<body>
<a class="sc-skip-link" href="#contenido-principal">Saltar al contenido principal</a>
<main class="layout" id="contenido-principal" tabindex="-1">
    <aside class="sidebar">
        <div class="app-brand"><span class="logo">SC</span><span>Sistema Clínico</span></div>
        <nav class="nav">
            <a href="panel">Panel</a>
            <a href="productos">Productos</a>
            <a class="activo" href="carrito">Carrito</a>
            <a href="historial">Cookies</a>
            <a href="estado">Estatus y cabeceras</a>
            <a href="logout">Cerrar sesion</a>
        </nav>
    </aside>

    <section class="contenido">
        <div class="topbar">
            <div>
                <h1>Movimiento de stock</h1>
                <p class="texto">Cada movimiento queda en HttpSession y tambien en una cookie de historial.</p>
            </div>
            <span class="pill"><%= usuario.getRol() %></span>
        </div>

        <section class="grid cols-2">
            <article class="tarjeta">
                <h2>Agregar salida</h2>
                <form action="carrito" method="post" class="formulario">
                    <label>Producto
                        <select name="idProducto" required>
                            <% if (productos != null) {
                                for (Producto producto : productos) { %>
                                    <option value="<%= producto.getId() %>">
                                        <%= producto.getNombre() %> - $<%= String.format(Locale.US, "%.2f", producto.getPrecio()) %> - stock <%= producto.getStock() %>
                                    </option>
                            <%  }
                            } %>
                        </select>
                    </label>
                    <label>Cantidad
                        <input type="number" name="cantidad" min="1" value="1" required>
                    </label>
                    <button type="submit">Registrar movimiento</button>
                </form>
            </article>

            <article class="tarjeta">
                <h2>Carrito de movimientos</h2>
                <table>
                    <thead>
                    <tr>
                        <th>Producto</th>
                        <th>Cantidad</th>
                        <th>Subtotal</th>
                    </tr>
                    </thead>
                    <tbody>
                    <% if (carrito != null && !carrito.isEmpty()) {
                        for (CarritoItem item : carrito) {
                            total += item.Subtotal();
                    %>
                        <tr>
                            <td><%= item.getProducto().getNombre() %></td>
                            <td><%= item.getCantidad() %></td>
                            <td class="precio">$<%= String.format(Locale.US, "%.2f", item.Subtotal()) %></td>
                        </tr>
                    <%  }
                    } else { %>
                        <tr>
                            <td colspan="3">No existen movimientos en la sesion.</td>
                        </tr>
                    <% } %>
                    </tbody>
                </table>
                <p class="texto" style="margin-top:18px;">Total: <strong class="precio">$<%= String.format(Locale.US, "%.2f", total) %></strong></p>
                <nav class="acciones">
                    <a class="btn secundario" href="historial">Ver cookie de historial</a>
                </nav>
            </article>
        </section>
    </section>
</main>
</body>
</html>

