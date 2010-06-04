package org.dits.symbols;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class JasymcaPrefs extends PreferenceActivity {

	/**
	 * 
	 */
	public JasymcaPrefs() {
		super();
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref);
	}

}
