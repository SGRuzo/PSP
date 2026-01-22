
import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.logging.Logger;

/**
 * Manejador de cliente que se ejecuta en su propio hilo.
 * Gestiona la comunicación con un cliente específico.
 * Procesa el protocolo de texto plano y maneja comandos especiales como /list y /ping.
 *
 * Implementa Runnable en lugar de extender Thread (mejor práctica de separación de responsabilidades)
 * Se ejecuta en un hilo separado del ExecutorService del servidor.
 */
public class ManejadorCliente implements Runnable {
    private static final Logger logger = Logger.getLogger(ManejadorCliente.class.getName());
    private static final int SOCKET_TIMEOUT = 30000;  // 30 segundos

    private final Socket socketCliente;
    private final int idCliente;
    private final GestorUsuario gestorUsuarios;
    private BufferedReader entrada;
    private PrintWriter salida;
    private String nombreUsuario;
    private volatile boolean conectado;  // volatile para visibilidad entre hilos

    /**
     * Constructor del manejador de cliente
     *
     * @param socketCliente Socket del cliente conectado
     * @param idCliente Identificador único del cliente
     * @param gestorUsuarios Referencia al gestor de usuarios compartido
     */
    public ManejadorCliente(Socket socketCliente, int idCliente, GestorUsuario gestorUsuarios) {
        this.socketCliente = socketCliente;
        this.idCliente = idCliente;
        this.gestorUsuarios = gestorUsuarios;
        this.nombreUsuario = null;
        this.conectado = false;
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
     * Inicializa los streams de entrada y salida con timeout
     *
     * @throws IOException Si hay problemas al crear los streams
     */
    private void inicializarStreams() throws IOException {
        try {
            // Configurar timeout del socket para evitar bloqueos indefinidos
            socketCliente.setSoTimeout(SOCKET_TIMEOUT);

            entrada = new BufferedReader(new InputStreamReader(socketCliente.getInputStream()));
            salida = new PrintWriter(socketCliente.getOutputStream(), true);
            conectado = true;

            logger.info("Streams inicializados para cliente #" + idCliente +
                       " con timeout de " + SOCKET_TIMEOUT + "ms");
        } catch (IOException e) {
            logger.severe("Error al inicializar streams para cliente #" + idCliente);
            throw e;
        }
    }

    /**
     * Procesa los mensajes que llegan del cliente
     */
    private void procesarMensajes() {
        String linea;
        try {
            while (conectado && (linea = entrada.readLine()) != null) {
                logger.info("Mensaje recibido de cliente #" + idCliente + ": " + linea);

                // Desempaquetar el mensaje usando el protocolo
                String[] partes = Protocolo.desempaquetar(linea.trim());

                if (partes.length == 0) {
                    logger.warning("Mensaje vacío recibido de cliente #" + idCliente);
                    continue;
                }

                String comando = partes[0];

                // Procesar el comando recibido (no bloqueante)
                procesarComando(comando, partes);
            }
        } catch (SocketTimeoutException e) {
            logger.warning("Timeout en cliente #" + idCliente +
                          ": Sin actividad en " + SOCKET_TIMEOUT + "ms");
            conectado = false;
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
     *
     * @param comando El comando a procesar
     * @param partes Array con los componentes del mensaje desempaquetado
     */
    private void procesarComando(String comando, String[] partes) {
        switch (comando) {
            case Protocolo.LOGIN:
                procesarLogin(partes);
                break;

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

            case "/ping":
                // Comando de slash directo
                procesarPing();
                break;

            case "/list":
                // Comando de slash directo
                procesarList();
                break;

            default:
                logger.warning("Comando desconocido de cliente #" + idCliente + ": " + comando);
                enviarRespuesta(Protocolo.ERROR, "Comando desconocido: " + comando);
                break;
        }
    }

    /**
     * Procesa el comando LOGIN
     * Formato: LOGIN|nombreUsuario
     *
     * @param partes Array del mensaje desempaquetado
     */
    private void procesarLogin(String[] partes) {
        if (partes.length < 2) {
            logger.warning("LOGIN incompleto de cliente #" + idCliente);
            enviarRespuesta(Protocolo.ERROR, "Formato LOGIN incorrecto");
            return;
        }

        String nuevoNombre = partes[1].trim();

        // Validar que el nombre no esté vacío
        if (nuevoNombre.isEmpty()) {
            logger.warning("Intento de login con nombre vacío del cliente #" + idCliente);
            enviarRespuesta(Protocolo.ERROR, "El nombre de usuario no puede estar vacío");
            return;
        }

        // Validar que el usuario no esté ya registrado
        if (gestorUsuarios.usuarioConectado(nuevoNombre)) {
            logger.warning("Intento de login con usuario duplicado: " + nuevoNombre);
            enviarRespuesta(Protocolo.ERROR, "El usuario " + nuevoNombre + " ya está conectado");
            return;
        }

        // Registrar el usuario en el GestorUsuarios
        if (gestorUsuarios.registrarUsuario(nuevoNombre, salida)) {
            this.nombreUsuario = nuevoNombre;
            logger.info("Cliente #" + idCliente + " autenticado como: " + nombreUsuario);
            enviarRespuesta(Protocolo.OK, "Login exitoso como " + nombreUsuario +
                          ". Usuarios conectados: " + gestorUsuarios.obtenerCantidadUsuarios());
        } else {
            logger.warning("Fallo al registrar usuario: " + nuevoNombre);
            enviarRespuesta(Protocolo.ERROR, "Error al registrar el usuario");
        }
    }

    /**
     * Procesa un mensaje normal
     * Formato: MSG|contenido
     *
     * @param partes Array del mensaje desempaquetado
     */
    private void procesarMensaje(String[] partes) {
        if (nombreUsuario == null) {
            logger.warning("Intento de enviar mensaje sin login de cliente #" + idCliente);
            enviarRespuesta(Protocolo.ERROR, "Debe hacer login primero");
            return;
        }

        if (partes.length < 2) {
            logger.warning("MSG incompleto de cliente #" + idCliente);
            enviarRespuesta(Protocolo.ERROR, "Formato MSG incorrecto");
            return;
        }

        String contenido = partes[1];
        logger.info("Mensaje de " + nombreUsuario + " (#" + idCliente + "): " + contenido);

        // Hacer broadcasting del mensaje a todos los usuarios
        int destinatarios = gestorUsuarios.broadcastingGlobal(nombreUsuario, contenido);
        logger.fine("Mensaje reenviado a " + destinatarios + " usuarios");

        enviarRespuesta(Protocolo.OK, "Mensaje enviado a " + destinatarios + " usuarios");
    }

    /**
     * Procesa el comando PING
     * Responde con PONG
     */
    private void procesarPing() {
        if (nombreUsuario == null) {
            logger.warning("PING sin login de cliente #" + idCliente);
            enviarRespuesta(Protocolo.ERROR, "Debe hacer login primero");
            return;
        }

        logger.info("PING recibido de " + nombreUsuario + " (#" + idCliente + ")");
        enviarRespuesta("PONG", "Servidor activo");
    }

    /**
     * Procesa el comando LIST
     * Retorna la lista de usuarios conectados
     */
    private void procesarList() {
        if (nombreUsuario == null) {
            logger.warning("LIST sin login de cliente #" + idCliente);
            enviarRespuesta(Protocolo.ERROR, "Debe hacer login primero");
            return;
        }

        logger.info("LIST solicitado por " + nombreUsuario + " (#" + idCliente + ")");

        String listaUsuarios = gestorUsuarios.obtenerListaUsuarios();
        int cantidad = gestorUsuarios.obtenerCantidadUsuarios();

        enviarRespuesta("LIST", "(" + cantidad + " conectados) " + listaUsuarios);
    }

    /**
     * Procesa el comando BYE (desconexión)
     */
    private void procesarBye() {
        if (nombreUsuario == null) {
            logger.warning("BYE sin login de cliente #" + idCliente);
        } else {
            logger.info(nombreUsuario + " (#" + idCliente + ") solicita desconexión");
            gestorUsuarios.desconectarUsuario(nombreUsuario);
        }

        enviarRespuesta(Protocolo.OK, "Desconexión confirmada");
        conectado = false;
    }

    /**
     * Envía una respuesta al cliente directamente
     * Formato: RESPUESTA|contenido
     *
     * @param respuesta Tipo de respuesta
     * @param contenido Contenido de la respuesta
     */
    private void enviarRespuesta(String respuesta, String contenido) {
        String mensaje = Protocolo.empaquetar(respuesta, contenido);

        if (conectado && salida != null) {
            salida.println(mensaje);
            salida.flush();
            logger.info("Respuesta enviada a cliente #" + idCliente + ": " + mensaje);
        }
    }

    /**
     * Cierra la conexión con el cliente de forma segura
     */
    private void cerrarConexion() {
        logger.info("Iniciando cierre de conexión para cliente #" + idCliente);
        conectado = false;

        // Desconectar del gestor si estaba registrado
        if (nombreUsuario != null) {
            gestorUsuarios.desconectarUsuario(nombreUsuario);
            logger.info("Usuario " + nombreUsuario + " desconectado del gestor");
        }

        try {
            if (entrada != null) {
                entrada.close();
            }
        } catch (IOException e) {
            logger.warning("Error al cerrar BufferedReader: " + e.getMessage());
        }

        try {
            if (salida != null) {
                salida.close();
            }
        } catch (Exception e) {
            logger.warning("Error al cerrar PrintWriter: " + e.getMessage());
        }

        if (socketCliente != null && !socketCliente.isClosed()) {
            try {
                socketCliente.close();
                logger.info("Conexión cerrada para cliente #" + idCliente);
            } catch (IOException e) {
                logger.warning("Error al cerrar socket del cliente #" + idCliente + ": " + e.getMessage());
            }
        }

        logger.info("Cierre completo de cliente #" + idCliente);
    }
}
