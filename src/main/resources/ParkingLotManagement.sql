INSERT INTO parking_lots (name, address, type) VALUES
('Terminal 1 Economy', '123 Airport Way Las Vegas', 'Economy'),
('Terminal 1 Long Term', '123 Airport Way Las Vegas', 'LongTerm'),
('Terminal 1 Short Term', '123 Airport Way Las Vegas', 'ShortTerm'),
('Terminal 3 Economy', '123 Airport Way Las Vegas', 'Economy'),
('Terminal 3 Long Term', '123 Airport Way Las Vegas', 'LongTerm'),
('Terminal 3 Short Term', '123 Airport Way Las Vegas', 'ShortTerm'),
('Waiting', '123 Airport Way Las Vegas', 'Waiting');

INSERT INTO floors (name, parking_lot_id)
SELECT 'Floor ' || floor_number, parking_lot.id
FROM parking_lots parking_lot
CROSS JOIN generate_series(1, 6) AS floor_number;

INSERT INTO sections (name, floor_id)
SELECT 'Section ' || section_number, floor.id
FROM floors floor
CROSS JOIN generate_series(1, 6) AS section_number;

INSERT INTO parking_spaces (number, section_id)
SELECT
    'S-' || ROW_NUMBER() OVER (
        PARTITION BY parking_lot.id
        ORDER BY floor.id, section.id, space_number
    ),
    section.id
FROM parking_lots parking_lot
JOIN floors floor ON floor.parking_lot_id = parking_lot.id
JOIN sections section ON section.floor_id = floor.id
CROSS JOIN generate_series(1, 10) AS space_number;

INSERT INTO cars (
    color,
    make,
    model,
    manufacturing_year,
    license_plate,
    parking_space_id
)
SELECT
    'Red',
    'Toyota',
    'Camry',
    2018,
    'ABC-123',
    parking_space.id
FROM parking_spaces parking_space
JOIN sections section ON parking_space.section_id = section.id
JOIN floors floor ON section.floor_id = floor.id
JOIN parking_lots parking_lot ON floor.parking_lot_id = parking_lot.id
WHERE parking_lot.name = 'Terminal 1 Economy'
  AND parking_space.number = 'S-1'
LIMIT 1;

INSERT INTO cars (
    color,
    make,
    model,
    manufacturing_year,
    license_plate,
    parking_space_id
)
SELECT
    'Blue',
    'Honda',
    'Civic',
    2020,
    'XYZ-789',
    parking_space.id
FROM parking_spaces parking_space
JOIN sections section ON parking_space.section_id = section.id
JOIN floors floor ON section.floor_id = floor.id
JOIN parking_lots parking_lot ON floor.parking_lot_id = parking_lot.id
WHERE parking_lot.name = 'Terminal 1 Economy'
  AND parking_space.number = 'S-2'
LIMIT 1;
