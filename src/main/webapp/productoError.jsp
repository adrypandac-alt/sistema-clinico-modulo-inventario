<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String mensajeError = (String) request.getAttribute("mensajeError");
    Integer codigoEstado = (Integer) request.getAttribute("codigoEstado");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Error HTTP</title>
    <link rel="stylesheet" href="css/estilos.css">
    <link rel="stylesheet" href="css/accessibility.css">
    <script src="js/accessibility.js"></script>
</head>
<body>
<a class="sc-skip-link" href="#contenido-principal">Saltar al contenido principal</a>
<main class="contenido" id="contenido-principal" tabindex="-1">
    <section class="tarjeta">
        <p class="error"><%= mensajeError == null ? "Ocurrio un error al procesar la solicitud" : mensajeError %></p>
        <h1>Status HTTP: <%= codigoEstado == null ? 500 : codigoEstado %></h1>
        <p class="texto">La respuesta fue enviada con cabeceras personalizadas desde el servlet.</p>
        <nav class="acciones">
            <a class="btn" href="productos">Volver a productos</a>
            <a class="btn secundario" href="panel">Panel</a>
        </nav>
    </section>
</main>
</body>
</html>

