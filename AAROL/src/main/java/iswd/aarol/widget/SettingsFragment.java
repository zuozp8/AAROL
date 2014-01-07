package iswd.aarol.widget;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import iswd.aarol.R;

public class SettingsFragment extends PreferenceFragment {
    public SettingsFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
