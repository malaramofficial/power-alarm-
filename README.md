# बिजली प्रहरी

GSS-केंद्रित बिजली लाइन मॉनिटरिंग और किसान सूचना प्रणाली।

## काम करने का तरीका

**IoT लाइन सेंसर → Firebase → GSS Dashboard → Notification → Farmer**

### मुख्य मॉड्यूल
- GSS dashboard: feeder status, outage duration, आज का summary और device health
- Feeder/village mapping
- Power event history और GSS verification
- IoT device: online status, 4G signal और battery health
- Firebase Cloud Messaging notifications
- Notification delivery logs
- Role model: `ADMIN`, `GSS`, `FARMER`
- Firestore security rules और query indexes

## Firebase collections

`users`, `feeders`, `villages`, `devices`, `power_events`, `subscriptions`, `notification_logs`

## सुरक्षा

220V लाइन को Android/ESP32 से सीधे न जोड़ें। वास्तविक deployment में certified isolation और उचित electrical protection जरूरी है।

## Firebase setup

Android app Firebase configuration मिलने तक optional/fallback mode में build हो सकती है। Production connection के लिए सही Firebase project का `google-services.json` अलग से जोड़ना होगा।

## Build

GitHub Actions `main` पर push होने पर release APK build और artifact publish करता है।

> यह प्रोजेक्ट अभी development stage में है; वास्तविक GSS deployment से पहले sensor accuracy, outage debounce, security rules और notification reliability का field test जरूरी है।
