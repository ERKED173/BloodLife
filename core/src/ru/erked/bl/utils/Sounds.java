package ru.erked.bl.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

public class Sounds {

    public Sound click;
    public Sound hit;
    public Sound heal;
    public Sound star;
    public Sound cash;
    public Sound bonus;
    public Sound swim;
    public Music mainTheme;

    public Sounds () {
        mainTheme = Gdx.audio.newMusic(Gdx.files.internal("sounds/music/main_theme.mp3"));
        click = Gdx.audio.newSound(Gdx.files.internal("sounds/sound/click.wav"));
        hit = Gdx.audio.newSound(Gdx.files.internal("sounds/sound/hit.wav"));
        cash = Gdx.audio.newSound(Gdx.files.internal("sounds/sound/cash.wav"));
        heal = Gdx.audio.newSound(Gdx.files.internal("sounds/sound/heal.wav"));
        star = Gdx.audio.newSound(Gdx.files.internal("sounds/sound/star.wav"));
        bonus = Gdx.audio.newSound(Gdx.files.internal("sounds/sound/bonus.wav"));
        swim = Gdx.audio.newSound(Gdx.files.internal("sounds/sound/swim.wav"));
    }

}
