CREATE TABLE parking_lots (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    address VARCHAR(255),
    type VARCHAR(255)
);

CREATE TABLE floors (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    parking_lot_id BIGINT,
    CONSTRAINT fk_floors_parking_lot
        FOREIGN KEY (parking_lot_id)
        REFERENCES parking_lots (id)
);

CREATE TABLE sections (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    floor_id BIGINT,
    CONSTRAINT fk_sections_floor
        FOREIGN KEY (floor_id)
        REFERENCES floors (id)
);

CREATE TABLE parking_spaces (
    id BIGSERIAL PRIMARY KEY,
    number VARCHAR(255),
    section_id BIGINT,
    CONSTRAINT fk_parking_spaces_section
        FOREIGN KEY (section_id)
        REFERENCES sections (id),
    color VARCHAR(255),
    make VARCHAR(255),
    model VARCHAR(255),
    manufacturing_year INTEGER,
    licensePlate VARCHAR(255)
);

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

