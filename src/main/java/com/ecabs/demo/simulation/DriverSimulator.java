package com.ecabs.demo.simulation;

import com.ecabs.demo.model.Driver;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import net.datafaker.Faker;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class DriverSimulator {

    private static final Logger log = LoggerFactory.getLogger(DriverSimulator.class);
    private final RestTemplate restTemplate;
    private final SimulationProperties properties;
    private final Map<UUID, Instant> registeredDrivers = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private final String baseUrl = "http://localhost:8080/api/v1";
    private final Faker faker = new Faker();

    public DriverSimulator(SimulationProperties properties) {
        this.properties = properties;
        this.restTemplate = new RestTemplate();
    }

    public void simulate() {
        log.info("Starting Driver Simulator...");
        while (true) {
            try {
                adjustDriverCount();
                unregisterInactiveDrivers();
                Thread.sleep((long) (1000 / properties.getTimeMultiplier()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Driver simulator was interrupted", e);
                break;
            } catch (Exception e) {
                log.error("An error occurred in the driver simulator", e);
            }
        }
    }

    private void adjustDriverCount() {
        int targetDriverCount = (int) (properties.getDriverCountAvg() + random.nextGaussian() * properties.getDriverCountStdDev());

        while (registeredDrivers.size() < targetDriverCount) {
            registerNewDriver();
        }
    }

    private void registerNewDriver() {
        double lat = -90 + 180 * random.nextDouble();
        double lon = -180 + 360 * random.nextDouble();

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/drivers")
                .queryParam("latitude", lat)
                .queryParam("longitude", lon)
                .queryParam("name", faker.name().firstName())
                .toUriString();

        try {
            Driver newDriver = restTemplate.postForObject(url, null, Driver.class);
            if (newDriver != null) {
                registeredDrivers.put(newDriver.getId(), Instant.now());
                log.info("Registered new driver: {}", newDriver.getId());
            }
        } catch (Exception e) {
            log.error("Failed to register new driver: {}", e.getMessage());
        }
    }

    private void unregisterInactiveDrivers() {
        for (Iterator<Map.Entry<UUID, Instant>> iterator = registeredDrivers.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<UUID, Instant> entry = iterator.next();
            UUID driverId = entry.getKey();
            Instant registeredTime = entry.getValue();
            long timeSinceRegistered = (long)((Instant.now().toEpochMilli() - registeredTime.toEpochMilli()) * properties.getTimeMultiplier());
            long randomUnregisterTime = ThreadLocalRandom.current().nextLong(properties.getDriverMinRegisteredTime(), properties.getDriverMaxRegisteredTime());

            if (timeSinceRegistered > randomUnregisterTime) {
                try {
                    restTemplate.delete(baseUrl + "/drivers/{driverId}", driverId);
                    iterator.remove(); // Safely remove the current item
                    log.info("Unregistered driver: {}", driverId);
                } catch (Exception e) {
                    log.error("Failed to unregister driver {}: {}", driverId, e.getMessage());
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

        DriverSimulator simulator = new DriverSimulator(properties);
        simulator.simulate();
    }
}
