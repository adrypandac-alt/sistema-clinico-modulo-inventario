package com.nexodist.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Capa de persistencia para las tablas de seguridad:
 *   - intentos_login      (auditoría de logins y bloqueo por fuerza bruta)
 *   - sesion_usuario       (control de sesiones activas)
 *   - historial_password   (política de no reutilización de contraseñas)
 *
 * Todas las operaciones son tolerantes a fallos: si MySQL no está disponible
 * los métodos retornan valores seguros por defecto (sin lanzar excepciones
 * hacia las capas superiores) — igual que DatosStorage.
 */
public final class SeguridadStorage {

    private static final Logger LOG = Logger.getLogger(SeguridadStorage.class.getName());

    /** Minutos de ventana para contar intentos fallidos. */
    public static final int VENTANA_INTENTOS_MINUTOS = 15;

    /** Máximo de intentos fallidos antes de bloquear. */
    public static final int MAX_INTENTOS_FALLIDOS = 5;

    /** Permite desactivar temporalmente el bloqueo sin eliminar la auditoría. */
    public static boolean bloqueoIntentosHabilitado() {
        return Boolean.parseBoolean(
                System.getProperty("sistema.seguridad.bloqueoLogin", "true"));
    }

    /** Duración de la sesión en horas. */
    public static final int DURACION_SESION_HORAS = 8;

    private SeguridadStorage() {}

    // ══════════════════════════════════════════════════════════════
    // INICIALIZACIÓN — crea las tablas si no existen
    // ══════════════════════════════════════════════════════════════

    /**
     * Crea las tres tablas de seguridad si no existen en la base de datos.
     * Se invoca desde AppContextListener al arrancar la aplicación.
     */
    public static void inicializarTablas() {
        try (Connection cn = DatosStorage.abrirConexionPublica();
             Statement st = cn.createStatement()) {

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS intentos_login (" +
                "  id_intento   INT AUTO_INCREMENT PRIMARY KEY," +
                "  correo       VARCHAR(120) NOT NULL," +
                "  ip           VARCHAR(45)  NOT NULL," +
                "  fecha        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "  exitoso      BOOLEAN      NOT NULL DEFAULT FALSE," +
                "  agente       VARCHAR(255)," +
                "  INDEX idx_intentos_correo (correo)," +
                "  INDEX idx_intentos_ip     (ip)," +
                "  INDEX idx_intentos_fecha  (fecha)" +
                ") ENGINE=InnoDB COMMENT='Auditoria de intentos de inicio de sesion'");

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS sesion_usuario (" +
                "  id_sesion    VARCHAR(128) NOT NULL PRIMARY KEY," +
                "  id_usuario   INT          NOT NULL," +
                "  ip           VARCHAR(45)  NOT NULL," +
                "  agente       VARCHAR(255)," +
                "  fecha_inicio TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "  fecha_expira TIMESTAMP    NOT NULL," +
                "  activa       BOOLEAN      NOT NULL DEFAULT TRUE," +
                "  INDEX idx_sesion_usuario (id_usuario)," +
                "  INDEX idx_sesion_activa  (activa, fecha_expira)," +
                "  CONSTRAINT fk_sesion_usuario" +
                "    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)" +
                "    ON UPDATE CASCADE ON DELETE CASCADE" +
                ") ENGINE=InnoDB COMMENT='Sesiones activas de usuarios autenticados'");

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS historial_password (" +
                "  id_historial INT AUTO_INCREMENT PRIMARY KEY," +
                "  id_usuario   INT          NOT NULL," +
                "  hash         VARCHAR(255) NOT NULL," +
                "  fecha        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "  INDEX idx_historial_usuario (id_usuario)," +
                "  CONSTRAINT fk_historial_usuario" +
                "    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)" +
                "    ON UPDATE CASCADE ON DELETE CASCADE" +
                ") ENGINE=InnoDB COMMENT='Historial de contrasenas hasheadas'");

            LOG.info("[SeguridadStorage] Tablas de seguridad verificadas/creadas correctamente.");

        } catch (SQLException e) {
            LOG.log(Level.WARNING,
                "[SeguridadStorage] MySQL no disponible — tablas de seguridad en memoria.", e);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // intentos_login
    // ══════════════════════════════════════════════════════════════

    /**
     * Registra un intento de login (exitoso o fallido).
     *
     * @param correo   correo del usuario que intentó ingresar
     * @param ip       dirección IP de origen
     * @param agente   User-Agent del navegador (puede ser null)
     * @param exitoso  true si el login fue correcto
     */
    public static void registrarIntento(String correo, String ip, String agente, boolean exitoso) {
        String sql = "INSERT INTO intentos_login (correo, ip, agente, exitoso) VALUES (?, ?, ?, ?)";
        try (Connection cn = DatosStorage.abrirConexionPublica();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, correo != null ? correo : "desconocido");
            ps.setString(2, ip != null ? ip : "0.0.0.0");
            ps.setString(3, agente != null && agente.length() > 255 ? agente.substring(0, 255) : agente);
            ps.setBoolean(4, exitoso);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.log(Level.FINE, "[SeguridadStorage] No se pudo registrar intento de login.", e);
        }
    }

    /**
     * Cuenta los intentos fallidos recientes de una IP o correo
     * dentro de la ventana de tiempo definida en VENTANA_INTENTOS_MINUTOS.
     *
     * @param ip     dirección IP a consultar
     * @param correo correo a consultar
     * @return número de intentos fallidos recientes
     */
    public static int contarIntentosFallidos(String ip, String correo) {
        String sql =
            "SELECT COUNT(*) FROM intentos_login " +
            "WHERE exitoso = FALSE " +
            "  AND fecha >= DATE_SUB(NOW(), INTERVAL ? MINUTE) " +
            "  AND (ip = ? OR correo = ?)";
        try (Connection cn = DatosStorage.abrirConexionPublica();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, VENTANA_INTENTOS_MINUTOS);
            ps.setString(2, ip != null ? ip : "0.0.0.0");
            ps.setString(3, correo != null ? correo : "");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOG.log(Level.FINE, "[SeguridadStorage] No se pudo contar intentos fallidos.", e);
        }
        return 0; // Fallo seguro: si no hay BD no bloqueamos
    }

    /**
     * Verifica si una IP o correo está actualmente bloqueado
     * por exceder MAX_INTENTOS_FALLIDOS en la ventana de tiempo.
     *
     * @param ip     dirección IP del cliente
     * @param correo correo del usuario
     * @return true si debe bloquearse el acceso
     */
    public static boolean estaBloqueado(String ip, String correo) {
        return bloqueoIntentosHabilitado()
                && contarIntentosFallidos(ip, correo) >= MAX_INTENTOS_FALLIDOS;
    }

    /**
     * Elimina los intentos fallidos antiguos (limpieza periódica).
     * Elimina registros con más de 24 horas de antigüedad.
     */
    public static void limpiarIntentosAntiguos() {
        String sql = "DELETE FROM intentos_login WHERE fecha < DATE_SUB(NOW(), INTERVAL 24 HOUR)";
        try (Connection cn = DatosStorage.abrirConexionPublica();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            int eliminados = ps.executeUpdate();
            if (eliminados > 0) {
                LOG.info("[SeguridadStorage] Limpieza: " + eliminados + " registros de intentos eliminados.");
            }
        } catch (SQLException e) {
            LOG.log(Level.FINE, "[SeguridadStorage] No se pudo limpiar intentos antiguos.", e);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // sesion_usuario
    // ══════════════════════════════════════════════════════════════

    /**
     * Registra una nueva sesión activa para el usuario autenticado.
     *
     * @param idSesion   identificador único de la sesión Jakarta (session.getId())
     * @param idUsuario  ID del usuario en la BD (0 si modo memoria)
     * @param ip         IP del cliente
     * @param agente     User-Agent del navegador
     */
    public static void registrarSesion(String idSesion, int idUsuario, String ip, String agente) {
        if (idUsuario <= 0) return; // Modo memoria — no persistir
        LocalDateTime expira = LocalDateTime.now().plusHours(DURACION_SESION_HORAS);
        String sql =
            "INSERT INTO sesion_usuario (id_sesion, id_usuario, ip, agente, fecha_expira) " +
            "VALUES (?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE activa = TRUE, fecha_expira = VALUES(fecha_expira), ip = VALUES(ip)";
        try (Connection cn = DatosStorage.abrirConexionPublica();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, idSesion);
            ps.setInt(2, idUsuario);
            ps.setString(3, ip != null ? ip : "0.0.0.0");
            ps.setString(4, agente != null && agente.length() > 255 ? agente.substring(0, 255) : agente);
            ps.setTimestamp(5, Timestamp.valueOf(expira));
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.log(Level.FINE, "[SeguridadStorage] No se pudo registrar sesión.", e);
        }
    }

    /**
     * Invalida una sesión específica (logout o expiración forzada).
     *
     * @param idSesion identificador de la sesión a invalidar
     */
    public static void invalidarSesion(String idSesion) {
        String sql = "UPDATE sesion_usuario SET activa = FALSE WHERE id_sesion = ?";
        try (Connection cn = DatosStorage.abrirConexionPublica();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, idSesion);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.log(Level.FINE, "[SeguridadStorage] No se pudo invalidar sesión.", e);
        }
    }

    /**
     * Invalida todas las sesiones activas de un usuario.
     * Se usa cuando el admin desactiva o elimina un usuario.
     *
     * @param idUsuario ID del usuario cuyas sesiones se invalidan
     */
    public static void invalidarSesionesPorUsuario(int idUsuario) {
        String sql = "UPDATE sesion_usuario SET activa = FALSE WHERE id_usuario = ? AND activa = TRUE";
        try (Connection cn = DatosStorage.abrirConexionPublica();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            int afectadas = ps.executeUpdate();
            if (afectadas > 0) {
                LOG.info("[SeguridadStorage] " + afectadas + " sesión(es) invalidadas para usuario #" + idUsuario);
            }
        } catch (SQLException e) {
            LOG.log(Level.FINE, "[SeguridadStorage] No se pudo invalidar sesiones del usuario.", e);
        }
    }

    /**
     * Cuenta las sesiones activas y no expiradas de un usuario.
     * Útil para detectar sesiones simultáneas sospechosas.
     *
     * @param idUsuario ID del usuario
     * @return número de sesiones activas
     */
    public static int contarSesionesActivas(int idUsuario) {
        String sql =
            "SELECT COUNT(*) FROM sesion_usuario " +
            "WHERE id_usuario = ? AND activa = TRUE AND fecha_expira > NOW()";
        try (Connection cn = DatosStorage.abrirConexionPublica();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOG.log(Level.FINE, "[SeguridadStorage] No se pudo contar sesiones activas.", e);
        }
        return 0;
    }

    // ══════════════════════════════════════════════════════════════
    // historial_password
    // ══════════════════════════════════════════════════════════════

    /**
     * Guarda el hash de la contraseña actual en el historial del usuario.
     * Se debe llamar cada vez que se cambia la contraseña exitosamente.
     *
     * @param idUsuario ID del usuario en la BD
     * @param hash      hash BCrypt de la nueva contraseña
     */
    public static void agregarAlHistorial(int idUsuario, String hash) {
        if (idUsuario <= 0 || hash == null || hash.isBlank()) return;
        String sql = "INSERT INTO historial_password (id_usuario, hash) VALUES (?, ?)";
        try (Connection cn = DatosStorage.abrirConexionPublica();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setString(2, hash);
            ps.executeUpdate();
            // Limpiar entradas antiguas — conservar solo los últimos MAX_HISTORIAL
            limpiarHistorialAntiguo(cn, idUsuario);
        } catch (SQLException e) {
            LOG.log(Level.FINE, "[SeguridadStorage] No se pudo guardar en historial de contraseñas.", e);
        }
    }

    /**
     * Verifica si una contraseña en texto plano ya fue usada
     * en los últimos MAX_HISTORIAL cambios del usuario.
     *
     * @param idUsuario        ID del usuario
     * @param claveTextoPlano  contraseña a verificar
     * @return true si la contraseña ya está en el historial reciente
     */
    public static boolean yaFueUsada(int idUsuario, String claveTextoPlano) {
        if (idUsuario <= 0 || claveTextoPlano == null) return false;
        String sql =
            "SELECT hash FROM historial_password " +
            "WHERE id_usuario = ? ORDER BY fecha DESC LIMIT ?";
        try (Connection cn = DatosStorage.abrirConexionPublica();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setInt(2, com.nexodist.util.PasswordUtil.MAX_HISTORIAL);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String hashAnterior = rs.getString("hash");
                    if (com.nexodist.util.PasswordUtil.verificar(claveTextoPlano, hashAnterior)) {
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.FINE, "[SeguridadStorage] No se pudo consultar historial de contraseñas.", e);
        }
        return false; // Fallo seguro: si no hay BD no bloqueamos
    }

    /**
     * Conserva solo los últimos MAX_HISTORIAL registros del historial
     * de un usuario, eliminando los más antiguos.
     */
    private static void limpiarHistorialAntiguo(Connection cn, int idUsuario) throws SQLException {
        String sql =
            "DELETE FROM historial_password WHERE id_historial IN (" +
            "  SELECT id_historial FROM (" +
            "    SELECT id_historial FROM historial_password " +
            "    WHERE id_usuario = ? ORDER BY fecha DESC " +
            "    LIMIT 99999 OFFSET ?" +
            "  ) AS subconsulta" +
            ")";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setInt(2, com.nexodist.util.PasswordUtil.MAX_HISTORIAL);
            ps.executeUpdate();
        }
    }
}
