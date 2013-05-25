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
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.view.IWindowManager;
import android.provider.Settings;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.TwoStatePreference;
import android.provider.Settings;
import android.text.Spannable;
import android.util.Log;
import android.util.TypedValue;
import android.view.IWindowManager;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.aokp.widgets.AlphaSeekBar;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.util.Helpers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SystemSettings extends SettingsPreferenceFragment implements
    Preference.OnPreferenceChangeListener{

    private static final String TAG = "SystemSettings";

    private static final String KEY_POWER_BUTTON_TORCH = "power_button_torch";

    private CheckBoxPreference mPowerButtonTorch;
    private static final String KEY_LOCK_CLOCK = "lock_clock";
    private static final String KEY_PIE_CONTROL = "pie_control";
    private static final String PREF_FORCE_DUAL_PANEL = "force_dualpanel";
    private static final String PREF_CUSTOM_CARRIER_LABEL = "custom_carrier_label";
    private static final String PREF_SHOW_OVERFLOW = "show_overflow";
    private static final String PREF_NOTIFICATION_SHOW_WIFI_SSID = "notification_show_wifi_ssid";
    private static final String PREF_LOW_BATTERY_WARNING_POLICY = "pref_low_battery_warning_policy";
    private static final String PREF_POWER_CRT_MODE = "system_power_crt_mode";
    private static final String PREF_POWER_CRT_SCREEN_OFF = "system_power_crt_screen_off";

    private PreferenceScreen mPieControl;
    private ListPreference mLowBatteryWarning;
    private static ContentResolver mContentResolver;

    CheckBoxPreference mShowActionOverflow;
    Preference mCustomLabel;
    ListPreference mCrtMode;
    CheckBoxPreference mCrtOff;

    Context mContext;

    String mCustomLabelText = null;

    CheckBoxPreference mShowWifiName;
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
        removePreferenceIfPackageNotInstalled(findPreference(KEY_LOCK_CLOCK));

        // Pie controls
        mPieControl = (PreferenceScreen) findPreference(KEY_PIE_CONTROL);

        mPowerButtonTorch = (CheckBoxPreference) findPreference(KEY_POWER_BUTTON_TORCH);
        if (torchSupported()) {
            mPowerButtonTorch.setChecked((Settings.System.getInt(getActivity().
                    getApplicationContext().getContentResolver(),
                    Settings.System.POWER_BUTTON_TORCH, 0) == 1));
        } else {
            getPreferenceScreen().removePreference(mPowerButtonTorch);
        }

        boolean isCrtOffChecked = (Settings.System.getBoolean(mContentResolver,
                        Settings.System.SYSTEM_POWER_ENABLE_CRT_OFF, true));
        mCrtOff = (CheckBoxPreference) findPreference(PREF_POWER_CRT_SCREEN_OFF);
        mCrtOff.setChecked(isCrtOffChecked);

        mCrtMode = (ListPreference) findPreference(PREF_POWER_CRT_MODE);
        int crtMode = Settings.System.getInt(mContentResolver,
                Settings.System.SYSTEM_POWER_CRT_MODE, 0);
        mCrtMode.setValue(Integer.toString(Settings.System.getInt(mContentResolver,
                Settings.System.SYSTEM_POWER_CRT_MODE, crtMode)));
        mCrtMode.setOnPreferenceChangeListener(this);

        mShowWifiName = (CheckBoxPreference) findPreference(PREF_NOTIFICATION_SHOW_WIFI_SSID);
            mShowWifiName.setOnPreferenceChangeListener(this);

        mShowActionOverflow = (CheckBoxPreference) findPreference(PREF_SHOW_OVERFLOW);
        mShowActionOverflow.setChecked(Settings.System.getBoolean(getActivity().
                        getApplicationContext().getContentResolver(),
                        Settings.System.UI_FORCE_OVERFLOW_BUTTON, false));

        mLowBatteryWarning = (ListPreference) findPreference(PREF_LOW_BATTERY_WARNING_POLICY);
        int lowBatteryWarning = Settings.System.getInt(getActivity().getContentResolver(),
                                    Settings.System.POWER_UI_LOW_BATTERY_WARNING_POLICY, 3);
        mLowBatteryWarning.setValue(String.valueOf(lowBatteryWarning));
        mLowBatteryWarning.setSummary(mLowBatteryWarning.getEntry());
        mLowBatteryWarning.setOnPreferenceChangeListener(this);

        mDualpane = (CheckBoxPreference) findPreference(PREF_FORCE_DUAL_PANEL);
            mDualpane.setOnPreferenceChangeListener(this);

    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPieControl != null) {
            updatePieControlDescription();
        }
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
        } else if (preference == mShowActionOverflow) {
            boolean enabled = mShowActionOverflow.isChecked();
            Settings.System.putBoolean(getContentResolver(), Settings.System.UI_FORCE_OVERFLOW_BUTTON,
                    enabled ? true : false);
            // Show toast appropriately
            if (enabled) {
                Toast.makeText(getActivity(), R.string.show_overflow_toast_enable,
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getActivity(), R.string.show_overflow_toast_disable,
                        Toast.LENGTH_LONG).show();
            }
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
        } else if (preference == mCrtOff) {
            Settings.System.putBoolean(mContentResolver,
                    Settings.System.SYSTEM_POWER_ENABLE_CRT_OFF,
                    ((TwoStatePreference) preference).isChecked());
            return true;
        } else if (preference.getKey().equals("transparency_dialog")) {
            // getFragmentManager().beginTransaction().add(new
            // TransparencyDialog(), null).commit();
            openTransparencyDialog();
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
        } else if (preference == mLowBatteryWarning) {
            int lowBatteryWarning = Integer.valueOf((String) objValue);
            int index = mLowBatteryWarning.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.POWER_UI_LOW_BATTERY_WARNING_POLICY, lowBatteryWarning);
            mLowBatteryWarning.setSummary(mLowBatteryWarning.getEntries()[index]);
            return true;
         } else if (preference == mShowWifiName) {
            Settings.System.putInt(getActivity().getContentResolver(), Settings.System.NOTIFICATION_SHOW_WIFI_SSID,
                    ((CheckBoxPreference)preference).isChecked() ? 0 : 1);
            return true;
         } else if (preference == mCrtMode) {
            int crtMode = Integer.valueOf((String) objValue);
            int index = mCrtMode.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SYSTEM_POWER_CRT_MODE, crtMode);
            mCrtMode.setSummary(mCrtMode.getEntries()[index]);
            return true;
        }
        return false;
    }

    private void updatePieControlDescription() {
        if (Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.PIE_CONTROLS, 0) == 1) {
            mPieControl.setSummary(getString(R.string.pie_control_enabled));
        } else {
            mPieControl.setSummary(getString(R.string.pie_control_disabled));
        }
    }

    private void openTransparencyDialog() {
        getFragmentManager().beginTransaction().add(new AdvancedTransparencyDialog(), null)
                .commit();
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

    public static class AdvancedTransparencyDialog extends DialogFragment {

        private static final int KEYGUARD_ALPHA = 112;

        private static final int STATUSBAR_ALPHA = 0;
        private static final int STATUSBAR_KG_ALPHA = 1;
        private static final int NAVBAR_ALPHA = 2;
        private static final int NAVBAR_KG_ALPHA = 3;
        private static final int LOCKSCREEN_ALPHA = 4;

        boolean linkTransparencies = true;
        CheckBox mLinkCheckBox, mMatchStatusbarKeyguard, mMatchNavbarKeyguard;
        ViewGroup mNavigationBarGroup;

        TextView mSbLabel;

        AlphaSeekBar mSeekBars[] = new AlphaSeekBar[5];

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setShowsDialog(true);
            setRetainInstance(true);
            linkTransparencies = getSavedLinkedState();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            View layout = View.inflate(getActivity(), R.layout.dialog_transparency, null);
            mLinkCheckBox = (CheckBox) layout.findViewById(R.id.transparency_linked);
            mLinkCheckBox.setChecked(linkTransparencies);

            mNavigationBarGroup = (ViewGroup) layout.findViewById(R.id.navbar_layout);
            mSbLabel = (TextView) layout.findViewById(R.id.statusbar_label);
            mSeekBars[STATUSBAR_ALPHA] = (AlphaSeekBar) layout.findViewById(R.id.statusbar_alpha);
            mSeekBars[STATUSBAR_KG_ALPHA] = (AlphaSeekBar) layout
                    .findViewById(R.id.statusbar_keyguard_alpha);
            mSeekBars[NAVBAR_ALPHA] = (AlphaSeekBar) layout.findViewById(R.id.navbar_alpha);
            mSeekBars[NAVBAR_KG_ALPHA] = (AlphaSeekBar) layout
                    .findViewById(R.id.navbar_keyguard_alpha);

            mMatchStatusbarKeyguard = (CheckBox) layout.findViewById(R.id.statusbar_match_keyguard);
            mMatchNavbarKeyguard = (CheckBox) layout.findViewById(R.id.navbar_match_keyguard);
            mSeekBars[LOCKSCREEN_ALPHA] = (AlphaSeekBar) layout.findViewById(R.id.lockscreen_alpha);

            try {
                // restore any saved settings
                int alphas[] = new int[2];
                ContentResolver resolver = getActivity().getContentResolver();
                int lockscreen_alpha = Settings.System.getInt(resolver,
                        Settings.System.LOCKSCREEN_ALPHA_CONFIG, KEYGUARD_ALPHA);
                mSeekBars[LOCKSCREEN_ALPHA].setCurrentAlpha(lockscreen_alpha);
                final String sbConfig = Settings.System.getString(getActivity()
                        .getContentResolver(),
                        Settings.System.STATUS_BAR_ALPHA_CONFIG);
                if (sbConfig != null) {
                    String split[] = sbConfig.split(";");
                    alphas[0] = Integer.parseInt(split[0]);
                    alphas[1] = Integer.parseInt(split[1]);

                    mSeekBars[STATUSBAR_ALPHA].setCurrentAlpha(alphas[0]);
                    mSeekBars[STATUSBAR_KG_ALPHA].setCurrentAlpha(alphas[1]);

                    mMatchStatusbarKeyguard.setChecked(alphas[1] == lockscreen_alpha);

                    if (linkTransparencies) {
                        mSeekBars[NAVBAR_ALPHA].setCurrentAlpha(alphas[0]);
                        mSeekBars[NAVBAR_KG_ALPHA].setCurrentAlpha(alphas[1]);
                    } else {
                        final String navConfig = Settings.System.getString(getActivity()
                                .getContentResolver(),
                                Settings.System.NAVIGATION_BAR_ALPHA_CONFIG);
                        if (navConfig != null) {
                            split = navConfig.split(";");
                            alphas[0] = Integer.parseInt(split[0]);
                            alphas[1] = Integer.parseInt(split[1]);
                            mSeekBars[NAVBAR_ALPHA].setCurrentAlpha(alphas[0]);
                            mSeekBars[NAVBAR_KG_ALPHA].setCurrentAlpha(alphas[1]);

                            mMatchNavbarKeyguard.setChecked(alphas[1] == lockscreen_alpha);
                        }
                    }
                }
            } catch (Exception e) {
                resetSettings();
            }

            updateToggleState();
            mMatchStatusbarKeyguard.setOnCheckedChangeListener(mUpdateStatesListener);
            mMatchNavbarKeyguard.setOnCheckedChangeListener(mUpdateStatesListener);
            mLinkCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    linkTransparencies = isChecked;
                    saveSavedLinkedState(isChecked);
                    updateToggleState();
                }
            });

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setView(layout);
            builder.setTitle(getString(R.string.transparency_dialog_title));
            builder.setNegativeButton(R.string.cancel, null);
            builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Settings.System.putInt(mContentResolver,
                            Settings.System.LOCKSCREEN_ALPHA_CONFIG,
                            mSeekBars[LOCKSCREEN_ALPHA].getCurrentAlpha());
                    // update keyguard alpha
                    if (!mSeekBars[STATUSBAR_KG_ALPHA].isEnabled()) {
                        mSeekBars[STATUSBAR_KG_ALPHA].setCurrentAlpha(
                                mSeekBars[LOCKSCREEN_ALPHA].getCurrentAlpha());
                    }
                    if (!mSeekBars[NAVBAR_KG_ALPHA].isEnabled()) {
                        mSeekBars[NAVBAR_KG_ALPHA].setCurrentAlpha(
                                mSeekBars[LOCKSCREEN_ALPHA].getCurrentAlpha());
                    }
                    if (linkTransparencies) {
                        String config = mSeekBars[STATUSBAR_ALPHA].getCurrentAlpha() + ";" +
                                mSeekBars[STATUSBAR_KG_ALPHA].getCurrentAlpha();
                        Settings.System.putString(getActivity().getContentResolver(),
                                Settings.System.STATUS_BAR_ALPHA_CONFIG, config);
                        Settings.System.putString(getActivity().getContentResolver(),
                                Settings.System.NAVIGATION_BAR_ALPHA_CONFIG, config);
                    } else {
                        String sbConfig = mSeekBars[STATUSBAR_ALPHA].getCurrentAlpha() + ";" +
                                mSeekBars[STATUSBAR_KG_ALPHA].getCurrentAlpha();
                        Settings.System.putString(getActivity().getContentResolver(),
                                Settings.System.STATUS_BAR_ALPHA_CONFIG, sbConfig);

                        String nbConfig = mSeekBars[NAVBAR_ALPHA].getCurrentAlpha() + ";" +
                                mSeekBars[NAVBAR_KG_ALPHA].getCurrentAlpha();
                        Settings.System.putString(getActivity().getContentResolver(),
                                Settings.System.NAVIGATION_BAR_ALPHA_CONFIG, nbConfig);
                    }

                }
            });

            return builder.create();
        }

        private void resetSettings() {
            Settings.System.putString(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_ALPHA_CONFIG, null);
            Settings.System.putString(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_ALPHA_CONFIG, null);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.LOCKSCREEN_ALPHA_CONFIG, KEYGUARD_ALPHA);
        }

        private void updateToggleState() {
            if (linkTransparencies) {
                mSbLabel.setText(R.string.transparency_dialog_transparency_sb_and_nv);
                mNavigationBarGroup.setVisibility(View.GONE);
            } else {
                mSbLabel.setText(R.string.transparency_dialog_statusbar);
                mNavigationBarGroup.setVisibility(View.VISIBLE);
            }

            mSeekBars[STATUSBAR_KG_ALPHA]
                    .setEnabled(!mMatchStatusbarKeyguard.isChecked());
            mSeekBars[NAVBAR_KG_ALPHA]
                    .setEnabled(!mMatchNavbarKeyguard.isChecked());

            // update keyguard alpha
            int lockscreen_alpha = Settings.System.getInt(getActivity().getContentResolver(),
                        Settings.System.LOCKSCREEN_ALPHA_CONFIG, KEYGUARD_ALPHA);
            if (!mSeekBars[STATUSBAR_KG_ALPHA].isEnabled()) {
                mSeekBars[STATUSBAR_KG_ALPHA].setCurrentAlpha(lockscreen_alpha);
            }
            if (!mSeekBars[NAVBAR_KG_ALPHA].isEnabled()) {
                mSeekBars[NAVBAR_KG_ALPHA].setCurrentAlpha(lockscreen_alpha);
            }
        }

        @Override
        public void onDestroyView() {
            if (getDialog() != null && getRetainInstance())
                getDialog().setDismissMessage(null);
            super.onDestroyView();
        }

        private CompoundButton.OnCheckedChangeListener mUpdateStatesListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateToggleState();
            }
        };

        private boolean getSavedLinkedState() {
            return getActivity().getSharedPreferences("transparency", Context.MODE_PRIVATE)
                    .getBoolean("link", true);
        }

        private void saveSavedLinkedState(boolean v) {
            getActivity().getSharedPreferences("transparency", Context.MODE_PRIVATE).edit()
                    .putBoolean("link", v).commit();
        }
    }
}
