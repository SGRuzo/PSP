

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Clase POJO para representar un mensaje en el sistema de chat.
 * Diseñada para ser fácilmente convertible a/desde JSON mediante GSON.
 *
 * Atributos:
 * - remitente: Usuario que envía el mensaje
 * - contenido: Cuerpo del mensaje
 * - tipoComando: Tipo de comando (MSG, LOGIN, BYE, LIST, PING, etc.)
 * - timestamp: Marca temporal del mensaje
 */
public class Mensaje implements Serializable {
    private static final long serialVersionUID = 1L;

    private String remitente;
    private String contenido;
    private String tipoComando;
    private String timestamp;

    /**
     * Constructor vacío (necesario para GSON)
     */
    public Mensaje() {
        this.timestamp = obtenerTimestamp();
    }

    /**
     * Constructor con parámetros principales
     *
     * @param remitente Usuario que envía el mensaje
     * @param contenido Cuerpo del mensaje
     * @param tipoComando Tipo de comando (MSG, LOGIN, BYE, LIST, PING)
     */
    public Mensaje(String remitente, String contenido, String tipoComando) {
        this.remitente = remitente;
        this.contenido = contenido;
        this.tipoComando = tipoComando;
        this.timestamp = obtenerTimestamp();
    }

    /**
     * Constructor completo con timestamp personalizado
     *
     * @param remitente Usuario que envía el mensaje
     * @param contenido Cuerpo del mensaje
     * @param tipoComando Tipo de comando
     * @param timestamp Marca temporal personalizada
     */
    public Mensaje(String remitente, String contenido, String tipoComando, String timestamp) {
        this.remitente = remitente;
        this.contenido = contenido;
        this.tipoComando = tipoComando;
        this.timestamp = timestamp;
    }

    // ==================== Getters ====================

    public String getRemitente() {
        return remitente;
    }

    public String getContenido() {
        return contenido;
    }

    public String getTipoComando() {
        return tipoComando;
    }

    public String getTimestamp() {
        return timestamp;
    }

    // ==================== Setters ====================

    public void setRemitente(String remitente) {
        this.remitente = remitente;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public void setTipoComando(String tipoComando) {
        this.tipoComando = tipoComando;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    // ==================== Métodos Auxiliares ====================

    /**
     * Obtiene la marca temporal actual en formato ISO 8601
     *
     * @return String con la fecha y hora actual
     */
    private String obtenerTimestamp() {
        LocalDateTime ahora = LocalDateTime.now();
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return ahora.format(formato);
    }

    /**
     * Representación en String del mensaje
     *
     * @return String con formato legible del mensaje
     */
    @Override
    public String toString() {
        return String.format("[%s] %s (%s): %s",
            timestamp, remitente, tipoComando, contenido);
    }

    /**
     * Convierte el mensaje a formato JSON simple (compatible con GSON)
     *
     * @return String en formato JSON
     */
    public String toJSON() {
        return String.format(
            "{\"remitente\":\"%s\",\"contenido\":\"%s\",\"tipoComando\":\"%s\",\"timestamp\":\"%s\"}",
            remitente, contenido, tipoComando, timestamp
        );
    }
}

