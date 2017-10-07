package ru.erked.bl;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

public class AndroidLauncher extends AndroidApplication {
	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		config.hideStatusBar = true;
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
		getWindow().getDecorView().setSystemUiVisibility(uiOptions);
		int lang;
		switch (getString(R.string.language)){
			case "EN":{
				lang = 0;
				break;
			}
			case "RU":{
				lang = 1;
				break;
			}
			default:{
				lang = 0;
				break;
			}
		}
		initialize(new MainBL(lang), config);
	}
}
