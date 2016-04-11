/*
 * Copyright (C) 2016 DEMENTED
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

package com.android.settings;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.net.Uri;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class DEMENTED extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, View.OnClickListener {

    private static final String TAG = "DEMENTED";
    private static final String KEY_DEMENTED_GITHUB = "https://github.com/cphelps76";
    private static final String KEY_GESTURE_PREFS = "prefs_gesture";
    private static final String KEY_BUTTON_PREFS = "prefs_buttons";
    private static final String KEY_HOME_FORCE_DEFAULT = "force_default_launcher";
    private static final String KEY_HOME_PREFS = "prefs_home";
    private static final String KEY_LOCKSCREEN_PREFS = "prefs_lock_screen";
    private static final String SETTINGS_APP = "com.android.settings";
    private static final String LOCK_SETTINGS =
            "com.android.settings.Settings$LockScreenSettingsActivity";

    private SwitchPreference mForceDefault;
    private ImageView mLogoView;
    private Preference mGesture;
    private Preference mLockscreen;

    private static Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.demented_interface_settings);

        mContext = getActivity();

        mGesture = findPreference(KEY_GESTURE_PREFS);

        mLockscreen = findPreference(KEY_LOCKSCREEN_PREFS);

        mForceDefault = (SwitchPreference) findPreference(KEY_HOME_FORCE_DEFAULT);
        mForceDefault.setChecked(Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.SET_DEFAULT_LAUNCHER, 0) != 0);

        removePreferences();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.preference_list_fragment, parent, false);
        mLogoView = (ImageView)rootView.findViewById(R.id.logo);
        updateView();
        return rootView;
    }

    private boolean hasButtons() {
        return getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys) != 0;
    }

    private void removePreference(Preference preference) {
        getPreferenceScreen().removePreference(preference);
    }

    private boolean gesturePrefAvailable() {
        return Utils.updatePreferenceToSpecificActivityFromMetaDataOrRemove(mContext,
                getPreferenceScreen(), KEY_GESTURE_PREFS);
    }

    private void removePreferences() {
        Utils.updatePreferenceToSpecificActivityFromMetaDataOrRemove(mContext,
            getPreferenceScreen(), KEY_GESTURE_PREFS);
        if (gesturePrefAvailable()) {
            mGesture.setSummary(R.string.gesture_settings_summary);
        }
        if (!hasButtons()) {
            removePreference(KEY_BUTTON_PREFS);
        }
        if (mForceDefault.isChecked()) {
            removePreference(KEY_HOME_PREFS);
        }
    }

    private void updateView() {
        if (mLogoView != null) {
            mLogoView.setClickable(true);
            mLogoView.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mLogoView) {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            launchUrl(KEY_DEMENTED_GITHUB);
        }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DEMENTED_INTERFACE;
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mForceDefault) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SET_DEFAULT_LAUNCHER,
                    mForceDefault.isChecked() ? 1 : 0);
            makePrefered();
            if (!mForceDefault.isChecked()) {
                Toast.makeText(mContext, getString(com.android.internal.R.string.default_launcher_unset),
                    Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(mContext, getString(com.android.internal.R.string.default_launcher_set),
                    Toast.LENGTH_LONG).show();
            }
        } else if (preference == mLockscreen) {
            launchLockSettings();
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return false;
    }

    private void launchLockSettings() {
        Intent lockSettings = new Intent();
        lockSettings.setComponent(new ComponentName(SETTINGS_APP, LOCK_SETTINGS));
        mContext.startActivity(lockSettings);
    }

    private void launchUrl(String url) {
        Uri uriUrl = Uri.parse(url);
        Intent demented = new Intent(Intent.ACTION_VIEW, uriUrl);
        mContext.startActivity(demented);
    }

    private static void makePrefered() {
       PackageManager pM = mContext.getPackageManager();
       ComponentName cN = new ComponentName(mContext, NoClass.class);
       pM.setComponentEnabledSetting(cN, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

       Intent intent = new Intent(Intent.ACTION_MAIN);
       intent.addCategory(Intent.CATEGORY_HOME);
       intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
       mContext.startActivity(intent);

       pM.setComponentEnabledSetting(cN, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }
}
