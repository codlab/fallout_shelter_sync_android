package eu.codlab.falloutsheltsync.event;

import com.google.android.gms.drive.DriveId;

import java.io.File;

/**
 * Created by kevinleperf on 24/08/15.
 */
public class FileEventRead {
    private String _file_name;
    private File _file_path;
    private DriveId _drive_id;
    private boolean _ok;

    public FileEventRead(String file_name, File file_path, DriveId drive_id, boolean ok) {
        _file_name = file_name;
        _file_path = file_path;
        _drive_id = drive_id;
        _ok = ok;
    }

    public String getFileName() {
        return _file_name;
    }

    public File getFilePath() {
        return _file_path;
    }

    public DriveId getDriveId() {
        return _drive_id;
    }

    public boolean isOk() {
        return _ok;
    }
}
