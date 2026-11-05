# Simulador de llaves de torneos de Jiu-Jitsu

Simulador de llaves de torneo de BJJ con categorías según la IBJJF (adultos y masters, con kimono).

## Requisitos

- Java 21 o superior
- Maven

## Abrir la aplicación

```bash
mvn javafx:run
```

Desde el IDE, ejecutar la clase `com.torneobjj.vista.Lanzador`.

En *Torneo → Cargar 60 competidores de prueba* se carga un torneo de ejemplo.

Desde el menú *Archivo* se crea un torneo nuevo (nombre y fecha), se guarda y se abre
(`Ctrl+N`, `Ctrl+S`, `Ctrl+O`). Los torneos se guardan como archivos `.torneo.json`.
Al cerrar con cambios sin guardar, el programa pregunta si se quieren guardar.

## Correr los tests

```bash
mvn test
```

## Estructura

| Paquete | Contenido |
|---|---|
| `modelo` | Participante, categorías de edad y peso, combate y regla de desempate |
| `llave` | Armado de llaves ronda por ronda y sorteo por categoría |
| `servicio` | `Torneo` (estado y validaciones) y generador de datos de prueba |
| `persistencia` | Guardar y abrir torneos en JSON |
| `vista` | Interfaz JavaFX: inscripción, pesaje, llaves y buscador |

## Estado

- [x] Paso 1: modelo y reglas (categorías de edad y peso, pesaje, combates y desempate)
- [x] Paso 2: sorteo de llaves (W.O., final directa, triangular de 3, eliminación con pase libre)
- [x] Paso 3: interfaz (JavaFX): inscripción, pesaje, llaves y buscador
- [x] Paso 4: carga y corrección de resultados (sumisión, puntos, ventajas, penalidades, decisión del árbitro)
- [x] Paso 5: nombre y fecha del torneo, guardar y abrir torneos (archivos `.torneo.json`)
