# DeepBlue Rescue

API REST académica para la gestión de rescate, rehabilitación y tratamiento de fauna marina.

## Tecnologías

- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA
- PostgreSQL
- Flyway
- MapStruct
- Bean Validation
- JUnit 5
- Mockito
- MockMvc
- Testcontainers
- Maven

## Arquitectura

```text
Controller
   ↓
Service
   ↓
Repository
   ↓
PostgreSQL
```

Los controladores gestionan las solicitudes HTTP, las rutas, la validación de datos y los códigos de respuesta.

Las reglas de negocio permanecen en la capa Service.

## Endpoints

### Rescue Cases

Consultar un caso de rescate por código:

```http
GET /api/rescue-cases/{caseCode}
```

Consultar casos por estado:

```http
GET /api/rescue-cases?status=IN_REHABILITATION
```

Cambiar el estado de un caso:

```http
PATCH /api/rescue-cases/{caseCode}/status
```

Ejemplo:

```json
{
  "status": "READY_FOR_RELEASE"
}
```

### Treatments

Registrar un tratamiento:

```http
POST /api/treatments
```

Ejemplo:

```json
{
  "animalCode": "AN-001",
  "specialistCode": "SP-001",
  "performedAt": "2026-08-20T10:30:00",
  "type": "WOUND_CARE",
  "description": "Cleaning and treatment of the animal wound"
}
```

Una creación exitosa retorna:

```text
201 Created
```

### Animals

Consultar un animal por código:

```http
GET /api/animals/{animalCode}
```

Consultar animales en rehabilitación:

```http
GET /api/animals/in-rehabilitation
```

Consultar tratamientos de un animal:

```http
GET /api/animals/{animalCode}/treatments
```

Consultar elegibilidad para recibir tratamiento:

```http
GET /api/animals/{animalCode}/treatment-eligibility
```

Ejemplo de respuesta:

```json
{
  "animalCode": "AN-001",
  "eligible": true
}
```

## Códigos HTTP

La API utiliza los siguientes códigos principales:

```text
200 OK
201 Created
400 Bad Request
404 Not Found
409 Conflict
500 Internal Server Error
```

## Validación

La aplicación utiliza Jakarta Bean Validation.

Entre las anotaciones utilizadas se encuentran:

```java
@NotNull
@NotBlank
@PastOrPresent
@Size
```

Los DTO de entrada son:

```text
ChangeRescueStatusRequest
CreateTreatmentRequest
```

Las validaciones de entrada se realizan antes de ejecutar las reglas de negocio del Service.

## Manejo global de errores

La aplicación utiliza:

```java
@RestControllerAdvice
```

mediante la clase:

```text
GlobalExceptionHandler
```

La estructura general de los errores es:

```json
{
  "timestamp": "2026-10-06T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "details": {}
}
```

Los principales errores manejados son:

- `400 Bad Request`: solicitud inválida.
- `404 Not Found`: recurso inexistente.
- `409 Conflict`: violación de una regla de negocio.
- `500 Internal Server Error`: error inesperado.

## Persistencia

La aplicación utiliza PostgreSQL como base de datos.

La configuración se encuentra en:

```text
src/main/resources/application.yml
```

También pueden utilizarse las variables de entorno:

```text
DB_URL
DB_USER
DB_PASSWORD
```

Hibernate utiliza:

```yaml
ddl-auto: validate
```

## Flyway

Las migraciones se encuentran en:

```text
src/main/resources/db/migration
```

Migraciones actuales:

```text
V1__create_schema.sql
V2__insert_expertise_catalog.sql
V3__add_tracking_device_to_animal.sql
```

## Pruebas

El proyecto contiene pruebas para las diferentes capas.

### Persistencia

Se utiliza PostgreSQL mediante Testcontainers.

### Service

Se utilizan:

```text
JUnit 5
Mockito
AssertJ
```

### Controller

Se utilizan:

```text
@WebMvcTest
@MockitoBean
MockMvc
jsonPath
Mockito.verify
```

Los archivos principales de pruebas de Controller son:

```text
RescueCaseControllerTest.java
TreatmentControllerTest.java
AnimalControllerTest.java
```

Las pruebas cubren escenarios como:

- Consulta exitosa de casos de rescate.
- Caso de rescate inexistente.
- Consulta de casos por estado.
- Parámetro de estado inválido.
- Cambio válido de estado.
- Request inválido.
- Transición de estado inválida.
- Registro exitoso de tratamiento.
- Tratamiento con datos inválidos.
- Animal inexistente.
- Violación de reglas de negocio.
- Consulta de animales.
- Animales en rehabilitación.
- Tratamientos asociados a un animal.
- Elegibilidad para tratamiento.
- Enum o JSON inválido.
- Error inesperado.
- Elegibilidad de un animal inexistente.

## Ejecutar las pruebas

Desde la raíz del proyecto:

```powershell
mvn clean test
```

Resultado esperado:

```text
BUILD SUCCESS
```

Para ejecutar solo las pruebas de Controller:

```powershell
mvn "-Dtest=*ControllerTest" test
```

También pueden ejecutarse individualmente:

```powershell
mvn "-Dtest=RescueCaseControllerTest" test
```

```powershell
mvn "-Dtest=TreatmentControllerTest" test
```

```powershell
mvn "-Dtest=AnimalControllerTest" test
```

## Ejecutar la aplicación

Con Maven:

```powershell
mvn spring-boot:run
```

O utilizando Maven Wrapper en Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

## Autor

**Jorge Luis Garcia Valderrama**

Ingeniería de Sistemas  
Universidad del Magdalena