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
import com.manfred.incidenttracker.security.AuthUser;
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
import org.springframework.test.util.ReflectionTestUtils;
import com.manfred.incidenttracker.dto.AssignIncidentRequest;
import com.manfred.incidenttracker.dto.UpdateIncidentRequest;
import com.manfred.incidenttracker.exception.ForbiddenException;
import com.manfred.incidenttracker.security.AuthUser;




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
        when(machine.isRoleAllowed(Status.open, Status.investigating, Role.admin)).thenReturn(true);


        /* ------------- ACT ------------- */
        service.transition(1L, Status.investigating, new AuthUser(1L, Role.admin));


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
        assertThrows(IllegalTransitionException.class, () -> service.transition(1L, Status.resolved, new AuthUser(1L, Role.admin)));
        verify(statusHistoryRepository, never()).save(any());

    }


    @Test
    void unknownIncidentThrows(){

        /* ------------- ARRANGE ------------- */
        when(incidentRepository.findById(1L)).thenReturn(Optional.empty());


        /* ------------- ACT ------------- */
        // Not needed here. Look at the lambda ahead.


        /* ------------- ASSERT / VERIFY ------------- */
        assertThrows(IncidentNotFoundException.class, () -> service.transition(1L, Status.investigating, new AuthUser(1L, Role.admin)));
        verify(statusHistoryRepository, never()).save(any());

    }


    // HELPERS
    private User user(Long id, Role role){
        User u = new User(role + "@test.com", "hash", role, null);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }
    private Incident incident(User reporter){
        return new Incident("API lorem", "lorem ipsum", Severity.low, reporter, 30, OffsetDateTime.now().plusMinutes(30));
    }


    @Test 
    void assigneeNotOnIncidentCantTransition(){

        /* ------------- ARRANGE ------------- */
        User incidentAssignee = user(3L, Role.assignee);
        AuthUser actor = new AuthUser(2L, Role.assignee);
        Incident incident = incident(user(1L, Role.reporter));
        incident.setAssignee(incidentAssignee);
        
        // Stubs
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, Role.assignee)));

        /* ------------- ASSERT / VERIFY ------------- */
        assertThrows(ForbiddenException.class, () -> service.transition(1L, Status.investigating, actor));
        verify(statusHistoryRepository, never()).save(any());

    }

    @Test 
    void assigneeCantAssignSomeoneElse(){

        /* ------------- ARRANGE ------------- */
        User user = new User("test@test.com", "test123", Role.reporter, null);
        Incident incident = incident(user);
        AuthUser actor = new AuthUser(2L, Role.assignee);
        AssignIncidentRequest request = new AssignIncidentRequest(3L);
        
        // Stubs
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));

        /* ------------- ASSERT / VERIFY ------------- */
        assertThrows(ForbiddenException.class, () -> service.assign(1L, request, actor));

    }

    @Test 
    void reporterCantEditOthersIncident(){

        /* ------------- ARRANGE ------------- */
        User reporter = user(1L, Role.reporter);
        Incident incident = incident(reporter);
        AuthUser actor = new AuthUser(2L, Role.reporter);
        UpdateIncidentRequest request = new UpdateIncidentRequest(null, null, null);
        
        // Stubs
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));

        /* ------------- ASSERT / VERIFY ------------- */
        assertThrows(ForbiddenException.class, () -> service.update(1L, request, actor));

    }
    
}
