package ejerprop;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class servidor {

    public static void main(String[] args) {
        int puerto = 5000;

        // Creamos un pool de 10 hilos (pueden atenderse hasta 10 clientes a la vez)
        ExecutorService executor = Executors.newFixedThreadPool(10);

        System.out.println("Servidor iniciado en el puerto " + puerto);

        try (ServerSocket serverSocket = new ServerSocket(puerto)) {

            while (true) {
                // Espera a que un cliente se conecte
                Socket socketCliente = serverSocket.accept();

                System.out.println("Nuevo cliente conectado: " + socketCliente);

                // Creamos el manejador del cliente
                ManejadorCliente manejador = new ManejadorCliente(socketCliente);

                // Lo ejecutamos en un hilo del pool
                executor.execute(manejador);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }
}
