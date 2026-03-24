package com.ecabs.demo.dto.event;

import com.ecabs.demo.model.Ride;

public record RideStartedEvent(Ride ride) {
}
