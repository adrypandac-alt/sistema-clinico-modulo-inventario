<%@ page import="com.nexodist.model.Producto" %>
<%@ page import="java.util.Locale" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    Producto producto = (Producto) request.getAttribute("producto");
    Integer codigoEstado = (Integer) request.getAttribute("codigoEstado");
    String mensaje = (String) request.getAttribute("mensaje");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Producto registrado</title>
    <link rel="stylesheet" href="css/estilos.css">
    <link rel="stylesheet" href="css/accessibility.css">
    <script src="js/accessibility.js"></script>
</head>
<body>
<a class="sc-skip-link" href="#contenido-principal">Saltar al contenido principal</a>
<main class="contenido" id="contenido-principal" tabindex="-1">
    <section class="tarjeta">
        <p class="aviso"><%= mensaje %></p>
        <h1>Producto registrado</h1>
        <% if (producto != null) { %>
            <table>
                <tr><th>Status HTTP</th><td><%= codigoEstado %></td></tr>
                <tr><th>Nombre</th><td><%= producto.getNombre() %></td></tr>
                <tr><th>Categoria</th><td><%= producto.getCategoria() %></td></tr>
                <tr><th>Precio</th><td class="precio">$<%= String.format(Locale.US, "%.2f", producto.getPrecio()) %></td></tr>
                <tr><th>Stock</th><td><%= producto.getStock() %></td></tr>
                <tr><th>Deposito</th><td><%= producto.getDeposito() %></td></tr>
            </table>
        <% } %>
        <nav class="acciones">
            <a class="btn" href="productos">Volver a productos</a>
            <a class="btn secundario" href="estado">Ver cabeceras</a>
        </nav>
    </section>
</main>
</body>
</html>

