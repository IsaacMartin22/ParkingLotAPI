package com.example.apiservice;

import com.example.apiservice.entity.Car;
import com.example.apiservice.entity.Floor;
import com.example.apiservice.entity.ParkingLot;
import com.example.apiservice.entity.ParkingLotType;
import com.example.apiservice.entity.ParkingSpace;
import com.example.apiservice.entity.Section;
import com.example.apiservice.repository.*;
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
    public CommandLineRunner seedData(ParkingLotRepository lotRepo, FloorRepository floorRepo, SectionRepository sectionRepo, ParkingSpaceRepository spaceRepo, CarRepository carRepo) {
        return args -> {
            // Names of parking lots to seed (keep the original plus additional ones)
            String[] lotNames = new String[] {
                    "Terminal 1 Economy",
                    "Terminal 1 Long Term",
                    "Terminal 1 Short Term",
                    "Terminal 3 Economy",
                    "Terminal 3 Long Term",
                    "Terminal 3 Short Term",
                    "Waiting"
            };

            for (int idx = 0; idx < lotNames.length; idx++) {
                String lotName = lotNames[idx];
                ParkingLot lot = new ParkingLot();
                lot.setName(lotName);
                lot.setAddress("123 Airport Way Las Vegas");

                // Infer type from the name where possible
                if (lotName.toLowerCase().contains("economy")) {
                    lot.setType(ParkingLotType.Economy);
                } else if (lotName.toLowerCase().contains("long")) {
                    lot.setType(ParkingLotType.LongTerm);
                } else if (lotName.toLowerCase().contains("short")) {
                    lot.setType(ParkingLotType.ShortTerm);
                } else if (lotName.toLowerCase().contains("waiting")) {
                    lot.setType(ParkingLotType.Waiting);
                } else {
                    lot.setType(ParkingLotType.Economy);
                }

                lot = lotRepo.save(lot);

                // Create sections and parking spaces on each floor
                List<ParkingSpace> spaces = new ArrayList<>();
                int spaceCounter = 1;

                // Create floors
                for (int f = 0; f < 6; f++) {
                    Floor floor = new Floor();
                    floor.setName("Floor-" + f);
                    floor.setParkingLot(lot);
                    floor = floorRepo.save(floor);

                    for (int sec = 0; sec < 6; sec++) {
                        Section section = new Section();
                        section.setName("Section-" + sec);
                        section.setFloor(floor);
                        section = sectionRepo.save(section);

                        // Create 10 parking spaces per section
                        for (int i = 0; i < 10; i++) {
                            ParkingSpace s = new ParkingSpace();
                            s.setNumber("S-" + spaceCounter);
                            s.setSection(section);
                            s = spaceRepo.save(s);
                            spaces.add(s);
                            spaceCounter++;
                        }
                    }
                }

                // For the very first lot only, create example cars (avoid duplicate license plates)
                if (idx == 0 && !spaces.isEmpty()) {
                    Car car1 = new Car();
                    car1.setMake("Toyota");
                    car1.setModel("Camry");
                    car1.setManufacturingYear(2018);
                    car1.setLicensePlate("ABC-123");
                    car1.setParkingSpace(spaces.get(0));
                    carRepo.save(car1);

                    spaceRepo.save(spaces.get(0));

                    Car car2 = new Car();
                    car2.setMake("Honda");
                    car2.setModel("Civic");
                    car2.setManufacturingYear(2020);
                    car2.setLicensePlate("XYZ-789");
                    car2.setParkingSpace(spaces.get(1));
                    carRepo.save(car2);
                }
            }
        };
    }
}
