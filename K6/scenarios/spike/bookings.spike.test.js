import { sleep, check } from "k6";
import { getToken } from "../../utils/auth.js";
import { createBooking } from "../../services/bookings.service.js";

const users = JSON.parse(open("../../data/users.json"));

export const options = {
   stages: [
    { duration: "5s", target: 5 },
    { duration: "10s", target: 5 },
    { duration: "5s", target: 0 }
  ]
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

  const res = createBooking(payload, params);

  check(res, {
    "booking criado": (r) => r.status === 200
  });

  sleep(1);
}