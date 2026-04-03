import http from "k6/http";
import { check } from "k6";
import { KEYCLOAK_BASE_URL, CLIENT_ID } from "../config/base.js";

export function getToken(user) {
  const payload = {
    client_id: CLIENT_ID,
    username: user.username,
    password: user.password,
    grant_type: "password"
  };

  const params = {
    headers: {
      "Content-Type": "application/x-www-form-urlencoded"
    }
  };

  const res = http.post(
    `${KEYCLOAK_BASE_URL}/realms/Bookings/protocol/openid-connect/token`,
    payload,
    params
  );

  check(res, {
    "token ok": (r) => r.status === 200
  });

  if (res.status !== 200) {
    console.error("ERRO TOKEN:", res.body);
    return null;
  }

  return res.json("access_token");
}