package T28;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

class Cliente implements Runnable {
    private Socket socket;
    public Cliente(Socket socket) {
        this.socket = socket;
    }



    @Override
    public void run() {
        try {
            BufferedReader lector =new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );
            PrintWriter escritor=new PrintWriter(socket.getOutputStream(), true);
            String linea;



            while ((linea =lector.readLine())!=null) {
                System.out.println("Cliente ["+socket.getInetAddress()+"] dijo: "+linea);
                if ("adios".equalsIgnoreCase(linea)) {
                    System.out.println("Conexión cerrada.");
                    break;
                }
                String vuelta="ECO: "+linea;
                escritor.println(vuelta);
                System.out.println("Enviado al cliente: "+vuelta);
            }
            socket.close();
            System.out.println("Cliente desconectado: "+socket.getInetAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
