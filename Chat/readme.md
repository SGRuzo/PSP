# Sistema de Chat Corporativo de Alto Rendimiento

## ⚠️ **DOCUMENTACIÓN COMPLEMENTARIA**

**Antes de leer esta documentación, revisa primero:** 📄 [`ANÁLISIS_CRÍTICO.md`](./ANÁLISIS_CRÍTICO.md)

Este archivo contiene un **análisis línea por línea** de:
- ✅ **Lo que SÍ cumples correctamente** (según el enunciado)
- ⚠️ **Lo que está a MEDIAS** (riesgo de incumplimiento)
- ❌ **Lo que FALTA implementar** (crítico para pasar)

---

## Explicación de la Estructura del Proyecto

Este proyecto implementa un **sistema de chat multicliente con arquitectura escalable**, respetando una clara **separación de responsabilidades** y cumpliendo con todos los requisitos críticos del ejercicio.

---

## 📁 Organización del Proyecto

### **1. Estructura de Carpetas**

```
Chat/
├── Compartido/              # Código compartido entre cliente y servidor
│   └── src/
│       ├── Protocolo.java     # Definición del protocolo de comunicación (COMANDO|REMITENTE|CONTENIDO)
│       ├── Mensaje.java       # Representación de mensajes (sin serialización de objetos)
│       └── UtilidadesSSL.java # Cifrado SSL para privacidad de comunicaciones
│
├── servidor/                # Código del servidor
│   └── src/
│       ├── Main.java          # Punto de entrada del servidor
│       ├── Logica.java        # Lógica de negocio (procesamiento de comandos)
│       ├── ConexionServidor.java  # Manejo de conexiones con clientes
│       └── Usuarios.java      # Gestión de usuarios conectados (thread-safe)
│
├── cliente/                 # Código del cliente
│   └── src/
│       ├── Main.java          # Punto de entrada del cliente
│       ├── ConexionCliente.java   # Gestión de conexión al servidor
│       └── Listener.java      # Hilo de escucha de mensajes entrantes
│
└── SocialChatInterface.java # Interfaz gráfica Swing (UI)
```

---

## 🏗️ Separación de Responsabilidades

La estructura respeta el principio **"No al Código Espagueti"**:

### **Capa de Comunicación (Compartido)**

| Archivo | Responsabilidad |
|---------|-----------------|
| **Protocolo.java** | Define el formato de comunicación en **texto plano** (no serialización de objetos). Ejemplo: `PRIVADO\|usuario1\|usuario2\|Hola` |
| **Mensaje.java** | Clase POJO que encapsula un mensaje con propiedades: `tipo`, `remitente`, `contenido`, `timestamp` |
| **UtilidadesSSL.java** | Utilidades para cifrado/descifrado SSL, garantizando **privacidad** |

**¿Por qué separado?**
- El cliente y el servidor necesitan ambos entender el protocolo
- Cambios en el protocolo se hacen en un único lugar
- Facilita mantenimiento y pruebas

---

### **Capa del Servidor (servidor/)**

| Archivo | Responsabilidad |
|---------|-----------------|
| **Main.java** | Punto de entrada. Inicializa el servidor, solicita puerto, arranca el ServerSocket |
| **Logica.java** | **Lógica de negocio pura**: procesa comandos (`/bye`, `/list`, `/ping`), valida mensajes, decide qué hacer con cada comando |
| **ConexionServidor.java** | **Gestión de red**: maneja sockets, lee/escribe datos, implementa `Runnable` para ejecutarse en un hilo |
| **Usuarios.java** | **Gestión de estado compartido**: almacena usuarios conectados en una colección **thread-safe** (`ConcurrentHashMap`), evita problemas de sincronización |

**Flujo de Procesamiento del Servidor:**
```
Main
 ├─> Crea ServerSocket (pide puerto al usuario)
 └─> Espera conexiones en un ExecutorService (Pool de Hilos)
      └─> Para cada cliente nuevo:
          └─> Crea ConexionServidor(socket, usuarios)
              └─> Lee mensaje del cliente
                  └─> Envía a Logica.procesar(mensaje)
                      └─> Logica devuelve respuesta
                          └─> ConexionServidor la reenvia al cliente
                              └─> ConexionServidor hace broadcast a todos
```

---

### **Capa del Cliente (cliente/)**

| Archivo | Responsabilidad |
|---------|-----------------|
| **Main.java** | Punto de entrada. Solicita IP, puerto y nickname. Inicializa ConexionCliente |
| **ConexionCliente.java** | **Conexión activa**: mantiene Socket abierto, envía mensajes al servidor |
| **Listener.java** | **Recepción pasiva**: implementa `Runnable`, escucha continuamente en segundo plano lo que dice el servidor |

**Flujo de Procesamiento del Cliente:**
```
Main
 └─> Solicita: IP, Puerto, Nickname
     └─> Crea ConexionCliente(ip, puerto, nickname)
     └─> Inicia Listener en un Thread aparte
         ├─> Thread: Listener.run()
         │   └─> Escucha mensajes del servidor continuamente
         │       └─> Los imprime en consola o UI
         │
         └─> Thread principal: lee input del usuario
             └─> Procesa comandos (/bye, /list, /ping)
             └─> Envía mediante ConexionCliente
```

---

### **Interfaz Gráfica (SocialChatInterface.java)**

- Implementa una UI moderna con Swing
- **Opcional** para el Nivel 1, pero ya está lista
- Puede integrarse con `ConexionCliente` para una experiencia visual

---

## 🔐 Cumplimiento de Normas Críticas

### ✅ **Formato de Datos: Texto Plano, No Serialización**

```
❌ PROHIBIDO:
ObjectOutputStream/ObjectInputStream

✅ PERMITIDO:
- Protocolo personalizado: COMANDO|REMITENTE|CONTENIDO
- JSON manual con GSON/Jackson
- Strings con separadores de campo
```

**En nuestro proyecto:**
- `Protocolo.java` define el protocolo en texto plano
- `ConexionServidor.java` y `ConexionCliente.java` leen/escriben con `BufferedReader` y `PrintWriter`
- `Mensaje.java` es un POJO sin serialización

---

### ✅ **Estabilidad: El Servidor Nunca Se Cae**

```
Servidor recibe datos inválidos → Maneja excepción → Responde ERROR|mensaje → Sigue funcionando
Cliente se desconecta abruptamente → Servidor detecta EOF → Elimina usuario de Usuarios → Sigue funcionando
```

**Mecanismos implementados:**
- Try-catch en `ConexionServidor.run()` para excepciones de red
- `Usuarios` usa `ConcurrentHashMap` para evitar corrupción de datos
- ExecutorService con tamaño fijo (máx 10) protege contra sobrecarga

---

## ⚙️ Nivel 1: Mínimo Obligatorio (5 pts)

### 📊 **ESTADO DE IMPLEMENTACIÓN: ANÁLISIS CRÍTICO**

---

### 1️⃣ **Arquitectura y Gestión Técnica**

#### ✅ Concurrencia: Pool de Hilos (ExecutorService) - **CORRECTO**

```java
// En Main.java (Servidor)
ExecutorService executorService = Executors.newFixedThreadPool(10);

while (true) {
    Socket clientSocket = serverSocket.accept();
    // Cada cliente se ejecuta en un hilo del pool
    executorService.execute(new ConexionServidor(clientSocket, usuarios));
}
```

**Estado:** ✅ **CUMPLE CORRECTAMENTE**
- Máximo 10 conexiones simultáneas
- Gestión automática de hilos sin overhead

---

#### ✅ Sincronización: Thread-Safe Collections - **CORRECTO**

```java
// En Usuarios.java
private ConcurrentHashMap<String, UsuarioInfo> usuariosConectados;
// NO usar HashMap, LinkedHashMap, etc (no thread-safe)
```

**Estado:** ✅ **CUMPLE CORRECTAMENTE**
- Sin `synchronized` manual
- Evita deadlocks y race conditions
- Broadcast seguro desde múltiples hilos

---

#### ✅ Separación de Responsabilidades - **CORRECTO**

| Componente | Responsabilidad | Ubicación |
|-----------|-----------------|-----------|
| **Sockets** | Lectura/escritura de red | `ConexionServidor`, `ConexionCliente` |
| **Hilos** | Ejecutor de tareas concurrentes | `Main.java` (ExecutorService) |
| **Lógica** | Procesar comandos y reglas de negocio | `Logica.java` |
| **Estado** | Guardar usuarios conectados | `Usuarios.java` |
| **Protocolo** | Serializar/deserializar mensajes | `Protocolo.java` |

**Estado:** ✅ **CUMPLE CORRECTAMENTE**
- Cero "código espagueti"
- Cada clase tiene una responsabilidad única
- Fácil mantenimiento y testing

---

### ⚠️ **PUNTOS CRÍTICOS A REVISAR**

#### **PROBLEMA 1: Cifrado SSL - "A Medias" ⚠️**

**Enunciado dice:** "garantizar la identidad de los usuarios (roles)... y la privacidad (cifrado SSL)"

**Tu estructura actual:** `UtilidadesSSL.java` existe pero está vacía

**RIESGO:** 
- SSL en Java **NO es una clase que cifra Strings**
- SSL requiere `SSLSocketFactory` y `SSLServerSocketFactory`
- Si solo haces cifrado manual de texto, **no es SSL real**

**¿Qué se necesita?**

Opción A - **SSL Real (Más correcto para el enunciado):**
```java
// En Main.java (Servidor)
SSLServerSocketFactory factory = 
    (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
SSLServerSocket serverSocket = 
    (SSLServerSocket) factory.createServerSocket(puerto);
```

Opción B - **Cifrado Manual (Más simple, pero no es "SSL oficial"):**
```java
// En UtilidadesSSL.java (si prefieres esta ruta)
public static String cifrar(String texto) { /*...*/ }
public static String descifrar(String texto) { /*...*/ }
```

**Recomendación:** 
- Mínimo necesario: Implementa `UtilidadesSSL.java` con AES (cifrado reversible)
- Óptimo: Usa `SSLSocket` de verdad para un "canal cifrado"
- **CRÍTICO:** El enunciado exige SSL, así que mínimo no puedes dejar vacío

---

#### **PROBLEMA 2: Gestión de Roles - "No Implementado" ❌**

**Enunciado dice:** "garantizar la identidad de los usuarios **(roles)**"

**Tu estructura actual:** No hay gestión de roles

**¿QUÉ FALTA?**

Necesitas agregar:

```java
// En Mensaje.java (añadir este campo)
private String rol;  // "ADMIN", "USER", "GUEST"

public Mensaje(String tipo, String remitente, String contenido, String rol) {
    this.tipo = tipo;
    this.remitente = remitente;
    this.contenido = contenido;
    this.rol = rol;
}
```

```java
// En Usuarios.java (guardar el rol junto al usuario)
private class UsuarioInfo {
    String nickname;
    Socket socket;
    String rol;  // IMPORTANTE: guardar el rol
    
    UsuarioInfo(String nickname, Socket socket, String rol) {
        this.nickname = nickname;
        this.socket = socket;
        this.rol = rol;
    }
}
```

```java
// En Logica.java (validar acciones según rol)
public String procesarComando(String comando, String rol) {
    if (comando.equals("/admin-shutdown") && !rol.equals("ADMIN")) {
        return "ERROR|No tienes permisos de ADMIN";
    }
    // ... resto de comandos
}
```

**Protocolo actualizado:**
```
MENSAJE|juan|USUARIO|Hola a todos
COMANDO|maria|ADMIN|/admin-shutdown
RESPUESTA|servidor|ADMIN|Servidor apagándose
```

---

#### **PROBLEMA 3: Notificación de Conexión - "Medio Implementado" ⚠️**

**Enunciado dice:** 
> "Notificará al resto: 'nickname acaba de conectarse a este chat'"

**Tu estructura actual:** Falta el flujo exacto

**¿QUÉ FALTA?**

En `ConexionServidor.java`, cuando un cliente se conecta:

```java
public void run() {
    try {
        // 1. Leer el primer mensaje (el LOGIN)
        String primeraLinea = entrada.readLine();
        
        // 2. Parsear según protocolo: LOGIN|juan|contraseña
        Mensaje login = Protocolo.deserializar(primeraLinea);
        String nickname = login.getRemitente();
        String rol = login.getRol();
        
        // 3. Agregar a usuarios
        usuarios.agregar(nickname, socket, rol);
        
        // 4. IMPORTANTE: Notificar a TODOS (broadcast especial)
        String notificacion = Protocolo.serializar(
            new Mensaje("NOTIFICACION", "servidor", 
                        nickname + " acaba de conectarse a este chat", "SISTEMA")
        );
        usuarios.broadcast(notificacion);
        
        // 5. Mostrar en servidor
        System.out.println("> Nuevo cliente conectado (" + nickname 
            + "). Actualmente hay " + usuarios.cantidad() + " usuarios conectados.");
        
        // 6. Loop de lectura normal
        while (true) {
            String linea = entrada.readLine();
            if (linea == null) break;  // Desconexión
            // ... procesar mensaje normal
        }
    } catch (IOException e) {
        // ... manejo de errores
    }
}
```

**CRÍTICO:** El usuario que se conecta también debe **recibir** la notificación de sí mismo

---

#### **PROBLEMA 4: Cierre Limpio del Servidor - "Falta Implementación" ❌**

**Enunciado dice:**
> "Si el servidor se cierra, los clientes deben cerrar sus conexiones tras recibir el mensaje: 'El servidor se desconectó'"

**Tu estructura actual:** No hay lógica de shutdown limpio

**¿QUÉ FALTA?**

En `Main.java` (Servidor):

```java
public class Main {
    private static ExecutorService executorService;
    private static Usuarios usuarios;
    
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Ingrese el puerto para el servidor: ");
        int puerto = sc.nextInt();
        
        usuarios = new Usuarios();
        executorService = Executors.newFixedThreadPool(10);
        
        ServerSocket serverSocket = new ServerSocket(puerto);
        System.out.println("Ningún cliente conectado");
        
        // Agregar hook de shutdown (Ctrl+C)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            apagarServidor();
        }));
        
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executorService.execute(new ConexionServidor(clientSocket, usuarios));
            }
        } finally {
            apagarServidor();
        }
    }
    
    private static void apagarServidor() {
        System.out.println("\n⚠️ Apagando servidor...");
        
        // 1. Enviar a todos los clientes el mensaje de desconexión
        String mensajeDesconexion = 
            "NOTIFICACION|servidor|El servidor se desconectó|SISTEMA";
        usuarios.broadcast(mensajeDesconexion);
        
        // 2. Dar tiempo para que se envíe
        try { Thread.sleep(500); } catch (InterruptedException e) {}
        
        // 3. Apagar ejecutor
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
        
        System.out.println("✅ Servidor apagado");
    }
}
```

---

#### **PROBLEMA 5: Mensaje "Ningún cliente conectado" - "Parcialmente Implementado" ⚠️**

**Enunciado dice:**
> "Mientras no haya clientes, mostrará: 'Ningún cliente conectado'"

**Tu estructura actual:** Se imprime una sola vez al inicio

**¿QUÉ FALTA?**

Cuando se desconecta el **último usuario**, debe volver a aparecer el mensaje:

```java
// En ConexionServidor.java
public void run() {
    String nickname = null;
    try {
        // ... parsear login ...
        nickname = login.getRemitente();
        usuarios.agregar(nickname, socket, rol);
        
        // ... loop de mensajes ...
        
    } catch (IOException e) {
        // ...
    } finally {
        if (nickname != null) {
            usuarios.eliminar(nickname);
            
            // CRÍTICO: Si no hay más usuarios, imprimir el mensaje
            if (usuarios.cantidad() == 0) {
                System.out.println("Ningún cliente conectado");
            }
        }
    }
}
```

---

### 📋 **TABLA RESUMEN: ESTADO ACTUAL VS. REQUISITOS**

| Requisito | Estado | Riesgo | Acciones |
|-----------|--------|--------|----------|
| **Pool de 10 hilos** | ✅ Correcto | ✅ Ninguno | Implementar como se describe |
| **ConcurrentHashMap** | ✅ Correcto | ✅ Ninguno | Implementar como se describe |
| **Separación capas** | ✅ Correcto | ✅ Ninguno | Mantener estructura |
| **Protocolo texto plano** | ✅ Correcto | ✅ Ninguno | Usar BufferedReader/PrintWriter |
| **Cifrado SSL** | ⚠️ A medias | ⚠️ Crítico | Implementar SSLSocket O UtilidadesSSL |
| **Gestión de roles** | ❌ Falta | ❌ Crítico | Agregar campo `rol` a Mensaje y Usuarios |
| **Notificación de conexión** | ⚠️ Parcial | ⚠️ Importante | Agregar broadcast especial en login |
| **Cierre limpio servidor** | ❌ Falta | ❌ Crítico | Implementar shutdown hook y broadcast |
| **Mensaje "Ningún cliente"** | ⚠️ Parcial | ⚠️ Importante | Detectar cuando cantidad() == 0 |
| **Comandos /bye /list /ping** | ✅ Diseño listo | ✅ Ninguno | Implementar en Logica.java |

---

### 2️⃣ **Comportamiento del Servidor**

#### ✅ Inicio con Solicitud de Puerto - **CORRECTO**

```java
// En Main.java
Scanner sc = new Scanner(System.in);
System.out.print("Ingrese el puerto para el servidor: ");
int puerto = sc.nextInt();
ServerSocket serverSocket = new ServerSocket(puerto);
```

**Estado:** ✅ **Listo para implementar**

---

#### ⚠️ Mensaje "Ningún cliente conectado" - **PARCIALMENTE IMPLEMENTADO**

**Requisito:** Debe mostrarse al inicio Y cuando se desconecta el último usuario

```java
// En Main.java (CORRECTO: al inicio)
System.out.println("Ningún cliente conectado");

// FALTA en ConexionServidor.java (detectar cuando es el último)
@Override
public void run() {
    String nickname = null;
    try {
        // ... lógica de conexión ...
    } finally {
        if (nickname != null) {
            usuarios.eliminar(nickname);
            
            // IMPORTANTE: Si no hay más usuarios
            if (usuarios.cantidad() == 0) {
                System.out.println("Ningún cliente conectado");
            }
        }
    }
}
```

**Estado:** ⚠️ **Falta lógica en el finally de ConexionServidor**

---

#### ⚠️ Mensaje de Conexión Nueva - **PARCIALMENTE IMPLEMENTADO**

**Requisito:** Mostrar en consola Y notificar a otros usuarios

```java
// En ConexionServidor.java (FALTA: esta lógica)
public void run() {
    try {
        // 1. Leer LOGIN: "LOGIN|juan|contraseña"
        String primeraLinea = entrada.readLine();
        Mensaje login = Protocolo.deserializar(primeraLinea);
        String nickname = login.getRemitente();
        String rol = login.getRol(); // FALTA: rol
        
        // 2. Agregar a usuarios
        usuarios.agregar(nickname, socket, rol);
        
        // 3. Mostrar en consola del servidor
        System.out.println("> Nuevo cliente conectado (" + nickname 
            + "). Actualmente hay " + usuarios.cantidad() + " usuarios conectados.");
        
        // 4. CRÍTICO: Notificar a TODOS que se conectó
        String notificacion = Protocolo.serializar(
            new Mensaje("NOTIFICACION", "servidor",
                        nickname + " acaba de conectarse a este chat", "SISTEMA")
        );
        usuarios.broadcast(notificacion);
        
        // 5. Continuar escuchando mensajes del cliente
        while (true) {
            String linea = entrada.readLine();
            if (linea == null) break;
            // ... procesar mensaje ...
        }
    } catch (IOException e) {
        // ... manejo de errores ...
    }
}
```

**Estado:** ⚠️ **Falta agregar el broadcast de notificación de conexión**

---

#### ✅ Broadcasting de Mensajes - **CORRECTO (DISEÑO)**

```java
// En ConexionServidor.java
String mensaje = entrada.readLine(); // Lee del cliente
System.out.println(nickname + ": " + mensaje); // Muestra en consola
usuarios.broadcast(mensaje); // Reenvía a TODOS los clientes
```

**Estado:** ✅ **Diseño correcto, lista para implementar**

---

#### ❌ Cierre Limpio del Servidor - **NO IMPLEMENTADO**

**Requisito:** 
1. El servidor recibe Ctrl+C (shutdown signal)
2. Envía a TODOS los clientes: "El servidor se desconectó"
3. Cierra todas las conexiones
4. Se apaga

**¿QUÉ FALTA?**

```java
// En Main.java
private static ExecutorService executorService;
private static Usuarios usuarios;
private static ServerSocket serverSocket;

public static void main(String[] args) throws IOException {
    Scanner sc = new Scanner(System.in);
    System.out.print("Ingrese el puerto para el servidor: ");
    int puerto = sc.nextInt();
    
    usuarios = new Usuarios();
    executorService = Executors.newFixedThreadPool(10);
    serverSocket = new ServerSocket(puerto);
    
    System.out.println("Ningún cliente conectado");
    
    // IMPORTANTE: Agregar hook de shutdown
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        apagarServidor();
    }));
    
    try {
        while (true) {
            Socket clientSocket = serverSocket.accept();
            executorService.execute(new ConexionServidor(clientSocket, usuarios));
        }
    } finally {
        apagarServidor();
    }
}

private static void apagarServidor() {
    System.out.println("\n⚠️ Apagando servidor...");
    
    // 1. Enviar mensaje de desconexión a TODOS
    String mensaje = "NOTIFICACION|servidor|El servidor se desconectó|SISTEMA";
    usuarios.broadcast(mensaje);
    
    // 2. Esperar a que se envíen
    try { Thread.sleep(500); } catch (InterruptedException e) {}
    
    // 3. Cerrar executor
    executorService.shutdown();
    try {
        if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
        }
    } catch (InterruptedException e) {
        executorService.shutdownNow();
    }
    
    // 4. Cerrar socket servidor
    try { serverSocket.close(); } catch (IOException e) {}
    
    System.out.println("✅ Servidor apagado");
}
```

**Estado:** ❌ **FALTA completamente**

---

### 3️⃣ **Comportamiento del Cliente**

#### ✅ Configuración Inicial (IP, Puerto, Nickname) - **CORRECTO**

```java
// En Main.java (Cliente)
Scanner sc = new Scanner(System.in);
System.out.print("IP del servidor: ");
String ip = sc.nextLine();
System.out.print("Puerto: ");
int puerto = sc.nextInt();
sc.nextLine(); // Consumir salto de línea
System.out.print("Nickname: ");
String nickname = sc.nextLine();

ConexionCliente conexion = new ConexionCliente(ip, puerto, nickname);
```

**Estado:** ✅ **Listo para implementar**

---

#### ⚠️ Mensaje de Conexión - **PARCIALMENTE IMPLEMENTADO**

**Requisito:** 
1. Cliente: "Conectado a la sala de chat"
2. Servidor notifica al resto: "nickname acaba de conectarse a este chat"

```java
// En ConexionCliente.java (FALTA: esta lógica)
public void conectar() throws IOException {
    socket = new Socket(ip, puerto);
    entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    salida = new PrintWriter(socket.getOutputStream(), true);
    
    // Enviar mensaje LOGIN
    String loginMsg = "LOGIN|" + nickname + "|usuario"; // Formato del protocolo
    salida.println(loginMsg);
    
    // FALTA: Esperar confirmación del servidor
    String respuesta = entrada.readLine();
    if (respuesta.contains("BIENVENIDA")) {
        System.out.println("Conectado a la sala de chat");
    }
}
```

**Estado:** ⚠️ **Falta el flujo de login inicial**

---

#### ✅ Mostrar Mensajes en Tiempo Real - **CORRECTO (DISEÑO)**

```java
// En Listener.java (hilo de escucha)
public void run() {
    try {
        while (true) {
            String mensaje = entrada.readLine();
            if (mensaje == null) {
                System.out.println("⚠️ Conexión cerrada por el servidor");
                break;
            }
            
            // Parsear según protocolo
            // Formato: TIPO|remitente|contenido
            if (mensaje.contains("NOTIFICACION")) {
                System.out.println("[NOTIFICACIÓN] " + mensaje);
            } else {
                System.out.println(mensaje); // nickname: contenido
            }
        }
    } catch (IOException e) {
        System.out.println("⚠️ Error de lectura: " + e.getMessage());
    }
}
```

**Estado:** ✅ **Diseño correcto, lista para implementar**

---

#### ✅ Comandos Obligatorios - **CORRECTO (DISEÑO)**

| Comando | Flujo |
|---------|-------|
| **/bye** | Cliente → Envía "COMANDO\|nickname\|/bye" → Servidor → Logica lo procesa → Cierra conexión cliente |
| **/list** | Cliente → Envía "COMANDO\|nickname\|/list" → Servidor → Logica consulta Usuarios → Responde lista |
| **/ping** | Cliente → Envía "COMANDO\|nickname\|/ping" → Servidor → Logica responde "pong" → Cliente lo imprime |

**Implementación en Main.java (Cliente):**

```java
// En Main.java (Cliente) - Thread de entrada de usuario
while (true) {
    System.out.print("> ");
    String input = scanner.readLine();
    
    if (input.startsWith("/")) {
        // Es un comando
        switch (input) {
            case "/bye":
                conexion.enviar("COMANDO|" + nickname + "|/bye");
                System.out.println("Desconectando...");
                System.exit(0);
                break;
            case "/list":
                conexion.enviar("COMANDO|" + nickname + "|/list");
                break;
            case "/ping":
                conexion.enviar("COMANDO|" + nickname + "|/ping");
                break;
            default:
                System.out.println("Comando desconocido");
        }
    } else {
        // Es un mensaje normal
        conexion.enviar("MENSAJE|" + nickname + "|" + input);
    }
}
```

**Estado:** ✅ **Diseño correcto, lista para implementar**

---

#### ❌ Mensaje "El servidor se desconectó" - **FALTA MANEJO**

**Requisito:** Cuando el cliente recibe este mensaje, debe cerrar la conexión limpiamente

```java
// En Listener.java (FALTA: esta lógica)
public void run() {
    try {
        while (true) {
            String mensaje = entrada.readLine();
            if (mensaje == null) {
                System.out.println("⚠️ Conexión cerrada por el servidor");
                break;
            }
            
            // IMPORTANTE: Si recibe el mensaje de desconexión del servidor
            if (mensaje.contains("El servidor se desconectó")) {
                System.out.println(mensaje);
                // Cerrar todo limpiamente
                socket.close();
                System.exit(0);
            }
            
            System.out.println(mensaje);
        }
    } catch (IOException e) {
        System.out.println("⚠️ Error de red: conexión perdida");
    }
}
```

**Estado:** ❌ **FALTA manejo del cierre del servidor**

---

### 4️⃣ **Robustez y Errores**

#### ✅ Control de Excepciones de Red - **CORRECTO**

```java
// En ConexionServidor.java
try {
    while (true) {
        String mensaje = entrada.readLine();
        if (mensaje == null) { // Cliente desconectado
            usuarios.eliminar(nickname);
            break;
        }
        procesar(mensaje);
    }
} catch (SocketException e) {
    System.out.println("⚠️ El cliente " + nickname + " se desconectó abruptamente");
    usuarios.eliminar(nickname);
} catch (IOException e) {
    System.out.println("⚠️ Error de red: Problema con la conexión");
    usuarios.eliminar(nickname);
} finally {
    try { 
        if (socket != null) socket.close(); 
    } catch (IOException ignored) { }
}
```

**Beneficios:**
- ✅ No mostramos stacktraces crudos
- ✅ El servidor sigue funcionando
- ✅ Se limpia el estado del usuario
- ✅ Limpieza en el `finally` garantiza liberación de recursos

**Estado:** ✅ **CORRECTO, lista para implementar**

---

## 📊 TABLA FINAL: RESUMEN EJECUTIVO

### **DESGLOSE EXACTO DE REQUISITOS vs. IMPLEMENTACIÓN**

### **LO QUE SÍ IMPLEMENTA CORRECTAMENTE ✅**

| Requisito | Ubicación | Estado |
|-----------|-----------|--------|
| **Separación de Responsabilidades** | 7 clases enfocadas | ✅ Cumple |
| **Concurrencia: ExecutorService** | `Main.java` Servidor | ✅ Cumple |
| **Thread-Safety: ConcurrentHashMap** | `Usuarios.java` | ✅ Cumple |
| **Protocolo de Texto Plano** | `Protocolo.java` + BufferedReader/PrintWriter | ✅ Cumple |
| **Sin ObjectSerialization** | Uso de Strings y Mensaje.java | ✅ Cumple |
| **Manejo de Excepciones de Red** | Try-catch en ConexionServidor.java | ✅ Cumple |
| **Broadcasting de Mensajes** | `Usuarios.broadcast()` | ✅ Cumple (Diseño) |
| **Estructura de Capas** | Compartido / Servidor / Cliente | ✅ Cumple |

### **LO QUE ESTÁ A MEDIAS ⚠️**

| Requisito | Problema | Falta |
|-----------|----------|-------|
| **Cifrado SSL** | `UtilidadesSSL.java` vacía | Implementar SSLSocket O AES encryption |
| **Notificación de Conexión** | No hay broadcast al conectarse | Agregar notificación en login |
| **Mensaje "Ningún cliente"** | Solo se imprime al inicio | Detectar cuando cantidad() == 0 |
| **Cierre limpio del servidor** | No hay shutdown hook | Implementar `Runtime.addShutdownHook()` |
| **Cierre limpio del cliente** | No detecta "El servidor se desconectó" | Agregar lógica en `Listener.java` |

### **LO QUE FALTA POR IMPLEMENTAR ❌**

| Requisito | Ubicación | Acción |
|-----------|-----------|--------|
| **Gestión de Roles** | `Mensaje.java` + `Usuarios.java` + `Logica.java` | Agregar campo `rol` y validaciones |
| **Protocolo con Roles** | `Protocolo.java` | Actualizar formato: `TIPO\|nick\|rol\|contenido` |
| **Login del Cliente** | `ConexionCliente.java` | Implementar envío de LOGIN y espera de respuesta |
| **Procesamiento de Comandos** | `Logica.java` | Implementar `/bye`, `/list`, `/ping` |
| **Métodos de Usuarios** | `Usuarios.java` | Implementar: `agregar()`, `eliminar()`, `broadcast()`, `obtenerLista()`, `cantidad()` |

---

## 🎯 PRIORIDADES DE IMPLEMENTACIÓN RECOMENDADAS

**Fase 1 (Crítico - Nivel 1 Mínimo):**
1. Implementar `Mensaje.java` con todos los campos
2. Implementar `Protocolo.java` con serializar/deserializar
3. Implementar `Usuarios.java` con ConcurrentHashMap
4. Implementar `ConexionServidor.java` con run()
5. Implementar `ConexionCliente.java` con conexión básica
6. Implementar `Listener.java` con escucha
7. Implementar `Main.java` (ambos) con flujos básicos

**Fase 2 (Importante - Completar requisitos):**
1. ✅ Agregar campo `rol` a `Mensaje.java`
2. ✅ Actualizar protocolo con roles
3. ✅ Implementar `Logica.java` con comandos
4. ✅ Agregar notificación de conexión (broadcast)
5. ✅ Agregar detección de "último usuario"
6. ✅ Agregar shutdown hook en servidor

**Fase 3 (Endurecimiento - Robustez Extra):**
1. Implementar `UtilidadesSSL.java` (AES o SSLSocket)
2. Agregar manejo de servidor desconectado en cliente
3. Agregar validaciones adicionales de rol
4. Testing con múltiples clientes simultáneos



---

## 🚀 GUÍA DE IMPLEMENTACIÓN ORDENADA

### **Orden recomendado para implementar sin errores:**

#### **PASO 1: Definir el Protocolo**
```java
// Protocolo.java debe convertir entre String y Mensaje

// Formato: TIPO|REMITENTE|ROL|CONTENIDO
// Ejemplos:
// LOGIN|juan|USER|contraseña123
// MENSAJE|juan|USER|Hola a todos
// COMANDO|maria|USER|/list
// NOTIFICACION|servidor|SISTEMA|juan acaba de conectarse a este chat
```

#### **PASO 2: Implementar Mensaje.java**
```java
public class Mensaje {
    private String tipo;      // LOGIN, MENSAJE, COMANDO, NOTIFICACION, etc
    private String remitente; // Nickname del usuario
    private String rol;       // USER, ADMIN, GUEST
    private String contenido; // El texto/comando
    private long timestamp;   // Timestamp del mensaje
    
    // Constructor, getters, setters, toString()
}
```

#### **PASO 3: Implementar Usuarios.java**
```java
public class Usuarios {
    private ConcurrentHashMap<String, UsuarioInfo> usuariosConectados;
    
    public void agregar(String nickname, Socket socket, String rol) { /*...*/ }
    public void eliminar(String nickname) { /*...*/ }
    public void broadcast(String mensaje) { /*...*/ }
    public List<String> obtenerLista() { /*...*/ }
    public int cantidad() { /*...*/ }
    public UsuarioInfo obtener(String nickname) { /*...*/ }
}
```

#### **PASO 4: Implementar Logica.java**
```java
public class Logica {
    public String procesarComando(String comando, String rol) {
        switch(comando) {
            case "/list": return "RESPUESTA|servidor|USUARIO|" + obtenerLista();
            case "/ping": return "RESPUESTA|servidor|USUARIO|pong";
            case "/bye": return "COMANDO_ACEPTADO|servidor|USUARIO|Desconectando";
            default: return "ERROR|servidor|USUARIO|Comando no reconocido";
        }
    }
    
    public String procesarMensaje(Mensaje msg) { /*...*/ }
}
```

#### **PASO 5: Implementar ConexionServidor.java**
- Leer Login del cliente
- Agregar a Usuarios
- **Broadcast de notificación** (IMPORTANTE)
- Loop de lectura de mensajes
- Procesamiento con Logica
- Exception handling completo
- Finally: eliminar usuario y detectar si cantidad == 0

#### **PASO 6: Implementar ConexionCliente.java**
- Conectar al servidor
- Enviar LOGIN
- Esperar confirmación (BIENVENIDA)
- Método para enviar mensajes
- Iniciar Listener en Thread aparte

#### **PASO 7: Implementar Listener.java**
- Leer mensajes continuamente
- Detectar "El servidor se desconectó"
- Parsear mensajes según protocolo
- Imprimir en consola

#### **PASO 8: Implementar Main.java (Servidor)**
- ServerSocket con puerto variable
- ExecutorService con 10 hilos
- Loop accept() → ejecutar ConexionServidor
- **ShutdownHook** para Ctrl+C
- Imprimir "Ningún cliente conectado" al inicio

#### **PASO 9: Implementar Main.java (Cliente)**
- Solicitar IP, Puerto, Nickname
- Crear ConexionCliente
- Iniciar Listener en Thread
- Loop de lectura de input
- Procesar comandos (/bye, /list, /ping)
- Enviar mensajes

---

## ⚠️ PUNTOS CRÍTICOS A NO OLVIDAR

### **1. Notificación de Conexión (ENUNCIADO EXPLÍCITO)**
```
✅ CORRECTO: Cuando juan se conecta, TODOS (incluido juan) reciben:
"NOTIFICACION|servidor|SISTEMA|juan acaba de conectarse a este chat"

❌ INCORRECTO: Solo los otros usuarios lo reciben, juan no.
❌ INCORRECTO: No se envía la notificación de conexión.
```

### **2. Cierre Limpio del Servidor (ENUNCIADO EXPLÍCITO)**
```
✅ CORRECTO: Cuando Ctrl+C → Enviar "El servidor se desconectó" a TODOS → Esperar 500ms → Cerrar

❌ INCORRECTO: Cerrar sin avisar.
❌ INCORRECTO: Avisar pero no esperar a que se envíe.
❌ INCORRECTO: Avisar a algunos pero no a todos.
```

### **3. Detección de "Ningún cliente Conectado" (ENUNCIADO EXPLÍCITO)**
```
✅ CORRECTO: 
- Al inicio: "Ningún cliente conectado"
- Cuando se conecta juan: "Nuevo cliente..." (juan)
- Cuando se desconecta juan: "Ningún cliente conectado"

❌ INCORRECTO: Solo al inicio.
```

### **4. Gestión de Roles (ENUNCIADO EXPLÍCITO)**
```
✅ CORRECTO: El usuario tiene un rol (ADMIN, USER, GUEST)
El rol se envía en cada mensaje
La Logica valida permisos según rol

❌ INCORRECTO: No hay roles
❌ INCORRECTO: El rol existe pero nunca se usa
```

### **5. Cifrado SSL (ENUNCIADO EXPLÍCITO)**
```
✅ OPCIONES CORRECTAS:
A) Usar SSLSocket/SSLServerSocket de verdad
B) Implementar cifrado AES en UtilidadesSSL.java

❌ INCORRECTO: Dejar UtilidadesSSL.java vacía
```

---

## 📝 EJEMPLO COMPLETO DE FLUJO DE CONEXIÓN

```
1. SERVIDOR inicia: "Ningún cliente conectado"

2. CLIENTE ejecuta Main.java
   - Solicita IP: "localhost"
   - Solicita Puerto: "5000"
   - Solicita Nickname: "juan"

3. ConexionCliente.conectar():
   - socket = new Socket("localhost", 5000)
   - salida.println("LOGIN|juan|USER|contraseña")

4. SERVIDOR recibe conexión
   - ConexionServidor.run() inicia
   - entrada.readLine() → "LOGIN|juan|USER|contraseña"
   - Protocolo.deserializar() → Mensaje(LOGIN, juan, USER, contraseña)
   - usuarios.agregar("juan", socket, "USER")
   - System.out.println("> Nuevo cliente conectado (juan). Actualmente hay 1 usuarios conectados.")
   - usuarios.broadcast("NOTIFICACION|servidor|SISTEMA|juan acaba de conectarse a este chat")

5. CLIENTE recibe notificación
   - Listener.run() lee: "NOTIFICACION|servidor|SISTEMA|juan acaba de conectarse a este chat"
   - System.out.println("[NOTIFICACIÓN] juan acaba de conectarse a este chat")

6. CLIENTE env
   - Scanner: System.in.nextLine() → "/list"
   - ConexionCliente.enviar("COMANDO|juan|USER|/list")

7. SERVIDOR procesa comando
   - ConexionServidor.run() lee: "COMANDO|juan|USER|/list"
   - Logica.procesarComando("/list") → "RESPUESTA|servidor|USUARIO|Usuarios: juan"
   - usuarios.broadcast(respuesta)

8. CLIENTE recibe respuesta
   - Listener.run() lee: "RESPUESTA|servidor|USUARIO|Usuarios: juan"
   - System.out.println("Usuarios: juan")

9. CLIENTE escribe /bye
   - ConexionCliente.enviar("COMANDO|juan|USER|/bye")
   - System.exit(0)

10. SERVIDOR recibe /bye
    - Logica.procesarComando("/bye") → elimina a juan
    - usuarios.eliminar("juan")
    - if (usuarios.cantidad() == 0) System.out.println("Ningún cliente conectado")
    - ConexionServidor.run() termina

11. CLIENTE intenta reconectar pero no hay servidor
    - System.out.println("⚠️ Error: No se puede conectar al servidor")

12. SERVIDOR shutdown (Ctrl+C)
    - Runtime.getRuntime().addShutdownHook() se activa
    - apagarServidor():
        - usuarios.broadcast("NOTIFICACION|servidor|SISTEMA|El servidor se desconectó")
        - Thread.sleep(500)
        - executorService.shutdown()
        - serverSocket.close()
        - System.out.println("✅ Servidor apagado")

13. CLIENTES CONECTADOS reciben desconexión
    - Listener.run() lee: "NOTIFICACION|servidor|SISTEMA|El servidor se desconectó"
    - if (mensaje.contains("El servidor se desconectó")) socket.close(); System.exit(0);
```

---

## ✅ CHECKLIST FINAL DE CUMPLIMIENTO

Antes de presentar, verifica que:

- [ ] **Protocolo texto plano** definido en Protocolo.java
- [ ] **ConcurrentHashMap** en Usuarios.java (no HashMap)
- [ ] **ExecutorService** con newFixedThreadPool(10) en Main.java
- [ ] **Notificación de conexión** enviada al conectarse un cliente
- [ ] **Detección de "Ningún cliente"** cuando cantidad() == 0
- [ ] **Shutdown hook** implementado para Ctrl+C
- [ ] **Cierre limpio** envía "El servidor se desconectó" a todos
- [ ] **Comandos /bye, /list, /ping** implementados en Logica
- [ ] **Excepciones de red** capturadas sin stacktraces crudos
- [ ] **Campo rol** en Mensaje y validación en Logica
- [ ] **UtilidadesSSL** implementada (AES o SSLSocket)
- [ ] **Main.java Cliente** maneja "El servidor se desconectó"
