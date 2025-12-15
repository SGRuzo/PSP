package T35;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// Respuesta completa de la API
@JsonIgnoreProperties(ignoreUnknown = true)
class Respuesta {
    private List<CriptoMoneda> data;

    public List<CriptoMoneda> getData() { return data;}
    public void setData(List<CriptoMoneda> data) { this.data = data; }
}