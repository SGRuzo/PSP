# Calculadora - Cliente/Servidor

### 1. **Inicio de la Comunicación**
- El Cliente inicia la comunicación enviando una petición al servidor
- La petición contiene:
    - n1: Primer número
    - operación: Tipo de operación matemática
    - n2: Segundo número

### 2. **Operaciones**

#### **OK**
```
Ejemplo:
{"operacion": "suma", "operandos": [5, 3]}

Proceso:
1. Recibe la petición
2. Realiza el cálculo (5 + 3 = 8)
3. Prepara la respuesta
4. Envía: {"resultado": 8}
```

#### **ERROR**
```
Ejemplo:
{"operacion": "division", "operandos": [5, 0]}

Proceso:
1. Recibe la petición
2. Detecta división por cero
3. Prepara respuesta de error
4. Envía: {"resultado": null, "error": "No se puede dividir entre 0"}
```

### 3. **Cierre de sesión**
- El cliente cierra la ventana (hace clic en la "X")
- El servidor envía mensaje de despedida:
```
{"¡Hasta pronto!"}
```
