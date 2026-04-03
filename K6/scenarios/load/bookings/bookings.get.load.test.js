import { sleep, check } from "k6";
import { getToken } from "../../../utils/auth.js";
import { getBooking } from "../../../services/bookings.service.js";

const users = JSON.parse(open("../../../../data/users.json"));

export const options = {
  vus: 5,
  duration: "1m"
};

export default function () {
  const user = users[Math.floor(Math.random() * users.length)];
  const token = getToken(user);

  const params = {
    headers: {
      Authorization: `Bearer ${token}`
    }
  };

  const randomId = Math.floor(Math.random() * 10) + 1;

  const res = getBooking(randomId, params);

  check(res, {
    "get booking": (r) => r.status === 200
  });

  sleep(1);
}