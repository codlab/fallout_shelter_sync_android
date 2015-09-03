package eu.codlab.falloutsheltsync.sync;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;

import eu.codlab.falloutsheltsync.R;

/**
 * Created by kevinleperf on 02/09/15.
 */
class NotificationHelper {
    private static final int getNotificationId(int slot) {
        return 43 * 100 + slot;
    }

    public static Notification createForegroungNotification(SyncService service) {
        Notification notification = new NotificationCompat.Builder(service)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(service.getString(R.string.foreground_title))
                .setContentText(service.getString(R.string.foreground_text))
                .setOngoing(true).build();
        return notification;
    }

    private static PendingIntent createPendingIntent(SyncService service, String action, int slot) {
        Intent previousIntent = new Intent(service, SyncService.class);
        previousIntent.setAction(action)
                .putExtra(Constants.SLOT, slot);
        return PendingIntent.getService(service, 0, previousIntent, 0);

    }

    public static void createForegroundNotification(SyncService service) {
        service.startForeground(42, createForegroungNotification(service));
    }

    public static void cancelForegroundNotification(SyncService service) {
        service.stopForeground(true);
    }

    public static void cancelNewSaveNotificationAvailable(SyncService service, int slot) {

        NotificationManager notificationManager =
                (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.cancel(getNotificationId(slot));

    }

    public static void createNewSaveNotificationAvailable(SyncService service, int slot) {
        Notification notification = new NotificationCompat.Builder(service)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(service.getString(R.string.upload_title))
                .setContentText(service.getString(R.string.upload_text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                .setVibrate(new long[0])
                .addAction(R.drawable.ic_clear_black_24dp, service.getString(R.string.upload_cancel),
                        createPendingIntent(service, Constants.ACTION_CANCEL, slot))
                .addAction(R.drawable.ic_publish_black_24dp, service.getString(R.string.upload_upload),
                        createPendingIntent(service, Constants.ACTION_UPLOAD, slot))
                .build();

        NotificationManager notificationManager =
                (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(getNotificationId(slot), notification);
    }
}
