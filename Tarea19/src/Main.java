import java.util.Scanner;

public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    System.out.println("Introduce un texto para contar las vocales:");
    String texto = sc.nextLine();

    // Contador compartido
    ContadorVocalesSincronizado.ContadorCompartido contador = new ContadorVocalesSincronizado.ContadorCompartido();

    // Un hilo por cada vocal
    Thread[] hilos = new Thread[5];
    char[] vocales = {'a', 'e', 'i', 'o', 'u'};

    for (int i = 0; i < vocales.length; i++) {
        hilos[i] = new ContadorVocalesSincronizado.HiloContador(vocales[i], texto, contador);
    }

    // Se inician todos los hilos
    for (Thread hilo : hilos) {
        hilo.start();
    }

    // Se espera a que acaben todos los hilos
    for (Thread hilo : hilos) {
        try {
            hilo.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    System.out.println("Total de vocales en el texto: " + contador.getTotal());
}