import java.net.Socket;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Usuarios {
    // Map thread-safe: nickname → UsuarioInfo
    private final ConcurrentHashMap<String, UsuarioInfo> usuariosConectados;

    public Usuarios() {
        this.usuariosConectados = new ConcurrentHashMap<>();
    }

    // Agregar nuevo usuario
    public void agregar(String nickname, Socket socket, String rol) {
        if (nickname == null || nickname.isEmpty() || socket == null) {
            System.out.println("⚠️ Intento de agregar usuario inválido");
            return;
        }
        usuariosConectados.put(nickname, new UsuarioInfo(nickname, socket, rol));
        System.out.println("[USUARIOS] " + nickname + " agregado. Total: "
                + cantidad());
    }

    // Eliminar usuario
    public void eliminar(String nickname) {
        UsuarioInfo user = usuariosConectados.remove(nickname);
        if (user != null) {
            try {
                if (!user.socket.isClosed()) {
                    user.socket.close();
                }
            } catch (IOException ignored) {
                // Socket ya estaba cerrado o error al cerrar
            }
            System.out.println("[USUARIOS] " + nickname + " eliminado. Total: "
                    + cantidad());
        }
    }

    // Enviar mensaje a TODOS los usuarios
    public void broadcast(String mensaje) {
        for (UsuarioInfo usuario : usuariosConectados.values()) {
            try {
                PrintWriter salida = new PrintWriter(
                        usuario.socket.getOutputStream(), true);
                salida.println(mensaje);
                salida.flush();  // Asegurar que se envía inmediatamente
            } catch (IOException e) {
                System.out.println("⚠️ Error enviando a " + usuario.nickname
                        + ": " + e.getMessage());
            }
        }
    }

    // Obtener lista de nicknames conectados
    public List<String> obtenerLista() {
        return new ArrayList<>(usuariosConectados.keySet());
    }

    // Cantidad de usuarios conectados
    public int cantidad() {
        return usuariosConectados.size();
    }

    // Obtener información de un usuario (útil para validaciones)
    public UsuarioInfo obtener(String nickname) {
        return usuariosConectados.get(nickname);
    }

    // Verificar si un usuario existe
    public boolean existe(String nickname) {
        return usuariosConectados.containsKey(nickname);
    }

    // Clase interna para almacenar info del usuario
    public static class UsuarioInfo {
        public final String nickname;
        public final Socket socket;
        public String rol;  // Mutable porque puede cambiar durante la sesión

        public UsuarioInfo(String nickname, Socket socket, String rol) {
            this.nickname = nickname;
            this.socket = socket;
            this.rol = rol;
        }
    }
}