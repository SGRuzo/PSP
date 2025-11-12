import java.util.Random;

class Cliente implements Runnable {
    private int idCliente;
    private SuperMercado supermercado;
    private Random random = new Random();

    public Cliente(int idCliente, SuperMercado supermercado) {
        this.idCliente = idCliente;
        this.supermercado = supermercado;
    }

    public int getIdCliente() {
        return idCliente;
    }

    @Override
    public void run() {
        try {
            // Simula tiempo de compra (entre 1 y 3 segundos)
            int tiempoCompra = 1000 + random.nextInt(2000);
            System.out.println("Cliente " + idCliente + " está haciendo la compra (" + tiempoCompra + " ms).");
            Thread.sleep(tiempoCompra);

            // Elige una caja al azar
            Caja caja = supermercado.elegirCaja();
            System.out.println("Cliente " + idCliente + " se pone en la cola de la caja " + caja);

            // Paga en la caja
            caja.atenderCliente(this);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
