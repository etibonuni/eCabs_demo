package com.ecabs.demo.dto.event;

import java.util.UUID;

public record DriverUnregisteredEvent(UUID driverId) {
}
