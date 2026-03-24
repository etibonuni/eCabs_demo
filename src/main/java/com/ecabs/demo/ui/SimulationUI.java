package com.ecabs.demo.ui;

import com.ecabs.demo.dto.event.*;
import com.ecabs.demo.dto.event.*;
import com.ecabs.demo.model.Driver;
import com.ecabs.demo.model.Ride;
import com.ecabs.demo.simulation.SimulationProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.ecabs.demo.util.Utils.distance;

public class SimulationUI {
    private static final Logger log = LoggerFactory.getLogger(SimulationUI.class);
    private static final String URL = "ws://localhost:8080/ws/simulation";
    private final SimulationProperties properties;
    private final Map<UUID, Driver> drivers = new ConcurrentHashMap<>();
    private final Map<UUID, Ride> rides = new ConcurrentHashMap<>();
    private final Map<UUID, Long> completedRidesByDriver = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Long simElapsedTime = 0L;

    public SimulationUI(SimulationProperties properties) {
        this.properties = properties;
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

        new SimulationUI(properties).run();
    }

    public void run() {
        WebSocketStompClient stompClient = createStompClient();
        StompSessionHandlerAdapter sessionHandler = new SimpSessionHandler(this);

        System.out.println("Connecting to WebSocket at " + URL + "...");
        CompletableFuture<StompSession> sessionFuture = stompClient.connectAsync(URL, sessionHandler);
        sessionFuture.exceptionally(ex -> {
            System.err.println("Failed to connect to WebSocket: " + ex.getMessage());
            System.exit(1);
            return null;
        });

        try {
            while (true) {
                render();
                Thread.sleep((long)(1000 / properties.getTimeMultiplier()));
                simElapsedTime += 1L;
            }
        } catch (InterruptedException e) {
            System.out.println("Simulation UI interrupted. Shutting down.");
            Thread.currentThread().interrupt();
        }
    }

    private WebSocketStompClient createStompClient() {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("wss-heartbeat-client-");
        scheduler.initialize();
        stompClient.setTaskScheduler(scheduler);
        stompClient.setDefaultHeartbeat(new long[]{10000, 10000});
        return stompClient;
    }

    private class SimpSessionHandler extends StompSessionHandlerAdapter {
        private final SimulationUI ui;

        public SimpSessionHandler(SimulationUI ui) {
            this.ui = ui;
        }

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            System.out.println("Connected. Subscribing to topics and requesting initial state...");
            session.subscribe("/topic/simulation", this);
            session.subscribe("/user/queue/initial-state", this);

            // Request a snapshot of the current state
            session.send("/app/request-initial-state", null);
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return byte[].class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            String destination = headers.getDestination();
            if (destination == null || !(payload instanceof byte[])) return;

            byte[] rawPayload = (byte[]) payload;

            try {
                if (destination.equals("/user/queue/initial-state")) {
                    InitialStateEvent event = objectMapper.readValue(rawPayload, InitialStateEvent.class);
                    ui.handleInitialState(event);
                } else if (destination.equals("/topic/simulation")) {
                    WebSocketEvent<?> event = objectMapper.readValue(rawPayload, new TypeReference<WebSocketEvent<?>>() {});
                    ui.handleEvent(event);
                }
            } catch (Exception e) {
                System.err.println("Error processing frame: " + e.getMessage());
                e.printStackTrace();
            }
            ui.render();
        }
        
        @Override
        public void handleException(StompSession s, StompCommand c, StompHeaders h, byte[] p, Throwable ex) {
            System.err.println("Error in WebSocket session: " + ex.getMessage());
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            System.err.println("Transport error: " + exception.getMessage());
        }
    }

    public void handleInitialState(InitialStateEvent event) {
        this.drivers.clear();
        this.rides.clear();
        this.completedRidesByDriver.clear();

        event.drivers().forEach(d -> this.drivers.put(d.getId(), d));
        event.rides().forEach(r -> this.rides.put(r.getId(), r));
        this.completedRidesByDriver.putAll(event.completedRidesByDriver());

        log.info("Initial State Received");
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleEvent(WebSocketEvent<?> event) {
        switch (event.getType()) {
            case DRIVER_REGISTERED -> {
                DriverRegisteredEvent payload = objectMapper.convertValue(event.getPayload(), DriverRegisteredEvent.class);
                drivers.put(payload.driver().getId(), payload.driver());
                completedRidesByDriver.putIfAbsent(payload.driver().getId(), 0L);
            }
            case DRIVER_UNREGISTERED -> {
                DriverUnregisteredEvent payload = objectMapper.convertValue(event.getPayload(), DriverUnregisteredEvent.class);
                drivers.remove(payload.driverId());
            }
            case RIDE_STARTED -> {
                RideStartedEvent payload = objectMapper.convertValue(event.getPayload(), RideStartedEvent.class);
                rides.put(payload.ride().getId(), payload.ride());
                // Mark the driver as unavailable
                Driver driver = drivers.get(payload.ride().getDriverId());
                if (driver != null) {
                    driver.setAvailable(false);
                }
            }
            case RIDE_COMPLETED -> {
                RideCompletedEvent payload = objectMapper.convertValue(event.getPayload(), RideCompletedEvent.class);
                completedRidesByDriver.compute(payload.driverId(), (id, count) -> (count == null) ? 1 : count + 1);
                // Mark the driver as available
                Driver driver = drivers.get(payload.driverId());
                if (driver != null) {
                    Ride ride = rides.get(payload.rideId());
                    driver.setLatitude(ride.getDropoffLatitude());
                    driver.setLongitude(ride.getDropoffLongitude());

                    driver.setAvailable(true);
                }
                rides.remove(payload.rideId());
            }
        }
    }

    public synchronized void render() {
        clearConsole();

        var availableDrivers = drivers.values().stream().filter(Driver::isAvailable).toList();
        var activeRides = rides.values();

        String leftColumnTitle = "--- AVAILABLE DRIVERS (" + availableDrivers.size() + ") ---";
        String rightColumnTitle = "--- ACTIVE RIDES (" + activeRides.size() + ") ---";
        String header = String.format("%-70s %s", leftColumnTitle, rightColumnTitle);

        System.out.println("--- eCabs Real-Time Simulation --- | Elapsed time: " + simElapsedTime + "s (x" + properties.getTimeMultiplier() + ")");
        System.out.println(header);
        System.out.println(new String(new char[header.length()]).replace("\0", "-"));

        int maxRows = Math.max(availableDrivers.size(), activeRides.size());

        var ridesList = activeRides.stream().toList();
        for (int i = 0; i < maxRows; i++) {
            String left = "";
            if (i < availableDrivers.size()) {
                Driver driver = availableDrivers.get(i);
                long completedRides = completedRidesByDriver.getOrDefault(driver.getId(), 0L);
                left = formatDriver(driver, completedRides);
            }
            String right = (i < ridesList.size()) ? formatRide(ridesList.get(i)) : "";
            System.out.printf("%-70s %s\n", left, right);
        }
    }

    private void clearConsole() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (IOException | InterruptedException ex) {
            // Fallback
            for (int i = 0; i < 50; ++i) System.out.println();
        }
    }

    private String formatDriver(Driver driver, long completedRides) {
        return String.format("Driver %s | Rides: %-3d | Loc: (%.2f, %.2f)",
                String.format("%-10s", driver.getName()),
                completedRides,
                driver.getLatitude(), driver.getLongitude());
    }

    private String formatRide(Ride ride) {
        return String.format("Ride %s | Driver: %s | (%.2f, %.2f) -> (%.2f, %.2f) (Dist = %.2f)",
                ride.getId().toString().substring(0, 8),
                String.format("%-10s", drivers.get(ride.getDriverId()).getName()),
                ride.getPickupLatitude(), ride.getPickupLongitude(),
                ride.getDropoffLatitude(), ride.getDropoffLongitude(),
                distance(ride.getPickupLatitude(), ride.getPickupLongitude(), ride.getDropoffLatitude(), ride.getDropoffLongitude()));
    }
}
