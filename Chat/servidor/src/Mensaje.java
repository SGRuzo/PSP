public class Mensaje {
    private final String tipo;      // LOGIN, MENSAJE, COMANDO, NOTIFICACION, RESPUESTA
    private final String remitente; // Nickname del usuario
    private final String rol;       // USER, ADMIN, GUEST, SISTEMA
    private final String contenido; // El texto/comando
    private long timestamp;   // Timestamp del mensaje

    // Constructor completo
    public Mensaje(String tipo, String remitente, String rol, String contenido) {
        this.tipo = tipo;
        this.remitente = remitente;
        this.rol = rol;
        this.contenido = contenido;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters (necesarios para Protocolo.java)
    public String getTipo() { return tipo; }
    public String getRemitente() { return remitente; }
    public String getRol() { return rol; }
    public String getContenido() { return contenido; }
    public long getTimestamp() { return timestamp; }

    // Setter para timestamp (opcional)
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return tipo + "|" + remitente + "|" + rol + "|" + contenido;
    }
}

