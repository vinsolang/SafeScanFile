# SafeScan

Telegram bot that scans uploaded files for malware with ClamAV.
Spring Boot 3 · Java 21 · MySQL · Redis · ClamAV · Docker.

**Status:** Version 1 (scanner MVP) and Version 2 (users, credits, rate
limiting) are implemented. Payments have their core logic but no gateway
yet (Version 3); the admin dashboard is Version 4.

## How a scan works

```
User sends file ─► SafeScanBot (telegram/)
                     └► ScanWorkflowService (service/)
                          1. user blocked?            → reject
                          2. rate limit (Redis)       → reject
                          3. declared size ≤ 20 MB    → reject
                          4. spend 1 credit (row-locked transaction)
                          5. download to /tmp/safescan/scan-xxxx.tmp
                          6. SHA-256
                          7. stream to ClamAV (INSTREAM over TCP)
                          8. save result to `scans`
                          9. ALWAYS delete temp file
                         10. no verdict (ClamAV down)? → refund the credit
```

Files are never executed or opened, only hashed and streamed to ClamAV.
The extension and declared MIME type are recorded but never trusted.

## Project layout

```
src/main/java/com/safescan/
├── SafeScanApplication.java
├── config/       SafeScanProperties (typed safescan.* config), SecurityConfig
├── entity/       User, Plan, Subscription, Scan, Payment (+ status enums)
├── repository/   Spring Data JPA repositories (incl. row-lock queries)
├── dto/          IncomingFile, ScanReport, IoSupplier
├── scanner/      FileScannerService, ClamAvScannerService, ClamdClient (INSTREAM)
├── service/      ScanWorkflowService, CreditService, UserService, PlanService,
│                 RateLimitService, FileValidationService, TempFileService, HashService
├── telegram/     SafeScanBot (commands + documents), BotMessages, TelegramBotConfig
├── payment/      PaymentProvider (interface), PaymentService (idempotent confirm)
├── controller/   HealthController  →  GET /api/health
└── exception/    domain exceptions + GlobalExceptionHandler
```

## Bot commands

`/start` `/help` `/scan` `/balance` `/plans` `/buy` `/history`, or just
send a file **as a document** (attach → File). `/buy` says payments aren't
available yet.

## Run it

1. Create a bot: talk to **@BotFather** → `/newbot` → copy the token.
2. `cp .env.example .env` and paste the token in. (`.env` is git-ignored.
   Never commit the token.)
3. Start everything:

   ```
   docker compose up -d --build
   ```

   ClamAV downloads virus definitions on first start (a few minutes):
   `docker compose logs -f clamav`. The app waits for it to be healthy.

4. Check it: `curl localhost:8080/api/health` →
   `{"status":"UP","scanner":"UP"}`. Then message your bot `/start`.

Without a token the app still starts (DB, scanner, health endpoint) and
logs a warning that the bot is disabled.

**Faster dev loop:** run only the dependencies in Docker and the app with
Maven:

```
docker compose up -d mysql redis clamav
export TELEGRAM_BOT_TOKEN=123456:ABC...
mvn spring-boot:run
```

### Test detection with EICAR

Use the standard, harmless EICAR antivirus test file instead of real
malware. Save this exact line (no trailing newline) as `eicar.txt` and send
it to the bot:

```
X5O!P%@AP[4\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*
```

Expected: 🔴 `Eicar-Test-Signature`. Your desktop antivirus may delete the file
when you save it; that's it working. Any normal file should come back 🟢.

## Tests

```
mvn test
```

Covers the ClamAV protocol against an in-process fake `clamd` (framing,
threat/clean/error replies, early rejection, unreachable server), SHA-256,
file-name sanitising, and temp-file handling (size cap, stale purge).

## Design notes

- **Credits are concurrency-safe.** `CreditService.consume` re-reads each
  candidate subscription with `SELECT … FOR UPDATE` before decrementing, and
  `Subscription` has an `@Version` column as a second guard. Credits from the
  soonest-expiring subscription are spent first.
- **No DB transaction spans a download or scan.** Spend, refund and result
  insert are each short transactions.
- **Refund on failure.** If ClamAV is down or the download fails, the credit
  is given back and the user is told it wasn't used.
- **Real size cap.** Telegram's declared size isn't trusted; the copy aborts
  past 20 MB (also Telegram's Bot API download limit).
- **Plain-text replies.** File and threat names are untrusted; no Markdown or
  HTML parse mode is used, so they can't inject formatting or links.
  Invisible characters like the RTL override (U+202E) are stripped from names.
- **Rate limiting fails open** if Redis is unreachable (credits still cap use).
- **Private chats only.** The bot ignores groups; credits belong to a person.

## Known limits / next steps

- **Archives:** zip bombs and deep nesting are limited by ClamAV's own
  settings (`MaxScanSize`, `MaxFiles`, `MaxRecursion` in `clamd.conf`), not by
  this app. Review them before production (Step 17).
- **Payments (Version 3):** implement a `PaymentProvider` for your gateway,
  add a signature-verified webhook controller that calls
  `PaymentService.confirm(txId)`, and build the `/buy` flow. `confirm` is
  already idempotent and row-locked, so duplicate webhooks can't double-grant.
- **Migrations:** `ddl-auto: update` is for development. Move to Flyway
  before real data.
- **Security:** `SecurityConfig` permits everything for now. Lock it down
  before adding the admin API or the payment webhook.
- **Scale (Version 5):** scans run in-process behind a semaphore (8 at a
  time). For heavier traffic move them to a Redis/RabbitMQ queue with
  scanner workers.
