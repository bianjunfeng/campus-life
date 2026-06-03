# Campus Life Public Demo / Alipay Sandbox Deployment

This deployment keeps the original project code and only changes runtime
configuration. It is intended for a resume/demo site that can show the full
business flow, including Alipay sandbox payment callbacks.

## Target Result

- Student site: `https://demo.example.com`
- Merchant site: `https://merchant.demo.example.com`
- Admin site: `https://admin.demo.example.com`
- Public API entry: `https://demo.example.com/api/**`
- Alipay sandbox notify URL: `https://demo.example.com/api/payment/alipay/notify`
- Alipay sandbox return URL: `https://demo.example.com/api/payment/alipay/return`

The server runs:

- Nginx for frontend static files and reverse proxy.
- `campus-life-gateway.jar` on `127.0.0.1:8090`.
- `campus-life-backend.jar` on `127.0.0.1:8080`.
- `campus-life-ai.jar` on `127.0.0.1:8083`.
- MySQL, Redis, and RabbitMQ in Docker, bound to `127.0.0.1`.

Kafka, Elasticsearch, OSS, WeChat Pay, and Milvus are disabled in the default
demo template. Alipay sandbox and AI chat are enabled. Knowledge-base upload can
still work with MySQL chunk storage and keyword fallback; enable Milvus later if
you want to demonstrate vector search explicitly.

## 1. Prepare Domain And Alipay Sandbox

Before deployment, prepare:

- One Linux server with a public IP. Recommended minimum: 2 vCPU, 4 GB RAM.
- DNS records:
  - `demo.example.com`
  - `merchant.demo.example.com`
  - `admin.demo.example.com`
- Alipay sandbox app values:
  - sandbox `APP_ID`
  - application private key
  - Alipay sandbox public key

Use these URLs in the sandbox/payment settings and in `/opt/campus-life/.env`:

```text
https://demo.example.com/api/payment/alipay/notify
https://demo.example.com/api/payment/alipay/return
```

Keep `ALIPAY_SKIP_NOTIFY_SIGN_VERIFY=false` for the public demo. This proves the
callback path includes signature verification, not only a mock state change.

## 2. Build Original Project Artifacts Locally

Run from the repository root on the build machine:

```bash
cd campus-life-frontend
npm ci
npm run build

cd ../campus-life-backend
mvn -DskipTests package

cd ../campus-life-gateway
mvn -DskipTests package

cd ../campus-life-ai
mvn -DskipTests package
```

Expected artifacts with the current Vite config:

- `../../dist/consumer-web` from the frontend project directory
- `../../dist/merchant-web` from the frontend project directory
- `../../dist/admin-web` from the frontend project directory
- `campus-life-backend/target/campus-life-backend-0.0.1-SNAPSHOT.jar`
- `campus-life-gateway/target/campus-life-gateway-0.0.1-SNAPSHOT.jar`
- `campus-life-ai/target/campus-life-ai-0.0.1-SNAPSHOT.jar`

For example, if the frontend directory is `D:\code1\code\campus-life-frontend`,
the generated app folders are under `D:\code1\dist`.

## 3. Prepare The Linux Server

Run on the server:

```bash
sudo apt update
sudo apt install -y nginx docker.io docker-compose-plugin openjdk-17-jre mysql-client
sudo systemctl enable --now docker nginx
sudo useradd -r -s /usr/sbin/nologin campus || true
sudo mkdir -p /opt/campus-life/{frontend/consumer-web,frontend/merchant-web,frontend/admin-web,backend/config,gateway/config,ai/config,ai/migration,data/uploads,data/ai-knowledge,mysql/data,redis/data,rabbitmq/data}
sudo chown -R campus:campus /opt/campus-life
```

## 4. Upload Files

Upload these files/directories:

```text
generated `consumer-web` directory          -> /opt/campus-life/frontend/consumer-web
generated `merchant-web` directory          -> /opt/campus-life/frontend/merchant-web
generated `admin-web` directory             -> /opt/campus-life/frontend/admin-web
campus-life-backend target jar              -> /opt/campus-life/backend/campus-life-backend.jar
campus-life-gateway target jar              -> /opt/campus-life/gateway/campus-life-gateway.jar
campus-life-ai target jar                   -> /opt/campus-life/ai/campus-life-ai.jar
deploy/demo/docker-compose.yml              -> /opt/campus-life/docker-compose.yml
deploy/demo/.env.example                    -> /opt/campus-life/.env
deploy/demo/backend/application-demo.yml    -> /opt/campus-life/backend/config/application-demo.yml
deploy/demo/gateway/application.yml         -> /opt/campus-life/gateway/config/application.yml
deploy/demo/ai/application-demo.yml         -> /opt/campus-life/ai/config/application-demo.yml
deploy/demo/ai/create-ai-database.sql       -> /opt/campus-life/ai/create-ai-database.sql
campus-life-ai/src/main/resources/db/migration/*.sql -> /opt/campus-life/ai/migration/
deploy/demo/nginx/campus-life.conf          -> /etc/nginx/sites-available/campus-life.conf
deploy/demo/systemd/campus-life-backend.service -> /etc/systemd/system/campus-life-backend.service
deploy/demo/systemd/campus-life-ai.service -> /etc/systemd/system/campus-life-ai.service
deploy/demo/systemd/campus-life-gateway.service -> /etc/systemd/system/campus-life-gateway.service
```

Then edit `/opt/campus-life/.env`:

```bash
sudo nano /opt/campus-life/.env
sudo chmod 600 /opt/campus-life/.env
```

Required changes:

- Replace all `CHANGE_ME_*` values.
- Set `DEMO_PUBLIC_BASE_URL=https://demo.example.com`.
- Set the Alipay sandbox keys as single-line values.
- Set `AI_OPENAI_COMPATIBLE_API_KEY` and AI database password.
- Keep `JWT_SECRET` identical for backend and gateway. The systemd files load
  the same `/opt/campus-life/.env`, so this is automatic for backend, gateway,
  and AI if the file is correct.

## 5. Start Infrastructure Containers

Run on the server:

```bash
cd /opt/campus-life
sudo docker compose --env-file .env up -d
sudo docker compose ps
```

Ports should only be bound on `127.0.0.1`, not exposed publicly.

## 6. Initialize Demo Database

Preferred for a resume demo:

1. Create a local demo database with clean users, shops, posts, vouchers, and a
   few realistic orders.
2. Export it and import it into the server database.

Example:

```bash
mysqldump -h 127.0.0.1 -u root -p campus_life_demo > campus_life_demo.sql
mysql -h 127.0.0.1 -u root -p campus_life_demo < campus_life_demo.sql
```

For a clean database, import the base schema and migrations in this order:

```bash
mysql -h 127.0.0.1 -u root -p campus_life_demo < /path/to/campus-life-backend/docs/db-schema.sql
mysql -h 127.0.0.1 -u root -p campus_life_demo < /path/to/campus-life-backend/src/main/resources/sql/migrate-shopping-cart-v1.sql
mysql -h 127.0.0.1 -u root -p campus_life_demo < /path/to/campus-life-backend/src/main/resources/sql/migrate-payment-domain-v1.sql
mysql -h 127.0.0.1 -u root -p campus_life_demo < /path/to/campus-life-backend/src/main/resources/sql/migrate-payment-reconciliation-v1.sql
mysql -h 127.0.0.1 -u root -p campus_life_demo < /path/to/campus-life-backend/src/main/resources/sql/migrate-payment-refund-review-v1.sql
mysql -h 127.0.0.1 -u root -p campus_life_demo < /path/to/campus-life-backend/src/main/resources/sql/migrate-payment-wallet-backfill-v1.sql
mysql -h 127.0.0.1 -u root -p campus_life_demo < /path/to/campus-life-backend/src/main/resources/sql/migrate-system-notification.sql
mysql -h 127.0.0.1 -u root -p campus_life_demo < /path/to/campus-life-backend/src/main/resources/sql/migrate-merchant-location-v1.sql
mysql -h 127.0.0.1 -u root -p campus_life_demo < /path/to/campus-life-backend/src/main/resources/sql/migrate-voucher-wallet-v2.sql
mysql -h 127.0.0.1 -u root -p campus_life_demo < /path/to/campus-life-backend/src/main/resources/sql/migrate-voucher-order-v3.sql
mysql -h 127.0.0.1 -u root -p campus_life_demo < /path/to/campus-life-backend/src/main/resources/sql/migrate-voucher-order-idempotency-v4.sql
mysql -h 127.0.0.1 -u root -p campus_life_demo < /path/to/campus-life-backend/src/main/resources/sql/migrate-voucher-order-use-deadline-v5.sql
mysql -h 127.0.0.1 -u root -p campus_life_demo < /path/to/campus-life-backend/src/main/resources/sql/migrate-voucher-order-order-no-unique.sql
mysql -h 127.0.0.1 -u root -p campus_life_demo < /path/to/campus-life-backend/src/main/resources/sql/migrate-event-reliability-v1.sql
```

Prepare fixed demo accounts:

- student account
- merchant account
- admin account

Do not expose real personal accounts or local development passwords.

## 7. Initialize AI Database

The AI service uses an independent database. Run this once before starting
`campus-life-ai`.

1. Edit `/opt/campus-life/ai/create-ai-database.sql` and replace
   `CHANGE_ME_AI_MYSQL_PASSWORD` with the same value as `AI_MYSQL_PASSWORD` in
   `/opt/campus-life/.env`.
2. Create the database and account:

```bash
cd /opt/campus-life
set -a
. /opt/campus-life/.env
set +a
sudo docker exec -i campus-demo-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" < /opt/campus-life/ai/create-ai-database.sql
```

3. Import the AI schema migrations once, in order:

```bash
set -a
. /opt/campus-life/.env
set +a
sudo docker exec -i campus-demo-mysql mysql -ucampus_life_ai_user -p"$AI_MYSQL_PASSWORD" campus_life_ai_demo < /opt/campus-life/ai/migration/V1__init_ai_schema.sql
sudo docker exec -i campus-demo-mysql mysql -ucampus_life_ai_user -p"$AI_MYSQL_PASSWORD" campus_life_ai_demo < /opt/campus-life/ai/migration/V2__conversation_runtime_route.sql
sudo docker exec -i campus-demo-mysql mysql -ucampus_life_ai_user -p"$AI_MYSQL_PASSWORD" campus_life_ai_demo < /opt/campus-life/ai/migration/V3__knowledge_base.sql
sudo docker exec -i campus-demo-mysql mysql -ucampus_life_ai_user -p"$AI_MYSQL_PASSWORD" campus_life_ai_demo < /opt/campus-life/ai/migration/V4__enable_role_agent_scenes.sql
```

Do not re-run `V2__conversation_runtime_route.sql` after it succeeds once; it
contains plain `ALTER TABLE` statements.

## 8. Start Backend, AI, And Gateway

Run on the server:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now campus-life-backend
sudo systemctl enable --now campus-life-ai
sudo systemctl enable --now campus-life-gateway
sudo journalctl -u campus-life-backend -n 100 --no-pager
sudo journalctl -u campus-life-ai -n 100 --no-pager
sudo journalctl -u campus-life-gateway -n 100 --no-pager
```

The backend systemd unit starts with:

```text
--spring.profiles.active=demo
```

This is intentional. Do not use `prod` for Alipay sandbox, because the project
contains production payment validation that rejects sandbox gateways in `prod`.

## 9. Enable Nginx And HTTPS

Edit `/etc/nginx/sites-available/campus-life.conf` and replace:

- `demo.example.com`
- `merchant.demo.example.com`
- `admin.demo.example.com`

Then run:

```bash
sudo ln -sf /etc/nginx/sites-available/campus-life.conf /etc/nginx/sites-enabled/campus-life.conf
sudo nginx -t
sudo systemctl reload nginx
```

After HTTP works, add HTTPS:

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d demo.example.com -d merchant.demo.example.com -d admin.demo.example.com
```

After Certbot finishes, update `/opt/campus-life/.env` if the final domain
differs, then restart:

```bash
sudo systemctl restart campus-life-backend campus-life-gateway
sudo systemctl reload nginx
```

## 10. Smoke Checks

Run on the server:

```bash
curl http://127.0.0.1:8080/actuator/health
curl http://127.0.0.1:8083/actuator/health
curl http://127.0.0.1:8090/actuator/health
curl -I http://127.0.0.1
curl -I https://demo.example.com
curl -I https://demo.example.com/api/voucher-service/health
```

Open in browser:

- `https://demo.example.com`
- `https://merchant.demo.example.com`
- `https://admin.demo.example.com`
- AI entry in the student, merchant, or admin UI

## 11. Payment Demo Script

Use this path during interviews:

1. Log in as a student.
2. Open welfare/coupon page.
3. Create a voucher order.
4. Choose Alipay.
5. Submit to Alipay sandbox page.
6. Pay with sandbox buyer account.
7. Return to `https://demo.example.com/payment/success`.
8. Show order status changed to paid.
9. Show admin payment reconciliation/refund pages if needed.
10. Show backend callback logs or database payment records if asked.

This demonstrates:

- payment order creation
- gateway auth and whitelist separation
- Alipay sandbox form payment
- asynchronous notify callback
- signature verification
- amount consistency check
- idempotent order state transition
- refund/reconciliation domain design

## 12. AI Demo Script

Use this path after normal login works:

1. Log in as student, merchant, or admin.
2. Open the AI assistant entry.
3. Send a general campus-life question.
4. Confirm the response is streamed or returned successfully.
5. Open admin AI config to show provider/model/scene management if needed.
6. Optional: create a personal knowledge base, upload a `.txt` or `.md` file,
   then ask a question using that knowledge base.

Default demo mode sets `AI_KNOWLEDGE_VECTOR_ENABLED=false`, so knowledge search
uses MySQL chunk storage and keyword fallback. To demonstrate real vector search,
add Milvus, set `AI_KNOWLEDGE_VECTOR_ENABLED=true`, and configure `MILVUS_URI`.

## 13. Common Failure Points

- Backend starts with `prod`: Alipay sandbox gateway is rejected. Use `demo`.
- Alipay returns but order stays pending: check `ALIPAY_NOTIFY_URL`, public HTTPS,
  Nginx `/api/` proxy, and backend callback logs.
- Signature verification fails: confirm `ALIPAY_PUBLIC_KEY` is the Alipay sandbox
  public key, not the application public key.
- Browser CORS error: `DEMO_PUBLIC_BASE_URL` does not match the actual HTTPS
  origin.
- Login succeeds through backend but fails through gateway: backend and gateway
  are not using the same `JWT_SECRET`.
- AI API returns 502: `campus-life-ai` is not running, or gateway `ai-uri` does
  not point to `http://127.0.0.1:8083`.
- AI health is UP but chat fails: `AI_OPENAI_COMPATIBLE_API_KEY`, base URL, or
  model name is wrong.
- AI login/auth fails: AI service is not using the same `JWT_SECRET` as backend
  and gateway.
- Knowledge upload indexes as failed: check `AI_KNOWLEDGE_VECTOR_ENABLED`. If it
  is true, Milvus and embedding config must be valid.
- Coupon grab fails: RabbitMQ is down or `.env` credentials do not match Docker.
- Upload URL is broken: `file.upload.public-base-url` must match the public
  frontend domain.
