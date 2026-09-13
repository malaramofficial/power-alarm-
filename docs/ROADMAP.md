# बिजली प्रहरी — 20-Step Build Plan

1. Product scope: GSS-first monitoring — **Done**
2. Android project baseline — **Done**
3. Dark Hindi GSS UI — **Done**
4. Dashboard status card — **Done**
5. Feeder model — **Done**
6. GSS dashboard state model — **Done**
7. Power event model — **Done**
8. IoT device status model — **Done**
9. IoT heartbeat model — **Done**
10. Village coverage model — **Done**
11. Farmer notification subscription model — **Done**
12. Notification delivery log model — **Done**
13. Firebase repository for feeders/events/devices — **Done**
14. GSS event verification — **Done**
15. Firestore security rules — **Done**
16. Firestore query index — **Done**
17. FCM token registration service — **Done**
18. Power event → notification Cloud Function — **Done**
19. GitHub Actions main-branch release build — **Done**
20. Release APK verification — **In progress after latest main push**

## Next production work

- Add the real Firebase project's `google-services.json`.
- Add authenticated GSS login and feeder assignment management.
- Connect the actual ESP32/4G device protocol.
- Add outage debounce/hysteresis in the IoT firmware so short voltage dips do not create false alerts.
- Field-test sensor accuracy before any live GSS deployment.
