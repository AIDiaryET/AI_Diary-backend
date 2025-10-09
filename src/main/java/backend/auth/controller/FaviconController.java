package backend.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FaviconController {

    // 파비콘 요청을 204 No Content로 처리 (경고 없애기)
    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
