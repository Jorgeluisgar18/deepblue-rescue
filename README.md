# DeepBlue Rescue

## Descripción

DeepBlue Rescue es un proyecto académico orientado al desarrollo
de una capa de persistencia para una plataforma de rescate y
rehabilitación de fauna marina.

El proyecto utiliza Java 21, Spring Boot 4, Spring Data JPA,
Hibernate, PostgreSQL, Flyway y Testcontainers.

El laboratorio se concentra exclusivamente en persistencia,
modelado de entidades, relaciones JPA, repositories, Query
Methods, consultas JPQL, migraciones y pruebas de integración.

## Modelo de datos

El sistema contiene las siguientes entidades:

- RescueCenter
- RescueCase
- Animal
- MedicalRecord
- Specialist
- Expertise
- Treatment

También se utilizan los enums:

- RescueStatus
- AnimalSex
- TreatmentType

## Relaciones

Las principales relaciones son:

```text
RescueCenter 1:N RescueCase

RescueCase 1:1 Animal

Animal 1:1 MedicalRecord

Animal 1:N Treatment

Specialist 1:N Treatment

Specialist N:M Expertise
```

La relación N:M entre Specialist y Expertise utiliza la tabla
intermedia:

```text
specialist_expertise
```

## Tecnologías

- Java 21
- Spring Boot 4.1.1
- Spring Data JPA
- Hibernate
- PostgreSQL
- Flyway
- Testcontainers
- Maven
- JUnit 5
- AssertJ

## Configuración de la base de datos

La configuración principal se encuentra en:

```text
src/main/resources/application.yml
```

Por defecto se utiliza:

```text
Database: deepblue
User: postgres
Password: postgres
```

También pueden utilizarse las variables de entorno:

```text
DB_URL
DB_USER
DB_PASSWORD
```

Hibernate está configurado con:

```yaml
ddl-auto: validate
```

Esto significa que Hibernate valida que las entidades coincidan
con el esquema existente, pero no crea ni modifica las tablas.

## Flyway

Flyway es responsable de crear y evolucionar el esquema de la
base de datos.

Las migraciones utilizadas son:

```text
V1__create_schema.sql
V2__insert_expertise_catalog.sql
V3__add_tracking_device_to_animal.sql
```

### V1

Crea las tablas, claves primarias, claves foráneas,
restricciones UNIQUE, CHECK e índices.

### V2

Inserta el catálogo inicial de áreas de experiencia:

- Marine Reptiles
- Marine Mammals
- Marine Birds
- Trauma
- Rehabilitation
- Toxicology

### V3

Agrega a la tabla animals la columna:

```text
tracking_device_code
```

La columna puede contener NULL, pero cuando existe un código
debe ser único.

## Testcontainers

Las pruebas de integración utilizan Testcontainers para levantar
temporalmente una instancia real de PostgreSQL.

El contenedor utilizado es:

```text
postgres:18-alpine
```

La base de datos de pruebas es:

```text
deepblue_test
```

Gracias a `@ServiceConnection`, Spring Boot utiliza
automáticamente la conexión proporcionada por el contenedor
durante las pruebas.

Al terminar las pruebas, el contenedor es eliminado
automáticamente.

## Query Methods implementados

### RescueCenterRepository

```java
findByCode(String code);
```

### RescueCaseRepository

```java
findByCaseCode(String caseCode);

findByStatusOrderByRescueDateAsc(RescueStatus status);

findByRescueCenterCode(String code);

findByRescueDateAfterOrderByRescueDateDesc(LocalDate date);
```

### AnimalRepository

```java
findByAnimalCode(String animalCode);

findByCommonNameContainingIgnoreCase(String commonName);

findByRescueCaseStatus(RescueStatus status);

findByRescueCaseRescueCenterCode(String centerCode);
```

### ExpertiseRepository

```java
findByNameIgnoreCase(String name);
```

### TreatmentRepository

```java
findByAnimalIdOrderByPerformedAtAsc(Long animalId);
```

## Consultas JPQL implementadas

### Especialistas activos por expertise

Consulta especialistas activos que tengan determinada área de
experiencia, ignorando diferencias entre mayúsculas y
minúsculas.

### Tratamientos entre fechas

Consulta tratamientos cuyo `performedAt` se encuentre entre dos
fechas y los ordena cronológicamente.

### Tratamientos por centro

Navega las relaciones:

```text
Treatment
→ Animal
→ RescueCase
→ RescueCenter
```

para consultar los tratamientos realizados a animales de
determinado centro.

### Tratamientos por expertise del especialista

Navega:

```text
Treatment
→ Specialist
→ Expertise
```

para encontrar tratamientos realizados por especialistas con
determinada experiencia.

### Reto JPQL

Obtiene animales que tengan determinado estado de rescate y que
hayan recibido al menos un tratamiento por un especialista con
una experiencia determinada.

La consulta utiliza `DISTINCT` para evitar animales duplicados.

## Restricciones de integridad

El esquema utiliza:

- PRIMARY KEY para identificar cada registro.
- FOREIGN KEY para garantizar relaciones válidas.
- UNIQUE para evitar valores duplicados.
- CHECK para limitar los estados permitidos de los casos de
  rescate.

Aunque Java utiliza enums, el CHECK de PostgreSQL continúa
siendo importante porque protege la información incluso cuando
los datos ingresan desde una fuente diferente a la aplicación
Java.

## Ejecutar el proyecto

Es necesario tener:

- Java 21
- Docker Desktop
- PostgreSQL, si se desea ejecutar la aplicación fuera de las
  pruebas

En Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

## Ejecutar las pruebas

Docker Desktop debe estar activo.

Ejecutar:

```powershell
.\mvnw.cmd clean test
```

El resultado esperado es:

```text
BUILD SUCCESS
```

Durante las pruebas Testcontainers crea automáticamente un
contenedor PostgreSQL temporal.

Para observarlo durante la ejecución puede utilizarse en otra
terminal:

```powershell
docker ps
```

## Arquitectura del proyecto

```text
src/main/java/com/deepblue/rescue
├── DeepBlueRescueApplication.java
├── domain
└── repository

src/main/resources
├── application.yml
└── db/migration
    ├── V1__create_schema.sql
    ├── V2__insert_expertise_catalog.sql
    └── V3__add_tracking_device_to_animal.sql

src/test/java/com/deepblue/rescue
└── PersistenceIntegrationTest.java
```

## Autor

Jorge Luis Garcia Valderrama

Ingeniería de Sistemas
Universidad del Magdalena
