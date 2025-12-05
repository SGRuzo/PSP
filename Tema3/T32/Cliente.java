package T32;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Cliente {

    public static void main(String[] args) {
        int puertoServidor = 6666;// Puerto donde escucha el servidor
        byte[] buffer = new byte[1024];// Buffer para enviar/recibir datos
        Scanner scanner = new Scanner(System.in);// Para leer texto del usuario

        try {
            InetAddress direccionServidor = InetAddress.getByName("localhost");
            // Dirección del servidor (localhost = mismo equipo)

            DatagramSocket datagramSocket = new DatagramSocket();
            // Socket UDP del cliente para enviar y recibir paquetes

            // Pedimos al usuario que escriba palabras
            System.out.print("Introduce una lista de palabras separadas por espacios: ");
            String listaPalabras = scanner.nextLine().trim();

            if (listaPalabras.isEmpty()) { // Evita enviar nada si está vacío
                System.out.println("No se ingresaron palabras.");
                scanner.close();
                datagramSocket.close();
                return;
            }

            buffer =listaPalabras.getBytes(); // Convertimos el texto en bytes para enviarlo

            // Construimos el paquete UDP con el mensaje y la dirección del servidor
            DatagramPacket pregunta =new DatagramPacket(buffer, buffer.length, direccionServidor, puertoServidor);

            datagramSocket.send(pregunta); // Enviamos el paquete
            System.out.println("Lista enviada al servidor: "+listaPalabras);

            // Preparamos un paquete vacío para recibir la respuesta del servidor
            DatagramPacket peticion= new DatagramPacket(buffer, buffer.length);
            datagramSocket.receive(peticion); // Esperamos la respuesta (bloqueante)

            // Convertimos los bytes recibidos a String
            String respuesta=new String(peticion.getData(), 0, peticion.getLength());
            System.out.println("Respuesta del servidor: \n La palabra más larga es: "+respuesta);

            scanner.close();
            datagramSocket.close(); // Cerramos recursos
        } catch (SocketException|UnknownHostException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
    }
}
}
