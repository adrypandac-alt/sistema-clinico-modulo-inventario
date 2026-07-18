<%@ page import="com.nexodist.model.Usuario" %>
<%@ page import="com.nexodist.model.Producto" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuario");
    if (usuario == null) {
        response.sendRedirect("index.jsp");
        return;
    }
    List<Producto> productos = (List<Producto>) request.getAttribute("productos");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Productos</title>
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
            <a class="activo" href="productos">Productos</a>
            <a href="carrito">Carrito</a>
            <a href="historial">Cookies</a>
            <a href="estado">Estatus y cabeceras</a>
            <a href="logout">Cerrar sesion</a>
        </nav>
    </aside>

    <section class="contenido">
        <div class="topbar">
            <div>
                <h1>Productos y stock</h1>
                <p class="texto">Precios registrados en dolares y stock por deposito.</p>
            </div>
            <span class="pill"><%= usuario.getRol() %></span>
        </div>

        <section class="grid cols-2">
            <article class="tarjeta">
                <h2>Registrar producto</h2>
                <form action="productos" method="post" class="formulario">
                    <label>Nombre
                        <input type="text" name="nombre" required>
                    </label>
                    <label>Categoria
                        <select name="categoria" required>
                            <option value="">Seleccione</option>
                            <option value="Tecnologia">Tecnologia</option>
                            <option value="Infraestructura">Infraestructura</option>
                            <option value="Insumos">Insumos</option>
                        </select>
                    </label>
                    <label>Precio en dolares
                        <input type="number" name="precio" step="0.01" min="0.01" required>
                    </label>
                    <label>Stock
                        <input type="number" name="stock" min="0" required>
                    </label>
                    <label>Deposito
                        <input type="text" name="deposito" placeholder="Quito Norte" required>
                    </label>
                    <button type="submit">Guardar producto</button>
                </form>
            </article>

            <article class="tarjeta">
                <h2>Inventario actual</h2>
                <table>
                    <thead>
                    <tr>
                        <th>ID</th>
                        <th>Producto</th>
                        <th>Precio</th>
                        <th>Stock</th>
                    </tr>
                    </thead>
                    <tbody>
                    <% if (productos != null) {
                        for (Producto producto : productos) { %>
                        <tr>
                            <td><%= producto.getId() %></td>
                            <td>
                                <strong><%= producto.getNombre() %></strong><br>
                                <span class="texto"><%= producto.getCategoria() %> - <%= producto.getDeposito() %></span>
                            </td>
                            <td class="precio">$<%= String.format(Locale.US, "%.2f", producto.getPrecio()) %></td>
                            <td><%= producto.getStock() %></td>
                        </tr>
                    <%  }
                    } %>
                    </tbody>
                </table>
            </article>
        </section>
    </section>
</main>
</body>
</html>

