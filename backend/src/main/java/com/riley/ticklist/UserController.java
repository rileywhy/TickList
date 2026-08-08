package com.riley.ticklist;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;




@RestController
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder,  JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService= jwtService;
    }

    @GetMapping("/current_user")
    public CurrentUserResponse getCurrentUser(@AuthenticationPrincipal User user) {
       return new CurrentUserResponse(user.getEmail(), user.getFirstName(), user.getLastName());
    }

   @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {

    User user = userRepository.findByEmail(request.getEmail());

    if (user == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    boolean matches = passwordEncoder.matches(
        request.getPassword(),
        user.getPassword()
    );

    if (!matches) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    
    String token = jwtService.generateToken(user);
    return ResponseEntity.ok(new LoginResponse(
    user.getFirstName(),
    user.getLastName(),
    user.getEmail(),
    token
));
}

    @PostMapping("/register")
    public CurrentUserResponse register(@Valid @RequestBody RegisterRequest request) {
        // Deliberate account-enumeration oracle: a 409 tells a stranger this email
        // has an account. Accepted for now; revisit alongside the privacy model.
        if (userRepository.findByEmail(request.email()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }

        // Translate at the boundary: only these four fields cross; id stays server-owned.
        User user = new User();
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPassword(passwordEncoder.encode(request.password()));
        User saved = userRepository.save(user);

        return new CurrentUserResponse(saved.getEmail(), saved.getFirstName(), saved.getLastName());
    }

    
}
