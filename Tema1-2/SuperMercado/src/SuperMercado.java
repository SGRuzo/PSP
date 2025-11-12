import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Clase principal
public class SuperMercado {
    // Variable compartida: total recaudado
    private double resultados = 0;

    // Lista de cajas del supermercado
    private List<Caja> cajas = new ArrayList<>();

    // Objeto para sincronizar la suma al total
    private final Object lock = new Object();

    public SuperMercado(int numCajas) {
        // Crear las cajas
        for (int i = 0; i < numCajas; i++) {
            cajas.add(new Caja(i + 1, this)); // id de caja y referencia al supermercado
        }
    }

    // Metodo sincronizado para añadir importe al total
    public void agregarResultado(double cantidad) {
        synchronized (lock) {
            resultados += cantidad;
        }
    }


    // Permite a un cliente elegir una caja al azar
    public Caja elegirCaja() {
        Random rand = new Random();
        int indice = rand.nextInt(cajas.size());
        return cajas.get(indice);
    }

    public double getResultados() {
        return resultados;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: java SuperMercado <numCajas> <numClientes>");
            return;
        }

        int numCajas = Integer.parseInt(args[0]);
        int numClientes = Integer.parseInt(args[1]);

        SuperMercado superMercado = new SuperMercado(numCajas);

        List<Thread> hilosClientes = new ArrayList<>();

        // Crear y lanzar los hilos de los clientes
        for (int i = 0; i < numClientes; i++) {
            Cliente cliente = new Cliente(i + 1, superMercado);
            Thread hilo = new Thread(cliente);
            hilosClientes.add(hilo);
            hilo.start();
        }

        // Esperar a que todos los clientes terminen
        for (Thread hilo : hilosClientes) {
            try {
                hilo.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.printf("Total recaudado en el supermercado: %.2f €\n", superMercado.getResultados());
    }
}
