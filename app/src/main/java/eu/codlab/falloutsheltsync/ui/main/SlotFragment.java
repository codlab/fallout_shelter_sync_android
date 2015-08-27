package eu.codlab.falloutsheltsync.ui.main;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.drive.Metadata;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import eu.codlab.falloutsheltsync.R;
import eu.codlab.falloutsheltsync.event.FileListSlot1;
import eu.codlab.falloutsheltsync.event.FileListSlot2;
import eu.codlab.falloutsheltsync.event.FileListSlot3;

/**
 * Created by kevinleperf on 24/08/15.
 */
public class SlotFragment extends Fragment {
    private final static String POSITION = "POSITION";

    public static SlotFragment newInstance(int position) {
        Bundle arguments = new Bundle();
        arguments.putInt(POSITION, position);

        SlotFragment fragment = new SlotFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    private int _index = -1;
    private int _scroll = 0;

    @Bind(R.id.recycler)
    public RecyclerView _recycler;

    private int getIndex() {
        if (_index == -1 && getArguments() != null) {
            _index = getArguments().getInt(POSITION, 0);
        }
        return _index;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.slot_fragment, parent, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ButterKnife.bind(this, view);
        _recycler.setLayoutManager(new LinearLayoutManager(view.getContext()));
        _recycler.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                _scroll += dy;
                ((MainActivity) getActivity())._refresh_layout.setEnabled(isScrolledToTop());
            }
        });
    }

    public boolean isScrolledToTop() {
        return Math.abs(_scroll) < 50;
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    private void refreshAdapter(List<Metadata> list, int slot) {
        _recycler.setAdapter(new SlotAdapter(getActivity(), list, slot));
    }

    @Subscribe(threadMode = ThreadMode.MainThread, sticky = true)
    public void onEventSlot1(FileListSlot1 slot) {
        if (getIndex() == 0) {
            refreshAdapter(slot.getList(), 1);
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread, sticky = true)
    public void onEventSlot2(FileListSlot2 slot) {
        if (getIndex() == 1) {
            refreshAdapter(slot.getList(), 2);
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread, sticky = true)
    public void onEventSlot3(FileListSlot3 slot) {
        if (getIndex() == 2) {
            refreshAdapter(slot.getList(), 3);
        }
    }
}
