package com.ecabs.demo.service;

import com.ecabs.demo.model.Driver;
import com.ecabs.demo.model.Ride;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class RideMatchingServiceTest {

    private RideMatchingService rideMatchingService;
    private SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    void setUp() {
        // Create a mock of the messaging template
        messagingTemplate = Mockito.mock(SimpMessagingTemplate.class);
        // Provide the mock to the service constructor
        rideMatchingService = new RideMatchingService(messagingTemplate);
    }

    @Test
    void testRegisterDriver() {
        Driver driver = rideMatchingService.registerDriver(10.0, 20.0, "Test Driver");
        assertNotNull(driver);
        assertNotNull(driver.getId());
        assertEquals(10.0, driver.getLatitude());
        assertEquals(20.0, driver.getLongitude());
        assertTrue(driver.isAvailable());
        // Verify that a message was sent, using a typed any() to avoid ambiguity
        Mockito.verify(messagingTemplate).convertAndSend(Mockito.eq("/topic/simulation"), Mockito.any(Object.class));
    }

    @Test
    void testUpdateDriverLocation() {
        Driver driver = rideMatchingService.registerDriver(10.0, 20.0, "Test Driver");
        UUID driverId = driver.getId();
        rideMatchingService.updateDriverLocation(driverId, 15.0, 25.0);
        assertEquals(15.0, driver.getLatitude());
        assertEquals(25.0, driver.getLongitude());
    }

    @Test
    void testRequestRide() {
        Driver driver1 = rideMatchingService.registerDriver(10.0, 20.0, "Test Driver");
        Driver driver2 = rideMatchingService.registerDriver(12.0, 22.0, "Test Driver");

        Ride ride = rideMatchingService.requestRide(10.1, 20.1, 40, 40);
        assertNotNull(ride);
        assertEquals(driver1.getId(), ride.getDriverId());
        assertFalse(driver1.isAvailable());
        assertTrue(driver2.isAvailable());
        // Verify that a ride started event was sent
        // 2 calls for driver registration, 1 for ride started
        Mockito.verify(messagingTemplate, Mockito.times(3)).convertAndSend(Mockito.eq("/topic/simulation"), Mockito.any(Object.class));
    }

    @Test
    void testCompleteRide() {
        Driver driver = rideMatchingService.registerDriver(10.0, 20.0, "Test Driver");
        Ride ride = rideMatchingService.requestRide(10.1, 20.1, 40, 40);
        assertNotNull(ride);
        assertFalse(driver.isAvailable());

        rideMatchingService.completeRide(ride.getId());
        assertTrue(driver.isAvailable());
        
        // Verify that a ride completed event was sent
        // 1 for register, 1 for start, 1 for complete
        Mockito.verify(messagingTemplate, Mockito.times(3)).convertAndSend(Mockito.eq("/topic/simulation"), Mockito.any(Object.class));
    }

    @Test
    void testGetAvailableDrivers() {
        rideMatchingService.registerDriver(10.0, 20.0, "Test Driver");
        rideMatchingService.registerDriver(12.0, 22.0, "Test Driver");
        rideMatchingService.registerDriver(15.0, 25.0, "Test Driver");

        List<Driver> drivers = rideMatchingService.getAvailableDrivers(10.1, 20.1, 2);
        assertEquals(2, drivers.size());
        assertEquals(10.0, drivers.get(0).getLatitude());
        assertEquals(12.0, drivers.get(1).getLatitude());
    }

    @Test
    void testConcurrentRideRequests() throws InterruptedException {
        rideMatchingService.registerDriver(10.0, 20.0, "Test Driver");
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Ride> rides = IntStream.range(0, 10)
                .mapToObj(i -> executor.submit(() -> rideMatchingService.requestRide(10.1, 20.1, 40, 40)))
                .map(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        return null;
                    }
                })
                .collect(Collectors.toList());

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        long successfulRides = rides.stream().filter(java.util.Objects::nonNull).count();
        assertEquals(1, successfulRides);
    }
}
