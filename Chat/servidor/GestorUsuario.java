
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Gestor de usuarios para el servidor de chat.
 * Mantiene un registro thread-safe de todos los clientes activos.
 * Permite registrar, eliminar y hacer broadcasting de mensajes a todos los usuarios.
 *
 * Utiliza ConcurrentHashMap para evitar problemas de concurrencia cuando múltiples
 * hilos acceden a la lista de usuarios simultáneamente.
 *
 * IMPORTANTE: PrintWriter no es thread-safe, por lo que se sincroniza el acceso
 * a cada PrintWriter individual para evitar race conditions.
 */
public class GestorUsuario {
    private static final Logger logger = Logger.getLogger(GestorUsuario.class.getName());

    /**
     * Map thread-safe que almacena: clave = nombreUsuario, valor = PrintWriter del cliente
     * ConcurrentHashMap no requiere sincronización explícita y es más eficiente que HashMap sincronizado
     */
    private final Map<String, PrintWriter> usuariosConectados;

    /**
     * Map de locks para sincronizar acceso a cada PrintWriter
     * Cada usuario tiene su propio lock para evitar deadlocks
     */
    private final Map<String, Object> locksUsuarios;

    /**
     * Constructor del gestor de usuarios
     * Inicializa el ConcurrentHashMap que almacenará los clientes activos
     */
    public GestorUsuario() {
        this.usuariosConectados = new ConcurrentHashMap<>();
        this.locksUsuarios = new ConcurrentHashMap<>();
        logger.info("GestorUsuarios inicializado");
    }

    /**
     * Registra un nuevo usuario en el sistema
     *
     * @param nombreUsuario Identificador único del usuario
     * @param salida PrintWriter para enviar mensajes a este cliente
     * @return true si se registró correctamente, false si el usuario ya existía
     */
    public boolean registrarUsuario(String nombreUsuario, PrintWriter salida) {
        if (nombreUsuario == null || nombreUsuario.trim().isEmpty()) {
            logger.warning("Intento de registrar usuario con nombre vacío");
            return false;
        }

        if (usuariosConectados.containsKey(nombreUsuario)) {
            logger.warning("Intento de registrar usuario duplicado: " + nombreUsuario);
            return false;
        }

        usuariosConectados.put(nombreUsuario, salida);
        // Crear un lock individual para este usuario
        locksUsuarios.put(nombreUsuario, new Object());
        logger.info("Usuario registrado: " + nombreUsuario +
                  " (Total conectados: " + usuariosConectados.size() + ")");

        // Notificar a todos que un nuevo usuario se conectó
        notificarConexionUsuario(nombreUsuario);

        return true;
    }

    /**
     * Desconecta un usuario del sistema
     *
     * @param nombreUsuario Identificador del usuario a desconectar
     * @return true si se desconectó correctamente, false si no existía
     */
    public boolean desconectarUsuario(String nombreUsuario) {
        if (usuariosConectados.remove(nombreUsuario) != null) {
            locksUsuarios.remove(nombreUsuario);
            logger.info("Usuario desconectado: " + nombreUsuario +
                      " (Total conectados: " + usuariosConectados.size() + ")");

            // Notificar a todos que el usuario se desconectó
            notificarDesconexionUsuario(nombreUsuario);

            return true;
        }

        logger.warning("Intento de desconectar usuario inexistente: " + nombreUsuario);
        return false;
    }

    /**
     * Verifica si un usuario está conectado
     *
     * @param nombreUsuario Identificador del usuario
     * @return true si el usuario está conectado
     */
    public boolean usuarioConectado(String nombreUsuario) {
        return usuariosConectados.containsKey(nombreUsuario);
    }

    /**
     * Obtiene el número total de usuarios conectados
     *
     * @return Cantidad de usuarios activos
     */
    public int obtenerCantidadUsuarios() {
        return usuariosConectados.size();
    }

    /**
     * Obtiene una copia de la lista de usuarios conectados
     * Se retorna una copia para evitar modificaciones externas al mapa
     *
     * @return Map inmutable con los usuarios conectados
     */
    public Map<String, PrintWriter> obtenerUsuarios() {
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(usuariosConectados));
    }

    /**
     * Obtiene una lista de nombres de usuarios conectados (sin los PrintWriters)
     * Útil para el comando LIST
     *
     * @return String con los nombres de usuarios separados por comas
     */
    public String obtenerListaUsuarios() {
        if (usuariosConectados.isEmpty()) {
            return "No hay usuarios conectados";
        }

        return String.join(", ", usuariosConectados.keySet());
    }

    /**
     * Envía un mensaje a un usuario específico
     *
     * @param nombreUsuario Destinatario del mensaje
     * @param remitente Usuario que envía el mensaje
     * @param contenido Contenido del mensaje
     * @return true si se envió correctamente
     */
    public boolean enviarMensajePrivado(String nombreUsuario, String remitente, String contenido) {
        PrintWriter salida = usuariosConectados.get(nombreUsuario);

        if (salida == null) {
            logger.warning("Intento de enviar mensaje a usuario desconectado: " + nombreUsuario);
            return false;
        }

        // Obtener lock del usuario para sincronizar escritura
        Object lock = locksUsuarios.get(nombreUsuario);
        if (lock == null) {
            logger.warning("Lock no disponible para usuario: " + nombreUsuario);
            return false;
        }

        synchronized (lock) {
            String mensaje = Protocolo.empaquetar(Protocolo.MSG, remitente, contenido);
            try {
                salida.println(mensaje);
                salida.flush();
                logger.fine("Mensaje privado enviado a " + nombreUsuario + " de " + remitente);
                return true;
            } catch (Exception e) {
                logger.warning("Error al enviar mensaje privado a " + nombreUsuario + ": " + e.getMessage());
                return false;
            }
        }
    }

    /**
     * Realiza BROADCASTING: envía un mensaje a todos los usuarios conectados
     * excepto al remitente (opcional)
     * Usa snapshot sincronizado para evitar TOCTOU (time-of-check, time-of-use)
     *
     * @param remitente Usuario que envía el mensaje
     * @param contenido Contenido del mensaje
     * @param excluirRemitente Si es true, no se envía el mensaje al remitente
     * @return Cantidad de usuarios a los que se envió el mensaje
     */
    public int broadcasting(String remitente, String contenido, boolean excluirRemitente) {
        int enviados = 0;
        String mensaje = Protocolo.empaquetar(Protocolo.MSG, remitente, contenido);

        // Crear snapshot sincronizado de usuarios para evitar cambios durante iteración
        Map<String, PrintWriter> snapshot;
        synchronized (usuariosConectados) {
            snapshot = new java.util.HashMap<>(usuariosConectados);
        }

        // Iterar sobre el snapshot (no requiere lock durante envío)
        for (Map.Entry<String, PrintWriter> entrada : snapshot.entrySet()) {
            String usuario = entrada.getKey();
            PrintWriter salida = entrada.getValue();

            // Si se requiere, excluir al remitente
            if (excluirRemitente && usuario.equals(remitente)) {
                continue;
            }

            // Obtener lock del usuario para sincronizar escritura
            Object lock = locksUsuarios.get(usuario);
            if (lock != null && salida != null) {
                synchronized (lock) {
                    try {
                        salida.println(mensaje);
                        salida.flush();
                        enviados++;
                    } catch (Exception e) {
                        logger.warning("Error al enviar mensaje a " + usuario + ": " + e.getMessage());
                        // No remover aquí, permitir que ManejadorCliente lo haga
                    }
                }
            }
        }

        logger.info("Mensaje de broadcasting enviado a " + enviados + " usuarios");
        return enviados;
    }

    /**
     * Realiza BROADCASTING a todos los usuarios incluyendo al remitente
     *
     * @param remitente Usuario que envía el mensaje
     * @param contenido Contenido del mensaje
     * @return Cantidad de usuarios a los que se envió el mensaje
     */
    public int broadcastingGlobal(String remitente, String contenido) {
        return broadcasting(remitente, contenido, false);
    }

    /**
     * Notifica a todos los usuarios que un nuevo usuario se ha conectado
     * Usa snapshot sincronizado para evitar TOCTOU
     *
     * @param nombreUsuario Nombre del usuario que se conectó
     */
    private void notificarConexionUsuario(String nombreUsuario) {
        String contenido = nombreUsuario + " se ha conectado al chat";
        String mensaje = Protocolo.empaquetar("NOTIFICACION", contenido);

        // Crear snapshot sincronizado de usuarios
        Map<String, PrintWriter> snapshot;
        synchronized (usuariosConectados) {
            snapshot = new java.util.HashMap<>(usuariosConectados);
        }

        // Iterar sobre el snapshot
        for (Map.Entry<String, PrintWriter> entrada : snapshot.entrySet()) {
            String usuario = entrada.getKey();
            PrintWriter salida = entrada.getValue();

            Object lock = locksUsuarios.get(usuario);
            if (lock != null && salida != null) {
                synchronized (lock) {
                    try {
                        salida.println(mensaje);
                        salida.flush();
                    } catch (Exception e) {
                        logger.warning("Error al notificar conexión a " + usuario + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Notifica a todos los usuarios que un usuario se ha desconectado
     * Usa snapshot sincronizado para evitar TOCTOU
     *
     * @param nombreUsuario Nombre del usuario que se desconectó
     */
    private void notificarDesconexionUsuario(String nombreUsuario) {
        String contenido = nombreUsuario + " se ha desconectado del chat";
        String mensaje = Protocolo.empaquetar("NOTIFICACION", contenido);

        // Crear snapshot sincronizado de usuarios
        Map<String, PrintWriter> snapshot;
        synchronized (usuariosConectados) {
            snapshot = new java.util.HashMap<>(usuariosConectados);
        }

        // Iterar sobre el snapshot
        for (Map.Entry<String, PrintWriter> entrada : snapshot.entrySet()) {
            String usuario = entrada.getKey();
            PrintWriter salida = entrada.getValue();

            Object lock = locksUsuarios.get(usuario);
            if (lock != null && salida != null) {
                synchronized (lock) {
                    try {
                        salida.println(mensaje);
                        salida.flush();
                    } catch (Exception e) {
                        logger.warning("Error al notificar desconexión a " + usuario + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Obtiene información de estado del gestor de usuarios
     *
     * @return String con información del estado actual
     */
    public String obtenerEstado() {
        return String.format("GestorUsuarios - Conectados: %d, Usuarios: %s",
            usuariosConectados.size(),
            obtenerListaUsuarios());
    }

    /**
     * Desconecta todos los usuarios (útil para apagar el servidor)
     */
    public void desconectarTodos() {
        logger.info("Desconectando todos los usuarios...");
        usuariosConectados.clear();
        logger.info("Todos los usuarios han sido desconectados");
    }
}
