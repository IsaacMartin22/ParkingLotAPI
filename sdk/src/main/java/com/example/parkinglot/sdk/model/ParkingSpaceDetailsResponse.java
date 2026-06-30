package com.example.parkinglot.sdk.model;

public class ParkingSpaceDetailsResponse {
    private Long id;
    private String number;
    private boolean occupied;
    private CarResponse car;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    public CarResponse getCar() {
        return car;
    }

    public void setCar(CarResponse car) {
        this.car = car;
    }
}

