package T32;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Servidor {
    public static void main(String[] args) {
        int puerto = 6666; // Puerto donde el servidor escuchará
        byte[] buffer = new byte[1024];// Buffer para los datos entrantes
        try {
            System.out.println("Servidor arrancando");
            DatagramSocket datagramSocket=new DatagramSocket(puerto);
            // Creamos el socket UDP asignado al puerto

            DatagramPacket peticion =new DatagramPacket(buffer, buffer.length);

            datagramSocket.receive(peticion); // Esperamos paquete del cliente
            System.out.println("Petición recibida");

            // Convertimos los bytes en texto
            String mensaje=new String(peticion.getData(), 0, peticion.getLength());

            System.out.println("Lista recibida: "+mensaje);

            // Dividimos las palabras por espacios
            String[] palabras = mensaje.split(" ");
            String palabraMasLarga = "";

            // Buscamos la palabra de mayor longitud
            for (String palabra : palabras) {
                if (palabra.length() > palabraMasLarga.length()) {
                    palabraMasLarga = palabra;
                }
            }



            // Preparamos la respuesta: palabra + número de letras
            String respuesta=palabraMasLarga+ " " +palabraMasLarga.length();
            buffer = respuesta.getBytes();// Pasamos la respuesta a bytes


            // Recuperamos dirección y puerto del cliente para contestarle
            int puertoCliente = peticion.getPort();
            InetAddress direccionCliente = peticion.getAddress();


            DatagramPacket respuestaPacket =new DatagramPacket(buffer, buffer.length, direccionCliente, puertoCliente);

            datagramSocket.send(respuestaPacket);// Enviamos la respuesta
            System.out.println("Respuesta enviada: "+respuesta);
            datagramSocket.close();// Cerramos socket
        } catch (SocketException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}