import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Gestor de credenciales que valida usuarios contra un archivo de hashes SHA-256
 * Formato del archivo de credenciales:
 * usuario:hash_sha256:rol
 *
 * Ejemplo:
 * juan:2c26b46911185131006ba5991d33f0f1e8de59b5e20b4adc6d6a1b15f03a8b6d:USER
 * admin:8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918:ADMIN
 */
public class GestorCredenciales {
    private static final Logger logger = Logger.getLogger(GestorCredenciales.class.getName());

    private static final String ARCHIVO_CREDENCIALES = "credenciales.txt";
    private static final String SEPARADOR_CREDENCIAL = ":";

    /**
     * Map que almacena las credenciales: clave = usuario, valor = [hash, rol]
     */
    private final Map<String, String[]> credenciales; // [0] = hash, [1] = rol

    /**
     * Constructor - carga las credenciales desde el archivo
     */
    public GestorCredenciales() {
        this.credenciales = new HashMap<>();
        cargarCredenciales();
    }

    /**
     * Carga las credenciales desde el archivo
     * Formato: usuario:hash_sha256:rol
     */
    private void cargarCredenciales() {
        File archivo = new File(ARCHIVO_CREDENCIALES);

        if (!archivo.exists()) {
            logger.warning("Archivo de credenciales no encontrado: " + ARCHIVO_CREDENCIALES);
            logger.info("Creando archivo de credenciales con usuarios de prueba...");
            crearArchivoPorDefecto();
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
            String linea;
            int lineaNum = 0;

            while ((linea = reader.readLine()) != null) {
                lineaNum++;
                linea = linea.trim();

                // Ignorar líneas vacías y comentarios
                if (linea.isEmpty() || linea.startsWith("#")) {
                    continue;
                }

                String[] partes = linea.split(SEPARADOR_CREDENCIAL);
                if (partes.length != 3) {
                    logger.warning("Formato inválido en línea " + lineaNum + ": " + linea);
                    continue;
                }

                String usuario = partes[0].trim();
                String hash = partes[1].trim();
                String rol = partes[2].trim();

                // Validar que el rol sea válido
                if (!rol.equals(Protocolo.ROLE_USER) && !rol.equals(Protocolo.ROLE_ADMIN)) {
                    logger.warning("Rol inválido en línea " + lineaNum + ": " + rol);
                    continue;
                }

                credenciales.put(usuario, new String[]{hash, rol});
                logger.info("Credencial cargada: usuario=" + usuario + ", rol=" + rol);
            }

            logger.info("Archivo de credenciales cargado correctamente. Total de usuarios: " + credenciales.size());

        } catch (IOException e) {
            logger.severe("Error al cargar credenciales: " + e.getMessage());
        }
    }

    /**
     * Crea un archivo de credenciales por defecto con usuarios de prueba
     * Contraseñas: "password" para usuario y "admin123" para admin
     */
    private void crearArchivoPorDefecto() {
        try {
            java.io.FileWriter fw = new java.io.FileWriter(ARCHIVO_CREDENCIALES);

            // Hashes pre-generados:
            // "password" = 5e884898da28047151d0e56f8dc62927510d5be4b82212dd7282c91fdf92c4fd
            // "admin123" = 0192023a7bbd73250516f069df18b500b4eee9a1a27220006112e68e8f0efa5c

            String contenido = "# Archivo de credenciales del servidor de chat\n" +
                    "# Formato: usuario:hash_sha256:rol\n" +
                    "# Roles permitidos: USER, ADMIN\n\n" +
                    "juan:5e884898da28047151d0e56f8dc62927510d5be4b82212dd7282c91fdf92c4fd:USER\n" +
                    "maria:5e884898da28047151d0e56f8dc62927510d5be4b82212dd7282c91fdf92c4fd:USER\n" +
                    "admin:0192023a7bbd73250516f069df18b500b4eee9a1a27220006112e68e8f0efa5c:ADMIN\n";

            fw.write(contenido);
            fw.close();

            logger.info("Archivo de credenciales creado con usuarios por defecto");

            // Recargar las credenciales
            cargarCredenciales();

        } catch (IOException e) {
            logger.severe("Error al crear archivo de credenciales: " + e.getMessage());
        }
    }

    /**
     * Valida las credenciales de un usuario
     *
     * @param nombreUsuario Nombre de usuario
     * @param password Contraseña en texto plano
     * @return Rol del usuario si las credenciales son válidas, null si no
     */
    public String validarCredenciales(String nombreUsuario, String password) {
        if (nombreUsuario == null || password == null || nombreUsuario.isEmpty() || password.isEmpty()) {
            logger.warning("Intento de validación con parámetros vacíos");
            return null;
        }

        String[] datos = credenciales.get(nombreUsuario);
        if (datos == null) {
            logger.warning("Usuario no encontrado: " + nombreUsuario);
            return null;
        }

        String hashAlmacenado = datos[0];
        String rol = datos[1];

        // Calcular el hash SHA-256 de la contraseña proporcionada
        String hashCalculado = calcularSHA256(password);

        // Comparar los hashes
        if (hashAlmacenado.equals(hashCalculado)) {
            logger.info("Validación exitosa para usuario: " + nombreUsuario + " con rol: " + rol);
            return rol;
        } else {
            logger.warning("Contraseña incorrecta para usuario: " + nombreUsuario);
            return null;
        }
    }

    /**
     * Calcula el hash SHA-256 de un texto
     *
     * @param texto Texto a hashear
     * @return Hash SHA-256 en formato hexadecimal
     */
    public static String calcularSHA256(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(texto.getBytes(StandardCharsets.UTF_8));

            // Convertir a hexadecimal
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            logger.severe("Error al calcular SHA-256: " + e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene el rol de un usuario (para verificación adicional)
     */
    public String obtenerRol(String nombreUsuario) {
        String[] datos = credenciales.get(nombreUsuario);
        return datos != null ? datos[1] : null;
    }

    /**
     * Verifica si un usuario existe en el sistema
     */
    public boolean usuarioExiste(String nombreUsuario) {
        return credenciales.containsKey(nombreUsuario);
    }
}

