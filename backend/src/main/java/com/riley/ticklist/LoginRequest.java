package com.riley.ticklist;

public class LoginRequest {
    private String email;
    private String password;
    private String firstNameString;
    private String lastNameString;


    public LoginRequest() {
    }

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    

    public LoginRequest(String email, String firstNameString, String lastNameString){
        email = this.email;
        firstNameString = this.firstNameString;
        lastNameString = this.lastNameString;
        
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    public void setFirstName (String firstNameString)
    {
        this.firstNameString = firstNameString;
    }

    public String getFirstName()
    {
        return firstNameString;
    }

    public void setLastName (String lastNameString)
    {
        this.lastNameString = lastNameString;
    }

    public String getLastName()
    {
        return lastNameString;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }




}
