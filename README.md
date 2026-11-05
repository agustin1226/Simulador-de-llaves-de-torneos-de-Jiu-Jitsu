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

En *Opciones → Cargar 60 competidores de prueba* se carga un torneo de ejemplo.

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
| `vista` | Interfaz JavaFX: inscripción, pesaje, llaves y buscador |

## Estado

- [x] Paso 1: modelo y reglas (categorías de edad y peso, pesaje, combates y desempate)
- [x] Paso 2: sorteo de llaves (W.O., final directa, triangular de 3, eliminación con pase libre)
- [x] Paso 3: interfaz (JavaFX): inscripción, pesaje, llaves y buscador
- [ ] Paso 4: carga de resultados
- [ ] Paso 5: guardar/cargar torneo
