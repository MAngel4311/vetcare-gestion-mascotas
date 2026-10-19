# VetCare — Frontend (React + Vite)

Interfaz web para el backend Spring Boot de VetCare (`vetcare-gestion-mascotas`).

## Cómo correrlo

1. Levanta el backend (puerto 8080) con PostgreSQL, como indica su README.
2. En esta carpeta:

```bash
npm install
npm run dev
```

3. Abre **http://localhost:5173** (el backend ya permite CORS desde ese puerto, por eso está fijo).

Si el backend corre en otra dirección: copia `.env.example` a `.env` y cambia `VITE_API_URL`.

## Usuarios de demostración (los crea el backend)

| Usuario | Contraseña | Rol |
|---|---|---|
| admin | Admin2026* | Administrador |
| recepcion | Recepcion2026* | Recepcionista |
| veterinario | Veterinario2026* | Veterinario |

Un **dueño** se crea de dos formas: él mismo en «¿Eres dueño de una mascota? Crea tu cuenta» (pantalla de login), o la
recepcionista lo registra en *Dueños* y el sistema le muestra su usuario (el documento) y una **contraseña temporal**, que
se ve una sola vez.

## Flujo de una cita (estados del PDF)

`Pendiente` → (recepción **confirma**) → `Confirmada` → (veterinario **registra la consulta**) → `Atendida`.
Se puede **cancelar** (con motivo obligatorio) mientras esté Pendiente o Confirmada.

## Pantallas por rol

| Pantalla | Recepcionista | Veterinario | Administrador | Dueño |
|---|:-:|:-:|:-:|:-:|
| Agenda / citas: ver | ✅ | ✅ | ✅ | solo las suyas |
| Programar, confirmar, reprogramar, cancelar | ✅ | — | ✅ | — |
| Registrar consulta (cita Confirmada) | — | ✅ | ✅ | — |
| Dueños: ver y registrar | ✅ | ver | ✅ | — |
| Mascotas: ver | ✅ | ✅ | ✅ | solo las suyas |
| Mascotas: crear, editar, dar de baja | ✅ | — | ✅ | las suyas |
| Historial clínico | — | ✅ | ✅ | el de sus mascotas |
| Registrar vacunas | — | ✅ | ✅ | — (las ve) |
| Vacunas próximas y recordatorios | ✅ | ✅ | ✅ (y enviar) | — |
| Reportes (CSV del servidor), Veterinarios, Usuarios | — | — | ✅ | — |
| Mi cuenta (cambiar contraseña, mis datos) | ✅ | ✅ | ✅ | ✅ |

Los botones se ocultan según el rol, pero **la seguridad real la aplica el backend** (401/403).

## Estructura

```
src/
├── api.js          cliente HTTP (token JWT, errores, descargas) y utilidades de fecha
├── auth.jsx        sesión, registro de dueños y permisos por rol
├── hooks.js        useLoad (cargar datos) y useForm
├── ui.jsx          Modal, Field, mensajes de error, useSubmit
├── App.jsx         menú lateral por rol y rutas
├── styles.css
└── pages/          Login, Agenda, Mascotas, Duenos, VacunasProximas,
                    Reportes, Veterinarios, Usuarios, Perfil
```
