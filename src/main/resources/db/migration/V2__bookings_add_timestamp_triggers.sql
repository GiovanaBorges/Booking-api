CREATE TRIGGER trg_bookings_created_at
BEFORE INSERT ON bookings
FOR EACH ROW
BEGIN
    SET NEW.created_at = IFNULL(NEW.created_at, NOW());
    SET NEW.updated_at = NOW();
END;

CREATE TRIGGER trg_bookings_updated_at
BEFORE UPDATE ON bookings
FOR EACH ROW
BEGIN
    SET NEW.updated_at = NOW();
END;