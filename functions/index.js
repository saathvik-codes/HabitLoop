const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

exports.notifyCircleMessage = onDocumentCreated(
  {
    document: "circles/{circleId}/messages/{messageId}",
    region: "asia-south1",
    retry: false,
  },
  async (event) => {
    const message = event.data && event.data.data();
    if (!message || !message.text || !message.userId) return;
    const db = getFirestore();
    const circleId = event.params.circleId;
    const [circle, members] = await Promise.all([
      db.collection("circles").doc(circleId).get(),
      db.collection("circles").doc(circleId).collection("members").get(),
    ]);
    const title = circle.exists ? circle.data().title : "Circle discussion";
    const recipientIds = members.docs.map((doc) => doc.id).filter((id) => id !== message.userId);
    if (!recipientIds.length) return;
    const deviceSnapshots = await Promise.all(
      recipientIds.map((uid) => db.collection("users").doc(uid).collection("devices").get())
    );
    const devices = deviceSnapshots.flatMap((snapshot) =>
      snapshot.docs
        .map((doc) => ({ ...doc.data(), ref: doc.ref }))
        .filter((device) => device.circleMessages !== false && typeof device.token === "string")
        .map((device) => ({ token: device.token, ref: device.ref }))
    );
    if (!devices.length) return;
    const body = `${String(message.username || "A member").slice(0, 24)}: ${String(message.text).slice(0, 120)}`;
    for (let index = 0; index < devices.length; index += 500) {
      const batch = devices.slice(index, index + 500);
      const response = await getMessaging().sendEachForMulticast({
        tokens: batch.map((device) => device.token),
        data: {
          category: "circle_message",
          circleId,
          title: String(title).slice(0, 60),
          body,
        },
        android: {
          priority: "high",
          ttl: 60 * 60 * 1000,
        },
      });
      const staleCodes = new Set([
        "messaging/registration-token-not-registered",
        "messaging/invalid-registration-token",
      ]);
      await Promise.all(response.responses.map((result, resultIndex) => {
        const ref = batch[resultIndex].ref;
        return !result.success && ref && staleCodes.has(result.error && result.error.code)
          ? ref.delete()
          : Promise.resolve();
      }));
    }
  }
);
