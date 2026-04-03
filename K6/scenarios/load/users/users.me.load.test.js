import { sleep, check } from "k6";

import { getToken } from "../../../utils/auth.js";
import {
  getOrCreateUser,
  getUserById,
  updateUser,
  getUserBySkill
} from "../../../services/users.service.js";

const users = JSON.parse(open("../../../data/users.json"));

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

  // ==========================
  // 1. CREATE OR GET (/me)
  // ==========================
  let res = getOrCreateUser(params);

  check(res, {
    "user criado ou obtido": (r) => r.status === 200
  });

  const userId = res.json("id");

  // ==========================
  // 2. GET BY ID
  // ==========================
  res = getUserById(userId, params);

  check(res, {
    "get user by id": (r) => r.status === 200
  });

  // ==========================
  // 3. UPDATE USER
  // ==========================
  const updatePayload = JSON.stringify({
    description: "Atualizado via k6",
    linkedinProfile: "https://linkedin.com/test",
    githubProfile: "https://github.com/test",
    experienceYears: 3
  });

  res = updateUser(userId, updatePayload, params);

  check(res, {
    "update user": (r) => r.status === 200
  });

  // ==========================
  // 4. GET BY SKILL
  // ==========================
  res = getUserBySkill("JAVA", params);

  check(res, {
    "get users by skill": (r) => r.status === 200
  });

  sleep(1);
}