import javax.swing.*;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Controlador del cliente de chat.
 * Gestiona la comunicación entre la Vista (UI) y el Modelo (lógica).
 * Coordina las acciones del usuario y actualiza la interfaz.
 * El controlador actúa como intermediario:
 * View → Eventos → Controller → Model/EscuchaServidor → Actualizar View
 */
public class Controller {
    private static final Logger logger = Logger.getLogger(Controller.class.getName());

    private final View vista;
    private final Model modelo;
    private ClienteConExecutor clienteExecutor;
    private String nombreUsuario;
    private volatile boolean conectado;

    /**
     * Constructor del controlador
     *
     * @param vista La vista (interfaz gráfica)
     * @param modelo El modelo de datos y conexión
     */
    public Controller(View vista, Model modelo) {
        this.vista = vista;
        this.modelo = modelo;
        this.conectado = false;
        this.nombreUsuario = null;

        inicializarEventos();
        logger.info("Controlador inicializado");
    }

    /**
     * Inicializa los listeners de los botones de la vista
     */
    private void inicializarEventos() {
        // Botón Conectar
        vista.obtenerBtnConectar().addActionListener(e -> accionConectar());

        // Botón Desconectar
        vista.obtenerBtnDesconectar().addActionListener(e -> accionDesconectar());

        // Botón Enviar
        vista.obtenerBtnEnviar().addActionListener(e -> accionEnviarMensaje());

        // Botón /list
        vista.obtenerBtnList().addActionListener(e -> accionComandoList());

        // Botón /ping
        vista.obtenerBtnPing().addActionListener(e -> accionComandoPing());

        logger.info("Eventos inicializados");
    }

    /**
     * Acción al presionar el botón Conectar
     */
    private void accionConectar() {
        logger.info("Acción: Conectar");

        // Solicitar datos del servidor
        String[] datosServidor = vista.solicitarDatosServidor();
        if (datosServidor == null) {
            logger.info("Conexión cancelada por el usuario");
            return;
        }

        String host = datosServidor[0];
        int puerto;

        try {
            puerto = Integer.parseInt(datosServidor[1]);
        } catch (NumberFormatException e) {
            vista.mostrarError("Error", "Puerto inválido");
            logger.warning("Puerto inválido: " + datosServidor[1]);
            return;
        }

        // Solicitar nombre de usuario
        nombreUsuario = vista.solicitarNombreUsuario();
        if (nombreUsuario == null || nombreUsuario.trim().isEmpty()) {
            logger.info("Conexión cancelada: nombre de usuario vacío");
            return;
        }

        // Intentar conectar
        conectarAlServidor(host, puerto, nombreUsuario);
    }

    /**
     * Conecta al servidor usando ClienteConExecutor con UN ÚNICO SOCKET
     * IMPORTANTE: Se utiliza un único socket bidireccional para enviar y recibir mensajes
     * Esto evita problemas de sincronización entre múltiples conexiones
     *
     * @param host Dirección del servidor
     * @param puerto Puerto del servidor
     * @param usuario Nombre de usuario para login
     */
    private void conectarAlServidor(String host, int puerto, String usuario) {
        try {
            logger.info("Conectando a " + host + ":" + puerto);

            // Crear ClienteConExecutor si no existe
            if (clienteExecutor == null) {
                clienteExecutor = new ClienteConExecutor(this);
            }

            // ✅ UN ÚNICO SOCKET creado aquí
            clienteExecutor.conectar(host, puerto);
            logger.info("ClienteConExecutor conectado con UN socket");

            // ✅ ENVIAR LOGIN por EL MISMO SOCKET
            clienteExecutor.enviarComando(Protocolo.LOGIN, usuario);
            logger.info("Comando LOGIN enviado por el mismo socket");

            // Actualizar UI
            conectado = true;
            nombreUsuario = usuario;
            vista.establecerEstado(true);
            vista.establecerUsuario(nombreUsuario);
            vista.mostrarMensajeSistema("[SISTEMA] Conectado como " + nombreUsuario);
            vista.mostrarMensajeSistema("Conectado a la sala de chat");
            logger.info("Cliente conectado y escuchando por UN ÚNICO socket");

        } catch (IOException e) {
            logger.severe("Error al conectar: " + e.getMessage());
            vista.mostrarError("Error de Conexión", "No se pudo conectar a " + host + ":" + puerto);
            conectado = false;
            vista.establecerEstado(false);
        } catch (Exception e) {
            logger.severe("Error inesperado: " + e.getMessage());
            vista.mostrarError("Error", "Error inesperado: " + e.getMessage());
            conectado = false;
        }
    }

    /**
     * Acción al presionar el botón Desconectar
     */
    private void accionDesconectar() {
        logger.info("Acción: Desconectar");

        if (!conectado) {
            vista.mostrarError("Error", "No estás conectado");
            return;
        }

        try {
            // ✅ Enviar comando BYE por el ÚNICO socket
            clienteExecutor.enviarComando(Protocolo.BYE);
            logger.info("Comando BYE enviado");

            // Esperar un poco para que el servidor responda
            Thread.sleep(200);

            desconectar();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            desconectar();
        } catch (IOException e) {
            logger.warning("Error al enviar BYE: " + e.getMessage());
            desconectar();
        }
    }

    /**
     * Desconecta del servidor y limpia recursos
     * Método idempotente - puede ser llamado múltiples veces sin problemas
     * Cierra el ExecutorService y todos los hilos asociados
     */
    private void desconectar() {
        logger.info("Desconectando...");

        // 1. Marcar como desconectado (evita circular waits)
        conectado = false;

        // 2. Detener ClienteConExecutor (cierra executor, escucha, y socket)
        if (clienteExecutor != null) {
            try {
                clienteExecutor.desconectar();
                logger.info("ClienteConExecutor detenido");
            } catch (Exception e) {
                logger.warning("Error al detener ClienteConExecutor: " + e.getMessage());
            }
            clienteExecutor = null;
        }

        // 3. Actualizar UI
        vista.establecerEstado(false);
        vista.mostrarMensajeSistema("[SISTEMA] Desconectado");
        nombreUsuario = null;

        logger.info("Desconexión completada");
    }

    /**
     * Acción al presionar el botón Enviar
     */
    private void accionEnviarMensaje() {
        if (!conectado) {
            vista.mostrarError("Error", "No estás conectado");
            return;
        }

        String mensaje = vista.obtenerTextoEntrada().trim();

        if (mensaje.isEmpty()) {
            return;
        }

        logger.info("Enviando mensaje: " + mensaje);

        try {
            // ✅ Usar ClienteConExecutor para enviar por el ÚNICO socket
            clienteExecutor.enviarComando(Protocolo.MSG, mensaje);

            // Mostrar el mensaje en la vista
            vista.mostrarMensaje("Yo: " + mensaje, true);

            // Limpiar campo de entrada
            vista.limpiarEntrada();

        } catch (Exception e) {
            logger.severe("Error al enviar mensaje: " + e.getMessage());
            vista.mostrarError("Error", "No se pudo enviar el mensaje");
        }
    }

    /**
     * Acción al presionar el botón /list
     */
    private void accionComandoList() {
        if (!conectado) {
            vista.mostrarError("Error", "No estás conectado");
            return;
        }

        logger.info("Comando: /list");

        try {
            // ✅ Usar ClienteConExecutor para enviar por el ÚNICO socket
            clienteExecutor.enviarComando(Protocolo.LIST);
            logger.info("Comando LIST enviado");

        } catch (IOException e) {
            logger.severe("Error al enviar /list: " + e.getMessage());
            vista.mostrarError("Error", "No se pudo enviar el comando /list");
        }
    }

    /**
     * Acción al presionar el botón /ping
     */
    private void accionComandoPing() {
        if (!conectado) {
            vista.mostrarError("Error", "No estás conectado");
            return;
        }

        logger.info("Comando: /ping");

        try {
            // ✅ Usar ClienteConExecutor para enviar por el ÚNICO socket
            clienteExecutor.enviarComando(Protocolo.PING);
            logger.info("Comando PING enviado");

        } catch (IOException e) {
            logger.severe("Error al enviar /ping: " + e.getMessage());
            vista.mostrarError("Error", "No se pudo enviar el comando /ping");
        }
    }

    /**
     * Muestra un mensaje en la vista
     * Llamado por EscuchaServidor cuando recibe mensajes del servidor
     *
     * @param mensaje El mensaje a mostrar
     */
    public void mostrarMensaje(String mensaje) {
        // Usar SwingUtilities para actualizar UI desde el hilo de escucha
        SwingUtilities.invokeLater(() -> vista.mostrarMensaje(mensaje, false));
    }

    /**
     * Muestra un diálogo de error
     * Llamado por EscuchaServidor cuando hay errores
     *
     * @param titulo Título del diálogo
     * @param mensaje Mensaje de error
     */
    public void procesarError(String titulo, String mensaje) {
        // Usar SwingUtilities para actualizar UI desde el hilo de escucha
        SwingUtilities.invokeLater(() -> vista.mostrarError(titulo, mensaje));
    }

    /**
     * Procesa la desconexión iniciada por el servidor
     * Llamado por EscuchaServidor cuando el servidor cierra la conexión
     */
    public void procesarDesconexion() {
        SwingUtilities.invokeLater(() -> {
            mostrarMensaje("[SISTEMA] Servidor cerró la conexión");
            desconectar();
        });
    }


    /**
     * Obtiene el modelo
     *
     * @return El modelo del cliente
     */
    public Model obtenerModelo() {
        return modelo;
    }
}
