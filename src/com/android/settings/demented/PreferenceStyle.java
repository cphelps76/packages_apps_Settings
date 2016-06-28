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
import android.graphics.Typeface;
import android.os.Bundle;
import android.net.Uri;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;
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
import java.lang.Integer;

public class PreferenceStyle extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "PreferenceStyle";
    private static final String KEY_ENABLE_STYLES = "enable_custom_text";
    private static final String KEY_TITLE_STYLE = "title_style";
    private static final String KEY_TITLE_COLOR_PICKER = "prefs_title_text_color";
    private static final String KEY_TITLE_TEXT_STYLE = "prefs_title_text_style";

    private Activity mActivity;
    private static Context mContext;
    private ContentResolver mResolver;
    private PreferenceScreen mPrefScreen;

    private SwitchPreference mEnableOptions;
    private ColorPickerPreference mTitleTextColor;
    private ListPreference mTitleTextStyle;
    private int mIntColor;
    private int mDefault = Integer.MIN_VALUE;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        addPreferencesFromResource(R.xml.preference_style);

        mActivity = getActivity();
        mContext = getContext();
        mPrefScreen = getPreferenceScreen();
        mResolver = getContentResolver();

        mEnableOptions = (SwitchPreference) mPrefScreen.findPreference(KEY_ENABLE_STYLES);
        mEnableOptions.setTitle(R.string.title_style_label);
        mEnableOptions.setChecked(Settings.System.getInt(mResolver,
                Settings.System.CUSTOM_TEXT_STYLE_ENABLED, 0) != 0);

        mTitleTextColor = (ColorPickerPreference) mPrefScreen.findPreference(KEY_TITLE_COLOR_PICKER);
        mTitleTextColor.setOnPreferenceChangeListener(this);

        mIntColor = Settings.System.getInt(mResolver,
                    Settings.System.SYSTEM_PREF_TEXT_COLOR, -2);

        mTitleTextStyle = (ListPreference) mPrefScreen.findPreference(KEY_TITLE_TEXT_STYLE);
        mTitleTextStyle.setOnPreferenceChangeListener(this);
        mTitleTextStyle.setValue(Integer.toString(Settings.System.getInt(mResolver,
                Settings.System.SYSTEM_PREF_TEXT_STYLE, 0)));

        Settings.System.putBoolean(mResolver,
                Settings.System.SYSTEM_PREF_STYLE_CHANGED, false);

        setSummaries();
    }

    private void setSummaries() {
        setEnableOptionsSummary();
        setTextColorSummary();
        setTextStyleSummary();
    }

    private void setEnableOptionsSummary() {
        boolean enabled = Settings.System.getInt(mResolver,
                Settings.System.CUSTOM_TEXT_STYLE_ENABLED, 0) == 1;
        if (!enabled) {
            mEnableOptions.setSummary(R.string.custom_text_styles_disabled);
        } else {
            mEnableOptions.setSummary(R.string.custom_text_styles_enabled);
        }
    }

    private void setTextColorSummary() {
        int defaultColor = -1;
        mIntColor = Settings.System.getInt(mResolver,
                    Settings.System.SYSTEM_PREF_TEXT_COLOR, -2);
        boolean enabled = Settings.System.getInt(mResolver,
                Settings.System.CUSTOM_TEXT_STYLE_ENABLED, 0) == 1;
        boolean isReset = Settings.System.getInt(mResolver,
                Settings.System.SYSTEM_PREF_RESET, 0) == 1;
        Log.i(TAG, "enabled is : " + enabled);
        Log.i(TAG, "isReset is : " + isReset);
        if (enabled) {
            if (!isReset) {
                if (mIntColor != mDefault) {
                    String hexColor = String.format("#%08x", (0xffffffff & mIntColor));
                    mTitleTextColor.setSummary(hexColor);
                    mTitleTextColor.setNewPreviewColor(mIntColor);
                    return;
                }
            } else {
                mTitleTextColor.setSummary(R.string.text_color_default);
                mTitleTextColor.setNewPreviewColor(defaultColor);
                return;
            }
        } else {
            mTitleTextColor.setSummary(R.string.text_color_default);
            mTitleTextColor.setNewPreviewColor(defaultColor);
        }
    }

    private void setTextStyleSummary() {
        int currentFontStyle = Settings.System.getInt(mResolver,
                Settings.System.SYSTEM_PREF_TEXT_STYLE, 0);
        boolean enabled = Settings.System.getInt(mResolver,
                Settings.System.CUSTOM_TEXT_STYLE_ENABLED, 0) == 1;
        if (enabled) {
            if (currentFontStyle == 0) {
                mTitleTextStyle.setSummary(R.string.pref_text_style_normal);
            } else if (currentFontStyle == 1) {
                mTitleTextStyle.setSummary(R.string.pref_text_style_bold);
            } else if (currentFontStyle == 2) {
                mTitleTextStyle.setSummary(R.string.pref_text_style_italic);
            } else if (currentFontStyle == 3) {
                mTitleTextStyle.setSummary(R.string.pref_text_style_bold_italic);
            }
        } else {
            mTitleTextStyle.setSummary(R.string.pref_text_style_normal);
        }
    }

    private void setReset(int i) {
        Settings.System.putInt(mResolver,
                Settings.System.SYSTEM_PREF_RESET, i);
        if (i == 1) {
            reset();
        }
    }

    private void reset() {
        Settings.System.putInt(mResolver,
                Settings.System.CUSTOM_TEXT_STYLE_ENABLED, 0);
        mEnableOptions.setChecked(Settings.System.getInt(mResolver,
                Settings.System.CUSTOM_TEXT_STYLE_ENABLED, 0) != 0);
        Settings.System.putInt(mResolver,
                Settings.System.SYSTEM_PREF_TEXT_COLOR, mDefault);
        Settings.System.putInt(mResolver,
                Settings.System.SYSTEM_PREF_TEXT_STYLE, 0);
        mTitleTextStyle.setValue(Integer.toString(Settings.System.getInt(mResolver,
                Settings.System.SYSTEM_PREF_TEXT_STYLE, 0)));
        reloadFragment();
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean enabled = Settings.System.getInt(mResolver,
                Settings.System.CUSTOM_TEXT_STYLE_ENABLED, 0) == 1;
        if (preference == mTitleTextColor) {
            if (enabled) {
                String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                        .valueOf(newValue)));
                int intHex = ColorPickerPreference.convertToColorInt(hex);
                final int currentHex = Settings.System.getInt(mResolver,
                        Settings.System.SYSTEM_PREF_TEXT_COLOR, -2);
                if (intHex != currentHex) {
                    Settings.System.putInt(mResolver,
                            Settings.System.SYSTEM_PREF_TEXT_COLOR, intHex);
                    reloadFragment();
                }
            }
            return true;
        } else if (preference == mTitleTextStyle) {
            final int currentStyle = Settings.System.getInt(mResolver,
                    Settings.System.SYSTEM_PREF_TEXT_STYLE, 0);
            int val = Integer.parseInt((String) newValue);
            int index = mTitleTextStyle.findIndexOfValue((String) newValue);
            if (enabled && currentStyle != val) {
                Settings.System.putInt(mResolver,
                        Settings.System.SYSTEM_PREF_TEXT_STYLE, val);
                reloadFragment();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mEnableOptions) {
            Settings.System.putInt(resolver, Settings.System.CUSTOM_TEXT_STYLE_ENABLED,
                    mEnableOptions.isChecked() ? 1 : 0);
            reloadFragment();
        } else if (preference == mTitleTextColor) {
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

    private void reloadFragment() {
        Settings.System.putBoolean(mResolver,
                Settings.System.SYSTEM_PREF_STYLE_CHANGED, true);
        try {
            FragmentManager manager = getFragmentManager();
            Fragment currentFragment = manager.findFragmentById(R.id.main_content);
            FragmentTransaction fragmentTransaction = manager.beginTransaction();
            fragmentTransaction.detach(currentFragment);
            fragmentTransaction.attach(currentFragment);
            fragmentTransaction.commit();
            manager.executePendingTransactions();
            setPreferenceScreen(getPreferenceScreen());
        } catch (Exception e) {
            Log.e(TAG, "WTF......" + e);
        }
        setSummaries();
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DEMENTED_INTERFACE;
    }
}
