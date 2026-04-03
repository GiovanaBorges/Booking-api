#!/bin/bash

echo "🚀 Iniciando testes k6..."

echo "👉 Rodando E2E..."
k6 run scenarios/e2e/users.flow.test.js

echo "👉 Rodando LOAD GETBYID..."
k6 run scenarios/load/users/users.getById.load.test.js

echo "👉 Rodando LOAD ME..."
k6 run scenarios/load/users/users.me.load.test.js

echo "👉 Rodando LOAD UPDATE..."
k6 run scenarios/load/users/users.update.load.test.js

echo "✅ Todos os testes finalizados!"