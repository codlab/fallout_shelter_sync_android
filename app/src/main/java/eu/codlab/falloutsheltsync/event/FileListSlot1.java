package eu.codlab.falloutsheltsync.event;

import com.google.android.gms.drive.Metadata;

import java.util.List;

/**
 * Created by kevinleperf on 24/08/15.
 */
public class FileListSlot1 {
    public List<Metadata> _metadataList;

    public FileListSlot1(List<Metadata> metadataList) {
        _metadataList = metadataList;
    }

    public List<Metadata> getList() {
        return _metadataList;
    }
}
