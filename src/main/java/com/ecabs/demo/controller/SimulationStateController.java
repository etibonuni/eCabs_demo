package com.ecabs.demo.controller;

import com.ecabs.demo.dto.event.InitialStateEvent;
import com.ecabs.demo.service.RideMatchingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class SimulationStateController {

    private final RideMatchingService rideMatchingService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public SimulationStateController(RideMatchingService rideMatchingService, SimpMessagingTemplate messagingTemplate) {
        this.rideMatchingService = rideMatchingService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Handles a request from a client to get the initial state of the simulation.
     * The client should send a message to /app/request-initial-state after connecting.
     */
    @MessageMapping("/request-initial-state")
    public void getInitialState(SimpMessageHeaderAccessor headerAccessor) {
        InitialStateEvent initialState = new InitialStateEvent(
                rideMatchingService.getAllDrivers(),
                rideMatchingService.getAllRides(),
                rideMatchingService.getCompletedRidesByDriver()
        );

        // Send the initial state directly back to the requesting client
        String sessionId = headerAccessor.getSessionId();
        messagingTemplate.convertAndSendToUser(
                sessionId,
                "/queue/initial-state",
                initialState,
                headerAccessor.getMessageHeaders()
        );
    }
}
