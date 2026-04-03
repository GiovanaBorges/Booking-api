import { sleep, check } from "k6";
import { getToken } from "../../../utils/auth.js";
import { 
  getOrCreateUser, 
  getUserById
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
      Authorization: `Bearer ${token}`
    }
  };

  // garante que existe
  const meRes = getOrCreateUser(params);
  const userId = meRes.json("id");

  const res = getUserById(userId, params);

  check(res, {
    "get user by id ok": (r) => r.status === 200
  });

  sleep(1);
}