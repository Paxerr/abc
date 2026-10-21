# DANASEA

Danasea is an online booking and experience management platform tailored for coastal and marine tourism in Da Nang, Vietnam. The platform bridges travelers and local marine tour operators through real-time reservation scheduling, secure automated payments, ticket verification, and weather-aware policy handling.

[![Build Status](https://github.com/capkimkhanh2k5/PBL6_HTTT/actions/workflows/ci.yml/badge.svg)](https://github.com/capkimkhanh2k5/PBL6_HTTT/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## Overview

Coastal adventure tourism requires strict capacity management, weather contingency handling, and reliable verification. DANASEA addresses these domain challenges through a modular architecture comprising:

- **Experience Catalog & Booking:** Real-time slot reservation, inventory locking, and multi-vendor checkout.
- **Payment Processing:** Dual integration with VNPay (VietQR / Domestic ATM) and PayPal, backed by idempotency keys to prevent duplicate transactions.
- **Cryptographic Verification:** HMAC-SHA256 signed QR tickets with constant-time verification to prevent ticket forgery and duplicate redemption.
- **Weather Contingency & Dynamic Refunds:** Automated slot cancellation and customer refund workflows driven by marine weather monitoring (Open-Meteo API).
- **Vendor Settlement & Disputes:** Transparent commission accounting, automated refund deduction, and structured arbitration workflows.
- **AI Concierge:** Natural language itinerary recommendations and policy clarification powered by large language models.

---

## Live Demo

A production preview instance is deployed via an automated CI/CD pipeline:

- **Application URL:** [https://danasea.tailfc3563.ts.net](https://danasea.tailfc3563.ts.net)

### Demo Accounts

The database is pre-seeded with test accounts for each system role:

| Role | Username | Password | Purpose |
|---|---|---|---|
| Admin | `admin@danasea.vn` | `Password123!` | Platform management, dispute arbitration, settlements |
| Vendor | `vendor@danasea.vn` | `Password123!` | Tour catalog, booking validation, payout tracking |
| Customer | `customer@danasea.vn` | `Password123!` | Experience discovery, checkout, ticket access |

---

## System Architecture

The application is structured as a **Modular Clean Monolith** following Hexagonal Architecture principles (Ports and Adapters) to ensure maintainable domain boundaries:

```
[ Web & Mobile Clients ]
           │ (HTTPS / REST)
           ▼
[ Nginx Reverse Proxy / Static Host ]
           │ (Proxy Pass / Forwarded Headers)
           ▼
[ Spring Boot Core Application ]
  ├── Security Layer (JWT, Cookie Origin Validation, RBAC)
  ├── Domain Modules (Auth, Booking, Order, Checkin, Dispute, Settlement, Weather, AI)
  └── Infrastructure Adapters (Flyway, JPA Repositories, External APIs)
           │
     ┌─────┴──────────────┐
     ▼                    ▼
[ PostgreSQL 17 ]    [ Redis 7 & RabbitMQ 3.12 ]
  (Primary Store)     (Distributed Locks & Event Queue)
```

---

## Technology Stack

### Backend
- **Framework:** Spring Boot 3.4 (Java 21 LTS)
- **Security:** Spring Security 6 with stateless JWT authentication and strict cookie origin validation
- **Persistence:** Spring Data JPA / Hibernate, PostgreSQL 17, Flyway
- **Caching & Messaging:** Redis 7 (distributed locks, rate limiting), RabbitMQ 3.12 (asynchronous task execution)
- **Testing:** JUnit 5, Mockito, Testcontainers (PostgreSQL, Redis, RabbitMQ)

### Frontend & Clients
- **Web:** React 18, TypeScript, Vite, Tailwind CSS
- **Mobile:** React Native / Expo (Vendor and Customer applications)

### Infrastructure & Operations
- **Containerization:** Docker, Docker Compose
- **Continuous Integration / Continuous Deployment:** GitHub Actions, GitHub Container Registry (GHCR)
- **Networking & Access:** Tailscale Funnel

---

## Getting Started

### Prerequisites
- Docker and Docker Compose
- Java 21 JDK (optional, for native backend builds)
- Node.js 20+ (optional, for native frontend builds)

### Local Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/capkimkhanh2k5/PBL6_HTTT.git
   cd PBL6_HTTT
   ```

2. **Configure environment variables:**
   ```bash
   cp .env.example .env
   ```
   Review and populate the required configuration values in `.env`.

3. **Start services with Docker Compose:**
   ```bash
   docker compose up -d
   ```

   Once healthy, the application endpoints will be available at:
   - Frontend: `http://localhost:3000`
   - Backend API: `http://localhost:8080/api`
   - RabbitMQ Management: `http://localhost:15672` (default: `guest` / `guest`)

---

## Automated Verification

The test suite includes over 2,500 automated tests covering unit domain logic, concurrent seat allocation, and database schema migrations:

```bash
cd backend
./mvnw clean verify
```

---

## Authors

- **Cap Kim Khanh** ([@capkimkhanh2k5](https://github.com/capkimkhanh2k5)) — Lead Developer
- Project PBL6 — Department of Information Technology, Da Nang University of Science and Technology (DUT)

---

## License

This project is licensed under the [MIT License](LICENSE).
