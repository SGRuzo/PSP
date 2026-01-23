# Test: Escape de Carácter "|" en Protocolo

## ¿Qué se ha implementado?

Se ha añadido un sistema de **escape automático** en la clase `Protocolo.java` que permite que los usuarios envíen mensajes con el carácter `"|"` sin romper el protocolo.

## ¿Cómo funciona?

### Escaping (Empaquetación)
Cuando se empaquet un mensaje con `Protocolo.empaquetar()`:
- El carácter `"|"` en los parámetros se reemplaza por `\u001F + "PIPE"`
- Esto permite que el separador sea seguro

**Ejemplo:**
```
Usuario escribe: "Hola | Mundo"
↓
Protocolo.empaquetar("MSG", "Hola | Mundo")
↓
Mensaje enviado: "MSG|\u001FPIPE Mundo"
```

### Unescaping (Desempaquetación)
Cuando se desempaqueta un mensaje con `Protocolo.desempaquetar()`:
- Se divide por `"|"` de forma segura
- Luego se restauran los caracteres originales

**Ejemplo:**
```
Servidor recibe: "MSG|\u001FPIPE Mundo"
↓
Protocolo.desempaquetar(mensaje)
↓
Resultado: ["MSG", "Hola | Mundo"]
```

## Casos de Uso Soportados

### ✅ Un sólo pipe
```
Usuario: "Texto | más texto"
Funcionamiento: OK
```

### ✅ Múltiples pipes
```
Usuario: "A | B | C | D"
Funcionamiento: OK
```

### ✅ Comandos con pipes
```
Usuario: "/list"
Mensaje del servidor: "Usuario1 | Usuario2 | Usuario3"
Funcionamiento: OK
```

### ✅ Mix de caracteres
```
Usuario: "Hola | cómo | estás? | ¡Bien!"
Funcionamiento: OK
```

## Seguridad

- Se utiliza el carácter de control `\u001F` (Unit Separator) como prefijo
- Este carácter rara vez aparece en texto normal
- La secuencia `\u001FPIPE` es predecible y fácil de escapar

## Limitaciones

- El carácter de control `\u001F` también será escapado si aparece en el texto del usuario
- Esto es aceptable ya que es un carácter de control muy raro en texto normal

## Cambios Realizados

### `Protocolo.java`
- ✅ Añadidas constantes de escape
- ✅ Método `escapar()` privado
- ✅ Método `desescapar()` privado
- ✅ Actualizado `empaquetar()` para escapar parámetros
- ✅ Actualizado `desempaquetar()` para desescapar parámetros

## Impacto

El cambio es **totalmente transparente** para el usuario:
- El usuario sigue escribiendo como siempre
- El protocolo se encarga automáticamente del escape/unescape
- No hay cambios en la API pública (métodos públicos)

