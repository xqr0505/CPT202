package edu.xjtlu.cpt202.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("edu.xjtlu.cpt202.backend.modules.*.mapper")
@SpringBootApplication
@MapperScan("edu.xjtlu.cpt202.backend.modules.**.mapper")
@EnableScheduling
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
