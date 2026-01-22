import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * Gestor de conexión cliente que utiliza ExecutorService para manejar multihilo.
 * Permite que múltiples clientes se conecten al servidor y ejecuten EscuchaServidor
 * en hilos separados sin congelar la UI.
 */
public class ClienteConExecutor {
    private static final Logger logger = Logger.getLogger(ClienteConExecutor.class.getName());

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> escuchaFuture;
    private EscuchaServidor escucha;
    private PrintWriter salida;
    private Socket socket;
    private final Controller controlador;

    /**
     * Constructor que recibe la referencia al controlador
     *
     * @param controlador Referencia al controlador de la UI
     */
    public ClienteConExecutor(Controller controlador) {
        this.controlador = controlador;
        logger.info("ClienteConExecutor inicializado");
    }

    /**
     * Conecta al servidor y inicia el hilo de escucha
     *
     * @param host Dirección del servidor
     * @param puerto Puerto del servidor
     * @throws Exception Si falla la conexión
     */
    public void conectar(String host, int puerto) throws Exception {
        try {
            logger.info("Intentando conectar a " + host + ":" + puerto);
            socket = new Socket(host, puerto);

            BufferedReader entrada = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            );
            salida = new PrintWriter(socket.getOutputStream(), true);

            // Crear y ejecutar EscuchaServidor en el executor
            escucha = new EscuchaServidor(entrada, controlador);
            escuchaFuture = executor.submit(escucha);

            logger.info("Cliente conectado a " + host + ":" + puerto);
            logger.info("EscuchaServidor iniciado en hilo separado");
            controlador.mostrarMensaje("[CONECTADO] Conectado al servidor");

        } catch (Exception e) {
            logger.severe("Error al conectar: " + e.getMessage());
            controlador.mostrarMensaje("[ERROR] No se pudo conectar al servidor: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Envía un mensaje al servidor
     *
     * @param mensaje El mensaje a enviar
     */
    public void enviarMensaje(String mensaje) {
        if (salida != null && !salida.checkError()) {
            salida.println(mensaje);
            logger.fine("Mensaje enviado: " + mensaje);
        } else {
            logger.warning("No se puede enviar mensaje, conexión perdida");
            controlador.mostrarMensaje("[ERROR] No hay conexión con el servidor");
        }
    }

    /**
     * Desconecta del servidor y detiene el executor
     */
    public void desconectar() {
        logger.info("Iniciando desconexión...");

        try {
            // Detener EscuchaServidor
            if (escucha != null) {
                escucha.detener();
                logger.info("EscuchaServidor detenido");
            }

            // Cancelar el Future
            if (escuchaFuture != null) {
                escuchaFuture.cancel(true);
                logger.info("Future cancelado");
            }

            // Cerrar PrintWriter
            if (salida != null) {
                salida.close();
                logger.info("PrintWriter cerrado");
            }

            // Cerrar Socket
            if (socket != null && !socket.isClosed()) {
                socket.close();
                logger.info("Socket cerrado");
            }

        } catch (Exception e) {
            logger.warning("Error al desconectar: " + e.getMessage());
        } finally {
            // Detener el executor
            executor.shutdownNow();
            logger.info("ExecutorService detenido - Cliente desconectado");
            controlador.mostrarMensaje("[DESCONECTADO] Desconectado del servidor");
        }
    }

    /**
     * Verifica si el cliente está conectado
     *
     * @return true si está conectado, false en caso contrario
     */
    public boolean estaConectado() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * Verifica si el hilo de escucha está activo
     *
     * @return true si está escuchando, false en caso contrario
     */
    public boolean estaEscuchando() {
        return escucha != null && escucha.estaEscuchando();
    }
}
