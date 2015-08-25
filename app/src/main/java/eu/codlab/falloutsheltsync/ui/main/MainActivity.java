package eu.codlab.falloutsheltsync.ui.main;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import eu.codlab.falloutsheltsync.R;
import eu.codlab.falloutsheltsync.event.SyncState;
import eu.codlab.falloutsheltsync.sync.SyncService;
import eu.codlab.falloutsheltsync.ui.views.SlidingTabLayout;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final int RESOLVE_CONNECTION_REQUEST_CODE = 29760928;

    private GoogleApiClient mGoogleApiClient;
    private SyncService mService;

    @Bind(R.id.toolbar)
    public Toolbar _toolbar;

    @Bind(R.id.pager)
    public ViewPager _pager;

    @Bind(R.id.tabs)
    public SlidingTabLayout _tabs;

    @Bind(R.id.refresh_layout)
    public SwipeRefreshLayout _refresh_layout;

    private ServiceConnection mServiceConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((SyncService.myBinder) service).getService();
            mService.setGoogleApiClient(mGoogleApiClient);
        }

        public void onServiceDisconnected(ComponentName name) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= 21)
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));

        ButterKnife.bind(this);
        setSupportActionBar(_toolbar);

        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        _pager.setAdapter(adapter);
        _tabs.setDistributeEvenly(true);

        _pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float v, int i1) {
            }

            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                _refresh_layout.setEnabled(state == ViewPager.SCROLL_STATE_IDLE);
            }
        });

        _tabs.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return getResources().getColor(R.color.tabsScrollColor);
            }
        });

        _tabs.setViewPager(_pager);

        _refresh_layout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (!(mGoogleApiClient.isConnected() || mGoogleApiClient.isConnecting())) {
                    mGoogleApiClient.connect();
                    _refresh_layout.setRefreshing(false);
                } else {
                    mService.listFiles();
                }
            }
        });

        if (SyncService.getInstance() != null)
            mGoogleApiClient = SyncService.getInstance().getClient();

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }

        /*try {
            CypherAES cypher = new CypherAES("tu89geji340t89u2",
                    intArrayToByte(new int[]{0xa7, 0xca, 0x9f, 0x33, 0x66, 0xd8, 0x92, 0xc2, 0xf0, 0xbe, 0xf4, 0x17, 0x34, 0x1c,
                            0xa9, 0x71, 0xb6, 0x9a, 0xe9, 0xf7, 0xba, 0xcc, 0xcf, 0xfc, 0xf4, 0x3c, 0x62, 0xd1, 0xd7,
                            0xd0, 0x21, 0xf9}));
            byte[] bytes = Base64Coder.decode(content.trim());
            String result = new String(cypher.decrypt(bytes));
            Log.d("MainActivity", result + "");
        } catch (Exception e) {
            e.printStackTrace();
        }*/


    }

    @Override
    public void onResume() {
        super.onResume();

        Intent intent = new Intent(this, SyncService.class);
        startService(intent);

        bindService(new Intent(this, SyncService.class), mServiceConn, Service.BIND_AUTO_CREATE);

        mGoogleApiClient.connect();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        unbindService(mServiceConn);

        super.onPause();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                // Unable to resolve, message user appropriately
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, 0).show();
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case RESOLVE_CONNECTION_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    mGoogleApiClient.connect();
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.update:
                openWebsite("http://fallout.codlab.eu");
                return true;
            case R.id.codlab:
                openWebsite("http://codlab.eu");
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(Bundle bundle) {
        SyncService.getInstance().setGoogleApiClient(mGoogleApiClient);
        SyncService.getInstance().init();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, "onConnectionSuspended " + i, Toast.LENGTH_SHORT).show();
    }

    @Subscribe(threadMode = ThreadMode.MainThread, sticky = true)
    public void onEventSyncState(SyncState state) {
        _refresh_layout.setRefreshing(state == null || state.getSyncState() == null ? true : false);
    }

    private void openWebsite(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

}
