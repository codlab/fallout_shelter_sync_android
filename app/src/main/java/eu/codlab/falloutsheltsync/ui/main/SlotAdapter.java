package eu.codlab.falloutsheltsync.ui.main;

import android.app.Activity;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.EventAttributes;
import com.google.android.gms.drive.Metadata;

import org.joda.time.DateTime;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import eu.codlab.falloutsheltsync.R;
import eu.codlab.falloutsheltsync.sync.SyncService;

/**
 * Created by kevinleperf on 24/08/15.
 */
public class SlotAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final static String STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE";
    private final static int LOCAL = 0;
    private final static int DISTANT = 1;
    private final Activity _parent;

    public class LocalViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.slot_name)
        public TextView _date;

        @OnClick(R.id.upload)
        public void onUploadClick() {

            if (_parent != null && _parent.shouldShowRequestPermissionRationale(STORAGE)) {
                _parent.requestPermissions(new String[]{STORAGE}, 42);
            } else {
                SyncService.getInstance().upload(_slot_name);
                Toast.makeText(_date.getContext(), R.string.uploading, Toast.LENGTH_SHORT).show();
                EventAttributes attributes = new EventAttributes();
                attributes.put("Slot", _slot_name);
                Answers.getInstance().logEvent("Upload", attributes);
            }
        }

        public LocalViewHolder(ViewGroup group) {
            this(LayoutInflater.from(group.getContext())
                    .inflate(R.layout.save_slot_local_item, group, false));

        }

        public LocalViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public class DistantViewHolder extends RecyclerView.ViewHolder {

        private Metadata _metadata;

        @Bind(R.id.slot_name)
        public TextView _date;

        @Bind(R.id.download)
        public Button _download;

        @Bind(R.id.use)
        public Button _use;

        @OnClick(R.id.download)
        public void onDownloadClick() {

            if (Build.VERSION.SDK_INT >= 23
                    && _parent != null && _parent.shouldShowRequestPermissionRationale(STORAGE)) {
                _parent.requestPermissions(new String[]{STORAGE}, 42);
            } else {
                if (_metadata != null && _metadata.getOriginalFilename() != null) {
                    SyncService.getInstance().readFileContent(_metadata.getOriginalFilename());
                    Toast.makeText(_date.getContext(), R.string.downloading, Toast.LENGTH_SHORT).show();
                    EventAttributes attributes = new EventAttributes();
                    attributes.put("Slot", _slot_name);
                    Answers.getInstance().logEvent("Download", attributes);
                }
            }
        }

        @OnClick(R.id.use)
        public void onUseClick() {
            if (Build.VERSION.SDK_INT >= 23
                    && _parent != null && _parent.shouldShowRequestPermissionRationale(STORAGE)) {
                _parent.requestPermissions(new String[]{STORAGE}, 42);
            } else {
                if (_metadata != null && _metadata.getOriginalFilename() != null) {
                    SyncService.getInstance().copy(_metadata.getOriginalFilename(), _slot_name);
                    EventAttributes attributes = new EventAttributes();
                    attributes.put("Slot", _slot_name);
                    Answers.getInstance().logEvent("Use", attributes);
                }
            }
        }

        public DistantViewHolder(ViewGroup group) {
            this(LayoutInflater.from(group.getContext())
                    .inflate(R.layout.save_slot_item, group, false));
        }

        public DistantViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void setFileDistant(Metadata metadata) {
            _metadata = metadata;
        }
    }

    private int _slot_name;
    private List<Metadata> _list;

    public SlotAdapter(Activity parent, List<Metadata> list, int slot_name) {
        _parent = parent;
        _list = list;
        _slot_name = slot_name;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        if (i == LOCAL) {
            return new LocalViewHolder(viewGroup);
        } else {
            return new DistantViewHolder(viewGroup);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (viewHolder instanceof LocalViewHolder) {
        } else {
            if (SyncService.getInstance().exists(_slot_name)) position--;

            Metadata data = getItem(position);


            DistantViewHolder holder = (DistantViewHolder) viewHolder;
            DateTime date = new DateTime(data.getCreatedDate());

            if (SyncService.getInstance().exists(data.getOriginalFilename())) {
                holder._use.setVisibility(View.VISIBLE);
                holder._download.setVisibility(View.GONE);
            } else {
                holder._use.setVisibility(View.GONE);
                holder._download.setVisibility(View.VISIBLE);
            }
            holder._date.setText(formatDate(date));
            holder.setFileDistant(data);
        }
    }

    public Metadata getItem(int position) {
        return _list.get(position);
    }

    @Override
    public int getItemCount() {
        return (SyncService.getInstance().exists(_slot_name) ? 1 : 0)
                + (_list != null ? _list.size() : 0);
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 && SyncService.getInstance().exists(_slot_name) ? LOCAL : DISTANT;
    }

    private String formatDate(DateTime date) {
        return String.format("%02d/%02d/%04d %02d:%02d:%02d",
                date.getDayOfMonth(), date.getMonthOfYear(), date.getYear(),
                date.getHourOfDay(), date.getMinuteOfHour(), date.getSecondOfMinute());
    }
}
