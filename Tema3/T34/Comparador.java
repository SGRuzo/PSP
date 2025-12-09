package T34;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;


public class Comparador {

    public static class ResultadoWeb {
        long tiempoMs;
        int tamañoCaracteres;

        ResultadoWeb(long tiempoMs, int tamañoCaracteres) {
            this.tiempoMs =tiempoMs;
            this.tamañoCaracteres =tamañoCaracteres;
        }
    }

    public static ResultadoWeb obtenerDatosWeb(String url) {
        try {
            HttpClient cliente=HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10)).build();

            HttpRequest request=HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            long inicio=System.currentTimeMillis();
            HttpResponse<String> response= cliente.send(request, HttpResponse.BodyHandlers.ofString());
            long fin = System.currentTimeMillis();

            long tiempoMs =fin-inicio;
            int tamañoCaracteres = response.body().length();

            return new ResultadoWeb(tiempoMs, tamañoCaracteres);

        } catch (Exception e) {
            System.err.println("Error al procesar la URL: "+url);
            e.printStackTrace();
            return null;
        }
    }
}