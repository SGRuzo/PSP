package T35;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


// Clase criptomoneda
@JsonIgnoreProperties(ignoreUnknown = true)
class CriptoMoneda {

    @JsonProperty("name")
    private String nombre;

    @JsonProperty("symbol")
    private String simbolo;

    @JsonProperty("price_usd")
    private String precioUsd; // String porque solo se muestra

    @JsonProperty("rank")
    private int ranking;

    @JsonProperty("percent_change_24h")
    private double cambio24h;

    public String getNombre() { return nombre; }
    public String getSimbolo() { return simbolo; }
    public String getPrecioUsd() { return precioUsd; }
    public int getRanking() { return ranking; }
    public double getCambio24h() { return cambio24h; }
}


