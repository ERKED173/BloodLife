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

public class Menu implements Screen {

    private Stage stage;
    private MainBL game;
    private Obfuscation obf;
    private boolean nextStart = false;
    private boolean isAbout = false;

    private BLButton exit;
    private BLButton start;
    private BLButton about;

    private RandomXS128 rand;
    private LinkedList<AdvSprite> advSprites;
    private AdvSprite logo;

    public Menu (MainBL game) {
        this.game = game;
    }

    @Override
    public void show() {
        game.sounds.mainTheme.setLooping(true);
        game.sounds.mainTheme.setVolume(0.25f);
        game.sounds.mainTheme.play();

        rand = new RandomXS128();
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        advSprites = new LinkedList<AdvSprite>();
        for (int i = 0; i < rand.nextInt(10) + 20; i++) {
            addPart();
        }

        buttonInit();

        obf = new Obfuscation(game.atlas.createSprite("obfuscation"), true);
        stage.addActor(obf);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(220f/255f, 150f/255f, 180f/255f, 0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if(obf.isActive() && !nextStart){
            obf.deactivate(1f, delta);
        } else if (nextStart) {
            if (obf.isActive()) {
                game.setScreen(new Space(game, -1));
                dispose();
            } else {
                obf.activate(1f, delta);
            }
        }

        for (AdvSprite sprite : advSprites) {
            sprite.updateSprite();
            if (!sprite.hasActions()) {
                changePart(sprite);
            }
        }

        if (!logo.hasActions()) {
            logo.addAction(Actions.sequence(
                    Actions.parallel(
                            Actions.sizeBy(0.25f*game.width, 0.0625f*game.width, 2f),
                            Actions.moveBy(-0.125f*game.width, -0.03125f*game.width, 2f)
                    ),
                    Actions.parallel(
                            Actions.sizeBy(-0.25f*game.width, -0.0625f*game.width, 2f),
                            Actions.moveBy(0.125f*game.width, 0.03125f*game.width, 2f)
                    )
            ));
        }
        logo.updateSprite();

        stage.getBatch().begin();
        if (isAbout) drawText();
        stage.getBatch().end();

        if (isAbout) {
            start.get().setVisible(false);
            exit.get().setVisible(false);
        } else {
            start.get().setVisible(true);
            exit.get().setVisible(true);
        }

        stage.act(delta);
        stage.draw();

        if(Gdx.input.isKeyJustPressed(Input.Keys.BACK)){
            dispose();
            Gdx.app.exit();
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.HOME)){
            dispose();
            Gdx.app.exit();
        }
    }

    private void buttonInit () {
        start = new BLButton(
                game,
                0.25f*game.width,
                0.5f*game.height + 0.025f*game.width,
                0.5f*game.width,
                game.fonts.large.getFont(),
                game.textSystem.get("start_button"),
                1,
                "start_button"
        );
        start.get().addListener(new ClickListener(){
            @Override
            public void clicked (InputEvent event, float x, float y) {
                if (!obf.isActive()) {
                    game.sounds.click.play();
                    nextStart = true;
                } else {
                    start.get().setChecked(false);
                }
            }
        });
        stage.addActor(start.get());

        about = new BLButton(
                game,
                0.25f*game.width,
                0.5f*game.height - 0.275f*game.width,
                0.5f*game.width,
                game.fonts.large.getFont(),
                game.textSystem.get("about_button"),
                1,
                "about_button"
        );
        about.get().addListener(new ClickListener(){
            @Override
            public void clicked (InputEvent event, float x, float y) {
                if (!obf.isActive() && !isAbout) {
                    game.sounds.click.play();
                    about.get().addAction(Actions.moveTo(0.475f*game.width, 0.025f*game.width));
                    about.get().setText(game.textSystem.get("back_button"));
                    isAbout = true;
                    start.get().setChecked(false);
                } else if (!obf.isActive() && isAbout) {
                    game.sounds.click.play();
                    about.get().addAction(Actions.moveTo(0.25f*game.width, 0.5f*game.height - 0.275f*game.width));
                    about.get().setText(game.textSystem.get("about_button"));
                    isAbout = false;
                    start.get().setChecked(false);
                } else {
                    start.get().setChecked(false);
                }
            }
        });
        stage.addActor(about.get());

        exit = new BLButton(
                game,
                0.25f*game.width,
                0.5f*game.height - 0.575f*game.width,
                0.5f*game.width,
                game.fonts.large.getFont(),
                game.textSystem.get("exit_button"),
                1,
                "exit_button"
        );
        exit.get().addListener(new ClickListener(){
            @Override
            public void clicked (InputEvent event, float x, float y) {
                if (!obf.isActive() && !isAbout) {
                    dispose();
                    Gdx.app.exit();
                } else {
                    exit.get().setChecked(false);
                }
            }
        });
        stage.addActor(exit.get());
        logo = new AdvSprite(
                game.atlas.createSprite("logo"),
                0.125f * game.width,
                0.95f * game.height - 0.2875f * game.width,
                0.75f * game.width,
                0.1875f * game.width
        );
        stage.addActor(logo);
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
                game.textSystem.get("devs"),
                0.5f * (game.width - game.fonts.largeS.getWidth(game.textSystem.get("devs"))),
                0.5f * (game.height + game.fonts.largeS.getWidth("A"))
        );
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void pause() {
        game.sounds.mainTheme.pause();
        game.sounds.mainTheme.stop();
    }

    @Override
    public void resume() {
        if (!game.sounds.mainTheme.isPlaying()) game.sounds.mainTheme.play();
    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
    }
}
