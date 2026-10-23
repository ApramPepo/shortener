import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = (__ENV.BASE_URL || 'http://localhost:8085').replace(/\/+$/, '');
const vus = Number(__ENV.VUS || 5);
const maxLoggedBodyLength = 300;

function logRequestFailure(operation, response, reason) {
  const rateLimitHeaders = {};
  for (const [name, value] of Object.entries(response.headers || {})) {
    if (/^(retry-after|x-rate-limit-(limit|remaining)|ratelimit(-.*)?)$/i.test(name)) {
      rateLimitHeaders[name] = value;
    }
  }

  const body = String(response.body || '').replace(/\s+/g, ' ').trim();
  console.error(
    `k6 request failure ${JSON.stringify({
      operation,
      status: response.status,
      reason,
      vu: __VU,
      iteration: __ITER,
      rateLimitHeaders,
      body: body.length > maxLoggedBodyLength
        ? `${body.slice(0, maxLoggedBodyLength)}...`
        : body,
    })}`,
  );
}

export const options = {
  stages: [
    { duration: __ENV.RAMP_UP || '30s', target: vus },
    { duration: __ENV.DURATION || '2m', target: vus },
    { duration: __ENV.RAMP_DOWN || '30s', target: 0 },
  ],
  thresholds: {
    checks: ['rate>0.99'],
    http_req_failed: ['rate<0.01'],
    'http_req_duration{operation:create}': ['p(95)<1000'],
    'http_req_duration{operation:stats}': ['p(95)<1000'],
    'http_req_duration{operation:redirect}': ['p(95)<1000'],
  },
};

export function setup() {
  const response = http.get(`${baseUrl}/actuator/health`, {
    tags: { operation: 'health' },
    timeout: '10s',
  });
  const healthy = check(response, {
    'health endpoint returns 200': (r) => r.status === 200,
    'application health is UP': (r) => {
      try {
        return r.json('status') === 'UP';
      } catch (_) {
        return false;
      }
    },
  });

  if (!healthy) {
    throw new Error(`Application is not healthy at ${baseUrl}/actuator/health`);
  }
}

export default function () {
  const id = `k6-${__VU}-${__ITER}-${Date.now()}-${Math.random().toString(36).slice(2)}`;
  const originalUrl = `https://example.com/k6/${id}`;
  const createResponse = http.post(
    `${baseUrl}/api/v1/urls`,
    JSON.stringify({ originalUrl }),
    {
      headers: { 'Content-Type': 'application/json' },
      tags: { operation: 'create' },
      timeout: '10s',
    },
  );
  let createBody;
  const createPassed = check(createResponse, {
    'create returns 201': (r) => r.status === 201,
    'create response is valid JSON': (r) => {
      try {
        createBody = r.json();
        return Boolean(createBody && createBody.shortCode && createBody.shortUrl);
      } catch (_) {
        return false;
      }
    },
  });
  if (!createPassed) {
    logRequestFailure('create', createResponse, 'expected HTTP 201 with a valid shortCode and shortUrl');
    sleep(1);
    return;
  }

  const code = createBody.shortCode;
  check(createBody, {
    'create preserves original URL': (body) => body.originalUrl === originalUrl,
    'create short URL contains its code': (body) => body.shortUrl.endsWith(`/${code}`),
  });

  const statsUrl = `${baseUrl}/api/v1/urls/${encodeURIComponent(code)}`;
  const initialStats = http.get(statsUrl, {
    tags: { operation: 'stats' },
    timeout: '10s',
  });
  let statsBody;
  const statsPassed = check(initialStats, {
    'stats returns 200': (r) => r.status === 200,
    'stats response is valid JSON': (r) => {
      try {
        statsBody = r.json();
        return Boolean(statsBody);
      } catch (_) {
        return false;
      }
    },
  });
  if (!statsPassed) {
    logRequestFailure('stats', initialStats, 'expected HTTP 200 with a valid JSON response');
    sleep(1);
    return;
  }
  check(statsBody, {
    'stats matches the created URL': (body) =>
      body.shortCode === code && body.originalUrl === originalUrl,
  });

  const redirectResponse = http.get(`${baseUrl}/${encodeURIComponent(code)}`, {
    redirects: 0,
    tags: { operation: 'redirect' },
    timeout: '10s',
  });
  const redirectPassed = check(redirectResponse, {
    'redirect returns 302': (r) => r.status === 302,
    'redirect location matches original URL': (r) => r.headers.Location === originalUrl,
  });
  if (!redirectPassed) {
    logRequestFailure('redirect', redirectResponse, 'expected HTTP 302 with the original URL in Location');
  }

  const updatedStats = http.get(statsUrl, {
    tags: { operation: 'stats' },
    timeout: '10s',
  });
  const updatedStatsPassed = check(updatedStats, {
    'post-redirect stats returns 200': (r) => r.status === 200,
    'redirect increments click count': (r) => {
      try {
        return r.json('clickCount') === statsBody.clickCount + 1;
      } catch (_) {
        return false;
      }
    },
  });
  if (!updatedStatsPassed) {
    logRequestFailure('stats', updatedStats, 'expected HTTP 200 and clickCount to increase after redirect');
  }

  sleep(1);
}
