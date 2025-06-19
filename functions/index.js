const { onSchedule } = require('firebase-functions/v2/scheduler');
const { initializeApp } = require('firebase-admin/app');
const { getFirestore, Timestamp } = require('firebase-admin/firestore');
const { getMessaging } = require('firebase-admin/messaging');
const { logger } = require('firebase-functions');

// Initialize Firebase Admin SDK
initializeApp();
const db = getFirestore();

// 🔔 Function 1: Send reminders for appointments not finished
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

// 🔔 Function 2: Notify doctor when a future appointment is canceled
exports.notifyDoctorOfCanceledFutureAppointment = onSchedule(
  {
    schedule: 'every 1 minutes',
    timeZone: 'Europe/Warsaw',
    maxInstances: 1,
  },
  async () => {
    try {
      const now = Timestamp.now();

      const snapshot = await db.collection('appointments')
        .where('status', '==', 'CANCELED')
        .where('date', '>', now)
        .get();

      if (snapshot.empty) {
        logger.log('No future canceled appointments found');
        return;
      }

      const toNotify = snapshot.docs.filter(doc => !doc.data().hasOwnProperty('notified'));

      if (toNotify.length === 0) {
        logger.log('No new canceled appointments to notify');
        return;
      }

      const promises = toNotify.map(async (doc) => {
        try {
          const appointment = doc.data();

          const doctorSnap = await db.collection('doctors').doc(appointment.doctor_id).get();
          if (!doctorSnap.exists || !doctorSnap.data()?.fcmToken) {
            throw new Error(`Doctor ${appointment.doctor_id} has no FCM token`);
          }

          const userSnap = await db.collection('users').doc(appointment.user_id).get();
          const userName = userSnap.exists ? userSnap.data().name || 'Unknown Patient' : 'Unknown Patient';

          const appointmentDate = appointment.date.toDate().toLocaleString('pl-PL', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
          });

          const message = {
            token: doctorSnap.data().fcmToken,
            notification: {
              title: 'Appointment Canceled',
              body: `Appointment with ${userName} on ${appointmentDate} has been canceled.`
            },
            data: {
              type: 'cancel_notification',
              appointmentId: doc.id,
              patientName: userName,
              appointmentDate: appointmentDate
            },
            android: {
              priority: 'high'
            }
          };

          await getMessaging().send(message);
          await doc.ref.update({ notified: true });
          logger.log(`Doctor notified about canceled appointment ${doc.id}`);
        } catch (error) {
          logger.error(`Failed to notify for appointment ${doc.id}:`, error);
        }
      });

      await Promise.all(promises);
    } catch (error) {
      logger.error('Error in notifyDoctorOfCanceledFutureAppointment:', error);
    }
  }
);
