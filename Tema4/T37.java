import java.util.Scanner;

public class T37 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Pedir el texto a cifrar
        System.out.print("Ingrese el texto a cifrar: ");
        String textoOriginal = scanner.nextLine();

        // Pedir el desplazamiento
        System.out.print("Ingrese el número de desplazamiento: ");
        int desplazamiento = scanner.nextInt();

        // Cifrar el texto y mostrarlo
        String textoCifrado = cifrarCesar(textoOriginal, desplazamiento);
        System.out.println("Texto cifrado: " + textoCifrado);

        scanner.close();
    }

    public static String cifrarCesar(String texto, int desplazamiento) {
        StringBuilder resultado = new StringBuilder();

        desplazamiento = desplazamiento % 26;

        for (char c : texto.toCharArray()) {
            if (Character.isLetter(c)) {
                char base = Character.isUpperCase(c) ? 'A' : 'a';
                char cifrada = (char) ((c - base + desplazamiento) % 26 + base);
                resultado.append(cifrada);
            } else {
                resultado.append(c);
            }
        }

        return resultado.toString();
    }
}
