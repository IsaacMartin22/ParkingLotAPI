WITH economy_extra_floors AS (
    SELECT floor.id
    FROM floors floor
    JOIN parking_lots parking_lot
        ON floor.parking_lot_id = parking_lot.id
    WHERE parking_lot.type = 'Economy'
      AND floor.name <> 'Floor 1'
),
economy_extra_sections AS (
    SELECT section.id
    FROM sections section
    JOIN economy_extra_floors floor
        ON section.floor_id = floor.id
),
economy_extra_parking_spaces AS (
    SELECT parking_space.id
    FROM parking_spaces parking_space
    JOIN economy_extra_sections section
        ON parking_space.section_id = section.id
)
DELETE FROM cars car
WHERE car.parking_space_id IN (
    SELECT id
    FROM economy_extra_parking_spaces
);

WITH economy_extra_floors AS (
    SELECT floor.id
    FROM floors floor
    JOIN parking_lots parking_lot
        ON floor.parking_lot_id = parking_lot.id
    WHERE parking_lot.type = 'Economy'
      AND floor.name <> 'Floor 1'
),
economy_extra_sections AS (
    SELECT section.id
    FROM sections section
    JOIN economy_extra_floors floor
        ON section.floor_id = floor.id
)
DELETE FROM parking_spaces parking_space
WHERE parking_space.section_id IN (
    SELECT id
    FROM economy_extra_sections
);

WITH economy_extra_floors AS (
    SELECT floor.id
    FROM floors floor
    JOIN parking_lots parking_lot
        ON floor.parking_lot_id = parking_lot.id
    WHERE parking_lot.type = 'Economy'
      AND floor.name <> 'Floor 1'
)
DELETE FROM sections section
WHERE section.floor_id IN (
    SELECT id
    FROM economy_extra_floors
);

DELETE FROM floors floor
USING parking_lots parking_lot
WHERE floor.parking_lot_id = parking_lot.id
  AND parking_lot.type = 'Economy'
  AND floor.name <> 'Floor 1';
