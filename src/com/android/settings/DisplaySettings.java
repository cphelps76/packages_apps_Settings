/*
 * Copyright (C) 2010 The Android Open Source Project
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

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SystemWriteManager;
import android.app.admin.DevicePolicyManager;
import android.content.*;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.display.HdmiManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.*;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.android.internal.view.RotationPolicy;
import com.android.settings.DreamSettings;

import java.util.ArrayList;

public class DisplaySettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, OnPreferenceClickListener {

    private static final String TAG = "DisplaySettings";
    private ListPreference  mDisplayOutputmode;
    private CharSequence[] mEntryValues;
    private int sel_index;
    private int index_entry;
    private static final int GET_USER_OPERATION=1;
    private int index_cvbs;
    private int select_cvbs;
    /** If there is no setting in the provider, use this. */
    private static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 30000;

    private static final String KEY_SCREEN_TIMEOUT = "screen_timeout";
    private static final String KEY_ACCELEROMETER = "accelerometer";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_NOTIFICATION_PULSE = "notification_pulse";
    private static final String KEY_Brightness = "brightness";
    private static final String KEY_DEFAULT_FREQUENCY = "default_frequency";
    private static final String KEY_SCREEN_SAVER = "screensaver";
    private static final String KEY_WALLPAPER = "wallpaper";
    private static final String KEY_AUTOBRIGHTNESS = "auto_brightness";
    private static final String KEY_OUTPUT_MODE = "output_mode";
    private static final String KEY_AUTO_ADJUST = "auto_adjust";
    private static final String KEY_POSITION = "position";

    private static final int DLG_GLOBAL_CHANGE_WARNING = 1;
    private static final int DLG_POSITION_CHANGE = 2;

    public static SystemWriteManager sw;
    private static HdmiManager mHdmiManager;

    private CheckBoxPreference mAccelerometer;
    private WarnedListPreference mFontSizePref;
    private CheckBoxPreference mNotificationPulse;
    private ListPreference mOutputModePref;
    private CheckBoxPreference mAutoAdjustPref;
    private Preference mPositionPref;

    private final Configuration mCurConfig = new Configuration();

    private ListPreference mScreenTimeoutPreference;
    private Preference mScreenSaverPreference;

    private ListPreference  mDefaultFrequency;
    private static final String STR_DEFAULT_FREQUENCY_VAR="ubootenv.var.defaulttvfrequency";
    private CharSequence[] mDefaultFrequencyEntries;

    private static float zoomStep = 4.0f; // defaulted to 1080p
    private static float zoomStepWidth = 1.78f; //defaulted to 1080p

    private static final int MAX_HEIGHT = 100;
    private static final int MAX_WIDTH = 100;

    private int mLeft, mTop, mWidth, mHeight, mRight, mBottom;
    private int mNewLeft, mNewTop, mNewRight, mNewBottom;

    private boolean mOriginWindowSet = false;

    private static final int MENU_ID_HDMI_RESET = Menu.FIRST;

    private final RotationPolicy.RotationPolicyListener mRotationPolicyListener =
            new RotationPolicy.RotationPolicyListener() {
        @Override
        public void onChange() {
            updateAccelerometerRotationCheckbox();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ContentResolver resolver = getActivity().getContentResolver();

        addPreferencesFromResource(R.xml.display_settings);

        sw = (SystemWriteManager)getSystemService("system_write");
        mHdmiManager = (HdmiManager) getSystemService(Context.HDMI_SERVICE);

        mOutputModePref = (ListPreference) findPreference(KEY_OUTPUT_MODE);
        mOutputModePref.setOnPreferenceChangeListener(this);
        mOutputModePref.setValue(mHdmiManager.getResolution());
        mOutputModePref.setEntries(mHdmiManager.getAvailableResolutions());
        mOutputModePref.setEntryValues(mHdmiManager.getAvailableResolutions());
        mOutputModePref.setSummary(mHdmiManager.getResolution());

        mPositionPref = findPreference(KEY_POSITION);
        mPositionPref.setOnPreferenceClickListener(this);

        mAutoAdjustPref = (CheckBoxPreference) findPreference(KEY_AUTO_ADJUST);
        mAutoAdjustPref.setChecked(Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.HDMI_AUTO_ADJUST, 0) != 0);

        if (!mHdmiManager.isHdmiPlugged()) {
            // If user is using CVBS hide this preference, since all they will have is cvbs
            getPreferenceScreen().removePreference(mOutputModePref);
            // Using cvbs only usually provides one resolution, so remove auto adjust
            getPreferenceScreen().removePreference(mAutoAdjustPref);
        }

        String autoBrightness = SystemProperties.get("prop.sp.brightness","on");
        if(autoBrightness.equals("off")) {
            getActivity().getSharedPreferences(AutoBrightnessSwitch.AUTO_PREF_NAME,
                            Context.MODE_PRIVATE).edit()
                       .putBoolean(AutoBrightnessSwitch.AUTO_PREF_ON_OFF, false).commit();
            getPreferenceScreen().removePreference(findPreference(KEY_AUTOBRIGHTNESS));
        }

        mAccelerometer = (CheckBoxPreference) findPreference(KEY_ACCELEROMETER);
        mAccelerometer.setPersistent(false);
        if (!RotationPolicy.isRotationSupported(getActivity())
                || RotationPolicy.isRotationLockToggleSupported(getActivity())) {
            // If rotation lock is supported, then we do not provide this option in
            // Display settings.  However, is still available in Accessibility settings,
            // if the device supports rotation.
            getPreferenceScreen().removePreference(mAccelerometer);
        }

        mScreenSaverPreference = findPreference(KEY_SCREEN_SAVER);
        if ((mScreenSaverPreference != null
                && getResources().getBoolean(
                        com.android.internal.R.bool.config_dreamsSupported) == false)) {
            getPreferenceScreen().removePreference(mScreenSaverPreference);
        }

        mScreenTimeoutPreference = (ListPreference) findPreference(KEY_SCREEN_TIMEOUT);
        final long currentTimeout = Settings.System.getLong(resolver, SCREEN_OFF_TIMEOUT,
                FALLBACK_SCREEN_TIMEOUT_VALUE);
        mScreenTimeoutPreference.setValue(String.valueOf(currentTimeout));
        mScreenTimeoutPreference.setOnPreferenceChangeListener(this);
        disableUnusableTimeouts(mScreenTimeoutPreference);
        updateTimeoutPreferenceDescription(currentTimeout);

        mScreenSaverPreference.setEnabled(currentTimeout != 2147483646);

        mFontSizePref = (WarnedListPreference) findPreference(KEY_FONT_SIZE);
        mFontSizePref.setOnPreferenceChangeListener(this);
        mFontSizePref.setOnPreferenceClickListener(this);
        mNotificationPulse = (CheckBoxPreference) findPreference(KEY_NOTIFICATION_PULSE);
        if (mNotificationPulse != null
                && getResources().getBoolean(
                        com.android.internal.R.bool.config_intrusiveNotificationLed) == false) {
            getPreferenceScreen().removePreference(mNotificationPulse);
        } else {
            try {
                mNotificationPulse.setChecked(Settings.System.getInt(resolver,
                        Settings.System.NOTIFICATION_LIGHT_PULSE) == 1);
                mNotificationPulse.setOnPreferenceChangeListener(this);
            } catch (SettingNotFoundException snfe) {
                Log.e(TAG, Settings.System.NOTIFICATION_LIGHT_PULSE + " not found");
            }
        }

        if (Utils.platformHasMbxUiMode()) {
            getPreferenceScreen().removePreference(findPreference(KEY_WALLPAPER));
        }

        if (!Utils.platformHasScreenBrightness()) {
            getPreferenceScreen().removePreference(findPreference(KEY_Brightness));
        }

        if (!Utils.platformHasScreenFontSize()) {
            getPreferenceScreen().removePreference(mFontSizePref);
        }

        if(Utils.platformHasDefaultTVFreq()){
    	    mDefaultFrequency = (ListPreference) findPreference(KEY_DEFAULT_FREQUENCY);
    	    mDefaultFrequency.setOnPreferenceChangeListener(this);
    	    String valDefaultFrequency = SystemProperties.get(STR_DEFAULT_FREQUENCY_VAR);
    	    mDefaultFrequencyEntries = getResources().getStringArray(R.array.default_frequency_entries);
    	    if(valDefaultFrequency.equals("")){
    	        valDefaultFrequency = getResources().getString(R.string.tv_default_frequency_summary);
    	    }
    	    int index_DF = findIndexOfEntry(valDefaultFrequency, mDefaultFrequencyEntries);
    	    mDefaultFrequency.setValueIndex(index_DF);
            mDefaultFrequency.setSummary(valDefaultFrequency);
        } else {
            getPreferenceScreen().removePreference(findPreference(KEY_DEFAULT_FREQUENCY));
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(Menu.NONE, MENU_ID_HDMI_RESET, 0, R.string.hdmi_menu_reset)
                .setEnabled(true)
                .setIcon(android.R.drawable.ic_menu_rotate)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS|MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ID_HDMI_RESET:
                reset();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateTimeoutPreferenceDescription(long currentTimeout) {
        ListPreference preference = mScreenTimeoutPreference;
        String summary;
        if (currentTimeout < 0) {
            // Unsupported value
            summary = "";
        } else {
            final CharSequence[] entries = preference.getEntries();
            final CharSequence[] values = preference.getEntryValues();
            if (entries == null || entries.length == 0) {
                summary = "";
            } else {
                int best = 0;
                for (int i = 0; i < values.length; i++) {
                    long timeout = Long.parseLong(values[i].toString());
                    if (currentTimeout >= timeout) {
                        best = i;
                    }
                }
                if(currentTimeout >= (Integer.MAX_VALUE-1))
                {
                    summary = entries[best].toString();
                }else{
                    summary = preference.getContext().getString(R.string.screen_timeout_summary,entries[best]);
                }

            }
        }
        preference.setSummary(summary);
    }

    private void disableUnusableTimeouts(ListPreference screenTimeoutPreference) {
        final DevicePolicyManager dpm =
                (DevicePolicyManager) getActivity().getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        final long maxTimeout = dpm != null ? dpm.getMaximumTimeToLock(null) : 0;
        if (maxTimeout == 0) {
            return; // policy not enforced
        }
        final CharSequence[] entries = screenTimeoutPreference.getEntries();
        final CharSequence[] values = screenTimeoutPreference.getEntryValues();
        ArrayList<CharSequence> revisedEntries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> revisedValues = new ArrayList<CharSequence>();
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.parseLong(values[i].toString());
            if (timeout <= maxTimeout) {
                revisedEntries.add(entries[i]);
                revisedValues.add(values[i]);
            }
        }
        if (revisedEntries.size() != entries.length || revisedValues.size() != values.length) {
            final int userPreference = Integer.parseInt(screenTimeoutPreference.getValue());
            screenTimeoutPreference.setEntries(
                    revisedEntries.toArray(new CharSequence[revisedEntries.size()]));
            screenTimeoutPreference.setEntryValues(
                    revisedValues.toArray(new CharSequence[revisedValues.size()]));
            if (userPreference <= maxTimeout) {
                screenTimeoutPreference.setValue(String.valueOf(userPreference));
            } else if (revisedValues.size() > 0
                    && Long.parseLong(revisedValues.get(revisedValues.size() - 1).toString())
                    == maxTimeout) {
                // If the last one happens to be the same as the max timeout, select that
                screenTimeoutPreference.setValue(String.valueOf(maxTimeout));
            } else {
                // There will be no highlighted selection since nothing in the list matches
                // maxTimeout. The user can still select anything less than maxTimeout.
                // TODO: maybe append maxTimeout to the list and mark selected.
            }
        }
        screenTimeoutPreference.setEnabled(revisedEntries.size() > 0);
    }

    int floatToIndex(float val) {
        String[] indices = getResources().getStringArray(R.array.entryvalues_font_size);
        float lastVal = Float.parseFloat(indices[0]);
        for (int i=1; i<indices.length; i++) {
            float thisVal = Float.parseFloat(indices[i]);
            if (val < (lastVal + (thisVal-lastVal)*.5f)) {
                return i-1;
            }
            lastVal = thisVal;
        }
        return indices.length-1;
    }

    public void readFontSizePreference(ListPreference pref) {
        try {
            mCurConfig.updateFrom(ActivityManagerNative.getDefault().getConfiguration());
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to retrieve font size");
        }

        // mark the appropriate item in the preferences list
        int index = floatToIndex(mCurConfig.fontScale);
        pref.setValueIndex(index);

        // report the current size in the summary text
        final Resources res = getResources();
        String[] fontSizeNames = res.getStringArray(R.array.entries_font_size);
        pref.setSummary(String.format(res.getString(R.string.summary_font_size),
                fontSizeNames[index]));
    }

    @Override
    public void onResume() {
        super.onResume();
        updateState();

        RotationPolicy.registerRotationPolicyListener(getActivity(),
                mRotationPolicyListener);

        updateState();
    }

    @Override
    public void onPause() {
        super.onPause();

        RotationPolicy.unregisterRotationPolicyListener(getActivity(),
                mRotationPolicyListener);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        if (dialogId == DLG_GLOBAL_CHANGE_WARNING) {
            return Utils.buildGlobalChangeWarningDialog(getActivity(),
                    R.string.global_font_change_title,
                    new Runnable() {
                        public void run() {
                            mFontSizePref.click();
                        }
                    });
        }
        return null;
    }

    private void reset() {
        mHdmiManager.resetPosition();
        mLeft = 0;
        mRight = 0;
        mWidth = mHdmiManager.getFullWidthPosition();
        mHeight = mHdmiManager.getFullHeightPosition();
    }

    private void initPosition() {
        final int[] position = mHdmiManager.getPosition(mHdmiManager.getResolution());
        mLeft = position[0];
        mTop = position[1];
        mWidth = position[2];
        mHeight = position[3];
        mRight = mWidth;// + mLeft;
        mBottom = mHeight;// + mTop;
        Log.d(TAG, "left=" + mLeft + " top=" + mTop + " width=" + mWidth + " height=" + mHeight + " right=" + mRight + " bottom=" + mBottom);
        mNewLeft = mLeft;
        mNewTop = mTop;
        mNewRight = mRight;
        mNewBottom = mBottom;
        if (!mHdmiManager.isRealOutputMode()) {
            sw.writeSysfs(mHdmiManager.FREESCALE_FB0, "1");
            sw.writeSysfs(mHdmiManager.FREESCALE_FB1, "1");
        } else if (mHdmiManager.getResolution().contains("720")
                || mHdmiManager.getResolution().contains("1080")) {
            if (mLeft == 0 && mTop == 0) {
                setOriginWindowForFreescale();
            }
        }
    }

    private void setOriginWindowForFreescale() {
        if (!mOriginWindowSet) {
            mOriginWindowSet = true;

            sw.writeSysfs(mHdmiManager.FREESCALE_MODE, "1");
            sw.writeSysfs(mHdmiManager.FREESCALE_AXIS, "0 0 " + mWidth + " " + mHeight);
            sw.writeSysfs(mHdmiManager.FREESCALE_FB0, "0x10001");
        }
    }

    private void initSteps() {
        String resolution = mHdmiManager.getResolution();
        if (resolution.contains("480")) {
            zoomStep = 2.0f;
            zoomStepWidth = 1.50f;
        } else if (resolution.contains("576")) {
            zoomStep = 2.0f;
            zoomStepWidth = 1.25f;
        } else if (resolution.contains("720")) {
            zoomStep = 3.0f;
            zoomStepWidth = 1.78f;
        } else {
            zoomStep = 4.0f;
            zoomStepWidth = 1.78f;
        }
    }

    private int getCurrentLeftRate() {
        Log.d(TAG, "mLeft is " + mLeft);
        int savedValue = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.HDMI_OVERSCAN_LEFT, 100);
        return savedValue;
    }

    private int getCurrentTopRate() {
        Log.d(TAG, "mTop is " + mTop);
        int savedValue = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.HDMI_OVERSCAN_TOP, 100);
        return savedValue;
    }

    private int getCurrentRightRate() {
        Log.d(TAG, "mRight is " + mRight);
        int savedValue = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.HDMI_OVERSCAN_RIGHT, 100);
        return savedValue;
    }

    private int getCurrentBottomRate() {
        Log.d(TAG, "mBottom is " + mBottom);
        int savedValue = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.HDMI_OVERSCAN_BOTTOM, 100);
        return savedValue;
    }

    private void updateState() {
        updateAccelerometerRotationCheckbox();
        readFontSizePreference(mFontSizePref);
        updateScreenSaverSummary();
		updateRequestRotationCheckbox();
    }

    private void updateScreenSaverSummary() {
        if (mScreenSaverPreference != null) {
            mScreenSaverPreference.setSummary(
                    DreamSettings.getSummaryTextWithDreamName(getActivity()));
        }
    }

    private void updateAccelerometerRotationCheckbox() {
        if (getActivity() == null) return;

        mAccelerometer.setChecked(!RotationPolicy.isRotationLocked(getActivity()));
    }

	private void updateRequestRotationCheckbox() {
        if (getActivity() == null) return;
    }

    public void writeFontSizePreference(Object objValue) {
        try {
            mCurConfig.fontScale = Float.parseFloat(objValue.toString());
            ActivityManagerNative.getDefault().updatePersistentConfiguration(mCurConfig);
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to save font size");
        }
    }

    private void showPositionDialog(Context context) {
        initPosition();
        initSteps();
        mHdmiManager.setMinScalingFrequency(408000);
        // sysfs are written as progress is changed for real-time effect
        // cancel obviously reverts back to previous values
        final int[] left_rate = {getCurrentLeftRate()};
        final int[] top_rate = {getCurrentTopRate()};
        final int[] right_rate = {getCurrentRightRate()};
        final int[] bottom_rate = {getCurrentBottomRate()};
        //final int[] newWidth = new int[1], newHeight = new int[1];
        LayoutInflater inflater = this.getActivity().getLayoutInflater();
        View dialog = inflater.inflate(R.layout.overscan_dialog, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialog);
        builder.setNegativeButton(R.string.dlg_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mHdmiManager.setPosition(mLeft, mTop, mRight, mBottom);
                mHdmiManager.savePosition(mLeft, mTop, mRight, mBottom);
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mHdmiManager.setPosition(mLeft, mTop, mRight, mBottom);
                mHdmiManager.savePosition(mLeft, mTop, mRight, mBottom);
            }
        });
        builder.setPositiveButton(R.string.dlg_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mHdmiManager.savePosition(mNewLeft, mNewTop, mNewRight, mNewBottom);
                mLeft = mNewLeft;
                mTop = mNewTop;
                mRight = mNewRight;
                mBottom = mNewBottom;
                Settings.Secure.putInt(getActivity().getContentResolver(),
                        Settings.Secure.HDMI_OVERSCAN_LEFT, left_rate[0]);
                Settings.Secure.putInt(getActivity().getContentResolver(),
                        Settings.Secure.HDMI_OVERSCAN_TOP, top_rate[0]);
                Settings.Secure.putInt(getActivity().getContentResolver(),
                        Settings.Secure.HDMI_OVERSCAN_RIGHT, right_rate[0]);
                Settings.Secure.putInt(getActivity().getContentResolver(),
                        Settings.Secure.HDMI_OVERSCAN_BOTTOM, bottom_rate[0]);
            }
        });
        builder.setTitle(R.string.hdmi_overscan_title);
        builder.setMessage(R.string.hdmi_overscan_help);
        AlertDialog alert = builder.show();

        TextView mMessage = (TextView) alert.findViewById(android.R.id.message);
        mMessage.setGravity(Gravity.CENTER_HORIZONTAL);
        NumberPicker mLeftPicker = (NumberPicker) dialog.findViewById(R.id.left_picker);
        mLeftPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        mLeftPicker.setMinValue(0);
        mLeftPicker.setMaxValue(100);
        mLeftPicker.setValue(left_rate[0]);
        mLeftPicker.setWrapSelectorWheel(false);
        mLeftPicker.requestFocus();
        mLeftPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                if (oldVal > newVal) {
                    //zoom out
                    zoomOut(picker);
                } else {
                    // zoom in
                    zoomIn(picker);
                }
                left_rate[0] = newVal;

            }
        });
        NumberPicker mTopPicker = (NumberPicker) dialog.findViewById(R.id.top_picker);
        mTopPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        mTopPicker.setMinValue(0);
        mTopPicker.setMaxValue(100);
        mTopPicker.setValue(top_rate[0]);
        mTopPicker.setWrapSelectorWheel(false);
        mTopPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                if (oldVal > newVal) {
                    //zoom out
                    zoomOut(picker);
                } else {
                    // zoom in
                    zoomIn(picker);
                }
                top_rate[0] = newVal;
            }
        });
        NumberPicker mRightPicker = (NumberPicker) dialog.findViewById(R.id.right_picker);
        mRightPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        mRightPicker.setMinValue(0);
        mRightPicker.setMaxValue(100);
        mRightPicker.setValue(right_rate[0]);
        mRightPicker.setWrapSelectorWheel(false);
        mRightPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                if (oldVal > newVal) {
                    //zoom out
                    zoomOut(picker);
                } else {
                    // zoom in
                    zoomIn(picker);
                }
                right_rate[0] = newVal;
            }
        });
        NumberPicker mBottomPicker = (NumberPicker) dialog.findViewById(R.id.bottom_picker);
        mBottomPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        mBottomPicker.setMinValue(0);
        mBottomPicker.setMaxValue(100);
        mBottomPicker.setValue(bottom_rate[0]);
        mBottomPicker.setWrapSelectorWheel(false);
        mBottomPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                if (oldVal > newVal) {
                   //zoom out
                    zoomOut(picker);
                } else {
                    // zoom in
                    zoomIn(picker);
                }
                bottom_rate[0] = newVal;
            }
        });
    }

    private void zoomOut(NumberPicker picker) {
        switch (picker.getId()) {
            case R.id.left_picker:
                mNewLeft += (int)(zoomStep * zoomStepWidth);
                break;
            case R.id.top_picker:
                mNewTop += zoomStep;
                break;
            case R.id.right_picker:
                mNewRight -= (int)(zoomStep * zoomStepWidth);
                break;
            case R.id.bottom_picker:
                mNewBottom -= zoomStep;
                break;
        }
        Log.d(TAG, "left=" + mNewLeft + " top=" + mNewTop + " right=" + mNewRight + " bottom=" + mNewBottom);
        mHdmiManager.setPosition(mNewLeft, mNewTop, mNewRight, mNewBottom);
    }

    private void zoomIn(NumberPicker picker) {
        switch (picker.getId()) {
            case R.id.left_picker:
                mNewLeft -= (int)(zoomStep * zoomStepWidth);
                break;
            case R.id.top_picker:
                mNewTop -= zoomStep;
                break;
            case R.id.right_picker:
                mNewRight += (int)(zoomStep * zoomStepWidth);
                break;
            case R.id.bottom_picker:
                mNewBottom += zoomStep;
                break;
        }
        Log.d(TAG, "left=" + mNewLeft + " top=" + mNewTop + " right=" + mNewRight + " bottom=" + mNewBottom);
        mHdmiManager.setPosition(mNewLeft, mNewTop, mNewRight, mNewBottom);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mAccelerometer) {
            RotationPolicy.setRotationLockForAccessibility(
                    getActivity(), !mAccelerometer.isChecked());
            return true;
        } else if (preference == mNotificationPulse) {
            boolean value = mNotificationPulse.isChecked();
            Settings.System.putInt(getContentResolver(), Settings.System.NOTIFICATION_LIGHT_PULSE,
                    value ? 1 : 0);
            return true;
        } else if (preference == mAutoAdjustPref) {
            Log.d(TAG, "auto adjust is " + mAutoAdjustPref.isChecked());
            int enabled = mAutoAdjustPref.isChecked() ? 1 : 0;
            Log.d(TAG, "setting HDMI_AUTO_ADJUST to " + enabled);
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    Settings.Secure.HDMI_AUTO_ADJUST, enabled);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (key.equals(KEY_SCREEN_TIMEOUT)) {
            int value = Integer.parseInt((String) objValue);
            try {
                Settings.System.putInt(getContentResolver(), SCREEN_OFF_TIMEOUT, value);
                updateTimeoutPreferenceDescription(value);
                mScreenSaverPreference.setEnabled(value != 2147483646);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist screen timeout setting", e);
            }
            return true;
        } else if (key.equals(KEY_FONT_SIZE)) {
            writeFontSizePreference(objValue);
            return true;
        } else if(key.equals(KEY_DEFAULT_FREQUENCY)){
            try {
                int frequency_index = Integer.parseInt((String) objValue);
                mDefaultFrequency.setSummary(mDefaultFrequencyEntries[frequency_index]);
                SystemProperties.set(STR_DEFAULT_FREQUENCY_VAR,mDefaultFrequencyEntries[frequency_index].toString());
            }catch(NumberFormatException e){
                Log.e(TAG, "could not persist default TV frequency setting", e);
            }
            return true;
        } else if (key.equals(KEY_OUTPUT_MODE)) {
            String oldMode = mHdmiManager.getResolution();
            String newMode = objValue.toString();
            if (!oldMode.equals(newMode)) {
                if (mHdmiManager.isHdmiPlugged()) {
                    mHdmiManager.closeVdac(newMode);
                    if (mHdmiManager.isRealOutputMode() &&
                            mHdmiManager.isHdmiOnly()) {
                        mHdmiManager.setOutputMode(newMode);
                    } else if (mHdmiManager.isHdmiOnly()) {
                        if (mHdmiManager.isFreescaleClosed()) {
                            mHdmiManager.setOutputWithoutFreescale(newMode);
                        } else {
                            mHdmiManager.setOutputMode(newMode);
                        }
                    }
                }
            }
            sw.writeSysfs(mHdmiManager.BLANK_DISPLAY, "0");
            Settings.Secure.putString(getActivity().getContentResolver(),
                    Settings.Secure.HDMI_RESOLUTION, newMode);
            mOutputModePref.setSummary(newMode);
            // reset position after resolution change
            reset();
            return true;
        }

        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mFontSizePref) {
            if (Utils.hasMultipleUsers(getActivity())) {
                showDialog(DLG_GLOBAL_CHANGE_WARNING);
            } else {
                mFontSizePref.click();
            }
            return true;
        } else if (preference == mPositionPref) {
            showPositionDialog(getActivity());
            return true;
        }
        return false;
    }

    private int findIndexOfEntry(String value, CharSequence[] entry) {
        if (value != null && entry != null) {
            for (int i = entry.length - 1; i >= 0; i--) {
                if (entry[i].equals(value)) return i;
            }
        }
        return getResources().getInteger(R.integer.outputmode_default_values);  //set 720p as default
    }
}
