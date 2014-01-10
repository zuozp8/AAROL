package iswd.aarol.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;


public class PackageManager {
    public static boolean isDownloaded(Context context, String name) {
        try {
            FileInputStream fileInputStream = context.openFileInput(getFileNameOfPackage(name));
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

    public static String getFileNameOfPackage(String name) {
        return "packages_" + name + ".xml";
    }

    public static int getColorOfPackage(Context context, String name) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        if (!sharedPref.contains(name + "|color")) {
            Random r = new Random();
            SharedPreferences.Editor prefEditor = sharedPref.edit();
            int randomColor = Color.HSVToColor(new float[]{r.nextInt(360), 1, 1});
            prefEditor.putInt(name + "|color", randomColor);
            prefEditor.commit();
        }
        return sharedPref.getInt(name + "|color", 0);
    }

    public static void setColorOfPackage(Context context, String name, int color) {
        SharedPreferences.Editor sharedPref = PreferenceManager.getDefaultSharedPreferences(context).edit();
        sharedPref.putInt(name + "|color", color);
        sharedPref.commit();
    }
}
