# k6 performance test

`shortener.js` exercises the app's health endpoint and the full URL lifecycle:
create a short URL, read its stats, follow its redirect without contacting the
destination, and verify the click count increased.

Run it against a running app with k6 installed:

```powershell
k6 run .\k6\shortener.js
```

The default target is `http://localhost:8085`. Set `BASE_URL` to test another
instance, and tune the workload with `VUS`, `RAMP_UP`, `DURATION`, and
`RAMP_DOWN`. For example:

```powershell
$env:BASE_URL = 'http://localhost:8085'
$env:VUS = '10'
$env:DURATION = '5m'
k6 run .\k6\shortener.js
```

The app's default rate limit is 100 requests per minute per client IP, while
each test iteration sends four API requests. Run against a dedicated test
instance with a suitably raised `RATE_LIMIT_CAPACITY` and
`RATE_LIMIT_REFILL_TOKENS` (for example, `2000` each) to measure application
capacity rather than the configured rate limit. The test intentionally does
not spoof client IPs or follow redirects to the external destination.

The test fails if checks do not pass at least 99%, requests fail at least 1%,
or the p95 create, stats, or redirect latency reaches one second.
