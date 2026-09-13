const { onDocumentCreated } = require('firebase-functions/v2/firestore');
const { initializeApp } = require('firebase-admin/app');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');
const { getMessaging } = require('firebase-admin/messaging');

initializeApp();

exports.onPowerEventCreated = onDocumentCreated('power_events/{eventId}', async (event) => {
  const data = event.data?.data();
  const eventId = event.params.eventId;
  if (!data?.feederId || !['ON', 'OFF'].includes(data.state)) return;

  const db = getFirestore();
  const users = await db.collection('users')
    .where('feederIds', 'array-contains', data.feederId)
    .get();

  const recipients = users.docs.flatMap((doc) => {
    const u = doc.data();
    if (u.role === 'GSS') return [];
    return Array.isArray(u.fcmTokens) ? u.fcmTokens.filter(Boolean) : [];
  });
  const tokens = [...new Set(recipients)];

  const isOn = data.state === 'ON';
  const title = isOn ? '🟢 बिजली आ गई' : '🔴 बिजली चली गई';
  const body = isOn ? 'फीडर की लाइन फिर से उपलब्ध है।' : 'फीडर की लाइन बंद हो गई है।';

  let sentCount = 0;
  let failedCount = 0;
  if (tokens.length) {
    const response = await getMessaging().sendEachForMulticast({
      tokens,
      notification: { title, body },
      data: { feederId: String(data.feederId), state: String(data.state), eventId }
    });
    sentCount = response.successCount;
    failedCount = response.failureCount;
  }

  await db.collection('notification_logs').doc(eventId).set({
    eventId,
    feederId: data.feederId,
    state: data.state,
    recipientCount: tokens.length,
    sentCount,
    failedCount,
    createdAt: FieldValue.serverTimestamp()
  });
});
