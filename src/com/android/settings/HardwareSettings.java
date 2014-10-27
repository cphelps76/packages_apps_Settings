package com.android.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.android.settings.matricom.PackageListAdapter;

/**
 * Created by bmc on 10/24/14.
 */
public class HardwareSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceClickListener {

    private static final String TAG = HardwareSettings.class.getSimpleName();

    private static final String KEY_CUSTOM_BUTTON = "custom_button";

    private Preference mCustomButtonPreference;

    private PackageListAdapter mPackageListAdapter;

    private ContentResolver resolver;

    private static String mCustomFunctionName;
    private static String mCustomFunctionUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.hardware_settings);

        mPackageListAdapter = new PackageListAdapter(getActivity());
        mPackageListAdapter.showAppInfo(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        resolver = getActivity().getContentResolver();

        mPackageListAdapter.reloadList();
        mCustomFunctionUri = Settings.Secure.getString(
                resolver, Settings.Secure.CUSTOM_BUTTON_URI);
        mCustomButtonPreference = findPreference(KEY_CUSTOM_BUTTON);
        mCustomButtonPreference.setOnPreferenceClickListener(this);
    }

    private void showCustomFunctionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final ListView list = new ListView(getActivity());
        final Dialog dialog;
        list.setAdapter(mPackageListAdapter);
        builder.setTitle(getString(R.string.custom_function_dialog_title));
        builder.setView(list);
        dialog = builder.show();
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                PackageListAdapter.PackageItem item = (PackageListAdapter.PackageItem) adapterView.getItemAtPosition(i);
                setCustomFunction(item.packageName, item.launchIntent);
                dialog.dismiss();
            }
        });
    }

    private int getSelectedFunction(String packageName) {
        return mPackageListAdapter.getItemPosition(packageName);
    }

    private void setCustomFunction(String function, Intent launch) {
        Settings.Secure.putString(resolver, Settings.Secure.CUSTOM_BUTTON_URI, launch.toURI());
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mCustomButtonPreference) {
            showCustomFunctionDialog();
            return true;
        }
        return false;
    }
}
