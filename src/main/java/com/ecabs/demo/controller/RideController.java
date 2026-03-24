package com.ecabs.demo.controller;

import com.ecabs.demo.model.Driver;
import com.ecabs.demo.model.Ride;
import com.ecabs.demo.service.RideMatchingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class RideController {

    private final RideMatchingService rideMatchingService;

    @Autowired
    public RideController(RideMatchingService rideMatchingService) {
        this.rideMatchingService = rideMatchingService;
    }

    @PostMapping("/drivers")
    public Driver registerDriver(@RequestParam double latitude, @RequestParam double longitude, @RequestParam String name) {
        return rideMatchingService.registerDriver(latitude, longitude, name);
    }

    @DeleteMapping("/drivers/{driverId}")
    public ResponseEntity<Object> removeDriver(@PathVariable UUID driverId) {
        try {
            rideMatchingService.removeDriver(driverId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e)
        {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/drivers/{driverId}/location")
    public Driver updateDriverLocation(@PathVariable UUID driverId, @RequestParam double latitude, @RequestParam double longitude) {
        return rideMatchingService.updateDriverLocation(driverId, latitude, longitude);
    }

    @PostMapping("/rides")
    public Ride requestRide(@RequestParam double pickupLatitude, @RequestParam double pickupLongitude, @RequestParam double dropoffLatitude, @RequestParam double dropoffLongitude) {
        return rideMatchingService.requestRide(pickupLatitude, pickupLongitude, dropoffLatitude, dropoffLongitude);
    }

    @PutMapping("/rides/{rideId}/complete")
    public Ride completeRide(@PathVariable UUID rideId) {
        return rideMatchingService.completeRide(rideId);
    }

    @GetMapping("/drivers/available")
    public List<Driver> getAvailableDrivers(@RequestParam double latitude, @RequestParam double longitude, @RequestParam int limit) {
        return rideMatchingService.getAvailableDrivers(latitude, longitude, limit);
    }
}
