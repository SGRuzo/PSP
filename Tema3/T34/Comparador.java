package T34;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;


public class Comparador {

    public static class Resultado {
        long tiempoMs;
        int tamañoCaracteres;

        Resultado(long tiempoMs, int tamañoCaracteres) {
            this.tiempoMs =tiempoMs;
            this.tamañoCaracteres =tamañoCaracteres;
        }
    }

    public static Resultado obtenerDatos(String url) {
        try {
            // Crear cliente HTTP
            HttpClient cliente=HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10)).build();

            //contenido
            HttpRequest request=HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            // Medir tiempo de respuesta
            long inicio=System.currentTimeMillis();
            HttpResponse<String> response= cliente.send(request, HttpResponse.BodyHandlers.ofString());
            long fin = System.currentTimeMillis();

            // Calcular resultados
            long tiempoMs =fin-inicio;
            int tamañoCaracteres = response.body().length();

            return new Resultado(tiempoMs, tamañoCaracteres);

        } catch (Exception e) {
            System.err.println("Error con la URL: "+url);
            e.printStackTrace();
            return null;
        }
    }
}