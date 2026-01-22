

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Logger;

/**
 * Modelo del cliente de chat.
 * Encapsula la conexión Socket y gestiona los flujos de texto (PrintWriter/BufferedReader).
 * Proporciona métodos para conectar, desconectar y enviar mensajes al servidor.
 *
 * Este modelo abstrae la comunicación de bajo nivel, permitiendo al Controller
 * trabajar con una interfaz de alto nivel sin preocuparse por los detalles del socket.
 */
public class Model {
    private static final Logger logger = Logger.getLogger(Model.class.getName());

    // Datos del cliente
    private String nombreUsuario;
    private String hostServidor;
    private int puertoServidor;
    private volatile boolean conectado;  // volatile para visibilidad entre hilos
    private volatile int usuariosConectados;  // volatile para visibilidad entre hilos

    // Socket y flujos de comunicación
    private Socket socket;
    private PrintWriter salida;
    private BufferedReader entrada;

    // Locks para sincronización de acceso concurrente a streams
    private final Object syncSocket = new Object();  // Para proteger creación/acceso al socket
    private final Object syncStreams = new Object(); // Para proteger acceso a salida/entrada

    /**
     * Constructor del modelo
     * Inicializa los valores por defecto
     */
    public Model() {
        this.nombreUsuario = null;
        this.hostServidor = "localhost";
        this.puertoServidor = 5000;
        this.conectado = false;
        this.usuariosConectados = 0;
        this.socket = null;
        this.salida = null;
        this.entrada = null;
        logger.info("Modelo inicializado");
    }

    /**
     * Conecta al servidor creando un socket e inicializando los flujos
     *
     * @param host Dirección del servidor
     * @param puerto Puerto del servidor
     * @return true si la conexión fue exitosa, false en caso contrario
     * @throws IOException Si hay problemas al crear la conexión
     */
    public boolean conectar(String host, int puerto) throws IOException {
        synchronized (syncSocket) {
            try {
                logger.info("Intentando conectar a " + host + ":" + puerto);

                // Establecer datos del servidor
                this.hostServidor = host;
                this.puertoServidor = puerto;

                // Crear el socket
                this.socket = new Socket(host, puerto);
                logger.info("Socket creado exitosamente");

                // Inicializar flujos de comunicación
                // PrintWriter con autoflush=true para que flush() se ejecute automáticamente
                synchronized (syncStreams) {
                    this.salida = new PrintWriter(socket.getOutputStream(), true);
                    this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                }

                this.conectado = true;
                logger.info("Conexión establecida con " + host + ":" + puerto);

                return true;

            } catch (IOException e) {
                logger.severe("Error al conectar: " + e.getMessage());
                conectado = false;
                limpiarRecursos();
                throw e;
            }
        }
    }

    /**
     * Desconecta del servidor y cierra todos los recursos
     */
    public void desconectar() {
        logger.info("Desconectando del servidor...");
        conectado = false;
        limpiarRecursos();
        logger.info("Desconexión completada");
    }

    /**
     * Limpia los recursos del socket y flujos
     */
    private void limpiarRecursos() {
        // Cerrar PrintWriter
        if (salida != null) {
            try {
                salida.close();
                logger.info("PrintWriter cerrado");
            } catch (Exception e) {
                logger.warning("Error al cerrar PrintWriter: " + e.getMessage());
            }
        }

        // Cerrar BufferedReader
        if (entrada != null) {
            try {
                entrada.close();
                logger.info("BufferedReader cerrado");
            } catch (IOException e) {
                logger.warning("Error al cerrar BufferedReader: " + e.getMessage());
            }
        }

        // Cerrar Socket
        if (socket != null) {
            try {
                socket.close();
                logger.info("Socket cerrado");
            } catch (IOException e) {
                logger.warning("Error al cerrar Socket: " + e.getMessage());
            }
        }

        this.socket = null;
        this.salida = null;
        this.entrada = null;
    }

    /**
     * Envía un mensaje de texto al servidor
     * Usa el protocolo de empaquetación definido en Protocolo.java
     * Sincronizado para evitar race conditions entre hilos
     *
     * @param mensaje El mensaje a enviar
     * @throws IOException Si hay problemas al enviar
     * @throws IllegalStateException Si no está conectado
     */
    public void enviarMensaje(String mensaje) throws IOException {
        if (!conectado || salida == null) {
            throw new IllegalStateException("No está conectado al servidor");
        }

        try {
            synchronized (syncStreams) {
                // Empaquetar el mensaje usando el protocolo
                String mensajeEmpaquetado = Protocolo.empaquetar(Protocolo.MSG, mensaje);
                salida.println(mensajeEmpaquetado);
                logger.info("Mensaje enviado: " + mensaje);
            }
        } catch (Exception e) {
            logger.severe("Error al enviar mensaje: " + e.getMessage());
            throw new IOException("Error al enviar mensaje", e);
        }
    }

    /**
     * Envía un comando genérico al servidor
     * Útil para enviar comandos que no son mensajes simples
     * Sincronizado para evitar race conditions entre hilos
     *
     * @param comando El comando a enviar (LOGIN, BYE, LIST, PING, etc.)
     * @param parametros Parámetros adicionales del comando
     * @throws IOException Si hay problemas al enviar
     * @throws IllegalStateException Si no está conectado
     */
    public void enviarComando(String comando, String... parametros) throws IOException {
        if (!conectado || salida == null) {
            throw new IllegalStateException("No está conectado al servidor");
        }

        try {
            synchronized (syncStreams) {
                // Empaquetar el comando usando el protocolo
                String comandoEmpaquetado = Protocolo.empaquetar(comando, parametros);
                salida.println(comandoEmpaquetado);
                logger.info("Comando enviado: " + comando);
            }
        } catch (Exception e) {
            logger.severe("Error al enviar comando: " + e.getMessage());
            throw new IOException("Error al enviar comando", e);
        }
    }

    /**
     * Lee una línea del servidor
     * Este método es bloqueante - se espera a que haya datos disponibles
     * Sincronizado para evitar race conditions entre hilos
     *
     * @return La línea recibida del servidor, o null si se cerró la conexión
     * @throws IOException Si hay problemas al leer
     * @throws IllegalStateException Si no está conectado
     */
    public String leerDelServidor() throws IOException {
        if (!conectado || entrada == null) {
            throw new IllegalStateException("No está conectado al servidor");
        }

        try {
            synchronized (syncStreams) {
                String linea = entrada.readLine();
                if (linea == null) {
                    logger.info("Servidor cerró la conexión");
                    conectado = false;
                } else {
                    logger.fine("Datos recibidos del servidor: " + linea);
                }
                return linea;
            }
        } catch (IOException e) {
            logger.severe("Error al leer del servidor: " + e.getMessage());
            conectado = false;
            throw e;
        }
    }

    /**
     * Obtiene el BufferedReader para uso externo (por ejemplo, por EscuchaServidor)
     *
     * @return El BufferedReader del socket
     */
    public BufferedReader obtenerEntrada() {
        return entrada;
    }

    /**
     * Obtiene el PrintWriter para uso externo (por ejemplo, para enviarlo a otro componente)
     *
     * @return El PrintWriter del socket
     */
    public PrintWriter obtenerSalida() {
        return salida;
    }

    /**
     * Establece el nombre de usuario
     *
     * @param nombre El nombre de usuario
     */
    public void establecerNombreUsuario(String nombre) {
        this.nombreUsuario = nombre;
        logger.info("Nombre de usuario establecido: " + nombre);
    }

    /**
     * Obtiene el nombre de usuario
     *
     * @return El nombre de usuario
     */
    public String obtenerNombreUsuario() {
        return nombreUsuario;
    }

    /**
     * Establece los datos del servidor
     *
     * @param host Dirección del servidor
     * @param puerto Puerto del servidor
     */
    public void establecerServidorDatos(String host, int puerto) {
        this.hostServidor = host;
        this.puertoServidor = puerto;
        logger.info("Datos del servidor establecidos: " + host + ":" + puerto);
    }

    /**
     * Obtiene la dirección del servidor
     *
     * @return La dirección del servidor
     */
    public String obtenerHostServidor() {
        return hostServidor;
    }

    /**
     * Obtiene el puerto del servidor
     *
     * @return El puerto del servidor
     */
    public int obtenerPuertoServidor() {
        return puertoServidor;
    }

    /**
     * Verifica si está conectado
     *
     * @return true si está conectado
     */
    public boolean estaConectado() {
        return conectado;
    }

    /**
     * Establece el número de usuarios conectados
     *
     * @param cantidad El número de usuarios
     */
    public void establecerUsuariosConectados(int cantidad) {
        this.usuariosConectados = cantidad;
        logger.fine("Usuarios conectados: " + cantidad);
    }

    /**
     * Obtiene el número de usuarios conectados
     *
     * @return El número de usuarios
     */
    public int obtenerUsuariosConectados() {
        return usuariosConectados;
    }

    /**
     * Verifica si el socket está todavía conectado y operativo
     *
     * @return true si el socket está abierto y conectado
     */
    public boolean estaSocketActivo() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * Obtiene una representación del estado actual
     *
     * @return String con el estado del modelo
     */
    public String obtenerEstado() {
        return String.format("Modelo - Usuario: %s, Servidor: %s:%d, Conectado: %s, Usuarios: %d, Socket Activo: %s",
            nombreUsuario != null ? nombreUsuario : "N/A",
            hostServidor,
            puertoServidor,
            conectado ? "Sí" : "No",
            usuariosConectados,
            estaSocketActivo() ? "Sí" : "No");
    }
}
