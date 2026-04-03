import { sleep, check } from "k6";

import { getToken } from "../../utils/auth.js";
import {
  getOrCreateUser,
  getUserById,
  updateUser,
  getUserBySkill
} from "../../services/users.service.js";

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

  const skillsPool = [
  "JAVA", "SPRING_BOOT", "JAVASCRIPT", "REACT", "ANGULAR",
  "PYTHON", "DJANGO", "FLASK", "C_SHARP", "DOTNET",
  "RUBY", "RUBY_ON_RAILS", "PHP", "LARAVEL", "GO",
  "KOTLIN", "SWIFT", "TYPESCRIPT", "HTML", "CSS",
  "SQL", "POSTGRESQL", "MYSQL", "MONGODB",
  "DOCKER", "KUBERNETES", "AWS", "AZURE", "GCP",
  "MACHINE_LEARNING", "AI", "DATA_SCIENCE",
  "DEVOPS", "MOBILE_DEVELOPMENT", "UX_UI",
  "GRAPHQL", "REST_API", "TEST_AUTOMATION"
];

function getRandomSkills() {
  const shuffled = skillsPool.sort(() => 0.5 - Math.random());

  const count = Math.floor(Math.random() * 5) + 1; // 1 a 5 skills

  return shuffled.slice(0, count);
}

  const updatePayload = JSON.stringify({
    description: `Dev gerado pelo k6 VU ${__VU}`,
    linkedinProfile: `https://linkedin.com/in/user-${__VU}`,
    githubProfile: `https://github.com/user-${__VU}`,
    portfolioUrl: `https://portfolio-${__VU}.dev`,
    experienceYears: Math.floor(Math.random() * 10) + 1,
    skills: getRandomSkills()
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