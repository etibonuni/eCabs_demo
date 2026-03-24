package com.ecabs.demo.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class Driver {
    private UUID id;
    private double latitude;
    private double longitude;
    private String name;
    private final AtomicBoolean available = new AtomicBoolean(true); // Initialize here

    public Driver() {
    }

    public Driver(double latitude, double longitude, String name) {
        this.id = UUID.randomUUID();
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
        this.available.set(true);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @JsonIgnore
    public AtomicBoolean getAvailable() {
        return available;
    }

    @JsonGetter("available")
    public boolean isAvailable() {
        return available.get();
    }

    public void setAvailable(boolean available) {
        this.available.set(available);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Driver driver = (Driver) o;
        return Objects.equals(id, driver.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
