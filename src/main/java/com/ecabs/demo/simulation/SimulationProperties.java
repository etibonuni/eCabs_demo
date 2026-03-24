package com.ecabs.demo.simulation;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "simulation")
public class SimulationProperties {

    private double timeMultiplier = 1.0;
    private double driverCountAvg = 5.0;
    private double driverCountStdDev = 2.0;
    private long driverMinRegisteredTime = 300000L; // 5 minutes
    private long driverMaxRegisteredTime = 900000L; // 15 minutes
    private long rideTimePerDistance = 10000;
    private double rideCountAvg = 3.0;
    private double rideCountStdDev = 1.5;

    public double getTimeMultiplier() {
        return timeMultiplier;
    }

    public void setTimeMultiplier(double timeMultiplier) {
        this.timeMultiplier = timeMultiplier;
    }

    public double getDriverCountAvg() {
        return driverCountAvg;
    }

    public void setDriverCountAvg(double driverCountAvg) {
        this.driverCountAvg = driverCountAvg;
    }

    public double getDriverCountStdDev() {
        return driverCountStdDev;
    }

    public void setDriverCountStdDev(double driverCountStdDev) {
        this.driverCountStdDev = driverCountStdDev;
    }

    public long getDriverMinRegisteredTime() {
        return driverMinRegisteredTime;
    }

    public void setDriverMinRegisteredTime(long driverMinRegisteredTime) {
        this.driverMinRegisteredTime = driverMinRegisteredTime;
    }

    public long getDriverMaxRegisteredTime() {
        return driverMaxRegisteredTime;
    }

    public void setDriverMaxRegisteredTime(long driverMaxRegisteredTime) {
        this.driverMaxRegisteredTime = driverMaxRegisteredTime;
    }

    public double getRideCountAvg() {
        return rideCountAvg;
    }

    public void setRideCountAvg(double rideCountAvg) {
        this.rideCountAvg = rideCountAvg;
    }

    public double getRideCountStdDev() {
        return rideCountStdDev;
    }

    public void setRideCountStdDev(double rideCountStdDev) {
        this.rideCountStdDev = rideCountStdDev;
    }

    public long getRideTimePerDistance() {
        return rideTimePerDistance;
    }

    public void setRideTimePerDistance(long rideTimePerDistance) {
        this.rideTimePerDistance = rideTimePerDistance;
    }
}
