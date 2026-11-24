# Backend: AFC Gym Management System

API REST con Spring Boot 4.1 (Java 21), Spring Data JPA, Flyway y Swagger.
Arquitectura en capas (entity / repository / service / controller), organizada por capa técnica, no por feature.
Base de datos H2 en desarrollo y PostgreSQL en producción.

Este backend reemplaza los datos simulados del frontend de AFC App (Expo/React Native, en `../AFC-APP`). Cubre, del
cronograma (`Cronograma-backend-AFC-Gym.xlsx`): Infraestructura, Autenticación y acceso (HU-01 a HU-06), Gestión de
socios (HU-07 a HU-12), Membresías y pagos (HU-13 a HU-19), Tienda e inventario (HU-20 a HU-25), Ventas / POS
(HU-26 a HU-28), Caja y corte de caja (HU-29 a HU-31), Horarios, clases y capacidad (HU-32 a HU-36), Rutinas y
ejercicios (HU-37 a HU-40), Anuncios (HU-41 a HU-43), Dashboard (HU-44) y Notificaciones (HU-45 a HU-47) — ver
`Historias-de-Usuario-AFC-Gym.docx`.

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
caja), abre la ventana **Maven** (derecha) y pulsa **Reload All Maven Projects** para que la descargue.

### Si Maven falla con `PKIX path building failed`

Pasa cuando un antivirus o proxy inspecciona HTTPS. En IntelliJ:

1. **File > Settings > Build, Execution, Deployment > Build Tools > Maven > Importing**: en **VM options for importer** escribe `-Djavax.net.ssl.trustStoreType=Windows-ROOT`.
2. En **Maven > Runner**: en **VM Options** escribe lo mismo.
3. Abre la ventana **Maven** (derecha) y pulsa **Reload All Maven Projects**.

### Pruebas

Clic derecho sobre la carpeta `src/test/java` > **Run 'All Tests'**. Las 43 clases de prueba (341 pruebas) ya pasan en
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
| `BOOTSTRAP_ADMIN_PASSWORD` | Contraseña del primer usuario administrador. Solo se usa si todavía no existe ningún usuario | Solo la primera vez |
| `BOOTSTRAP_ADMIN_USERNAME`, `BOOTSTRAP_ADMIN_FULL_NAME`, `BOOTSTRAP_ADMIN_EMAIL` | Datos de ese primer usuario | No |
| `ALLOWED_ORIGINS` | Direcciones del frontend permitidas por CORS, separadas por comas | Sí, con el frontend desplegado |
| `VIDEOS_DIRECTORY` | Carpeta donde se guardan los videos subidos de los ejercicios (por defecto `./data/videos`). En producción tiene que ser un disco persistente | Sí, si se suben videos |
| `EXPO_ACCESS_TOKEN` | Token de acceso de Expo, solo si en el proyecto de Expo se activó "Enhanced push security" | No |
| `EXPIRY_REMINDER_DAYS_BEFORE` | Con cuántos días de anticipación se avisa que vence la membresía (por defecto 3, de 1 a 30) | No |
| `EXPIRY_REMINDER_CRON` | A qué hora corre el recordatorio de vencimientos, en formato cron de Spring (por defecto `0 0 9 * * *`, todos los días a las 9:00 hora del servidor) | No |

En producción Swagger y la documentación de la API quedan desactivados.

## Autenticación y roles

La API usa tokens JWT (válidos 1 hora). El token se envía en el encabezado `Authorization: Bearer <token>`.

**Roles:** `ADMIN` (recepción/administración) y `SOCIO` (miembro del gimnasio).

| Endpoint | Quién puede usarlo |
|---|---|
| `POST /v1/auth/login` | Cualquiera (es público) |
| `POST /v1/auth/password-resets` y `/confirm` | Cualquiera (son públicos) |
| `POST /v1/auth/logout` | Cualquier usuario con sesión iniciada |
| `GET /v1/users/me` | Cualquier usuario con sesión iniciada |
| `POST /v1/users` (crear otro admin) | Solo `ADMIN` |
| `POST /v1/members`, `GET /v1/members`, `GET /v1/members/{id}`, `PUT /v1/members/{id}`, `PATCH /v1/members/{id}/deactivate`, `PATCH /v1/members/{id}/credentials` | Solo `ADMIN` |
| `POST /v1/members/{id}/payments`, `GET /v1/members/{id}/payments`, `PATCH /v1/payments/{id}/approve` | Solo `ADMIN` |
| `POST`/`GET /v1/categories`, `POST`/`GET /v1/suppliers` | Solo `ADMIN` |
| `POST /v1/products`, `GET /v1/products`, `GET`/`PUT /v1/products/{id}`, `PATCH /v1/products/{id}/deactivate`, `POST`/`GET /v1/products/{id}/inventory-movements` | Solo `ADMIN` |
| `POST /v1/sales`, `POST /v1/sales/stock-validation`, `GET /v1/sales`, `GET /v1/sales/{id}` | Solo `ADMIN` |
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
| `POST /v1/check-ins` | Solo `ADMIN` |

Cada vez que se agregue un endpoint hay que decidir qué roles lo usan en `configuration/security/SecurityConfiguration`.

### Cerrar sesión con JWT sin estado

El token es JWT sin sesión en el servidor. `POST /v1/auth/logout` guarda el `jti` del token en la tabla `jwt_blocklist`
con su fecha de expiración; un `JwtDecoder` personalizado (`RevokedTokenValidator`) rechaza cualquier token cuyo `jti`
esté en esa lista. No hay limpieza automática de la tabla todavía; a este tamaño no hace falta.

### Recuperación de contraseña

`POST /v1/auth/password-resets` genera un token de 30 minutos y lo guarda en `password_reset_token`, pero **no existe
ningún canal de envío todavía** (correo o SMS, pendiente de definir con el negocio — ver Requerimientos-AFC-Gym.docx,
sección 5). Por ahora el token solo se puede consultar directamente en la base de datos; falta integrar el envío real
antes de que este flujo sea usable por un socio de verdad.

### Membresías y pagos

`POST /v1/members/{id}/payments` renueva la membresía de un socio. El comportamiento cambia según `method`:

| Método | Qué pasa |
|---|---|
| `CARD` | Se aprueba al instante (`status: APPROVED`) y la membresía se renueva en la misma operación. Requiere `cardLastFour` (4 dígitos). |
| `SPEI` | Queda `PENDING`; la membresía **no** se renueva hasta aprobarla con `PATCH /v1/payments/{id}/approve`. |
| `CASH` | Igual que SPEI, pero requiere `scheduledDate` (fecha de la cita en recepción). |

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
  `GET /v1/products?lowStock=true` lista solo los productos en alerta; `?search=` busca por nombre o código.
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
tiene) y la contraseña pide al menos 8 caracteres. Si la olvida, usa la recuperación de contraseña de siempre (con la
misma limitación: todavía no hay canal de envío).

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
cuando el socio lo cierra. El cierre se guarda con el identificador del token: aunque cierre y abra la app no reaparece,
y al iniciar sesión de nuevo vuelve a mostrarse si sigue vigente.

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
| Aviso de renovación (HU-47) | En cuanto un pago queda aprobado (tarjeta al instante, o SPEI/efectivo cuando el admin lo aprueba) le llega "Pago confirmado. Tu membresía quedó renovada. Vence el ..." a todos sus dispositivos |

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

El estado del socio (`ACTIVE`/`EXPIRED`/`TO_PURGE`) **no se guarda en la base de datos**: se calcula siempre al vuelo
a partir de `due_date`, como pide Requerimientos-AFC-Gym.docx ("nunca se confía en un valor enviado por el cliente").

## Arquitectura

Todo el código se organiza por capa técnica (no por feature), calcando carpeta por carpeta el proyecto de referencia
que diste (`AaaS_API`): `persistance` (`entities` + `repositories`), `service` (+ `service.dto`, `service.impl`),
`mapper`, `web` (`controllers` + `dto`), `configuration` (incl. `configuration.security`), `exception` y `utils`.
El detalle completo está en `.claude/skills/backend-conventions/SKILL.md`.

Endpoints actuales: `POST /v1/auth/login`, `POST /v1/auth/logout`, `POST /v1/auth/password-resets`,
`POST /v1/auth/password-resets/confirm`, `POST /v1/users`, `GET /v1/users/me`, `POST /v1/members`, `GET /v1/members`,
`GET /v1/members/{id}`, `PUT /v1/members/{id}`, `PATCH /v1/members/{id}/deactivate`,
`PATCH /v1/members/{id}/credentials`, `POST /v1/members/{id}/payments`, `GET /v1/members/{id}/payments`,
`PATCH /v1/payments/{id}/approve`, `POST /v1/categories`, `GET /v1/categories`, `POST /v1/suppliers`,
`GET /v1/suppliers`, `POST /v1/products`, `GET /v1/products`, `GET /v1/products/{id}`, `PUT /v1/products/{id}`,
`PATCH /v1/products/{id}/deactivate`, `POST /v1/products/{id}/inventory-movements`,
`GET /v1/products/{id}/inventory-movements`, `POST /v1/sales/stock-validation`, `POST /v1/sales`, `GET /v1/sales`,
`GET /v1/sales/{id}`, `GET /v1/cash-cuts`, `GET /v1/cash-cuts/export`, `POST /v1/members/{id}/account`,
`POST /v1/coaches`, `GET /v1/coaches`, `POST /v1/schedule-blocks`, `GET /v1/schedule-blocks`,
`GET /v1/schedule-blocks/occupancy`, `GET /v1/schedule-blocks/{id}`, `PUT /v1/schedule-blocks/{id}`,
`PATCH /v1/schedule-blocks/{id}/activate`, `PATCH /v1/schedule-blocks/{id}/deactivate`, `POST /v1/bookings`,
`GET /v1/bookings/me`, `PATCH /v1/bookings/{id}/cancel`, `POST /v1/routines`, `GET /v1/routines`,
`GET /v1/routines/by-day`, `GET /v1/routines/{id}`, `PUT /v1/routines/{id}`, `DELETE /v1/routines/{id}`,
`POST /v1/routines/{id}/exercises/{exerciseId}/video`, `DELETE /v1/routines/{id}/exercises/{exerciseId}/video`,
`GET /v1/exercise-videos/{archivo}`, `POST /v1/announcements`, `GET /v1/announcements`,
`GET /v1/announcements/active`, `POST /v1/announcements/{id}/dismiss`, `DELETE /v1/announcements/{id}`,
`GET /v1/dashboard/summary`, `POST /v1/push-tokens`, `POST /v1/check-ins`.

## Decisiones tomadas para poder construir esto ya (a confirmar con el negocio)

- El endpoint `POST /v1/users` solo crea cuentas `ADMIN` (HU-05); dar de alta la cuenta de acceso de un socio (si
  llega a necesitar una) sigue sin construirse.
- El PIN y el token de huella se guardan con el mismo hash (BCrypt) que las contraseñas; nunca en texto plano.
- El "lector de huella" sigue sin definirse a nivel de hardware (igual que se señaló en Requerimientos-AFC-Gym.docx):
  el backend solo compara un `fingerprintToken` opaco contra lo guardado, no procesa biometría real.
- Al dar de alta un socio, la fecha de vencimiento inicial se calcula sola (hoy + la duración del plan elegido); después
  solo la mueve un pago aprobado (ver "Membresías y pagos").
- "Baja de socio" (HU-10) es baja lógica (`active = false`), no borra el registro: así se conserva su historial de
  check-ins y no rompe la llave foránea de `check_in`.
- El folio (`code`) se genera como `SOC-######` a partir del id autogenerado; como el id no existe hasta guardar, el
  alta hace un guardado interno en dos pasos (no expuesto en la API, el cliente solo ve una llamada).
- La venta usa los mismos métodos de pago que las membresías (`CARD`, `SPEI`, `CASH`), pero en la tienda la venta se
  da por cobrada en el momento: no hay estado `PENDING` ni aprobación posterior como en los pagos de membresía.
- El HU-27 menciona a "recepción" como actor; como solo existen los roles `ADMIN` y `SOCIO`, vender es de `ADMIN`.
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
