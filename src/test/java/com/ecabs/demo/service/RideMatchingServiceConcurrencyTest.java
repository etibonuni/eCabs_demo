package com.ecabs.demo.service;

import com.ecabs.demo.model.Driver;
import com.ecabs.demo.model.Ride;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RideMatchingServiceConcurrencyTest {

    private RideMatchingService rideMatchingService;

    @BeforeEach
    void setUp() {
        SimpMessagingTemplate messagingTemplate = Mockito.mock(SimpMessagingTemplate.class);
        rideMatchingService = new RideMatchingService(messagingTemplate);

        // Register exactly 10 available drivers at various locations
        for (int i = 1; i <= 10; i++) {
            rideMatchingService.registerDriver(i, i, "Test Driver "+i);
        }
    }

    @Test
    void testConcurrentRideRequests_ShouldOnlyAllocateAvailableDrivers() throws InterruptedException {
        int concurrentRiders = 50;
        int totalAvailableDrivers = 10;

        ExecutorService executorService = Executors.newFixedThreadPool(concurrentRiders);

        // Latches to control the exact timing of thread execution
        CountDownLatch readyLatch = new CountDownLatch(concurrentRiders); // Wait for threads to prep
        CountDownLatch startLatch = new CountDownLatch(1);                // The starting pistol
        CountDownLatch doneLatch = new CountDownLatch(concurrentRiders);  // Wait for threads to finish

        AtomicInteger successfulAllocations = new AtomicInteger(0);
        AtomicInteger failedAllocations = new AtomicInteger(0);

        for (int i = 0; i < concurrentRiders; i++) {
            executorService.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();

                    Ride ride = rideMatchingService.requestRide(0, 0, 10, 10);
                    if (ride != null) {
                        successfulAllocations.incrementAndGet();
                    } else {
                        failedAllocations.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();

        startLatch.countDown();

        doneLatch.await();

        // 1. Only 10 riders should have successfully gotten a driver
        assertEquals(totalAvailableDrivers, successfulAllocations.get(),
                "Exactly 10 drivers should have been successfully allocated.");

        // 2. The other 40 riders should have safely failed (no double bookings)
        assertEquals(concurrentRiders - totalAvailableDrivers, failedAllocations.get(),
                "40 requests should have safely failed due to lack of available drivers.");

        // 3. All 10 drivers must now be UNAVAILABLE
        List<Driver> availableDriversLeft = rideMatchingService.getAvailableDrivers(0, 0, 1000);
        assertEquals(0, availableDriversLeft.size(),
                "There should be 0 available drivers remaining.");

        executorService.shutdown();
    }
}
