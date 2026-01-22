import javax.swing.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Clase principal del cliente de chat.
 * Punto de entrada de la aplicación del cliente.
 * Inicializa el MVC (Modelo-Vista-Controlador) y lanza la aplicación.
 * Utiliza ExecutorService para gestionar hilos de forma eficiente.
 */
public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    private static ExecutorService executorService;

    /**
     * Metodo principal de la aplicación
     *
     * @param args Argumentos de línea de comandos (no se usan)
     */
    public static void main(String[] args) {
        logger.info("Iniciando Cliente de Chat...");

        // Crear ExecutorService con un pool de hilos
        executorService = Executors.newFixedThreadPool(3);

        // Usar SwingUtilities.invokeLater para ejecutar en el EDT
        SwingUtilities.invokeLater(() -> {
            try {
                // Crear el modelo en un hilo separado
                executorService.submit(() -> {
                    try {
                        Model modelo = new Model();
                        logger.info("Modelo creado");

                        // Crear la vista en el EDT
                        SwingUtilities.invokeLater(() -> {
                            View vista = new View();
                            logger.info("Vista creada");

                            // Crear el controlador
                            Controller controlador = new Controller(vista, modelo);
                            logger.info("Controlador creado");

                            logger.info("Aplicación iniciada correctamente");
                            System.out.println("  CLIENTE DE CHAT");
                            System.out.println("Esperando conexión...");

                            // Configurar el cierre de la aplicación
                            vista.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                            vista.addWindowListener(new java.awt.event.WindowAdapter() {
                                @Override
                                public void windowClosing(java.awt.event.WindowEvent e) {
                                    shutdownExecutor();
                                }
                            });
                        });
                    } catch (Exception e) {
                        logger.severe("Error al crear el modelo: " + e.getMessage());
                        e.printStackTrace();
                        mostrarError(e);
                    }
                });} catch (Exception e) {
                logger.severe("Error al iniciar la aplicación: " + e.getMessage());
                e.printStackTrace();
                mostrarError(e);
            }
        });
    }

    /**
     * Muestra un diálogo de error al usuario
     *
     * @param e Excepción ocurrida
     */
    private static void mostrarError(Exception e) {
        JOptionPane.showMessageDialog(null,
                "Error al iniciar la aplicación: " + e.getMessage(),
                "Error Fatal",
                JOptionPane.ERROR_MESSAGE);
        shutdownExecutor();
        System.exit(1);
    }

    /**
     * Detiene el ExecutorService de forma ordenada
     */
    private static void shutdownExecutor() {
        if (executorService != null && !executorService.isShutdown()) {
            logger.info("Cerrando ExecutorService...");
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                    logger.warning("ExecutorService forzado a cerrar");
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
                logger.severe("Interrumpido durante el cierre: " + e.getMessage());
            }
        }
    }
}
