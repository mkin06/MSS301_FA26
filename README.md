# MSS301_FA26

Bai tap mon **MSS301 - Microservices** (FPT University, Fall 2026).

## Cau truc

| Thu muc | Noi dung |
|---|---|
| `Slot 3/ShoppingOnline` | Product Service - Spring Boot 3.2.4 + MongoDB |

## Slot 3 - Product Service

REST API danh muc san pham, kien truc 3 lop (Controller - Service - Repository).

| Chuc nang | Method | Endpoint |
|---|---|---|
| Tao san pham | `POST` | `/api/product` |
| Xem tat ca san pham | `GET` | `/api/product` |

### Cong nghe

- Java 21, Spring Boot 3.2.4
- Spring Web, Spring Data MongoDB
- Lombok, Testcontainers
- MongoDB 7.0.5 chay bang Docker Compose

### Chay thu

```bash
cd "Slot 3/ShoppingOnline"

# Khoi dong MongoDB + mongo-express
docker compose up -d

# Chay ung dung
mvn spring-boot:run
```

- Ung dung: http://localhost:8080
- mongo-express (xem du lieu): http://localhost:8081
