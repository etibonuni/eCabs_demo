package com.ecabs.demo.simulation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Set the simulation profile to enable the simulation threads within the server
@Component
@Profile("simulation")
public class SimulationRunner implements CommandLineRunner {

    private final SimulationProperties properties;

    @Autowired
    public SimulationRunner(SimulationProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(() -> {
            DriverSimulator driverSimulator = new DriverSimulator(properties);
            driverSimulator.simulate();
        });
        executor.submit(() -> {
            RiderSimulator riderSimulator = new RiderSimulator(properties);
            riderSimulator.simulate();
        });
    }
}
