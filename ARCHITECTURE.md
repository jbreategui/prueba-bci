# Arquitectura — Customers API

Diagramas en Mermaid con sintaxis C4. GitHub los renderiza nativamente.

---

## 1. Container Diagram (C4 nivel 2)

Vista de alto nivel: qué contenedores componen la solución y cómo interactúan.

```mermaid
C4Container
    title Container Diagram - Customers API

    Person(cliente, "Cliente", "Consumidor de la API (Postman, app móvil, otro servicio)")

    System_Boundary(api, "Customers API") {
        Container(spring, "Customers API", "Spring Boot 3.5 / Java 21", "Expone endpoints REST para registro y consulta de usuarios. Genera y valida JWT.")
        ContainerDb(h2, "Base de datos", "H2 en memoria", "Persiste usuarios y teléfonos")
    }

    Rel(cliente, spring, "POST /sign-up, GET /me", "HTTPS/JSON + Bearer JWT")
    Rel(spring, h2, "JPA/Hibernate", "JDBC")
```

---

## 2. Component Diagram (C4 nivel 3)

Componentes internos del servicio Spring Boot.

```mermaid
C4Component
    title Component Diagram - Customers API Internal

    Person(cliente, "Cliente")

    Container_Boundary(api, "Customers API") {
        Component(filter, "JwtAuthenticationFilter", "OncePerRequestFilter", "Valida JWT del header Authorization y popula el SecurityContext")
        Component(controller, "UserController", "@RestController", "Endpoints /sign-up y /me")
        Component(globalEx, "GlobalExceptionHandler", "@RestControllerAdvice", "Formatea errores como {mensaje: ...}")
        Component(validators, "Custom Validators", "ConstraintValidator", "@ValidEmail, @ValidPassword con regex configurable")
        Component(userService, "UserService", "@Service", "Lógica de registro: valida email único, hashea password, construye user + phones")
        Component(jwtService, "JwtService", "@Service", "Genera y valida tokens JWT (HS256)")
        Component(repo, "UserRepository", "JpaRepository", "Acceso a datos de usuarios")
        Component(security, "SecurityConfig", "@Configuration", "Define rutas públicas, stateless, filtro JWT en la cadena")
    }

    ContainerDb(h2, "Base de datos", "H2")

    Rel(cliente, filter, "HTTPS + Authorization Bearer")
    Rel(filter, controller, "chain.doFilter")
    Rel(controller, validators, "@Valid dispara validación")
    Rel(controller, userService, "delega lógica")
    Rel(userService, jwtService, "generate(user)")
    Rel(userService, repo, "saveAndFlush, findById, findByEmail")
    Rel(repo, h2, "SQL vía Hibernate")
    Rel(controller, globalEx, "excepciones son interceptadas")
    Rel(filter, jwtService, "validate(token)")
    Rel(security, filter, "registra en la cadena")
```

---

## 3. Sequence Diagram — Flujo de `POST /sign-up`

```mermaid
sequenceDiagram
    autonumber
    actor Cliente
    participant F as JwtAuthFilter
    participant C as UserController
    participant V as Validators
    participant S as UserService
    participant J as JwtService
    participant E as PasswordEncoder
    participant R as UserRepository
    participant DB as H2

    Cliente->>F: POST /sign-up (JSON)
    F->>F: /sign-up es público → no hace nada
    F->>C: chain.doFilter
    C->>V: @Valid dispara validaciones
    V-->>C: OK (formato email/password válidos)
    C->>S: signUp(request)
    S->>R: findByEmail(email)
    R->>DB: SELECT
    DB-->>R: Optional.empty()
    R-->>S: vacío (email disponible)
    S->>E: encode(password)
    E-->>S: hash BCrypt
    S->>R: saveAndFlush(user)
    R->>DB: INSERT users + INSERT phones (cascade)
    DB-->>R: user con UUID + timestamps
    R-->>S: user managed
    S->>J: generate(user)
    J-->>S: JWT firmado
    S->>R: saveAndFlush(user) (update token)
    R->>DB: UPDATE users SET token
    S-->>C: SignUpResponse
    C-->>Cliente: 201 Created + JSON
```

### Flujo de error (email ya registrado)

```mermaid
sequenceDiagram
    autonumber
    actor Cliente
    participant C as UserController
    participant S as UserService
    participant G as GlobalExceptionHandler
    participant R as UserRepository

    Cliente->>C: POST /sign-up
    C->>S: signUp(request)
    S->>R: findByEmail(email)
    R-->>S: Optional.of(user)
    S-->>C: throw EmailAlreadyRegisteredException
    C-->>G: excepción propaga
    G-->>Cliente: 409 Conflict<br/>{"mensaje": "El correo ya registrado"}
```

---

## 4. ER Diagram — Modelo de datos

```mermaid
erDiagram
    USERS ||--o{ PHONES : "tiene"

    USERS {
        uuid id PK
        varchar name
        varchar email UK
        varchar password "hash BCrypt"
        timestamp created
        timestamp modified
        timestamp last_login
        varchar token "JWT HS256"
        boolean is_active
    }

    PHONES {
        uuid id PK
        varchar number
        varchar city_code
        varchar country_code
        uuid user_id FK
    }
```

---

## Decisiones arquitectónicas

- **Capas claras**: controller / service / repository. El controller solo traduce HTTP; la lógica vive en el service; la persistencia en el repo.
- **Stateless**: no hay sesiones. Cada request se autentica por JWT. Escalable horizontalmente sin sticky sessions.
- **Seguridad por filtro**: el JWT se valida en `JwtAuthenticationFilter` antes de que la request llegue al controller. Patrón Chain of Responsibility de Spring Security.
- **Validación declarativa**: `@ValidEmail` y `@ValidPassword` encapsulan las reglas de negocio sobre formato. Regex configurable vía properties.
- **Manejo centralizado de errores**: `@RestControllerAdvice` garantiza que toda respuesta de error sigue el formato `{"mensaje": "..."}` exigido por BCI.
- **Inyección por constructor**: todos los beans usan final + constructor, facilitando tests sin Spring y dejando visibles las dependencias.
- **Transacciones atómicas**: `@Transactional` en el método de registro — o se crean user + phones juntos, o nada.
