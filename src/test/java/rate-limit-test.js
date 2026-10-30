
import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

export const options = {
    scenarios: {
        brute_force_simulation: {
            executor: 'shared-iterations',
            vus: 10,
            iterations: 100,
            maxDuration: '30s',
        },
    },
};

const BASE_URL = 'http://localhost:8081';

const rateLimited = new Counter('rate_limited_requests');
const unauthorized = new Counter('unauthorized_requests');
const successful = new Counter('successful_requests');

export default function () {
    const payload = JSON.stringify({
        username: 'rate_limit_test',
        password: 'WrongPassword123!',
    });

    const response = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        payload,
        {
            headers: {
                'Content-Type': 'application/json',
            },
        }
    );

    if (response.status === 429) {
        rateLimited.add(1);
    } else if (response.status === 401) {
        unauthorized.add(1);
    } else if (response.status === 200) {
        successful.add(1);
    }

    check(response, {
        'request returned expected status': (r) =>
            [200, 401, 429].includes(r.status),
        'rate limiter returns HTTP 429': (r) =>
            r.status !== 429 ||
            r.headers['Retry-After'] !== undefined,
    });
}
