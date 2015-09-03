package eu.codlab.falloutsheltsync.sync;

import android.os.FileObserver;
import android.util.Log;

/**
 * Created by kevinleperf on 02/09/15.
 */
public class SlotObsever extends FileObserver {
    private SyncService _parent;
    private int _slot;

    public SlotObsever(SyncService parent, int slot) {
        this(String.format(Constants.VAULT_FORMAT, slot));
        _slot = slot;
        _parent = parent;
    }

    private SlotObsever(String path) {
        super(path);
    }

    private SlotObsever(String path, int mask) {
        super(path, mask);
    }

    @Override
    public void onEvent(int event, String path) {
        switch (event & FileObserver.ALL_EVENTS) {
            case FileObserver.CLOSE_WRITE:
                Log.d("SlotObserver", "having event finished write on " + _slot);
                _parent.onSlotAvailable(_slot);

        }
    }

    public int getSlot() {
        return _slot;
    }
}
