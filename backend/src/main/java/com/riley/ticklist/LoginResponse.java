package com.riley.ticklist;

public class LoginResponse {
    private String firstName;
    private String lastName;
    private String email;
    private String token;

    public LoginResponse(String firstName, String lastName, String email, String token) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.token = token;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getToken()
    {
        return token;
    }
}