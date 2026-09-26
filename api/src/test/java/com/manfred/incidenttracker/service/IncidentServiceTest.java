package com.manfred.incidenttracker.service;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.manfred.incidenttracker.domain.IncidentStateMachine;
import com.manfred.incidenttracker.entity.User;
import com.manfred.incidenttracker.exception.IllegalTransitionException;
import com.manfred.incidenttracker.exception.IncidentNotFoundException;
import com.manfred.incidenttracker.repository.CommentRepository;
import com.manfred.incidenttracker.repository.IncidentRepository;
import com.manfred.incidenttracker.repository.StatusHistoryRepository;
import com.manfred.incidenttracker.repository.UserRepository;
import com.manfred.incidenttracker.entity.Incident;
import com.manfred.incidenttracker.entity.Role;
import com.manfred.incidenttracker.entity.Severity;
import com.manfred.incidenttracker.entity.Status;




@ExtendWith(MockitoExtension.class)
public class IncidentServiceTest {

    @Mock 
    private IncidentRepository incidentRepository;
    @Mock 
    private UserRepository userRepository;
    @Mock 
    private StatusHistoryRepository statusHistoryRepository;
    @Mock 
    private CommentRepository commentRepository;
    @Mock 
    private IncidentStateMachine machine;

    @InjectMocks 
    private IncidentService service;


    @Test
    void legalTransitionSaves(){

        /* ------------- ARRANGE ------------- */
        User user = new User("test@test.com", "test123", Role.admin, null);

        Incident incident = new Incident(
            "API lorem", 
            "lorem ipsum", 
            Severity.low, 
            user, 
            30, 
            OffsetDateTime.now().plusMinutes(30)
        );

        // Stubs
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));


        /* ------------- ACT ------------- */
        service.transition(1L, Status.investigating, 1L);


        /* ------------- ASSERT / VERIFY ------------- */
        assertEquals(Status.investigating, incident.getIncidentStatus());
        verify(statusHistoryRepository).save(any());

    }


    @Test
    void ilegalTransitionThrows(){

        /* ------------- ARRANGE ------------- */
        User user = new User("test@test.com", "test123", Role.admin, null);

        Incident incident = new Incident(
            "API lorem", 
            "lorem ipsum", 
            Severity.low, 
            user, 
            30, 
            OffsetDateTime.now().plusMinutes(30)
        );

        // Stubs
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        doThrow(new IllegalTransitionException(Status.open, Status.resolved)).when(machine).validateTransition(Status.open, Status.resolved);


        /* ------------- ACT ------------- */
        // Not needed here. Look at the lambda ahead.


        /* ------------- ASSERT / VERIFY ------------- */
        assertThrows(IllegalTransitionException.class, () -> service.transition(1L, Status.resolved, 1L));
        verify(statusHistoryRepository, never()).save(any());

    }


    @Test
    void unknownIncidentThrows(){

        /* ------------- ARRANGE ------------- */
        when(incidentRepository.findById(1L)).thenReturn(Optional.empty());


        /* ------------- ACT ------------- */
        // Not needed here. Look at the lambda ahead.


        /* ------------- ASSERT / VERIFY ------------- */
        assertThrows(IncidentNotFoundException.class, () -> service.transition(1L, Status.investigating, 1L));
        verify(statusHistoryRepository, never()).save(any());

    }
    
}
