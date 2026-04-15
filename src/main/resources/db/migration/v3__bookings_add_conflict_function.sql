CREATE FUNCTION has_booking_conflict(
    p_provider_id BIGINT,
    p_start TIMESTAMP,
    p_end TIMESTAMP
)
RETURNS BOOLEAN
DETERMINISTIC
BEGIN
    DECLARE conflict_count INT;

    SELECT COUNT(*)
    INTO conflict_count
    FROM bookings
    WHERE provider_id = p_provider_id
      AND (start_ts <= p_end AND end_ts >= p_start);

    RETURN conflict_count > 0;
END;