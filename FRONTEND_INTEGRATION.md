# Guía para conectar el frontend (AFC App) con este backend

Esta guía es para quien conecte las pantallas de la app (Expo/React Native, carpeta `AFC-APP`) con la API. Dice qué
endpoint usa cada pantalla, cómo manejar la sesión y qué errores pueden llegar. El detalle de cada campo está en
Swagger (`http://localhost:8091/swagger-ui.html` con el backend corriendo en desarrollo) y en el `README.md`.
Reemplaza a `AFC-APP/API_CONTEXT.md`, que se escribió antes de que existiera el backend y ya no coincide con la API.

## Lo básico

- Todas las rutas empiezan con `/v1` (no `/api`) y hablan JSON.
- Las pantallas que necesitan sesión mandan `Authorization: Bearer <token>`.
- En desarrollo el backend acepta peticiones desde `http://localhost:8081` (Expo web) y `http://localhost:19006`. En
  producción las direcciones permitidas se configuran con `ALLOWED_ORIGINS`.
- Fechas: `2026-11-25`. Horas: `18:00`. Fecha y hora local (citas): `2026-11-27T18:00`. Los campos que terminan en
  `At` (`createdAt`, `approvedAt`...) vienen en UTC con `Z` al final; hay que convertirlos a la hora local para
  mostrarlos.
- Montos: números con dos decimales, en pesos mexicanos.

## Sesión

1. **Entrar:** `POST /v1/auth/login` con `{ "username": "...", "password": "..." }` responde
   `{ "token", "role", "expiresInSeconds" }`. Hay que guardar el token y el rol (SecureStore en el teléfono,
   localStorage en web).
2. **A dónde mandar al usuario según el rol:** `ADMIN` va a `AdminDashboard`. `SOCIO` va a `MemberHome`. `COACH`
   existe pero todavía no tiene pantallas: mostrar "Tu cuenta aún no tiene acceso a la app" y cerrar sesión.
3. **Mantener la sesión viva:** el token vence a los 60 minutos si no se renueva. Cuando el usuario haga algo y al
   token le quede menos de la mitad de `expiresInSeconds`, llamar `POST /v1/auth/refresh` (sin cuerpo, con el token
   actual) y guardar el token nuevo. Si el usuario deja la app sin usar más de 60 minutos, la sesión se cierra sola.
   Una sesión dura como máximo 12 horas aunque se siga usando.
4. **Cualquier `401`** (token vencido, sesión cerrada, contraseña cambiada, socio dado de baja): borrar el token
   guardado y regresar al login.
5. **Salir:** `POST /v1/auth/logout`. Cierra esa sesión en el servidor (también los tokens anteriores de esa
   sesión).
6. **Perfil del usuario:** `GET /v1/users/me` (nombre, correo, rol).

## Errores

Todo error llega con este formato, con el mensaje ya en español y listo para mostrarse tal cual:

```json
{ "status": 400, "message": "Faltan campos obligatorios: fullName", "fields": { "fullName": "es obligatorio" } }
```

- `fields` solo viene cuando el error es de campos concretos: sirve para marcar en rojo cada input.
- `400` datos mal capturados, `401` sesión (ver arriba), `403` ese rol no puede hacer eso, `404` no existe,
  `409` choca con el estado actual (sin stock, cupo lleno, ya existe), `503` el pago con tarjeta o transferencia no
  está configurado en el servidor.

## Pantalla por pantalla

| Pantalla (frontend) | Qué llama |
|---|---|
| `LoginScreen` / `LoginForm` | `POST /v1/auth/login`. "¿Olvidaste tu contraseña?": `POST /v1/auth/password-resets` con `{ "email" }` (siempre responde `204`; al usuario le llega un correo con el código). Luego una pantalla donde pegue el código y escriba la nueva contraseña: `POST /v1/auth/password-resets/confirm` con `{ "token", "newPassword" }`. Si se configura `PASSWORD_RESET_URL` con un deep link (por ejemplo `afcapp://recuperar`), el correo trae ese enlace con `?token=...` y la app puede abrir esa pantalla con el código ya puesto |
| `AdminDashboardScreen` | `GET /v1/dashboard/summary` (socios activos/vencidos, ingresos del mes y de los últimos meses, ventas por categoría, alertas de vencimiento y stock bajo) |
| `MembersScreen` / `AddMemberModal` | Lista y búsqueda: `GET /v1/members?search=...&status=ACTIVE`. Alta: `POST /v1/members`. Editar: `PUT /v1/members/{id}`. Baja: `PATCH /v1/members/{id}/deactivate`. PIN o huella: `PATCH /v1/members/{id}/credentials`. Cuenta para la app del socio: `POST /v1/members/{id}/account` |
| Renovar en recepción (`PaymentModal` del admin) | `POST /v1/members/{id}/payments` con `membershipType` y `method`: tarjeta queda aprobada al momento (pide `cardLastFour`), transferencia queda pendiente, efectivo pide `scheduledDate`. Historial: `GET /v1/members/{id}/payments`. Aprobar o rechazar una transferencia: `PATCH /v1/payments/{id}/approve` y `PATCH /v1/payments/{id}/reject` con `{ "reason" }` |
| Check-in en recepción (`PinModal`) | `POST /v1/check-ins` con `{ "memberId", "pin" }` o `{ "memberId", "fingerprintToken" }` |
| `POSTiendaScreen` y sus modales | Catálogo: `GET /v1/products?search=...&categoryId=...&lowStock=true` (los tres filtros son opcionales y se combinan), `POST`/`PUT /v1/products`, `PATCH /v1/products/{id}/deactivate`. Categorías y proveedores: `GET`/`POST /v1/categories` y `/v1/suppliers`. Entrada de inventario: `POST /v1/products/{id}/inventory-movements`. Al agregar al carrito: `POST /v1/sales/stock-validation`. Cobrar: `POST /v1/sales` |
| `VentasScreen` | `GET /v1/sales?from=2026-11-01&to=2026-11-30`, detalle `GET /v1/sales/{id}`, Excel `GET /v1/sales/export?from=...&to=...` |
| `CajaCorteScreen` | `GET /v1/cash-cuts?period=DAILY|WEEKLY|MONTHLY&category=MEMBERSHIPS|STORE|ALL&date=...` y Excel `GET /v1/cash-cuts/export` con los mismos parámetros. No trae egresos ni utilidad: esos números del diseño no existen en el backend |
| `AdminScheduleScreen` / `AdminScheduleModal` | Coaches: `GET`/`POST /v1/coaches`. Horarios: `GET`/`POST /v1/schedule-blocks`, `PUT /v1/schedule-blocks/{id}`, `PATCH .../activate` y `.../deactivate`. Aforo: `GET /v1/schedule-blocks/occupancy?date=...` |
| `CapacityScreen` (socio) | Aforo del día: `GET /v1/schedule-blocks/occupancy?date=...`. Reservar: `POST /v1/bookings` con `{ "scheduleBlockId", "classDate" }`. Mis reservas: `GET /v1/bookings/me`. Cancelar: `PATCH /v1/bookings/{id}/cancel` |
| `RoutinesScreen` / `AddRoutineModal` | `GET`/`POST /v1/routines`, `PUT`/`DELETE /v1/routines/{id}` (una rutina por día, máximo 8 ejercicios). Video por archivo: `POST /v1/routines/{id}/exercises/{exerciseId}/video` como `multipart/form-data` con el campo `file` (MP4, MOV o WEBM, hasta 100 MB); o mandar `videoUrl` en el ejercicio |
| `MemberRoutinesScreen` | `GET /v1/routines/by-day` (hoy) o `?dayOfWeek=MONDAY`. Si ese día no hay rutina responde `404` con un mensaje para mostrar |
| `AdminAnnouncementsScreen` | `GET`/`POST /v1/announcements`, `DELETE /v1/announcements/{id}` |
| `MemberHomeScreen` | Avisos al abrir (`AnnouncementModal`): `GET /v1/announcements/active` y, al cerrar uno, `POST /v1/announcements/{id}/dismiss`. Notificaciones (`notificationService`): después del login, `POST /v1/push-tokens` con `{ "token": "ExponentPushToken[...]" }` |
| Pagar la membresía desde la app (`MembershipCard` / `PaymentModal` del socio) | `POST /v1/payments/me` con `membershipType` y `method`. **Tarjeta:** abrir `checkoutUrl` (Mercado Pago); el pago se aprueba solo cuando Mercado Pago avisa al backend. **Transferencia:** mostrar `bankTransfer` (banco, CLABE y la referencia `PAG-000123` que el socio debe poner en el concepto). **Efectivo:** mandar `appointmentAt`; el socio ve si la cita queda `PENDING`, `ACCEPTED`, `CANCELLED` (con motivo y horario sugerido) o `COMPLETED`. Mis pagos: `GET /v1/payments/me`. Cancelar: `PATCH /v1/payments/me/{id}/cancel`. Reagendar: `PATCH /v1/payments/me/{id}/appointment` |

Hoy no existen pantallas para estas dos funciones del backend; si se quieren usar, hay que crearlas:

- **Agenda de citas de pago en efectivo (admin):** `GET /v1/payment-appointments`, `PATCH /v1/payment-appointments/{id}/accept`
  y `PATCH /v1/payment-appointments/{id}/cancel` con `{ "reason", "suggestedAt" }`.
- **Auditoría (admin):** `GET /v1/audit-logs?actorUserId=&action=&outcome=&from=&to=&page=0&size=50`.

## Valores fijos que la app debe usar

| Campo | Valores |
|---|---|
| `membershipType` | `WEEKLY`, `BIWEEKLY`, `MONTHLY`, `QUARTERLY`, `YEARLY` |
| `method` / `paymentMethod` | `CARD`, `SPEI`, `CASH` |
| `status` del socio (lo calcula el servidor) | `ACTIVE`, `EXPIRED`, `TO_PURGE` |
| `type` de anuncio | `INFO`, `URGENT`, `MAINTENANCE`, `PROMOTION` (el `alert` del diseño es `URGENT`) |
| `dayOfWeek` | `MONDAY` a `SUNDAY` |

Si se manda otro valor, el error dice cuáles son los permitidos.

## Descargar los Excel

Las descargas también necesitan el token, así que un enlace simple no sirve. En web: `fetch` con el encabezado
`Authorization`, convertir la respuesta a `blob` y descargarla con el nombre que viene en `Content-Disposition`. En el
teléfono: `FileSystem.downloadAsync(url, destino, { headers: { Authorization: 'Bearer ...' } })` de `expo-file-system`
y luego compartir el archivo.

## Pendientes del lado del negocio

- **Correo de recuperación de contraseña:** el backend ya lo envía por SMTP, pero en producción hay que dar de alta la
  cuenta de correo que lo manda (`MAIL_HOST`, `MAIL_FROM` y compañía, ver el README). Sin eso, el código se genera y
  no le llega a nadie.
- **Rol Coach:** existe, pero sin permisos hasta que el negocio defina qué puede hacer un coach en la app.
