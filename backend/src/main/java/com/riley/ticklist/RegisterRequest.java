package com.riley.ticklist;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The only fields a client may supply when creating an account. Server-owned
 * fields (id, the stored bcrypt hash) have no representation here, so a request
 * carrying them has nowhere to land -- the boundary, not a guard, prevents
 * mass assignment against the User entity.
 */
public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank String firstName,
    @NotBlank String lastName,
    @NotBlank @Size(min = 8, message = "Password must be at least 8 characters.") String password
) {}
