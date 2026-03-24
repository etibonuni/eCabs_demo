package com.ecabs.demo.config;

import com.ecabs.demo.simulation.SimulationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SimulationProperties.class)
public class SimulationConfig {

}
