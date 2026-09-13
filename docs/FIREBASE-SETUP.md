# बिजली प्रहरी — Firebase setup

यह दस्तावेज़ production Firebase जोड़ने के लिए है। Repository में कोई secret या `google-services.json` commit नहीं करना है।

## 1. Firebase project

1. Firebase Console में नया project बनाएं।
2. Android app जोड़ें।
3. Package name रखें: `com.malaramofficial.bijliprahari`.
4. Authentication, Firestore और Cloud Messaging enable करें।
5. Android configuration file `google-services.json` डाउनलोड करें और local build environment में रखें।

## 2. Authentication

प्राथमिक operational roles:

- `ADMIN` — पूरा सिस्टम प्रबंधन
- `GSS` — केवल assigned feeders का संचालन/verification
- `FARMER` — अपने feeder की स्थिति और notifications

Role Firestore के `users/{uid}.role` में server/admin द्वारा नियंत्रित होगा। User को स्वयं `ADMIN` या `GSS` बनाने की अनुमति नहीं है।

## 3. Feeder assignment

GSS user document में `feederIds` array रखें, उदाहरण:

```text
users/{uid}
  role: "GSS"
  feederIds: ["meethi-beri", "demo-2"]
```

Feeder document में कम से कम `name`, `gssName`, `currentState`, `active` रखें।

## 4. Power events

Expected fields:

```text
feederId
state: "ON" | "OFF"
atMillis
source: "device" | "gss"
verified: boolean
```

Production IoT device को सीधे खुले Firestore client write की जगह authenticated server/device ingestion path से event भेजना चाहिए। फिलहाल Firestore rules direct event creation को केवल Admin/assigned GSS तक सीमित करते हैं।

## 5. FCM

App को current FCM token Firestore में user document के `fcmTokens` array में register करना है। Cloud Function `power_events/{eventId}` बनने पर eligible users को notification भेजती है।

आगे token refresh/invalid-token cleanup और feeder/village topics जोड़े जाएंगे।

## 6. Security checklist

- Firebase secrets repository में commit न करें।
- `ADMIN` role केवल trusted admin workflow से दें।
- GSS को केवल assigned feeders तक सीमित रखें।
- `notification_logs` client-write से बंद रहें।
- IoT credentials को Android APK में hard-code न करें।
- Power outage का निर्णय heartbeat timeout से अकेले न लें; heartbeat और power event अलग signals हैं।

## 7. Production inputs still required

यह repository architecture और code readiness देता है, लेकिन वास्तविक production connection के लिए Firebase project configuration, authentication setup और physical IoT device credentials/hardware अभी अलग deployment inputs हैं।
