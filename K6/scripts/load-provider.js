import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 10,
  duration: '10s',

  stages: [
    { duration: '2s', target: 10 },
    { duration: '6s', target: 10 },
    { duration: '2s', target: 0 }
  ]

};

export default function () {
    const res = http.get('http://localhost:8081/provideravailability/allproviders');   
    sleep(1);

    check(res,{
        "status is 200": (r) => r.status === 200,
        "time is less than 500ms": (r) => r.timings.duration < 500
    })
}