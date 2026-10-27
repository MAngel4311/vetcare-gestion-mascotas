# Taller de seguimiento 5

**Ingeniería de Software II** · Universidad del Quindío, Facultad de Ingeniería  
**Ingeniería en Sistemas y Computación Nocturna** · Armenia, Quindío, 2026

## Integrantes

| Nombre | Código |
| --- | --- |
| Jefferson Rubio Rincón | 1092850160 |
| Miguel Angel Villegas Villada | 1092850295 |

**Proyecto:** VetCare, sistema de gestión para clínica veterinaria  
**Repositorio:** https://github.com/MAngel4311/vetcare-gestion-mascotas

---

Funcionalidad analizada: **programar una cita** (`POST /api/citas`). Intervienen `CitaController`, `CitaService`, `DisponibilidadService`, `CitaRepository`, `NotificacionService` y las entidades `Mascota`, `Veterinario`, `Cita` y `Dueno`.

## Los 3 hallazgos

### H-01 · El horario de atención está duplicado
- **Tipo:** deuda técnica
- **Dónde:** `CitaService.java`, líneas 53-57 y 211-231; `DisponibilidadService.java`, líneas 33-51.
- **Qué pasa:** las dos clases leen las mismas claves de configuración (`vetcare.agenda.hora-apertura` y `hora-cierre`). `DisponibilidadService` las convierte una vez y comprueba que el cierre sea posterior a la apertura. `CitaService` las guarda como texto, las convierte en cada llamada y no hace esa comprobación.
- **Por qué importa:** una misma regla de negocio (el horario de la clínica) vive en dos sitios y puede quedar en desacuerdo. Además, la regla no se puede probar sin Spring.
- **Principio afectado:** una sola fuente de verdad (DRY), cohesión y responsabilidad única.

### H-02 · El texto de los correos se arma en seis clases
- **Tipo:** deuda técnica
- **Dónde:** `CitaService.avisar` (líneas 185-195), `NotificacionService.enviarConfirmacion` (98-108), `RecordatorioCitaService` (56), `RecordatorioVacunaService` (82), `DuenoService` (75) y `VeterinarioService` (80).
- **Qué pasa:** cada clase redacta su propio saludo, bloque de datos y despedida. Además, programar una cita publica un evento y las demás operaciones publican el texto ya armado: son dos mecanismos para lo mismo.
- **Por qué importa:** cambiar el formato de los avisos obliga a tocar seis clases, y `CitaService` conoce cosas de redacción que no le corresponden.
- **Principio afectado:** responsabilidad única y separación de capas.

### H-03 · Un veterinario puede registrar la consulta de una cita ajena
- **Tipo:** defecto funcional (autorización)
- **Dónde:** `RegistroClinicoService.java`, método `registrar`, líneas 39-66.
- **Qué pasa:** el método busca la cita por id y la procesa sin comprobar que sea del veterinario que hace la petición. En cambio `historial` y `CitaService.obtener` sí lo comprueban.
- **Por qué importa:** se podría escribir en la historia clínica de otro profesional usando un id ajeno.
- **Principio afectado:** control de acceso a nivel de objeto y consistencia.

## Hallazgo elegido

Elegimos el H-01. El cambio consiste en crear una clase llamada `HorarioAtencion`, que guarda en un solo lugar el horario de la clínica y la regla para validarlo, y hacer que `CitaService` y `DisponibilidadService` la usen en vez de repetirla. Lo elegimos porque mejora el orden del código sin cambiar nada de lo que ve el usuario: las citas se siguen aceptando y rechazando igual que antes. Además es un arreglo pequeño, que toca pocos archivos, y se puede comprobar con facilidad, porque la regla se puede probar sola, sin necesitar la base de datos. Los otros dos hallazgos quedaron pendientes: el H-02 obligaría a tocar seis clases, y el H-03 es más grave pero cambia el comportamiento del sistema, así que es un defecto que se corrige aparte y no un simple reordenamiento del código.

| Hallazgo | Impacto | Riesgo | Esfuerzo | Decisión |
| --- | --- | --- | --- | --- |
| H-01 | 2 | 2 | 1 | **Elegido** |
| H-02 | 2 | 1 | 3 | Pendiente: esfuerzo alto |
| H-03 | 3 | 2 | 1 | Pendiente: cambia el comportamiento |
