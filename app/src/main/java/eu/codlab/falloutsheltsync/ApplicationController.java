package eu.codlab.falloutsheltsync;

import android.app.Application;
import android.content.SharedPreferences;

import com.crashlytics.android.Crashlytics;

import net.danlew.android.joda.JodaTimeAndroid;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.UUID;

import eu.codlab.crypto.core.utils.Base64Coder;
import eu.codlab.crypto.core.utils.PRNGFixes;
import eu.codlab.falloutsheltsync.webservice.models.Authent;
import io.fabric.sdk.android.Fabric;

/**
 * Created by kevinleperf on 22/08/15.
 */
public class ApplicationController extends Application {
    private final static String GUID = "GUID";
    private final static String PASS = "PASS";
    private static ApplicationController _instance;

    private SecureRandom _random = new SecureRandom();
    private SharedPreferences _shared_preferences;
    private String _guid = null;
    private String _pass = null;

    private HashMap<String, Boolean> _values = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        JodaTimeAndroid.init(this);
        PRNGFixes.apply();
        _instance = this;
        Fabric.with(this, new Crashlytics());
    }

    public static ApplicationController getInstance() {
        return _instance;
    }

    public String getGuid() {
        if (_guid == null) {
            _guid = getString(GUID, null);
            if (_guid == null) {
                _guid = UUID.randomUUID().toString();
                setString(GUID, _guid);
            }
        }
        return _guid;
    }

    private String getPass() {
        if (_pass == null) {
            _pass = getString(PASS, null);
            if (_pass == null) {
                _pass = Base64Coder.encodeString(new BigInteger(130, _random).toString(32));
                setString(PASS, _pass);
            }
        }
        return _pass;
    }

    public Authent getAuthenticationObject() {
        return new Authent(getGuid(), getPass());
    }

    private SharedPreferences getSharedPreferences() {
        if (_shared_preferences == null) {
            _shared_preferences = getSharedPreferences(getClass().getSimpleName(), 0);
        }
        return _shared_preferences;
    }

    public boolean getBoolean(String name, boolean default_value) {
        if (_values.get(name) == null) {
            boolean value = getSharedPreferences().getBoolean(name, default_value);
            _values.put(name, value);
            return value;
        } else {
            return _values.get(name);
        }
    }

    public void setBoolean(String name, boolean value) {
        getSharedPreferences().edit().putBoolean(name, value).commit();
        _values.put(name, value);
    }

    private String getString(String name, String default_value) {
        return getSharedPreferences().getString(name, default_value);
    }

    private void setString(String name, String value) {
        getSharedPreferences().edit().putString(name, value).commit();
    }

}
