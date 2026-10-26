# k6 performance test

`shortener.js` first checks the app's health endpoint, then exercises the URL
lifecycle: create a short URL, read its stats, follow its redirect without
contacting the destination, and verify the click count increased.

Start the backend and its MongoDB and Redis dependencies, then run the test
from the repository root with [k6](https://k6.io/) installed:

```powershell
docker compose up --build -d backend
k6 run .\k6\shortener.js
```

The default target is `http://localhost:8085` and the default workload ramps
up to 1,000 virtual users over 30 seconds, runs for 2 minutes, then ramps down
over 30 seconds. Set `BASE_URL` to test another instance, and tune the workload
with `VUS`, `RAMP_UP`, `DURATION`, and `RAMP_DOWN`. For example:

```powershell
$env:BASE_URL = 'http://localhost:8085'
$env:VUS = '10'
$env:RAMP_UP = '10s'
$env:DURATION = '5m'
$env:RAMP_DOWN = '10s'
k6 run .\k6\shortener.js
```

Each successful iteration sends four API requests. The app's default rate
limit is 100 requests per minute per client IP, so the default k6 workload will
be rate-limited. To measure application capacity instead, use a dedicated test
instance and raise `RATE_LIMIT_CAPACITY` and `RATE_LIMIT_REFILL_TOKENS` enough
for the expected request rate. The test intentionally does not spoof client IPs
or follow redirects to the external destination.

The test fails if checks pass at a rate of 99% or lower, HTTP requests fail at
a rate of 1% or higher, or p95 create, stats, or redirect latency reaches one
second.
