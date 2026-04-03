import { sleep, check } from "k6";
import { getToken } from "../../../utils/auth.js";
import {
  getOrCreateUser,
  updateUser
} from "../../../services/users.service.js";

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

  const meRes = getOrCreateUser(params);
  const userId = meRes.json("id");

  const payload = JSON.stringify({
    description: `Atualizado ${Math.random()}`,
    experienceYears: Math.floor(Math.random() * 10)
  });

  const res = updateUser(userId, payload, params);

  check(res, {
    "update user ok": (r) => r.status === 200
  });

  sleep(1);
}