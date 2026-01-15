#!/usr/bin/env bash
set -e

ENVIRONMENT="$1"

if [[ "$ENVIRONMENT" != "dev" && "$ENVIRONMENT" != "prod" ]]; then
  echo "Uso: ./create_secrets_auto.sh dev|prod"
  exit 1
fi

ENV_UPPER=$(echo "$ENVIRONMENT" | tr '[:lower:]' '[:upper:]')

if [[ ! -f ".env.secrets" ]]; then
  echo "❌ Arquivo .env.secrets não encontrado"
  exit 1
fi

REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner)

echo "🔐 Criando secrets para environment: $ENVIRONMENT"
echo "📦 Repositório: $REPO"
echo "------------------------------------"

while IFS= read -r line; do
  [[ -z "$line" || "$line" =~ ^# ]] && continue

  key="${line%%=*}"
  value="${line#*=}"

  if [[ "$key" == ${ENV_UPPER}_* ]]; then
    SECRET_NAME="${key#${ENV_UPPER}_}"

    gh secret set "$SECRET_NAME" \
      --body "$value" \
      --repo "$REPO" \
      --env "$ENVIRONMENT"

    echo "✅ $SECRET_NAME"
  fi
done < .env.secrets

echo "------------------------------------"
echo "🎉 Secrets criadas para $ENVIRONMENT"
