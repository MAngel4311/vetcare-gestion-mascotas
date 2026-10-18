# Vet-Care — Backend

API REST en **Spring Boot 3.5 + Java 17 + PostgreSQL** para gestionar dueños, mascotas, citas,
historial clínico y vacunas de una clínica veterinaria, con **inicio de sesión (JWT) y cuatro roles**
(recepcionista, veterinario, administrador y usuario/dueño, que se registra solo).

## Cómo correrlo

1. Instala **JDK 17+** y **PostgreSQL**. Crea una base de datos llamada `vetcare`.
2. Define la clave de la base de datos como variable de entorno (no la escribas en el código):
   `DB_PASSWORD=tu_clave` (y `DB_USER` si tu usuario no es `postgres`).
3. Abre la carpeta en tu IDE y ejecuta `VetCareApplication` (o `mvn spring-boot:run`).
   Las tablas se crean solas.
4. Abre **http://localhost:8080/swagger-ui.html**.

### Iniciar sesión en Swagger

1. Abre `POST /api/auth/login`, pulsa *Try it out* y envía por ejemplo
   `{ "username": "admin", "password": "Admin2026*" }`.
2. Copia el valor de `token` de la respuesta.
3. Pulsa el botón **Authorize** (arriba a la derecha), pega el token (sin la palabra *Bearer*) y **Authorize**.
4. Ya puedes probar los endpoints. Para probar otro rol, vuelve a hacer login y a autorizar.

### Usuarios de demostración

Si la tabla de usuarios está vacía al arrancar, se crean estos tres:

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `Admin2026*` | ADMINISTRADOR |
| `recepcion` | `Recepcion2026*` | RECEPCIONISTA |
| `veterinario` | `Veterinario2026*` | VETERINARIO |

Cámbialas (con `PUT /api/usuarios/{id}`) o pon `vetcare.seed.usuarios-demo=false` cuando tengas las tuyas.

### Si ya tenías la base de datos creada (importante al añadir el rol USUARIO y el estado CONFIRMADA)

Hibernate creó las tablas `usuarios` y `citas` con una lista fija de valores (roles y estados). Ejecuta **una sola vez** en pgAdmin (Query Tool de la base `vetcare`):

```sql
ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_rol_check;
ALTER TABLE citas DROP CONSTRAINT IF EXISTS citas_estado_check;
```

Las columnas nuevas (`mascotas.activo`, `vacunaciones.recordatorio_enviado`, `citas.motivo_cancelacion`) las crea Hibernate solas; las mascotas que ya existían quedan activas.

## Quién puede hacer qué

| Acción | Recepcionista | Veterinario | Administrador | Usuario (dueño) |
|---|:-:|:-:|:-:|:-:|
| Consultar dueños, mascotas, veterinarios, citas, disponibilidad, vacunas | ✅ | ✅ | ✅ | ❌ (solo lo suyo, ver abajo) |
| Registrar / editar dueños y mascotas (F-01, F-05) | ✅ | ❌ | ✅ | solo su ficha y sus mascotas |
| Programar, cancelar, reprogramar citas (F-02, F-06) | ✅ | ❌ | ✅ | ❌ (solo ve las suyas) |
| Registrar consulta y ver historial clínico (F-03, F-04) | ❌ | ✅ | ✅ | solo ve el historial de sus mascotas |
| Registrar vacunas (F-08) | ❌ | ✅ | ✅ | ❌ (solo ve las de sus mascotas) |
| Reportes (F-07) | ❌ | ❌ | ✅ | ❌ |
| Crear / editar veterinarios y usuarios | ❌ | ❌ | ✅ | ❌ |
| Registrarse a sí mismo (`POST /api/auth/registro`, sin token) | — | — | — | ✅ |

**Rol USUARIO (dueño).** Se registra solo con `POST /api/auth/registro` (se crea su ficha de dueño y su cuenta, y se devuelve el
token). Solo puede ver y editar su propia ficha, registrar y editar **sus** mascotas, y consultar el historial, las vacunas y las
citas de **sus** mascotas. Lo que es de otro dueño responde `404` (mascotas, citas) o `403` (ficha de otro dueño). Si el dueño ya
existía (lo registró recepción), el administrador le crea la cuenta con `POST /api/usuarios` (`rol: USUARIO` y `duenoId`).

Sin token se responde **401**; con un rol sin permiso, **403**.

## Endpoints

| Funcionalidad | Método y ruta |
|---|---|
| Sesión | `POST /api/auth/login` · `POST /api/auth/registro` (dueños) · `GET /api/auth/me` · `PUT /api/auth/password` (cambiar mi clave) |
| Usuarios (admin) | `POST /api/usuarios` · `GET /api/usuarios` · `PUT /api/usuarios/{id}` |
| F-01 Dueños (crea también su cuenta) | `POST /api/duenos` · `GET /api/duenos?documento=&nombre=` · `GET/PUT /api/duenos/{id}` |
| Veterinarios | `POST /api/veterinarios` · `GET /api/veterinarios?activo=` · `GET/PUT /api/veterinarios/{id}` |
| **Disponibilidad** | `GET /api/veterinarios/{id}/disponibilidad?fecha=&duracionMinutos=` |
| F-05 Mascotas | `POST /api/mascotas` · `GET /api/mascotas?nombre=&duenoId=&especie=` · `GET/PUT /api/mascotas/{id}` |
| F-02 Programar cita | `POST /api/citas` |
| Agenda | `GET /api/citas?fecha=&veterinarioId=&mascotaId=&estado=` · `GET /api/citas/{id}` |
| Confirmar cita | `PATCH /api/citas/{id}/confirmar` |
| F-02 Cancelar (con motivo) | `PATCH /api/citas/{id}/cancelar` con cuerpo `{"motivo":"..."}` |
| F-06 Reprogramar | `PUT /api/citas/{id}/reprogramar` |
| F-03 Registrar consulta | `POST /api/citas/{id}/registro-clinico` |
| F-04 Historial | `GET /api/mascotas/{id}/historial` |
| F-08 Vacunas | `POST /api/mascotas/{id}/vacunas` · `GET /api/mascotas/{id}/vacunas` |
| **F-08 Seguimiento** | `GET /api/vacunas/proximas?dias=30&incluirVencidas=false` |
| Recordatorio de vacunas (admin) | `POST /api/vacunas/recordatorios` |
| Probar el correo (admin) | `POST /api/notificaciones/prueba?para=` |
| F-07 Reporte detallado | `GET /api/reportes/consultas?desde=&hasta=&veterinarioId=&estado=` |
| **F-07 Reporte consolidado** | `GET /api/reportes/resumen?desde=&hasta=&veterinarioId=&estado=` |
| Exportar reporte (CSV) | `GET /api/reportes/consultas/csv?desde=&hasta=&veterinarioId=&estado=` |

Fechas con hora: `2026-11-10T09:00:00`. Solo fechas: `2026-11-10`.
Motivos de cita: `CONTROL`, `VACUNACION`, `URGENCIA`, `OTRO`.
Estados de cita: `PROGRAMADA` (pendiente de confirmar) → `CONFIRMADA` → `ATENDIDA`, o `CANCELADA`.

## Correos automáticos (notificaciones)

Todo sale por correo y es automático; el dueño no tiene que hacer nada:

| Cuándo | Qué recibe el dueño |
|---|---|
| La recepcionista programa una cita | Confirmación de la cita registrada |
| Se confirma, cancela o reprograma una cita | Aviso con el estado y los datos actuales (la cancelación incluye el motivo) |
| 24 horas antes de la cita (revisa cada 15 min) | Recordatorio de la cita, una sola vez |
| La recepcionista registra al dueño | Su usuario y su clave temporal |
| La próxima dosis de una vacuna vence en 7 días (todos los días 08:00 y al arrancar) | Recordatorio de la vacuna, una sola vez por dosis |

**Para que envíe correos de verdad** (3 variables de entorno, sin tocar código; en IntelliJ: *Run > Edit Configurations > Environment variables*):

```
MAIL_HOST=smtp.gmail.com
MAIL_USER=tucorreo@gmail.com
MAIL_PASSWORD=<contraseña de aplicación de Google>
```

La contraseña de aplicación se crea en la cuenta de Google (Seguridad > Verificación en 2 pasos > Contraseñas de aplicaciones).
Después reinicia y comprueba con `POST /api/notificaciones/prueba?para=tucorreo@gmail.com` (solo administrador): responde si el
correo está configurado y si llegó. Sin `MAIL_HOST` la app funciona igual y solo deja un aviso en la consola.
Un dueño sin email registrado no recibe correos. Se ajusta en `application.properties`
(`vetcare.citas.recordatorio-horas`, `vetcare.vacunas.recordatorio-dias`, `*-habilitado`).

## Reglas de negocio (de qué requisito sale cada una)

- **Cuenta del dueño** — Cuando la recepcionista registra un dueño (`POST /api/duenos`) se le crea solo su cuenta
  USUARIO: el usuario es su documento y la respuesta trae una `passwordTemporal` (solo se muestra esa vez). La
  recepcionista se la entrega al dueño, que la cambia con `PUT /api/auth/password`. El dueño también puede
  registrarse solo con `POST /api/auth/registro`.

- **SWR-02 / SWR-11** — Un veterinario no puede tener dos citas PROGRAMADAS que se crucen
  (al agendar y al reprogramar). La fila del veterinario se bloquea durante la operación para
  que dos peticiones simultáneas no se salten la validación.
- **StR de F-02 ("hora disponible")** — `disponibilidad` devuelve las citas ocupadas del día y los
  huecos libres dentro del horario de atención (`vetcare.agenda.hora-apertura` / `hora-cierre`).
  Es informativo; la validación real sigue siendo la de choque. Si hoy ya pasó la hora de apertura,
  los huecos libres empiezan desde ahora. Con `duracionMinutos` solo salen huecos donde cabe la cita.
- **SWR-03** — El motivo se elige de una lista (enum).
- **SWR-04** — Al agendar, se envía un correo de confirmación al dueño **después** de guardar
  la cita y en otro hilo: si el correo falla, la cita no se pierde. Si no configuras
  `spring.mail.*`, solo queda un aviso en el log.
- **SWR-05 / RF-09** — Solo se cancelan citas PROGRAMADAS o CONFIRMADAS que aún no han pasado, y siempre con un
  motivo (queda guardado en `motivoCancelacion`). Una cita ATENDIDA no se cancela. Al cancelar, el horario queda libre.
- **Confirmar (RF-08)** — Una cita nace PROGRAMADA y se pasa a CONFIRMADA con `PATCH /api/citas/{id}/confirmar`.
  Las citas PROGRAMADAS y CONFIRMADAS ocupan el horario del veterinario.
- **RN-03 horario** — Las citas solo se aceptan de lunes a sábado, entre `hora-apertura` y `hora-cierre`
  (08:00 a 18:00), y deben empezar y terminar el mismo día.
- **RN-04** — La consulta (registro clínico) solo se registra sobre una cita CONFIRMADA.
- **RF-14 / RN-07** — No se desactiva un veterinario que tiene citas futuras PROGRAMADAS o CONFIRMADAS.
- **RN-08** — Una mascota se da de baja con `activo=false` en `PUT /api/mascotas/{id}`: conserva su historial
  pero no admite citas nuevas, y no se puede dar de baja si tiene citas futuras vigentes.
- **RF-13** — Todos los días a las 08:00 (hora de Bogotá) se envía por correo un recordatorio de las vacunas cuya
  próxima dosis vence dentro de `vetcare.vacunas.recordatorio-dias` días (7 por defecto). Cada dosis se avisa una sola vez.
  El administrador puede lanzarlo a mano con `POST /api/vacunas/recordatorios`. Sin `spring.mail.*` solo queda un aviso en el log.
- **RF-04** — `GET /api/mascotas?especie=perro` filtra por especie (se combina con nombre y dueño).
- **RF-15** — `GET /api/reportes/consultas/csv` descarga el detalle del reporte en CSV (separador `;`, abre en Excel).
- **SWR-06 / 07 / 08** — El registro clínico exige diagnóstico y tratamiento; acepta peso,
  temperatura y alergias. La mascota, el veterinario y la fecha se toman de la cita, y la cita
  pasa a ATENDIDA. Una cita solo puede tener un registro clínico. Los registros no se editan ni se borran.
- **SWR-09** — El historial de la mascota sale del más reciente al más antiguo.
- **SWR-10** — La mascota exige un dueño existente; un dueño no puede tener dos mascotas con el
  mismo nombre (sin importar mayúsculas).
- **SWR-12** — El reporte filtra por periodo (inclusivo) y veterinario opcional. Por defecto
  muestra solo consultas ATENDIDAS; con `estado=` puedes ver otro estado. El resumen agrega el
  total de consultas y cuántas hubo por veterinario y por motivo.
- **SWR-13** — La próxima fecha de vacuna debe ser posterior a la de aplicación.
- **Seguimiento de vacunas** — `proximas` lista las vacunas cuya próxima dosis cae en los siguientes
  N días, con nombre y datos de contacto del dueño. Si la mascota ya recibió después la misma
  vacuna, esa dosis ya no aparece. Con `incluirVencidas=true` también salen las atrasadas
  (`diasRestantes` negativo).
- El documento del dueño y del veterinario es único. Un administrador no puede desactivarse a sí mismo
  ni quitarse el rol.

## Errores

Todos los errores tienen el mismo formato:

```json
{
  "timestamp": "2026-11-01T10:15:30",
  "status": 409,
  "error": "Conflict",
  "message": "El veterinario ya tiene una cita en ese horario"
}
```

`400` datos inválidos (con `fieldErrors` por campo) · `401` sin sesión o credenciales incorrectas ·
`403` rol sin permiso · `404` no existe · `409` duplicado, choque de horario o estado no permitido.

## Estructura

```
com.vetcare
├── controller   → endpoints REST
├── service      → reglas de negocio (transacciones)
├── repository   → acceso a datos (Spring Data JPA)
├── model        → entidades JPA (+ enums)
├── dto          → objetos de entrada/salida de la API
├── mapper       → entidad → DTO
├── security     → respuestas 401 / 403 en JSON
├── exception    → errores y manejo global
├── event        → evento "cita programada"
├── config       → seguridad (JWT), CORS, Swagger, usuarios demo
└── util
```

## Configuración importante (`application.properties`)

- `vetcare.jwt.secret` — clave con la que se firman los tokens (mínimo 32 caracteres). En un entorno real,
  defínela con la variable de entorno `JWT_SECRET`.
- `vetcare.jwt.expiracion-minutos` — duración de la sesión (480 = 8 horas).
- `vetcare.cors.allowed-origins` — orígenes del frontend permitidos.
- `vetcare.agenda.hora-apertura` / `hora-cierre` — horario de atención (lunes a sábado): valida las citas y calcula horas libres.
- `vetcare.vacunas.recordatorio-habilitado` / `recordatorio-dias` / `recordatorio-cron` — recordatorio de vacunas por correo.

## Límites conocidos

- El token no se puede revocar: si desactivas un usuario, su sesión actual sigue valiendo hasta que expire.
- Un usuario VETERINARIO no está ligado a una fila de `veterinarios`: cualquier veterinario con sesión
  puede registrar la consulta de cualquier cita.
- No hay pruebas automáticas ni borrado de registros (a propósito, para conservar el historial).
- El registro de mascota exige que el dueño ya exista: el frontend debe resolver el caso "dueño nuevo".
