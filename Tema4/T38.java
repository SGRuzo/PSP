import java.util.HexFormat;
import java.util.Scanner;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class T38 {
    private static String hashGuardado;
    public static void main(String[] args) {
        Scanner scanner=new Scanner(System.in);

        //1. Fase Registro
        System.out.print("Crea una contraseña: ");
        String contrasenhaRegistro=scanner.nextLine();
        // Calcular hash de la contraseña
        hashGuardado =calcularHashSHA256(contrasenhaRegistro);
        System.out.println("Contraseña registrada exitosamente.");
        System.out.println("Hash almacenado: "+hashGuardado);

        //2. Fase Login
        System.out.println("Usuario registrado. Inicie sesión para probar.");
        System.out.print("Introduce tu contraseña: ");
        String contrasenhaLogin =scanner.nextLine();
        // Calcular hash de la contraseña introducida
        String hashIngresado=  calcularHashSHA256(contrasenhaLogin);

        //3. Resultado
        if (hashGuardado.equals(hashIngresado)) {
            System.out.println("ACCESO CONCEDIDO");
        } else {
            System.out.println("ERROR: Credenciales inválidas");
        }
        scanner.close();
    }

    private static String calcularHashSHA256(String contrasenia) {
        try {
            // 1. Instanciar
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // 2. Actualizar
            md.update(contrasenia.getBytes());

            // 3. Resumir (devuelve bytes)
            byte[] resumen=md.digest();

            // 4. Convertir a hexadecimal (legible)
            String hex =HexFormat.of().formatHex(resumen);
            return hex;
        } catch (NoSuchAlgorithmException e) {
            System.err.println("ERROR:Algoritmo SHA-256 no disponible");
            e.printStackTrace();
            return null;
        }

    }
}
