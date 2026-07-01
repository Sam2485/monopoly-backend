package com.BusinessGame.Vyapar.controller;

import com.BusinessGame.Vyapar.dto.ApiResponse;
import com.BusinessGame.Vyapar.dto.LoginRequest;
import com.BusinessGame.Vyapar.dto.LoginResponse;
import com.BusinessGame.Vyapar.service.AuthenticationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authenticationService.login(request);
        return ApiResponse.success(response, "Login successful");
    }
}
