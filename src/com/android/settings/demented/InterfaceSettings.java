/*
 * Copyright (C) 2013 DEMENTED Droid
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.demented;

import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.ListPreference;
import android.preference.TwoStatePreference;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.IWindowManager;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InterfaceSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "InterfaceSettings";

    private static final String KEY_NOTIFICATION_PULSE = "notification_pulse";
    private static final String KEY_BATTERY_LIGHT = "battery_light";
    private static final String KEY_LOCK_CLOCK = "lock_clock";
    private static final String KILL_APP_LONGPRESS_BACK = "kill_app_longpress_back";
    private static final String KEY_POWER_CRT_MODE = "system_power_crt_mode";
    private static final String KEY_POWER_CRT_SCREEN_OFF = "system_power_crt_screen_off";
    private static final String KEY_HARDWARE_KEYS = "hardware_keys";

    private PreferenceScreen mNotificationPulse;
    private PreferenceScreen mBatteryPulse;
    private CheckBoxPreference mKillAppLongpressBack;
    private ListPreference mCrtMode;
    private CheckBoxPreference mCrtOff;

    private boolean mIsCrtOffChecked = false;

    Context mContext;

    private final ArrayList<Preference> mAllPrefs = new ArrayList<Preference>();
    private final ArrayList<CheckBoxPreference> mResetCbPrefs
            = new ArrayList<CheckBoxPreference>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.interface_settings);

        // Only show the hardware keys config on a device that does not have a navbar
        // and the navigation bar config on phones that has a navigation bar
        boolean removeKeys = false;

        PreferenceScreen prefSet = getPreferenceScreen();

        IWindowManager windowManager = IWindowManager.Stub.asInterface(
                ServiceManager.getService(Context.WINDOW_SERVICE));
        try {
            if (windowManager.hasNavigationBar()) {
                removeKeys = true;
            }
        } catch (RemoteException e) {
            // Do nothing
        }

        if (removeKeys) {
            prefSet.removePreference(findPreference(KEY_HARDWARE_KEYS));
        }

        // Dont display the lock clock preference if its not installed
        removePreferenceIfPackageNotInstalled(findPreference(KEY_LOCK_CLOCK));

        // Notification lights
        mNotificationPulse = (PreferenceScreen) findPreference(KEY_NOTIFICATION_PULSE);
        if (mNotificationPulse != null) {
            if (!getResources().getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed)) {
                getPreferenceScreen().removePreference(mNotificationPulse);
            } else {
                updateLightPulseDescription();
            }
        }

        // Battery lights
        mBatteryPulse = (PreferenceScreen) findPreference(KEY_BATTERY_LIGHT);
        if (mBatteryPulse != null) {
            if (getResources().getBoolean(
                    com.android.internal.R.bool.config_intrusiveBatteryLed) == false) {
                getPreferenceScreen().removePreference(mBatteryPulse);
            } else {
                updateBatteryPulseDescription();
            }
        }

        mKillAppLongpressBack = findAndInitCheckboxPref(KILL_APP_LONGPRESS_BACK);

        // respect device default configuration
        // true fades while false animates
        boolean electronBeamFadesConfig = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_animateScreenLights);

        // use this to enable/disable crt on feature
        mIsCrtOffChecked = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.SYSTEM_POWER_ENABLE_CRT_OFF,
                electronBeamFadesConfig ? 0 : 1) == 1;

        mCrtOff = (CheckBoxPreference) findPreference(KEY_POWER_CRT_SCREEN_OFF);
        mCrtOff.setChecked(mIsCrtOffChecked);

        mCrtMode = (ListPreference) prefSet.findPreference(KEY_POWER_CRT_MODE);
        int crtMode = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.SYSTEM_POWER_CRT_MODE, 0);
        mCrtMode.setValue(String.valueOf(crtMode));
        mCrtMode.setSummary(mCrtMode.getEntry());
        mCrtMode.setOnPreferenceChangeListener(this);
    }

    private CheckBoxPreference findAndInitCheckboxPref(String key) {
        CheckBoxPreference pref = (CheckBoxPreference) findPreference(key);
        if (pref == null) {
            throw new IllegalArgumentException("Cannot find preference with key = " + key);
        }
        mAllPrefs.add(pref);
        mResetCbPrefs.add(pref);
        return pref;
    }

    @Override
    public void onResume() {
        super.onResume();

        // All users
        if (mNotificationPulse != null) {
            updateLightPulseDescription();
        }
        if (mBatteryPulse != null) {
            updateBatteryPulseDescription();
        }
        updateKillAppLongpressBackOptions();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void writeKillAppLongpressBackOptions() {
        Settings.Global.putInt(getActivity().getContentResolver(),
                Settings.Global.KILL_APP_LONGPRESS_BACK,
                mKillAppLongpressBack.isChecked() ? 1 : 0);
    }

    private void updateKillAppLongpressBackOptions() {
        mKillAppLongpressBack.setChecked(Settings.Global.getInt(
            getActivity().getContentResolver(), Settings.Global.KILL_APP_LONGPRESS_BACK, 0) != 0);
    }

    private boolean removePreferenceIfPackageNotInstalled(Preference preference) {
        String intentUri=((PreferenceScreen) preference).getIntent().toUri(1);
        Pattern pattern = Pattern.compile("component=([^/]+)/");
        Matcher matcher = pattern.matcher(intentUri);

        String packageName=matcher.find()?matcher.group(1):null;
        if(packageName != null) {
            try {
                getPackageManager().getPackageInfo(packageName, 0);
            } catch (NameNotFoundException e) {
                Log.e(TAG,"package "+packageName+" not installed, hiding preference.");
                getPreferenceScreen().removePreference(preference);
                return true;
            }
        }
        return false;
    }

    private void updateLightPulseDescription() {
        if (Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.NOTIFICATION_LIGHT_PULSE, 0) == 1) {
            mNotificationPulse.setSummary(getString(R.string.notification_light_enabled));
        } else {
            mNotificationPulse.setSummary(getString(R.string.notification_light_disabled));
         }
    }

    private void updateBatteryPulseDescription() {
        if (Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.BATTERY_LIGHT_ENABLED, 1) == 1) {
            mBatteryPulse.setSummary(getString(R.string.notification_light_enabled));
        } else {
            mBatteryPulse.setSummary(getString(R.string.notification_light_disabled));
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mKillAppLongpressBack) {
            writeKillAppLongpressBackOptions();
        } else if (preference == mCrtOff) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SYSTEM_POWER_ENABLE_CRT_OFF,
                    mCrtOff.isChecked() ? 1 : 0);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (preference == mCrtMode) {
            int crtMode = Integer.valueOf((String) objValue);
            int index = mCrtMode.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SYSTEM_POWER_CRT_MODE, crtMode);
            mCrtMode.setSummary(mCrtMode.getEntries()[index]);
            return true;
        }
        return true;
    }
}
