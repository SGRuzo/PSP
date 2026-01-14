import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;

public class Listener implements Runnable {
    private final BufferedReader entrada;
    private final Socket socket;

    public Listener(BufferedReader entrada, Socket socket) {
        this.entrada = entrada;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            String mensaje;
            while ((mensaje = entrada.readLine()) != null) {
                // IMPORTANTE: Detectar cierre del servidor
                if (mensaje.contains("El servidor se desconectó")) {
                    System.out.println("\n⚠️ " + mensaje);
                    socket.close();
                    System.exit(0);
                }

                // Mostrar el mensaje
                mostrarMensaje(mensaje);
            }

            // Si llegamos aquí, el servidor cerró la conexión
            System.out.println("\n⚠️ El servidor cerró la conexión");
            System.exit(0);

        } catch (IOException e) {
            System.out.println("\n⚠️ Error de conexión: " + e.getMessage());
            System.exit(0);
        }
    }

    private void mostrarMensaje(String mensaje) {
        // Parsear según protocolo TIPO|REMITENTE|ROL|CONTENIDO
        String[] partes = mensaje.split("\\|", 4);

        if (partes.length >= 3) {
            String tipo = partes[0];
            String remitente = partes[1];
            String contenido = partes.length > 3 ? partes[3] : partes[2];

            switch(tipo) {
                case "NOTIFICACION":
                    System.out.println("[NOTIFICACIÓN] " + contenido);
                    break;
                case "RESPUESTA":
                    System.out.println("[RESPUESTA] " + contenido);
                    break;
                case "ERROR":
                    System.out.println("[ERROR] " + contenido);
                    break;
                case "MENSAJE":
                    System.out.println(remitente + ": " + contenido);
                    break;
                case "BIENVENIDA":
                    System.out.println("[BIENVENIDA] " + contenido);
                    break;
                default:
                    System.out.println(mensaje);
            }
        } else {
            System.out.println(mensaje);
        }
    }
}