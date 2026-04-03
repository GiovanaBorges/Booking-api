import fetch from "node-fetch";
import fs from "fs";

import { BASE_URL,USERNAME,PASSWORD,CLIENT_ID, KEYCLOAK_BASE_URL } from "./config.js";

const TOTAL_USERS = 20;
const REALM = "Bookings";

// ==========================
// 📂 LOAD USERS
// ==========================
const users = JSON.parse(
  fs.readFileSync(new URL("./users.json", import.meta.url))
);

// ==========================
// 🔐 TOKEN ADMIN
// ==========================
async function getAdminToken() {
  const res = await fetch(
    `${KEYCLOAK_BASE_URL}/realms/master/protocol/openid-connect/token`,
    {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        username: "admin",
        password: "admin123",
        grant_type: "password",
        client_id: "admin-cli"
      })
    }
  );

  const data = await res.json();
  return data.access_token;
}

// ==========================
// 🔎 PEGAR GRUPOS
// ==========================
async function getGroups(token) {
  const res = await fetch(
    `${KEYCLOAK_BASE_URL}/admin/realms/${REALM}/groups`,
    {
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );

  const groups = await res.json();

  return {
    usersGroupId: groups.find(g => g.name === "clients")?.id,
    providersGroupId: groups.find(g => g.name === "providers")?.id
  };
}

// ==========================
// 👤 CRIAR USUÁRIO
// ==========================
async function createUser(token, user) {
  const res = await fetch(
    `${KEYCLOAK_BASE_URL}/admin/realms/${REALM}/users`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        username: user.username,
        email: user.email,
        enabled: true,
        credentials: [
          {
            type: "password",
            value: user.password,
            temporary: false
          }
        ]
      })
    }
  );

   // 👀 usuário já existe
  if (res.status === 409) {
    console.log(`⚠️ ${user.username} já existe`);
    return null;
  }

  if (res.status !== 201) {
    const error = await res.text();
    console.log("❌ ERRO:", error);
    return null;
  }

  const location = res.headers.get("location");
  const userId = location.split("/").pop();

  console.log(`✅ Criado: ${user.username}`);

  return userId;
}

// ==========================
// ➕ ADD GRUPO
// ==========================
async function addToGroup(token, userId, groupId) {
  if (!userId || !groupId) return;

  await fetch(
    `${KEYCLOAK_BASE_URL}/admin/realms/${REALM}/users/${userId}/groups/${groupId}`,
    {
      method: "PUT",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

// ==========================
// 🚀 MAIN
// ==========================
async function main() {
  const token = await getAdminToken();
  const { usersGroupId, providersGroupId } = await getGroups(token);

  for (const user of users) {
    const userId = await createUser(token, user);

    if (!userId) continue;

    if (user.group === "providers") {
      await addToGroup(token, userId, providersGroupId);
    } else {
      await addToGroup(token, userId, usersGroupId);
    }

    console.log(`🎯 ${user.username} → ${user.group}`);
  }
}

main();