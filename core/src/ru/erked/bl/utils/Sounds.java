package ru.erked.bl.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

public class Sounds {

    public Sound click;
    public Sound death;
    public Music mainTheme;

    public Sounds () {
        mainTheme = Gdx.audio.newMusic(Gdx.files.internal("sounds/music/main_theme.mp3"));
        click = Gdx.audio.newSound(Gdx.files.internal("sounds/sound/click.wav"));
        death = Gdx.audio.newSound(Gdx.files.internal("sounds/sound/death.wav"));
    }

}
