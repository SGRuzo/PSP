public class Protocolo {

    // Convierte: Mensaje → String (para enviar por socket)
    public static String serializar(Mensaje msg) {
        return msg.getTipo() + "|"
                + msg.getRemitente() + "|"
                + msg.getRol() + "|"
                + msg.getContenido();
    }

    // Convierte: String → Mensaje (para recibir del socket)
    public static Mensaje deserializar(String linea) {
        String[] partes = linea.split("\\|", 4);  // Máximo 4 partes

        if (partes.length < 4) {
            // Error: formato incompleto
            return new Mensaje("ERROR", "sistema", "SISTEMA",
                    "Protocolo inválido: " + linea);
        }

        return new Mensaje(
                partes[0].trim(),  // tipo
                partes[1].trim(),  // remitente
                partes[2].trim(),  // rol
                partes[3].trim()   // contenido
        );
    }

    // Validar si un string es válido según protocolo
    public static boolean esValido(String linea) {
        return linea != null && linea.split("\\|").length >= 3;
    }
}

