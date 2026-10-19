package com.oinkvalley.board_svc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** board-svc 진입점. */
@SpringBootApplication
@EnableScheduling
public class BoardSvcApplication {

	public static void main(String[] args) {
		SpringApplication.run(BoardSvcApplication.class, args);
	}
}
