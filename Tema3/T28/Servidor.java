package T28;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Servidor {
    public static void main(String[] args) {
        final int PORT=6666;
        final String HOST="localhost";
        ServerSocket servidor=null;
        try {
            InetSocketAddress dir=new InetSocketAddress(HOST, PORT);
            servidor=new ServerSocket();
            servidor.bind(dir);



            System.out.println("Servidor activo en "+HOST+":"+PORT);
            while (true) {
                System.out.println("Esperando cliente...");
                Socket socketCliente =servidor.accept();
                System.out.println("Cliente conectado: "+ socketCliente.getInetAddress());
                Cliente manejador=new Cliente(socketCliente);
                Thread hilo =new Thread(manejador);
                hilo.start();
            }



        } catch (IOException e) {
            e.printStackTrace();

        }
    }
}

