package com.api.batterymantra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BatteryMantraApplication {

	public static void main(String[] args) {
		SpringApplication.run(BatteryMantraApplication.class, args);
	}

}
