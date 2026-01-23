import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilidad para detectar y gestionar URLs en mensajes de chat
 */
public class UtilURLs {
    private static final Logger logger = Logger.getLogger(UtilURLs.class.getName());

    // Patrón para detectar URLs (http, https, ftp, www)
    private static final Pattern URL_PATTERN = Pattern.compile(
        "(?:(?:https?|ftp)://)?(?:www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b(?:[-a-zA-Z0-9()@:%_\\+.~#?&/=]*)",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Detecta si una cadena contiene URLs
     * @param texto Texto a analizar
     * @return true si contiene URLs, false en caso contrario
     */
    public static boolean contieneURLs(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return false;
        }
        Matcher matcher = URL_PATTERN.matcher(texto);
        return matcher.find();
    }

    /**
     * Extrae todas las URLs de un texto
     * @param texto Texto a analizar
     * @return Lista de URLs encontradas
     */
    public static List<String> extraerURLs(String texto) {
        List<String> urls = new ArrayList<>();
        if (texto == null || texto.trim().isEmpty()) {
            return urls;
        }

        Matcher matcher = URL_PATTERN.matcher(texto);
        while (matcher.find()) {
            String url = matcher.group();
            urls.add(url);
        }

        return urls;
    }

    /**
     * Abre una URL en el navegador predeterminado
     * @param urlString La URL a abrir
     */
    public static void abrirURL(String urlString) {
        try {
            // Validar que la URL sea válida
            String urlAAbrir = urlString.trim();

            // Si no tiene protocolo, añadir https://
            if (!urlAAbrir.startsWith("http://") && !urlAAbrir.startsWith("https://") && !urlAAbrir.startsWith("ftp://")) {
                urlAAbrir = "https://" + urlAAbrir;
            }

            // Validar que sea una URL correcta
            URL url = new URL(urlAAbrir);

            // Intentar abrir con Desktop
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(new URI(urlAAbrir));
                    logger.info("URL abierta en navegador: " + urlAAbrir);
                } else {
                    logger.warning("Desktop no soporta BROWSE");
                }
            } else {
                logger.warning("Desktop no está soportado");
            }
        } catch (Exception e) {
            logger.severe("Error al abrir URL: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Formatea un texto reemplazando URLs con etiquetas HTML
     * @param texto Texto a formatear
     * @return HTML formateado con enlaces
     */
    public static String formatearConEnlaces(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return "";
        }

        String resultado = texto;
        Matcher matcher = URL_PATTERN.matcher(texto);

        // Usar un StringBuffer para las reemplazos
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String url = matcher.group();
            // Escapar caracteres especiales para HTML
            String urlEscapada = java.util.regex.Matcher.quoteReplacement(url);
            String enlaceHTML = "<a href='#' onclick='return false;' style='color: #0084FF; text-decoration: underline; cursor: pointer;' title='" + urlEscapada + "'>" + urlEscapada + "</a>";
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(enlaceHTML));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }
}

