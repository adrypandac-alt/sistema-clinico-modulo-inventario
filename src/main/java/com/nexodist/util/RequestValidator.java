package com.nexodist.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/**
 * En esta clase centralizo la validación de parámetros recibidos desde JSP.
 *
 * La ubico como utilidad compartida porque varios servlets reciben enteros,
 * nombres, documentos y correos con las mismas reglas. Con esta clase evito
 * conversiones directas que podrían terminar en errores 500 si el navegador
 * envía un valor vacío o manipulado.
 */
public final class RequestValidator {
    private static final Pattern NOMBRE = Pattern.compile("^[\\p{L}][\\p{L} .'-]*$");
    private static final Pattern CORREO = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private RequestValidator() {}

    /**
     * En este método convierto un identificador o cantidad positiva.
     * Rechazo nulos, texto, cero y negativos antes de que lleguen al negocio.
     */
    public static int enteroPositivo(String valor, String campo) {
        String limpio = requerido(valor, campo, 20);
        try {
            int numero = Integer.parseInt(limpio);
            if (numero <= 0) throw new IllegalArgumentException("✖ " + campo + " debe ser mayor que cero.");
            return numero;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("✖ " + campo + " debe ser un número entero válido.", e);
        }
    }

    public static int enteroNoNegativo(String valor, String campo) {
        String limpio = requerido(valor, campo, 20);
        try {
            int numero = Integer.parseInt(limpio);
            if (numero < 0) throw new IllegalArgumentException("✖ " + campo + " no puede ser negativo.");
            return numero;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("✖ " + campo + " debe ser un número entero válido.", e);
        }
    }

    /** En este método valido dinero con máximo dos decimales sin usar float. */
    public static BigDecimal decimalNoNegativo(String valor, String campo) {
        String limpio = requerido(valor, campo, 30);
        try {
            BigDecimal numero = new BigDecimal(limpio);
            if (numero.signum() < 0) throw new IllegalArgumentException("✖ " + campo + " no puede ser negativo.");
            if (numero.scale() > 2) throw new IllegalArgumentException("✖ " + campo + " admite como máximo dos decimales.");
            return numero.setScale(2, RoundingMode.UNNECESSARY);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("✖ " + campo + " debe ser un número decimal válido.", e);
        }
    }

    /** En este método elimino espacios y compruebo longitud y obligatoriedad. */
    public static String requerido(String valor, String campo, int maximo) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException("✖ " + campo + " es obligatorio.");
        String limpio = valor.trim();
        if (limpio.length() > maximo) throw new IllegalArgumentException("✖ " + campo + " supera " + maximo + " caracteres.");
        return limpio;
    }

    /** En este método valido nombres permitiendo acentos, ñ, guiones y apóstrofes. */
    public static String nombre(String valor, String campo, int maximo) {
        String limpio = requerido(valor, campo, maximo);
        if (!NOMBRE.matcher(limpio).matches()) throw new IllegalArgumentException("✖ " + campo + " contiene caracteres no permitidos.");
        return limpio;
    }

    /** En este método valido documentos sin tratarlos como cantidades numéricas. */
    public static String documento(String valor, String campo) {
        String limpio = requerido(valor, campo, 13);
        if (!limpio.matches("\\d{10}|\\d{13}")) throw new IllegalArgumentException("✖ " + campo + " debe contener 10 o 13 dígitos.");
        return limpio;
    }

    public static String correoOpcional(String valor) {
        if (valor == null || valor.isBlank()) return "";
        String limpio = valor.trim();
        if (limpio.length() > 120 || !CORREO.matcher(limpio).matches()) throw new IllegalArgumentException("✖ El correo electrónico no tiene un formato válido.");
        return limpio;
    }
}
