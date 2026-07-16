package com.example.david_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;



@EnableScheduling
@SpringBootApplication
public class DavidApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(DavidApiApplication.class, args);
	}

}


