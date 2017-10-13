package ru.erked.bl.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;

import ru.erked.bl.MainBL;

public class DesktopLauncher {

	public static void main (String[] arg) {

		/**/
		TexturePacker.Settings settings = new TexturePacker.Settings();
		settings.maxWidth = 2048;
		settings.maxHeight = 2048;
		TexturePacker.process(
				settings,
				"C:/Projects/Java/BloodLife/resource",
				"C:/Projects/Java/BloodLife/source/android/assets/textures",
				"texture");
 		/**/

		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.width = 393;
		config.height = 700;
		new LwjglApplication(new MainBL(1), config);

	}

}
