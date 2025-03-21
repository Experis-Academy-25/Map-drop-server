package com.mapdrop.controllers;

import com.mapdrop.models.ERole;
import com.mapdrop.models.Role;
import com.mapdrop.models.User;
import com.mapdrop.payload.request.LoginRequest;
import com.mapdrop.payload.request.SignupRequest;
import com.mapdrop.payload.response.JwtResponse;
import com.mapdrop.payload.response.MessageResponse;
import com.mapdrop.repository.RoleRepository;
import com.mapdrop.repository.UserRepository;
import com.mapdrop.security.jwt.JwtUtils;
import com.mapdrop.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin
@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        // If using a salt for password use it here
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream().map((item) -> item.getAuthority())
                .collect(Collectors.toList());
        return ResponseEntity
                .ok(new JwtResponse(jwt, userDetails.getId(), userDetails.getUsername(), userDetails.getEmail(), roles));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signupRequest) {
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken"));
        }
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
        }

        // Check if valid email format
        // This regex can probably be simplified
        if (!signupRequest.getEmail().matches("[[[a-z]|[0-9]]+[.]*]*@[[[a-z]|[0-9]]+[.]*]*[[a-z]|[0-9]]+[.]+[[a-z][0-9]]+")) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is not valid format"));
        }

        // Check if valid password
        if (signupRequest.getPassword().length() < 8 ||
                // What it does: for each constraint like "contains at least one of", checks if valid
                !signupRequest.getPassword().matches(".*[-!\"#$%&()*,./:;?@^_`{|}~+<=>].*") ||
                !signupRequest.getPassword().matches(".*[a-z].*") ||
                !signupRequest.getPassword().matches(".*[A-Z].*") ||
                !signupRequest.getPassword().matches(".*[0-9].*")
        ) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Password not following complexity requirements"));
        }

        // Create a new user add salt here if using one
        User user = new User(signupRequest.getUsername(), signupRequest.getEmail(), encoder.encode(signupRequest.getPassword()));
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        Set<String> strRoles = signupRequest.getRole();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null) {
            Role userRole = roleRepository.findByName(ERole.ROLE_USER).orElseThrow(() -> new RuntimeException("Error: Role is not found"));
            roles.add(userRole);
        } else {
            // This is a placeholder for if more roles are going to be added -- it doesn't really make sense like this
            strRoles.forEach((role) -> {
                switch (role) {
                    // Case: role "user" was provided in the RequestBody
                    case "user":
                        Role adminRole = roleRepository.findByName(ERole.ROLE_USER).orElseThrow(() -> new RuntimeException("Error: Role is not found"));
                        roles.add(adminRole);
                        break;
                    // Case: the provided role does not match any roles -> use ROLE_USER by default
                    default:
                        Role userRole = roleRepository.findByName(ERole.ROLE_USER).orElseThrow(() -> new RuntimeException("Error: Role is not found"));
                        roles.add(userRole);
                        break;
                }
            });
        }
        user.setRoles(roles);
        userRepository.save(user);
        return ResponseEntity.ok((new MessageResponse("User registered successfully")));
    }
}