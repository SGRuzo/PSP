package T34;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Introduce la primera URL: ");
        String url1=scanner.nextLine();

        System.out.print("Introduce la segunda URL: ");
        String url2=scanner.nextLine();

        // Datos URLs
        Comparador.Resultado resultado1 =Comparador.obtenerDatos(url1);
        Comparador.Resultado resultado2 =Comparador.obtenerDatos(url2);

        if (resultado1==null|| resultado2==null) {
            System.out.println("Error al obtener los datos de una o ambas URLs. Verifica las URLs e inténtalo de nuevo.");
            return;
        }

        // Comparar tiempos de respuesta
        String urlMasRapida= resultado1.tiempoMs<resultado2.tiempoMs ? url1:url2;
        long tiempoMasRapido= Math.min(resultado1.tiempoMs, resultado2.tiempoMs);

        // Comparar numero caracteres)
        String urlMasContenido= resultado1.tamañoCaracteres>resultado2.tamañoCaracteres ? url1:url2;
        int tamañoMasContenido= Math.max(resultado1.tamañoCaracteres, resultado2.tamañoCaracteres);

        System.out.println("La web más rápida ha sido: "+urlMasRapida +" con " + tiempoMasRapido +" ms.");
        System.out.println("La web con más contenido ha sido: "+urlMasContenido + " con "+tamañoMasContenido +" caracteres.");

        scanner.close();
    }
}