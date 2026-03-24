package com.ecabs.demo.dto.event;

import java.util.UUID;

public record RideCompletedEvent(UUID rideId, UUID driverId) {
}
