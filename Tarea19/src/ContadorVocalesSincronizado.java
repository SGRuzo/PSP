import java.util.Scanner;

public class ContadorVocalesSincronizado {
    static class ContadorCompartido {
        private int total = 0; // variable compartida
        private final Object lock = new Object(); // candado

        public void incrementar(int cantidad) { // Solo un hilo a la vez
            synchronized (lock) {
                total += cantidad;
            }
        }
        public int getTotal() {
            return total;
        }
    }

    // Hilo cuenta una vocal
    static class HiloContador extends Thread {
        private final char vocal;
        private final String texto;
        private final ContadorCompartido contador;

        public HiloContador(char vocal, String texto, ContadorCompartido contador) {
            this.vocal = vocal;
            this.texto = texto;
            this.contador = contador;
        }

        @Override
        public void run() {
            int contadorLocal = 0;

            // contar la vocal en el texto
            for (int i = 0; i < texto.length(); i++) {
                char c = Character.toLowerCase(texto.charAt(i)); // ignoramos mayúsculas
                if (c == vocal) {
                    contadorLocal++;
                }
                try {
                    Thread.sleep(5); // Se duerme para simular
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Se actualiza el total compartido
            contador.incrementar(contadorLocal);

            System.out.println("Hilo '" + vocal + "' ha contado " + contadorLocal + " veces la vocal '" + vocal + "'.");
        }
    }
}
