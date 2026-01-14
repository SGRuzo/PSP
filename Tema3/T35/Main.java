package T35;
import java.net.http.*;
import java.net.URI;
import java.util.Scanner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        System.out.print("Introduce nombre o símbolo de la criptomoneda: ");
        String busqueda = sc.nextLine().trim().toLowerCase();

        try {
            // 1. Cliente HTTP moderno
            HttpClient client = HttpClient.newHttpClient();

            // 2. Petición GET
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.coinlore.net/api/tickers/"))
                    .GET()
                    .build();

            // 3. Enviar y recibir String
            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                System.out.println("Error HTTP: " + response.statusCode());
                return;
            }

            // 4. Jackson: leer JSON como árbol
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());

            JsonNode dataArray = root.get("data");

            if (dataArray == null || !dataArray.isArray()) {
                System.out.println("Formato inesperado en la respuesta.");
                return;
            }

            JsonNode encontrada = null;

            // 5. Buscar por símbolo o nombre
            for (JsonNode coin : dataArray) {
                String symbol = coin.get("symbol").asText().toLowerCase();
                String name = coin.get("name").asText().toLowerCase();

                if (symbol.equals(busqueda) || name.equals(busqueda)) {
                    encontrada = coin;
                    break;
                }
            }

            if (encontrada == null) {
                System.out.println("Moneda no encontrada.");
                return;
            }

            // 6. Mostrar datos
            String name = encontrada.get("name").asText();
            String symbol = encontrada.get("symbol").asText();
            String priceUsd = encontrada.get("price_usd").asText();
            int rank = encontrada.get("rank").asInt();
            double change24h = encontrada.get("percent_change_24h").asDouble();

            System.out.println("Nombre: " + name);
            System.out.println("Símbolo: " + symbol);
            System.out.println("Rank: " + rank);
            System.out.println("Precio USD: " + priceUsd);

            if (change24h >= 0) {
                System.out.println("Variación 24h: +" + change24h + "%");
            } else {
                System.out.println("Variación 24h: " + change24h + "%");
            }

        } catch (Exception e) {
            System.out.println("Error procesando la petición.");
            e.printStackTrace();
        }
    }
}
