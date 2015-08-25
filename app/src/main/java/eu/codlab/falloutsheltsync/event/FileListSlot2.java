package eu.codlab.falloutsheltsync.event;

import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;

import java.util.List;

/**
 * Created by kevinleperf on 24/08/15.
 */
public class FileListSlot2 {
    public List<Metadata> _metadataList;

    public FileListSlot2(List<Metadata> metadataList) {
        _metadataList = metadataList;
    }

    public List<Metadata> getList() {
        return _metadataList;
    }
}
