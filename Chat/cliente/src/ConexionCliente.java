import java.io.*;
import java.net.Socket;

public class ConexionCliente {
    private final String ip;
    private final int puerto;
    private final String nickname;
    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;

    public ConexionCliente(String ip, int puerto, String nickname) {
        this.ip = ip;
        this.puerto = puerto;
        this.nickname = nickname;
    }

    // Conectar al servidor
    public void conectar() throws IOException {
        try {
            socket = new Socket(ip, puerto);
            entrada = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(socket.getOutputStream(), true);

            // Enviar LOGIN
            String loginMsg = Protocolo.serializar(
                    new Mensaje("LOGIN", nickname, "USER", "contraseña"));
            salida.println(loginMsg);

            // Esperar respuesta del servidor
            String respuesta = entrada.readLine();
            if (respuesta != null && respuesta.contains("BIENVENIDA")) {
                System.out.println("✅ Conectado a la sala de chat");
                System.out.println("Escribe /help para ver comandos disponibles");
            } else {
                System.out.println("❌ Error en login: " + respuesta);
                throw new IOException("Login rechazado");
            }

        } catch (IOException e) {
            System.out.println("❌ No se pudo conectar a " + ip + ":" + puerto);
            throw e;
        }
    }

    // Enviar un mensaje al servidor
    public void enviar(String mensaje) {
        if (salida != null && !salida.checkError()) {
            salida.println(mensaje);
        } else {
            System.out.println("❌ No conectado al servidor");
        }
    }

    // Obtener el BufferedReader para que Listener lo use
    public BufferedReader getEntrada() {
        return entrada;
    }

    // Obtener el nickname
    public String getNickname() {
        return nickname;
    }

    // Obtener el socket para manejo de desconexión
    public Socket getSocket() {
        return socket;
    }

    // Cerrar conexión
    public void cerrar() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) { }
    }
}