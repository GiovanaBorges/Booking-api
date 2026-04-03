import { sleep, check } from "k6";
import { getToken } from "../../utils/auth.js";
import {
  createBooking,
  getBooking,
  updateBooking,
  deleteBooking
} from "../../services/bookings.service.js";

const users = JSON.parse(open("../../data/users.json"));

export const options = {
  stages: [
    { duration: "30s", target: 5 },
    { duration: "1m", target: 10 },
    { duration: "30s", target: 0 }
  ],
  thresholds: {
    http_req_duration: ["p(95)<500"],
    http_req_failed: ["rate<0.01"]
  }
};

export default function () {
  const user = users[(__VU - 1) % users.length];
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

  // CREATE
  let res = createBooking(payload, params);
  check(res, { "create booking": (r) => r.status === 200 });

  const bookingId = res.json("id");

  // GET
  res = getBooking(bookingId, params);
  check(res, { "get booking": (r) => r.status === 200 });

  // UPDATE
  res = updateBooking(bookingId, payload, params);
  check(res, { "update booking": (r) => r.status === 200 });

  // DELETE
  res = deleteBooking(bookingId, params);
  check(res, { "delete booking": (r) => r.status === 200 });

  sleep(1);
}