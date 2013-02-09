/*
 * Copyright (C) 2012 CyanogenMod
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

package com.android.settings.cyanogenmod;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.view.IWindowManager;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.text.Spannable;
import android.util.Log;
import android.view.IWindowManager;
import android.view.Display;
import android.view.LayoutInflater;
import android.widget.EditText;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SystemSettings extends SettingsPreferenceFragment implements
    Preference.OnPreferenceChangeListener{

    private static final String TAG = "SystemSettings";

    private static final String KEY_POWER_BUTTON_TORCH = "power_button_torch";

    private CheckBoxPreference mPowerButtonTorch;
    private static final String KEY_CHRONUS = "chronus";
    private static final String PREF_FORCE_DUAL_PANEL = "force_dualpanel";
    private static final String PREF_CUSTOM_CARRIER_LABEL = "custom_carrier_label";

    Preference mCustomLabel;

    Context mContext;

    String mCustomLabelText = null;

    CheckBoxPreference mDualpane;
    private boolean torchSupported() {
        return getResources().getBoolean(R.bool.has_led_flash);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.system_settings);

        mCustomLabel = findPreference(PREF_CUSTOM_CARRIER_LABEL);
        updateCustomLabelTextSummary();

        // Dont display the lock clock preference if its not installed
        removePreferenceIfPackageNotInstalled(findPreference(KEY_CHRONUS));

        mPowerButtonTorch = (CheckBoxPreference) findPreference(KEY_POWER_BUTTON_TORCH);
        if (torchSupported()) {
            mPowerButtonTorch.setChecked((Settings.System.getInt(getActivity().
                    getApplicationContext().getContentResolver(),
                    Settings.System.POWER_BUTTON_TORCH, 0) == 1));
        } else {
            getPreferenceScreen().removePreference(mPowerButtonTorch);
        }

        mDualpane = (CheckBoxPreference) findPreference(PREF_FORCE_DUAL_PANEL);
            mDualpane.setOnPreferenceChangeListener(this);

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mPowerButtonTorch) {
            boolean enabled = mPowerButtonTorch.isChecked();
            Settings.System.putInt(getContentResolver(), Settings.System.POWER_BUTTON_TORCH,
                    enabled ? 1 : 0);
            return true;
        } else if (preference == mCustomLabel) {
            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

            alert.setTitle(R.string.custom_carrier_label_title);
            alert.setMessage(R.string.custom_carrier_label_explain);

            // Set an EditText view to get user input
            final EditText input = new EditText(getActivity());
            input.setText(mCustomLabelText != null ? mCustomLabelText : "");

            alert.setView(input);
            alert.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
 
                public void onClick(DialogInterface dialog, int whichButton) {
                    String value = ((Spannable) input.getText()).toString();
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.CUSTOM_CARRIER_LABEL, value);
                    updateCustomLabelTextSummary();
                    Intent i = new Intent();
                    i.setAction("com.android.settings.LABEL_CHANGED");
                    getActivity().sendBroadcast(i);
                }
            });

            alert.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Canceled.
                }
            });

            alert.show();   
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver cr = getActivity().getContentResolver();

        if (preference == mDualpane) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.FORCE_DUAL_PANEL,
                    ((CheckBoxPreference)preference).isChecked() ? 0 : 1);
            return true;
        }
        return false;
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

    private void updateCustomLabelTextSummary() {
        mCustomLabelText = Settings.System.getString(getActivity().getContentResolver(),
                Settings.System.CUSTOM_CARRIER_LABEL);
        if (mCustomLabelText == null || mCustomLabelText.length() == 0) {
            mCustomLabel.setSummary(R.string.custom_carrier_label_notset);
        } else {
            mCustomLabel.setSummary(mCustomLabelText);
        }
    }   

}
