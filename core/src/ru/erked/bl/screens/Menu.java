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
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import java.util.LinkedList;

import ru.erked.bl.MainBL;
import ru.erked.bl.systems.BLButton;
import ru.erked.bl.utils.AdvSprite;
import ru.erked.bl.utils.Obfuscation;

class Menu implements Screen {

    private Stage stage;
    private MainBL game;
    private Obfuscation obf;
    private boolean nextStart = false;
    private boolean isAbout = false;
    private boolean isLevel = false;

    private BLButton exit;
    private BLButton start;
    private BLButton about;
    private BLButton right;
    private BLButton left;
    private BLButton sound;

    private BLButton cheat;

    private BLButton[] levels;
    private static int page = 0;

    static boolean isSoundOn = true;
    static int maxLevel = 1;
    private int curLevel;

    private RandomXS128 rand;
    private LinkedList<AdvSprite> advSprites;
    private AdvSprite logo;

    Menu (MainBL game, boolean isLevel) {
        this.game = game;
        this.isLevel = isLevel;
    }

    @Override
    public void show() {
        maxLevel = game.prefs.getInteger("max_level", 1);
        isSoundOn = game.prefs.getBoolean("is_sound_on", true);

        game.sounds.mainTheme.setLooping(true);
        game.sounds.mainTheme.setVolume(0.25f);
        if (isSoundOn) game.sounds.mainTheme.play();

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
            obf.deactivate(0.5f, delta);
        } else if (nextStart) {
            if (obf.isActive()) {
                game.setScreen(new Tutorial(game, curLevel));
            } else {
                obf.activate(0.5f, delta);
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
            for (BLButton b : levels) b.get().setVisible(false);
            start.get().setVisible(false);
            exit.get().setVisible(false);
            left.get().setVisible(false);
            cheat.get().setVisible(false);
            right.get().setVisible(false);
            sound.get().setVisible(true);
        } else if (isLevel) {
            for (int i = 0; i < levels.length; i++) levels[i].get().setText((page*20 + (i + 1)) + "");
            if (page == maxLevel / 20) {
                for (int i = 0; i < (maxLevel % 20); i++) {
                    levels[i].get().setColor(Color.WHITE);
                    levels[i].get().setDisabled(false);
                }
                for (int i = (maxLevel % 20); i < levels.length; i++) {
                    levels[i].get().setColor(Color.GRAY);
                    levels[i].get().setDisabled(true);
                }
            } else if (page < maxLevel / 20) {
                for (BLButton level : levels) {
                    level.get().setColor(Color.WHITE);
                    level.get().setDisabled(false);
                }
            } else {
                for (BLButton level : levels) {
                    level.get().setColor(Color.GRAY);
                    level.get().setDisabled(true);
                }
            }
            for (BLButton level : levels) {
                if (!obf.isActive() && level.get().isChecked()) {
                    if (Menu.isSoundOn) game.sounds.click.play();
                    level.get().setChecked(false);
                    curLevel = Integer.parseInt(String.valueOf(level.get().getText()));
                    nextStart = true;
                }
            }
            for (BLButton b : levels) b.get().setVisible(true);
            about.get().setVisible(false);
            sound.get().setVisible(false);
            exit.get().setVisible(false);
            left.get().setVisible(true);
            cheat.get().setVisible(true);
            right.get().setVisible(true);
        } else {
            for (BLButton b : levels) b.get().setVisible(false);
            left.get().setVisible(false);
            cheat.get().setVisible(false);
            right.get().setVisible(false);
            sound.get().setVisible(false);
            start.get().setVisible(true);
            about.get().setVisible(true);
            exit.get().setVisible(true);
        }

        stage.act(delta);
        stage.draw();

        if(Gdx.input.isKeyJustPressed(Input.Keys.BACK)){
            game.prefs.putInteger("max_level", maxLevel);
            game.prefs.putBoolean("is_sound_on", isSoundOn);
            game.prefs.flush();
            dispose();
            Gdx.app.exit();
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.HOME)){
            game.prefs.putInteger("max_level", maxLevel);
            game.prefs.putBoolean("is_sound_on", isSoundOn);
            game.prefs.flush();
            dispose();
            Gdx.app.exit();
        }
    }

    private void buttonInit () {
        if (isLevel) {
            start = new BLButton(
                    game,
                    0.725f*game.width,
                    0.025f*game.width,
                    0.25f * game.width,
                    game.fonts.small.getFont(),
                    game.textSystem.get("back_button"),
                    1,
                    "start_button"
            );
        } else {
            start = new BLButton(
                    game,
                    0.25f * game.width,
                    0.5f * game.height + 0.025f * game.width,
                    0.5f * game.width,
                    game.fonts.large.getFont(),
                    game.textSystem.get("start_button"),
                    1,
                    "start_button"
            );
        }
        start.get().addListener(new ClickListener(){
            @Override
            public void clicked (InputEvent event, float x, float y) {
                if (!obf.isActive() && !isLevel) {
                    if (Menu.isSoundOn) game.sounds.click.play();
                    page = 0;
                    start.get().setSize(0.25f*game.width, 0.125f*game.width);
                    start.get().addAction(Actions.moveTo(0.725f*game.width, 0.025f*game.width));
                    TextButton.TextButtonStyle style = start.get().getStyle();
                    style.font = game.fonts.small.getFont();
                    start.get().setStyle(style);
                    start.get().setText(game.textSystem.get("back_button"));
                    isLevel = true;
                    start.get().setChecked(false);
                } else if (!obf.isActive() && isLevel) {
                    if (Menu.isSoundOn) game.sounds.click.play();
                    start.get().setSize(0.5f*game.width, 0.25f*game.width);
                    start.get().addAction(Actions.moveTo(0.25f*game.width, 0.5f*game.height + 0.025f*game.width));
                    TextButton.TextButtonStyle style = start.get().getStyle();
                    style.font = game.fonts.large.getFont();
                    start.get().setStyle(style);
                    start.get().setText(game.textSystem.get("start_button"));
                    isLevel = false;
                    start.get().setChecked(false);
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
                    if (Menu.isSoundOn) game.sounds.click.play();
                    about.get().setSize(0.25f*game.width, 0.125f*game.width);
                    about.get().addAction(Actions.moveTo(0.725f*game.width, 0.025f*game.width));
                    TextButton.TextButtonStyle style = about.get().getStyle();
                    style.font = game.fonts.small.getFont();
                    about.get().setStyle(style);
                    about.get().setText(game.textSystem.get("back_button"));
                    isAbout = true;
                    about.get().setChecked(false);
                } else if (!obf.isActive() && isAbout) {
                    if (Menu.isSoundOn) game.sounds.click.play();
                    about.get().setSize(0.5f*game.width, 0.25f*game.width);
                    about.get().addAction(Actions.moveTo(0.25f*game.width, 0.5f*game.height - 0.275f*game.width));
                    TextButton.TextButtonStyle style = about.get().getStyle();
                    style.font = game.fonts.large.getFont();
                    about.get().setStyle(style);
                    about.get().setText(game.textSystem.get("about_button"));
                    isAbout = false;
                    about.get().setChecked(false);
                } else {
                    about.get().setChecked(false);
                }
            }
        });
        stage.addActor(about.get());

        sound = new BLButton(
                game,
                0.725f*game.width,
                0.175f*game.width,
                0.25f*game.width,
                game.fonts.small.getFont(),
                game.textSystem.get("sound_button"),
                1,
                "sound_button"
        );
        if (isSoundOn) {
            sound.get().setChecked(true);
            sound.get().setColor(Color.WHITE);
        } else {
            sound.get().setChecked(false);
            sound.get().setColor(Color.GRAY);
        }
        sound.get().addListener(new ClickListener(){
            @Override
            public void clicked (InputEvent event, float x, float y) {
                if (!obf.isActive() && isAbout) {
                    if (Menu.isSoundOn) game.sounds.click.play();
                    if (sound.get().getColor().equals(Color.GRAY)) {
                        isSoundOn = true;
                        game.sounds.mainTheme.play();
                        sound.get().setChecked(true);
                        sound.get().setColor(Color.WHITE);
                    } else {
                        isSoundOn = false;
                        game.sounds.mainTheme.stop();
                        sound.get().setChecked(false);
                        sound.get().setColor(Color.GRAY);
                    }
                } else {
                    sound.get().setChecked(false);
                }
            }
        });
        stage.addActor(sound.get());

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

        levels = new BLButton[20];
        float sizeXY = (0.8f*game.width + 0.4f*game.height) / 10.75f;
        float edgeMerge = (game.width - (4.75f*sizeXY)) / 2f;
        for (int i = 0; i < 20; i++) {
            levels[i] = new BLButton(
                    game,
                    edgeMerge + (i % 4)*1.25f*sizeXY,
                    0.65f*game.height - (i / 4)*1.25f*sizeXY,
                    sizeXY,
                    game.fonts.small.getFont(),
                    (i + 1) + "",
                    2,
                    "level_" + (i + 1)
            );
            stage.addActor(levels[i].get());
        }
        left = new BLButton(
                game,
                edgeMerge,
                0.65f*game.height - 5f*1.25f*sizeXY,
                sizeXY,
                game.fonts.large.getFont(),
                "<",
                2,
                "left_button"
        );
        left.get().addListener(new ClickListener(){
            @Override
            public void clicked (InputEvent event, float x, float y) {
                if (!obf.isActive() && isLevel && page > 0) {
                    page--;
                    left.get().setChecked(false);
                    if (Menu.isSoundOn) game.sounds.click.play();
                } else {
                    left.get().setChecked(false);
                }
            }
        });
        stage.addActor(left.get());
        right = new BLButton(
                game,
                game.width - edgeMerge - sizeXY,
                0.65f*game.height - 5f*1.25f*sizeXY,
                sizeXY,
                game.fonts.large.getFont(),
                ">",
                2,
                "right_button"
        );
        right.get().addListener(new ClickListener(){
            @Override
            public void clicked (InputEvent event, float x, float y) {
                if (!obf.isActive() && isLevel && page < 9) {
                    page++;
                    right.get().setChecked(false);
                    if (Menu.isSoundOn) game.sounds.click.play();
                } else {
                    right.get().setChecked(false);
                }
            }
        });
        stage.addActor(right.get());

        //
        cheat = new BLButton(
                game,
                0.5f*(game.width - sizeXY),
                0.65f*game.height - 5f*1.25f*sizeXY,
                sizeXY,
                game.fonts.large.getFont(),
                "+",
                2,
                "cheat_button"
        );
        cheat.get().addListener(new ClickListener(){
            @Override
            public void clicked (InputEvent event, float x, float y) {
                if (!obf.isActive() && isLevel) {
                    maxLevel++;
                    cheat.get().setChecked(false);
                    if (Menu.isSoundOn) game.sounds.click.play();
                } else {
                    cheat.get().setChecked(false);
                }
            }
        });
        stage.addActor(cheat.get());
        //
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
                0.5f * (game.height + 10.0f*game.fonts.largeS.getWidth("A"))
        );
        game.fonts.largeS.draw(
                stage.getBatch(),
                game.textSystem.get("my_name"),
                0.5f * (game.width - game.fonts.largeS.getWidth(game.textSystem.get("my_name"))),
                0.5f * (game.height + 6.0f*game.fonts.largeS.getWidth("A"))
        );
        game.fonts.largeS.draw(
                stage.getBatch(),
                game.textSystem.get("cnt"),
                0.5f * (game.width - game.fonts.largeS.getWidth(game.textSystem.get("cnt"))),

                0.5f * (game.height - 3.0f*game.fonts.largeS.getWidth("A"))
        );
        game.fonts.largeS.draw(
                stage.getBatch(),
                game.textSystem.get("my_mail"),
                0.5f * (game.width - game.fonts.largeS.getWidth(game.textSystem.get("my_mail"))),
                0.5f * (game.height - 7.0f*game.fonts.largeS.getWidth("A"))
        );
        game.fonts.smallS.draw(
                stage.getBatch(),
                game.textSystem.get("versus"),
                0.005f * game.width,
                1.25f*game.fonts.smallS.getWidth("A")
        );
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void pause() {
        game.prefs.putInteger("max_level", maxLevel);
        game.prefs.putBoolean("is_sound_on", isSoundOn);
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
        game.prefs.putInteger("max_level", maxLevel);
        game.prefs.putBoolean("is_sound_on", isSoundOn);
        game.prefs.flush();
    }
}
