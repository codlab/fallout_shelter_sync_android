package eu.codlab.falloutsheltsync.event;

/**
 * Created by kevinleperf on 24/08/15.
 */
public class SyncState {
    private Boolean _sync_state;

    public SyncState(Boolean sync_state) {
        _sync_state = sync_state;
    }

    public Boolean getSyncState() {
        return _sync_state;
    }
}
