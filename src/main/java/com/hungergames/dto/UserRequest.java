package com.hungergames.dto;

import com.hungergames.model.User;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.Instant;

@Data
public class UserRequest {

    @NotBlank(message = "Username must not be blank")
    private String username;

    @Min(value = 0, message = "Starting balance must be non-negative")
    private double startingBalance = 1000.0;

    private String password;
}
