package com.example.apiservice;

import com.example.apiservice.entity.Car;
import com.example.apiservice.entity.ParkingLot;
import com.example.apiservice.entity.ParkingSpace;
import com.example.apiservice.repository.CarRepository;
import com.example.apiservice.repository.ParkingLotRepository;
import com.example.apiservice.repository.ParkingSpaceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class ApiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiServiceApplication.class, args);
    }

    // Seed some sample data at startup
    @Bean
    public CommandLineRunner seedData(ParkingLotRepository lotRepo, ParkingSpaceRepository spaceRepo, CarRepository carRepo) {
        return args -> {
            ParkingLot lot = new ParkingLot();
            lot.setName("Downtown Lot");
            lot.setAddress("123 Main St");
            lot.setCapacity(4);
            lot = lotRepo.save(lot);

            List<ParkingSpace> spaces = new ArrayList<>();
            for (int i = 1; i <= 4; i++) {
                ParkingSpace s = new ParkingSpace();
                s.setNumber("S-" + i);
                s.setLevel(1);
                s.setOccupied(false);
                s.setParkingLot(lot);
                s = spaceRepo.save(s);
                spaces.add(s);
            }

            Car car1 = new Car();
            car1.setMake("Toyota");
            car1.setModel("Camry");
            car1.setManufacturingYear(2018);
            car1.setLicensePlate("ABC-123");
            car1.setParked(true);
            car1.setParkingSpace(spaces.get(0));
            carRepo.save(car1);

            spaces.get(0).setOccupied(true);
            spaceRepo.save(spaces.get(0));

            Car car2 = new Car();
            car2.setMake("Honda");
            car2.setModel("Civic");
            car2.setManufacturingYear(2020);
            car2.setLicensePlate("XYZ-789");
            car2.setParked(false);
            carRepo.save(car2);
        };
    }
}

