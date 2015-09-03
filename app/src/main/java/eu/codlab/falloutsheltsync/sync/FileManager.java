package eu.codlab.falloutsheltsync.sync;

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
 * Created by kevinleperf on 02/09/15.
 */
class FileManager {
    private List<SlotObsever> _slot_observers;
    private SyncService _parent;

    private FileManager() {

    }

    public FileManager(SyncService parent) {
        _parent = parent;
    }

    public void init(SyncService parent) {
        _parent = parent;
        _slot_observers = new ArrayList<>();
        for (int i = 1; i < 4; i++) {
            _slot_observers.add(new SlotObsever(_parent, i));
        }

        if (_slot_observers != null)
            for (SlotObsever slot : _slot_observers) slot.startWatching();
    }

    public void onDestroy() {
        _parent = null;
        if (_slot_observers != null)
            for (SlotObsever slot : _slot_observers) slot.stopWatching();
    }


    private void enableListeningWrite() {
        if (_slot_observers != null)
            for (SlotObsever slot : _slot_observers) slot.startWatching();
    }

    private void disableListeningWrite() {
        //if (_slot_observers != null)
        //    for (SlotObsever slot : _slot_observers) slot.stopWatching();
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

    public void copy(String origin, int slot) {
        disableListeningWrite();
        File internal = new File(SyncService.getInstance().getExternalFilesDir(null), origin);
        copy(internal, slot);
        enableListeningWrite();
    }

    public void copy(File origin, int slot) {
        disableListeningWrite();
        File file = new File(String.format(Constants.VAULT_FORMAT, slot));
        FileUtils.copyFile(origin, file);
        enableListeningWrite();
    }

    private void readFileContent(final String file_name, final File output, final DriveId drive_id) {

        Drive.DriveApi.getFile(_parent.getClient(), drive_id)
                .open(_parent.getClient(), DriveFile.MODE_READ_ONLY, null)
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
        Drive.DriveApi.getRootFolder(_parent.getClient())
                .createFile(_parent.getClient(), changeSet, result.getDriveContents())
                .setResultCallback(new ResultCallback<DriveFolder.DriveFileResult>() {
                    @Override
                    public void onResult(DriveFolder.DriveFileResult driveFileResult) {
                        if (driveFileResult.getStatus().isSuccess()) {
                            listFiles();
                        }
                    }
                });
    }

    public void newFileContent(final int slot, final DateTime date, final String content) {
        Drive.DriveApi.newDriveContents(_parent.getClient())
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

        Drive.DriveApi.query(_parent.getClient(), query)
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
        Drive.DriveApi.fetchDriveId(_parent.getClient(), drive_id)
                .setResultCallback(new ResultCallback<DriveApi.DriveIdResult>() {
                    @Override
                    public void onResult(DriveApi.DriveIdResult driveIdResult) {
                        readFileContent(file_name, output, driveIdResult.getDriveId());
                    }
                });
    }

    public void listFiles() {

        EventBus.getDefault().postSticky(new SyncState(null));

        Query query = new Query.Builder()
                .addFilter(Filters.contains(SearchableField.TITLE, ".fallout"))
                .build();

        Drive.DriveApi.query(_parent.getClient(), query)
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

    public void newFileContent(final int slot, final DateTime date) {
        File file = new File(String.format(Constants.VAULT_FORMAT, slot));
        newFileContent(slot, date, readFileName(file.getAbsolutePath()));
    }


    private String formatDate(DateTime date) {
        return String.format("%04d%02d%02d%02d%02d",
                date.getYear(), date.getMonthOfYear(), date.getDayOfMonth(),
                date.getHourOfDay(), date.getMinuteOfHour());//, date.getSecondOfMinute());
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
