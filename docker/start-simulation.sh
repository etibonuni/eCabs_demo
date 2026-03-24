#!/bin/sh

cleanup() {
    echo "Shutting down simulation..."
    if [ ! -z "$APP_PID" ] && kill -0 $APP_PID 2>/dev/null; then kill $APP_PID; fi
    if [ ! -z "$DRIVER_SIM_PID" ] && kill -0 $DRIVER_SIM_PID 2>/dev/null; then kill $DRIVER_SIM_PID; fi
    if [ ! -z "$RIDER_SIM_PID" ] && kill -0 $RIDER_SIM_PID 2>/dev/null; then kill $RIDER_SIM_PID; fi
    if [ ! -z "$UI_PID" ] && kill -0 $UI_PID 2>/dev/null; then kill $UI_PID; fi
}

trap cleanup INT TERM

LOG_DIR="/app/logs"
mkdir -p $LOG_DIR

CP="/app/app.jar:/app/libs/*"

echo "Starting Web Service..."
# The main web service uses the executable JAR
java -jar /app/app-exec.jar > "$LOG_DIR/app.log" 2>&1 &
APP_PID=$!

echo "Waiting for the main web service to be ready..."
i=0
while [ $i -lt 60 ]; do
    if curl -s -o /dev/null http://localhost:8080/ws/simulation; then
        echo "Web service is ready."
        break
    fi
    echo "Service not ready yet. Retrying in 1 second..."
    sleep 1
    i=$((i+1))
done

if [ $i -eq 60 ]; then
    echo "Web service failed to start in 60 seconds. Exiting."
    exit 1
fi

echo "Starting Driver Simulator..."
java -cp $CP com.ecabs.demo.simulation.DriverSimulator /app/simulation.yaml > "$LOG_DIR/driver-sim.log" 2>&1 &
DRIVER_SIM_PID=$!

echo "Starting Rider Simulator..."
java -cp $CP com.ecabs.demo.simulation.RiderSimulator /app/simulation.yaml > "$LOG_DIR/rider-sim.log" 2>&1 &
RIDER_SIM_PID=$!

echo "Starting Simulation UI..."
java -cp $CP com.ecabs.demo.ui.SimulationUI &
UI_PID=$!

echo "Simulation is running. Waiting for UI process (PID: $UI_PID) to exit..."
wait $UI_PID

echo "UI process has exited. Cleaning up other services."
cleanup
