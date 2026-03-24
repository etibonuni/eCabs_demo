# Ride Matching Service

This is a simple ride matching service implemented in Java using Spring Boot as a submission to th eCabs technical interview practical assignment.

In addition to the requirements outlined in the assignment, this project also supplies a simple simulation environment
that exercises the core ride-sharing code, displaying the results in real-time to the user/examiner in a console window.

The webapp itself is packaged as a Spring Boot application and functions as a web server implementing a simple REST interface
as outlined in the requirements. In additional some extra classes are provided to enable real-time simulation of the 
ride-sharing service:

- Class DriverSimlator creates driver register/unregister request traffic which is configured via application.yaml or alternaticvely and external simulation.yaml file
- Class RiderSimulator similarly creates and ends ride requests which are also configurable
- Class SimulationUI connects to a websocket provided by the rideshare web service through which it gets real-time updates regarding driver and ride actions, displaying them to the user in real-time

All three classes provide a main() function so that they can be run independently of the main Spring application. Additionally, if the 
`simulation` profile is activated in the Spring app at startup, it will initiaise instances of DriverSimulator and RiderSimulator
internally to create dummy traffic. `SimulatorUI` is intended to be run exclusively as a stadalone console app.

## Requirements

- Java 17 or higher
- Maven
- Docker

## Setup

1. Clone the repository.
2. Open a terminal and navigate to the project's root directory.

## Building the application
```bash
mvn clean package
```
This will create two jar files:
- `demo-0.0.1-SNAPSHOT-exec.jar`: Fully packaged executable jar with dependencies used to run the web server
- `demo-0.0.1-SNAPSHOT.jar`: Simple jar with only the project classes enabling the simulation classes to be run directly via the main() functions

## Running the application

To run the application, execute the following command in the terminal:

```bash
java -jar target/demo-0.0.1-SNAPSHOT-exec.jar
```

Alternatively, in order to enable the simulation classes:
```bash
java -Dspring.profiles.active=simulation -jar target/demo-0.0.1-SNAPSHOT-exec.jar
```

The application will start on port 8080.
The SimulationUI can be run from the command line as follows:
```bash
java -cp "`pwd`/target/demo-0.0.1-SNAPSHOT.jar:`pwd`/target/libs/*" com.ecabs.demo.ui.SimulationUI simulation.yaml
```

Alternatively, for ease of testing/deployment two dockerfiles have been provided:
- `Dockerfile` will create a simple image that only runs the webapp with no simulation activity. Suggested command: 
```bash
docker build --no-cache -f Dockerfile -t rideshare .
run --rm -p 8080:8080 rideshare
```
- `Dockerfile.simulation` will create an image that runs the web server, driver simulator, rider simulator and SimulationUI in separate processes 
in the same container. This simplifies quickly setting up the full simulation environment.
```bash
docker build --no-cache -f Dockerfile.simulation -t simulation-image .
docker run --rm simulation-image
```
Alternatively, in order to experiment with simulation properties without rebuilding the image with every change,
it is possible to map a custom simulation.yaml file when running the image:
```bash
run --rm -v "`pwd`/simulation.yaml:/app/simulation.yaml" simulation-image
```

## API Endpoints
The Swagger OpenAI API documentation can be accessed at `http://localhost:8080/swagger-ui/index.html#/ride-controller/removeDriver`

- `PUT /api/v1/rides/{rideId}/complete`: Mark a ride as complete.
- `PUT /api/v1/drivers/{driverId}/location?latitude={lat}&longitude={lon}`: Update a driver's location.
- `POST /api/v1/rides?pickupLatitude={lat}&pickupLongitude={lon}`: Request a new ride.
- `POST /api/v1/drivers?latitude={lat}&longitude={lon}&nane=name`: Register a new driver.
- `GET /api/v1/drivers/available?latitude={lat}&longitude={lon}&limit={limit}`: Get a list of available drivers.
- `DELETE /api/v1/drivers/{driverId}`: Unregister a driver

