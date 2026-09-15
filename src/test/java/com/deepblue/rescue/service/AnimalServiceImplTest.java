package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.mapper.AnimalMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.service.impl.AnimalServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnimalServiceImplTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private AnimalMapper mapper;

    @InjectMocks
    private AnimalServiceImpl service;

    @Test
    void shouldAllowTreatmentWhenAnimalIsInRehabilitation() {

        RescueCase rescueCase =
                new RescueCase(
                        "RES-001",
                        LocalDate.of(2026, 8, 20),
                        "Santa Marta",
                        RescueStatus.IN_REHABILITATION
                );

        Animal animal = new Animal(
                "AN-001",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );

        rescueCase.assignAnimal(animal);

        when(
                animalRepository
                        .findByAnimalCode("AN-001")
        ).thenReturn(Optional.of(animal));

        boolean result =
                service.canReceiveTreatment(
                        "AN-001"
                );

        assertThat(result).isTrue();
    }

    @Test
    void shouldRejectTreatmentWhenAnimalIsReleased() {

        RescueCase rescueCase =
                new RescueCase(
                        "RES-001",
                        LocalDate.of(2026, 8, 20),
                        "Santa Marta",
                        RescueStatus.RELEASED
                );

        Animal animal = new Animal(
                "AN-001",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );

        rescueCase.assignAnimal(animal);

        when(
                animalRepository
                        .findByAnimalCode("AN-001")
        ).thenReturn(Optional.of(animal));

        boolean result =
                service.canReceiveTreatment(
                        "AN-001"
                );

        assertThat(result).isFalse();
    }
}