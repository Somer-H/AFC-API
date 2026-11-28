# Backend: AFC Gym Management System

API REST con Spring Boot 4.1 (Java 21), Spring Data JPA, Flyway y Swagger.
Arquitectura en capas (entity / repository / service / controller), organizada por capa técnica, no por feature.
Base de datos H2 en desarrollo y PostgreSQL en producción.

Este backend reemplaza los datos simulados del frontend de AFC App (Expo/React Native, en `../AFC-APP`). Cubre, del
cronograma (`Cronograma-backend-AFC-Gym.xlsx`): Infraestructura, Autenticación y acceso (HU-01 a HU-06), Gestión de
socios (HU-07 a HU-12), Membresías y pagos (HU-13 a HU-19), Tienda e inventario (HU-20 a HU-25), Ventas / POS
(HU-26 a HU-28), Caja y corte de caja (HU-29 a HU-31), Horarios, clases y capacidad (HU-32 a HU-36), Rutinas y
ejercicios (HU-37 a HU-40), Anuncios (HU-41 a HU-43), Dashboard (HU-44), Notificaciones (HU-45 a HU-47), Funciones transversales (HU-48 a HU-51) y los flujos de pago
del socio (tarjeta con Mercado Pago,
transferencia y efectivo con cita, según el documento de alcance de pagos) — ver
`Historias-de-Usuario-AFC-Gym.docx`. Además cumple los requerimientos no funcionales de `Requerimientos-AFC-Gym.docx`
que tocan al backend: sesión que expira por inactividad (RNF-05), rol Coach previsto (RNF-02), exportación del
historial de ventas (criterio general 6) y tiempos de respuesta medidos (RNF-08).

Para conectar las pantallas de la app con esta API, ver `FRONTEND_INTEGRATION.md` (qué endpoint usa cada pantalla,
manejo de la sesión y de los errores).

## Abrir y correr el proyecto en IntelliJ IDEA

1. **File > Open**, elige la carpeta `backend` (la que contiene `pom.xml`) y pulsa **Trust Project**.
2. Espera a que IntelliJ importe Maven (barra de progreso abajo a la derecha).
3. **File > Project Structure > Project**: en **SDK** elige un JDK 21 y en **Language level** elige 21.
4. Abre `src/main/java/com/afcgym/platform/AfcGymPlatformApplication.java` y pulsa el triángulo verde junto a `main` > **Run**.
5. Cuando la consola diga `Started AfcGymPlatformApplication`, abre:

| Qué | URL |
|---|---|
| Documentación de la API (Swagger) | http://localhost:8091/swagger-ui.html |
| Estado de la aplicación | http://localhost:8091/actuator/health |
| Consola de la base de datos (solo desarrollo) | http://localhost:8091/h2-console |

Datos de la consola H2: JDBC URL `jdbc:h2:file:./data/afc-gym-dev`, usuario `sa`, contraseña vacía.

Para detener la aplicación pulsa el cuadro rojo de la ventana **Run**.

### Si el puerto 8091 está ocupado

**Run > Edit Configurations**, selecciona `AfcGymPlatformApplication` y en **Environment variables** escribe `PORT=8095`
(o el puerto que quieras).

### Dependencias nuevas

Si al abrir el proyecto IntelliJ marca en rojo `org.dhatim.fastexcel` (la librería que genera el Excel del corte de
caja) o `org.springframework.mail` (la que envía los correos de recuperación de contraseña), abre la ventana **Maven**
(derecha) y pulsa **Reload All Maven Projects** para que las descargue.

### Si Maven falla con `PKIX path building failed`

Pasa cuando un antivirus o proxy inspecciona HTTPS. En IntelliJ:

1. **File > Settings > Build, Execution, Deployment > Build Tools > Maven > Importing**: en **VM options for importer** escribe `-Djavax.net.ssl.trustStoreType=Windows-ROOT`.
2. En **Maven > Runner**: en **VM Options** escribe lo mismo.
3. Abre la ventana **Maven** (derecha) y pulsa **Reload All Maven Projects**.

### Pruebas

Clic derecho sobre la carpeta `src/test/java` > **Run 'All Tests'**. Las 57 clases de prueba (479 pruebas) ya pasan en
este repositorio (`./mvnw test`, perfil `test`, H2 en memoria).

## Perfiles

| Perfil | Base de datos | Uso |
|---|---|---|
| `dev` (por defecto) | H2 en archivo (`./data`, ignorado por Git), modo PostgreSQL | Desarrollo local |
| `test` | H2 en memoria | Pruebas automáticas |
| `prod` | PostgreSQL | Despliegue |

Producción se activa con `SPRING_PROFILES_ACTIVE=prod` y estas variables de entorno (nunca se suben al repositorio):

| Variable | Para qué | Obligatoria |
|---|---|---|
| `DB_URL` | Conexión a PostgreSQL | Sí |
| `DB_USER` y `DB_PASSWORD` | Credenciales de la base de datos | Sí |
| `SECRET_KEY` | Clave para firmar los tokens JWT (mínimo 32 caracteres, aleatoria) | Sí |
| `JWT_TTL_MINUTES` | Minutos de inactividad tras los que se cierra la sesión (por defecto 60, de 5 a 1440) | No |
| `SESSION_MAX_HOURS` | Horas máximas que dura una sesión aunque el usuario siga activo; después tiene que volver a entrar (por defecto 12, de 1 a 168) | No |
| `BOOTSTRAP_ADMIN_PASSWORD` | Contraseña del primer usuario administrador. Solo se usa si todavía no existe ningún usuario | Solo la primera vez |
| `BOOTSTRAP_ADMIN_USERNAME`, `BOOTSTRAP_ADMIN_FULL_NAME`, `BOOTSTRAP_ADMIN_EMAIL` | Datos de ese primer usuario | No |
| `ALLOWED_ORIGINS` | Direcciones del frontend permitidas por CORS, separadas por comas | Sí, con el frontend desplegado |
| `VIDEOS_DIRECTORY` | Carpeta donde se guardan los videos subidos de los ejercicios (por defecto `./data/videos`). En producción tiene que ser un disco persistente | Sí, si se suben videos |
| `MERCADO_PAGO_ACCESS_TOKEN` | Access token de Mercado Pago (de prueba `TEST-...` o de producción `APP_USR-...`). Sin él, el pago con tarjeta responde "no disponible" | Sí, para cobrar con tarjeta |
| `MERCADO_PAGO_WEBHOOK_SECRET` | Clave secreta de la notificación (Webhooks) que da el panel de Mercado Pago. Sin ella se rechazan todas las notificaciones y el pago con tarjeta queda desactivado | Sí, para cobrar con tarjeta |
| `MERCADO_PAGO_NOTIFICATION_URL` | URL pública de este backend que recibe los avisos: `https://<dominio>/v1/payment-webhooks/mercado-pago` | Sí, para cobrar con tarjeta |
| `MERCADO_PAGO_BACK_URL` | A dónde regresa al socio Mercado Pago al terminar (por ejemplo el deep link de la app) | No |
| `BANK_TRANSFER_BANK_NAME`, `BANK_TRANSFER_BENEFICIARY`, `BANK_TRANSFER_CLABE` | Datos bancarios que se le muestran al socio para transferir (CLABE de 18 dígitos). Sin ellos la transferencia responde "no disponible" | Sí, para cobrar por transferencia |
| `PAYMENT_APPOINTMENT_MINUTES` | Cuánto dura una cita de pago en efectivo, para detectar empalmes (por defecto 30) | No |
| `PAYMENT_APPOINTMENT_MAX_DAYS_AHEAD` | Con cuántos días de anticipación máxima se puede pedir una cita (por defecto 30) | No |
| `EXPO_ACCESS_TOKEN` | Token de acceso de Expo, solo si en el proyecto de Expo se activó "Enhanced push security" | No |
| `EXPIRY_REMINDER_DAYS_BEFORE` | Con cuántos días de anticipación se avisa que vence la membresía (por defecto 3, de 1 a 30) | No |
| `EXPIRY_REMINDER_CRON` | A qué hora corre el recordatorio de vencimientos, en formato cron de Spring (por defecto `0 0 9 * * *`, todos los días a las 9:00 en la zona de `APP_TIME_ZONE`) | No |
| `MAIL_HOST`, `MAIL_PORT` | Servidor SMTP para enviar los correos de recuperación de contraseña (puerto 587 por defecto; 465 usa SSL directo). Sin `MAIL_HOST` no se envían correos | Sí, para recuperar contraseñas |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | Usuario y contraseña del servidor SMTP (en Gmail, una contraseña de aplicación) | Si el servidor pide usuario |
| `MAIL_FROM` | Remitente de los correos, por ejemplo `AFC Gym <no-responder@afcgym.mx>` | Sí, para recuperar contraseñas |
| `PASSWORD_RESET_URL` | Enlace que se pone en el correo de recuperación, por ejemplo el deep link de la app (`afcapp://recuperar`). El backend le agrega `?token=...`. Sin él, el correo solo trae el código | No |
| `APP_TIME_ZONE` | Zona horaria del gimnasio (por defecto `America/Mexico_City`). La aplicación la fija al arrancar, así que "hoy", la hora de inicio de las clases, los cortes por día y el recordatorio usan la hora del gimnasio aunque el servidor esté en UTC | No |

En producción Swagger y la documentación de la API quedan desactivados.

## Autenticación y roles

La API usa tokens JWT. El token se envía en el encabezado `Authorization: Bearer <token>` y vence tras 60 minutos sin
renovarse (ver "Sesión por inactividad" abajo).

**Roles:** `ADMIN` (recepción/administración) y `SOCIO` (miembro del gimnasio). `COACH` está previsto (RNF-02) pero
todavía sin permisos: ver "Rol Coach" abajo.

| Endpoint | Quién puede usarlo |
|---|---|
| `POST /v1/auth/login` | Cualquiera (es público) |
| `POST /v1/auth/password-resets` y `/confirm` | Cualquiera (son públicos) |
| `POST /v1/auth/logout`, `POST /v1/auth/refresh` | Cualquier usuario con sesión iniciada |
| `GET /v1/users/me` | Cualquier usuario con sesión iniciada |
| `POST /v1/users` (crear otro admin) | Solo `ADMIN` |
| `POST /v1/members`, `GET /v1/members`, `GET /v1/members/{id}`, `PUT /v1/members/{id}`, `PATCH /v1/members/{id}/deactivate`, `PATCH /v1/members/{id}/credentials` | Solo `ADMIN` |
| `POST /v1/members/{id}/payments`, `GET /v1/members/{id}/payments`, `PATCH /v1/payments/{id}/approve` | Solo `ADMIN` |
| `POST`/`GET /v1/categories`, `POST`/`GET /v1/suppliers` | Solo `ADMIN` |
| `POST /v1/products`, `GET /v1/products`, `GET`/`PUT /v1/products/{id}`, `PATCH /v1/products/{id}/deactivate`, `POST`/`GET /v1/products/{id}/inventory-movements` | Solo `ADMIN` |
| `POST /v1/sales`, `POST /v1/sales/stock-validation`, `GET /v1/sales`, `GET /v1/sales/export`, `GET /v1/sales/{id}` | Solo `ADMIN` |
| `GET /v1/cash-cuts`, `GET /v1/cash-cuts/export` | Solo `ADMIN` |
| `POST /v1/members/{id}/account` (crear la cuenta de acceso de un socio) | Solo `ADMIN` |
| `POST`/`GET /v1/coaches` | Solo `ADMIN` |
| `POST`/`GET /v1/schedule-blocks`, `GET`/`PUT /v1/schedule-blocks/{id}`, `PATCH /v1/schedule-blocks/{id}/activate` y `/deactivate` | Solo `ADMIN` |
| `GET /v1/schedule-blocks/occupancy` | `ADMIN` y `SOCIO` |
| `POST /v1/bookings`, `PATCH /v1/bookings/{id}/cancel` | `ADMIN` y `SOCIO` (el socio solo sobre sus propias reservas) |
| `GET /v1/bookings/me` | Solo `SOCIO` |
| `POST`/`GET /v1/routines`, `GET`/`PUT`/`DELETE /v1/routines/{id}`, `POST`/`DELETE /v1/routines/{id}/exercises/{exerciseId}/video` | Solo `ADMIN` |
| `GET /v1/routines/by-day` | `ADMIN` y `SOCIO` |
| `GET /v1/exercise-videos/{archivo}` | Cualquiera (es público, ver abajo) |
| `POST`/`GET /v1/announcements`, `DELETE /v1/announcements/{id}` | Solo `ADMIN` |
| `GET /v1/announcements/active`, `POST /v1/announcements/{id}/dismiss` | `ADMIN` y `SOCIO` |
| `GET /v1/dashboard/summary` | Solo `ADMIN` |
| `POST /v1/push-tokens` | Solo `SOCIO` |
| `POST /v1/payments/me`, `GET /v1/payments/me`, `GET /v1/payments/me/{id}`, `PATCH /v1/payments/me/{id}/cancel`, `PATCH /v1/payments/me/{id}/appointment` | Solo `SOCIO` (cada socio solo ve y toca sus propios pagos) |
| `PATCH /v1/payments/{id}/reject`, `GET /v1/payment-appointments`, `PATCH /v1/payment-appointments/{id}/accept`, `PATCH /v1/payment-appointments/{id}/cancel` | Solo `ADMIN` |
| `POST /v1/payment-webhooks/mercado-pago` | Público, pero solo se procesa si trae la firma válida de Mercado Pago |
| `GET /v1/audit-logs` | Solo `ADMIN` (solo lectura: no existe forma de editar ni borrar registros) |
| `POST /v1/check-ins` | Solo `ADMIN` |

Cada vez que se agregue un endpoint hay que decidir qué roles lo usan en `configuration/security/SecurityConfiguration`.
Lo que no tenga regla queda solo para `ADMIN` y `SOCIO`; un `COACH` recibe `403` en todo lo que no esté en esta tabla
como "cualquier usuario con sesión iniciada".

### Errores y validaciones (HU-48 y HU-49)

Toda respuesta de error tiene el mismo formato, con el mensaje en español y sin datos técnicos:

```json
{ "status": 400, "message": "Faltan campos obligatorios: fullName, membershipType",
  "fields": { "fullName": "es obligatorio", "membershipType": "es obligatorio" } }
```

| Caso | Respuesta |
|---|---|
| Campos obligatorios vacíos | `400` "Faltan campos obligatorios: ..." y `fields` con cada campo |
| Campo con formato incorrecto (correo, patrón, longitud, rango) | `400` "Revisa estos campos: email (no es un correo válido)" |
| Valor de opción inexistente | `400` "El valor 'DAILY' no es válido para membershipType. Valores permitidos: WEEKLY, ..." |
| JSON mal formado | `400` "El cuerpo de la petición no es un JSON válido" |
| Parámetro con tipo incorrecto o faltante | `400` "El parámetro id no tiene un formato válido" / "Falta el parámetro obligatorio period" |
| Ruta inexistente, método no permitido, contenido que no es JSON | `404`, `405` y `415` con su mensaje |
| Error inesperado del servidor | `500` "Ocurrió un error inesperado. Intenta de nuevo más tarde" (el detalle solo queda en el log del servidor) |

`fields` solo aparece cuando el error es de un campo concreto. Las operaciones exitosas responden con el registro
guardado (`200`/`201`) o `204` cuando no hay nada que devolver; el frontend muestra su propio mensaje de confirmación.

### Auditoría (HU-50 y HU-51)

La tabla `audit_log` guarda, sin que cada pantalla tenga que hacer nada:

- **Cada cambio que hace un admin** (cualquier `POST`, `PUT`, `PATCH` o `DELETE` exitoso: altas, ediciones, bajas,
  aprobación y rechazo de pagos, citas, inventario, rutinas, anuncios...).
- **Cada pago aprobado por Mercado Pago** (actor vacío, acción `MercadoPago.paymentApproved`).
- **Cada intento no autorizado**: un rol que no tiene permiso (`403`) o un socio que intenta ver o modificar un pago de
  otro socio (responde `404` para no revelar que existe, pero queda registrado como `DENIED`).

Cada registro dice **quién** (`actorUserId`, `actorRole`), **cuándo** (`occurredAt`) y **qué** (`action` como
`Payments.approve`, método, ruta, `targetId` del registro afectado o creado, `outcome` `SUCCESS`/`DENIED` y el código
de respuesta). Nunca se guarda el cuerpo de la petición (podría traer contraseñas o PIN).

El admin lo consulta con `GET /v1/audit-logs?actorUserId=&action=&outcome=&from=2026-11-01&to=2026-11-30&page=0&size=50`
(más reciente primero, máximo 100 por página). La aplicación no tiene ninguna forma de editar ni borrar registros: la
entidad no tiene setters, el repositorio solo permite guardar y consultar, y no hay endpoints `PUT`/`DELETE`.

Los datos sensibles (pagos, contacto) ya estaban restringidos por rol: un socio solo ve sus propias reservas y pagos,
y todo lo de socios, pagos de recepción, corte de caja, dashboard y auditoría es solo `ADMIN`.

### Sesión por inactividad y cierre de sesión (RNF-05)

Al iniciar sesión se abre una **sesión** (claim `sid`, con su hora de inicio en `auth_time`) y se entrega un token que
vence a los `JWT_TTL_MINUTES` (60 por defecto). Mientras el usuario usa la app, el frontend pide un token nuevo de la
misma sesión con `POST /v1/auth/refresh` (sin cuerpo, con el token actual en `Authorization`). La respuesta tiene la
misma forma que el login:

```json
{ "token": "eyJ...", "role": "SOCIO", "expiresInSeconds": 3600 }
```

- **Si el usuario deja de usar la app** más de `JWT_TTL_MINUTES`, el token vence; la siguiente petición responde `401`
  y el frontend lo manda al login. Eso es la expiración por inactividad.
- **Lo que tiene que hacer el frontend:** guardar el token nuevo cada vez que llame a refresh. Una forma simple es
  llamarlo cuando el usuario hace algo y al token le queda menos de la mitad de `expiresInSeconds`. El token anterior
  sigue sirviendo hasta que vence, así que las peticiones que ya iban en camino no fallan.
- **Tope de la sesión:** aunque el usuario siga activo, una sesión dura como máximo `SESSION_MAX_HOURS` (12 por
  defecto). Cerca de ese tope el token se entrega con menos tiempo, y después refresh responde `401` "Tu sesión terminó.
  Inicia sesión de nuevo". Así un token robado no se puede renovar para siempre.
- Refresh vuelve a revisar al usuario: si lo desactivaron (o se dio de baja al socio, que también desactiva su cuenta),
  o si cambió su contraseña con la recuperación después de que empezó esa sesión, responde `401`. Así, recuperar la
  contraseña saca a quien tuviera la sesión abierta en otro teléfono en máximo `JWT_TTL_MINUTES`. Si le cambiaron el
  rol, el token nuevo ya trae el rol nuevo.
- **Cerrar sesión** (`POST /v1/auth/logout`) guarda el `sid` en la tabla `jwt_blocklist` hasta el tope de la sesión, y
  `RevokedTokenValidator` rechaza cualquier token cuyo `jti` o `sid` esté ahí. Por eso el logout invalida **todos** los
  tokens de esa sesión (el actual y los que se renovaron antes), no solo el último. Los tokens de otros dispositivos del
  mismo usuario son otras sesiones y no se tocan.
- Los avisos que el socio cerró (HU-42) se guardan por sesión, no por token: renovar el token no los vuelve a mostrar.
- Renovar el token no se guarda en la auditoría (pasaría cada pocos minutos); cerrar sesión sí.
- La tabla `jwt_blocklist` no tiene limpieza automática todavía: guarda una fila por cada cierre de sesión y se consulta
  por llave primaria, así que su tamaño no afecta la velocidad.

### Rol Coach (RNF-02)

El rol `COACH` ya existe en el código y en la base de datos, pero **no tiene permisos sobre ningún módulo**, porque el
documento de requerimientos deja "por confirmar" si el coach tendrá cuenta propia. Un usuario `COACH` puede iniciar
sesión, ver su perfil (`GET /v1/users/me`), renovar su token y cerrar sesión; cualquier otra ruta responde `403` y queda
en la auditoría como intento no autorizado.

Hoy no hay endpoint para crear cuentas `COACH` (`POST /v1/users` solo crea admins). Cuando el negocio decida qué puede
hacer un coach (por ejemplo, ver y editar las rutinas), hay que: permitir el rol en `POST /v1/users`, agregar
`Role.COACH` a las reglas de esos endpoints en `SecurityConfiguration` y decidir si el coach se liga a un registro de
`/v1/coaches` (hoy los coaches son solo un catálogo con nombre y foto).

### Recuperación de contraseña

`POST /v1/auth/password-resets` con `{ "email": "..." }` genera un código de un solo uso que vence en 30 minutos, lo
guarda en `password_reset_token` y lo manda por correo a esa dirección. Responde `204` siempre, exista o no el correo,
para no revelar quién tiene cuenta. Con el código, `POST /v1/auth/password-resets/confirm` con
`{ "token": "...", "newPassword": "..." }` cambia la contraseña y cierra las sesiones que estaban abiertas.

El correo sale en español, en texto plano:

```
Asunto: Recupera tu contraseña de AFC Gym

Hola Ana López:
Recibimos una solicitud para cambiar la contraseña de tu cuenta de AFC Gym.
Para elegir una nueva, abre este enlace:
afcapp://recuperar?token=1f586c...
Si la app te lo pide, copia este código:
1f586c...
Solo sirve una vez y vence a las 14:10 del 25/11/2026.
Si no fuiste tú, ignora este correo: tu contraseña no cambia.
```

- **Se envía por SMTP**, así que sirve con cualquier proveedor: el correo del dominio, Gmail o Google Workspace (con una
  contraseña de aplicación), Outlook/Microsoft 365, SendGrid, Amazon SES, Brevo, etc. Se configura con `MAIL_HOST`,
  `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` y `MAIL_FROM` (ver la tabla de variables). Con el puerto 465 usa SSL
  directo; con cualquier otro (normalmente 587) usa STARTTLS.
- **El enlace es opcional:** si se configura `PASSWORD_RESET_URL` (por ejemplo el deep link de la app,
  `afcapp://recuperar`, o una página web), el correo trae ese enlace con `?token=...` al final. Si no, solo trae el
  código para copiarlo.
- **El correo se manda después de guardar el código y en segundo plano:** la respuesta no espera al servidor de correo
  (tarda lo mismo con un correo que existe que con uno que no), y si el servidor de correo falla, el error queda en el
  log y la petición no se cae.
- **Si `MAIL_HOST` o `MAIL_FROM` no están configurados**, el código se guarda igual pero no se envía, y el log avisa
  "email is not configured". El código nunca se escribe en el log.
- `GET /actuator/health` no revisa el servidor de correo, para que una falla del correo no marque toda la API como caída.

### Membresías y pagos

`POST /v1/members/{id}/payments` renueva la membresía de un socio. El comportamiento cambia según `method`:

| Método | Qué pasa |
|---|---|
| `CARD` | Se aprueba al instante (`status: APPROVED`) y la membresía se renueva en la misma operación. Requiere `cardLastFour` (4 dígitos). |
| `SPEI` | Queda `PENDING_BANK_VALIDATION`; la membresía **no** se renueva hasta aprobarla con `PATCH /v1/payments/{id}/approve` (o se rechaza con `PATCH /v1/payments/{id}/reject`). |
| `CASH` | Queda `PENDING`; requiere `scheduledDate` (fecha de la cita en recepción) y se aprueba igual que SPEI. |

Este endpoint es el de **recepción** (lo usa el admin). El socio paga desde la app con los flujos de la sección
"Flujos de pago del socio".

El monto se calcula solo: precio base del plan (`membershipType.basePrice`, valores de ejemplo en
`MembershipType.java`, **a confirmar con el negocio**) menos el descuento por racha de asistencia (HU-17): 10% a
partir de 80 días hábiles consecutivos de check-in, 20% a partir de 150. La racha se calcula en
`utils/AttendanceStreakCalculator.java` a partir del historial real de `check_in`, nunca a mano.

`GET /v1/members/{id}/payments` da el historial completo (HU-18), más reciente primero.

**Simplificación a confirmar con el negocio:** el HU-16 original describe el pago en efectivo como confirmado "al
llegar el socio" (vía check-in). Aquí se simplificó: tanto SPEI como efectivo quedan `PENDING` y se aprueban con el
mismo endpoint genérico (`PATCH /v1/payments/{id}/approve`), en vez de acoplar el módulo de pagos al de check-in.

### Tienda e inventario

- Categorías (`/v1/categories`) y proveedores (`/v1/suppliers`) son catálogos simples (alta + listado). Un producto
  puede tener o no categoría/proveedor; si se manda un id que no existe, responde `404`.
- `POST /v1/products` exige `code` (único, `409` si se repite), `name` y `price`. `stock` arranca en 0 y `minStock`
  en 5 si no se mandan.
- `lowStock` en la respuesta se calcula al vuelo (`stock <= minStock`, HU-23), no se guarda.
  `GET /v1/products?lowStock=true` lista solo los productos en alerta; `?search=` busca por nombre o código y
  `?categoryId=` deja solo los de esa categoría (HU-21). Los tres filtros son opcionales y se combinan:
  `GET /v1/products?search=prote&categoryId=1&lowStock=true`.
- `POST /v1/products/{id}/inventory-movements` registra una entrada de inventario (compra, HU-22): suma al stock,
  guarda cantidad, costo unitario, stock resultante y el id del admin que la registró (sale del token).
  `GET` sobre la misma ruta da el historial de movimientos.
- "Baja de producto" es baja lógica (`active = false`), igual que con socios: no se borra porque su historial de
  movimientos (y, más adelante, sus ventas) apuntan a él.

### Ventas / POS

- `POST /v1/sales/stock-validation` (HU-26) revisa un carrito **sin vender nada**: `204` si todo alcanza, `409` con el
  stock real si no (`"Stock insuficiente para Shaker: disponible 5"`). Sirve para que el front valide al agregar al
  carrito; si el mismo producto viene en varias líneas, se suman antes de comparar.
- `POST /v1/sales` (HU-27) registra la venta completa:

```json
{ "items": [ { "productId": 1, "quantity": 2 }, { "productId": 2, "quantity": 1 } ], "paymentMethod": "CASH" }
```

  Vuelve a validar el stock (el carrito pudo cambiar), descuenta cada producto, deja un movimiento de inventario tipo
  `SALE` por producto y genera el folio `V-000001`. Todo pasa en una sola transacción: si un producto no alcanza, no se
  descuenta ninguno. Cada producto se bloquea mientras se vende (`PESSIMISTIC_WRITE`), así dos cajas que venden lo
  último al mismo tiempo no dejan el stock en negativo (probado con 10 ventas simultáneas sobre stock 5: 5 pasan, 5
  reciben `409`, stock final 0). El cajero sale del token.
- El precio y el nombre se copian a cada renglón del ticket (`sale_item`): si después cambias el precio de un
  producto, las ventas pasadas no se alteran.
- `GET /v1/sales?from=2026-11-01&to=2026-11-17` (HU-28): `from` y `to` son obligatorios (`400` si faltan, o si `from`
  es posterior a `to`) y ambos días se incluyen completos. Cada venta trae folio, productos, total y método de pago.
- `GET /v1/sales/{id}` da un ticket con sus renglones.
- `GET /v1/sales/export?from=2026-11-01&to=2026-11-30` descarga el mismo historial como Excel
  (`ventas_2026-11-01_2026-11-30.xlsx`), con las mismas reglas de fechas. Arriba trae los filtros aplicados (desde,
  hasta, cuándo se generó), cuántas ventas hubo y el total vendido; luego una fila por venta con folio, fecha y hora,
  método de pago, productos (`2 x Proteína, 1 x Shaker`), quién atendió y total, y al final el total. Si no hubo ventas
  lo dice en el archivo en lugar de dejarlo vacío. Esto cubre el criterio general 6 de los requerimientos ("los
  reportes de corte de caja y de ventas conservan los filtros aplicados y son legibles al exportarse").

### Caja y corte de caja

`GET /v1/cash-cuts?period=WEEKLY&category=ALL&date=2026-11-17` (HU-29) calcula el corte **en el momento**, a partir
de los pagos y ventas reales; no se guarda en ninguna tabla.

| Parámetro | Valores | Obligatorio |
|---|---|---|
| `period` | `DAILY` (ese día), `WEEKLY` (de lunes a domingo de esa semana), `MONTHLY` (todo ese mes) | Sí |
| `category` | `MEMBERSHIPS`, `STORE` o `ALL` (las dos) | Sí |
| `date` | Cualquier día dentro del periodo que quieres ver. Si no se manda, es hoy. No puede ser futura | No |

- **Membresías** suma los pagos `APPROVED` según la fecha en que se aprobaron (cuando de verdad entró el dinero).
  Un SPEI o un efectivo que sigue `PENDING` no cuenta hasta que se aprueba.
- **Tienda** suma el total de cada venta según su fecha.
- Si una categoría no se pidió, su campo (`membershipIncome` o `storeIncome`) viene en `null`, no en 0, para no
  confundir "no se pidió" con "no se vendió nada".
- `paymentMethods` (HU-30) siempre trae los tres métodos (`CARD`, `SPEI`, `CASH`), con monto, porcentaje y número de
  operaciones. Con `ALL` los porcentajes se calculan sobre el total combinado, no promediando los de cada categoría.
  Si no hubo ingresos, todo sale en 0 (no hay división entre cero).

`GET /v1/cash-cuts/export` (HU-31) recibe los mismos parámetros y descarga el corte como Excel
(`corte-de-caja_semanal_todo_2026-11-16_2026-11-22.xlsx`). El archivo trae arriba los filtros aplicados (periodo,
categoría, fechas y cuándo se generó), luego los ingresos por categoría y el desglose por método de pago. En Swagger
aparece un enlace **Download file** en la respuesta.

### Cuenta de acceso del socio

Hasta ahora un socio solo existía como ficha (folio, PIN, huella) y no podía iniciar sesión. Para que pueda reservar
clases desde la app, el admin le crea una cuenta con `POST /v1/members/{id}/account`:

```json
{ "username": "ana.socia", "email": "ana@correo.com", "password": "Secret-123" }
```

Eso crea un usuario con rol `SOCIO` ligado a esa ficha (`userId` aparece en la respuesta del socio). Con ese usuario
y contraseña el socio entra por el mismo `POST /v1/auth/login`. Un socio solo puede tener una cuenta (`409` si ya
tiene) y la contraseña pide al menos 8 caracteres. Si la olvida, usa la recuperación de contraseña (le llega un código a
su correo).

### Horarios, clases y capacidad

**Coaches** (`/v1/coaches`): catálogo simple (nombre y foto opcional), para asignarlos a los horarios.

**Bloques de horario** (`/v1/schedule-blocks`, HU-32 y HU-33): cada bloque es una clase que se repite cada semana.

```json
{ "dayOfWeek": "MONDAY", "startTime": "07:00", "endTime": "08:00", "maxCapacity": 20, "coachId": 1 }
```

- `maxCapacity` es obligatorio y mayor a cero; la hora de inicio tiene que ser anterior a la de fin. `coachId` es
  opcional.
- El **turno** (`shift`) no se manda: se calcula con la hora de inicio, `MORNING` (antes de las 12:00) o `EVENING`.
- `PATCH .../deactivate` lo suspende: ya nadie puede reservar, pero las reservas que ya existían **no se borran**.
  `PATCH .../activate` lo reabre.
- Al editar, no se puede cambiar el día de un bloque que tiene reservas futuras, ni bajar el cupo por debajo de las
  reservas que ya tiene alguna fecha (`409` con la fecha y el número de reservas).

**Aforo** (`GET /v1/schedule-blocks/occupancy`, HU-34): por cada bloque del día da reservas, lugares libres,
porcentaje de ocupación y coach, contando las reservas reales de esa fecha. Se elige el día con `date=2026-11-24`, o
con `dayOfWeek=MONDAY` (toma el próximo lunes, o hoy si hoy es lunes), o sin nada (hoy). El socio también puede verlo
para saber dónde hay lugar.

**Reservas** (`/v1/bookings`, HU-35 y HU-36):

```json
{ "scheduleBlockId": 1, "classDate": "2026-11-24" }
```

- Un socio reserva para sí mismo (se toma de su sesión). El admin puede reservar por un socio mandando además
  `memberId`.
- Se rechaza si el cupo ya está lleno, si el bloque está desactivado, si la fecha no cae en el día del bloque, si la
  clase ya empezó, si el socio ya tiene lugar en esa clase o si su membresía está vencida.
- Cada bloque se bloquea mientras se reserva, así dos socios no pueden quedarse con el último lugar al mismo tiempo
  (probado: 6 socios reservando a la vez una clase de cupo 2, entran 2 y los otros 4 reciben `409`).
- `PATCH /v1/bookings/{id}/cancel` libera el lugar en ese momento. Solo puede cancelar el socio dueño de la reserva
  (otro socio recibe `403`) o un admin. No se puede cancelar una clase que ya empezó.
- `GET /v1/bookings/me` le muestra al socio sus próximas reservas activas.

### Rutinas y ejercicios

**Alta (HU-37) y un solo set por día (HU-38)**: `POST /v1/routines`

```json
{
  "dayOfWeek": "MONDAY",
  "title": "Pierna",
  "description": "Calentar 10 minutos",
  "coachId": 1,
  "exercises": [
    { "name": "Sentadilla", "sets": 4, "reps": "10-12", "restSeconds": 90 },
    { "name": "Peso muerto", "sets": 3, "reps": "Al fallo", "videoUrl": "https://youtu.be/abc" }
  ]
}
```

- El coach y al menos un ejercicio son obligatorios; máximo 8 ejercicios. Los ejercicios se guardan en el orden en que
  se mandan. `reps` es texto libre (`"10-12"`, `"Al fallo"`), `restSeconds` es opcional.
- Solo puede haber una rutina por día: si el día ya tiene una, responde `409` con
  `"Ya hay una rutina para el lunes; edítala o elimínala primero"`. La base de datos también lo impide.
- `PUT /v1/routines/{id}` reemplaza la rutina completa. Los ejercicios que se mandan **con su `id`** se conservan (y
  conservan su video subido); los que vienen sin `id` se crean; los que no vienen se borran.
- `DELETE /v1/routines/{id}` borra la rutina (y sus videos) y deja el día libre.

**Video por ejercicio (HU-39)**: cada ejercicio tiene un video por URL **o** por archivo, nunca los dos.

- Por URL: el campo `videoUrl` del ejercicio (tiene que empezar con `http://` o `https://`).
- Por archivo: `POST /v1/routines/{id}/exercises/{exerciseId}/video`, formulario `multipart/form-data` con el campo
  `file`. Acepta MP4, MOV o WEBM de hasta 100 MB. Subir un archivo quita la URL que hubiera, y viceversa.
- `DELETE /v1/routines/{id}/exercises/{exerciseId}/video` le quita el video, sea URL o archivo.
- En la respuesta, cada ejercicio trae `videoType` (`URL`, `FILE` o nada) y `videoUrl`, que ya es la dirección para
  reproducirlo: la URL externa, o `/v1/exercise-videos/<archivo>` si es un archivo subido.
- `GET /v1/exercise-videos/<archivo>` sirve el video con soporte de `Range` (lo que usan los reproductores para
  adelantar). Es público a propósito: el reproductor de video del celular o del navegador no puede mandar el token, y el
  nombre del archivo es aleatorio (32 caracteres), así que no se puede adivinar.

**Rutina del día (HU-40)**: `GET /v1/routines/by-day` (hoy) o `?dayOfWeek=FRIDAY`. Trae coach, descripción y los
ejercicios con series, repeticiones, descanso y video. Si ese día no tiene rutina responde `404` con
`"No hay rutina asignada para el viernes"`.

### Anuncios

**Publicar (HU-41)**: `POST /v1/announcements`

```json
{
  "title": "Clases suspendidas",
  "message": "Las clases vespertinas se suspenden por mantenimiento.",
  "type": "URGENT",
  "startDate": "2026-11-24",
  "endDate": "2026-11-26"
}
```

- `title`, `message`, `type` y `startDate` son obligatorios. `type` puede ser `INFO`, `URGENT`, `PROMOTION` o
  `MAINTENANCE`.
- `endDate` es opcional: es el último día en que se muestra. Si no se manda, el anuncio sigue vigente hasta que un
  admin lo borre. No se acepta una fecha final anterior a la inicial, ni una que ya pasó.
- `GET /v1/announcements` le da al admin todos (pasados, vigentes y programados); `current` indica si hoy está vigente.
  Se calcula al vuelo, no se guarda.

**Ver los vigentes (HU-42)**: `GET /v1/announcements/active` trae solo los que aplican hoy: urgentes primero, luego
mantenimiento, informativos y promociones, y dentro de cada tipo los más recientes.

Para que un aviso cerrado no vuelva a salir en la misma sesión, la app llama a `POST /v1/announcements/{id}/dismiss`
cuando el socio lo cierra. El cierre se guarda con el identificador de la sesión: aunque cierre y abra la app no reaparece,
y al iniciar sesión de nuevo vuelve a mostrarse si sigue vigente. Renovar el token (`POST /v1/auth/refresh`) no cuenta
como sesión nueva.

**Eliminar (HU-43)**: `DELETE /v1/announcements/{id}`, solo admin. Se borra de verdad, junto con sus cierres, y deja de
mostrarse a los socios de inmediato.

### Dashboard (HU-44)

`GET /v1/dashboard/summary` arma la vista general del admin. Todo sale de los datos reales en ese momento; nada se
guarda ni está quemado:

| Bloque | Qué trae |
|---|---|
| `members` | Socios `active`, `expired` y `toPurge` (con las mismas reglas que el estado de cada socio, sin contar los dados de baja) y `newThisMonth` (altas del mes) |
| `income` | Ingresos del mes actual por membresías y por tienda, el total, el total del mes anterior y `changePercent` (cambio contra el mes anterior; vacío si el mes anterior fue 0) |
| `monthlyIncome` | Los últimos 6 meses, del más viejo al actual, para la gráfica |
| `storeSalesByCategory` | Ventas de tienda del mes por categoría de producto (monto y unidades), de mayor a menor. Los productos sin categoría salen como "Sin categoría" |
| `alerts.expiringMemberships` | Socios cuya membresía vence entre hoy y los próximos días (3 por defecto; se cambia con `?expiringWithinDays=7`, de 1 a 30), con los días que les quedan |
| `alerts.lowStockProducts` | Productos activos con `stock <= minStock`, los más escasos primero |

Los ingresos se calculan con el mismo servicio que el corte de caja, así que el dashboard y el corte mensual siempre
dan la misma cifra.

### Notificaciones push (HU-45 a HU-47)

Las notificaciones salen del servidor por la API de push de Expo (la que ya usa `notificationService.js` del
frontend):

| Pieza | Cómo funciona |
|---|---|
| Registrar el dispositivo (HU-45) | El socio, con su sesión iniciada, envía el `ExpoPushToken` de su teléfono a `POST /v1/push-tokens` (`{ "token": "ExponentPushToken[...]" }`). Puede registrar varios dispositivos. Volver a enviar el mismo token no lo duplica, y si otro socio inicia sesión en ese teléfono, el token pasa a ser suyo |
| Recordatorio de vencimiento (HU-46) | Un proceso programado corre todos los días (`EXPIRY_REMINDER_CRON`) y avisa a los socios activos cuya membresía vence entre hoy y los próximos `EXPIRY_REMINDER_DAYS_BEFORE` días: "Tu membresía vence en 3 días (27/11/2026)". Se avisa una sola vez por cada fecha de vencimiento (tabla `expiry_reminder`) |
| Cancelación si ya renovó (HU-46) | La decisión se toma al momento de enviar con la fecha de vencimiento actual: si el socio ya renovó, su fecha ya salió de la ventana y no recibe nada |
| Aviso de renovación (HU-47) | En cuanto un pago queda aprobado (tarjeta al instante, o SPEI/efectivo cuando el admin lo aprueba) le llega "Tu pago ya se recibió. Tu membresía quedó renovada. Vence el ..." a todos sus dispositivos |

Detalles:

- El aviso de renovación se manda **después** de guardar el pago. Si Expo falla o tarda, el pago ya quedó registrado y
  la respuesta al admin no cambia; el error solo se anota en el log.
- Si Expo no acepta el recordatorio (sin internet, Expo caído), no se marca como enviado y se vuelve a intentar en la
  siguiente corrida mientras siga dentro de la ventana.
- Si Expo responde que un dispositivo ya no existe (`DeviceNotRegistered`, por ejemplo porque desinstalaron la app), su
  token se borra solo.
- Socios sin dispositivo registrado no reciben nada y no se marca recordatorio: si registran su teléfono después, aún
  dentro de la ventana, les llega.
- Cada mensaje trae en `data` el tipo (`EXPIRY_REMINDER` o `MEMBERSHIP_RENEWED`) y `dueDate`, para que la app sepa a qué
  pantalla llevar al tocarlo.

Para que el frontend lo use: después de `registerForPushNotificationsAsync()` en `MemberHomeScreen`, mandar el token
que regresa a `POST /v1/push-tokens`, y quitar la notificación local de "¡Renovación Exitosa!" que hoy dispara el propio
teléfono, porque el servidor ya manda la suya (si no, el socio recibiría dos). Las notificaciones push solo llegan a un
teléfono real; en web no aplican.

### Flujos de pago del socio (documento de alcance de pagos)

El socio renueva su membresía desde la app con `POST /v1/payments/me`
(`{ "membershipType": "MONTHLY", "method": "CARD" | "SPEI" | "CASH", "appointmentAt": "2026-11-25T18:00" }`).
El monto siempre lo calcula el servidor (precio del plan menos el descuento por racha); el cliente nunca lo manda.
Cada pago tiene una referencia legible (`PAG-000123`). Un socio solo puede tener **un pago en proceso** a la vez:
si quiere cambiar de método, cancela el actual con `PATCH /v1/payments/me/{id}/cancel`.

**A. Tarjeta con Mercado Pago (cobro automático)**

1. `POST /v1/payments/me` con `CARD` crea el pago en `AWAITING_GATEWAY` y una preferencia de Checkout Pro en Mercado
   Pago; la respuesta trae `checkoutUrl`. La app abre esa URL y el socio paga en la página de Mercado Pago: los datos
   de la tarjeta nunca pasan por este backend.
2. Mercado Pago avisa a `POST /v1/payment-webhooks/mercado-pago`. El backend:
   - comprueba la firma `x-signature` con `MERCADO_PAGO_WEBHOOK_SECRET` (si no coincide responde `401` y no hace nada);
   - **no confía en el contenido del aviso**: consulta el pago directo en la API de Mercado Pago;
   - verifica que la referencia sea un pago con tarjeta de este gimnasio y que el monto y la moneda (MXN) coincidan;
   - bloquea el pago en la base de datos mientras lo procesa, así que si el aviso llega repetido (Mercado Pago reintenta)
     la membresía se renueva **una sola vez**.
3. Si el cobro se aprueba: el pago pasa a `APPROVED`, se renueva la membresía y le llega el push "Tu pago ya se recibió".
   Si se rechaza: pasa a `GATEWAY_REJECTED` y el socio puede intentar de nuevo con otro pago.
4. La app sigue el estado con `GET /v1/payments/me/{id}`. El admin no puede aprobar a mano un pago con tarjeta.

Solo se aceptan tarjetas (se excluyen OXXO y cajeros, que dejarían el pago pendiente varios días) y en una sola
exhibición. La liga de pago vence a las 24 horas.

**B. Transferencia (validación manual del admin)**

1. `POST /v1/payments/me` con `SPEI` deja el pago en `PENDING_BANK_VALIDATION` y la respuesta trae `bankTransfer`
   (banco, beneficiario, CLABE y la referencia para el concepto). Los datos bancarios salen de la configuración
   (`BANK_TRANSFER_*`), no del frontend.
2. El admin revisa su banca y:
   - confirma con `PATCH /v1/payments/{id}/approve` → `APPROVED`, se guarda la fecha y qué admin aprobó
     (`approvedAt`, `approvedBy`), se renueva la membresía y llega el push "Tu pago ya se recibió";
   - o, si el depósito no llegó, rechaza con `PATCH /v1/payments/{id}/reject` (`{ "reason": "..." }`, obligatorio)
     → `REJECTED`, se guarda motivo, fecha y qué admin rechazó, y al socio le llega "No recibimos tu transferencia" con el
     motivo. El socio puede iniciar otro pago.

**C. Efectivo con cita (confirmación del admin)**

1. `POST /v1/payments/me` con `CASH` y `appointmentAt` (futura, máximo `PAYMENT_APPOINTMENT_MAX_DAYS_AHEAD` días) deja
   el pago en `PENDING_APPOINTMENT` y crea una cita `PENDING` ligada al socio, al pago y al monto.
2. El admin consulta `GET /v1/payment-appointments`: citas pendientes y próximas aceptadas, ordenadas por hora, con
   datos del socio (folio, nombre, teléfono) y del pago (referencia, plan, monto).
3. `PATCH /v1/payment-appointments/{id}/accept` → cita `ACCEPTED` y pago `APPOINTMENT_CONFIRMED`. Responde `409` si ya
   hay otra cita aceptada a menos de `PAYMENT_APPOINTMENT_MINUTES` minutos. Al socio le llega "Cita confirmada".
4. `PATCH /v1/payment-appointments/{id}/cancel` (`{ "reason": "...", "suggestedAt": "..." }`, ambos opcionales)
   → cita `CANCELLED` y pago `APPOINTMENT_CANCELLED`. Al socio le llega el motivo y el horario sugerido; puede
   reagendar con `PATCH /v1/payments/me/{id}/appointment` (vuelve a `PENDING_APPOINTMENT`) o cancelar y elegir otro
   método.
5. Cuando el socio paga en recepción, el admin lo confirma con `PATCH /v1/payments/{id}/approve` → pago `APPROVED`
   y cita `COMPLETED`, que sale de la agenda y deja libre ese horario.

| Estado del pago | Significa |
|---|---|
| `AWAITING_GATEWAY` | Esperando que el socio pague en Mercado Pago |
| `GATEWAY_REJECTED` | Mercado Pago rechazó el cobro |
| `PENDING_BANK_VALIDATION` | Transferencia esperando que el admin la confirme |
| `PENDING_APPOINTMENT` | Pago en efectivo con cita por confirmar |
| `APPOINTMENT_CONFIRMED` | Cita aceptada; falta que el socio pague |
| `APPOINTMENT_CANCELLED` | Recepción canceló la cita; el socio puede reagendar o cambiar de método |
| `PENDING` | Pago en efectivo registrado en recepción (endpoint del admin) |
| `APPROVED` | Pagado; la membresía ya se renovó |
| `REJECTED` | Transferencia rechazada por el admin |
| `CANCELLED` | El socio canceló el pago antes de pagarlo |

**Cómo conectar Mercado Pago (una vez):**

1. Crear una aplicación en el panel de desarrolladores de Mercado Pago (producto Checkout Pro).
2. Copiar el access token de prueba a `MERCADO_PAGO_ACCESS_TOKEN`.
3. En la sección **Webhooks** de la aplicación, registrar la URL pública
   `https://<dominio>/v1/payment-webhooks/mercado-pago`, marcar el evento **Pagos** y copiar la clave secreta que
   genera a `MERCADO_PAGO_WEBHOOK_SECRET`. Poner la misma URL en `MERCADO_PAGO_NOTIFICATION_URL`.
4. Para probar en local, el webhook necesita que el backend sea accesible desde internet (por ejemplo con un túnel).
5. Al pasar a producción, cambiar a las credenciales de producción; el backend usa la liga de pruebas solo cuando el
   token empieza con `TEST-`.

**Lo que le toca al frontend** (nosotros no lo tocamos):

- Cambiar el formulario de tarjeta de `PaymentModal.js` (hoy pide número y CVV) por abrir `checkoutUrl`, por ejemplo
  con `expo-web-browser`, y al volver consultar `GET /v1/payments/me/{id}`.
- Mostrar los datos de `bankTransfer` que manda el backend en lugar de la CLABE fija.
- Mandar fecha y hora reales de la cita (`appointmentAt`) en lugar del texto libre "Hoy por la tarde".
- Traducir los estados de la tabla de arriba.

### Probarlo desde Swagger (en desarrollo)

1. Abre http://localhost:8091/swagger-ui.html.
2. En `POST /v1/auth/login` pulsa **Try it out** y envía:

```json
{ "username": "admin", "password": "Admin12345!" }
```

3. Copia el valor de `token` de la respuesta.
4. Pulsa el botón **Authorize** (arriba a la derecha), pega el token y confirma.
5. Para registrar un socio usa `POST /v1/members` (el folio se genera solo, la fecha de vencimiento se calcula según
   el tipo de membresía: `WEEKLY`, `BIWEEKLY`, `MONTHLY`, `QUARTERLY` o `YEARLY`):

```json
{ "fullName": "Ana Test", "phone": "5512345678", "membershipType": "MONTHLY" }
```

6. El socio recién creado no tiene PIN ni huella todavía (eso es otro paso, HU-12). Regístralo con
   `PATCH /v1/members/{id}/credentials`:

```json
{ "pin": "1234" }
```

7. Para hacer el check-in usa `POST /v1/check-ins` con ese mismo `id`:

```json
{ "memberId": 1, "pin": "1234" }
```

8. `GET /v1/members?search=ana&status=ACTIVE` busca por nombre/folio/teléfono y filtra por estado
   (`ACTIVE`, `EXPIRED`, `TO_PURGE` — se calcula en el servidor a partir de la fecha de vencimiento, nunca se guarda).

El usuario `admin` con contraseña `Admin12345!` existe **solo en desarrollo** (perfil `dev`, base H2 local). Nunca se usa en producción.

## Rendimiento (RNF-08)

El requerimiento pide que las consultas habituales respondan en 3 segundos o menos. Se midió con el jar de producción
sobre una base H2 en archivo (modo PostgreSQL) cargada con un volumen mayor al de un gimnasio real después de varios
años: 5,000 socios, 300,000 asistencias, 60,200 pagos, 500 productos, 60,000 ventas con 120,000 renglones, 50,000
movimientos de inventario, 100,000 registros de auditoría y 50,000 reservas. Cada consulta se repitió 10 veces después
de calentar.

| Consulta | Mediana | Peor caso |
|---|---|---|
| Dashboard | 196 ms | 296 ms |
| Socios: lista completa (5,000) | 60 ms | 84 ms |
| Socios: búsqueda por nombre / filtro vencidos | 24 / 42 ms | 48 / 47 ms |
| Historial de pagos de un socio | 17 ms | 21 ms |
| Productos: catálogo, búsqueda, stock bajo | 16 a 19 ms | 41 ms |
| Ventas del mes / exportarlas a Excel | 49 / 74 ms | 64 / 88 ms |
| Corte de caja mensual / exportarlo a Excel | 19 / 26 ms | 43 / 31 ms |
| Aforo del día, horarios, rutinas, agenda de citas | 12 a 20 ms | 26 ms |
| Auditoría (página de 50) | 16 ms | 21 ms |
| Socio: mis pagos, anuncios vigentes | 12 a 14 ms | 36 ms |
| Renovar membresía (calcula la racha de asistencia) | 19 ms | 32 ms |
| Registrar una venta | 19 ms | 21 ms |
| Login | 425 ms | 450 ms |
| Renovar token | 11 ms | 31 ms |
| Fuera de lo habitual: ventas de 11 meses / exportarlas | 258 / 334 ms | 315 / 371 ms |

Con 20 clientes a la vez haciendo 200 consultas mezcladas (dashboard, socios, ventas, productos, aforo, corte): mediana
157 ms, 95 % por debajo de 481 ms y la más lenta 640 ms. Todo queda muy por debajo de los 3 segundos.

- El login tarda ~0.4 s a propósito: la contraseña se compara con BCrypt costo 12, que es lento para que adivinar
  contraseñas sea caro.
- La medición fue en H2 porque en esta máquina no hay PostgreSQL. H2 crea índices para las llaves foráneas por su
  cuenta y PostgreSQL no, por eso `V13__query_indexes.sql` agrega los índices de las columnas por las que se filtra
  (vencimiento y alta de socios, asistencias por socio, pagos aprobados por fecha, movimientos por producto, productos
  por categoría y proveedor, citas por socio). Conviene repetir la medición en el servidor de producción.
- La búsqueda de socios por nombre recorre la tabla completa (busca el texto en cualquier parte del nombre, y eso no
  aprovecha un índice normal). Con 5,000 socios tarda 24 ms; solo con cientos de miles valdría la pena un índice de
  texto (`pg_trgm`).

## Base de datos y migraciones

El esquema lo crea Flyway al arrancar (`src/main/resources/db/migration`); Hibernate solo lo valida.
Una migración ya aplicada nunca se edita: se crea una nueva (`V2__description.sql`).

| Migración | Contenido |
|---|---|
| `V1__initial_schema.sql` | `app_user`, `member`, `password_reset_token`, `check_in`, `jwt_blocklist` |
| `V2__member_profile.sql` | Amplía `member` con `code` (folio), `phone`, `email`, `membership_type` y `due_date`. Si ya tenías socios de prueba de antes, la migración les asigna folio/membresía/vencimiento por defecto en vez de fallar. |
| `V3__payment.sql` | Tabla `payment` (método, estado, monto, descuento, folio de referencia de tarjeta, fecha de cita) |
| `V4__store.sql` | Tablas `category`, `supplier`, `product` e `inventory_movement` |
| `V5__sales.sql` | Tablas `sale` (folio, total, método de pago, cajero) y `sale_item` (renglones del ticket) |
| `V6__schedules.sql` | Tablas `coach`, `schedule_block` y `booking`, y la columna `member.user_id` que liga a un socio con su cuenta de acceso |
| `V7__routines.sql` | Tablas `routine` (una por día de la semana) y `exercise` (con su URL o archivo de video, nunca ambos) |
| `V8__announcements.sql` | Tablas `announcement` y `announcement_dismissal` (qué avisos cerró cada sesión) |
| `V9__push_notifications.sql` | Tablas `push_token` (dispositivos de cada socio, un token no se repite) y `expiry_reminder` (qué recordatorio de vencimiento ya se envió, uno por socio y fecha) |
| `V10__payment_flows.sql` | Nuevos estados de `payment`, quién aprobó o rechazó, motivo de rechazo, datos de Mercado Pago (preferencia, id del cobro, liga de pago) y la tabla `payment_appointment` (citas para pagar en efectivo) |
| `V11__audit_log.sql` | Tabla `audit_log` (historial de cambios del admin e intentos no autorizados) |
| `V12__coach_role.sql` | Permite el rol `COACH` en `app_user` y en `audit_log` |
| `V13__query_indexes.sql` | Índices para las consultas frecuentes en PostgreSQL (ver "Rendimiento") |
| `V14__credentials_changed_at.sql` | Guarda cuándo cambió la contraseña de cada usuario, para cerrar las sesiones anteriores |
| `V15__appointment_completed_status.sql` | Nuevo estado `COMPLETED` para las citas de efectivo ya cobradas |

El estado del socio (`ACTIVE`/`EXPIRED`/`TO_PURGE`) **no se guarda en la base de datos**: se calcula siempre al vuelo
a partir de `due_date`, como pide Requerimientos-AFC-Gym.docx ("nunca se confía en un valor enviado por el cliente").

## Arquitectura

Todo el código se organiza por capa técnica (no por feature), calcando carpeta por carpeta el proyecto de referencia
que diste (`AaaS_API`): `persistance` (`entities` + `repositories`), `service` (+ `service.dto`, `service.impl`),
`mapper`, `web` (`controllers` + `dto`), `configuration` (incl. `configuration.security`), `exception` y `utils`.

Endpoints actuales: `POST /v1/auth/login`, `POST /v1/auth/refresh`, `POST /v1/auth/logout`, `POST /v1/auth/password-resets`,
`POST /v1/auth/password-resets/confirm`, `POST /v1/users`, `GET /v1/users/me`, `POST /v1/members`, `GET /v1/members`,
`GET /v1/members/{id}`, `PUT /v1/members/{id}`, `PATCH /v1/members/{id}/deactivate`,
`PATCH /v1/members/{id}/credentials`, `POST /v1/members/{id}/payments`, `GET /v1/members/{id}/payments`,
`PATCH /v1/payments/{id}/approve`, `POST /v1/categories`, `GET /v1/categories`, `POST /v1/suppliers`,
`GET /v1/suppliers`, `POST /v1/products`, `GET /v1/products`, `GET /v1/products/{id}`, `PUT /v1/products/{id}`,
`PATCH /v1/products/{id}/deactivate`, `POST /v1/products/{id}/inventory-movements`,
`GET /v1/products/{id}/inventory-movements`, `POST /v1/sales/stock-validation`, `POST /v1/sales`, `GET /v1/sales`,
`GET /v1/sales/export`, `GET /v1/sales/{id}`, `GET /v1/cash-cuts`, `GET /v1/cash-cuts/export`, `POST /v1/members/{id}/account`,
`POST /v1/coaches`, `GET /v1/coaches`, `POST /v1/schedule-blocks`, `GET /v1/schedule-blocks`,
`GET /v1/schedule-blocks/occupancy`, `GET /v1/schedule-blocks/{id}`, `PUT /v1/schedule-blocks/{id}`,
`PATCH /v1/schedule-blocks/{id}/activate`, `PATCH /v1/schedule-blocks/{id}/deactivate`, `POST /v1/bookings`,
`GET /v1/bookings/me`, `PATCH /v1/bookings/{id}/cancel`, `POST /v1/routines`, `GET /v1/routines`,
`GET /v1/routines/by-day`, `GET /v1/routines/{id}`, `PUT /v1/routines/{id}`, `DELETE /v1/routines/{id}`,
`POST /v1/routines/{id}/exercises/{exerciseId}/video`, `DELETE /v1/routines/{id}/exercises/{exerciseId}/video`,
`GET /v1/exercise-videos/{archivo}`, `POST /v1/announcements`, `GET /v1/announcements`,
`GET /v1/announcements/active`, `POST /v1/announcements/{id}/dismiss`, `DELETE /v1/announcements/{id}`,
`GET /v1/dashboard/summary`, `POST /v1/push-tokens`, `POST /v1/payments/me`, `GET /v1/payments/me`,
`GET /v1/payments/me/{id}`, `PATCH /v1/payments/me/{id}/cancel`, `PATCH /v1/payments/me/{id}/appointment`,
`PATCH /v1/payments/{id}/reject`, `GET /v1/payment-appointments`, `PATCH /v1/payment-appointments/{id}/accept`,
`PATCH /v1/payment-appointments/{id}/cancel`, `POST /v1/payment-webhooks/mercado-pago`, `GET /v1/audit-logs`,
`POST /v1/check-ins`.

## Decisiones tomadas para poder construir esto ya (a confirmar con el negocio)

- El endpoint `POST /v1/users` solo crea cuentas `ADMIN` (HU-05); dar de alta la cuenta de acceso de un socio (si
  llega a necesitar una) sigue sin construirse.
- El PIN y el token de huella se guardan con el mismo hash (BCrypt) que las contraseñas; nunca en texto plano.
- El "lector de huella" sigue sin definirse a nivel de hardware (igual que se señaló en Requerimientos-AFC-Gym.docx):
  el backend solo compara un `fingerprintToken` opaco contra lo guardado, no procesa biometría real.
- Al dar de alta un socio, la fecha de vencimiento inicial se calcula sola (hoy + la duración del plan elegido); después
  solo la mueve un pago aprobado (ver "Membresías y pagos").
- "Baja de socio" (HU-10) es baja lógica (`active = false`), no borra el registro: así se conserva su historial de
  check-ins y no rompe la llave foránea de `check_in`. Si el socio tenía cuenta en la app, la cuenta también se
  desactiva: ya no puede iniciar sesión ni renovar su token.
- Si dos personas aceptan al mismo tiempo (en el mismo instante) dos citas de efectivo que se empalman, las dos pueden
  quedar aceptadas: la revisión de empalmes no bloquea toda la agenda. Con una sola recepción no pasa; si llega a
  pasar, el admin cancela una con horario sugerido.
- El folio (`code`) se genera como `SOC-######` a partir del id autogenerado; como el id no existe hasta guardar, el
  alta hace un guardado interno en dos pasos (no expuesto en la API, el cliente solo ve una llamada).
- La venta usa los mismos métodos de pago que las membresías (`CARD`, `SPEI`, `CASH`), pero en la tienda la venta se
  da por cobrada en el momento: no hay estado `PENDING` ni aprobación posterior como en los pagos de membresía.
- El HU-27 menciona a "recepción" como actor; como los roles con permisos son `ADMIN` y `SOCIO`, vender es de `ADMIN`.
- Vender más de lo que hay en stock, o un producto dado de baja, responde `409` (conflicto con el estado actual del
  inventario), no `400`: la petición está bien formada, lo que no alcanza es el stock.
- **El corte de caja solo reporta ingresos.** El frontend muestra "egresos" y "utilidad" con números fijos, pero no
  existe ningún registro de gastos; inventar esos campos sería dar datos falsos. Si el negocio quiere utilidad real,
  hace falta una nueva pieza (`Expense`/egresos: alta de gastos con monto, categoría y fecha) y entonces el corte
  restaría esos gastos. Mientras tanto la respuesta no trae `expenses` ni `profit`.
- La semana del corte va de lunes a domingo.
- El corte se calcula sumando en Java los pagos y ventas del periodo (no hay `@Query` con `SUM`/`GROUP BY`, por las
  reglas del proyecto). Para el volumen de un gimnasio (cientos o pocos miles de movimientos al mes) es inmediato; si
  algún día crece mucho, se puede pasar a consultas agregadas o a una tabla de cortes cerrados.
- La exportación es solo Excel (XLSX), que el HU-31 acepta ("PDF y/o XLSX"). Se usa la librería `fastexcel`, que es
  pequeña; un PDF requeriría otra librería y diseño de página, se puede agregar si el negocio lo pide.
- **Cuenta del socio:** para cumplir el HU-35 ("como socio, quiero reservar") hizo falta que el socio pudiera iniciar
  sesión, y eso no existía. Se resolvió con una cuenta `SOCIO` que crea el admin desde la ficha del socio. No hay
  auto-registro desde la app; si el negocio lo quiere, es una pieza aparte.
- El frontend tiene dos modelos de horario distintos (`AdminScheduleScreen` sin día de la semana y `CapacityScreen`
  con día). Se unificaron en uno solo: un bloque siempre tiene día de la semana, y la ocupación se calcula por fecha.
- El turno (matutino/vespertino) se calcula con la hora de inicio en vez de capturarse, para que nunca contradiga el
  horario.
- Para reservar se exige que la membresía esté vigente; no hay límite de con cuánta anticipación se puede reservar
  (si el negocio quiere, por ejemplo, "máximo 7 días antes", es un cambio chico).
- **Dónde se guardan los videos:** el HU-39 deja el almacenamiento "por decidir" (S3, Cloudinary, Supabase). Por
  ahora se guardan en una carpeta del servidor (`VIDEOS_DIRECTORY`). Funciona en desarrollo y en un servidor con disco
  propio; en un hosting sin disco persistente (muchos planes gratuitos) los videos se perderían al reiniciar. Cambiar a
  S3/Cloudinary solo toca `LocalVideoStorageServiceImpl` (todo lo demás usa la interfaz `IVideoStorageService`).
- Las rutinas son por día de la semana y las ven todos los socios; no se asignan a socios específicos (el HU-40 pide
  "la rutina correspondiente al día", no una rutina personal).
- Cuando un día no tiene rutina, el endpoint responde `404` con el mensaje en lugar de una respuesta vacía, para que el
  frontend pueda mostrar ese texto tal cual.
- El frontend usa dos juegos de tipos de anuncio (`info`/`urgent`/`maintenance` en admin y `alert`/`promo` en el socio).
  Se unificaron en `INFO`, `URGENT`, `MAINTENANCE` y `PROMOTION`; lo que hoy es `alert` corresponde a `URGENT`.
- El HU-41 pide "una fecha", pero para saber qué está vigente (HU-42) hace falta un rango: `startDate` obligatoria y
  `endDate` opcional. Sin fecha final el anuncio se queda hasta que lo borren, como sugiere el HU-43.
- Los cierres de avisos no tienen limpieza automática (igual que `jwt_blocklist`); se borran al eliminar su anuncio.
- El dashboard del frontend muestra tendencias de socios ("+5 este mes"). Saber cuántos socios estaban activos en una
  fecha pasada requeriría guardar historial de estados, que hoy no existe (el estado se calcula con la fecha de
  vencimiento). Por eso el resumen trae `newThisMonth` (altas del mes) en lugar de esa tendencia; la tendencia de
  ingresos (`changePercent`) sí es real.
- Las ventas por categoría usan la categoría actual del producto: si un producto cambia de categoría, sus ventas del mes
  se cuentan en la nueva.
- Los días de anticipación del recordatorio son configuración del servidor (`EXPIRY_REMINDER_DAYS_BEFORE`), no una
  pantalla del admin. Si el negocio quiere cambiarlo desde la app, es una pieza chica (guardar el valor en la base).
- No hay endpoint para dar de baja un dispositivo al cerrar sesión. Si otro socio inicia sesión en el mismo teléfono, el
  token pasa a ser suyo; si nadie vuelve a entrar, el teléfono sigue recibiendo los avisos del socio anterior hasta
  que desinstalen la app. Se puede agregar `DELETE` si el negocio lo pide.
- No se guarda un historial de notificaciones enviadas (bandeja "mis notificaciones"); solo lo necesario para no repetir
  recordatorios. El frontend no tiene esa pantalla.
- El proceso programado vive dentro de la misma aplicación. Si en producción se levantan varias copias del backend, cada
  una correría el recordatorio; la restricción única de `expiry_reminder` evita guardar dos veces, pero Expo podría
  recibir el mismo aviso dos veces en la misma corrida. Con una sola instancia (lo normal para un gimnasio) no pasa.
- **Alcance del documento de pagos:** habla de "orden" y "pedido", pero en la app el socio solo paga la renovación de
  su membresía (es el único pago del `PaymentModal`). Por eso los tres flujos son para renovaciones; el pago funciona
  como la orden (no hay una tabla de órdenes aparte). Los estados que el documento escribe en español
  (`pendiente_validacion_bancaria`, `cita_confirmada_efectivo`...) están en inglés, como el resto de la API.
- Si el socio cancela un pago con tarjeta pero alcanza a pagar en la página de Mercado Pago antes de que venza la liga,
  el cobro se respeta: el pago pasa a `APPROVED` y la membresía se renueva, porque el dinero sí se cobró.
- Reembolsos y contracargos de Mercado Pago no revierten la renovación en automático; se ven en el panel de Mercado
  Pago y se manejan a mano.
- Cada cita dura 30 minutos para detectar empalmes y no se valida contra un horario de atención (el documento no lo
  define). Ambos se pueden ajustar.
- El pago en efectivo registrado desde recepción (`POST /v1/members/{id}/payments`) sigue igual: queda `PENDING` y no
  usa citas.
- La auditoría registra los cambios **exitosos** del admin; un intento del admin que falla por validación (`400`) no se
  guarda porque no cambió nada. Las peticiones sin token (`401`) tampoco se guardan, para no llenar la tabla con
  tráfico anónimo.
- El registro de auditoría se escribe al terminar cada petición, en su propia transacción. Si la base de datos fallara
  justo en ese instante, el cambio quedaría hecho sin su registro (y el error queda en el log del servidor). Para un
  historial a prueba de todo, en producción se puede además quitarle al usuario de la base de datos el permiso de
  `UPDATE`/`DELETE` sobre `audit_log`.
- `action` se forma con el nombre del controlador y del método (`Members.deactivate`, `Payments.reject`), así que una
  pantalla de auditoría en el frontend puede traducirlo con una tabla.
