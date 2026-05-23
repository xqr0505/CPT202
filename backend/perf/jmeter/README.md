# Booking List Tab Toggle Perf (List Cache A/B)

This folder contains a JMeter CLI plan to stress:

- `GET /api/v1/customer/bookings/list?tab=UPCOMING`
- `GET /api/v1/customer/bookings/list?tab=HISTORY`

The plan alternates `UPCOMING -> HISTORY` in the same thread/session, simulating frequent tab switching.

## 1) Preconditions

- Backend is running and reachable.
- A valid customer JWT token is available.
- Test data contains both upcoming and history bookings for this customer.
- Apache JMeter 5.6+ installed and available.

## 2) Key parameters

- `threads`: concurrent users, default `20`
- `ramp_up`: seconds to ramp, default `20`
- `loops`: per-thread loop count, default `180`
- `auth_token`: required JWT

## 3) Warm-up run

```bash
jmeter -n -t backend/perf/jmeter/booking-list-tab-toggle.jmx \
  -Jbase_protocol=http \
  -Jbase_host=127.0.0.1 \
  -Jbase_port=8081 \
  -Jthreads=10 \
  -Jramp_up=10 \
  -Jloops=30 \
  -Jauth_token=YOUR_CUSTOMER_JWT \
  -l backend/perf/jmeter/results/warmup.jtl
```

## 4) List cache ON run

Run backend with:

- `BOOKING_CACHE_LIST_ENABLED=true`

Then run:

```bash
jmeter -n -t backend/perf/jmeter/booking-list-tab-toggle.jmx \
  -Jbase_protocol=http \
  -Jbase_host=127.0.0.1 \
  -Jbase_port=8081 \
  -Jthreads=20 \
  -Jramp_up=20 \
  -Jloops=180 \
  -Jauth_token=YOUR_CUSTOMER_JWT \
  -l backend/perf/jmeter/results/list-cache-on.jtl \
  -e -o backend/perf/jmeter/reports/list-cache-on
```

## 5) List cache OFF run

Restart backend with:

- `BOOKING_CACHE_LIST_ENABLED=false`

Keep Redis running to avoid timeout pollution. Then run the same load:

```bash
jmeter -n -t backend/perf/jmeter/booking-list-tab-toggle.jmx \
  -Jbase_protocol=http \
  -Jbase_host=127.0.0.1 \
  -Jbase_port=8081 \
  -Jthreads=20 \
  -Jramp_up=20 \
  -Jloops=180 \
  -Jauth_token=YOUR_CUSTOMER_JWT \
  -l backend/perf/jmeter/results/list-cache-off.jtl \
  -e -o backend/perf/jmeter/reports/list-cache-off
```

## 6) Acceptance criterion

- Pass if `p95_cache_on <= p95_cache_off * 0.70`

