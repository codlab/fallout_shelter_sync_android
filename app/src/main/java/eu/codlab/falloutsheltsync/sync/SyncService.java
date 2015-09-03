package eu.codlab.falloutsheltsync.sync;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;
import android.widget.Toast;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.EventAttributes;
import com.google.android.gms.common.api.GoogleApiClient;

import org.joda.time.DateTime;

import java.io.File;

import eu.codlab.falloutsheltsync.R;

/**
 * Created by kevinleperf on 24/08/15.
 */
public class SyncService extends Service {
    private static SyncService _instance;

    public static SyncService getInstance() {
        return _instance;
    }

    private GoogleApiClient _google_api_client;
    private FileManager _file_manager;

    private void checkState() {
        if (_google_api_client != null && _google_api_client.isConnected()) {
            NotificationHelper.createForegroundNotification(this);
        } else {
            NotificationHelper.cancelForegroundNotification(this);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        _instance = this;
        _file_manager = new FileManager(this);
        checkState();
    }

    @Override
    public int onStartCommand(Intent intent, int flag, int startId) {
        if (intent != null && intent.getAction() != null
                && intent.getIntExtra(Constants.SLOT, -1) != -1) {
            int slot = intent.getIntExtra(Constants.SLOT, -1);
            if (Constants.ACTION_CANCEL.equals(intent.getAction())) {
                NotificationHelper.cancelNewSaveNotificationAvailable(this, slot);
            } else if (Constants.ACTION_UPLOAD.equals(intent.getAction())) {
                upload(slot);
                NotificationHelper.cancelNewSaveNotificationAvailable(this, slot);
            }
        }
        return super.onStartCommand(intent, flag, startId);
    }

    @Override
    public void onDestroy() {
        _file_manager.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IBinder mBinder = new myBinder();

    public void setGoogleApiClient(GoogleApiClient google_api_client) {
        _google_api_client = google_api_client;

        checkState();
    }

    public void init() {
        _file_manager.init(this);
        _file_manager.listFiles();
        checkState();
    }

    public GoogleApiClient getClient() {
        return _google_api_client;
    }

    public boolean exists(int slot) {
        File file = new File(String.format(Constants.VAULT_FORMAT, slot));
        return file.exists();
    }


    public boolean exists(final String file_name) {
        if (file_name == null || TextUtils.isEmpty(file_name)) return false;
        File output = new File(SyncService.getInstance().getExternalFilesDir(null), file_name);
        return output.exists();
    }

    public void readFileContent(String originalFilename) {
        _file_manager.readFileContent(originalFilename);
    }

    public void copy(String originalFilename, int slot_name) {
        _file_manager.copy(originalFilename, slot_name);
    }

    public void newFileContent(int slot_name, DateTime dateTime) {
        _file_manager.newFileContent(slot_name, dateTime);
    }

    public void upload(int slot_name) {
        newFileContent(slot_name, new DateTime());
        Toast.makeText(this, R.string.uploading, Toast.LENGTH_SHORT).show();
        EventAttributes attributes = new EventAttributes();
        attributes.put("Slot", slot_name);
        Answers.getInstance().logEvent("Upload", attributes);
    }

    public void listFiles() {
        _file_manager.listFiles();
    }

    public void onSlotAvailable(int slot) {
        NotificationHelper.createNewSaveNotificationAvailable(this, slot);
    }

    public class myBinder extends Binder {
        public SyncService getService() {
            return SyncService.this;
        }
    }
}
