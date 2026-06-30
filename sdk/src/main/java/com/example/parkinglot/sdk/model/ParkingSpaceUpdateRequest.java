package com.example.parkinglot.sdk.model;

public class ParkingSpaceUpdateRequest {
    private String number;
    private Boolean clearCar;

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Boolean getClearCar() {
        return clearCar;
    }

    public void setClearCar(Boolean clearCar) {
        this.clearCar = clearCar;
    }
}

