package com.miracast.enabler.ui;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.miracast.enabler.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            // World-readable so XSharedPreferences can read from hook processes
            getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
            setPreferencesFromResource(R.xml.preferences, rootKey);

            setupDisplaySource();
            setupDeviceInfo();
        }

        private void setupDisplaySource() {
            // Only show display source option on foldable devices
            PreferenceCategory displayCategory = findPreference("display_category");
            if (displayCategory == null) return;

            String device = Build.DEVICE;
            boolean isFoldable = "rango".equals(device)
                || "comet".equals(device)
                || "cometl".equals(device)
                || "felix".equals(device);

            if (!isFoldable) {
                getPreferenceScreen().removePreference(displayCategory);
            }
        }

        private void setupDeviceInfo() {
            Preference devicePref = findPreference("status_device");
            if (devicePref != null) {
                String info = Build.MODEL + " (" + Build.DEVICE + ")"
                    + "\nAndroid " + Build.VERSION.RELEASE
                    + " (API " + Build.VERSION.SDK_INT + ")"
                    + "\nSoC: " + Build.SOC_MODEL;
                devicePref.setSummary(info);
            }

            Preference hookPref = findPreference("status_hooks");
            if (hookPref != null) {
                hookPref.setSummary("Reboot after enabling module in LSPosed");
            }
        }
    }
}
