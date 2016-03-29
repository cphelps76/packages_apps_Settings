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
import android.content.Intent;
import android.os.Bundle;
import android.net.Uri;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.view.LayoutInflater;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.internal.logging.MetricsLogger;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class DEMENTED extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, View.OnClickListener {

    private static final String TAG = "DEMENTED";
    private static final String KEY_DEMENTED_GITHUB = "https://github.com/cphelps76";
    private static final String KEY_GESTURE_SETTINGS = "prefs_gesture";
    private static final String KEY_HARDWARE_KEYS = "prefs_buttons";

    private ImageView mLogoView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.demented_interface_settings);
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

    private void removePreferences() {
        Utils.updatePreferenceToSpecificActivityFromMetaDataOrRemove(getActivity(),
            getPreferenceScreen(), KEY_GESTURE_SETTINGS);
        if (!hasButtons()) {
            removePreference(KEY_HARDWARE_KEYS);
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

    private void launchUrl(String url) {
        Uri uriUrl = Uri.parse(url);
        Intent demented = new Intent(Intent.ACTION_VIEW, uriUrl);
        getActivity().startActivity(demented);
    }
}
