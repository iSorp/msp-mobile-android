package ch.bfh.ti.these.msp.ui;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;
import ch.bfh.ti.these.msp.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}
