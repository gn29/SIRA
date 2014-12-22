package ru.easyapp.sira;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;

public class AboutActivity extends Activity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		ActionBar bar = getActionBar();
		bar.setTitle(getResources().getString(R.string.popup_aboud_item));
		bar.setDisplayHomeAsUpEnabled(true);
	}

}
