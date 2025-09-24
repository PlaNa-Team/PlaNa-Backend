package com.plana;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication // (exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@EnableScheduling
public class PlaNaApplication {

	public static void main(String[] args) {
		System.out.println("Hello World! 1");
		SpringApplication.run(PlaNaApplication.class, args);
		System.out.println("Hello World! 2");
	}

}
