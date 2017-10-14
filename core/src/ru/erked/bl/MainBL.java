package ru.erked.bl;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;

import ru.erked.bl.screens.Technical;
import ru.erked.bl.systems.BLTextSystem;
import ru.erked.bl.utils.Fonts;
import ru.erked.bl.utils.Sounds;

public class MainBL extends Game {

	public int lang;
	public TextureAtlas atlas;
	public BLTextSystem textSystem;
	public Sounds sounds;
	public Fonts fonts;
	public Preferences prefs;
	public float width;
	public float height;

	public MainBL (int lang) {
		this.lang = lang;
	}

	@Override
	public void create () {
		setScreen(new Technical(this));
	}
	
	@Override
	public void dispose () {

	}
}
