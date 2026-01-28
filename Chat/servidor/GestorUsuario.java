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
     * Registra un nuevo usuario en el sistema.
     * Si el nombre de usuario ya existe, desconecta la sesión anterior.
     *
     * @param nombreUsuario Identificador único del usuario
     * @param salida PrintWriter para enviar mensajes a este cliente
     * @return true si se registró correctamente, false si hay error
     */
    public boolean registrarUsuario(String nombreUsuario, PrintWriter salida) {
        if (nombreUsuario == null || nombreUsuario.trim().isEmpty()) {
            logger.warning("Intento de registrar usuario con nombre vacío");
            return false;
        }

        // IMPORTANTE: Verificar si el nombre ya existe
        if (usuariosConectados.containsKey(nombreUsuario)) {
            logger.warning("Usuario duplicado detectado: " + nombreUsuario + ". Desconectando sesión anterior.");

            // Obtener el PrintWriter anterior y cerrarlo
            PrintWriter antiguo = usuariosConectados.get(nombreUsuario);
            if (antiguo != null) {
                try {
                    antiguo.close();
                    logger.info("Conexión anterior de " + nombreUsuario + " cerrada");
                } catch (Exception e) {
                    logger.warning("Error al cerrar conexión anterior: " + e.getMessage());
                }
            }

            // Remover el usuario antiguo del sistema
            usuariosConectados.remove(nombreUsuario);
            locksUsuarios.remove(nombreUsuario);
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
     * excepto al remitente (opcional).
     * Usa snapshot sincronizado para evitar TOCTOU (time-of-check, time-of-use).
     * Es robusto ante desconexiones inesperadas de sockets durante el envío.
     *
     * @param remitente Usuario que envía el mensaje
     * @param contenido Contenido del mensaje
     * @param excluirRemitente Si es true, no se envía el mensaje al remitente
     * @return Cantidad de usuarios a los que se envió el mensaje correctamente
     */
    public int broadcasting(String remitente, String contenido, boolean excluirRemitente) {
        int enviados = 0;
        int fallos = 0;
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
                        // Verificar que el PrintWriter no esté en estado de error
                        if (!salida.checkError()) {
                            salida.println(mensaje);
                            salida.flush();
                            enviados++;
                        } else {
                            logger.warning("PrintWriter en error para usuario: " + usuario);
                            fallos++;
                        }
                    } catch (Exception e) {
                        logger.warning("Error al enviar mensaje a " + usuario + ": " + e.getMessage());
                        fallos++;
                        // NO remover aquí, permitir que ManejadorCliente lo haga
                        // Continuar con el siguiente usuario en lugar de fallar todo
                    }
                }
            }
        }

        if (fallos > 0) {
            logger.warning("Broadcasting completado: " + enviados + " enviados, " + fallos + " fallos");
        } else {
            logger.info("Mensaje de broadcasting enviado a " + enviados + " usuarios");
        }

        return enviados;
    }

    /**
     * Realiza BROADCASTING a todos los usuarios EXCEPTO al remitente
     * De esta forma, el usuario que envía el mensaje no lo recibe de vuelta
     *
     * @param remitente Usuario que envía el mensaje
     * @param contenido Contenido del mensaje
     * @return Cantidad de usuarios a los que se envió el mensaje
     */
    public int broadcastingGlobal(String remitente, String contenido) {
        return broadcasting(remitente, contenido, true);  // ✅ CAMBIO: true para excluir remitente
    }

    /**
     * Notifica a todos los usuarios que un nuevo usuario se ha conectado
     * Usa snapshot sincronizado para evitar TOCTOU
     * Es robusto ante fallos de envío a usuarios individuales
     *
     * @param nombreUsuario Nombre del usuario que se conectó
     */
    private void notificarConexionUsuario(String nombreUsuario) {
        String contenido = nombreUsuario + " acaba de conectarse a este chat";
        String mensaje = Protocolo.empaquetar("NOTIFICACION", contenido);

        // Crear snapshot sincronizado de usuarios
        Map<String, PrintWriter> snapshot;
        synchronized (usuariosConectados) {
            snapshot = new java.util.HashMap<>(usuariosConectados);
        }

        int notificados = 0;
        int fallos = 0;

        // Iterar sobre el snapshot
        for (Map.Entry<String, PrintWriter> entrada : snapshot.entrySet()) {
            String usuario = entrada.getKey();
            PrintWriter salida = entrada.getValue();

            // Excluir al usuario que se acaba de conectar
            if (usuario.equals(nombreUsuario)) {
                continue;
            }

            Object lock = locksUsuarios.get(usuario);
            if (lock != null && salida != null) {
                synchronized (lock) {
                    try {
                        if (!salida.checkError()) {
                            salida.println(mensaje);
                            salida.flush();
                            notificados++;
                        } else {
                            fallos++;
                        }
                    } catch (Exception e) {
                        logger.warning("Error al notificar conexión a " + usuario + ": " + e.getMessage());
                        fallos++;
                    }
                }
            }
        }

        if (fallos > 0) {
            logger.fine("Notificación de conexión: " + notificados + " enviadas, " + fallos + " fallos");
        }
    }

    /**
     * Notifica a todos los usuarios que un usuario se ha desconectado
     * Usa snapshot sincronizado para evitar TOCTOU
     * Es robusto ante fallos de envío a usuarios individuales
     *
     * @param nombreUsuario Nombre del usuario que se desconectó
     */
    private void notificarDesconexionUsuario(String nombreUsuario) {
        String contenido = nombreUsuario + " abandonó el chat";
        String mensaje = Protocolo.empaquetar("NOTIFICACION", contenido);

        // Crear snapshot sincronizado de usuarios
        Map<String, PrintWriter> snapshot;
        synchronized (usuariosConectados) {
            snapshot = new java.util.HashMap<>(usuariosConectados);
        }

        int notificados = 0;
        int fallos = 0;

        logger.info("Enviando notificación de desconexión para " + nombreUsuario + " a " + snapshot.size() + " usuarios activos");

        // Iterar sobre el snapshot
        for (Map.Entry<String, PrintWriter> entrada : snapshot.entrySet()) {
            String usuario = entrada.getKey();
            PrintWriter salida = entrada.getValue();

            Object lock = locksUsuarios.get(usuario);
            if (lock != null && salida != null) {
                synchronized (lock) {
                    try {
                        if (!salida.checkError()) {
                            salida.println(mensaje);
                            salida.flush();
                            notificados++;
                            logger.fine("Notificación de desconexión enviada a " + usuario);
                        } else {
                            fallos++;
                            logger.warning("PrintWriter en error para usuario: " + usuario);
                        }
                    } catch (Exception e) {
                        logger.warning("Error al notificar desconexión a " + usuario + ": " + e.getMessage());
                        fallos++;
                    }
                }
            }
        }

        logger.info("Notificación de desconexión completada: " + notificados + " enviadas, " + fallos + " fallos para " + nombreUsuario);
    }

    /**
     * Obtiene el mensaje de notificación de conexión para mostrar en el servidor
     *
     * @param nombreUsuario Nombre del usuario que se conectó
     * @return Mensaje formateado para mostrar en la consola del servidor
     */
    public String obtenerMensajeConexion(String nombreUsuario) {
        int cantidadUsuarios = usuariosConectados.size();
        return "> Nuevo cliente conectado (" + nombreUsuario + "). Actualmente hay " + cantidadUsuarios + " usuarios conectados";
    }

    /**
     * Obtiene el mensaje de notificación de desconexión para mostrar en el servidor
     *
     * @param nombreUsuario Nombre del usuario que se desconectó
     * @return Mensaje formateado para mostrar en la consola del servidor
     */
    public String obtenerMensajeDesconexion(String nombreUsuario) {
        return nombreUsuario + " dejó este chat";
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
     * Obtiene el estado de monitoreo: muestra "Ningún cliente conectado" si está vacío
     * o la lista de usuarios conectados con su cantidad
     *
     * @return String con el estado de monitoreo para mostrar en consola
     */
    public String obtenerEstadoMonitoreo() {
        if (usuariosConectados.isEmpty()) {
            return "Ningún cliente conectado";
        }
        int cantidad = usuariosConectados.size();
        String listaUsuarios = obtenerListaUsuarios();
        return "(" + cantidad + " conectados) " + listaUsuarios;
    }

    /**
     * Notifica a todos los usuarios que el servidor se está cerrando
     * Se ejecuta antes de desconectar todos los usuarios
     * Es robusto ante fallos de envío a usuarios individuales
     */
    public void notificarCierreServidor() {
        logger.info("Notificando cierre del servidor a todos los clientes...");
        String mensaje = Protocolo.empaquetar(Protocolo.SERVIDOR_DESCONECTADO, "El servidor se desconectó");

        // Crear snapshot sincronizado de usuarios
        Map<String, PrintWriter> snapshot;
        synchronized (usuariosConectados) {
            snapshot = new java.util.HashMap<>(usuariosConectados);
        }

        int notificados = 0;
        int fallos = 0;

        // Iterar sobre el snapshot
        for (Map.Entry<String, PrintWriter> entrada : snapshot.entrySet()) {
            String usuario = entrada.getKey();
            PrintWriter salida = entrada.getValue();

            Object lock = locksUsuarios.get(usuario);
            if (lock != null && salida != null) {
                synchronized (lock) {
                    try {
                        if (!salida.checkError()) {
                            salida.println(mensaje);
                            salida.flush();
                            notificados++;
                        } else {
                            fallos++;
                        }
                        logger.fine("Mensaje de cierre enviado a " + usuario);
                    } catch (Exception e) {
                        logger.warning("Error al notificar cierre a " + usuario + ": " + e.getMessage());
                        fallos++;
                    }
                }
            }
        }

        if (fallos > 0) {
            logger.fine("Notificación de cierre: " + notificados + " enviadas, " + fallos + " fallos");
        }
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
