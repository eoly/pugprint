# 3. Use Kable for BLE
Date: 2026-09-20 · Status: Accepted

## Decision
Use Kable (JuulLabs), coroutine/Flow-based, for scanning, connect, notify, and writes.

## Consequences
- Matches our StateFlow/coroutine architecture; avoids BluetoothGattCallback boilerplate.
- Alternative considered: Nordic Kotlin-BLE-Library (very robust queueing; heavier API).
- Risk: Kable has had reports of missed notification packets under load — mitigate with our
  own operation queue and integration tests; fall back to Nordic if needed.
