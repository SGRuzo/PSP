

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

/**
 * Clase de utilidades de seguridad para el chat.
 * Proporciona métodos para:
 * - Hashing de contraseñas SHA-256 (Nivel 3)
 * - Gestión de certificados SSL/TLS (Nivel 4)
 */
public class Seguridad {
    private static final Logger logger = Logger.getLogger(Seguridad.class.getName());
    private static final String ALGORITMO_HASH = "SHA-256";
    private static final String PROTOCOLO_SSL = "TLSv1.2";

    // ==================== NIVEL 3: HASHING SHA-256 ====================

    /**
     * Convierte una contraseña a su equivalente SHA-256
     *
     * @param contrasena Contraseña en texto plano
     * @return String hexadecimal del hash SHA-256
     * @throws NoSuchAlgorithmException Si SHA-256 no está disponible
     */
    public static String hashSHA256(String contrasena) throws NoSuchAlgorithmException {
        if (contrasena == null || contrasena.isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }

        MessageDigest digest = MessageDigest.getInstance(ALGORITMO_HASH);
        byte[] hashBytes = digest.digest(contrasena.getBytes());

        return bytesAHexadecimal(hashBytes);
    }

    /**
     * Verifica que una contraseña coincida con su hash
     *
     * @param contrasena Contraseña en texto plano
     * @param hash Hash SHA-256 almacenado
     * @return true si la contraseña es correcta, false en caso contrario
     */
    public static boolean verificarSHA256(String contrasena, String hash) {
        try {
            String nuevoHash = hashSHA256(contrasena);
            return nuevoHash.equals(hash);
        } catch (NoSuchAlgorithmException e) {
            logger.severe("Error al verificar hash: " + e.getMessage());
            return false;
        }
    }

    /**
     * Convierte un array de bytes a su representación hexadecimal
     *
     * @param bytes Array de bytes
     * @return String hexadecimal (minúsculas)
     */
    private static String bytesAHexadecimal(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // ==================== NIVEL 4: CERTIFICADOS SSL/TLS ====================

    /**
     * Carga el contexto SSL con el almacén de claves (KeyStore)
     * Utilizado en el servidor para proporcionar el certificado
     *
     * @param rutaKeyStore Ruta al archivo .jks del servidor
     * @param passwordKeyStore Contraseña del KeyStore
     * @return SSLContext configurado para el servidor
     * @throws Exception Si hay problemas al cargar el certificado
     */
    public static SSLContext crearSSLContextServidor(String rutaKeyStore, String passwordKeyStore) throws Exception {
        if (rutaKeyStore == null || rutaKeyStore.isEmpty()) {
            throw new IllegalArgumentException("La ruta del KeyStore no puede estar vacía");
        }

        logger.info("Cargando KeyStore servidor desde: " + rutaKeyStore);

        // Cargar el KeyStore
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(rutaKeyStore)) {
            keyStore.load(fis, passwordKeyStore.toCharArray());
        } catch (IOException e) {
            logger.severe("Error al cargar el KeyStore: " + e.getMessage());
            throw new Exception("No se pudo cargar el KeyStore: " + rutaKeyStore, e);
        }

        // Inicializar el KeyManagerFactory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keyStore, passwordKeyStore.toCharArray());

        // Crear el SSLContext
        SSLContext sslContext = SSLContext.getInstance(PROTOCOLO_SSL);
        sslContext.init(kmf.getKeyManagers(), null, null);

        logger.info("SSLContext del servidor inicializado correctamente");
        return sslContext;
    }

    /**
     * Carga el contexto SSL con el almacén de confianza (TrustStore)
     * Utilizado en el cliente para verificar el certificado del servidor
     *
     * @param rutaTrustStore Ruta al archivo .jks del cliente (truststore)
     * @param passwordTrustStore Contraseña del TrustStore
     * @return SSLContext configurado para el cliente
     * @throws Exception Si hay problemas al cargar el certificado
     */
    public static SSLContext crearSSLContextCliente(String rutaTrustStore, String passwordTrustStore) throws Exception {
        if (rutaTrustStore == null || rutaTrustStore.isEmpty()) {
            throw new IllegalArgumentException("La ruta del TrustStore no puede estar vacía");
        }

        logger.info("Cargando TrustStore cliente desde: " + rutaTrustStore);

        // Cargar el TrustStore
        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(rutaTrustStore)) {
            trustStore.load(fis, passwordTrustStore.toCharArray());
        } catch (IOException e) {
            logger.severe("Error al cargar el TrustStore: " + e.getMessage());
            throw new Exception("No se pudo cargar el TrustStore: " + rutaTrustStore, e);
        }

        // Inicializar el TrustManagerFactory
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(trustStore);

        // Crear el SSLContext
        SSLContext sslContext = SSLContext.getInstance(PROTOCOLO_SSL);
        sslContext.init(null, tmf.getTrustManagers(), null);

        logger.info("SSLContext del cliente inicializado correctamente");
        return sslContext;
    }

    /**
     * Carga un SSLContext bidireccional (servidor con cliente autenticado)
     * Requiere tanto KeyStore como TrustStore
     *
     * @param rutaKeyStore Ruta al keystore del servidor
     * @param passwordKeyStore Contraseña del keystore
     * @param rutaTrustStore Ruta al truststore del servidor
     * @param passwordTrustStore Contraseña del truststore
     * @return SSLContext completamente configurado
     * @throws Exception Si hay problemas al cargar los certificados
     */
    public static SSLContext crearSSLContextBidireccional(
            String rutaKeyStore, String passwordKeyStore,
            String rutaTrustStore, String passwordTrustStore) throws Exception {

        logger.info("Configurando SSL bidireccional con autenticación mutua");

        // Cargar KeyStore
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(rutaKeyStore)) {
            keyStore.load(fis, passwordKeyStore.toCharArray());
        }

        // Cargar TrustStore
        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(rutaTrustStore)) {
            trustStore.load(fis, passwordTrustStore.toCharArray());
        }

        // Inicializar managers
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keyStore, passwordKeyStore.toCharArray());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(trustStore);

        // Crear SSLContext
        SSLContext sslContext = SSLContext.getInstance(PROTOCOLO_SSL);
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        logger.info("SSLContext bidireccional inicializado correctamente");
        return sslContext;
    }
}
