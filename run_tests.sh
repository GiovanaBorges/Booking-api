#!/bin/bash
set -e 
# =========================
# Obtém o token dinâmico do Keycloak.
# Cria 3 bookings e 3 providers em loop.
# Atualiza cada um logo em seguida.
# Deleta todos os bookings e providers criados.
# Mostra todos os outputs em JSON formatado com jq.
# ==========================


# =========================
# CONFIGURAÇÃO DO KEYCLOAK
# =========================

#!/bin/bash
BASE_URL="http://localhost:8081"
KEYCLOAK_URL="http://localhost:8080/realms/Bookings/protocol/openid-connect/token"

CLIENT_ID="MY-APP-CLI"
USERNAME="email@test.com"
PASSWORD="12345"

echo "🔑 Obtendo token..."
TOKEN=$(curl -s -X POST "$KEYCLOAK_URL" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=$CLIENT_ID" \
  -d "username=$USERNAME" \
  -d "password=$PASSWORD" \
  -d "grant_type=password" | jq -r '.access_token')

if [[ -z "$TOKEN" || "$TOKEN" == "null" ]]; then
  echo "❌ Token inválido"
  exit 1
fi

AUTH=(-H "Authorization: Bearer $TOKEN")

echo "=================="
echo "👤 Criando / obtendo usuário via /users/me"

USER_RESPONSE=$(curl -s -X GET "$BASE_URL/users/me" "${AUTH[@]}")
echo "$USER_RESPONSE" | jq

USER_ID=$(echo "$USER_RESPONSE" | jq -r '.id')

if [[ -z "$USER_ID" || "$USER_ID" == "null" ]]; then
  echo "❌ User ID inválido"
  exit 1
fi

echo "✅ User criado/obtido: ID $USER_ID"

echo "=================="
echo "📅 Criando Provider Availability"

PROVIDER_RESPONSE=$(curl -s -X POST "$BASE_URL/provider-availability/register" \
  "${AUTH[@]}" \
  -H "Content-Type: application/json" \
  -d "{
    \"day_of_week\": 1,
    \"startTime\": \"09:00\",
    \"end_time\": \"18:00\",
    \"providerId\": $USER_ID
  }")

echo "$PROVIDER_RESPONSE" | jq

PROVIDER_AVAILABILITY_ID=$(echo "$PROVIDER_RESPONSE" | jq -r '.id')

if [[ -z "$PROVIDER_AVAILABILITY_ID" || "$PROVIDER_AVAILABILITY_ID" == "null" ]]; then
  echo "❌ Provider availability não criada"
  exit 1
fi

echo "✅ Provider availability criada: ID $PROVIDER_AVAILABILITY_ID"

echo "=================="
echo "📌 Criando Booking"

BOOKING_RESPONSE=$(curl -s -X POST "$BASE_URL/bookings/register" \
  "${AUTH[@]}" \
  -H "Content-Type: application/json" \
  -d "{
    \"providerId\": $USER_ID,
    \"customerId\": $USER_ID,
    \"startsTs\": \"2026-01-10T14:00:00\",
    \"endTs\": \"2026-01-10T15:00:00\",
    \"status\": \"CONFIRMED\"
  }")

echo "$BOOKING_RESPONSE" | jq

BOOKING_ID=$(echo "$BOOKING_RESPONSE" | jq -r '.id')

if [[ -z "$BOOKING_ID" || "$BOOKING_ID" == "null" ]]; then
  echo "❌ Booking não criado"
  exit 1
fi

echo "✅ Booking criado: ID $BOOKING_ID"

echo "=================="
echo "🎉 Fluxo completo executado com sucesso"