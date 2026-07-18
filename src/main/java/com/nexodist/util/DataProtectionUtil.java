package com.nexodist.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Cifrado determinista AES-256-GCM para datos personales almacenados. */
public final class DataProtectionUtil {
    private static final String PREFIJO = "ENC1:";
    private static final byte[] CLAVE = cargarClave();

    private DataProtectionUtil() {}

    public static String proteger(String valor) {
        if (valor == null || valor.isBlank() || valor.startsWith(PREFIJO)) return valor;
        try {
            byte[] plano = valor.getBytes(StandardCharsets.UTF_8);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(CLAVE, "HmacSHA256"));
            byte[] nonce = Arrays.copyOf(mac.doFinal(plano), 12);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(CLAVE, "AES"), new GCMParameterSpec(128, nonce));
            byte[] cifrado = cipher.doFinal(plano);
            byte[] salida = new byte[nonce.length + cifrado.length];
            System.arraycopy(nonce, 0, salida, 0, nonce.length);
            System.arraycopy(cifrado, 0, salida, nonce.length, cifrado.length);
            return PREFIJO + Base64.getUrlEncoder().withoutPadding().encodeToString(salida);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo proteger el dato sensible", e);
        }
    }

    public static String revelar(String valor) {
        if (valor == null || !valor.startsWith(PREFIJO)) return valor;
        try {
            byte[] entrada = Base64.getUrlDecoder().decode(valor.substring(PREFIJO.length()));
            byte[] nonce = Arrays.copyOfRange(entrada, 0, 12);
            byte[] cifrado = Arrays.copyOfRange(entrada, 12, entrada.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(CLAVE, "AES"), new GCMParameterSpec(128, nonce));
            return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Dato sensible cifrado con una clave diferente", e);
        }
    }

    private static byte[] cargarClave() {
        String configurada = System.getProperty("sistema.data.key");
        if (configurada == null || configurada.isBlank()) configurada = System.getenv("SISTEMA_DATA_KEY");
        if (configurada == null || configurada.length() < 16) configurada = cargarOCrearClaveLocal();
        try {
            return MessageDigest.getInstance("SHA-256").digest(configurada.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String cargarOCrearClaveLocal() {
        try {
            Path ruta = Path.of(System.getProperty("user.dir"), ".sistema-data.key");
            if (Files.exists(ruta)) return Files.readString(ruta, StandardCharsets.UTF_8).trim();
            byte[] aleatoria = new byte[32];
            new SecureRandom().nextBytes(aleatoria);
            String clave = Base64.getEncoder().encodeToString(aleatoria);
            Files.writeString(ruta, clave, StandardCharsets.UTF_8);
            return clave;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cargar o crear la clave de cifrado", e);
        }
    }
}
