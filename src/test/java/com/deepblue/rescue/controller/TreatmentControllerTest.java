package com.deepblue.rescue.controller;

import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.GlobalExceptionHandler;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.service.TreatmentService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TreatmentController.class)
@Import(GlobalExceptionHandler.class)
class TreatmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TreatmentService service;

    @Test
    void shouldRegisterTreatmentAndReturn201()
            throws Exception {

        TreatmentResponse response =
                new TreatmentResponse(
                        1L,
                        "AN-001",
                        "SP-001",
                        LocalDateTime.of(
                                2026,
                                8,
                                20,
                                10,
                                30
                        ),
                        TreatmentType.WOUND_CARE,
                        "Cleaning and treatment of the animal wound"
                );

        when(
                service.register(
                        any(CreateTreatmentRequest.class)
                )
        ).thenReturn(response);

        mockMvc.perform(
                        post("/api/treatments")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "animalCode": "AN-001",
                                          "specialistCode": "SP-001",
                                          "performedAt": "2026-08-20T10:30:00",
                                          "type": "WOUND_CARE",
                                          "description": "Cleaning and treatment of the animal wound"
                                        }
                                        """)
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.animalCode")
                                .value("AN-001")
                )
                .andExpect(
                        jsonPath("$.specialistCode")
                                .value("SP-001")
                )
                .andExpect(
                        jsonPath("$.type")
                                .value("WOUND_CARE")
                )
                .andExpect(
                        jsonPath("$.description")
                                .value(
                                        "Cleaning and treatment of the animal wound"
                                )
                );

        verify(service)
                .register(
                        any(CreateTreatmentRequest.class)
                );
    }

    @Test
    void shouldReturn400WhenTreatmentRequestIsInvalid()
            throws Exception {

        mockMvc.perform(
                        post("/api/treatments")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "animalCode": "",
                                          "specialistCode": "",
                                          "description": ""
                                        }
                                        """)
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Bad Request")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Request validation failed"
                                )
                )
                .andExpect(
                        jsonPath("$.details.animalCode")
                                .value(
                                        "Animal code is required"
                                )
                )
                .andExpect(
                        jsonPath("$.details.specialistCode")
                                .value(
                                        "Specialist code is required"
                                )
                )
                .andExpect(
                        jsonPath("$.details.performedAt")
                                .value(
                                        "Treatment date is required"
                                )
                )
                .andExpect(
                        jsonPath("$.details.type")
                                .value(
                                        "Treatment type is required"
                                )
                );

        verify(
                service,
                never()
        ).register(any());
    }

    @Test
    void shouldReturn404WhenAnimalDoesNotExist()
            throws Exception {

        when(
                service.register(
                        any(CreateTreatmentRequest.class)
                )
        ).thenThrow(
                new ResourceNotFoundException(
                        "Animal not found: AN-999"
                )
        );

        mockMvc.perform(
                        post("/api/treatments")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "animalCode": "AN-999",
                                          "specialistCode": "SP-001",
                                          "performedAt": "2026-08-20T10:30:00",
                                          "type": "WOUND_CARE",
                                          "description": "Cleaning and treatment of the animal wound"
                                        }
                                        """)
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Not Found")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Animal not found: AN-999"
                                )
                )
                .andExpect(
                        jsonPath("$.details")
                                .isMap()
                );

        verify(service)
                .register(
                        any(CreateTreatmentRequest.class)
                );
    }

    @Test
    void shouldReturn409WhenBusinessRuleIsViolated()
            throws Exception {

        when(
                service.register(
                        any(CreateTreatmentRequest.class)
                )
        ).thenThrow(
                new BusinessRuleException(
                        "Treatment cannot be registered"
                )
        );

        mockMvc.perform(
                        post("/api/treatments")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "animalCode": "AN-001",
                                          "specialistCode": "SP-001",
                                          "performedAt": "2026-08-20T10:30:00",
                                          "type": "WOUND_CARE",
                                          "description": "Cleaning and treatment of the animal wound"
                                        }
                                        """)
                )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Conflict")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Treatment cannot be registered"
                                )
                );

        verify(service)
                .register(
                        any(CreateTreatmentRequest.class)
                );
    }

    @Test
    void shouldReturn400WhenTreatmentTypeIsInvalid()
            throws Exception {

        mockMvc.perform(
                        post("/api/treatments")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "animalCode": "AN-001",
                                          "specialistCode": "SP-001",
                                          "performedAt": "2026-08-20T10:30:00",
                                          "type": "INVALID_TYPE",
                                          "description": "Cleaning and treatment of the animal wound"
                                        }
                                        """)
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Bad Request")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Malformed or invalid JSON request"
                                )
                )
                .andExpect(
                        jsonPath("$.details.body")
                                .value(
                                        "Check JSON syntax and enum values"
                                )
                );

        verify(
                service,
                never()
        ).register(any());
    }

    @Test
    void shouldReturn500WhenUnexpectedErrorOccurs()
            throws Exception {

        when(
                service.register(
                        any(CreateTreatmentRequest.class)
                )
        ).thenThrow(
                new RuntimeException(
                        "Unexpected internal problem"
                )
        );

        mockMvc.perform(
                        post("/api/treatments")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "animalCode": "AN-001",
                                          "specialistCode": "SP-001",
                                          "performedAt": "2026-08-20T10:30:00",
                                          "type": "WOUND_CARE",
                                          "description": "Cleaning and treatment of the animal wound"
                                        }
                                        """)
                )
                .andExpect(
                        status().isInternalServerError()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(500)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "Internal Server Error"
                                )
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "An unexpected error occurred"
                                )
                )
                .andExpect(
                        jsonPath("$.details")
                                .isMap()
                );

        verify(service)
                .register(
                        any(CreateTreatmentRequest.class)
                );
    }
}