package com.ecabs.demo.dto.event;

import com.ecabs.demo.model.Driver;

public record DriverRegisteredEvent(Driver driver) {
}
