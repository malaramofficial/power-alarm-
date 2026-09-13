const { onDocumentCreated } = require('firebase-functions/v2/firestore');
const { initializeApp } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');
const { getMessaging } = require('firebase-admin/messaging');

initializeApp();

exports.onPowerEventCreated = onDocumentCreated('power_events/{eventId}', async (event) => {
  const data = event.data?.data();
  if (!data?.feederId || !data?.state) return;

  const db = getFirestore();
  const users = await db.collection('users').where('feederIds', 'array-contains', data.feederId).get();
  const tokens = users.docs.flatMap((d) => d.data().fcmTokens || []).filter(Boolean);
  if (!tokens.length) return;

  const isOn = data.state === 'ON';
  const title = isOn ? '🟢 बिजली आ गई' : '🔴 बिजली चली गई';
  const body = isOn ? 'फीडर की लाइन फिर से उपलब्ध है।' : 'फीडर की लाइन बंद हो गई है।';

  await getMessaging().sendEachForMulticast({
    tokens,
    notification: { title, body },
    data: { feederId: String(data.feederId), state: String(data.state) },
  });
});
