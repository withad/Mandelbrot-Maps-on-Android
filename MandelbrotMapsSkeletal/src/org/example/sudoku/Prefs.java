/***
 * Excerpted from "Hello, Android! 3e",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/eband3 for more book information.
***/
package org.example.sudoku;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Prefs extends PreferenceActivity {
   // Option names and default values
   private static final String OPT_MUSIC = "music";
   private static final boolean OPT_MUSIC_DEF = true;
   private static final String OPT_HINTS = "hints";
   private static final boolean OPT_HINTS_DEF = true;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.settings);
   }

   /** Get the current value of the music option */
   
   public static boolean getMusic(Context context) {
      return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(OPT_MUSIC, OPT_MUSIC_DEF);
   }
   
   
   /** Get the current value of the hints option */
   
   public static boolean getHints(Context context) {
      return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(OPT_HINTS, OPT_HINTS_DEF);
   }
   
}
