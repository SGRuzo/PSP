import java.util.concurrent.ThreadLocalRandom;

class Domino extends Thread {
    private String nombre;
    private int numero;
    private Domino siguiente;

    public Domino(String nombre, int numero) {
        this.nombre = nombre;
        this.numero = numero;
    }

    @Override
    public void run() {
        int fichas=5;
        if (numero < fichas) {
            siguiente = new Domino("Ficha " + (numero + 1), numero + 1);
            siguiente.start();
        }


        for (int i = 1; i <= 5; i++) {
            System.out.println("[" + nombre + "] iteración: " + i);
            try {
                int espera = ThreadLocalRandom.current().nextInt(100, 601);
                Thread.sleep(espera);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (siguiente != null) {
            try {
                siguiente.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Acabó el hilo de la " + nombre);
    }
}

public class Main {
    public static void main(String[] args) {
        Domino primero = new Domino("Ficha 1", 1);
        primero.start();
    }
}