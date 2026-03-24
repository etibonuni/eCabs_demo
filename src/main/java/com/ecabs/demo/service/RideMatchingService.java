package com.ecabs.demo.service;

import com.ecabs.demo.dto.event.*;
import com.ecabs.demo.dto.event.*;
import com.ecabs.demo.model.Driver;
import com.ecabs.demo.model.Ride;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.ecabs.demo.util.Utils.distance;

@Service
public class RideMatchingService {

    private final Map<UUID, Driver> drivers = new ConcurrentHashMap<>();
    private final Set<Driver> availableDrivers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Ride> rides = new ConcurrentHashMap<>();
    private final Map<UUID, Long> completedRidesByDriver = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public RideMatchingService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public Driver registerDriver(double latitude, double longitude, String name) {
        Driver driver = new Driver(latitude, longitude, name);
        drivers.put(driver.getId(), driver);
        completedRidesByDriver.putIfAbsent(driver.getId(), 0L);
        availableDrivers.add(driver);

        // Publish event for simulationUI
        messagingTemplate.convertAndSend("/topic/simulation",
                new WebSocketEvent<>(EventType.DRIVER_REGISTERED, new DriverRegisteredEvent(driver)));

        return driver;
    }

    public void removeDriver(UUID driverId) {
        Driver driver = drivers.get(driverId);
        if (driver == null) {
            return;
        }

        // Make sure the driver is really available, avoiding multithreading race issues
        if (driver.getAvailable().compareAndSet(true, false)) {
            // We've successfully "claimed" the driver. No new rides can be assigned.
            // Now we can safely remove them from the system.
            availableDrivers.remove(driver);

            if (drivers.remove(driverId) != null) {
                completedRidesByDriver.remove(driverId);

                // Publish event for simulationUI
                messagingTemplate.convertAndSend("/topic/simulation",
                        new WebSocketEvent<>(EventType.DRIVER_UNREGISTERED, new DriverUnregisteredEvent(driverId)));
            }
        } else {
            // The driver was not available, which implies they have an active ride.
            throw new RuntimeException("Driver has active rides - cannot be unregistered");
        }
    }

    public Driver updateDriverLocation(UUID driverId, double latitude, double longitude) {
        Driver driver = drivers.get(driverId);
        if (driver != null) {
            driver.setLatitude(latitude);
            driver.setLongitude(longitude);
        }
        return driver;
    }

    public Ride requestRide(double pickupLatitude, double pickupLongitude,
                            double dropoffLatitude, double dropoffLongitude) {
        List<Driver> availableDrivers = findNearestAvailableDrivers(pickupLatitude, pickupLongitude);
        for (Driver driver : availableDrivers) {
            if (driver.getAvailable().compareAndSet(true, false)) {
                this.availableDrivers.remove(driver);

                Ride ride = new Ride(driver.getId(), pickupLatitude, pickupLongitude, dropoffLatitude, dropoffLongitude);
                rides.put(ride.getId(), ride);

                // Publish event
                messagingTemplate.convertAndSend("/topic/simulation",
                        new WebSocketEvent<>(EventType.RIDE_STARTED, new RideStartedEvent(ride)));

                return ride;
            }
        }
        return null;
    }

    public Ride completeRide(UUID rideId) {
        Ride ride = rides.get(rideId);
        if (ride != null && !ride.isCompleted()) {
            ride.setCompleted(true);
            Driver driver = drivers.get(ride.getDriverId());
            if (driver != null) {
                // Driver is now at ride dropoff point
                driver.setLatitude(ride.getDropoffLatitude());
                driver.setLongitude(ride.getDropoffLongitude());

                driver.setAvailable(true);
                availableDrivers.add(driver);

                completedRidesByDriver.compute(driver.getId(), (id, count) -> (count == null) ? 1 : count + 1);

                // Publish event
                messagingTemplate.convertAndSend("/topic/simulation",
                        new WebSocketEvent<>(EventType.RIDE_COMPLETED, new RideCompletedEvent(ride.getId(), driver.getId())));
            }
            // Clean up the completed ride from the main map
            rides.remove(rideId);
        }
        return ride;
    }

    public List<Driver> getAvailableDrivers(double latitude, double longitude, int limit) {
        // We maintain the availableDrivers set as an optimization to avoid iterating all the drivers
        // to find the available ones
        return availableDrivers.stream()
                .sorted(Comparator.comparingDouble(driver -> distance(latitude, longitude, driver.getLatitude(), driver.getLongitude())))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public Collection<Driver> getAllDrivers() {
        return drivers.values();
    }

    public Collection<Ride> getAllRides() {
        // Return only active rides
        return rides.values().stream().filter(r -> !r.isCompleted()).collect(Collectors.toList());
    }

    public Map<UUID, Long> getCompletedRidesByDriver() {
        return completedRidesByDriver;
    }

    private List<Driver> findNearestAvailableDrivers(double latitude, double longitude) {
        return availableDrivers.stream()
                .sorted(Comparator.comparingDouble(driver -> distance(latitude, longitude, driver.getLatitude(), driver.getLongitude())))
                .collect(Collectors.toList());
    }

}
