CREATE OR REPLACE FUNCTION enforce_floor_limit()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    sibling_count BIGINT;
BEGIN
    IF NEW.parking_lot_id IS NULL THEN
        RETURN NEW;
    END IF;

    SELECT COUNT(*)
    INTO sibling_count
    FROM floors
    WHERE parking_lot_id = NEW.parking_lot_id
      AND id <> COALESCE(NEW.id, -1);

    IF sibling_count >= 6 THEN
        RAISE EXCEPTION 'Parking lot % cannot have more than 6 floors', NEW.parking_lot_id
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION enforce_section_limit()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    sibling_count BIGINT;
BEGIN
    IF NEW.floor_id IS NULL THEN
        RETURN NEW;
    END IF;

    SELECT COUNT(*)
    INTO sibling_count
    FROM sections
    WHERE floor_id = NEW.floor_id
      AND id <> COALESCE(NEW.id, -1);

    IF sibling_count >= 10 THEN
        RAISE EXCEPTION 'Floor % cannot have more than 10 sections', NEW.floor_id
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION enforce_space_limit()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    sibling_count BIGINT;
BEGIN
    IF NEW.section_id IS NULL THEN
        RETURN NEW;
    END IF;

    SELECT COUNT(*)
    INTO sibling_count
    FROM parking_spaces
    WHERE section_id = NEW.section_id
      AND id <> COALESCE(NEW.id, -1);

    IF sibling_count >= 10 THEN
        RAISE EXCEPTION 'Section % cannot have more than 10 parking spaces', NEW.section_id
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_enforce_floor_limit
BEFORE INSERT OR UPDATE OF parking_lot_id ON floors
FOR EACH ROW
EXECUTE FUNCTION enforce_floor_limit();

CREATE TRIGGER trg_enforce_section_limit
BEFORE INSERT OR UPDATE OF floor_id ON sections
FOR EACH ROW
EXECUTE FUNCTION enforce_section_limit();

CREATE TRIGGER trg_enforce_space_limit
BEFORE INSERT OR UPDATE OF section_id ON parking_spaces
FOR EACH ROW
EXECUTE FUNCTION enforce_space_limit();

