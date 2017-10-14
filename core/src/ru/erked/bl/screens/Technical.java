package ru.erked.bl.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;

import ru.erked.bl.MainBL;
import ru.erked.bl.systems.BLTextSystem;
import ru.erked.bl.utils.Fonts;
import ru.erked.bl.utils.Sounds;

public class Technical implements Screen {

    private MainBL game;
    private float timer = 0f;
    public static final float METER = Gdx.graphics.getWidth() / 8f;

    public Technical (MainBL game) {
        this.game = game;
    }

    @Override
    public void show() {
        game.atlas = new TextureAtlas("textures/texture.atlas");
        game.textSystem = new BLTextSystem(game.lang);
        game.fonts = new Fonts(game.textSystem.get("FONT_CHARS"));
        game.sounds = new Sounds();
        game.prefs = Gdx.app.getPreferences("preferences");

        game.width = Gdx.graphics.getWidth();
        game.height = Gdx.graphics.getHeight();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        timer += delta;
        if (timer > 4f) {
            game.setScreen(new Menu(game, false));
            dispose();
        }
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {

    }
}
