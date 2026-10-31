# Diagnóstico técnico y refactorización — Vet-Care

**Asignatura:** Ingeniería de Software II
**Fecha de revisión:** 30 de octubre de 2026
**Integrantes:** Jefferson Rubio Rincón · Miguel Angel Villegas
**Repositorio:** https://github.com/MAngel4311/vetcare-gestion-mascotas.git — **Rama:** `<completar>`
**Commit de refactorización:** `<completar>` — *refactor: separar responsabilidades del horario de atencion*

---

## 1. Contexto y alcance

Vet-Care es una aplicación para manejar una clínica veterinaria: los dueños, las mascotas, los veterinarios, las citas y los registros clínicos. El backend está hecho con Spring Boot 3.5, Java 25 y PostgreSQL, y el frontend con React y Vite.

Para este taller escogimos la funcionalidad **programar una cita** (`POST /api/citas`), porque es donde se juntan más clases del sistema: `CitaController`, `CitaService`, `DisponibilidadService`, `CitaRepository`, `NotificacionService` y las entidades `Mascota`, `Veterinario`, `Cita` y `Dueno`.

**Alcance.** Se revisó el código del backend de esa funcionalidad. No se revisó el frontend ni el rendimiento.

## 2. Arquitectura observada

El proyecto es un **monolito en capas**: `controller → service → repository → model`. Usa DTO para lo que entra y sale, un manejador global de errores (`GlobalExceptionHandler`) y autenticación con JWT. Todo se despliega como una sola aplicación.

| Capa | Responsabilidad |
| --- | --- |
| `controller` | Recibe la petición HTTP y llama al servicio |
| `service` | Reglas de negocio (`CitaService`, `DisponibilidadService`, `NotificacionService`, …) |
| `repository` | Acceso a la base de datos |
| `model` | Entidades |

**Flujo de programar una cita.** `CitaController` recibe la petición y llama a `CitaService.crear`. Este valida el horario y la disponibilidad (con `DisponibilidadService`), guarda la cita con `CitaRepository` y por último publica un evento que envía el correo de confirmación.

**Lo que está bien:** las capas están separadas, se usan DTO, los errores se manejan en un solo lugar, el correo no frena la creación de la cita y hay un bloqueo para que no se crucen dos citas.
**Lo que se puede mejorar:** hay reglas de negocio repetidas entre servicios y el texto de los correos está repartido en varias clases (por eso H-01 y H-02).

## 3. Hallazgos técnicos

### H-01 · El horario de atención está duplicado
- **Clasificación:** deuda técnica.
- **Ubicación:** `CitaService.java` (líneas 53-57 y 211-231) y `DisponibilidadService.java` (líneas 33-51).
- **Evidencia:** las dos clases leen las mismas claves de configuración (`vetcare.agenda.hora-apertura` y `hora-cierre`), pero cada una las trata a su manera. `DisponibilidadService` las convierte una sola vez y revisa que el cierre sea después de la apertura. `CitaService` las guarda como texto, las convierte cada vez que las usa y no hace esa revisión.
- **Riesgo:** una misma regla del negocio está en dos lugares y con el tiempo pueden quedar diferentes. Además, no se puede probar sin Spring.
- **Principio afectado:** DRY, cohesión y responsabilidad única.
- **Estado:** intervenido y verificado.

### H-02 · El texto de los correos se arma en seis clases
- **Clasificación:** deuda técnica.
- **Ubicación:** `CitaService.avisar` (185-195), `NotificacionService.enviarConfirmacion` (98-108), `RecordatorioCitaService` (56), `RecordatorioVacunaService` (82), `DuenoService` (75), `VeterinarioService` (80).
- **Evidencia:** cada clase redacta su propio saludo, datos y despedida; además hay dos mecanismos distintos (evento y texto ya armado) para lo mismo.
- **Riesgo:** cambiar el formato de los avisos obliga a tocar seis clases.
- **Estado:** identificado, no intervenido.

### H-03 · Un veterinario puede registrar la consulta de una cita ajena
- **Clasificación:** defecto funcional (autorización).
- **Ubicación:** `RegistroClinicoService.registrar` (líneas 39-66).
- **Evidencia:** busca la cita por id y la procesa sin comprobar que sea del veterinario que hace la petición; `historial` y `CitaService.obtener` sí lo comprueban.
- **Riesgo:** escribir en la historia clínica de otro profesional con un id ajeno.
- **Estado:** identificado, no intervenido (corregirlo cambia el comportamiento, así que no es una refactorización).

## 4. Priorización

Escala de 1 (bajo) a 3 (alto). Son valores propuestos por el equipo, no métricas objetivas.

| Hallazgo | Impacto | Riesgo | Esfuerzo | Decisión |
| --- | --- | --- | --- | --- |
| H-01 | 2 | 2 | 1 | **Seleccionado para refactorizar** |
| H-02 | 2 | 1 | 3 | Pendiente: esfuerzo alto |
| H-03 | 3 | 2 | 1 | Pendiente: cambia el comportamiento |

**Justificación.** Escogimos H-01 porque es un cambio pequeño que toca pocos archivos y no cambia lo que ve el usuario. Además, la regla se puede probar sola, sin necesitar la base de datos. H-03 es más grave, pero es un defecto y se corrige aparte, no es un simple reordenamiento del código. H-02 pide tocar seis clases, por eso lo dejamos para después.

## 5. Intervención realizada (H-01)

### 5.1 Problema y objetivo
La regla del horario (lunes a sábado, de 08:00 a 18:00) estaba repetida en dos clases, y además `CitaService` la mezclaba con el flujo de la cita. **Objetivo:** dejarla en un solo lugar sin cambiar nada de cómo funciona el sistema.

### 5.2 Archivos intervenidos

| Archivo | Cambio |
| --- | --- |
| `service/HorarioAtencion.java` | Nuevo: lee la configuración y aplica la regla |
| `service/CitaService.java` | Se quita la lectura del horario y el cuerpo de `validarHorario`; delega en `HorarioAtencion` |
| `service/DisponibilidadService.java` | El constructor recibe `HorarioAtencion` en vez de dos textos |
| `src/test/.../HorarioAtencionTest.java` | Nuevo: 9 pruebas de la regla |

### 5.3 Antes y después

```
ANTES
CitaService
  ├─ lee apertura y cierre como texto (@Value)
  └─ validarHorario(): aplica las 5 reglas
DisponibilidadService
  └─ lee apertura y cierre por su cuenta (@Value)

DESPUÉS
HorarioAtencion        ← único lugar donde se lee el horario y se aplica la regla
  ├─ apertura(), cierre()
  └─ validarCita(inicio, fin, ahora)
CitaService            → delega la validación
DisponibilidadService  → pide apertura y cierre a HorarioAtencion
```

### 5.4 Fragmento representativo

```java
// CitaService
private void validarHorario(LocalDateTime inicio, LocalDateTime fin) {
    horario.validarCita(inicio, fin, LocalDateTime.now(ZONA));
}
```

La hora actual se pasa como parámetro (`ahora`) para poder probar la regla sin depender del reloj. Los mensajes de error y el orden en que se revisan son los mismos de antes.

### 5.5 Justificación técnica
- **Cohesión y responsabilidad única:** `CitaService` se queda solo con el flujo de la cita, y el horario queda en su propia clase.
- **DRY:** ahora hay una sola fuente para la configuración y para la regla.
- **Arquitectura:** las capas siguen igual. `HorarioAtencion` está en la capa de servicios y no se tocaron los controllers, los repositorios, la base de datos ni la API. Lo único que se pagó fue una clase más.
- **A futuro:** si el horario cambia (por ejemplo por festivos o por el horario de cada veterinario), solo hay que cambiar un lugar.

## 6. Verificación

### 6.1 Estrategia
Lo que hicimos fue comparar el código viejo con el nuevo usando muchos casos inventados, y además agregar pruebas unitarias para la regla.

### 6.2 Resultados

| Prueba | Comando | Resultado |
| --- | --- | --- |
| Comparación antes y después (500 casos al azar) | ver `evidencias/harness_diferencial/LEEME.md` | **0 diferencias** |
| Pruebas unitarias de la regla (9 casos) | `mvn test -Dtest=HorarioAtencionTest` | `<completar>` |
| Compilación completa | `mvn -q -DskipTests compile` | `<completar>` |
| Pruebas funcionales (171 casos) | arrancar el backend y ejecutar `java ProbarVetCare.java` | `<completar>` |

En la comparación aparecieron los seis resultados posibles de la regla (cita válida, fin antes del inicio, fecha pasada, cruza de día, domingo y fuera de horario). Una configuración inválida (cierre antes de la apertura) sigue haciendo fallar el arranque. Detalle: `docs/refactorizacion/evidencias/resultado_diferencial.txt`.

### 6.3 Limitaciones
La comparación solo prueba la regla, no toda la aplicación. Tampoco se midió la cobertura del código ni se hicieron pruebas con el frontend.

## 7. Conclusiones y lecciones aprendidas

- La regla del horario ahora está en un solo lugar, se puede probar sin Spring y el sistema se comporta igual que antes.
- H-02 y H-03 siguen pendientes. Quedaron documentados, pero no los corregimos.
- Aprendimos que una refactorización se hace en pasos pequeños y comprobando que nada cambie para el usuario.
- **Práctica que habría detectado estos problemas antes:** la revisión de código entre compañeros (la duplicación se nota al comparar los dos servicios), las pruebas automáticas, el análisis estático y una revisión de cada sprint con una lista de chequeo de las reglas del negocio.

## 8. Preguntas de la refactorización

### Pregunta 1. ¿Cuál era el problema identificado y dónde estaba localizado?
**Respuesta:** el problema era que la regla del horario de atención (lunes a sábado, de 08:00 a 18:00) estaba repetida en dos clases: `CitaService.java` (líneas 53-57 y 211-231) y `DisponibilidadService.java` (líneas 33-51). Además, `CitaService` tenía esa regla mezclada con el flujo de la cita, y por eso no se podía probar sola.

### Pregunta 2. ¿Qué principio de diseño o atributo de calidad estaba comprometido?
**Respuesta:**
- **Responsabilidad única y cohesión:** `CitaService` se encargaba del flujo de la cita y también de las reglas del horario, o sea que hacía dos cosas.
- **No repetirse (DRY):** la misma regla estaba en dos lugares y cada uno la manejaba distinto (uno revisaba que el cierre fuera después de la apertura y el otro no).
- **Atributos de calidad:** mantenibilidad y facilidad para probar el código.

### Pregunta 3. ¿Qué modificación realizaron y cómo cambiaron las responsabilidades o dependencias?
**Respuesta:** creamos la clase `HorarioAtencion`, que guarda el horario y tiene la regla en el método `validarCita(inicio, fin, ahora)`.
- **Antes:** `CitaService` tenía el flujo de la cita, la regla y la lectura de la configuración. `DisponibilidadService` leía la configuración por su cuenta.
- **Después:** `CitaService` solo se ocupa del flujo de la cita y le pasa la regla a `HorarioAtencion`. `DisponibilidadService` le pide la apertura y el cierre. `HorarioAtencion` es el único lugar que lee la configuración y aplica la regla.
- **Dependencias:** las dos clases ahora dependen de `HorarioAtencion` en vez de leer la configuración directamente. No se crearon dependencias nuevas con otras capas.

### Pregunta 4. ¿Qué consecuencias tiene el cambio sobre la arquitectura de la aplicación?
**Respuesta:** la arquitectura en capas (controller, service, repository) sigue igual. `HorarioAtencion` está en la capa de servicios y no se tocaron los controllers, los repositorios, la base de datos ni la API. Lo que mejora es que la regla ahora se puede probar sola, sin Spring ni base de datos, y que si el horario cambia (por festivos o por el horario de cada veterinario) solo hay que cambiar un lugar. Lo que cuesta es una clase más y un paso extra para llegar a la regla.

### Pregunta 5. ¿Qué evidencias demuestran que la funcionalidad conserva su comportamiento?
**Respuesta:**
- Probamos el código viejo y el nuevo con 500 fechas y horas inventadas al azar. En todas respondieron igual y salieron los seis resultados posibles de la regla (`docs/refactorizacion/evidencias/resultado_diferencial.txt`).
- Los mensajes de error y el orden en que salen son los mismos de antes.
- Si la configuración está mal (cierre antes de la apertura), la aplicación sigue fallando al arrancar.
- Hicimos nueve pruebas unitarias (`HorarioAtencionTest`) que dejan fijo cómo funciona la regla.

### Pregunta de cierre. ¿Qué práctica de desarrollo habría detectado estos problemas antes?
**Respuesta:** habrían ayudado la revisión de código entre compañeros, las pruebas automáticas en integración continua, el análisis estático y una revisión al final de cada sprint con una lista de chequeo.

## 9. Trazabilidad Git

| Commit | Mensaje | Hash |
| --- | --- | --- |
| Diagnóstico | `docs: registrar diagnostico tecnico` | `<completar>` |
| Refactorización | `refactor: separar responsabilidades del horario de atencion` | `<completar>` |
| Verificación | `docs: documentar verificacion de refactorizacion` | `<completar>` |
