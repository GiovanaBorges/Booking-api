import http from "k6/http";
import { check, sleep } from "k6";
import {  BASE_URL,CLIENT_ID, KEYCLOAK_BASE_URL } from "./config.js";
import {open} from "k6/fs"

const users = JSON.parse(open("./users.json"));

export const options = {
  stages: [
    { duration: "10s", target: 5 },
    { duration: "20s", target: 10 },
    { duration: "10s", target: 0 }
  ],
  thresholds: {
    http_req_duration: ["p(95)<500"],
    http_req_failed: ["rate<0.01"]
  }
};

// ==========================
// TOKEN DINÂMICO POR USER
// ==========================
function getToken(user) {
  const payload = {
    client_id: CLIENT_ID,
    username: user.username,
    password: user.password,
    grant_type: "password"
  };

  const res = http.post(
    `${KEYCLOAK_BASE_URL}/realms/Bookings/protocol/openid-connect/token`,
    payload
  );

  check(res, {
    "token obtido": (r) => r.status === 200
  });

  return res.json("access_token");
}

// ==========================
// TESTE PRINCIPAL
// ==========================
export default function () {

  // cada VU pega um usuário diferente
 if (!users || users.length === 0) {
  throw new Error("users.json vazio ou não carregado");
}


const index = (__VU - 1) % users.length;
const user = users[index];

  const token = getToken(user);

  const params = {
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json"
    }
  };

  // -------------------------
  // GET /users/me (createOrGet)
  // -------------------------
  let res = http.get(`${BASE_URL}/users/me`, params);

  check(res, {
    "user obtido/criado": (r) => r.status === 200
  });

  const userId = res.json("id");

  // -------------------------
  // PROVIDER AVAILABILITY
  // -------------------------
  const providerPayload = JSON.stringify({
    day_of_week: Math.floor(Math.random() * 7),
    startTime: "09:00:00",
    end_time: "17:00:00",
    providerId: userId
  });

  res = http.post(
    `${BASE_URL}/provideravailability/register`,
    providerPayload,
    params
  );

  check(res, {
    "provider criado": (r) => r.status === 200
  });

  // -------------------------
  // BUSCAR PROVIDERS
  // -------------------------
  res = http.get(`${BASE_URL}/provideravailability/allproviders`, params);

  check(res, {
    "providers fetched": (r) => r.status === 200
  });

  // -------------------------
  // CREATE BOOKING
  // -------------------------
  const bookingPayload = JSON.stringify({
    providerId: userId,
    customerId: userId,
    startsTs: "2026-01-10T14:00:00",
    endTs: "2026-01-10T15:00:00",
    status: "CONFIRMED"
  });

  res = http.post(`${BASE_URL}/bookings/register`, bookingPayload, params);

  check(res, {
    "booking criado": (r) => r.status === 200
  });

  sleep(1);
}