package com.grupocinco.mrelote;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MreloteApplication {

	public static void main(String[] args) {
		SpringApplication.run(MreloteApplication.class, args);
	}

}
