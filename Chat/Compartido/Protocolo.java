

/**
 * Clase que define el protocolo de comunicación entre cliente y servidor.
 * Contiene constantes para los comandos y el separador de campos.
 *
 * IMPORTANTE: Implementa sistema de escape para permitir que el usuario
 * use el carácter "|" en sus mensajes sin romper el protocolo.
 */
public class Protocolo {
    // Separador de campos en los mensajes
    public static final String SEPARADOR = "|";

    // Secuencia de escape para el carácter separador
    // Se usa \x1F (Unit Separator) como prefijo para escapar caracteres especiales
    private static final String ESCAPE_PREFIX = "\u001F";  // Carácter de control (Unit Separator)
    private static final String ESCAPE_SEPARADOR = ESCAPE_PREFIX + "PIPE";

    // Comandos disponibles
    public static final String LOGIN = "LOGIN";
    public static final String MSG = "MSG";
    public static final String BYE = "BYE";
    public static final String LIST = "LIST";
    public static final String PING = "PING";

    // Respuestas del servidor
    public static final String OK = "OK";
    public static final String ERROR = "ERROR";
    public static final String SERVIDOR_DESCONECTADO = "SERVIDOR_DESCONECTADO";

    /**
     * Escapa caracteres especiales en un parámetro para que no rompan el protocolo
     * Reemplaza "|" con una secuencia de escape
     *
     * @param texto Texto a escapar
     * @return Texto con caracteres especiales escapados
     */
    private static String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace(SEPARADOR, ESCAPE_SEPARADOR);
    }

    /**
     * Desescapa caracteres especiales que fueron escapados
     * Restaura "|" desde su secuencia de escape
     *
     * @param texto Texto a desescapar
     * @return Texto con caracteres especiales restaurados
     */
    private static String desescapar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace(ESCAPE_SEPARADOR, SEPARADOR);
    }

    /**
     * Empaqueta un mensaje con los parámetros dados.
     * Escapa automáticamente los caracteres especiales en los parámetros.
     *
     * @param comando Comando a enviar
     * @param params Parámetros del comando
     * @return String con el formato: COMANDO|param1|param2|...
     */
    public static String empaquetar(String comando, String... params) {
        StringBuilder sb = new StringBuilder(comando);
        for (String param : params) {
            sb.append(SEPARADOR).append(escapar(param));
        }
        return sb.toString();
    }

    /**
     * Desempaqueta un mensaje en sus componentes.
     * Desescapa automáticamente los caracteres especiales en los parámetros.
     *
     * @param mensaje Mensaje a desempaquetar
     * @return Array de String con los componentes
     */
    public static String[] desempaquetar(String mensaje) {
        String[] partes = mensaje.split("\\" + SEPARADOR);

        // Desescapar todos los parámetros excepto el comando (índice 0)
        for (int i = 1; i < partes.length; i++) {
            partes[i] = desescapar(partes[i]);
        }

        return partes;
    }
}
