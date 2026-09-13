# बिजली प्रहरी IoT protocol

## Device → Cloud event

A monitor device reports a power transition to `power_events` with:

- `feederId`: stable feeder ID
- `state`: `ON` or `OFF`
- `atMillis`: UTC epoch milliseconds
- `source`: `device`
- `deviceId`: stable device ID
- `sequence`: monotonically increasing device sequence

## Heartbeat

Device status is stored in `devices/{deviceId}`:

- `feederId`
- `online`
- `batteryPercent`
- `signalPercent`
- `lastHeartbeatAt`
- `firmwareVersion`

## Reliability rules

1. Never connect mains voltage directly to ESP32 GPIO.
2. Use an appropriately rated isolated sensing circuit.
3. Require a short debounce window before accepting an ON/OFF transition.
4. Device should buffer unsent events locally and retry after network recovery.
5. Sequence numbers prevent duplicate event processing.
6. Heartbeat timeout is treated as **device offline**, not automatically as a power outage.
7. Secrets/API credentials must never be committed to this repository.
