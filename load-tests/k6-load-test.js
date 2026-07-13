import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '1m', target: 100 },  // Ramp-up to 100 concurrent developers
    { duration: '3m', target: 100 },  // Stay at 100 VUs
    { duration: '1m', target: 1000 }, // Scale up to 1000 concurrent executions (burst stress)
    { duration: '3m', target: 1000 }, // Stay at 1000 VUs (scheduler spikes & retry storms simulation)
    { duration: '1m', target: 0 },    // Cooldown
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% of requests must complete within 500ms
    http_req_failed: ['rate<0.01'],   // Error rate must be less than 1%
  },
};

const BASE_URL = 'http://localhost:8080/api/v1';
const INTERNAL_TOKEN = 'default-internal-token-secret-key-12345';

// Mock setup payload: linear workflow with fan-out
const workflowJson = JSON.stringify({
  name: 'Load Test Workflow',
  definitionJson: JSON.stringify({
    nodes: [
      { id: 'start-1', type: 'START' },
      { id: 'job-1', type: 'JOB', jobPublicId: '550e8400-e29b-44d4-a716-446655440000' },
      { id: 'end-1', type: 'END' }
    ],
    edges: [
      { from: 'start-1', to: 'job-1' },
      { from: 'job-1', to: 'end-1' }
    ]
  })
});

export default function () {
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer dummy-developer-jwt-token-placeholder', // Mocked token auth
  };

  // 1. Submit workflow definition (stress testing API parsing)
  let projectPublicId = '550e8400-e29b-44d4-a716-446655440000';
  let createRes = http.post(`${BASE_URL}/projects/${projectPublicId}/workflows`, workflowJson, { headers });
  
  check(createRes, {
    'create definition status is 200': (r) => r.status === 200,
  });

  if (createRes.status === 200) {
    let definitionPublicId = JSON.parse(createRes.body).publicId;

    // 2. Trigger Workflow Run
    let runRes = http.post(`${BASE_URL}/workflows/${definitionPublicId}/runs`, {}, { headers });
    check(runRes, {
      'trigger workflow run status is 200': (r) => r.status === 200,
    });
  }

  sleep(1); // Standard pacing delay between loop iterations
}
