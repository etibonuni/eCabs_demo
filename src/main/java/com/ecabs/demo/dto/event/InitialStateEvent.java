package com.ecabs.demo.dto.event;

import com.ecabs.demo.model.Driver;
import com.ecabs.demo.model.Ride;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public record InitialStateEvent(
    Collection<Driver> drivers,
    Collection<Ride> rides,
    Map<UUID, Long> completedRidesByDriver
) {
}
