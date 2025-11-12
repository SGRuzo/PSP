import java.util.Random;

class Caja {
    private int id;
    private SuperMercado supermercado;

    public Caja(int id, SuperMercado supermercado) {
        this.id = id;
        this.supermercado = supermercado;
    }

    // Método sincronizado: solo una persona puede pagar a la vez en la caja
    public synchronized void atenderCliente(Cliente cliente) {
        try {
            // Simula el tiempo de pago
            System.out.println("Cliente " + cliente.getIdCliente() + " está pagando en la caja " + id + "...");
            Thread.sleep(1000 + new Random().nextInt(1000));

            // Importe aleatorio entre 10 y 100 €
            double importe = 10 + new Random().nextDouble() * 90;
            supermercado.agregarResultado(importe);

            System.out.printf("Cliente %d terminó en la caja %d y pagó %.2f €\n",
                    cliente.getIdCliente(), id, importe);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
