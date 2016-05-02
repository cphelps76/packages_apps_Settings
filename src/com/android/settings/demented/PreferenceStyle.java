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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

    private View mView;
    private ViewGroup mViewGroup;
    private int mLayoutResId = R.layout.preference_list_fragment;

    private Activity mActivity;
    private static Context mContext;
    private ContentResolver mResolver;
    private Handler mHandler;
    private SettingsObserver mObserver;
    private PreferenceScreen mPrefScreen;
    private boolean mReset;
    public static boolean mChanged;

    private PreferenceCategory mTitleOptions;
    private ColorPickerPreference mTitleTextColor;
    private String mHexColor;
    private ListPreference mTitleTextStyle;
    private boolean mUiMode;
    private int mIntColor;
    private int mDefaultColor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        addPreferencesFromResource(R.xml.preference_style);
        mPrefScreen = getPreferenceScreen();

        if (mUiMode) {
            mDefaultColor = getResources().getColor(
                    com.android.internal.R.color.primary_text_default_material_light);
        } else {
            mDefaultColor = getResources().getColor(
                    com.android.internal.R.color.primary_text_default_material_dark);
        }

        mActivity = getActivity();
        mContext = getContext();
        mResolver = getContentResolver();
        mHandler = new Handler();

        mObserver = new SettingsObserver(mContext, mHandler);
        mObserver.observe();

        mUiMode = Settings.System.getInt(mResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, 0) == 1;

        mReset = Settings.System.getInt(mResolver,
                Settings.System.SYSTEM_PREF_RESET, 0) == 1;

        mTitleOptions = (PreferenceCategory) mPrefScreen.findPreference(KEY_TITLE_STYLE);

        mTitleTextColor = (ColorPickerPreference) mPrefScreen.findPreference(KEY_TITLE_COLOR_PICKER);
        mTitleTextColor.setOnPreferenceChangeListener(this);
        mIntColor = Settings.System.getInt(mResolver,
                    Settings.System.SYSTEM_PREF_TEXT_COLOR, -2);
        if (mReset) {
            mTitleTextColor.setSummary(getResources().getString(R.string.text_color_default));
        } else {
            mHexColor = String.format("#%08x", (0xffffffff & mIntColor));
            mTitleTextColor.setSummary(mHexColor);
        }
        mTitleTextColor.setNewPreviewColor(mIntColor);

        mTitleTextStyle = (ListPreference) mPrefScreen.findPreference(KEY_TITLE_TEXT_STYLE);
        mTitleTextStyle.setOnPreferenceChangeListener(this);
        mTitleTextStyle.setValue(Integer.toString(Settings.System.getInt(mContext
                .getContentResolver(), Settings.System.SYSTEM_PREF_TEXT_STYLE,
                0)));
        mTitleTextStyle.setSummary(mTitleTextStyle.getEntry());

        Settings.System.putBoolean(getContentResolver(),
                Settings.System.SYSTEM_PREF_STYLE_CHANGED, false);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View rootView = inflater.inflate(mLayoutResId, parent, false);
        mViewGroup = parent;
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mView = view;
    }

    private void setReset(int i) {
        if (i == 1) {
            Settings.System.putInt(mResolver,
                    Settings.System.SYSTEM_PREF_RESET, i);
            reset();
        } else {
            Settings.System.putInt(mResolver,
                    Settings.System.SYSTEM_PREF_RESET, i);
        }
    }

    private void reset() {
        Settings.System.putInt(mResolver,
                Settings.System.SYSTEM_PREF_TEXT_COLOR, mDefaultColor);
        mTitleTextColor.setSummary(getResources().getString(R.string.text_color_default));
        mTitleTextColor.setNewPreviewColor(mDefaultColor);
        mTitleTextStyle.setValue(Integer.toString(Settings.System.getInt(mResolver,
                Settings.System.SYSTEM_PREF_TEXT_STYLE, 0)));
        Settings.System.putInt(mResolver,
                Settings.System.SYSTEM_PREF_TEXT_STYLE, 0);
        mTitleTextStyle.setSummary(mTitleTextStyle.getEntry());
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String key = preference.getKey();
        if (preference == mTitleTextColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mHexColor = hex;
            mTitleTextColor.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
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
        if (preference == mTitleTextColor) {
            setReset(0);
        } else if (preference == mTitleTextStyle) {
            setReset(0);
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
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
            Settings.System.putBoolean(getContentResolver(),
                    Settings.System.SYSTEM_PREF_STYLE_CHANGED, true);
            reLoadView();
        }
    }

    private void reLoadView() {
        final View tmpView = mView;
        try {
            setReset(0);
            if (mView != null) {
                if (mViewGroup != null) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override 
                        public void run() {
                            mViewGroup.removeView(mView);
                            mView.requestLayout();
                            mView.forceLayout();
                            mViewGroup.addView(tmpView);
                            onCreate(new Bundle());
                        } 
                    });
                }
            }
        } catch (Exception e) {
        }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DEMENTED_INTERFACE;
    }
}
