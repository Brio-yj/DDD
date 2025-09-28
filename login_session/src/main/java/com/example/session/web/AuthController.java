package com.example.session.web;

import com.example.session.user.UserRepository;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request, HttpSession session) {
        String username = request.getOrDefault("username", "");
        String password = request.getOrDefault("password", "");

        return userRepository.findByUsername(username)
                .filter(user -> user.getPassword().equals(password))
                .map(user -> {
                    session.setAttribute("USER", user.getUsername());
                    return ResponseEntity.ok(Map.of("message", "login success", "username", user.getUsername()));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "invalid credentials")));
    }

    @GetMapping("/whoami")
    public ResponseEntity<?> whoami(HttpSession session) {
        Object user = session.getAttribute("USER");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "not authenticated"));
        }
        return ResponseEntity.ok(Map.of("username", user));
    }
}
