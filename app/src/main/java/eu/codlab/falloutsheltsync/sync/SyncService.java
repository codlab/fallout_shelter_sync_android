package eu.codlab.falloutsheltsync.sync;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;

import org.joda.time.DateTime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import de.greenrobot.event.EventBus;
import eu.codlab.falloutsheltsync.ApplicationController;
import eu.codlab.falloutsheltsync.event.FileEventRead;
import eu.codlab.falloutsheltsync.event.FileListObtained;
import eu.codlab.falloutsheltsync.event.FileListSlot1;
import eu.codlab.falloutsheltsync.event.FileListSlot2;
import eu.codlab.falloutsheltsync.event.FileListSlot3;
import eu.codlab.falloutsheltsync.event.SyncState;
import eu.codlab.falloutsheltsync.util.FileUtils;

/**
 * Created by kevinleperf on 24/08/15.
 */
public class SyncService extends Service {
    private final static String VAULT_FORMAT = "/sdcard/Android/data/com.bethsoft.falloutshelter/files/Vault%d.sav";
    private static SyncService _instance;

    public static SyncService getInstance() {
        return _instance;
    }

    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        _instance = this;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IBinder mBinder = new myBinder();

    public void setGoogleApiClient(GoogleApiClient google_api_client) {
        mGoogleApiClient = google_api_client;
    }

    public void init() {
        listFiles();
    }

    private String formatDate(DateTime date) {
        return String.format("%04d%02d%02d%02d%02d",
                date.getYear(), date.getMonthOfYear(), date.getDayOfMonth(),
                date.getHourOfDay(), date.getMinuteOfHour());//, date.getSecondOfMinute());
    }

    public GoogleApiClient getClient() {
        return mGoogleApiClient;
    }


    public void listFiles() {

        EventBus.getDefault().postSticky(new SyncState(null));

        Query query = new Query.Builder()
                .addFilter(Filters.contains(SearchableField.TITLE, ".fallout"))
                .build();

        Drive.DriveApi.query(mGoogleApiClient, query)
                .setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
                    @Override
                    public void onResult(DriveApi.MetadataBufferResult metadataBufferResult) {
                        MetadataBuffer buffer = metadataBufferResult.getMetadataBuffer();
                        EventBus.getDefault().postSticky(new FileListObtained(buffer));

                        List<Metadata> slot1 = new ArrayList<>();
                        List<Metadata> slot2 = new ArrayList<>();
                        List<Metadata> slot3 = new ArrayList<>();

                        Metadata data = null;
                        for (int i = 0; i < buffer.getCount(); i++) {
                            data = buffer.get(i);
                            //TODO REGEXP
                            if (data.getTitle().endsWith(".fallout")) {
                                if (data.getTitle().indexOf("Vault1.") >= 0) {
                                    slot1.add(data);
                                } else if (data.getTitle().indexOf("Vault2.") >= 0) {
                                    slot2.add(data);
                                } else if (data.getTitle().indexOf("Vault3.") >= 0) {
                                    slot3.add(data);
                                }
                            }
                        }

                        Comparator comparator = new Comparator<Metadata>() {
                            @Override
                            public int compare(Metadata lhs, Metadata rhs) {
                                Date left = lhs.getCreatedDate();
                                Date right = lhs.getCreatedDate();
                                return left.getTime() > right.getTime() ? 1
                                        : (left.getTime() == right.getTime() ? 0 : -1);
                            }

                            @Override
                            public boolean equals(Object object) {
                                return false;
                            }
                        };

                        Collections.sort(slot1, comparator);

                        EventBus.getDefault().postSticky(new FileListSlot1(slot1));
                        EventBus.getDefault().postSticky(new FileListSlot2(slot2));
                        EventBus.getDefault().postSticky(new FileListSlot3(slot3));
                        EventBus.getDefault().postSticky(new SyncState(true));
                    }
                });
    }


    private String readFileName(String filename) {
        FileInputStream fstream = null;
        try {
            fstream = new FileInputStream(filename);
            StringBuffer buffer = new StringBuffer();

            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            while ((strLine = br.readLine()) != null) {
                buffer.append(strLine);
            }
            br.close();
            return buffer.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void newFileContent(final int slot, final DateTime date) {
        File file = new File(String.format(VAULT_FORMAT, slot));
        newFileContent(slot, date, readFileName(file.getAbsolutePath()));
    }

    public boolean exists(int slot) {
        File file = new File(String.format(VAULT_FORMAT, slot));
        return file.exists();
    }


    public boolean exists(final String file_name) {
        if (file_name == null || TextUtils.isEmpty(file_name)) return false;
        File output = new File(SyncService.getInstance().getExternalFilesDir(null), file_name);
        return output.exists();
    }


    public void copy(String origin, int slot) {
        File internal = new File(SyncService.getInstance().getExternalFilesDir(null), origin);
        copy(internal, slot);
    }

    public void copy(File origin, int slot) {
        File file = new File(String.format(VAULT_FORMAT, slot));
        FileUtils.copyFile(origin, file);
    }


    public void newFileContent(final int slot, final DateTime date, final String content) {
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {
                    @Override
                    public void onResult(DriveApi.DriveContentsResult driveContentsResult) {
                        newFileContent(slot, date, content, driveContentsResult);
                    }
                });
    }

    public void readFileContent(final String file_name) {
        File output = new File(SyncService.getInstance().getExternalFilesDir(null), file_name);
        readFileContent(file_name, output);
    }

    public void readFileContent(final String file_name, final File output) {
        Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, file_name))
                .build();

        Drive.DriveApi.query(mGoogleApiClient, query)
                .setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
                    @Override
                    public void onResult(DriveApi.MetadataBufferResult metadataBufferResult) {
                        MetadataBuffer buffer = metadataBufferResult.getMetadataBuffer();
                        int count = buffer.getCount();
                        if (count > 0) {
                            readFileContent(file_name, output, buffer.get(0).getDriveId());
                        }
                    }
                });
    }

    public void readFileContent(final String file_name, final File output, final String drive_id) {
        Drive.DriveApi.fetchDriveId(mGoogleApiClient, drive_id)
                .setResultCallback(new ResultCallback<DriveApi.DriveIdResult>() {
                    @Override
                    public void onResult(DriveApi.DriveIdResult driveIdResult) {
                        readFileContent(file_name, output, driveIdResult.getDriveId());
                    }
                });
    }

    private void readFileContent(final String file_name, final File output, final DriveId drive_id) {

        Drive.DriveApi.getFile(mGoogleApiClient, drive_id)
                .open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null)
                .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {
                    @Override
                    public void onResult(DriveApi.DriveContentsResult driveContentsResult) {
                        try {
                            if (!output.exists()) output.createNewFile();

                            InputStream input = driveContentsResult.getDriveContents()
                                    .getInputStream();
                            FileOutputStream output_stream = new FileOutputStream(output);

                            copyStream(input, output_stream);

                            input.close();
                            output_stream.close();

                            EventBus.getDefault().post(new FileEventRead(file_name, output, drive_id, true));

                            listFiles();
                        } catch (IOException e) {
                            e.printStackTrace();
                            EventBus.getDefault().post(new FileEventRead(file_name, output, drive_id, false));
                        }
                    }
                });
    }

    private void newFileContent(int slot, DateTime date, final String content, DriveApi.DriveContentsResult result) {

        OutputStream fileOutputStream = result.getDriveContents().getOutputStream();
        Writer writer = new OutputStreamWriter(fileOutputStream);
        try {
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle("Vault" + slot + "." + formatDate(date)
                        + "." + ApplicationController.getInstance().getGuid() + ".fallout")
                .setMimeType("text/plain").build();

        // Create a file in the root folder
        Drive.DriveApi.getRootFolder(mGoogleApiClient)
                .createFile(mGoogleApiClient, changeSet, result.getDriveContents())
                .setResultCallback(new ResultCallback<DriveFolder.DriveFileResult>() {
                    @Override
                    public void onResult(DriveFolder.DriveFileResult driveFileResult) {
                        if (driveFileResult.getStatus().isSuccess()) {
                            listFiles();
                        }
                    }
                });
    }

    public class myBinder extends Binder {
        public SyncService getService() {
            return SyncService.this;
        }
    }

    public static void copyStream(InputStream input, OutputStream output)
            throws IOException {
        byte[] buffer = new byte[1024]; // Adjust if you want
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }
}
