package com.ecabs.demo.model;

import java.util.UUID;

public class Ride {
    private UUID id;
    private UUID driverId;
    private double pickupLatitude;
    private double pickupLongitude;
    private double dropoffLatitude;
    private double dropoffLongitude;
    private boolean completed;

    public Ride() {
    }

    public Ride(UUID driverId, double pickupLatitude, double pickupLongitude, double dropoffLatitude, double dropoffLongitude) {
        this.id = UUID.randomUUID();
        this.driverId = driverId;
        this.pickupLatitude = pickupLatitude;
        this.pickupLongitude = pickupLongitude;
        this.dropoffLatitude = dropoffLatitude;
        this.dropoffLongitude = dropoffLongitude;
        this.completed = false;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getDriverId() {
        return driverId;
    }

    public void setDriverId(UUID driverId) {
        this.driverId = driverId;
    }

    public double getPickupLatitude() {
        return pickupLatitude;
    }

    public void setPickupLatitude(double pickupLatitude) {
        this.pickupLatitude = pickupLatitude;
    }

    public double getPickupLongitude() {
        return pickupLongitude;
    }

    public void setPickupLongitude(double pickupLongitude) {
        this.pickupLongitude = pickupLongitude;
    }

    public double getDropoffLatitude() { return dropoffLatitude; }

    public void setDropoffLatitude(double dropoffLatitude) { this.dropoffLatitude = dropoffLatitude; }

    public double getDropoffLongitude() {
        return dropoffLongitude;
    }

    public void setDropoffLongitude(double dropoffLongitude) {
        this.dropoffLongitude = dropoffLongitude;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
