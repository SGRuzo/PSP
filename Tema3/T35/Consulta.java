package T35;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;

public class Consulta{

    private static final String ROJO= "\u001B[31m";
    private static final String AZUL="\u001B[34m";

    private static final String URL_API="https://api.coinlore.net/api/tickers/";

    public static void main(String[] args){

        try(Scanner scanner=new Scanner(System.in)){
            System.out.print("Introduce el nombre o símbolo de la criptomoneda: ");
            String entrada=scanner.nextLine().trim();

            if(entrada.isEmpty()){
                System.out.println("Error: entrada vacía.");
                return;
            }
            CriptoMoneda moneda=buscarCriptomoneda(entrada);
            mostrarInformacion(moneda);
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
    }

    private static CriptoMoneda buscarCriptomoneda(String busqueda)
        throws IOException,InterruptedException{
        HttpClient cliente=HttpClient.newHttpClient();
        HttpRequest request=HttpRequest.newBuilder()
                .uri(URI.create(URL_API))
                .header("Accept","application/json")
                .GET()
                .build();

        HttpResponse<String> response=
                cliente.send(request,HttpResponse.BodyHandlers.ofString());

        if(response.statusCode()!=200){
            throw new IOException("Error HTTP: "+response.statusCode());
        }
        ObjectMapper mapper=new ObjectMapper();
        Respuesta respuestaAPI=
                mapper.readValue(response.body(),Respuesta.class);
        String busquedaLower=busqueda.toLowerCase();

        return respuestaAPI.getData().stream()
                .filter(m->
                        m.getNombre().toLowerCase().equals(busquedaLower)||
                                m.getSimbolo().toLowerCase().equals(busquedaLower))
                .findFirst()
                .orElseThrow(()->
                        new IllegalArgumentException("Moneda no encontrada."));
    }

    private static void mostrarInformacion(CriptoMoneda moneda){
        System.out.println("Nombre: "+moneda.getNombre());
        System.out.println("Símbolo: "+moneda.getSimbolo());
        System.out.println("Precio USD: $"+moneda.getPrecioUsd());
        System.out.println("Ranking: #"+moneda.getRanking());

        double cambio=moneda.getCambio24h();
        String color=cambio<0?ROJO:AZUL;
        String signo=cambio>=0?"+":"";

        System.out.printf(
                "Variación 24h: %s%s%.2f%%%s%n",
                color,signo,cambio
        );
    }
}
