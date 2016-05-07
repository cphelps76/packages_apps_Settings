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
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.net.Uri;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
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

    private View mView;
    private ImageView mLogoView;
    private ViewGroup mViewGroup;

    private Activity mActivity;
    private Bundle mBundle;
    private static Context mContext;
    private ContentResolver mResolver;
    private Handler mHandler;
    private PreferenceScreen mPrefScreen;
    private SettingsObserver mObserver;

    private SwitchPreference mForceDefault;
    private Preference mGesture;
    private Preference mLockscreen;

    private boolean mHasChanged;
    private boolean mReset;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.demented_interface_settings);

        mActivity = getActivity();
        mBundle = savedInstanceState;
        mContext = getContext();
        mHandler = new Handler();
        mResolver = getContentResolver();

        mHasChanged = false;
        mReset = false;

        mGesture = findPreference(KEY_GESTURE_PREFS);

        mLockscreen = findPreference(KEY_LOCKSCREEN_PREFS);

        mForceDefault = (SwitchPreference) findPreference(KEY_HOME_FORCE_DEFAULT);
        mForceDefault.setChecked(Settings.System.getInt(mResolver,
                Settings.System.SET_DEFAULT_LAUNCHER, 0) != 0);

        removePreferences();
    }

    @Override
    public void onResume() {
        super.onResume();
        mObserver = new SettingsObserver(mContext, mHandler);
        mObserver.observe();
        if (mHasChanged) {
            reLoadView();
        }
    }

    private void reLoadView() {
        final View tmpView = mView;
        try {
            if (mView != null) {
                if (mViewGroup != null) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override 
                        public void run() {
                            mViewGroup.removeView(mView);
                            mView.requestLayout();
                            mView.forceLayout();
                            mViewGroup.addView(tmpView);
                            onCreate(mBundle);
                            setPreferenceScreen(mPrefScreen);
                        } 
                    });
                } 
            }
        } catch (Exception e) {
        }
        Settings.System.putBoolean(getContentResolver(),
                Settings.System.SYSTEM_PREF_STYLE_CHANGED, false);
        mReset = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, parent, savedInstanceState);
        mViewGroup = parent;
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mView = view;
        mLogoView = (ImageView) mView.findViewById(R.id.logo);
        updateView();
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

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String key = preference.getKey();
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        mPrefScreen = preferenceScreen;
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

    private final class SettingsObserver extends ContentObserver {
        private boolean mRegistered;

        private final Uri HAS_PREF_STYLE_CHANGED =
                Settings.System.getUriFor(Settings.System.SYSTEM_PREF_STYLE_CHANGED);

        public SettingsObserver(Context context, Handler handler) {
            super(handler);
        }

        public void observe() {
            if (mRegistered) {
                mResolver.unregisterContentObserver(this);
            }

            mResolver.registerContentObserver(HAS_PREF_STYLE_CHANGED, false, this);
            mRegistered = true;
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updateSettings();
        }

        private void updateSettings() {
            if (mReset) {
                mHasChanged = false;
                mReset = false;
            } else {
                mHasChanged = true;
            }
        }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DEMENTED_INTERFACE;
    }
}
