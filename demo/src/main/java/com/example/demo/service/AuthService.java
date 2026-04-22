package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.Model.ERole;
import com.example.demo.Model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.UserDetailsImpl;
import com.example.demo.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public String registerUser(SignupRequest request) {
        if (userRepository.existsByUsername(request.getUsername()))
            throw new RuntimeException("Username is already taken!");

        if (userRepository.existsByEmail(request.getEmail()))
            throw new RuntimeException("Email is already in use!");

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Set<ERole> roles = new HashSet<>();
        if (request.getRoles() == null || request.getRoles().isEmpty()) {
            roles.add(ERole.ROLE_USER);
        } else {
            request.getRoles().forEach(role -> {
                switch (role.toLowerCase()) {
                    case "admin" -> roles.add(ERole.ROLE_ADMIN);
                    default -> roles.add(ERole.ROLE_USER);
                }
            });
        }
        user.setRoles(roles);
        userRepository.save(user);

        return "User registered successfully!";
    }

    public JwtResponse loginUser(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return new JwtResponse(jwt, userDetails.getId(),
                userDetails.getUsername(), userDetails.getEmail(), roles);
    }
}