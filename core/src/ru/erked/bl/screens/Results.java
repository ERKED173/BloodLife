package ru.erked.bl.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.ParallelAction;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import java.util.LinkedList;

import ru.erked.bl.MainBL;
import ru.erked.bl.systems.BLButton;
import ru.erked.bl.utils.AdvSprite;
import ru.erked.bl.utils.Obfuscation;

class Results implements Screen {

    private Stage stage;
    private MainBL game;
    private Obfuscation obf;
    private boolean nextStart = false;

    private BLButton next;

    private RandomXS128 rand;
    private LinkedList<AdvSprite> advSprites;

    private int curLevel;
    private int score;
    private int prevScore;

    private String[] text;

    Results (MainBL game, int curLevel, int score) {
        this.game = game;
        this.curLevel = curLevel;
        this.score = score;
    }

    @Override
    public void show() {
        game.sounds.mainTheme.setLooping(true);
        game.sounds.mainTheme.setVolume(0.25f);
        if (Menu.isSoundOn) game.sounds.mainTheme.play();

        prevScore = game.prefs.getInteger("level_score_" + curLevel, 0);

        rand = new RandomXS128();
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        advSprites = new LinkedList<AdvSprite>();
        for (int i = 0; i < rand.nextInt(10) + 20; i++) {
            addPart();
        }

        buttonInit();

        text = new String[6];
        text[0] = game.textSystem.get("lvl");
        text[1] = game.textSystem.get("cmp");
        text[2] = game.textSystem.get("oscsk");
        text[3] = game.textSystem.get("cursk");
        text[4] = game.textSystem.get("oldsk");
        text[5] = game.textSystem.get("newsk");

        obf = new Obfuscation(game.atlas.createSprite("obfuscation"), true);
        stage.addActor(obf);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(220f/255f, 150f/255f, 180f/255f, 0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if(obf.isActive() && !nextStart){
            obf.deactivate(0.5f, delta);
        } else if (nextStart) {
            if (obf.isActive()) {
                game.setScreen(new Menu(game, true));
            }
            obf.activate(0.5f, delta);
        }

        for (AdvSprite sprite : advSprites) {
            sprite.updateSprite();
            if (!sprite.hasActions()) {
                changePart(sprite);
            }
        }

        stage.getBatch().begin();
        drawText();
        stage.getBatch().end();

        stage.act(delta);
        stage.draw();

        if(Gdx.input.isKeyJustPressed(Input.Keys.BACK)){
            game.prefs.putInteger("max_level", Menu.maxLevel);
            game.prefs.putBoolean("is_sound_on", Menu.isSoundOn);
            game.prefs.flush();
            dispose();
            Gdx.app.exit();
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.MENU)){
            game.prefs.putInteger("max_level", Menu.maxLevel);
            game.prefs.putBoolean("is_sound_on", Menu.isSoundOn);
            game.prefs.flush();
            dispose();
            Gdx.app.exit();
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.HOME)){
            game.prefs.putInteger("max_level", Menu.maxLevel);
            game.prefs.putBoolean("is_sound_on", Menu.isSoundOn);
            game.prefs.flush();
            dispose();
            Gdx.app.exit();
        }
    }

    private void buttonInit () {
        next = new BLButton(
                game,
                0.725f*game.width,
                0.025f*game.width,
                0.25f*game.width,
                game.fonts.small.getFont(),
                game.textSystem.get("next_button"),
                1,
                "next_button"
        );
        next.get().addListener(new ClickListener(){
            @Override
            public void clicked (InputEvent event, float x, float y) {
                if (!obf.isActive()) {
                    if (Menu.isSoundOn) game.sounds.click.play();
                    nextStart = true;
                } else {
                    next.get().setChecked(false);
                }
            }
        });
        stage.addActor(next.get());
    }

    private void addPart () {
        Color color = new Color((rand.nextInt(25) + 230)/255f, (rand.nextInt(15) + 1)/255f, (rand.nextInt(50) + 25)/255f, 1f);
        float x = rand.nextInt((int)(game.width));
        float y = rand.nextInt((int)(game.height));
        float length = rand.nextInt((int)(0.01f*game.width)) + 0.01f*game.width;
        AdvSprite particle = new AdvSprite(
                game.atlas.createSprite("particle"),
                x,
                y,
                length,
                length);
        particle.getSprite().setAlpha(0f);
        float lifeTime = rand.nextInt(2) + 1 + rand.nextFloat();
        particle.addAction(new ParallelAction(
                new SequenceAction(
                        Actions.sizeBy(10f, 10f, 0.5f * lifeTime),
                        Actions.sizeBy(-10f, -10f, 0.5f * lifeTime)
                ),
                new SequenceAction(
                        Actions.alpha(1f, 0.5f * lifeTime),
                        Actions.alpha(0f, 0.5f * lifeTime)
                ),
                Actions.rotateBy(rand.nextInt(2) == 0 ? 360 : -360, lifeTime)
        ));
        particle.setColor(color);
        stage.addActor(particle);
        advSprites.addFirst(particle);
    }
    private void changePart (AdvSprite e) {
        e.addAction(Actions.alpha(0f));
        Color color = new Color((rand.nextInt(25) + 230)/255f, (rand.nextInt(15) + 1)/255f, (rand.nextInt(50) + 25)/255f, 1f);
        float x = rand.nextInt((int)(game.width));
        float y = rand.nextInt((int)(game.height));
        float length = rand.nextInt((int)(0.01f*game.width)) + 0.01f*game.width;
        e.setPosition(x, y);
        e.setWidth(length);
        e.setHeight(length);
        float lifeTime = rand.nextInt(2) + 1 + rand.nextFloat();
        e.addAction(new ParallelAction(
                new SequenceAction(
                        Actions.sizeBy(10f, 10f, 0.5f * lifeTime),
                        Actions.sizeBy(-10f, -10f, 0.5f * lifeTime)
                ),
                new SequenceAction(
                        Actions.alpha(1f, 0.5f * lifeTime),
                        Actions.alpha(0f, 0.5f * lifeTime)
                ),
                Actions.rotateBy(rand.nextInt(2) == 0 ? 360 : -360, lifeTime)
        ));
        e.setColor(color);
    }

    private void drawText () {
        game.fonts.largeS.draw(
                stage.getBatch(),
                text[0] + " " + curLevel + " " + text[1],
                0.5f*(game.width - (game.fonts.largeS.getWidth(text[0] + " " + curLevel + " " + text[1]))),
                0.95f*game.height
        );

        if (prevScore < score) {
            game.fonts.mediumS.draw(
                    stage.getBatch(),
                    text[4] + ": " + prevScore,
                    0.5f*(game.width - (game.fonts.mediumS.getWidth(text[4] + ": " + prevScore))),
                    0.5f*(game.height + 3f*game.fonts.mediumS.getHeight("A"))
            );
            game.fonts.mediumS.setColor(Color.GOLD);
            game.fonts.mediumS.draw(
                    stage.getBatch(),
                    text[5] + ": " + score,
                    0.5f*(game.width - (game.fonts.mediumS.getWidth(text[5] + ": " + score))),
                    0.5f*(game.height - game.fonts.mediumS.getHeight("A"))
            );
            game.fonts.mediumS.setColor(Color.WHITE);
            game.prefs.putInteger("level_score_" + curLevel, score);
        } else {
            game.fonts.mediumS.draw(
                    stage.getBatch(),
                    text[3] + ": " + prevScore,
                    0.5f*(game.width - (game.fonts.mediumS.getWidth(text[3] + ": " + prevScore))),
                    0.5f*(game.height + 3f*game.fonts.mediumS.getHeight("A"))
            );
            game.fonts.mediumS.draw(
                    stage.getBatch(),
                    text[2] + ": " + score,
                    0.5f*(game.width - (game.fonts.mediumS.getWidth(text[2] + ": " + score))),
                    0.5f*(game.height - game.fonts.mediumS.getHeight("A"))
            );
        }
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void pause() {
        game.prefs.putInteger("max_level", Menu.maxLevel);
        game.prefs.putBoolean("is_sound_on", Menu.isSoundOn);
        game.prefs.flush();
        if (game.sounds.mainTheme.isPlaying()) {
            game.sounds.mainTheme.pause();
            game.sounds.mainTheme.stop();
        }
    }

    @Override
    public void resume() {
        if (!game.sounds.mainTheme.isPlaying()) if (Menu.isSoundOn) game.sounds.mainTheme.play();
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        game.prefs.putInteger("max_level", Menu.maxLevel);
        game.prefs.putBoolean("is_sound_on", Menu.isSoundOn);
        game.prefs.flush();
    }
}
