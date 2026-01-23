# 💬 Aplicación de Chat Cliente-Servidor

Una aplicación de chat distribuida implementada en Java con arquitectura **MVC (Modelo-Vista-Controlador)**, comunicación mediante **Sockets TCP** y protocolo personalizado.

---

## 📁 Estructura del Proyecto

```
Chat/
├── cliente/                    # Módulo del cliente
│   ├── Servidor.java              # Punto de entrada
│   ├── Controller.java        # Controlador MVC
│   ├── Model.java             # Modelo (conexión Socket)
│   ├── View.java              # Vista (interfaz gráfica Swing)
│   ├── EscuchaServidor.java   # Hilo de escucha de mensajes
│   └── cliente.iml            # Configuración del módulo
├── servidor/                   # Módulo del servidor
│   ├── Servidor.java              # Punto de entrada
│   ├── ChatServer.java        # Servidor TCP principal
│   ├── GestorUsuario.java     # Gestor de usuarios conectados
│   ├── ManejadorCliente.java  # Manejador de cada cliente
│   └── servidor.iml           # Configuración del módulo
├── Compartido/                 # Clases compartidas
│   ├── Protocolo.java         # Protocolo de comunicación
│   ├── Mensaje.java           # Estructura de mensajes
│   ├── Config.java            # Configuración global
│   ├── Seguridad.java         # Funciones de seguridad
│   └── Compartido.iml         # Configuración del módulo
├── compile.sh                 # Script de compilación
├── compile_fixed.sh           # Script de compilación mejorado
└── README.md                  # Este archivo
```

---

## 🏗️ Arquitectura General

La aplicación sigue una arquitectura **Cliente-Servidor** con:

- **Múltiples clientes**: Pueden conectarse simultáneamente
- **Un servidor central**: Gestiona conexiones y distribuye mensajes
- **Protocolo personalizado**: Comunicación basada en comandos separados por `|`
- **Concurrencia**: Uso de hilos para manejar múltiples conexiones

```
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│  Cliente 1  │         │   Cliente 2 │         │  Cliente N  │
│  (MVC)      │         │   (MVC)     │         │  (MVC)      │
└──────┬──────┘         └──────┬──────┘         └──────┬──────┘
       │ Sockets TCP            │                      │
       └────────────────────────┼──────────────────────┘
                                │
                        ┌───────▼────────┐
                        │ Servidor Chat  │
                        │ (Pool 10 hilos)│
                        └────────────────┘
```

---

## 📦 MÓDULO CLIENTE (`cliente/`)

### Descripción General
El cliente es una **aplicación de escritorio** con interfaz gráfica Swing que implementa el patrón MVC para separar la lógica de presentación, datos y control.

### 🔧 Clases del Cliente

#### **1. Servidor.java**
**Punto de entrada de la aplicación del cliente**

**Responsabilidades:**
- Inicializar el logger
- Crear instancias de Model, View y Controller
- Configurar y ejecutar la aplicación en el EDT (Event Dispatch Thread)
- Capturar y mostrar errores fatales

**Métodos principales:**
- `main(String[] args)`: Método principal que inicia la aplicación

**Flujo:**
```
main() → SwingUtilities.invokeLater()
       → Crear Model
       → Crear View
       → Crear Controller
       → Mostrar interfaz gráfica
```

---

#### **2. Model.java**
**Encapsula la lógica de conexión y comunicación de red**

**Responsabilidades:**
- Gestionar la conexión Socket con el servidor
- Manejar los flujos de entrada/salida (PrintWriter/BufferedReader)
- Enviar comandos y mensajes al servidor
- Almacenar información del cliente (nombre de usuario, servidor, puerto)

**Atributos principales:**
```java
private Socket socket;              // Conexión TCP
private PrintWriter salida;         // Flujo de salida
private BufferedReader entrada;     // Flujo de entrada
private String nombreUsuario;       // Nombre del usuario
private boolean conectado;          // Estado de conexión
private String hostServidor;        // Host del servidor
private int puertoServidor;         // Puerto del servidor
```

**Métodos principales:**
- `conectar(String host, int puerto)`: Establece conexión con el servidor
- `desconectar()`: Cierra la conexión y libera recursos
- `enviarMensaje(String mensaje)`: Envía un mensaje de chat
- `enviarComando(String comando, String... params)`: Envía comandos al servidor
- `obtenerEntrada()`: Retorna el BufferedReader para leer del servidor
- `establecerNombreUsuario(String nombre)`: Establece el nombre del usuario

**Excepciones manejadas:**
- `IOException`: Errores de red y E/S

---

#### **3. View.java**
**Interfaz gráfica de usuario usando Swing**

**Responsabilidades:**
- Crear y gestionar todos los componentes visuales
- Mostrar mensajes al usuario
- Capturar entrada del usuario
- Mantener la interfaz sincronizada con el estado de la aplicación

**Componentes principales:**
```java
private JTextArea areaChat;         // Área de chat (solo lectura)
private JTextField campoEntrada;    // Campo de entrada de mensajes
private JButton btnEnviar;          // Botón para enviar mensajes
private JButton btnConectar;        // Botón para conectar
private JButton btnDesconectar;     // Botón para desconectar
private JButton btnComandoList;     // Botón para ver usuarios
private JButton btnComandoPing;     // Botón para enviar ping
private JLabel lblEstado;           // Etiqueta de estado
private JLabel lblUsuario;          // Etiqueta con nombre de usuario
```

**Métodos principales:**
- `mostrarMensaje(String mensaje)`: Muestra un mensaje en el área de chat
- `mostrarError(String titulo, String mensaje)`: Muestra un diálogo de error
- `solicitarDatosServidor()`: Abre diálogo para ingresar host y puerto
- `solicitarNombreUsuario()`: Abre diálogo para ingresar nombre de usuario
- `establecerEstado(boolean conectado)`: Actualiza el estado visual
- `establecerUsuario(String nombreUsuario)`: Muestra el nombre del usuario
- `obtenerTextoEntrada()`: Retorna el texto ingresado
- `limpiarEntrada()`: Limpia el campo de entrada

---

#### **4. Controller.java**
**Controlador que coordina Model y View (patrón MVC)**

**Responsabilidades:**
- Gestionar eventos de la interfaz gráfica
- Coordinar acciones entre Model y View
- Iniciar/detener hilos de escucha
- Actualizar la interfaz según el estado de la conexión

**Atributos principales:**
```java
private final View vista;           // Referencia a la vista
private final Model modelo;         // Referencia al modelo
private EscuchaServidor escuchaServidor;  // Hilo de escucha
private String nombreUsuario;       // Nombre del usuario conectado
private volatile boolean conectado; // Estado de conexión
```

**Métodos principales:**
- `accionConectar()`: Gestiona el evento de conectar al servidor
- `accionDesconectar()`: Gestiona el evento de desconectar
- `accionEnviarMensaje()`: Gestiona el envío de mensajes
- `accionComandoList()`: Envía comando LIST para ver usuarios
- `accionComandoPing()`: Envía comando PING
- `conectarAlServidor(String host, int puerto, String usuario)`: Realiza la conexión
- `desconectar()`: Limpia recursos y actualiza interfaz
- `mostrarMensaje(String mensaje)`: Actualiza la vista desde el hilo de escucha
- `procesarError(String titulo, String mensaje)`: Muestra errores en la UI
- `procesarDesconexion()`: Maneja la desconexión iniciada por el servidor

**Flujo de Conexión:**
```
1. Usuario presiona "Conectar"
2. accionConectar() solicita host, puerto y nombre de usuario
3. conectarAlServidor() crea conexión Socket
4. Envía comando LOGIN con el nombre de usuario
5. Inicia hilo EscuchaServidor para recibir mensajes
6. Vista se actualiza a estado "conectado"
```

---

#### **5. EscuchaServidor.java**
**Hilo secundario que escucha mensajes del servidor**

**Responsabilidades:**
- Leer continuamente del socket sin bloquear la UI
- Procesar mensajes según el protocolo
- Actualizar la vista mediante SwingUtilities
- Manejar desconexión del servidor

**Atributos principales:**
```java
private final BufferedReader entrada;    // Flujo de entrada del socket
private final Controller controlador;     // Referencia al controlador
private volatile boolean escuchando;     // Flag para detener el hilo
```

**Métodos principales:**
- `run()`: Bucle principal que lee del servidor
- `procesarMensaje(String mensaje)`: Procesa mensajes según tipo
- `detener()`: Detiene el hilo de escucha de forma segura

**Tipos de mensajes procesados:**
- `MSG|usuario|contenido`: Mensaje de otro usuario
- `OK`: Confirmación del servidor
- `ERROR|motivo`: Mensaje de error
- `LIST|usuario1,usuario2,...`: Lista de usuarios conectados
- `PONG`: Respuesta a PING

---

## 🖥️ MÓDULO SERVIDOR (`servidor/`)

### Descripción General
El servidor es una **aplicación de consola** que acepta múltiples conexiones de clientes simultáneamente mediante un pool de hilos.

### 🔧 Clases del Servidor

#### **1. Servidor.java**
**Punto de entrada de la aplicación servidor**

**Responsabilidades:**
- Solicitar el puerto de escucha al usuario
- Validar el puerto ingresado
- Crear instancia del servidor
- Mantener el servidor ejecutándose

**Métodos principales:**
- `main(String[] args)`: Punto de entrada
- `solicitarPuerto(Scanner scanner)`: Solicita y valida el puerto

**Validaciones:**
- Puerto entre 1024 y 65535
- Puerto por defecto: 5000
- Si está ocupado, genera error

---

#### **2. ChatServer.java**
**Servidor TCP que gestiona múltiples conexiones**

**Responsabilidades:**
- Crear ServerSocket en el puerto especificado
- Aceptar conexiones de clientes
- Crear ManejadorCliente para cada conexión
- Gestionar pool de hilos (máximo 10 conexiones simultáneas)
- Realizar limpieza de recursos al apagar

**Atributos principales:**
```java
private final int puerto;                    // Puerto de escucha
private ServerSocket serverSocket;           // Socket servidor
private ExecutorService poolHilos;           // Pool de 10 hilos
private volatile boolean ejecutandose;       // Flag de ejecución
private int contadorConexiones;              // Contador de conexiones
private final GestorUsuario gestorUsuarios;  // Gestor de usuarios
```

**Métodos principales:**
- `run()`: Método principal del servidor (Runnable)
- `iniciarServidor()`: Crea el ServerSocket
- `aceptarConexiones()`: Bucle que acepta conexiones
- `detener()`: Apaga el servidor de forma segura
- `crearManejadorCliente(Socket socket)`: Crea handler para nueva conexión

**Flujo del servidor:**
```
1. Crear ServerSocket
2. Iniciar pool de 10 hilos
3. Bucle infinito:
   a. Aceptar nueva conexión Socket
   b. Crear ManejadorCliente
   c. Ejecutar en pool de hilos
4. Al apagar: cerrar sockets y pool
```

---

#### **3. GestorUsuario.java**
**Gestor de usuarios y lista de contactos conectados**

**Responsabilidades:**
- Mantener lista de usuarios conectados
- Agregar/eliminar usuarios de la lista
- Obtener lista de usuarios conectados
- Validar unicidad de nombres de usuario

**Métodos principales:**
- `agregarUsuario(String nombre, ManejadorCliente handler)`: Registra nuevo usuario
- `eliminarUsuario(String nombre)`: Desconecta un usuario
- `obtenerLista()`: Retorna lista de usuarios conectados
- `existe(String nombre)`: Verifica si un usuario ya existe
- `obtenerManejador(String nombre)`: Obtiene el handler de un usuario

**Estructura interna:**
```java
private ConcurrentHashMap<String, ManejadorCliente> usuarios;
// Mapa thread-safe de nombre → handler
```

---

#### **4. ManejadorCliente.java**
**Manejador de la conexión de un cliente (ejecutado en hilo)**

**Responsabilidades:**
- Leer comandos del cliente
- Procesar comandos (LOGIN, MSG, BYE, LIST, PING)
- Enviar respuestas al cliente
- Distribuir mensajes a otros usuarios
- Gestionar desconexión del cliente

**Atributos principales:**
```java
private final Socket socket;                 // Socket del cliente
private final GestorUsuario gestorUsuarios;  // Referencia al gestor
private final int idConexion;                // ID único de conexión
private String nombreUsuario;                // Nombre del usuario
private PrintWriter salida;                  // Flujo de salida
private BufferedReader entrada;              // Flujo de entrada
```

**Métodos principales:**
- `run()`: Bucle principal de lectura de comandos
- `procesarComando(String comando, String[] partes)`: Procesa comando recibido
- `procesarLOGIN(String[] partes)`: Maneja comando LOGIN
- `procesarMSG(String[] partes)`: Maneja comando MSG
- `procesarBYE()`: Maneja comando BYE (desconexión)
- `procesarLIST()`: Maneja comando LIST
- `procesarPING()`: Maneja comando PING
- `enviarMensaje(String tipo, String contenido)`: Envía mensaje al cliente
- `distribuirMensaje(String remitente, String contenido)`: Distribuye a todos

**Flujo de procesamiento:**
```
1. Recibir comando del cliente
2. Desempaquetar usando Protocolo
3. Verificar tipo de comando
4. Ejecutar acción correspondiente
5. Responder al cliente o distribuir
```

---

## 🔀 MÓDULO COMPARTIDO (`Compartido/`)

### Descripción General
Contiene clases compartidas entre cliente y servidor para garantizar compatibilidad en la comunicación.

### 🔧 Clases Compartidas

#### **1. Protocolo.java**
**Define el protocolo de comunicación entre cliente y servidor**

**Constantes de comandos:**
```java
LOGIN      // Autenticación de usuario
MSG        // Mensaje de chat
BYE        // Desconexión
LIST       // Solicitar lista de usuarios
PING       // Solicitar latencia
OK         // Respuesta exitosa
ERROR      // Respuesta de error
```

**Separador:** `|` (pipe)

**Métodos principales:**
- `empaquetar(String comando, String... params)`: Crea mensaje con formato
  ```
  Ejemplo: Protocolo.empaquetar("MSG", "usuario", "Hola")
  Resultado: "MSG|usuario|Hola"
  ```

- `desempaquetar(String mensaje)`: Divide mensaje en componentes
  ```
  Ejemplo: Protocolo.desempaquetar("MSG|usuario|Hola")
  Resultado: ["MSG", "usuario", "Hola"]
  ```

**Formato de mensajes:**
```
LOGIN|usuario
MSG|usuario|contenido_mensaje
BYE
LIST
PING
OK
ERROR|motivo
```

---

#### **2. Mensaje.java**
**Clase POJO para representar un mensaje**

**Responsabilidades:**
- Encapsular datos de un mensaje
- Generar timestamp automático
- Facilitar serialización/deserialización (GSON ready)

**Atributos:**
```java
private String remitente;       // Usuario que envía
private String contenido;       // Cuerpo del mensaje
private String tipoComando;     // LOGIN, MSG, BYE, LIST, PING
private String timestamp;       // Marca temporal
```

**Métodos principales:**
- `Mensaje()`: Constructor vacío
- `Mensaje(String remitente, String contenido, String tipoComando)`: Constructor con parámetros
- Getters y setters para todos los atributos
- `obtenerTimestamp()`: Genera timestamp actual con formato
- `toString()`: Representación en texto

**Formato timestamp:** `HH:mm:ss dd/MM/yyyy`

---

#### **3. Config.java**
**Configuración global de la aplicación**

**Responsabilidades:**
- Centralizar constantes de configuración
- Facilitar cambios sin modificar código

**Constantes principales:**
```java
HOST_DEFECTO        // "localhost"
PUERTO_DEFECTO      // 5000
PUERTO_MINIMO       // 1024
PUERTO_MAXIMO       // 65535
NUMERO_HILOS        // 10
TIMEOUT_SOCKET      // Timeout de socket
```

---

#### **4. Seguridad.java**
**Funciones de seguridad y validación**

**Responsabilidades:**
- Validar entrada de usuarios
- Sanitizar mensajes
- Verificar credenciales
- Prevenir inyección de comandos

**Métodos principales:**
- `validarNombreUsuario(String nombre)`: Valida formato del usuario
- `sanitizarMensaje(String mensaje)`: Limpia caracteres especiales
- `validarPuerto(int puerto)`: Verifica rango válido de puerto
- `encriptarPassword(String password)`: (Opcional) Encriptación

---

## 🔌 PROTOCOLO DE COMUNICACIÓN

### Formato General
```
COMANDO|param1|param2|...|paramN
```

### Comandos Disponibles

#### **LOGIN** - Autenticación de usuario
```
Cliente → Servidor:  LOGIN|juan
Servidor → Cliente:  OK
                 o   ERROR|Usuario ya existe
```

#### **MSG** - Envío de mensaje
```
Cliente → Servidor:  MSG|Hola a todos
Servidor → Otros:    MSG|juan|Hola a todos
```

#### **LIST** - Solicitar usuarios conectados
```
Cliente → Servidor:  LIST
Servidor → Cliente:  LIST|juan,pedro,maria
```

#### **PING** - Verificar latencia
```
Cliente → Servidor:  PING
Servidor → Cliente:  PONG
```

#### **BYE** - Desconexión
```
Cliente → Servidor:  BYE
Servidor → Cliente:  OK
```

---

## 🚀 COMPILACIÓN Y EJECUCIÓN

### Compilar
```bash
# Compilar todo
./compile_fixed.sh

# O manualmente
javac -d bin/cliente cliente/*.java
javac -d bin/servidor servidor/*.java
javac -d bin/compartido Compartido/*.java
```

### Ejecutar Servidor
```bash
java -cp bin/servidor:bin/compartido servidor.Servidor
# Ingrese puerto cuando se solicite (ej: 5000)
```

### Ejecutar Cliente
```bash
java -cp bin/cliente:bin/compartido cliente.Servidor
# Se abrirá ventana gráfica
# Ingrese host (localhost) y puerto (5000)
# Ingrese nombre de usuario
```

---

## 📋 FLUJO DE EJECUCIÓN

### 1️⃣ Inicio del Servidor
```
Servidor.main()
  → Solicitar puerto (5000)
  → new ChatServer(5000)
  → new Thread(servidor).start()
  → serverSocket = new ServerSocket(5000)
  → Bucle aceptarConexiones()
```

### 2️⃣ Inicio de Cliente
```
Servidor.main()
  → new Model()
  → new View()
  → new Controller(vista, modelo)
  → Vista.mostrarVentana()
  → Esperando usuario
```

### 3️⃣ Conexión de Cliente
```
Usuario presiona "Conectar"
  → Controller.accionConectar()
  → Solicita host, puerto, nombre
  → Model.conectar(host, puerto)
  → Socket = new Socket(host, puerto)
  → PrintWriter/BufferedReader
  → Enviar LOGIN|nombreUsuario
  → Iniciar EscuchaServidor (hilo)
```

### 4️⃣ En Servidor (Lado servidor)
```
ServerSocket.accept() → Nueva conexión
  → new ManejadorCliente(socket)
  → pool.execute(manejador)
  → manejador.run()
  → Lee "LOGIN|nombreUsuario"
  → gestorUsuarios.agregarUsuario()
  → GestorUsuario notifica a otros
```

### 5️⃣ Envío de Mensaje
```
Usuario escribe: "Hola"
  → Presiona "Enviar"
  → Controller.accionEnviarMensaje()
  → Model.enviarMensaje("Hola")
  → PrintWriter.println("MSG|Hola")
  
En servidor:
  → ManejadorCliente.procesarMSG()
  → Distribuir a todos excepto remitente
  → "MSG|usuario|Hola"

En otros clientes:
  → EscuchaServidor.run() lee mensaje
  → procesarMensaje()
  → Controller.mostrarMensaje()
  → Vista.areaChat.append()
```

### 6️⃣ Desconexión
```
Usuario presiona "Desconectar"
  → Controller.accionDesconectar()
  → Enviar BYE
  → EscuchaServidor.detener()
  → Model.desconectar()
  → Socket.close()
  → Vista.establecerEstado(false)
```

---

## 🎯 CARACTERÍSTICAS PRINCIPALES

✅ **Múltiples conexiones simultáneas** (hasta 10)  
✅ **Chat en tiempo real**  
✅ **Interfaz gráfica intuitiva (Swing)**  
✅ **Protocolo personalizado y seguro**  
✅ **Manejo robusto de errores**  
✅ **Logging detallado**  
✅ **Thread-safe (ConcurrentHashMap)**  
✅ **Comandos: /list, /ping, /bye**  
✅ **Desconexión segura de recursos**  
✅ **Timestamps en mensajes**  

---

## 🔒 SEGURIDAD

- ✅ Validación de nombres de usuario
- ✅ Prevención de usuarios duplicados
- ✅ Thread-safe con colecciones sincronizadas
- ✅ Manejo seguro de recursos (try-finally)
- ✅ Cierre de sockets al desconectar
- ✅ Logging de eventos y errores

---

## 🐛 MANEJO DE ERRORES

| Error | Acción |
|-------|--------|
| Puerto inválido | Mostrar diálogo y solicitar de nuevo |
| Conexión rechazada | Mostrar error de conexión |
| Usuario duplicado | Rechazar login con ERROR |
| Socket desconectado | Notificar desconexión |
| E/S durante mensaje | Mostrar error y permitir reintentar |

---

## 📝 NOTAS DE DESARROLLO

- **Logger**: Configurado para todas las clases, ver logs en consola
- **Pool de hilos**: Máximo 10 conexiones simultáneas
- **EDT**: Todos los cambios en UI se hacen via `SwingUtilities.invokeLater()`
- **Synchronized**: Usado donde es necesario para thread-safety
- **Volatile**: Variables de bandera (conectado, escuchando, ejecutándose)

---

## 👨‍💻 Autor
Sistema de Chat - Aplicación PSP (Programación de Servicios y Procesos)

**Fecha:** 2026  
**Versión:** 1.0

---

## 📚 Referencias

- **Sockets TCP**: `java.net.Socket`, `java.net.ServerSocket`
- **Threading**: `java.util.concurrent.ExecutorService`, `Thread`
- **Swing**: `javax.swing.*`
- **Logging**: `java.util.logging.*`
- **Colecciones thread-safe**: `java.util.concurrent.ConcurrentHashMap`

---
analiza el codigo al completo. siguiendo el siguiente enunciado: que está completo al 100%? que está a medias o le falta algo? que falta por completo? que crees que hay que corregir?  sobra algo que no se especifica en el ennciado?

genera un unico y exclusivo readme en el que expliques que esta compelto, que falta a medias, que sobra, que falta del todo y que crees que hay que corregir? tienes completamente prohibido crear más de un readme y completamente prohibido tocar los archivos actuales



1. Arquitectura y Gestión Técnica    
   ●​ Concurrencia: Servidor multicliente que gestione hasta 10 conexiones    
   simultáneas mediante un Pool de Hilos (ExecutorService).    
   ●​ Sincronización: Se aconseja el uso de colecciones thread-safe de    
   java.util.concurrent para evitar problemas de concurrencia durante el    
   broadcast.    
   ●​ Separación de Responsabilidades (No "Código Espagueti"): Está    
   terminantemente prohibido tener toda la lógica en una megaclase o    
   megafunción. Se debe separar la lógica de red (sockets), la gestión de hilos y    
   la lógica de negocio (procesamiento de mensajes/comandos).
2. Comportamiento del Servidor    
   ●​ Inicio: Al arrancar, solicitará el puerto por el que se establecerá la conexión.    
   Mientras no haya nadie, mostrará: "Ningún cliente conectado".    
   ●​ Gestión de Conexiones:    
   ○​ Cada vez que se conecte un cliente, mostrará por pantalla: "> Nuevo    
   cliente conectado (nickname). Actualmente hay X usuarios    
   conectados".    
   ○​ Si un cliente se desconecta y no queda nadie, volverá a mostrar:    
   "Ningún cliente conectado".    
   ●​ Broadcasting: Mostrará por consola todos los mensajes recibidos e indicará    
   "nickname: mensaje...". Automáticamente, reenviará dicho mensaje a    
   todos los clientes conectados con ese mismo formato.    
   ●​ Cierre del Sistema: Si el servidor se cierra, todos los clientes deben cerrar    
   adecuadamente sus conexiones tras recibir el mensaje: "El servidor se    
   desconectó".3. Comportamiento del Cliente    
   ●​ Configuración Inicial: Al arrancar, solicitará la IP, el puerto y el nickname del    
   usuario. Solo entonces establecerá la conexión.    
   ●​ Interfaz de Chat:    
   ○​ Una vez conectado, mostrará al usuario: "Conectado a la sala de chat".    
   ○​ Notificará al resto de participantes: "nickname acaba de conectarse a    
   este chat".    
   ○​ Mostrará en tiempo real los mensajes recibidos del resto con el    
   formato nickname: mensaje.    
   ●​ Comandos Obligatorios:    
   ○​ /bye: Cierra la conexión de forma limpia, sale del programa y notifica    
   al resto: "nickname dejó este chat".    
   ○​ /list: Muestra la lista de usuarios conectados en ese momento.    
   ○​ /ping: El servidor debe responder "pong" para verificar la    
   latencia/conexión.
4. Robustez y Errores    
   ●​ Se deberán controlar las excepciones de red (desconexiones bruscas, puertos    
   ocupados, etc.) y mostrar los correspondientes mensajes de error de manera    
   controlada, evitando que el programa "explote" (stacktrace crudo).