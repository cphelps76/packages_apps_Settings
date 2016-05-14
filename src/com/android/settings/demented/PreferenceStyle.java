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

package com.android.settings.demented;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.net.Uri;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.internal.logging.MetricsLogger;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import com.android.settings.DEMENTED;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.lang.CharSequence;

public class PreferenceStyle extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "PreferenceStyle";
    private static final String KEY_TITLE_STYLE = "title_style";
    private static final String KEY_TITLE_COLOR_PICKER = "prefs_title_text_color";
    private static final String KEY_TITLE_TEXT_STYLE = "prefs_title_text_style";

    private Activity mActivity;
    private static Context mContext;
    private ContentResolver mResolver;
    private Handler mHandler;
    private SettingsObserver mObserver;
    private PreferenceScreen mPrefScreen;

    private PreferenceCategory mTitleOptions;
    private ColorPickerPreference mTitleTextColor;
    private ListPreference mTitleTextStyle;
    private int mIntColor;
    private int mDefaultColor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        addPreferencesFromResource(R.xml.preference_style);

        mActivity = getActivity();
        mContext = getContext();
        mHandler = new Handler();
        mPrefScreen = getPreferenceScreen();
        mResolver = getContentResolver();

        mObserver = new SettingsObserver(mContext, mHandler);
        mObserver.observe();

        mTitleOptions = (PreferenceCategory) mPrefScreen.findPreference(KEY_TITLE_STYLE);

        mTitleTextColor = (ColorPickerPreference) mPrefScreen.findPreference(KEY_TITLE_COLOR_PICKER);
        mTitleTextColor.setOnPreferenceChangeListener(this);
        mIntColor = Settings.System.getInt(mResolver,
                    Settings.System.SYSTEM_PREF_TEXT_COLOR, -2);
        setTextColorSummary();

        mTitleTextStyle = (ListPreference) mPrefScreen.findPreference(KEY_TITLE_TEXT_STYLE);
        mTitleTextStyle.setOnPreferenceChangeListener(this);
        mTitleTextStyle.setValue(Integer.toString(Settings.System.getInt(mResolver,
                Settings.System.SYSTEM_PREF_TEXT_STYLE, 0)));
        mTitleTextStyle.setSummary(mTitleTextStyle.getEntry());

        Settings.System.putBoolean(mResolver,
                Settings.System.SYSTEM_PREF_STYLE_CHANGED, false);
    }

    private void setReset(int i) {
        Settings.System.putInt(mResolver,
                Settings.System.SYSTEM_PREF_RESET, i);
        if (i == 1) {
            reset();
        }
        setTextColorSummary();
    }

    private void setTextColorSummary() {
        mIntColor = Settings.System.getInt(mResolver,
                    Settings.System.SYSTEM_PREF_TEXT_COLOR, -2);
        if (inversionModeEnabled()) {
            mDefaultColor = getResources().getColor(
                    com.android.internal.R.color.primary_text_default_material_light);
        } else {
            mDefaultColor = getResources().getColor(
                    com.android.internal.R.color.primary_text_default_material_dark);
        }
        if (!isReset()) {
            String hexColor = String.format("#%08x", (0xffffffff & mIntColor));
            mTitleTextColor.setSummary(hexColor);
        } else {
            mIntColor = mDefaultColor;
            mTitleTextColor.setSummary(R.string.text_color_default);
        }
        mTitleTextColor.setNewPreviewColor(mIntColor);
    }

    private boolean inversionModeEnabled() {
        return Settings.Secure.getInt(mResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, 0) !=0;
    }

    private boolean isReset() {
        return Settings.System.getInt(mResolver,
                Settings.System.SYSTEM_PREF_RESET, 0) == 1;
    }

    private void reset() {
        Settings.System.putInt(mResolver,
                Settings.System.SYSTEM_PREF_TEXT_COLOR, -2);
        mTitleTextStyle.setValue(Integer.toString(Settings.System.getInt(mResolver,
                Settings.System.SYSTEM_PREF_TEXT_STYLE, 0)));
        Settings.System.putInt(mResolver,
                Settings.System.SYSTEM_PREF_TEXT_STYLE, 0);
        mTitleTextStyle.setValue(Integer.toString(Settings.System.getInt(mResolver,
                Settings.System.SYSTEM_PREF_TEXT_STYLE, 0)));
        mTitleTextStyle.setSummary(mTitleTextStyle.getEntry());
        setTextColorSummary();
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String key = preference.getKey();
        if (preference == mTitleTextColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mResolver,
                    Settings.System.SYSTEM_PREF_TEXT_COLOR, intHex);
            return true;
        } else if (preference == mTitleTextStyle) {
            int val = Integer.parseInt((String) newValue);
            int index = mTitleTextStyle.findIndexOfValue((String) newValue);
            Settings.System.putInt(mResolver,
                    Settings.System.SYSTEM_PREF_TEXT_STYLE, val);
            mTitleTextStyle.setSummary(mTitleTextStyle.getEntries()[index]);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        setReset(0);
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.pref_style, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_reset) {
            confirmStyleReset();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmStyleReset() {
        DialogInterface.OnClickListener onConfirmListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setReset((which == DialogInterface.BUTTON_POSITIVE) ? 1 : 0);
            }
        };

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.confirm_reset_style_title)
                .setMessage(R.string.confirm_reset_style_text)
                .setPositiveButton(R.string.reset_text, onConfirmListener)
                .setNegativeButton(android.R.string.cancel, onConfirmListener)
                .create()
                .show();
    }

    private final class SettingsObserver extends ContentObserver {
        private boolean mRegistered;

        private final Uri PREF_TEXT_COLOR =
                Settings.System.getUriFor(Settings.System.SYSTEM_PREF_TEXT_COLOR);
        private final Uri PREF_TEXT_STYLE =
                Settings.System.getUriFor(Settings.System.SYSTEM_PREF_TEXT_STYLE);
        private final Uri PREF_TEXT_RESET =
                Settings.System.getUriFor(Settings.System.SYSTEM_PREF_RESET);

        public SettingsObserver(Context context, Handler handler) {
            super(handler);
        }

        public void observe() {
            if (mRegistered) {
                mResolver.unregisterContentObserver(this);
            }
            mResolver.registerContentObserver(PREF_TEXT_COLOR, false, this);
            mResolver.registerContentObserver(PREF_TEXT_STYLE, false, this);
            mResolver.registerContentObserver(PREF_TEXT_RESET, false, this);
            mRegistered = true;
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updateSettings();
        }

        private void updateSettings() {
            Settings.System.putBoolean(mResolver,
                    Settings.System.SYSTEM_PREF_STYLE_CHANGED, true);
            reLoadFragment();
        }
    }

    private void reLoadFragment() {
        try {
            FragmentManager manager = getFragmentManager();
            Fragment currentFragment = manager.findFragmentById(R.id.main_content);
            Log.i(TAG, "Current Fragment is : " + currentFragment);
            FragmentTransaction fragmentTransaction = manager.beginTransaction();
            fragmentTransaction.detach(currentFragment);
            fragmentTransaction.attach(currentFragment);
            fragmentTransaction.commit();
            manager.executePendingTransactions();
        } catch (Exception e) {
        }
        setTextColorSummary();
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DEMENTED_INTERFACE;
    }
}
