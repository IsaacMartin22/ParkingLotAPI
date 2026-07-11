package apiservice.service;
import apiservice.dbentity.Floor;
import apiservice.dbentity.ParkingLot;
import apiservice.dbentity.ParkingSpace;
import apiservice.dbentity.Section;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class FloorContextResolver {

    public record FloorContext(Long lotId, Long floorId, Long spaceId) {
    }


    public Optional<FloorContext> resolve(ParkingSpace space) {
        if (space == null) {
            return Optional.empty();
        }

        Section section = space.getSection();
        if (section == null) {
            return Optional.empty();
        }

        Floor floor = section.getFloor();
        if (floor == null) {
            return Optional.empty();
        }

        ParkingLot lot = floor.getParkingLot();
        if (lot == null) {
            return Optional.empty();
        }

        return Optional.of(new FloorContext(lot.getId(), floor.getId(), space.getId()));
    }
}

