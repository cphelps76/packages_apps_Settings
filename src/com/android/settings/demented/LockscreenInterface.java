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

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class LockscreenInterface extends SettingsPreferenceFragment {
    private static final String TAG = "LockscreenInterface";

    private static final String KEY_ADDITIONAL_OPTIONS = "options_group";
    private static final String KEY_SLIDER_OPTIONS = "slider_group";
    private static final String KEY_LOCKSCREEN_BUTTONS = "lockscreen_buttons";

    private int mUnsecureUnlockMethod;

    private PreferenceScreen mLockscreenButtons;
    private PreferenceCategory mAdditionalOptions;

    public boolean hasButtons() {
        return !getResources().getBoolean(com.android.internal.R.bool.config_showNavigationBar);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createCustomLockscreenView();
    }

    private PreferenceScreen createCustomLockscreenView() {
        PreferenceScreen prefs = getPreferenceScreen();
        if (prefs != null) {
            prefs.removeAll();
        }

        addPreferencesFromResource(R.xml.lockscreen_interface_settings);
        prefs = getPreferenceScreen();

        mAdditionalOptions = (PreferenceCategory) prefs.findPreference(KEY_ADDITIONAL_OPTIONS);

        mLockscreenButtons = (PreferenceScreen) findPreference(KEY_LOCKSCREEN_BUTTONS);
        if (!hasButtons()) {
            mAdditionalOptions.removePreference(mLockscreenButtons);
	}

        mUnsecureUnlockMethod = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.LOCKSCREEN_UNSECURE_USED, 1);

        //setup custom lockscreen customize view
        if (mUnsecureUnlockMethod != 1) {
             PreferenceCategory sliderCategory = (PreferenceCategory) findPreference(KEY_SLIDER_OPTIONS);
             getPreferenceScreen().removePreference(sliderCategory);
        }
        return prefs;
    }

    @Override
    public void onResume() {
        super.onResume();
        createCustomLockscreenView();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}
