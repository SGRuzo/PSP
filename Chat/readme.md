### Estructura de Proyecto Sugerida

```text
PSP_ChatCorporativo/
├── compartido/
│   ├── Protocolo.java       # Definición de constantes y formato de tramas (mensajes)
│   ├── Mensaje.java         # Clase POJO para representar un mensaje (fácil paso a JSON)
│   ├── Seguridad.java       # Utilidades para SHA-256 y configuración SSL/TLS
│   └── Config.java          # Constantes compartidas (Puertos, IPs por defecto)
│
├── cliente/
│   ├── View.java            # Interfaz Swing (JFrame, JTextArea, etc.)
│   ├── Controller.java      # Captura eventos de la View y llama al Model
│   ├── Model.java           # Gestión del Socket, envío de datos y cifrado
│   └── EscuchaServidor.java # Hilo secundario que lee mensajes entrantes del servidor
│
└── servidor/
    ├── Servidor.java            # Punto de entrada, configuración del Pool de Hilos
    ├── ChatServer.java      # Servidor principal (ServerSocket) y aceptación de conexiones
    ├── ManejadorCliente.java# Hilo (Runnable) que gestiona la lógica de cada cliente
    └── GestorUsuarios.java  # Clase thread-safe para manejar la lista de usuarios y roles

```

---

### Análisis de la Lógica por Directorio

#### 1. Directorio `compartido/` (El nexo de unión)

Para evitar repetir código y cumplir con el requisito de **protocolo propio**:

* **Protocolo.java**: Define cómo se "empaqueta" la información. Por ejemplo: `LOGIN|user|pass` o `MSG|emisor|texto`. Aquí defines los separadores (como `|` o `;`) para que tanto cliente como servidor sepan cómo "partir" la cadena recibida.
* **Mensaje.java**: Una clase simple que guarde `remitente`, `contenido`, `tipoComando` y `timestamp`. Aunque no uses `ObjectOutputStream`, esta clase te sirve para que **GSON** convierta un objeto Java en un String JSON fácilmente.
* **Seguridad.java**: Contendrá el método para convertir contraseñas a SHA-256 (Nivel 3) y, si llegas al Nivel 4, los métodos para cargar los certificados `.jks` para el SSL.

#### 2. Directorio `servidor/` (El cerebro)

* **ChatServer.java**: Usa un `ExecutorService` (Pool de hilos) para limitar a 10 conexiones simultáneas. No maneja la lógica de los mensajes, solo acepta sockets y los pasa al pool.
* **ManejadorCliente.java**: Es el corazón del servidor. Lee el texto plano del socket, lo procesa según el `Protocolo` y decide si es un comando (`/list`, `/ping`) o un mensaje normal.
* **GestorUsuarios.java**: Utiliza una colección segura como `ConcurrentHashMap<String, ManejadorCliente>`. Esto permite al servidor saber quién está conectado y permite el **Broadcasting** (recorrer el mapa para reenviar el mensaje a todos).

#### 3. Directorio `cliente/` (La interfaz)

* **View.java**: Solo componentes visuales. Debe tener un método `mostrarMensaje(String m)` que el controlador llamará.
* **EscuchaServidor.java**: Es vital para el Nivel 5. Es un hilo que hace un `while(true)` leyendo el `BufferedReader` del socket. Si llega algo, se lo pasa al `Controller` para que actualice la `View`. Sin este hilo independiente, la ventana se "congelaría" mientras espera datos.
* **Model.java**: Encapsula el `SSLSocket` o `Socket` normal. Provee métodos como `enviarMensaje(String m)` o `conectar(ip, puerto)`.

---

### Puntos clave para el éxito en la defensa

1. **Robustez**: En el `ManejadorCliente`, rodea la lectura del socket con un `try-catch` de `IOException`. Si el cliente cierra la ventana de golpe, el servidor debe detectar la excepción, avisar al `GestorUsuarios` para que lo elimine de la lista y seguir funcionando.
2. **No Serialización**: Asegúrate de usar `PrintWriter` y `BufferedReader`. Ejemplo de envío:
```java
    out.println(Protocolo.formatearMensaje("CHAT", nickname, "Hola a todos"));
```


3. **Sincronización**: Al ser un entorno multihilo, si dos personas escriben a la vez, el `ConcurrentHashMap` evitará que el servidor "explote" (evita la `ConcurrentModificationException`).

