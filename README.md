# Customers API

API REST para registro de usuarios. Solución a la prueba técnica **Evaluación JAVA - Especialista Integración BCI**.

## Stack

- Java 21
- Spring Boot 3.5.13
- Spring Security (autenticación JWT)
- Spring Data JPA + Hibernate
- H2 (base de datos en memoria)
- jjwt 0.12.6 (generación y validación de JWT)
- Maven

## Requisitos previos

- JDK 21 instalado
- Maven 3.9+ (o usar el wrapper incluido `./mvnw`)

## Cómo ejecutar

```bash
./mvnw spring-boot:run
```

La aplicación arranca en `http://localhost:8080`.

### H2 Console

- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:customersdb`
- Usuario: `sa`
- Password: (vacío)

## Endpoints

### `POST /sign-up` (público)

Registra un nuevo usuario.

**Request:**

```json
{
  "name": "Juan Perez",
  "email": "juan@bci.cl",
  "password": "Abcdef12",
  "phones": [
    { "number": "1234567", "citycode": "1", "contrycode": "57" }
  ]
}
```

**Respuestas:**

- `201 Created` — usuario creado, retorna JSON con `id`, `created`, `modified`, `last_login`, `token` (JWT), `isactive`, y los phones.
- `400 Bad Request` — validaciones fallaron (formato email/password, campos faltantes).
- `409 Conflict` — email ya registrado.

### `GET /me` (requiere JWT)

Retorna los datos del usuario autenticado.

**Headers:**

```
Authorization: Bearer <token>
```

**Respuestas:**

- `200 OK` — datos del usuario.
- `401 Unauthorized` — token ausente o inválido.
- `404 Not Found` — usuario no existe en la BD.

## Ejemplo con curl

```bash
# Registrar usuario
curl -X POST http://localhost:8080/sign-up \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Juan Perez",
    "email": "juan@bci.cl",
    "password": "Abcdef12",
    "phones": [{ "number": "1234567", "citycode": "1", "contrycode": "57" }]
  }'

# Consultar usuario autenticado (usar el token recibido)
curl -X GET http://localhost:8080/me \
  -H "Authorization: Bearer eyJhbGc..."
```

## Formato de error

Todas las respuestas de error siguen el formato:

```json
{ "mensaje": "descripción del error" }
```

## Validaciones

- **Email**: formato por regex configurable (`app.email.regex`).
- **Password**: formato por regex configurable (`app.password.regex`).
- **Email único**: no se permite registrar dos usuarios con el mismo correo.
- **Phones**: al menos un teléfono con `number`, `citycode` y `contrycode` obligatorios.

## Propiedades configurables

En `src/main/resources/application.properties`:

| Propiedad | Descripción | Default |
|---|---|---|
| `app.password.regex` | Regex de password | 8+ chars, 1 mayúscula, 1 minúscula, 1 dígito |
| `app.password.message` | Mensaje de error de password | personalizado |
| `app.email.regex` | Regex de email | formato estándar |
| `app.email.message` | Mensaje de error de email | personalizado |
| `app.jwt.secret` | Secret para firmar JWT (HS256) | clave de dev |
| `app.jwt.expiration-ms` | Vigencia del JWT en ms | 86400000 (24h) |

## Arquitectura

```
controller/         Capa HTTP (UserController)
service/            Lógica de negocio (UserService, JwtService)
repository/         Acceso a datos (UserRepository)
domain/             Entidades JPA (User, Phone)
dto/                DTOs de request/response
validation/         Custom validators (@ValidEmail, @ValidPassword)
security/           Filtro JWT (JwtAuthenticationFilter)
exception/          Excepciones custom + GlobalExceptionHandler
config/             SecurityConfig
```

## Modelo de datos

- **users** (id UUID, name, email único, password hasheado BCrypt, created, modified, last_login, token JWT, is_active)
- **phones** (id UUID, number, city_code, country_code, user_id FK)

Relación: `User` 1—N `Phone`.

## Seguridad

- Passwords almacenados con **BCrypt** (salt integrado).
- Autenticación stateless con **JWT** firmado con HS256.
- Endpoints públicos: `/sign-up`, `/h2-console/**`.
- Resto de endpoints: requieren `Authorization: Bearer <token>`.

## Notas de diseño

- **Token**: JWT real firmado, no UUID placeholder.
- **Validaciones por regex configurables**: custom validators (`@ValidEmail`, `@ValidPassword`) que leen la regex desde properties vía `@Value`.
- **Inyección por constructor** (no `@Autowired` en campos) en todos los beans.
- **`saveAndFlush`** en el service para forzar el INSERT y obtener los timestamps autogenerados antes de mapear la respuesta.
- **Transacciones**: `@Transactional` en el método de registro garantiza atomicidad (si falla el guardado de phones, el user no queda huérfano).
