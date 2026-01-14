import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class ConexionServidor implements Runnable {
    private final Socket socket;
    private final Usuarios usuarios;
    private final Logica logica;
    private PrintWriter salida;
    private String nickname;
    private String rol;

    public ConexionServidor(Socket socket, Usuarios usuarios) {
        this.socket = socket;
        this.usuarios = usuarios;
        this.logica = new Logica(usuarios);
    }

    @Override
    public void run() {
        try {
            // 1. Inicializar streams
            BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(socket.getOutputStream(), true);

            // 2. Leer LOGIN del cliente (primer mensaje)
            String primeraLinea = entrada.readLine();
            if (primeraLinea == null) {
                System.out.println("⚠️ Cliente desconectado antes de login");
                return;
            }

            Mensaje loginMsg = Protocolo.deserializar(primeraLinea);

            // Validar que sea LOGIN
            if (!loginMsg.getTipo().equals("LOGIN")) {
                salida.println(Logica.crearError("Se esperaba LOGIN como primer mensaje"));
                return;
            }

            nickname = loginMsg.getRemitente();
            rol = loginMsg.getRol();

            // 3. Validar si el usuario ya existe
            if (usuarios.existe(nickname)) {
                salida.println(Logica.crearError("El nickname '" + nickname
                        + "' ya está en uso"));
                return;
            }

            // 4. Agregar usuario a la lista
            usuarios.agregar(nickname, socket, rol);

            // 5. Enviar bienvenida
            salida.println(Protocolo.serializar(
                    new Mensaje("BIENVENIDA", "servidor", "SISTEMA",
                            "Bienvenido " + nickname + ". Escriba /help para ver comandos")
            ));

            // 6. IMPORTANTE: Notificar a TODOS que se conectó
            String notificacion = Logica.crearNotificacion(
                    nickname + " acaba de conectarse a este chat");
            usuarios.broadcast(notificacion);

            // 7. Mostrar en consola del servidor
            System.out.println("> Nuevo cliente conectado (" + nickname
                    + "). Actualmente hay "
                    + usuarios.cantidad() + " usuarios conectados.");

            // 8. LOOP: Leer mensajes del cliente
            String linea;
            while ((linea = entrada.readLine()) != null) {
                procesar(linea);
            }

        } catch (SocketException e) {
            System.out.println("⚠️ El cliente " + nickname
                    + " se desconectó abruptamente");
        } catch (IOException e) {
            System.out.println("⚠️ Error de red con cliente " + nickname
                    + ": " + e.getMessage());
        } finally {
            // IMPORTANTE: Limpiar al desconectar
            if (nickname != null) {
                usuarios.eliminar(nickname);

                // Notificar que se fue
                String aviso = Logica.crearNotificacion(
                        nickname + " se ha desconectado");
                usuarios.broadcast(aviso);

                // Si no hay más usuarios, imprimir el mensaje
                if (usuarios.cantidad() == 0) {
                    System.out.println("Ningún cliente conectado");
                }
            }

            // Cerrar socket
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException ignored) { }
        }
    }

    // Procesar mensaje recibido
    private void procesar(String linea) {
        try {
            Mensaje msg = Protocolo.deserializar(linea);

            if (msg == null || msg.getTipo() == null) {
                System.out.println("⚠️ Mensaje inválido recibido");
                return;
            }

            // Actualizar rol si cambió
            if (!msg.getRol().equals(this.rol)) {
                this.rol = msg.getRol();
                Usuarios.UsuarioInfo user = usuarios.obtener(nickname);
                if (user != null) {
                    user.rol = rol;
                }
            }

            // ¿Es un comando?
            if (msg.getContenido().startsWith("/")) {
                // Validar permisos
                if (!logica.puedeEjecutar(msg.getContenido(), rol)) {
                    salida.println(Logica.crearError(
                            "No tienes permisos para " + msg.getContenido()));
                    return;
                }

                // Procesar comando
                String respuesta = logica.procesarComando(msg.getContenido(), rol);
                salida.println(respuesta);

                // Broadcast especial si es /bye
                if (msg.getContenido().equals("/bye")) {
                    usuarios.eliminar(nickname);
                    usuarios.broadcast(Logica.crearNotificacion(
                            nickname + " se ha desconectado"));
                }

            } else {
                // Mensaje normal
                String respuesta = logica.procesarMensaje(nickname, rol,
                        msg.getContenido());

                // Mostrar en consola del servidor
                System.out.println(nickname + ": " + msg.getContenido());

                // Broadcast a todos
                usuarios.broadcast(respuesta);
            }

        } catch (Exception e) {
            System.out.println("⚠️ Error procesando mensaje: "
                    + e.getMessage());
            salida.println(Logica.crearError("Error procesando tu mensaje"));
        }
    }
}