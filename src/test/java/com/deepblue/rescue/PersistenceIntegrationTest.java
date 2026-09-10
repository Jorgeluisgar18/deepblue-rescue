package com.deepblue.rescue;

import com.deepblue.rescue.domain.*;
import com.deepblue.rescue.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.springframework.jdbc.core.JdbcTemplate;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
class PersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("deepblue_test")
                    .withUsername("deepblue")
                    .withPassword("deepblue");

    @Autowired
    private RescueCenterRepository rescueCenterRepository;

    @Autowired
    private RescueCaseRepository rescueCaseRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private SpecialistRepository specialistRepository;

    @Autowired
    private ExpertiseRepository expertiseRepository;

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ---------------------------------------------------------
    // Paso 48 - Métodos heredados de JpaRepository
    // ---------------------------------------------------------

    @Test
    void shouldUseInheritedRepositoryMethods() {

        long initialCount = rescueCenterRepository.count();

        RescueCenter center = new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean Center",
                "Santa Marta"
        );

        RescueCenter saved = rescueCenterRepository.save(center);

        assertThat(saved.getId()).isNotNull();

        assertThat(
                rescueCenterRepository.findById(saved.getId())
        ).isPresent();

        assertThat(
                rescueCenterRepository.existsById(saved.getId())
        ).isTrue();

        assertThat(
                rescueCenterRepository.count()
        ).isEqualTo(initialCount + 1);
    }

    // ---------------------------------------------------------
    // Paso 49 - Relación RescueCenter 1:N RescueCase
    // ---------------------------------------------------------

    @Test
    void shouldPersistOneToManyRelationship() {

        RescueCenter center = new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean Center",
                "Santa Marta"
        );

        rescueCenterRepository.save(center);

        RescueCase case1 = new RescueCase(
                "RES-001",
                LocalDate.of(2026, 8, 1),
                "Bahía Concha",
                RescueStatus.IN_REHABILITATION
        );

        RescueCase case2 = new RescueCase(
                "RES-002",
                LocalDate.of(2026, 8, 2),
                "Taganga",
                RescueStatus.UNDER_EVALUATION
        );

        center.addCase(case1);
        center.addCase(case2);

        rescueCaseRepository.saveAll(
                List.of(case1, case2)
        );

        List<RescueCase> cases =
                rescueCaseRepository.findByRescueCenterCode("DB-CAR");

        assertThat(cases).hasSize(2);

        assertThat(cases)
                .allMatch(rescueCase ->
                        rescueCase.getRescueCenter()
                                .getCode()
                                .equals("DB-CAR"));
    }

    // ---------------------------------------------------------
    // Paso 50 - RescueCase 1:1 Animal
    // ---------------------------------------------------------

    @Test
    void shouldPersistRescueCaseWithAnimal() {

        RescueCenter center = new RescueCenter(
                "DB-ONE",
                "DeepBlue One",
                "Santa Marta"
        );

        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase(
                "RES-2026-001",
                LocalDate.of(2026, 8, 5),
                "Bahía Concha",
                RescueStatus.IN_REHABILITATION
        );

        center.addCase(rescueCase);

        Animal animal = new Animal(
                "AN-2026-001",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );

        rescueCase.assignAnimal(animal);

        rescueCaseRepository.save(rescueCase);
        rescueCaseRepository.flush();

        assertThat(rescueCase.getId()).isNotNull();
        assertThat(animal.getId()).isNotNull();

        assertThat(rescueCase.getAnimal())
                .isSameAs(animal);

        assertThat(animal.getRescueCase())
                .isSameAs(rescueCase);
    }

    // ---------------------------------------------------------
    // Paso 51 - Animal 1:1 MedicalRecord
    // ---------------------------------------------------------

    @Test
    void shouldPersistAnimalWithMedicalRecordUsingCascade() {

        RescueCenter center = new RescueCenter(
                "DB-MED",
                "DeepBlue Medical",
                "Santa Marta"
        );

        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase(
                "RES-2026-002",
                LocalDate.of(2026, 8, 6),
                "Playa Grande",
                RescueStatus.UNDER_EVALUATION
        );

        center.addCase(rescueCase);

        Animal animal = new Animal(
                "AN-2026-002",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.UNKNOWN
        );

        rescueCase.assignAnimal(animal);

        MedicalRecord medicalRecord =
                new MedicalRecord(
                        new BigDecimal("28.40"),
                        "STABLE",
                        "Left front flipper injury",
                        "Under observation"
                );

        animal.assignMedicalRecord(medicalRecord);

        rescueCaseRepository.save(rescueCase);
        rescueCaseRepository.flush();

        assertThat(animal.getId()).isNotNull();

        assertThat(
                animal.getMedicalRecord()
        ).isNotNull();

        assertThat(
                animal.getMedicalRecord().getId()
        ).isNotNull();
    }

    // ---------------------------------------------------------
    // Paso 52 - Specialist N:M Expertise
    // ---------------------------------------------------------

    @Test
    void shouldPersistManyToManyRelationship() {

        Expertise trauma =
                expertiseRepository
                        .findByNameIgnoreCase("Trauma")
                        .orElseThrow();

        Expertise rehabilitation =
                expertiseRepository
                        .findByNameIgnoreCase("Rehabilitation")
                        .orElseThrow();

        Specialist specialist = new Specialist(
                "SPEC-001",
                "Elena",
                "Vargas",
                "elena@deepblue.org",
                true
        );

        specialist.addExpertise(trauma);
        specialist.addExpertise(rehabilitation);

        specialistRepository.saveAndFlush(specialist);

        assertThat(
                specialist.getExpertiseAreas()
        ).hasSize(2);

        assertThat(
                specialist.getExpertiseAreas()
                        .stream()
                        .map(Expertise::getName)
        ).containsExactlyInAnyOrder(
                "Trauma",
                "Rehabilitation"
        );
    }

    // ---------------------------------------------------------
    // Paso 53 - Query Method por status
    // ---------------------------------------------------------

    @Test
    void shouldFindCasesByStatus() {

        RescueCenter center = new RescueCenter(
                "DB-STATUS",
                "DeepBlue Status",
                "Santa Marta"
        );

        rescueCenterRepository.save(center);

        RescueCase case1 = new RescueCase(
                "RES-STATUS-001",
                LocalDate.of(2026, 8, 1),
                "Location 1",
                RescueStatus.IN_REHABILITATION
        );

        RescueCase case2 = new RescueCase(
                "RES-STATUS-002",
                LocalDate.of(2026, 8, 2),
                "Location 2",
                RescueStatus.READY_FOR_RELEASE
        );

        RescueCase case3 = new RescueCase(
                "RES-STATUS-003",
                LocalDate.of(2026, 8, 3),
                "Location 3",
                RescueStatus.IN_REHABILITATION
        );

        center.addCase(case1);
        center.addCase(case2);
        center.addCase(case3);

        rescueCaseRepository.saveAll(
                List.of(case1, case2, case3)
        );

        List<RescueCase> result =
                rescueCaseRepository
                        .findByStatusOrderByRescueDateAsc(
                                RescueStatus.IN_REHABILITATION
                        );

        assertThat(result).hasSize(2);

        assertThat(result)
                .extracting(RescueCase::getCaseCode)
                .containsExactly(
                        "RES-STATUS-001",
                        "RES-STATUS-003"
                );
    }

    // ---------------------------------------------------------
    // Paso 54 - Query Method navegando relaciones
    // ---------------------------------------------------------

    @Test
    void shouldFindAnimalsByCenterCode() {

        RescueCenter caribbean = new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean",
                "Santa Marta"
        );

        RescueCenter pacific = new RescueCenter(
                "DB-PAC",
                "DeepBlue Pacific",
                "Buenaventura"
        );

        rescueCenterRepository.saveAll(
                List.of(caribbean, pacific)
        );

        RescueCase caribbeanCase = new RescueCase(
                "RES-CAR-001",
                LocalDate.of(2026, 8, 10),
                "Santa Marta",
                RescueStatus.IN_REHABILITATION
        );

        RescueCase pacificCase = new RescueCase(
                "RES-PAC-001",
                LocalDate.of(2026, 8, 11),
                "Buenaventura",
                RescueStatus.IN_REHABILITATION
        );

        caribbean.addCase(caribbeanCase);
        pacific.addCase(pacificCase);

        Animal turtle = new Animal(
                "AN-CAR-001",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );

        Animal dolphin = new Animal(
                "AN-PAC-001",
                "Common Dolphin",
                "Delphinus delphis",
                AnimalSex.MALE
        );

        caribbeanCase.assignAnimal(turtle);
        pacificCase.assignAnimal(dolphin);

        rescueCaseRepository.saveAll(
                List.of(caribbeanCase, pacificCase)
        );

        List<Animal> result =
                animalRepository
                        .findByRescueCaseRescueCenterCode(
                                "DB-CAR"
                        );

        assertThat(result).hasSize(1);

        assertThat(result.getFirst().getAnimalCode())
                .isEqualTo("AN-CAR-001");
    }

    // ---------------------------------------------------------
    // Paso 55 - JPQL Specialist por expertise
    // ---------------------------------------------------------

    @Test
    void shouldFindActiveSpecialistsByExpertise() {

        Expertise trauma =
                expertiseRepository
                        .findByNameIgnoreCase("Trauma")
                        .orElseThrow();

        Expertise rehabilitation =
                expertiseRepository
                        .findByNameIgnoreCase("Rehabilitation")
                        .orElseThrow();

        Expertise marineMammals =
                expertiseRepository
                        .findByNameIgnoreCase("Marine Mammals")
                        .orElseThrow();

        Expertise marineBirds =
                expertiseRepository
                        .findByNameIgnoreCase("Marine Birds")
                        .orElseThrow();

        Specialist elena = new Specialist(
                "SPEC-E",
                "Elena",
                "Vargas",
                "elena@test.org",
                true
        );

        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);

        Specialist mateo = new Specialist(
                "SPEC-M",
                "Mateo",
                "Ruiz",
                "mateo@test.org",
                true
        );

        mateo.addExpertise(marineMammals);
        mateo.addExpertise(rehabilitation);

        Specialist sofia = new Specialist(
                "SPEC-S",
                "Sofia",
                "Mendoza",
                "sofia@test.org",
                true
        );

        sofia.addExpertise(marineBirds);
        sofia.addExpertise(trauma);

        specialistRepository.saveAll(
                List.of(elena, mateo, sofia)
        );

        List<Specialist> result =
                specialistRepository
                        .findActiveByExpertise("Trauma");

        assertThat(result)
                .extracting(Specialist::getFirstName)
                .containsExactly(
                        "Sofia",
                        "Elena"
                );
    }

    // ---------------------------------------------------------
    // Pasos 56 y 57 - Crear y consultar tratamientos
    // ---------------------------------------------------------

    @Test
    void shouldFindTreatmentsByAnimalChronologically() {

        RescueCenter center = new RescueCenter(
                "DB-TREAT",
                "DeepBlue Treatment",
                "Santa Marta"
        );

        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase(
                "RES-TREAT-001",
                LocalDate.of(2026, 8, 10),
                "Bahía Concha",
                RescueStatus.IN_REHABILITATION
        );

        center.addCase(rescueCase);

        Animal animal = new Animal(
                "AN-TREAT-001",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );

        rescueCase.assignAnimal(animal);

        rescueCaseRepository.save(rescueCase);

        Specialist elena = new Specialist(
                "SPEC-TREAT-E",
                "Elena",
                "Vargas",
                "elena.treat@test.org",
                true
        );

        Specialist mateo = new Specialist(
                "SPEC-TREAT-M",
                "Mateo",
                "Ruiz",
                "mateo.treat@test.org",
                true
        );

        specialistRepository.saveAll(
                List.of(elena, mateo)
        );

        Treatment treatment1 = new Treatment(
                animal,
                elena,
                LocalDateTime.of(2026, 8, 10, 8, 0),
                TreatmentType.WOUND_CARE,
                "Wound cleaning"
        );

        Treatment treatment2 = new Treatment(
                animal,
                elena,
                LocalDateTime.of(2026, 8, 10, 10, 0),
                TreatmentType.HYDRATION,
                "Fluid therapy"
        );

        Treatment treatment3 = new Treatment(
                animal,
                mateo,
                LocalDateTime.of(2026, 8, 10, 12, 0),
                TreatmentType.OBSERVATION,
                "Clinical observation"
        );

        treatmentRepository.saveAll(
                List.of(
                        treatment1,
                        treatment2,
                        treatment3
                )
        );

        List<Treatment> result =
                treatmentRepository
                        .findByAnimalIdOrderByPerformedAtAsc(
                                animal.getId()
                        );

        assertThat(result)
                .extracting(Treatment::getType)
                .containsExactly(
                        TreatmentType.WOUND_CARE,
                        TreatmentType.HYDRATION,
                        TreatmentType.OBSERVATION
                );
    }

    // ---------------------------------------------------------
    // Paso 58 - JPQL por intervalo de fechas
    // ---------------------------------------------------------

    @Test
    void shouldFindTreatmentsBetweenDates() {

        RescueCenter center = new RescueCenter(
                "DB-DATE",
                "DeepBlue Date",
                "Santa Marta"
        );

        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase(
                "RES-DATE-001",
                LocalDate.of(2026, 8, 1),
                "Santa Marta",
                RescueStatus.IN_REHABILITATION
        );

        center.addCase(rescueCase);

        Animal animal = new Animal(
                "AN-DATE-001",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );

        rescueCase.assignAnimal(animal);

        rescueCaseRepository.save(rescueCase);

        Specialist specialist = new Specialist(
                "SPEC-DATE",
                "Elena",
                "Vargas",
                "date@test.org",
                true
        );

        specialistRepository.save(specialist);

        Treatment first = new Treatment(
                animal,
                specialist,
                LocalDateTime.of(2026, 8, 1, 10, 0),
                TreatmentType.OBSERVATION,
                "First treatment"
        );

        Treatment second = new Treatment(
                animal,
                specialist,
                LocalDateTime.of(2026, 8, 10, 10, 0),
                TreatmentType.HYDRATION,
                "Second treatment"
        );

        Treatment third = new Treatment(
                animal,
                specialist,
                LocalDateTime.of(2026, 8, 20, 10, 0),
                TreatmentType.OBSERVATION,
                "Third treatment"
        );

        treatmentRepository.saveAll(
                List.of(first, second, third)
        );

        List<Treatment> result =
                treatmentRepository.findTreatmentsBetween(
                        LocalDateTime.of(2026, 8, 5, 0, 0),
                        LocalDateTime.of(2026, 8, 15, 23, 59)
                );

        assertThat(result).hasSize(1);

        assertThat(result.getFirst().getPerformedAt())
                .isEqualTo(
                        LocalDateTime.of(
                                2026, 8, 10, 10, 0
                        )
                );
    }

    // ---------------------------------------------------------
    // Paso 59 - Constraint UNIQUE
    // ---------------------------------------------------------

    @Test
    void shouldRejectDuplicatedAnimalCode() {

        RescueCenter center = new RescueCenter(
                "DB-UNIQUE",
                "DeepBlue Unique",
                "Santa Marta"
        );

        rescueCenterRepository.save(center);

        RescueCase case1 = new RescueCase(
                "RES-UNIQUE-001",
                LocalDate.of(2026, 8, 1),
                "Location 1",
                RescueStatus.ADMITTED
        );

        RescueCase case2 = new RescueCase(
                "RES-UNIQUE-002",
                LocalDate.of(2026, 8, 2),
                "Location 2",
                RescueStatus.ADMITTED
        );

        center.addCase(case1);
        center.addCase(case2);

        Animal animal1 = new Animal(
                "AN-100",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );

        Animal animal2 = new Animal(
                "AN-100",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.MALE
        );

        case1.assignAnimal(animal1);
        case2.assignAnimal(animal2);

        rescueCaseRepository.save(case1);
        rescueCaseRepository.flush();

        assertThatThrownBy(() -> {
            rescueCaseRepository.save(case2);
            rescueCaseRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test
    void shouldPersistIntegratorScenarioAndResolveQueries() {

        // ---------------------------------------------------------
        // Centro
        // ---------------------------------------------------------

        RescueCenter center = new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean",
                "Santa Marta"
        );

        rescueCenterRepository.save(center);

        // ---------------------------------------------------------
        // Caso de rescate
        // ---------------------------------------------------------

        RescueCase rescueCase = new RescueCase(
                "RES-2026-100",
                LocalDate.of(2026, 8, 18),
                "Bahía Concha",
                RescueStatus.IN_REHABILITATION
        );

        center.addCase(rescueCase);

        // ---------------------------------------------------------
        // Animal
        // ---------------------------------------------------------

        Animal animal = new Animal(
                "AN-2026-100",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );

        rescueCase.assignAnimal(animal);

        // ---------------------------------------------------------
        // Expediente médico
        // ---------------------------------------------------------

        MedicalRecord medicalRecord = new MedicalRecord(
                new BigDecimal("27.80"),
                "STABLE",
                "Injury caused by fishing net",
                "Possible plastic ingestion"
        );

        animal.assignMedicalRecord(medicalRecord);

        rescueCaseRepository.save(rescueCase);
        rescueCaseRepository.flush();

        // ---------------------------------------------------------
        // Expertise existentes desde V2
        // ---------------------------------------------------------

        Expertise marineReptiles =
                expertiseRepository
                        .findByNameIgnoreCase("Marine Reptiles")
                        .orElseThrow();

        Expertise trauma =
                expertiseRepository
                        .findByNameIgnoreCase("Trauma")
                        .orElseThrow();

        Expertise rehabilitation =
                expertiseRepository
                        .findByNameIgnoreCase("Rehabilitation")
                        .orElseThrow();

        // ---------------------------------------------------------
        // Especialista
        // ---------------------------------------------------------

        Specialist elena = new Specialist(
                "SPEC-001",
                "Elena",
                "Vargas",
                "elena@deepblue.org",
                true
        );

        elena.addExpertise(marineReptiles);
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);

        specialistRepository.saveAndFlush(elena);

        // ---------------------------------------------------------
        // Tratamientos
        // ---------------------------------------------------------

        Treatment woundCare = new Treatment(
                animal,
                elena,
                LocalDateTime.of(2026, 8, 18, 10, 0),
                TreatmentType.WOUND_CARE,
                "Cleaning of left front flipper"
        );

        Treatment hydration = new Treatment(
                animal,
                elena,
                LocalDateTime.of(2026, 8, 18, 12, 0),
                TreatmentType.HYDRATION,
                "Subcutaneous fluid therapy"
        );

        treatmentRepository.saveAll(
                List.of(woundCare, hydration)
        );

        treatmentRepository.flush();

        // =========================================================
        // CONSULTA 1
        // ¿Existe RES-2026-100?
        // =========================================================

        assertThat(
                rescueCaseRepository
                        .findByCaseCode("RES-2026-100")
        ).isPresent();

        // =========================================================
        // CONSULTA 2
        // Casos IN_REHABILITATION
        // =========================================================

        List<RescueCase> rehabilitationCases =
                rescueCaseRepository
                        .findByStatusOrderByRescueDateAsc(
                                RescueStatus.IN_REHABILITATION
                        );

        assertThat(rehabilitationCases)
                .extracting(RescueCase::getCaseCode)
                .contains("RES-2026-100");

        // =========================================================
        // CONSULTA 3
        // Animales del centro DB-CAR
        // =========================================================

        List<Animal> animalsByCenter =
                animalRepository
                        .findByRescueCaseRescueCenterCode(
                                "DB-CAR"
                        );

        assertThat(animalsByCenter)
                .extracting(Animal::getAnimalCode)
                .containsExactly("AN-2026-100");

        // =========================================================
        // CONSULTA 4
        // Nombre común contiene turtle
        // =========================================================

        List<Animal> turtles =
                animalRepository
                        .findByCommonNameContainingIgnoreCase(
                                "turtle"
                        );

        assertThat(turtles)
                .extracting(Animal::getAnimalCode)
                .contains("AN-2026-100");

        // =========================================================
        // CONSULTA 5
        // Especialistas con expertise Trauma
        // =========================================================

        List<Specialist> traumaSpecialists =
                specialistRepository
                        .findActiveByExpertise("Trauma");

        assertThat(traumaSpecialists)
                .extracting(Specialist::getProfessionalCode)
                .contains("SPEC-001");

        // =========================================================
        // CONSULTA 6
        // Tratamientos de AN-2026-100 en orden cronológico
        // =========================================================

        Animal savedAnimal =
                animalRepository
                        .findByAnimalCode("AN-2026-100")
                        .orElseThrow();

        List<Treatment> treatments =
                treatmentRepository
                        .findByAnimalIdOrderByPerformedAtAsc(
                                savedAnimal.getId()
                        );

        assertThat(treatments)
                .extracting(Treatment::getType)
                .containsExactly(
                        TreatmentType.WOUND_CARE,
                        TreatmentType.HYDRATION
                );

        // =========================================================
        // CONSULTA 7
        // Tratamientos realizados por especialistas
        // con expertise Rehabilitation
        // =========================================================

        List<Treatment> rehabilitationTreatments =
                treatmentRepository
                        .findBySpecialistExpertise(
                                "Rehabilitation"
                        );

        assertThat(rehabilitationTreatments)
                .hasSize(2);

        // =========================================================
        // CONSULTA 8
        // Tratamientos entre dos fechas
        // =========================================================

        List<Treatment> treatmentsBetween =
                treatmentRepository
                        .findTreatmentsBetween(
                                LocalDateTime.of(
                                        2026, 8, 18, 0, 0
                                ),
                                LocalDateTime.of(
                                        2026, 8, 18, 23, 59
                                )
                        );

        assertThat(treatmentsBetween)
                .hasSize(2);

        List<Animal> challengeResult =
                animalRepository
                        .findByStatusAndTreatmentSpecialistExpertise(
                                RescueStatus.IN_REHABILITATION,
                                "trauma"
                        );

        assertThat(challengeResult)
                .extracting(Animal::getAnimalCode)
                .containsExactly("AN-2026-100");
    }
    @Test
    void shouldRejectInvalidForeignKey() {

        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        """
                        INSERT INTO rescue_cases
                        (case_code, rescue_date, rescue_location, status, rescue_center_id)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                        "RES-FK-INVALID",
                        java.sql.Date.valueOf(
                                LocalDate.of(2026, 8, 25)
                        ),
                        "Test location",
                        "ADMITTED",
                        999999L
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}