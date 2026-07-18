package com.nexodist.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Utilidad centralizada para el manejo seguro de contraseñas.
 *
 * Usa BCrypt con cost factor 12 (recomendación OWASP 2024).
 * - Cost 12 ≈ ~300 ms por hash en hardware moderno → impráctica la fuerza bruta.
 * - El salt se genera e incluye automáticamente dentro del hash resultante.
 * - Un hash BCrypt siempre tiene exactamente 60 caracteres: $2a$12$<53 chars>
 *
 * Compatibilidad: detecta si una contraseña ya está hasheada (empieza con "$2")
 * para no re-hashear durante la migración de datos existentes.
 */
public final class PasswordUtil {

    /** Cost factor BCrypt — OWASP recomienda mínimo 10, óptimo 12 para web. */
    private static final int COST = 12;

    /** Prefijo que identifica un hash BCrypt válido. */
    private static final String PREFIJO_BCRYPT = "$2";

    /** Longitud mínima de contraseña aceptada por la aplicación. */
    public static final int LONGITUD_MINIMA = 8;

    /** Cantidad máxima de contraseñas anteriores que no se pueden reutilizar. */
    public static final int MAX_HISTORIAL = 5;

    private PasswordUtil() {
        // Clase utilitaria — no instanciable
    }

    /**
     * Genera un hash BCrypt para la contraseña dada.
     *
     * @param claveTextoPlano contraseña en texto claro
     * @return hash BCrypt de 60 caracteres listo para guardar en BD
     * @throws IllegalArgumentException si la clave es nula, vacía o muy corta
     */
    public static String hashear(String claveTextoPlano) {
        validarClave(claveTextoPlano);
        return BCrypt.withDefaults().hashToString(COST, claveTextoPlano.toCharArray());
    }

    /**
     * Verifica si una contraseña en texto plano coincide con su hash BCrypt.
     *
     * @param claveTextoPlano contraseña a verificar
     * @param hash            hash almacenado en la base de datos
     * @return true si coinciden, false en cualquier otro caso
     */
    public static boolean verificar(String claveTextoPlano, String hash) {
        if (claveTextoPlano == null || claveTextoPlano.isBlank()) return false;
        if (hash == null || hash.isBlank()) return false;
        try {
            BCrypt.Result resultado = BCrypt.verifyer().verify(
                    claveTextoPlano.toCharArray(), hash);
            return resultado.verified;
        } catch (Exception e) {
            // Hash malformado o algoritmo no soportado → fallo seguro
            return false;
        }
    }

    /**
     * Indica si el valor ya es un hash BCrypt (empieza con "$2").
     * Útil para no re-hashear contraseñas durante migración.
     *
     * @param valor string a evaluar
     * @return true si ya es un hash BCrypt
     */
    public static boolean esHash(String valor) {
        return valor != null && valor.startsWith(PREFIJO_BCRYPT);
    }

    /**
     * Hashea solo si la clave aún no está hasheada.
     * Método de migración: si el valor es texto plano lo hashea;
     * si ya es BCrypt lo devuelve tal cual.
     *
     * @param claveOHash contraseña en texto plano o hash existente
     * @return hash BCrypt garantizado
     */
    public static String hashearSiNecesario(String claveOHash) {
        if (esHash(claveOHash)) return claveOHash;
        return hashear(claveOHash);
    }

    /**
     * Valida que la contraseña cumpla los requisitos mínimos.
     *
     * @param clave contraseña a validar
     * @throws IllegalArgumentException si no cumple los requisitos
     */
    private static void validarClave(String clave) {
        if (clave == null || clave.isBlank()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía.");
        }
        if (clave.length() < LONGITUD_MINIMA) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos " + LONGITUD_MINIMA + " caracteres.");
        }
    }
}
