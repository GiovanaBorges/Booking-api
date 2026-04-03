import { sleep, check } from "k6";
import { getToken } from "../../../utils/auth.js";
import {
  createBooking,
  deleteBooking
} from "../../../services/bookings.service.js";

const users = JSON.parse(open("../../../data/users.json"));

export const options = {
  vus: 5,
  duration: "1m"
};

export default function () {
  const user = users[Math.floor(Math.random() * users.length)];
  const token = getToken(user);

  const params = {
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json"
    }
  };

  const payload = JSON.stringify({
    providerId: 1,
    customerId: 1,
    startTs: "2026-01-10T14:00:00",
    endTs: "2026-01-10T15:00:00",
    status: "CONFIRMED"
  });

  const createRes = createBooking(payload, params);
  const bookingId = createRes.json("id");

  const res = deleteBooking(bookingId, params);

  check(res, {
    "delete booking": (r) => r.status === 200
  });

  sleep(1);
}