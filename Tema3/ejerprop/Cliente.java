package ejerprop;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Cliente {

    public static void main(String[] args) {
        String host = "localhost";
        int puerto = 5000;

        try (
                Socket socket = new Socket(host, puerto);

                BufferedReader entrada = new BufferedReader(
                        new InputStreamReader(socket.getInputStream())
                );

                PrintWriter salida = new PrintWriter(
                        socket.getOutputStream(), true
                );

                Scanner teclado = new Scanner(System.in);
        ) {
            // Leemos el mensaje de bienvenida
            System.out.println(entrada.readLine());

            String texto;

            while (true) {
                System.out.print("Tú: ");
                texto = teclado.nextLine();

                // Enviamos al servidor
                salida.println(texto);

                // Leemos respuesta
                String respuesta = entrada.readLine();
                System.out.println("Servidor: " + respuesta);

                if (texto.equalsIgnoreCase("salir")) {
                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
