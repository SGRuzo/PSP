import java.io.*;
import java.net.Socket;
import java.util.logging.Logger;

/**
 * Manejador de cliente que se ejecuta en su propio hilo.
 * Gestiona la comunicación con un cliente específico.
 * Implementa autenticación obligatoria con contraseña y validación de roles.
 * Procesa comandos de usuario y administración con control de permisos.
 *
 * Implementa Runnable en lugar de extender Thread (mejor práctica de separación de responsabilidades)
 * Se ejecuta en un hilo separado del ExecutorService del servidor.
 */
public class ManejadorCliente implements Runnable {
    private static final Logger logger = Logger.getLogger(ManejadorCliente.class.getName());

    private final Socket socketCliente;
    private final int idCliente;
    private final GestorUsuario gestorUsuarios;
    private BufferedReader entrada;
    private PrintWriter salida;
    private String nombreUsuario;
    private String rolUsuario; // Rol autenticado del usuario
    private volatile boolean conectado;
    private volatile boolean autenticado; // Indica si el usuario pasó autenticación

    /**
     * Constructor del manejador de cliente
     */
    public ManejadorCliente(Socket socketCliente, int idCliente, GestorUsuario gestorUsuarios) {
        this.socketCliente = socketCliente;
        this.idCliente = idCliente;
        this.gestorUsuarios = gestorUsuarios;
        this.nombreUsuario = null;
        this.rolUsuario = null;
        this.conectado = false;
        this.autenticado = false;
        logger.info("ManejadorCliente #" + idCliente + " creado");
    }

    /**
     * Ejecuta la tarea de manejo del cliente en su propio hilo.
     * Inicializa streams, procesa mensajes y cierra la conexión.
     */
    @Override
    public void run() {
        try {
            inicializarStreams();
            procesarMensajes();
        } catch (IOException e) {
            logger.severe("Error al inicializar streams para cliente #" + idCliente + ": " + e.getMessage());
        } finally {
            cerrarConexion();
        }
    }

    /**
     * Inicializa los streams de entrada y salida
     */
    private void inicializarStreams() throws IOException {
        try {
            entrada = new BufferedReader(new InputStreamReader(socketCliente.getInputStream()));
            salida = new PrintWriter(socketCliente.getOutputStream(), true);
            conectado = true;
            logger.info("Streams inicializados para cliente #" + idCliente);
        } catch (IOException e) {
            logger.severe("Error al inicializar streams para cliente #" + idCliente);
            throw e;
        }
    }

    /**
     * Procesa los mensajes que llegan del cliente
     * El cliente DEBE autenticarse primero antes de usar otros comandos
     */
    private void procesarMensajes() {
        String linea;
        try {
            while (conectado && (linea = entrada.readLine()) != null) {
                logger.info("Mensaje recibido de cliente #" + idCliente + ": " + linea);

                String[] partes = Protocolo.desempaquetar(linea.trim());

                if (partes.length == 0) {
                    logger.warning("Mensaje vacío recibido de cliente #" + idCliente);
                    continue;
                }

                String comando = partes[0];
                procesarComando(comando, partes);
            }
        } catch (IOException e) {
            if (conectado) {
                logger.warning("Desconexión del cliente #" + idCliente + ": " + e.getMessage());
            }
        } finally {
            conectado = false;
        }
    }

    /**
     * Procesa un comando recibido del cliente
     * IMPORTANTE: LOGIN es el único comando permitido sin autenticación
     */
    private void procesarComando(String comando, String[] partes) {
        // El LOGIN es el único comando permitido antes de autenticarse
        if (Protocolo.LOGIN.equals(comando)) {
            procesarLogin(partes);
            return;
        }

        // Todos los demás comandos requieren autenticación
        if (!autenticado) {
            logger.warning("Intento de comando sin autenticación de cliente #" + idCliente + ": " + comando);
            enviarRespuesta(Protocolo.AUTH_ERROR, "Debe autenticarse primero con LOGIN");
            return;
        }

        // Procesar comandos autenticados
        switch (comando) {
            case Protocolo.MSG:
                procesarMensaje(partes);
                break;

            case Protocolo.BYE:
                procesarBye();
                break;

            case Protocolo.PING:
                procesarPing();
                break;

            case Protocolo.LIST:
                procesarList();
                break;

            case Protocolo.KICK:
                procesarKick(partes);
                break;

            case Protocolo.SHUTDOWN:
                procesarShutdown();
                break;

            case "/ping":
                procesarPing();
                break;

            case "/list":
                procesarList();
                break;

            default:
                logger.warning("Comando desconocido de cliente #" + idCliente + ": " + comando);
                enviarRespuesta(Protocolo.ERROR, "Comando desconocido: " + comando);
                break;
        }
    }

    /**
     * Procesa el comando LOGIN con autenticación
     * Formato: LOGIN|nombreUsuario|password
     * Valida las credenciales y bloquea tras 3 intentos fallidos
     */
    private void procesarLogin(String[] partes) {
        // Ya autenticado - no permitir nuevo login
        if (autenticado) {
            logger.warning("Intento de re-login de usuario autenticado: " + nombreUsuario);
            enviarRespuesta(Protocolo.ERROR, "Ya estás autenticado como " + nombreUsuario);
            return;
        }

        if (partes.length < 3) {
            logger.warning("LOGIN incompleto de cliente #" + idCliente);
            enviarRespuesta(Protocolo.ERROR, "Formato LOGIN incorrecto. Usa: LOGIN|usuario|password");
            return;
        }

        String usuario = partes[1].trim();
        String password = partes[2];

        if (usuario.isEmpty()) {
            logger.warning("Intento de login con usuario vacío");
            enviarRespuesta(Protocolo.ERROR, "El nombre de usuario no puede estar vacío");
            return;
        }

        // Validar credenciales (retorna el rol o null si falla)
        String rol = gestorUsuarios.validarCredenciales(usuario, password);

        if (Protocolo.USER_BLOCKED.equals(rol)) {
            logger.warning("Usuario bloqueado por intentos fallidos: " + usuario);
            enviarRespuesta(Protocolo.USER_BLOCKED, "Usuario bloqueado por demasiados intentos fallidos. Intenta en 30 segundos");
            return;
        }

        if (rol == null) {
            logger.warning("Login fallido para usuario: " + usuario);
            enviarRespuesta(Protocolo.AUTH_ERROR, "Usuario o contraseña incorrectos");
            return;
        }

        // Credenciales válidas - registrar usuario
        if (gestorUsuarios.registrarUsuarioAutenticado(usuario, rol, salida)) {
            this.nombreUsuario = usuario;
            this.rolUsuario = rol;
            this.autenticado = true;

            logger.info("Cliente #" + idCliente + " autenticado como: " + nombreUsuario + " (Rol: " + rol + ")");
            System.out.println(gestorUsuarios.obtenerMensajeConexion(nombreUsuario));

            enviarRespuesta(Protocolo.OK, "Login exitoso. Bienvenido " + nombreUsuario +
                          " (Rol: " + rol + "). Usuarios conectados: " + gestorUsuarios.obtenerCantidadUsuarios());
        } else {
            logger.warning("Error al registrar usuario autenticado: " + usuario);
            enviarRespuesta(Protocolo.ERROR, "Error al registrar el usuario en el servidor");
        }
    }

    /**
     * Procesa un mensaje de chat
     * Formato: MSG|contenido
     */
    private void procesarMensaje(String[] partes) {
        if (partes.length < 2) {
            logger.warning("MSG incompleto de cliente #" + idCliente);
            enviarRespuesta(Protocolo.ERROR, "Formato MSG incorrecto");
            return;
        }

        String contenido = partes[1];
        logger.info("Mensaje de " + nombreUsuario + " (#" + idCliente + "): " + contenido);

        int destinatarios = gestorUsuarios.broadcastingGlobal(nombreUsuario, contenido);
        logger.fine("Mensaje reenviado a " + destinatarios + " usuarios");

        enviarRespuesta(Protocolo.OK, "Mensaje enviado a " + destinatarios + " usuarios");
    }

    /**
     * Procesa el comando PING
     */
    private void procesarPing() {
        logger.info("PING recibido de " + nombreUsuario + " (#" + idCliente + ")");
        enviarRespuesta("PONG", "Servidor activo");
    }

    /**
     * Procesa el comando LIST
     */
    private void procesarList() {
        logger.info("LIST solicitado por " + nombreUsuario + " (#" + idCliente + ")");

        String listaUsuarios = gestorUsuarios.obtenerListaUsuarios();
        int cantidad = gestorUsuarios.obtenerCantidadUsuarios();

        enviarRespuesta("LIST", "(" + cantidad + " conectados) " + listaUsuarios);
    }

    /**
     * Procesa el comando KICK (expulsión de usuario)
     * Formato: KICK|nombreUsuarioAExpulsar
     * SOLO permitido para usuarios con rol ADMIN
     */
    private void procesarKick(String[] partes) {
        // Validar permisos de ADMIN
        if (!gestorUsuarios.esAdmin(nombreUsuario)) {
            logger.warning("Intento de KICK sin permisos de ADMIN: " + nombreUsuario);
            enviarRespuesta(Protocolo.PERMISSION_DENIED, "Solo los ADMIN pueden usar /kick");
            return;
        }

        if (partes.length < 2) {
            logger.warning("KICK incompleto de cliente #" + idCliente);
            enviarRespuesta(Protocolo.ERROR, "Formato KICK incorrecto. Usa: KICK|nick");
            return;
        }

        String usuarioAExpulsar = partes[1].trim();

        if (usuarioAExpulsar.isEmpty()) {
            enviarRespuesta(Protocolo.ERROR, "El nombre de usuario a expulsar no puede estar vacío");
            return;
        }

        // No permitir auto-expulsión
        if (usuarioAExpulsar.equals(nombreUsuario)) {
            logger.warning("Intento de auto-expulsión: " + nombreUsuario);
            enviarRespuesta(Protocolo.ERROR, "No puedes expulsarte a ti mismo");
            return;
        }

        // Ejecutar expulsión
        if (gestorUsuarios.expulsarUsuario(usuarioAExpulsar)) {
            logger.info("Usuario " + usuarioAExpulsar + " expulsado por " + nombreUsuario);
            System.out.println("[ADMIN] " + nombreUsuario + " expulsó a " + usuarioAExpulsar);

            // Notificar al ADMIN que ejecutó la expulsión
            enviarRespuesta(Protocolo.OK, "Usuario " + usuarioAExpulsar + " ha sido expulsado");

            // Notificar a todos los demás usuarios
            gestorUsuarios.broadcasting(nombreUsuario, "El usuario " + usuarioAExpulsar + " fue expulsado del servidor", false);
        } else {
            logger.warning("Intento de expulsar usuario inexistente: " + usuarioAExpulsar);
            enviarRespuesta(Protocolo.ERROR, "Usuario no encontrado: " + usuarioAExpulsar);
        }
    }

    /**
     * Procesa el comando SHUTDOWN (cierre del servidor)
     * SOLO permitido para usuarios con rol ADMIN
     */
    private void procesarShutdown() {
        // Validar permisos de ADMIN
        if (!gestorUsuarios.esAdmin(nombreUsuario)) {
            logger.warning("Intento de SHUTDOWN sin permisos de ADMIN: " + nombreUsuario);
            enviarRespuesta(Protocolo.PERMISSION_DENIED, "Solo los ADMIN pueden usar /shutdown");
            return;
        }

        logger.warning("SHUTDOWN iniciado por ADMIN: " + nombreUsuario);
        System.out.println("[ADMIN] " + nombreUsuario + " ha iniciado el SHUTDOWN del servidor");

        // Notificar a todos los usuarios sobre el cierre inminente
        gestorUsuarios.broadcasting(nombreUsuario, "[SERVIDOR] El servidor se está cerrando", false);

        enviarRespuesta(Protocolo.OK, "Iniciando cierre del servidor");

        // Dar tiempo para que los mensajes se envíen
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            logger.warning("Interrupción durante espera de SHUTDOWN: " + e.getMessage());
        }

        // Solicitar cierre del servidor mediante System.exit
        logger.severe("SHUTDOWN completado. Cerrando JVM");
        System.exit(0);
    }

    /**
     * Procesa el comando BYE (desconexión)
     */
    private void procesarBye() {
        logger.info(nombreUsuario + " (#" + idCliente + ") solicita desconexión");
        gestorUsuarios.desconectarUsuario(nombreUsuario);
        System.out.println(gestorUsuarios.obtenerEstadoMonitoreo());

        enviarRespuesta(Protocolo.OK, "Desconexión confirmada");
        conectado = false;
    }

    /**
     * Envía una respuesta al cliente
     */
    private void enviarRespuesta(String respuesta, String contenido) {
        String mensaje = Protocolo.empaquetar(respuesta, contenido);

        if (conectado && salida != null) {
            salida.println(mensaje);
            salida.flush();
            logger.info("Respuesta enviada a cliente #" + idCliente + ": " + respuesta);
        }
    }

    /**
     * Cierra la conexión con el cliente de forma segura
     */
    private void cerrarConexion() {
        logger.info("Iniciando cierre de conexión para cliente #" + idCliente);
        conectado = false;

        if (autenticado && nombreUsuario != null) {
            gestorUsuarios.desconectarUsuario(nombreUsuario);
            System.out.println(gestorUsuarios.obtenerMensajeDesconexion(nombreUsuario));
            System.out.println(gestorUsuarios.obtenerEstadoMonitoreo());
            logger.info("Usuario " + nombreUsuario + " desconectado");
        }

        try {
            if (entrada != null) entrada.close();
        } catch (IOException e) {
            logger.warning("Error al cerrar entrada: " + e.getMessage());
        }

        try {
            if (salida != null) salida.close();
        } catch (Exception e) {
            logger.warning("Error al cerrar salida: " + e.getMessage());
        }

        if (socketCliente != null && !socketCliente.isClosed()) {
            try {
                socketCliente.close();
                logger.info("Socket cerrado para cliente #" + idCliente);
            } catch (IOException e) {
                logger.warning("Error al cerrar socket: " + e.getMessage());
            }
        }

        logger.info("Cierre completo de cliente #" + idCliente);
    }
}
