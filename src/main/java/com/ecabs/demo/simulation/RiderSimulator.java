package com.ecabs.demo.simulation;

import com.ecabs.demo.model.Ride;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static com.ecabs.demo.util.Utils.distance;

public class RiderSimulator {
    private record RideInfo ( Instant start, Double distance ) {}

    private static final Logger log = LoggerFactory.getLogger(RiderSimulator.class);
    private final RestTemplate restTemplate;
    private final SimulationProperties properties;
    private final Map<UUID, RideInfo> activeRides = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private final String baseUrl = "http://localhost:8080/api/v1"; // Correct base URL

    public RiderSimulator(SimulationProperties properties) {
        this.properties = properties;
        this.restTemplate = new RestTemplate();
    }

    public void simulate() {
        log.info("Starting Rider Simulator...");
        while (true) {
            try {
                adjustRideCount();
                completeActiveRides();
                Thread.sleep((long) (1500 / properties.getTimeMultiplier()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Rider simulator was interrupted", e);
                break;
            } catch (Exception e) {
                log.error("An error occurred in the rider simulator", e);
            }
        }
    }

    private void adjustRideCount() {
        int targetRideCount = (int) (properties.getRideCountAvg() + random.nextGaussian() * properties.getRideCountStdDev());
        while (activeRides.size() < targetRideCount) {
            requestNewRide();
        }
    }

    private void requestNewRide() {
        double lat = -90 + 180 * random.nextDouble();
        double lon = -180 + 360 * random.nextDouble();
        double dropLat = lat + (-10 + 20 * random.nextDouble());
        double dropLon = lon + (-10 + 20 * random.nextDouble());

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/rides")
                .queryParam("pickupLatitude", lat)
                .queryParam("pickupLongitude", lon)
                .queryParam("dropoffLatitude", dropLat)
                .queryParam("dropoffLongitude", dropLon)
                .toUriString();

        try {
            Ride newRide = restTemplate.postForObject(url, null, Ride.class);
            if (newRide != null) {
                activeRides.put(newRide.getId(), new RideInfo(Instant.now(), distance(newRide.getPickupLatitude(), newRide.getPickupLongitude(), newRide.getDropoffLatitude(), newRide.getDropoffLongitude())));
                log.info("Requested new ride: {}", newRide.getId());
            }
        } catch (Exception e) {
            log.warn("Failed to request new ride (likely no drivers available): {}", e.getMessage());
        }
    }

    private void completeActiveRides() {
        for (Iterator<Map.Entry<UUID, RideInfo>> iterator = activeRides.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<UUID, RideInfo> entry = iterator.next();
            UUID rideId = entry.getKey();
            Instant startTime = entry.getValue().start();
            double distance = entry.getValue().distance();

            long timeSinceStarted = (long)((Instant.now().toEpochMilli() - startTime.toEpochMilli()) * properties.getTimeMultiplier());

            // Make time roughly proportional to ride distance
            double randomRideTime = distance * properties.getRideTimePerDistance();
            double jitter = randomRideTime * 0.1; // 10% random variation
            randomRideTime += jitter - (
                    ThreadLocalRandom.current().nextLong(0, (long)(jitter * 2)) - 2*jitter);

            if (timeSinceStarted > randomRideTime) {
                try {
                    restTemplate.put(baseUrl + "/rides/{rideId}/complete", null, rideId);
                    iterator.remove(); // Safely remove the current item
                    log.info("Completed ride: {}", rideId);
                } catch (Exception e) {
                    log.error("Failed to complete ride {}: {}", rideId, e.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        SimulationProperties properties;
        String propsName = "/app/simulation.yaml";
        if (args.length > 0) {
            propsName = args[0];
        }
        log.info("Loading properties from file: {}", propsName);
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
        
        Map<String, Object> yamlContent = mapper.readValue(new File(propsName), new TypeReference<>() {});
        properties = mapper.convertValue(yamlContent.get("simulation"), SimulationProperties.class);

        RiderSimulator simulator = new RiderSimulator(properties);
        simulator.simulate();
    }
}
