package ejerprop;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ManejadorCliente implements Runnable {

    private Socket socket;

    public ManejadorCliente(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                // Para leer del cliente
                BufferedReader entrada = new BufferedReader(
                        new InputStreamReader(socket.getInputStream())
                );

                // Para escribir al cliente
                PrintWriter salida = new PrintWriter(
                        socket.getOutputStream(), true
                );
        ) {
            salida.println("Bienvenido al servidor. Escribe 'salir' para desconectarte.");

            String mensaje;

            // Mientras el cliente siga enviando cosas
            while ((mensaje = entrada.readLine()) != null) {
                System.out.println("Cliente " + socket + " dice: " + mensaje);

                if (mensaje.equalsIgnoreCase("salir")) {
                    salida.println("Adiós!");
                    break;
                }

                // Respondemos al cliente
                salida.println("Servidor recibió: " + mensaje);
            }

        } catch (IOException e) {
            System.out.println("Cliente desconectado: " + socket);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
