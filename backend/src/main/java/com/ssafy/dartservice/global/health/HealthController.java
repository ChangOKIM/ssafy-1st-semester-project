package com.ssafy.dartservice.global.health;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
public class HealthController {

	@GetMapping("/api/v1/health")
	public String health() {
		return "backend ok";
	}
}
