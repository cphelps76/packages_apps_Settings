/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (C) 2014 Matricom
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

package com.android.settings.ethernet;

import android.content.Context;
import android.net.ethernet.EthernetManager;
import android.net.ethernet.EthernetDevInfo;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.Preference;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import com.android.settings.R;

public class EthernetInfo extends PreferenceFragment {
    private static final String TAG = "DeviceInfoSettingEthInfo";

    private static final String KEY_IP_ADDRESS = "eth_ip_address";
    private static final String KEY_DNS = "eth_dns";
    private static final String KEY_GATEWAY = "eth_gateway";
    private static final String KEY_MAC_ADDRESS = "eth_mac";
    private static final String KEY_MASK = "eth_mask";

    private static final int MENU_REFRESH = Menu.FIRST;

    private Preference mIpAddress;
    private Preference mDns;
    private Preference mGateway;
    private Preference mMacAddress;
    private Preference mMask;

    private EthernetManager mEthManager;
    private EthernetDevInfo mEthInfo;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.ethernet_info);

        mEthManager = (EthernetManager) getActivity().getSystemService(Context.ETH_SERVICE);

        mIpAddress = findPreference(KEY_IP_ADDRESS);
        mDns = findPreference(KEY_DNS);
        mGateway = findPreference(KEY_GATEWAY);
        mMacAddress = findPreference(KEY_MAC_ADDRESS);
        mMask = findPreference(KEY_MASK);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        mEthInfo = mEthManager.getSavedEthConfig();
        updateSummaries();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(Menu.NONE, MENU_REFRESH, 0, R.string.ethernet_refresh)
                .setEnabled(true)
                .setIcon(android.R.drawable.ic_menu_rotate)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        super.onCreateOptionsMenu(menu, inflater);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_REFRESH:
                Toast.makeText(getActivity(), "Refreshing ethernet information....", Toast.LENGTH_SHORT).show();
                updateSummaries();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateSummaries() {
        if (mEthInfo != null) {
            mIpAddress.setSummary(mEthInfo.getIpAddress());
            mDns.setSummary(mEthInfo.getDnsAddress());
            mGateway.setSummary(mEthInfo.getRouteAddress());
            mMacAddress.setSummary(mEthInfo.getMacAddress());
            mMask.setSummary(mEthInfo.getNetMask());
        }
    }
}
