# Scripts

Windows quick-start scripts.

- `run-local-dev.cmd`: start db+redis (docker) and open backend+frontend terminals.
- `run-local-backend.cmd`: run Spring Boot backend on `:8081` (profile `dev`).
- `run-local-frontend.cmd`: run Vue frontend on `:5331` (points API at `http://localhost:8081/api`).
- `run-bookinglist-perf.cmd`: run booking-list list-cache A/B perf flow (warm-up + cache ON + cache OFF) with JMeter CLI.

