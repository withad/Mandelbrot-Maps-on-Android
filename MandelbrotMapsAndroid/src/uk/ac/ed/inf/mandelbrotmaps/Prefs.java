package uk.ac.ed.inf.mandelbrotmaps;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Prefs extends PreferenceActivity {
	
	private static final String CRUDE_OPTION = "CRUDE";
	private static final boolean CRUDE_OPT_DEFAULT = true;
	
	private static final String SHOW_TIMES_OPTION = "SHOW_TIMES";
	private static final boolean SHOW_TIMES_OPT_DEFAULT = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
	}
	
	public static boolean performCrude(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(CRUDE_OPTION, CRUDE_OPT_DEFAULT);
	}
	
	public static boolean showTimes(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SHOW_TIMES_OPTION, SHOW_TIMES_OPT_DEFAULT);
	}
}
