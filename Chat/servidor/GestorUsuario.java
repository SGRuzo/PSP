import java.io.PrintWriter;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Gestor de usuarios para el servidor de chat.
 * Mantiene un registro thread-safe de todos los clientes activos.
 * Gestiona autenticación, roles, bloqueos temporales y permisos.
 *
 * Utiliza ConcurrentHashMap para evitar problemas de concurrencia cuando múltiples
 * hilos acceden a la lista de usuarios simultáneamente.
 */
public class GestorUsuario {
    private static final Logger logger = Logger.getLogger(GestorUsuario.class.getName());

    // Información de usuario autenticado
    private static class UsuarioAutenticado {
        String nombre;
        String rol;
        PrintWriter salida;

        UsuarioAutenticado(String nombre, String rol, PrintWriter salida) {
            this.nombre = nombre;
            this.rol = rol;
            this.salida = salida;
        }
    }

    // Información de intentos fallidos de login
    private static class IntentosLogin {
        int fallos;
        long tiempoBloqueo; // 0 si no está bloqueado

        IntentosLogin() {
            this.fallos = 0;
            this.tiempoBloqueo = 0;
        }
    }

    private static final int MAX_INTENTOS_LOGIN = 3;
    private static final long TIEMPO_BLOQUEO_MS = 30000; // 30 segundos

    /**
     * Map thread-safe que almacena: clave = nombreUsuario, valor = UsuarioAutenticado
     */
    private final Map<String, UsuarioAutenticado> usuariosConectados;

    /**
     * Map de locks para sincronizar acceso a cada usuario
     */
    private final Map<String, Object> locksUsuarios;

    /**
     * Map que rastréia intentos fallidos de login por IP/usuario
     */
    private final Map<String, IntentosLogin> intentosFallidos;

    /**
     * Gestor de credenciales (contraseñas y roles)
     */
    private final GestorCredenciales gestorCredenciales;

    /**
     * Constructor del gestor de usuarios
     */
    public GestorUsuario() {
        this.usuariosConectados = new ConcurrentHashMap<>();
        this.locksUsuarios = new ConcurrentHashMap<>();
        this.intentosFallidos = new ConcurrentHashMap<>();
        this.gestorCredenciales = new GestorCredenciales();
        logger.info("GestorUsuarios inicializado con gestión de autenticación");
    }

    /**
     * Valida las credenciales de un usuario
     * Implementa bloqueo temporal tras 3 intentos fallidos
     *
     * @param nombreUsuario Nombre de usuario
     * @param password Contraseña en texto plano
     * @return Rol del usuario si es válido, null si falla
     */
    public String validarCredenciales(String nombreUsuario, String password) {
        if (nombreUsuario == null || password == null) {
            logger.warning("Intento de login con parámetros nulos");
            return null;
        }

        IntentosLogin intentos = intentosFallidos.computeIfAbsent(nombreUsuario, k -> new IntentosLogin());

        synchronized (intentos) {
            // Verificar si el usuario está bloqueado
            if (intentos.tiempoBloqueo > 0) {
                long tiempoTranscurrido = System.currentTimeMillis() - intentos.tiempoBloqueo;
                if (tiempoTranscurrido < TIEMPO_BLOQUEO_MS) {
                    logger.warning("Usuario bloqueado (intentos fallidos): " + nombreUsuario);
                    return Protocolo.USER_BLOCKED; // Usar como marcador especial
                } else {
                    // Desbloquear después del tiempo de espera
                    intentos.fallos = 0;
                    intentos.tiempoBloqueo = 0;
                }
            }

            // Validar credenciales
            String rolValido = gestorCredenciales.validarCredenciales(nombreUsuario, password);

            if (rolValido != null) {
                // Login exitoso: resetear intentos
                intentos.fallos = 0;
                intentos.tiempoBloqueo = 0;
                logger.info("Login exitoso para usuario: " + nombreUsuario + " con rol: " + rolValido);
                return rolValido;
            } else {
                // Login fallido: incrementar intentos
                intentos.fallos++;
                logger.warning("Login fallido para usuario: " + nombreUsuario + " (intento " + intentos.fallos + "/" + MAX_INTENTOS_LOGIN + ")");

                if (intentos.fallos >= MAX_INTENTOS_LOGIN) {
                    intentos.tiempoBloqueo = System.currentTimeMillis();
                    logger.warning("Usuario bloqueado por intentos fallidos: " + nombreUsuario);
                }

                return null;
            }
        }
    }

    /**
     * Registra un usuario autenticado en el sistema
     *
     * @param nombreUsuario Nombre de usuario
     * @param rol Rol del usuario (USER o ADMIN)
     * @param salida PrintWriter para comunicación
     * @return true si se registró correctamente
     */
    public boolean registrarUsuarioAutenticado(String nombreUsuario, String rol, PrintWriter salida) {
        if (nombreUsuario == null || nombreUsuario.trim().isEmpty()) {
            logger.warning("Intento de registrar usuario con nombre vacío");
            return false;
        }

        // Desconectar sesión anterior si existe
        if (usuariosConectados.containsKey(nombreUsuario)) {
            logger.warning("Usuario duplicado detectado: " + nombreUsuario + ". Desconectando sesión anterior.");
            UsuarioAutenticado anterior = usuariosConectados.get(nombreUsuario);
            if (anterior != null && anterior.salida != null) {
                try {
                    anterior.salida.close();
                } catch (Exception e) {
                    logger.warning("Error al cerrar conexión anterior: " + e.getMessage());
                }
            }
            usuariosConectados.remove(nombreUsuario);
            locksUsuarios.remove(nombreUsuario);
        }

        UsuarioAutenticado usuario = new UsuarioAutenticado(nombreUsuario, rol, salida);
        usuariosConectados.put(nombreUsuario, usuario);
        locksUsuarios.put(nombreUsuario, new Object());

        logger.info("Usuario autenticado registrado: " + nombreUsuario + " (Rol: " + rol + ", Total: " + usuariosConectados.size() + ")");
        notificarConexionUsuario(nombreUsuario);

        return true;
    }

    /**
     * Desconecta un usuario del sistema
     */
    public boolean desconectarUsuario(String nombreUsuario) {
        if (usuariosConectados.remove(nombreUsuario) != null) {
            locksUsuarios.remove(nombreUsuario);
            logger.info("Usuario desconectado: " + nombreUsuario + " (Total: " + usuariosConectados.size() + ")");
            notificarDesconexionUsuario(nombreUsuario);
            return true;
        }
        return false;
    }

    /**
     * Verifica si un usuario está conectado
     */
    public boolean usuarioConectado(String nombreUsuario) {
        return usuariosConectados.containsKey(nombreUsuario);
    }

    /**
     * Obtiene el rol de un usuario conectado
     */
    public String obtenerRolUsuario(String nombreUsuario) {
        UsuarioAutenticado usuario = usuariosConectados.get(nombreUsuario);
        return usuario != null ? usuario.rol : null;
    }

    /**
     * Verifica si un usuario tiene permiso de ADMIN
     */
    public boolean esAdmin(String nombreUsuario) {
        String rol = obtenerRolUsuario(nombreUsuario);
        return Protocolo.ROLE_ADMIN.equals(rol);
    }

    /**
     * Desconecta forzosamente a un usuario (comando KICK)
     */
    public boolean expulsarUsuario(String nombreUsuarioAExpulsar) {
        UsuarioAutenticado usuario = usuariosConectados.get(nombreUsuarioAExpulsar);
        if (usuario != null && usuario.salida != null) {
            try {
                String mensaje = Protocolo.empaquetar(Protocolo.ERROR, "Has sido expulsado del servidor");
                usuario.salida.println(mensaje);
                usuario.salida.flush();
                usuario.salida.close();
                usuariosConectados.remove(nombreUsuarioAExpulsar);
                locksUsuarios.remove(nombreUsuarioAExpulsar);
                logger.info("Usuario expulsado: " + nombreUsuarioAExpulsar);
                return true;
            } catch (Exception e) {
                logger.warning("Error al expulsar usuario: " + e.getMessage());
            }
        }
        return false;
    }

    /**
     * Obtiene el número total de usuarios conectados
     */
    public int obtenerCantidadUsuarios() {
        return usuariosConectados.size();
    }

    /**
     * Obtiene lista de usuarios conectados
     */
    public String obtenerListaUsuarios() {
        if (usuariosConectados.isEmpty()) {
            return "No hay usuarios conectados";
        }
        return String.join(", ", usuariosConectados.keySet());
    }

    /**
     * Envía un mensaje privado a un usuario
     */
    public boolean enviarMensajePrivado(String nombreUsuario, String remitente, String contenido) {
        UsuarioAutenticado usuario = usuariosConectados.get(nombreUsuario);
        if (usuario == null || usuario.salida == null) {
            return false;
        }

        Object lock = locksUsuarios.get(nombreUsuario);
        if (lock == null) {
            return false;
        }

        synchronized (lock) {
            String mensaje = Protocolo.empaquetar(Protocolo.MSG, remitente, contenido);
            try {
                usuario.salida.println(mensaje);
                usuario.salida.flush();
                logger.fine("Mensaje privado enviado a " + nombreUsuario);
                return true;
            } catch (Exception e) {
                logger.warning("Error al enviar mensaje: " + e.getMessage());
                return false;
            }
        }
    }

    /**
     * Broadcasting con sincronización
     */
    public int broadcasting(String remitente, String contenido, boolean excluirRemitente) {
        int enviados = 0;
        String mensaje = Protocolo.empaquetar(Protocolo.MSG, remitente, contenido);

        Map<String, UsuarioAutenticado> snapshot;
        synchronized (usuariosConectados) {
            snapshot = new java.util.HashMap<>(usuariosConectados);
        }

        for (Map.Entry<String, UsuarioAutenticado> entrada : snapshot.entrySet()) {
            String usuario = entrada.getKey();
            UsuarioAutenticado usuarioObj = entrada.getValue();

            if (excluirRemitente && usuario.equals(remitente)) {
                continue;
            }

            Object lock = locksUsuarios.get(usuario);
            if (lock != null && usuarioObj.salida != null) {
                synchronized (lock) {
                    try {
                        if (!usuarioObj.salida.checkError()) {
                            usuarioObj.salida.println(mensaje);
                            usuarioObj.salida.flush();
                            enviados++;
                        }
                    } catch (Exception e) {
                        logger.warning("Error en broadcasting a " + usuario + ": " + e.getMessage());
                    }
                }
            }
        }

        return enviados;
    }

    /**
     * Broadcasting global (excluyendo remitente)
     */
    public int broadcastingGlobal(String remitente, String contenido) {
        return broadcasting(remitente, contenido, true);
    }

    /**
     * Notifica conexión de usuario
     */
    private void notificarConexionUsuario(String nombreUsuario) {
        String contenido = nombreUsuario + " acaba de conectarse al chat";
        String mensaje = Protocolo.empaquetar("NOTIFICACION", contenido);

        Map<String, UsuarioAutenticado> snapshot;
        synchronized (usuariosConectados) {
            snapshot = new java.util.HashMap<>(usuariosConectados);
        }

        for (Map.Entry<String, UsuarioAutenticado> entrada : snapshot.entrySet()) {
            String usuario = entrada.getKey();
            UsuarioAutenticado usuarioObj = entrada.getValue();

            if (usuario.equals(nombreUsuario)) {
                continue;
            }

            Object lock = locksUsuarios.get(usuario);
            if (lock != null && usuarioObj.salida != null) {
                synchronized (lock) {
                    try {
                        if (!usuarioObj.salida.checkError()) {
                            usuarioObj.salida.println(mensaje);
                            usuarioObj.salida.flush();
                        }
                    } catch (Exception e) {
                        logger.warning("Error notificando conexión: " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Notifica desconexión de usuario
     */
    private void notificarDesconexionUsuario(String nombreUsuario) {
        String contenido = nombreUsuario + " abandonó el chat";
        String mensaje = Protocolo.empaquetar("NOTIFICACION", contenido);

        Map<String, UsuarioAutenticado> snapshot;
        synchronized (usuariosConectados) {
            snapshot = new java.util.HashMap<>(usuariosConectados);
        }

        for (Map.Entry<String, UsuarioAutenticado> entrada : snapshot.entrySet()) {
            String usuario = entrada.getKey();
            UsuarioAutenticado usuarioObj = entrada.getValue();

            Object lock = locksUsuarios.get(usuario);
            if (lock != null && usuarioObj.salida != null) {
                synchronized (lock) {
                    try {
                        if (!usuarioObj.salida.checkError()) {
                            usuarioObj.salida.println(mensaje);
                            usuarioObj.salida.flush();
                        }
                    } catch (Exception e) {
                        logger.warning("Error notificando desconexión: " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Obtiene mensaje de monitoreo del estado del servidor
     */
    public String obtenerMensajeConexion(String nombreUsuario) {
        return "[SERVIDOR] " + nombreUsuario + " se ha conectado. Total: " + obtenerCantidadUsuarios();
    }

    /**
     * Obtiene mensaje de desconexión
     */
    public String obtenerMensajeDesconexion(String nombreUsuario) {
        return "[SERVIDOR] " + nombreUsuario + " se ha desconectado";
    }

    /**
     * Obtiene estado de monitoreo
     */
    public String obtenerEstadoMonitoreo() {
        return "[MONITOREO] Usuarios conectados: " + obtenerCantidadUsuarios() + " - " + obtenerListaUsuarios();
    }
}
