

/**
 * Clase que define el protocolo de comunicación entre cliente y servidor.
 * Contiene constantes para los comandos y el separador de campos.
 */
public class Protocolo {
    // Separador de campos en los mensajes
    public static final String SEPARADOR = "|";

    // Comandos disponibles
    public static final String LOGIN = "LOGIN";
    public static final String MSG = "MSG";
    public static final String BYE = "BYE";
    public static final String LIST = "LIST";
    public static final String PING = "PING";

    // Respuestas del servidor
    public static final String OK = "OK";
    public static final String ERROR = "ERROR";

    /**
     * Empaqueta un mensaje con los parámetros dados.
     *
     * @param comando Comando a enviar
     * @param params Parámetros del comando
     * @return String con el formato: COMANDO|param1|param2|...
     */
    public static String empaquetar(String comando, String... params) {
        StringBuilder sb = new StringBuilder(comando);
        for (String param : params) {
            sb.append(SEPARADOR).append(param);
        }
        return sb.toString();
    }

    /**
     * Desempaqueta un mensaje en sus componentes.
     *
     * @param mensaje Mensaje a desempaquetar
     * @return Array de String con los componentes
     */
    public static String[] desempaquetar(String mensaje) {
        return mensaje.split("\\" + SEPARADOR);
    }
}
