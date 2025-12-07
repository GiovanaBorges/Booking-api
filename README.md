📅 Booking Platform — Distributed Scheduling System

Sistema de agendamento distribuído estilo Calendly, utilizando:

Spring Boot (Java 17+)

MySQL como fonte de verdade

Redis (Redlock) para evitar double-booking

RabbitMQ para eventos assíncronos

Docker para rodar toda a stack rapidamente

Este projeto demonstra uma arquitetura realista de produção, com bloqueio distribuído, idempotência, filas e consistência ACID.

🚀 Recursos Implementados
✔️ Agendamentos com prevenção de conflito
✔️ Locks distribuídos com Redis + Redlock
✔️ API REST completa
✔️ Publicação de eventos RabbitMQ
✔️ Persistência ACID no MySQL
✔️ Idempotência por header
✔️ Worker assíncrono de notificações
🏗️ Arquitetura
Cliente → Booking API → (Redis | MySQL | RabbitMQ)
                          ↓
                    Notification Worker

📦 Requisitos

Para rodar rapidamente, basta apenas:

Docker

Docker Compose (opcional, se quiser subir toda a stack automaticamente)

Nenhum código-fonte local é necessário.

▶️ Como Rodar a API (sem baixar o projeto)

Assim que a imagem estiver publicada no Docker Hub, você poderá rodar:

docker pull <seu-usuario>/<booking-image>
docker run -p 8080:8080 <seu-usuario>/<booking-image>


🔸 Troque <seu-usuario>/<booking-image> pela sua imagem real quando você publicar.
🔸 Esse comando sobe apenas o serviço. Para o sistema completo com Redis, MySQL e RabbitMQ, veja abaixo.

▶️ Como Rodar a Stack Completa (usando Docker Compose)

Se você quiser rodar toda a infraestrutura, basta:

docker compose up


Ou, caso queira usar a imagem publicada sem clonar o repositório:

curl -O https://raw.githubusercontent.com/<seu-usuario>/<repo>/main/docker-compose.yml
docker compose up


Novamente, substitua <seu-usuario>/<repo> quando você publicar.

🔧 Serviços
Serviço	Porta	URL
Booking API	8080	http://localhost:8080

MySQL	3306	mysql://localhost:3306
Redis	6379	redis://localhost:6379
RabbitMQ UI	15672	http://localhost:15672

Credenciais padrão do RabbitMQ:

user: guest
pass: guest

📚 Endpoints Principais
Criar Agendamento
POST /api/bookings
Idempotency-Key: <uuid>

{
  "providerId": 1,
  "customerId": 55,
  "start": "2025-12-04T10:00:00Z",
  "end": "2025-12-04T10:30:00Z"
}

Possíveis Respostas

201 CREATED → reserva criada

409 CONFLICT → horário já reservado

423 LOCKED → lock não adquirido

409 IDEMPOTENCY REPLAY → requisição repetida

🔒 Lock Distribuído

Cada tentativa de reserva:

tenta adquirir um Redlock no Redis

verifica overlap no MySQL

cria a reserva

publica evento booking.created

Se o lock não for adquirido → 423 LOCKED.

🗄️ Modelagem de Dados (mínima)
users

Representa clientes e prestadores.

id, name, email, role

bookings

Fonte de verdade das reservas.

id, provider_id, customer_id, start_ts, end_ts, status

provider_availability (opcional, recomendado)
provider_id, day_of_week, start_time, end_time


Nenhuma dependência de MongoDB, pagamentos ou relatórios — modelo mínimo e funcional.

🧵 Evento RabbitMQ
exchange: booking
routingKey: booking.created
payload: {
  "bookingId": 1,
  "provider": 10,
  "start": "2025-12-04T10:00:00Z"
}

🧪 Testes Incluídos

validação de horários

verificação de overlap

testes de idempotência

carga simultânea com múltiplos usuários

📈 Observabilidade (opcional)

Sugestões:

OpenTelemetry

Prometheus

Grafana

Métricas úteis:

booking_locks_acquired_total

booking_conflicts_total

booking_latency_seconds

🗺️ Sugestões Futuras (Arquitetura Avançada)

Para quem quiser evoluir o sistema, ideias opcionais:

Sincronização com Google Calendar

Multi-tenant

Persistência de notificações (ex.: MongoDB)

Serviço independente para relatórios

Suporte a pagamentos

Lembretes via WhatsApp/SMS

API Gateway / microsserviços

Esses itens não fazem parte do modelo mínimo, mas são caminhos de evolução.