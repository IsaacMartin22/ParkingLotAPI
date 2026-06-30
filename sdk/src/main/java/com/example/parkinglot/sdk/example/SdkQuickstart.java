package com.example.parkinglot.sdk.example;

import com.example.parkinglot.sdk.ParkingLotApiClient;
import com.example.parkinglot.sdk.model.CarCreateRequest;
import com.example.parkinglot.sdk.model.CarResponse;
import com.example.parkinglot.sdk.model.ParkingSpaceUpdateRequest;

public class SdkQuickstart {

    public static void main(String[] args) {
        String baseUrl = args.length > 0 ? args[0] : "http://localhost:8082";
        ParkingLotApiClient client = new ParkingLotApiClient(baseUrl);

        CarCreateRequest create = new CarCreateRequest();
        create.setColor("Blue");
        create.setMake("Toyota");
        create.setModel("Corolla");
        create.setManufacturingYear(2022);
        create.setLicensePlate("SDK-123");
        create.setParkingSpaceId(10L);

        CarResponse created = client.createCar(create);
        System.out.println("Created car ID: " + created.getId());

        ParkingSpaceUpdateRequest clearSpace = new ParkingSpaceUpdateRequest();
        clearSpace.setClearCar(true);
        client.updateParkingSpace(10L, clearSpace);
        System.out.println("Cleared car from parking space 10");
    }
}

