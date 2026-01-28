# Batalla Urbana — Backend

Backend del juego web multijugador en tiempo real **Batalla Urbana**.

El servidor es **server-authoritative**: valida turnos, rondas y habilidades de los personajes, y emite los eventos a los clientes mediante **WebSockets (STOMP)**, garantizando la coherencia del estado de la partida.

---

## Tecnologías

- Java
- Spring Boot
- WebSockets (STOMP)
- Arquitectura Hexagonal

---

## Demo online

- Aplicación web: http://93.93.112.225

---

## Funcionalidades

- Gestión de salas y partidas multijugador
- Flujo de juego por turnos y rondas
- Habilidades de personajes y reglas de juego
- Comunicación en tiempo real mediante WebSockets
- Validación de acciones en servidor (no se permite jugar fuera de turno)
- Dominio desacoplado de la infraestructura (arquitectura hexagonal)

---

## Arquitectura

El proyecto sigue una **arquitectura hexagonal**, separando la lógica del juego del framework y la infraestructura.

- `domain/`  
  Contiene las reglas del juego, entidades y validaciones.
- `application/`  
  Casos de uso que orquestan la lógica del dominio.
- `in/`  
  Adaptadores de entrada (controladores WebSocket).
- `out/`  
  Adaptadores de salida (infraestructura).

Esta estructura facilita el mantenimiento, el testing y la evolución del proyecto.

---

## Ejecutar en local

### Requisitos

- Java
- Maven

### Pasos

```bash
git clone https://github.com/sauldm/batalla-urbana-back
cd batalla-urbana-back
mvn clean spring-boot:run
```

---

## WebSockets

- Comunicación en tiempo real mediante WebSockets usando STOMP
- El servidor recibe las acciones de los jugadores
- El backend valida turnos, rondas y habilidades
- El servidor emite eventos con el estado actualizado de la partida
- Los clientes se suscriben a topics para recibir los cambios en tiempo real

---

## Roadmap


- Registro e inicio de sesión
- Reconexión de jugadores en caso de pérdida de conexión
- Persistencia de partidas
- Observabilidad del sistema (logs y métricas)
- Escalado para múltiples partidas simultáneas

---

## Objetivo del backend

Este backend se ha desarrollado como parte de un portfolio profesional con el objetivo de demostrar:

- Dominio de Java y Spring Boot
- Uso de arquitectura hexagonal
- Gestión de lógica de negocio compleja en tiempo real
- Implementación de un servidor autoritativo para juegos multijugador

