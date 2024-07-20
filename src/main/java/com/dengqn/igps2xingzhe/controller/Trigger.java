package com.dengqn.igps2xingzhe.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 同步
 *
 * @author dengqn
 * @since 2024/7/20 11:21
 */
@RestController
@RequestMapping("/api/trigger")
public class Trigger {

	@GetMapping("/sync/igps2xingzhe")
	public ResponseEntity<String> onSyncIGPS2XingZhe() {
		return ResponseEntity.ok("ok");
	}

}
