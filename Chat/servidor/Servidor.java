

import java.util.Scanner;
import java.util.logging.Logger;

/**
 * Clase principal del servidor de chat.
 * Solicita el puerto de escucha por consola e inicia el servidor.
 */
public class Servidor {
    private static final Logger logger = Logger.getLogger(Servidor.class.getName());
    private static final int PUERTO_MINIMO = 1024;
    private static final int PUERTO_MAXIMO = 65535;
    private static final int PUERTO_DEFECTO = 5000;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int puerto = PUERTO_DEFECTO;

        try {
            System.out.println("  SERVIDOR DE CHAT");
            System.out.println();

            // Solicitar puerto al usuario
            puerto = solicitarPuerto(scanner);

            // Crear e iniciar el servidor
            System.out.println();
            System.out.println("Iniciando servidor en puerto " + puerto + "...");
            ChatServer servidor = new ChatServer(puerto);

            // Iniciar el servidor en un hilo separado
            Thread hiloServidor = new Thread(() -> servidor.iniciar());
            hiloServidor.setName("Servidor-Chat");
            hiloServidor.start();

            System.out.println("✓ Servidor iniciado correctamente");
            System.out.println();
            System.out.println("Ningún cliente conectado");
            System.out.println("Presione CTRL+C para detener el servidor");
            System.out.println();

            // Mantener el servidor ejecutándose
            hiloServidor.join();

        } catch (NumberFormatException e) {
            System.err.println("✗ Error: El puerto debe ser un número entero");
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println("✗ Error: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("✗ Error al iniciar el servidor: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            scanner.close();
        }
    }

    /**
     * Solicita al usuario que ingrese un puerto válido
     *
     * @param scanner Scanner para lectura de consola
     * @return Puerto válido en el rango permitido
     * @throws IllegalArgumentException Si el puerto no es válido
     */
    private static int solicitarPuerto(Scanner scanner) throws IllegalArgumentException {
        System.out.print("Ingrese el puerto de escucha [" + PUERTO_DEFECTO + "]: ");

        String entrada = scanner.nextLine().trim();

        // Si el usuario presiona Enter sin escribir nada, usar puerto por defecto
        if (entrada.isEmpty()) {
            System.out.println("Usando puerto por defecto: " + PUERTO_DEFECTO);
            return PUERTO_DEFECTO;
        }

        // Validar que sea un número
        int puerto;
        try {
            puerto = Integer.parseInt(entrada);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Puerto inválido: " + entrada);
        }

        // Validar rango de puerto
        if (puerto < PUERTO_MINIMO || puerto > PUERTO_MAXIMO) {
            throw new IllegalArgumentException(
                String.format("El puerto debe estar entre %d y %d (ingresado: %d)",
                    PUERTO_MINIMO, PUERTO_MAXIMO, puerto)
            );
        }

        return puerto;
    }
}
