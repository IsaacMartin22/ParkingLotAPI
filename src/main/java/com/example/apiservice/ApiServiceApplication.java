package com.example.apiservice;

import com.example.apiservice.entity.Car;
import com.example.apiservice.entity.Level;
import com.example.apiservice.entity.ParkingLot;
import com.example.apiservice.entity.ParkingLotType;
import com.example.apiservice.entity.ParkingSpace;
import com.example.apiservice.entity.Section;
import com.example.apiservice.repository.CarRepository;
import com.example.apiservice.repository.LevelRepository;
import com.example.apiservice.repository.ParkingLotRepository;
import com.example.apiservice.repository.ParkingSpaceRepository;
import com.example.apiservice.repository.SectionRepository;
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
    public CommandLineRunner seedData(ParkingLotRepository lotRepo, LevelRepository levelRepo, SectionRepository sectionRepo, ParkingSpaceRepository spaceRepo, CarRepository carRepo) {
        return args -> {
            ParkingLot lot = new ParkingLot();
            lot.setName("Terminal 1 Economy");
            lot.setAddress("123 Airport Way Las Vegas");
            lot.setCapacity(60);
            lot.setType(ParkingLotType.Economy);
            lot = lotRepo.save(lot);

            // Create floors (levels)
            List<Level> levels = new ArrayList<>();
            for (int f = 1; f <= 2; f++) {
                Level level = new Level();
                level.setName("Floor-" + f);
                level.setParkingLot(lot);
                level = levelRepo.save(level);
                levels.add(level);
            }

            // Create sections and parking spaces in each level
            List<ParkingSpace> spaces = new ArrayList<>();
            int spaceCounter = 1;
            for (Level level : levels) {
                for (int sec = 1; sec <= 6; sec++) {
                    Section section = new Section();
                    section.setName("Section-" + sec);
                    section.setLevel(level);
                    section = sectionRepo.save(section);

                    // Create 10 parking spaces per section
                    for (int i = 1; i <= 10; i++) {
                        ParkingSpace s = new ParkingSpace();
                        s.setNumber("S-" + spaceCounter);
                        s.setSection(section);
                        s.setOccupied(false);
                        s = spaceRepo.save(s);
                        spaces.add(s);
                        spaceCounter++;
                    }
                }
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

