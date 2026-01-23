
import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 * Hilo secundario que escucha constantemente al servidor.
 * Lee del socket de forma bloqueante sin congelar la interfaz gráfica.
 *
 * CRUCIAL: Este hilo corre en paralelo al hilo principal (Swing),
 * permitiendo que la UI permanezca responsiva mientras espera mensajes.
 */
public class EscuchaServidor implements Runnable {
    private static final Logger logger = Logger.getLogger(EscuchaServidor.class.getName());

    private final BufferedReader entrada;
    private final Controller controlador;
    private volatile boolean escuchando;

    /**
     * Constructor de EscuchaServidor
     *
     * @param entrada BufferedReader del socket para leer mensajes
     * @param controlador Referencia al controlador para actualizar la vista
     */
    public EscuchaServidor(BufferedReader entrada, Controller controlador) {
        this.entrada = entrada;
        this.controlador = controlador;
        this.escuchando = true;
        logger.info("EscuchaServidor creado");
    }

    /**
     * Metodo principal del hilo (run).
     * Lee continuamente del servidor y procesa los mensajes recibidos.
     * Este metodo se ejecuta en un hilo separado gestionado por ExecutorService.
     */
    @Override
    public void run() {
        logger.info("EscuchaServidor iniciado - esperando mensajes del servidor");

        try {
            String linea;
            while (escuchando && (linea = entrada.readLine()) != null) {
                logger.fine("Línea recibida: " + linea);
                procesarMensaje(linea);
            }
        } catch (IOException e) {
            if (escuchando) {
                logger.severe("Error al leer del servidor: " + e.getMessage());
                controlador.mostrarMensaje("[ERROR] Conexión perdida con el servidor");
            }
        } finally {
            logger.info("EscuchaServidor finalizado");
            detener();
        }
    }

    /**
     * Procesa un mensaje recibido del servidor.
     * Desempaqueta el mensaje y lo envía al controlador según su tipo.
     * Ejecuta todas las actualizaciones de UI en el EDT.
     *
     * @param mensaje El mensaje completo recibido del servidor
     */
    private void procesarMensaje(String mensaje) {
        try {
            // Desempaquetar el mensaje: TIPO|contenido1|contenido2|...
            String[] partes = Protocolo.desempaquetar(mensaje);

            if (partes.length == 0) {
                logger.warning("Mensaje vacío recibido");
                return;
            }

            String tipo = partes[0];
            String contenido = partes.length > 1 ? partes[1] : "";

            logger.fine("Procesando mensaje tipo: " + tipo + ", contenido: " + contenido);

            // Procesar según el tipo de mensaje
            switch (tipo) {
                case Protocolo.OK:
                    procesarOK(contenido);
                    break;

                case Protocolo.ERROR:
                    procesarError(contenido);
                    break;

                case Protocolo.MSG:
                    procesarMensajeChat(partes);
                    break;

                case "LIST":
                    procesarLista(contenido);
                    break;

                case "PONG":
                    procesarPong(contenido);
                    break;

                case "NOTIFICACION":
                    procesarNotificacion(contenido);
                    break;

                case Protocolo.SERVIDOR_DESCONECTADO:
                    procesarCierreServidor(contenido);
                    break;

                default:
                    logger.warning("Tipo de mensaje desconocido: " + tipo);
                    mostrarEnUI("[SISTEMA] Mensaje desconocido: " + tipo);
                    break;
            }

        } catch (Exception e) {
            logger.severe("Error al procesar mensaje: " + e.getMessage());
            mostrarEnUI("[ERROR] No se pudo procesar el mensaje recibido");
        }
    }

    /**
     * Ejecuta una acción de UI de forma segura en el EDT
     *
     * @param mensaje Mensaje a mostrar
     */
    private void mostrarEnUI(String mensaje) {
        SwingUtilities.invokeLater(() -> controlador.mostrarMensaje(mensaje));
    }

    /**
     * Ejecuta una acción de error en el EDT
     *
     * @param titulo Título del error
     * @param mensaje Mensaje de error
     */
    private void mostrarErrorEnUI(String titulo, String mensaje) {
        SwingUtilities.invokeLater(() -> controlador.procesarError(titulo, mensaje));
    }

    /**
     * Procesa una respuesta OK del servidor
     *
     * @param contenido El contenido de la respuesta
     */
    private void procesarOK(String contenido) {
        logger.info("Respuesta OK: " + contenido);
        mostrarEnUI("[OK] " + contenido);
    }

    /**
     * Procesa una respuesta de ERROR del servidor
     *
     * @param contenido El contenido del error
     */
    private void procesarError(String contenido) {
        logger.warning("Error del servidor: " + contenido);
        mostrarEnUI("[ERROR] " + contenido);
        mostrarErrorEnUI("Error del Servidor", contenido);
    }

    /**
     * Procesa un mensaje de chat de otro usuario
     *
     * @param partes Array desempaquetado: [MSG, remitente, contenido]
     */
    private void procesarMensajeChat(String[] partes) {
        if (partes.length < 3) {
            logger.warning("Mensaje de chat incompleto");
            return;
        }

        String remitente = partes[1];
        String contenido = partes[2];

        logger.info("Mensaje de chat de " + remitente + ": " + contenido);

        String mensajeFormato = remitente + ": " + contenido;
        mostrarEnUI(mensajeFormato);
    }

    /**
     * Procesa la respuesta del comando LIST (lista de usuarios)
     *
     * @param contenido Los usuarios conectados
     */
    private void procesarLista(String contenido) {
        logger.info("Lista de usuarios recibida: " + contenido);
        mostrarEnUI("[USUARIOS] " + contenido);
    }

    /**
     * Procesa la respuesta a un PING
     *
     * @param contenido El contenido de la respuesta
     */
    private void procesarPong(String contenido) {
        logger.info("PONG recibido: " + contenido);
        mostrarEnUI("[PING] Servidor respondió: " + contenido);
    }

    /**
     * Procesa una notificación del sistema (conexión/desconexión de usuarios)
     *
     * @param contenido El contenido de la notificación
     */
    private void procesarNotificacion(String contenido) {
        logger.info("Notificación del sistema: " + contenido);
        mostrarEnUI("[NOTIFICACIÓN] " + contenido);
    }

    /**
     * Procesa la notificación de cierre del servidor
     *
     * @param contenido El contenido del mensaje de cierre
     */
    private void procesarCierreServidor(String contenido) {
        logger.severe("¡El servidor se desconectó!");
        mostrarErrorEnUI("Desconexión del Servidor", "El servidor se desconectó");
        mostrarEnUI("[⚠️ ALERTA] El servidor se desconectó");
        escuchando = false;
    }

    /**
     * Detiene la escucha del servidor
     * Se llama cuando se desconecta el cliente
     */
    public void detener() {
        logger.info("Deteniendo EscuchaServidor...");
        escuchando = false;

        if (entrada != null) {
            try {
                entrada.close();
                logger.info("BufferedReader cerrado");
            } catch (IOException e) {
                logger.warning("Error al cerrar BufferedReader: " + e.getMessage());
            }
        }
    }

    /**
     * Verifica si el hilo está escuchando
     *
     * @return true si está escuchando, false en caso contrario
     */
    public boolean estaEscuchando() {
        return escuchando;
    }
}
