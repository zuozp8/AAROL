package iswd.aarol.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;


public class PackageManager {
    public static boolean isDownloaded(Context context, String name) {
        try {
            FileInputStream fileInputStream = context.openFileInput("packages_" + name + ".xml");
            return true;
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    public static boolean isEnabled(Context context, String name) {
        Set<String> packageNames = getEnabledPackageList(context);

        return packageNames.contains(name);
    }

    public static void setEnabled(Context context, String name, boolean state) {
        Set<String> packageNames = getEnabledPackageList(context);
        packageNames = new HashSet<String>(packageNames); // workaround for bug https://code.google.com/p/android/issues/detail?id=27801
        if (state) {
            packageNames.add(name);
        } else {
            packageNames.remove(name);
        }
        SharedPreferences.Editor sharedPref = PreferenceManager.getDefaultSharedPreferences(context).edit();
        sharedPref.putStringSet("enabledPackages", packageNames);
        sharedPref.commit();
    }

    public static Set<String> getEnabledPackageList(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> listOfEnabledPackages = sharedPref.getStringSet("enabledPackages", null);
        if (listOfEnabledPackages == null)
            listOfEnabledPackages = new HashSet<String>();
        return listOfEnabledPackages;
    }
}
