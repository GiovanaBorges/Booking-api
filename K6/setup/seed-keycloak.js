import fetch from "node-fetch";
import fs from "fs";

import { BASE_URL, KEYCLOAK_BASE_URL } from "../config/base.js";

const TOTAL_USERS = 20;
const REALM = "Bookings";

// ==========================
// LOAD USERS
// ==========================
const users = JSON.parse(
  fs.readFileSync(new URL("../data/users.json", import.meta.url))
);


// pega token admin
async function getAdminToken() {
  const params = new URLSearchParams({
    client_id: "admin-cli",
    username: "admin",
    password: "admin123",
    grant_type: "password"
  });

  const res = await fetch(
    `${KEYCLOAK_BASE_URL}/realms/master/protocol/openid-connect/token`,
    {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: params
    }
  );

  const data = await res.json();
  return data.access_token;
}

// cria usuário
async function createUser(token, user) {
  const res = await fetch(
    `${KEYCLOAK_BASE_URL}/admin/realms/${REALM}/users`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        username: user.email,
        email: user.email,
        enabled: true
      })
    }
  );

  if (res.status !== 201) {
    console.log(`❌ erro ao criar user:`, await res.text());
    return null;
  }

  const location = res.headers.get("location");
  return location.split("/").pop();
}

// seta senha
async function setPassword(token, userId, password) {
  const res = await fetch(
    `${KEYCLOAK_BASE_URL}/admin/realms/${REALM}/users/${userId}/reset-password`,
    {
      method: "PUT",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        type: "password",
        value: password,
        temporary: false
      })
    }
  );

  if (res.status !== 204) {
    console.log(`❌ erro ao setar senha:`, await res.text());
  }
}

// limpa required actions
async function cleanUser(token, userId) {
  const resGet = await fetch(
    `${KEYCLOAK_BASE_URL}/admin/realms/${REALM}/users/${userId}`,
    {
      headers: { Authorization: `Bearer ${token}` }
    }
  );

  const user = await resGet.json();

  user.emailVerified = true;
  user.requiredActions = [];

  const resPut = await fetch(
    `${KEYCLOAK_BASE_URL}/admin/realms/${REALM}/users/${userId}`,
    {
      method: "PUT",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify(user)
    }
  );

  if (resPut.status !== 204) {
    console.log("❌ erro ao limpar user:", await resPut.text());
  }
}

// pega ID do grupo
async function getGroupId(token, groupName) {
  const res = await fetch(
    `${KEYCLOAK_BASE_URL}/admin/realms/${REALM}/groups`,
    {
      headers: { Authorization: `Bearer ${token}` }
    }
  );

  const groups = await res.json();
  const group = groups.find((g) => g.name === groupName);

  return group ? group.id : null;
}

// adiciona user ao grupo
async function addUserToGroup(token, userId, groupId) {
  const res = await fetch(
    `${KEYCLOAK_BASE_URL}/admin/realms/${REALM}/users/${userId}/groups/${groupId}`,
    {
      method: "PUT",
      headers: { Authorization: `Bearer ${token}` }
    }
  );

  if (res.status !== 204) {
    console.log(`❌ erro ao adicionar ao grupo:`, await res.text());
  }
}

// teste de login
async function testLogin(user) {
  const params = new URLSearchParams({
    client_id: "MY-APP-CLI",
    username: user.email,
    password: user.password,
    grant_type: "password"
  });

  const res = await fetch(
    `${KEYCLOAK_BASE_URL}/realms/${REALM}/protocol/openid-connect/token`,
    {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: params
    }
  );

  if (res.status === 200) {
    console.log(`✅ login OK: ${user.email}`);
  } else {
    console.log(`❌ login falhou: ${user.email}`);
    console.log(await res.text());
  }
}

// pega role do realm
async function getRealmRole(token, roleName) {
  const res = await fetch(
    `${KEYCLOAK_BASE_URL}/admin/realms/${REALM}/roles/${roleName}`,
    {
      headers: { Authorization: `Bearer ${token}` }
    }
  );

  if (res.status !== 200) {
    console.log(`❌ erro ao buscar role ${roleName}:`, await res.text());
    return null;
  }

  return await res.json();
}

// adiciona role ao usuário
async function addRealmRoleToUser(token, userId, role) {
  const res = await fetch(
    `${KEYCLOAK_BASE_URL}/admin/realms/${REALM}/users/${userId}/role-mappings/realm`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify([role])
    }
  );

  if (res.status !== 204) {
    console.log(`❌ erro ao adicionar role:`, await res.text());
  }
}


// MAIN
async function main() {
    const token = await getAdminToken();

  for (const user of users) {
    console.log(`\n🚀 criando ${user.email}`);

    const userId = await createUser(token, user);
    if (!userId) continue;

    await setPassword(token, userId, user.password);
    await cleanUser(token, userId);

    // 👥 GROUP
    const groupId = await getGroupId(token, user.group);
    if (groupId) {
      await addUserToGroup(token, userId, groupId);
    }

    // ROLE DIRETA
    const role = await getRealmRole(token, user.role); // <-- precisa ter "role" no JSON
    if (role) {
      await addRealmRoleToUser(token, userId, role);
    }

    await testLogin(user);
  }
}

main();