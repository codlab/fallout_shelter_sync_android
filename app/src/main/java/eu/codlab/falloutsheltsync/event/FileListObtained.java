package eu.codlab.falloutsheltsync.event;

import com.google.android.gms.drive.MetadataBuffer;

/**
 * Created by kevinleperf on 24/08/15.
 */
public class FileListObtained {
    private MetadataBuffer _buffer;

    public FileListObtained(MetadataBuffer buffer) {
        _buffer = buffer;
    }

    public MetadataBuffer getMetadataBuffer() {
        return _buffer;
    }
}
