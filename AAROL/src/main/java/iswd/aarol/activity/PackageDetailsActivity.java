package iswd.aarol.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import iswd.aarol.R;
import iswd.aarol.model.LocationPackage;
import iswd.aarol.model.LocationPackageFactory;
import iswd.aarol.model.PackageManager;
import iswd.aarol.widget.LocationsArrayAdapter;

public class PackageDetailsActivity extends Activity {

    public static final String PACKAGE_NAME_EXTRA = "packageName";
    private String locationPackageName;
    private LocationPackage locationPackage;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_package_details);

        Bundle extras = getIntent().getExtras();
        locationPackageName = extras.getString(PACKAGE_NAME_EXTRA);

        try {
            locationPackage = LocationPackageFactory.create(this, locationPackageName);
        } catch (Exception e) {
            throw new RuntimeException(e);
            //TODO handle
        }
        ((TextView) findViewById(R.id.packageNameInDetails)).setText(locationPackage.getPackageName());

        ListView listView = (ListView) findViewById(R.id.locationsList);
        listView.setAdapter(new LocationsArrayAdapter(this, locationPackage.getLocations()));
    }

    public void enableClicked(View view) {
        boolean isChecked = ((CheckBox) view).isChecked();
        PackageManager.setEnabled(this, locationPackageName, isChecked);
    }

    public void leadToClicked(View view) {
        SharedPreferences.Editor sharedPref = PreferenceManager.getDefaultSharedPreferences(this).edit();
        sharedPref.putString(MainActivity.LEAD_TO_PACKAGE_NAME, locationPackageName);
        sharedPref.putInt(MainActivity.LEAD_TO_LOCATION_ID, (Integer) view.getTag(R.id.tag_location));
        sharedPref.commit();

        PackageManager.setEnabled(this, locationPackageName, true);

        Intent backToMainIntent = new Intent(this, MainActivity.class);
        backToMainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(backToMainIntent);
    }
}
