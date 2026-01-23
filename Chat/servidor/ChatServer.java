

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Servidor de chat que gestiona múltiples conexiones de clientes.
 * Utiliza un ExecutorService (thread pool) para manejar las conexiones de forma eficiente.
 */
public class ChatServer {
    private static final Logger logger = Logger.getLogger(ChatServer.class.getName());

    private final int puerto;
    private ServerSocket serverSocket;
    private ExecutorService executorService;  // Thread pool para manejar clientes
    private volatile boolean ejecutandose;
    private int contadorConexiones;
    private final GestorUsuario gestorUsuarios;

    /**
     * Constructor del servidor de chat
     *
     * @param puerto Puerto en el que escuchará el servidor
     */
    public ChatServer(int puerto) {
        this.puerto = puerto;
        this.ejecutandose = false;
        this.contadorConexiones = 0;
        this.gestorUsuarios = new GestorUsuario();
        logger.info("GestorUsuarios creado para el servidor");
    }

    /**
     * Inicia el servidor y comienza a aceptar conexiones
     */
    public void iniciar() {
        try {
            iniciarServidor();
            aceptarConexiones();
        } catch (IOException e) {
            logger.severe("Error en el servidor: " + e.getMessage());
            logger.fine("Stack trace: " + java.util.Arrays.toString(e.getStackTrace()));
        } finally {
            detenerServidor();
        }
    }

    /**
     * Inicializa el servidor socket y el thread pool
     *
     * @throws IOException Si no se puede crear el ServerSocket
     */
    private void iniciarServidor() throws IOException {
        try {
            serverSocket = new ServerSocket(puerto);
            logger.info("ServerSocket creado en puerto: " + puerto);

            // Crear thread pool con máximo de 10 threads concurrentes
            executorService = Executors.newFixedThreadPool(10);
            logger.info("ExecutorService creado con thread pool de 10 threads");

            ejecutandose = true;
            logger.info("Servidor iniciado correctamente");

        } catch (IOException e) {
            logger.severe("No se pudo iniciar el servidor en puerto " + puerto);
            throw e;
        }
    }

    /**
     * Acepta conexiones de clientes de forma continua
     * Cada conexión se asigna a un hilo del thread pool
     */
    private void aceptarConexiones() {
        logger.info("Ningún cliente conectado");

        while (ejecutandose) {
            try {
                // 1. Aceptar socket del cliente
                Socket socketCliente = serverSocket.accept();
                contadorConexiones++;

                String ipCliente = socketCliente.getInetAddress().getHostAddress();
                int puertoCliente = socketCliente.getPort();

                logger.info("Nueva conexión #" + contadorConexiones + " desde " +
                           ipCliente + ":" + puertoCliente);

                // 2. Crear manejador del cliente (implementa Runnable)
                ManejadorCliente manejadorCliente = new ManejadorCliente(socketCliente, contadorConexiones, gestorUsuarios);

                // 3. Ejecutar en thread pool en lugar de crear Thread directamente
                executorService.execute(manejadorCliente);
                logger.info("Manejador del cliente #" + contadorConexiones + " encolado en thread pool");

            } catch (SocketException e) {
                if (ejecutandose) {
                    logger.warning("Error de socket: " + e.getMessage());
                }
            } catch (IOException e) {
                if (ejecutandose) {
                    logger.severe("Error al aceptar conexión: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Detiene el servidor de forma ordenada
     */
    public synchronized void detenerServidor() {
        logger.info("Deteniendo servidor...");
        ejecutandose = false;

        // Notificar a todos los usuarios que el servidor se cierra
        if (gestorUsuarios != null) {
            gestorUsuarios.notificarCierreServidor();
        }

        // Desconectar todos los usuarios
        if (gestorUsuarios != null) {
            gestorUsuarios.desconectarTodos();
        }

        // Cerrar ServerSocket
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                logger.info("ServerSocket cerrado");
            } catch (IOException e) {
                logger.warning("Error al cerrar ServerSocket: " + e.getMessage());
            }
        }

        // Shutdown ordenado del ExecutorService
        if (executorService != null && !executorService.isShutdown()) {
            logger.info("Iniciando shutdown del thread pool...");
            executorService.shutdown();
            try {
                // Esperar hasta 5 segundos a que terminen los threads
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warning("Thread pool no terminó en el tiempo límite. Forzando shutdown...");
                    executorService.shutdownNow();
                }
                logger.info("Thread pool cerrado correctamente");
            } catch (InterruptedException e) {
                logger.warning("Interrupción durante shutdown del thread pool: " + e.getMessage());
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        logger.info("Servidor detenido");
    }

    /**
     * Obtiene el número de conexiones totales procesadas
     *
     * @return Contador de conexiones
     */
    public int obtenerContadorConexiones() {
        return contadorConexiones;
    }

    /**
     * Verifica si el servidor está en ejecución
     *
     * @return true si el servidor está ejecutándose
     */
    public boolean estaEjecutandose() {
        return ejecutandose;
    }

    /**
     * Obtiene el puerto del servidor
     *
     * @return Puerto configurado
     */
    public int obtenerPuerto() {
        return puerto;
    }

    /**
     * Obtiene el gestor de usuarios del servidor
     *
     * @return Instancia del GestorUsuarios
     */
    public GestorUsuario obtenerGestorUsuarios() {
        return gestorUsuarios;
    }
}
