# david-api — TRAMED Nyarugenge Data Warehouse API

A Spring Boot REST API that collects pharmacy sales data from multiple branches, stores it in a PostgreSQL staging layer, and promotes it nightly into a Star Schema data warehouse for analytics and forecasting.

Built for **TRAMED Nyarugenge**.

---

## What This System Does

Pharmacy computers run **SparkBind** — a Java ETL agent that reads the local Ishyiga POS database (Apache Derby) and sends sales data to this API every hour via HTTP POST.

This API:
1. Receives the data and stores it in **staging tables** (raw, unmodified)
2. Every night at 2 AM, promotes the data into a **Star Schema warehouse** (clean, analytical)
3. Exposes the warehouse via REST endpoints for dashboards and reports

---

## Architecture

```
SparkBind (pharmacy computer)
    ↓ HTTP POST every hour
IngestionController → IngestionService → Staging Tables (PostgreSQL)
    ↓ @Scheduled every night at 2 AM
WarehouseSyncService → Warehouse Tables (Star Schema)
    ↓ HTTP GET/POST
WarehouseController → Dashboard / Reports
```

---

## Project Structure

```
src/main/java/com/example/david_api/
│
├── ingestion/
│   ├── controller/     IngestionController.java       — door IN (receives SparkBind data)
│   ├── service/        IngestionService.java          — validates + deduplicates + saves
│   ├── dto/            ProductDTO, ClientDTO, SaleDTO, StockDTO
│   ├── entity/         StagingProduct, StagingClient, StagingSale, StagingSaleLine, StagingStock
│   └── repository/     5 JPA repositories with dedup query methods
│
├── warehouse/
│   ├── controller/     WarehouseController.java       — door OUT (exposes warehouse)
│   ├── service/        WarehouseSyncService.java      — ETL: staging → warehouse
│   ├── entity/         DimDate, DimProduct, DimClient, DimPharmacy, FactSale
│   └── repository/     5 JPA repositories with FK lookup methods
│
└── DavidApiApplication.java                           — main class + @EnableScheduling
```

---

## Staging Tables (OLTP — raw data from SparkBind)

| Table | Description |
|---|---|
| `staging_products` | Products from pharmacy POS |
| `staging_clients` | Clients / affiliations |
| `staging_sales` | Invoice headers |
| `staging_sale_lines` | Individual product lines per invoice |
| `staging_stock` | Current stock snapshots |

## Warehouse Tables (OLAP — Star Schema)

| Table | Description |
|---|---|
| `dim_date` | Date dimension — day, month, year, quarter, week |
| `dim_product` | Product dimension |
| `dim_client` | Client dimension |
| `dim_pharmacy` | Pharmacy branch dimension |
| `fact_sale` ⭐ | Central fact table — one row per sale line |

---

## API Endpoints

### Ingestion (protected by X-API-KEY header)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/ingest/products` | Receive products from SparkBind |
| POST | `/api/v1/ingest/clients` | Receive clients from SparkBind |
| POST | `/api/v1/ingest/sales` | Receive sales + sale lines from SparkBind |
| POST | `/api/v1/ingest/stock` | Receive stock snapshots from SparkBind |

### Warehouse (open)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/warehouse/sync` | Trigger warehouse sync manually |
| GET | `/api/v1/warehouse/status` | Row counts staging vs warehouse + timestamps |
| GET | `/api/v1/health` | Health check — keeps Render instance alive |

### Example status response

```json
{
  "staging_products": 45,
  "staging_clients": 758,
  "staging_sale_lines": 20425,
  "dim_product": 45,
  "dim_client": 758,
  "fact_sale": 20425,
  "stock_last_synced": "2026-07-16T14:02:28",
  "last_sale_line_received": "2026-07-16T18:30:00"
}
```

---

## How to Run Locally

### Prerequisites
- Java 17
- Maven
- PostgreSQL running locally

### 1. Clone the repo
```bash
git clone https://github.com/kadave462/david-api.git
cd david-api
```

### 2. Set up local PostgreSQL
Create a database called `tramed_dw` and update `application.properties` if needed:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/tramed_dw
spring.datasource.username=postgres
spring.datasource.password=postgres
```

### 3. Run
```bash
./mvnw spring-boot:run
```

App starts on `http://localhost:8080`

### 4. Test the status endpoint
```
GET http://localhost:8080/api/v1/warehouse/status
```

---

## How to Run in Production (Render)

The app is deployed on **Render** using Docker.

Live URL: `https://david-api-la1t.onrender.com`



To trigger a manual sync from PowerShell:
---

## Security

All ingestion endpoints require:
```
X-API-KEY: TRAMEDNYARUGENGE-2026
```

Warehouse endpoints are open (read-only status + manual sync trigger).

---

## Build Schedule

| Period | Focus | Status |
|---|---|---|
| June 25–30 | Setup — SparkBind fix + Spring Boot project | ✅ Done |
| July 1–7 | Ingestion — data flowing into PostgreSQL | ✅ Done — 9,527 invoices synced |
| July 8–14 | Warehouse — Star Schema + ETL service + controller | ✅ Done — deployed July 16 |
| July 15–21 | Forecasting + Reports — stock depletion, revenue reports | 🔄 In Progress |
| July 22–31 | Dashboard — React frontend on Render | ⏳ Upcoming |

---

## Where We Are Now (July 16, 2026)

The warehouse is **live in production**.

- ✅ 45 products, 758 clients, 20,425 sale lines ingested from TRAMED Nyarugenge (Jan–Jul 2026)
- ✅ Star Schema warehouse syncing nightly at 2 AM automatically
- ✅ `GET /warehouse/status` confirms data moving from staging to warehouse in real time
- 🔄 `fact_sale` currently filling up — sync in progress
- ⏳ Next: stock depletion forecasting + revenue reports (Week 3)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3 |
| ORM | JPA / Hibernate |
| Database | PostgreSQL |
| Deployment | Render (cloud) + Docker |
| Version control | Git / GitHub |
| Data model | Star Schema (OLAP) |
| Scheduling | Spring `@Scheduled` |
