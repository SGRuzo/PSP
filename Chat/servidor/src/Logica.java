import java.util.List;
// Importaciones del paquete Compartido
// Nota: Asegurar que Compartido esté en el classpath durante compilación

public class Logica {
    private final Usuarios usuarios;

    public Logica(Usuarios usuarios) {
        this.usuarios = usuarios;
    }

    // Procesar comandos especiales (/bye, /list, /ping)
    @SuppressWarnings("unused")
    public String procesarComando(String comando, String rol) {
        comando = comando.trim();

        switch(comando) {
            case "/bye":
                return Protocolo.serializar(
                        new Mensaje("COMANDO_ACEPTADO", "servidor", "SISTEMA",
                                "Desconectando...")
                );

            case "/list":
                List<String> usuarios_lista = usuarios.obtenerLista();
                String respuesta = "Usuarios conectados: "
                        + String.join(", ", usuarios_lista)
                        + " (Total: " + usuarios.cantidad() + ")";
                return Protocolo.serializar(
                        new Mensaje("RESPUESTA", "servidor", "SISTEMA", respuesta)
                );

            case "/ping":
                return Protocolo.serializar(
                        new Mensaje("RESPUESTA", "servidor", "SISTEMA", "pong")
                );

            case "/help":
                String help = """
                        Comandos disponibles:
                          /list  - Listar usuarios conectados
                          /ping  - Verificar latencia
                          /bye   - Desconectarse""";
                return Protocolo.serializar(
                        new Mensaje("RESPUESTA", "servidor", "SISTEMA", help)
                );

            default:
                return Protocolo.serializar(
                        new Mensaje("ERROR", "servidor", "SISTEMA",
                                "Comando desconocido: " + comando +
                                        ". Escribe /help para ver comandos")
                );
        }
    }

    // Procesar un mensaje normal (no comando)
    public String procesarMensaje(String remitente, String rol, String contenido) {
        // En un chat normal, simplemente retransmitimos
        return Protocolo.serializar(
                new Mensaje("MENSAJE", remitente, rol, contenido)
        );
    }

    // Validar si el usuario puede ejecutar una acción (según rol)
    public boolean puedeEjecutar(String comando, String rol) {
        // Por ejemplo: solo ADMIN puede /admin-shutdown
        return !comando.equals("/admin-shutdown") || rol.equals("ADMIN");
    }

    // Crear mensaje de notificación del sistema
    public static String crearNotificacion(String contenido) {
        return Protocolo.serializar(
                new Mensaje("NOTIFICACION", "servidor", "SISTEMA", contenido)
        );
    }

    // Crear mensaje de error
    public static String crearError(String contenido) {
        return Protocolo.serializar(
                new Mensaje("ERROR", "servidor", "SISTEMA", contenido)
        );
    }
}