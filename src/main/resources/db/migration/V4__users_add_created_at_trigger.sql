CREATE TRIGGER trg_users_created_at
BEFORE INSERT ON users
FOR EACH ROW
BEGIN
    SET NEW.created_at = IFNULL(NEW.created_at, NOW());
END;