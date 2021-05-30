package com.example.uscan;

public class User {

    String timestamp, facescore;

    public User (){}

    public User(String timestamp, String facescore) {
        this.timestamp = timestamp;
        this.facescore = facescore;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getFacescore() {
        return facescore;
    }

    public void setFacescore(String facescore) {
        this.facescore = facescore;
    }
}
