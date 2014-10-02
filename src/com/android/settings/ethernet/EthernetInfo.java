/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.PreferenceFragment;
import android.preference.Preference;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import com.android.settings.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileReader;
import android.util.Log;

public class EthernetInfo extends PreferenceFragment {
    private static final String TAG = "DeviceInfoSettingEthInfo";

    private static final String KEY_IP_ADDRESS = "eth_ip_address";
    private static final String KEY_DNS = "eth_dns";
    private static final String KEY_GATEWAY = "eth_gateway";
    private static final String KEY_MAC_ADDRESS = "eth_mac";
    private static final String KEY_MASK = "eth_mask";

    private static final int MENU_REFRESH = Menu.FIRST;

    private static final String CONFIG_PATH = "/sys/class/efuse/mac";

    Preference mIpAddress;
    Preference mDns;
    Preference mGateway;
    Preference mMacAddress;
    Preference mMask;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.ethernet_info);

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
        mIpAddress.setSummary(getIpAddress());
        mDns.setSummary(getDns());
        mGateway.setSummary(getGateway());
        mMacAddress.setSummary((getMacfromEfuse()));
        mMask.setSummary(getMask());
    }

    private String getMacfromEfuse() {
        String sn = null;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(CONFIG_PATH), 12);
            try {
                sn = reader.readLine();
            } finally {
                reader.close();
            }
            Log.d(TAG, "/sys/class/efuse/mac: " + sn);

            if(sn.equals("00:00:00:00:00:00")) {
                sn = SystemProperties.get("ubootenv.var.ethaddr",getString(R.string.status_unavailable));
            }
        } catch (IOException e) {
            Log.e(TAG, "IO Exception when getting serial number for Device Info screen", e);
            sn = SystemProperties.get("ubootenv.var.ethaddr", getString(R.string.status_unavailable));
        }
        return sn;
    }

    private String getIpAddress() {
        return SystemProperties.get("dhcp.eth0.ipaddress", getString(R.string.status_unavailable));
    }

    private String getDns() {
        return SystemProperties.get("dhcp.eth0.dns1", getString(R.string.status_unavailable));
    }

    private String getMask()   {
        return SystemProperties.get("dhcp.eth0.mask", getString(R.string.status_unavailable));
    }

    private String getGateway()   {
        return SystemProperties.get("dhcp.eth0.gateway", getString(R.string.status_unavailable));
    }


}
