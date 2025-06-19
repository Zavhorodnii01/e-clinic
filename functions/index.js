const { onSchedule } = require('firebase-functions/v2/scheduler');
const { initializeApp } = require('firebase-admin/app');
const { getFirestore, Timestamp } = require('firebase-admin/firestore');
const { getMessaging } = require('firebase-admin/messaging');
const { logger } = require('firebase-functions');

initializeApp();
const db = getFirestore();

exports.sendAppointmentReminders = onSchedule(
  {
    schedule: 'every 2 minutes',
    timeZone: 'Europe/Warsaw',
    maxInstances: 1,
  },
  async () => {
    try {
      const now = Timestamp.now();
      const twoMinutesAgo = Timestamp.fromMillis(now.toMillis() - 120000);

      const querySnapshot = await db.collection('appointments')
        .where('status', '==', 'NOT_FINISHED')
        .where('date', '<=', twoMinutesAgo)
        .get();

      if (querySnapshot.empty) {
        logger.log('No unfinished appointments found');
        return;
      }

      const promises = querySnapshot.docs.map(async (doc) => {
        try {
          const appointment = doc.data();
          const doctorRef = db.collection('doctors').doc(appointment.doctor_id);
          const doctorSnap = await doctorRef.get();

          if (!doctorSnap.exists || !doctorSnap.data()?.fcmToken) {
            throw new Error(`Doctor ${appointment.doctor_id} has no FCM token`);
          }

          const message = {
            token: doctorSnap.data().fcmToken,
            notification: {
              title: 'Appointment Not Finished',
              body: 'Please confirm the appointment is completed'
            },
            data: {
              type: 'finish_reminder',
              appointmentId: doc.id
            },
            android: {
              priority: 'high'
            }
          };

          await getMessaging().send(message);
          logger.log(`Reminder sent for appointment ${doc.id}`);
        } catch (error) {
          logger.error(`Error processing appointment ${doc.id}:`, error);
        }
      });

      await Promise.all(promises);
    } catch (error) {
      logger.error('Error in sendAppointmentReminders:', error);
    }
  }
);
