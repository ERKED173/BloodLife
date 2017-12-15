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
    private boolean nextRecords = false;
    private boolean nextShop = false;
    private boolean isAbout = false;
    private boolean isLevel = false;

    private BLButton exit;
    private BLButton start;
    private BLButton about;
    private BLButton right;
    private BLButton left;
    private BLButton sound;
    private BLButton music;
    private BLButton records;
    private BLButton shop;

    private BLButton endless;

    private BLButton[] levels;
    private static int page = 0;

    private int curLevel;
    private AdvSprite[] stars;

    private String[] text;

    private RandomXS128 rand;
    private LinkedList<AdvSprite> advSprites;
    private AdvSprite logo;

    Menu (MainBL game, boolean isLevel) {
        this.game = game;
        this.isLevel = isLevel;
    }

    @Override
    public void show() {
        game.sounds.mainTheme.setLooping(true);
        game.sounds.mainTheme.setVolume(0.1f);
        if (Technical.isMusicOn) game.sounds.mainTheme.play();

        rand = new RandomXS128();
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        advSprites = new LinkedList<>();
        for (int i = 0; i < rand.nextInt(10) + 20; i++) {
            addPart();
        }

        buttonInit();

        float sizeXY = (0.8f*game.width + 0.4f*game.height) / 8.25f;
        float edgeMerge = (game.width - (3.5f*sizeXY)) / 2f;
        stars = new AdvSprite[30];
        for (int i = 0; i < 27; ++i) {
            stars[i] = new AdvSprite(
                    game.atlas.createSprite("star"),
                    edgeMerge + (i % 3)*0.35f*sizeXY + ((i % 9)/3)*1.25f*sizeXY,
                    0.64f*game.height - (i / 9)*1.25f*sizeXY,
                    0.3f * sizeXY,
                    0.3f * sizeXY
            );
            stage.addActor(stars[i]);
        }
        for (int i = 27; i < 30; ++i) {
            stars[i] = new AdvSprite(
                    game.atlas.createSprite("star"),
                    edgeMerge + ((i + 3) % 3)*0.35f*sizeXY + (((i + 3) % 9)/3)*1.25f*sizeXY,
                    0.64f*game.height - ((i + 3) / 9)*1.25f*sizeXY,
                    0.3f * sizeXY,
                    0.3f * sizeXY
            );
            stage.addActor(stars[i]);
        }

        text = new String[11];
        text[0] = game.textSystem.get("devs");
        text[1] = game.textSystem.get("my_name");
        text[2] = game.textSystem.get("cnt");
        text[3] = game.textSystem.get("my_mail");
        text[4] = game.textSystem.get("versus");
        text[5] = game.textSystem.get("music_1");
        text[6] = game.textSystem.get("music_2");
        text[7] = game.textSystem.get("music_3");
        text[8] = game.textSystem.get("music_4");
        text[9] = game.textSystem.get("music_5");
        text[10] = game.textSystem.get("music_6");

        obf = new Obfuscation(game.atlas.createSprite("obfuscation"), true);
        stage.addActor(obf);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(220f/255f, 150f/255f, 180f/255f, 0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if(obf.isActive() && !nextStart && !nextRecords && !nextShop){
            obf.deactivate(0.5f, delta);
        } else if (nextStart) {
            if (obf.isActive()) {
                if (curLevel != 101)
                    game.setScreen(new Tutorial(game, curLevel));
                else
                    game.setScreen(new Space(game, curLevel));
            } else {
                obf.activate(0.5f, delta);
            }
        } else if (nextRecords) {
            if (obf.isActive()) {
                game.setScreen(new Records(game));
            } else {
                obf.activate(0.5f, delta);
            }
        } else if (nextShop) {
            if (obf.isActive()) {
                game.setScreen(new Shop(game));
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
            for (AdvSprite s : stars) s.setVisible(false);
            start.get().setVisible(false);
            exit.get().setVisible(false);
            left.get().setVisible(false);
            records.get().setVisible(false);
            shop.get().setVisible(false);
            endless.get().setVisible(false);
            right.get().setVisible(false);
            sound.get().setVisible(true);
            music.get().setVisible(true);
        } else if (isLevel) {
            for (int i = 0; i < levels.length; i++) levels[i].get().setText((page * 10 + (i + 1)) + "");
            for (int i = 0; i < 10; ++i) {
                for (int j = 0; j < 3; ++j) stars[i * 3 + j].setColor(Color.LIGHT_GRAY);
                for (int j = 0; j < game.prefs.getInteger("level_star_" + ((i + 1) + 10 * page), 0); ++j) {
                    stars[i * 3 + j].setColor(Color.YELLOW);
                }
            }
            for (int i = 0; i < 30; ++i) { stars[i].updateSprite(); }
            if (page == Technical.maxLevel / 10) {
                for (int i = 0; i < (Technical.maxLevel % 10); i++) {
                    levels[i].get().setColor(Color.WHITE);
                    levels[i].get().setDisabled(false);
                }
                for (int i = (Technical.maxLevel % 10); i < levels.length; i++) {
                    levels[i].get().setColor(Color.GRAY);
                    levels[i].get().setDisabled(true);
                }
            } else if (page < Technical.maxLevel / 10) {
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
                    if (Technical.isSoundOn) game.sounds.click.play();
                    level.get().setChecked(false);
                    curLevel = Integer.parseInt(String.valueOf(level.get().getText()));
                    nextStart = true;
                }
            }
            for (BLButton b : levels) b.get().setVisible(true);
            for (AdvSprite s : stars) s.setVisible(true);
            about.get().setVisible(false);
            sound.get().setVisible(false);
            music.get().setVisible(false);
            exit.get().setVisible(false);
            left.get().setVisible(true);
            records.get().setVisible(true);
            shop.get().setVisible(true);
            if (Technical.maxLevel > 100) endless.get().setVisible(true);
            right.get().setVisible(true);
        } else {
            for (BLButton b : levels) b.get().setVisible(false);
            for (AdvSprite s : stars) s.setVisible(false);
            left.get().setVisible(false);
            records.get().setVisible(false);
            shop.get().setVisible(false);
            endless.get().setVisible(false);
            right.get().setVisible(false);
            sound.get().setVisible(false);
            music.get().setVisible(false);
            start.get().setVisible(true);
            about.get().setVisible(true);
            exit.get().setVisible(true);
        }

        stage.act(delta);
        stage.draw();

        if(Gdx.input.isKeyJustPressed(Input.Keys.BACK)){
            game.prefs.putInteger("max_level", Technical.maxLevel);
            game.prefs.putInteger("current_skin", Technical.curSkin);
            game.prefs.putInteger("money", Technical.money);
            game.prefs.putInteger("time_level", Technical.timeLevel);
            game.prefs.putInteger("direction_level", Technical.dirLevel);
            game.prefs.putBoolean("is_sound_on", Technical.isSoundOn);
            game.prefs.putBoolean("is_music_on", Technical.isMusicOn);
            game.prefs.flush();
            dispose();
            Gdx.app.exit();
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.MENU)){
            game.prefs.putInteger("max_level", Technical.maxLevel);
            game.prefs.putInteger("current_skin", Technical.curSkin);
            game.prefs.putInteger("money", Technical.money);
            game.prefs.putInteger("time_level", Technical.timeLevel);
            game.prefs.putInteger("direction_level", Technical.dirLevel);
            game.prefs.putBoolean("is_sound_on", Technical.isSoundOn);
            game.prefs.putBoolean("is_music_on", Technical.isMusicOn);
            game.prefs.flush();
            dispose();
            Gdx.app.exit();
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.HOME)){
            game.prefs.putInteger("max_level", Technical.maxLevel);
            game.prefs.putInteger("current_skin", Technical.curSkin);
            game.prefs.putInteger("money", Technical.money);
            game.prefs.putInteger("time_level", Technical.timeLevel);
            game.prefs.putInteger("direction_level", Technical.dirLevel);
            game.prefs.putBoolean("is_sound_on", Technical.isSoundOn);
            game.prefs.putBoolean("is_music_on", Technical.isMusicOn);
            game.prefs.flush();
            dispose();
            Gdx.app.exit();
        }
    }

    private void buttonInit () {
        if (isLevel) {
            start = new BLButton(
                    game,
                    0.675f*game.width,
                    0.025f*game.width,
                    0.3f * game.width,
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
                    if (Technical.isSoundOn) game.sounds.click.play();
                    page = 0;
                    start.get().setSize(0.3f*game.width, 0.15f*game.width);
                    start.get().addAction(Actions.moveTo(0.675f*game.width, 0.025f*game.width));
                    TextButton.TextButtonStyle style = start.get().getStyle();
                    style.font = game.fonts.small.getFont();
                    start.get().setStyle(style);
                    start.get().setText(game.textSystem.get("back_button"));
                    isLevel = true;
                    start.get().setChecked(false);
                } else if (!obf.isActive() && isLevel) {
                    if (Technical.isSoundOn) game.sounds.click.play();
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
                    if (Technical.isSoundOn) game.sounds.click.play();
                    about.get().setSize(0.3f*game.width, 0.15f*game.width);
                    about.get().addAction(Actions.moveTo(0.675f*game.width, 0.025f*game.width));
                    TextButton.TextButtonStyle style = about.get().getStyle();
                    style.font = game.fonts.small.getFont();
                    about.get().setStyle(style);
                    about.get().setText(game.textSystem.get("back_button"));
                    isAbout = true;
                    about.get().setChecked(false);
                } else if (!obf.isActive() && isAbout) {
                    if (Technical.isSoundOn) game.sounds.click.play();
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
                0.025f*game.width,
                0.025f*game.width,
                0.3f*game.width,
                game.fonts.small.getFont(),
                game.textSystem.get("sound_button"),
                1,
                "sound_button"
        );
        if (Technical.isSoundOn) {
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
                    if (Technical.isSoundOn) game.sounds.click.play();
                    if (sound.get().getColor().equals(Color.GRAY)) {
                        Technical.isSoundOn = true;
                        sound.get().setChecked(true);
                        sound.get().setColor(Color.WHITE);
                    } else {
                        Technical.isSoundOn = false;
                        sound.get().setChecked(false);
                        sound.get().setColor(Color.GRAY);
                    }
                } else {
                    sound.get().setChecked(false);
                }
            }
        });
        stage.addActor(sound.get());

        music = new BLButton(
                game,
                0.025f*game.width,
                0.2f*game.width,
                0.3f*game.width,
                game.fonts.small.getFont(),
                game.textSystem.get("music_button"),
                1,
                "music_button"
        );
        if (Technical.isMusicOn) {
            music.get().setChecked(true);
            music.get().setColor(Color.WHITE);
        } else {
            music.get().setChecked(false);
            music.get().setColor(Color.GRAY);
        }
        music.get().addListener(new ClickListener(){
            @Override
            public void clicked (InputEvent event, float x, float y) {
                if (!obf.isActive() && isAbout) {
                    if (Technical.isMusicOn) game.sounds.click.play();
                    if (music.get().getColor().equals(Color.GRAY)) {
                        Technical.isMusicOn = true;
                        game.sounds.mainTheme.play();
                        music.get().setChecked(true);
                        music.get().setColor(Color.WHITE);
                    } else {
                        Technical.isMusicOn = false;
                        game.sounds.mainTheme.stop();
                        music.get().setChecked(false);
                        music.get().setColor(Color.GRAY);
                    }
                } else {
                    music.get().setChecked(false);
                }
            }
        });
        stage.addActor(music.get());

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

        levels = new BLButton[10];
        float sizeXY = (0.8f*game.width + 0.4f*game.height) / 8.25f;
        float edgeMerge = (game.width - (3.5f*sizeXY)) / 2f;
        for (int i = 0; i < 9; i++) {
            levels[i] = new BLButton(
                    game,
                    edgeMerge + (i % 3)*1.25f*sizeXY,
                    0.65f*game.height - (i / 3)*1.25f*sizeXY,
                    sizeXY,
                    game.fonts.small.getFont(),
                    (i + 1) + "",
                    2,
                    "level_" + (i + 1)
            );
            stage.addActor(levels[i].get());
        }
        levels[9] = new BLButton(
                game,
                edgeMerge + 1.25f*sizeXY,
                0.65f*game.height - (9 / 3)*1.25f*sizeXY,
                sizeXY,
                game.fonts.small.getFont(),
                (9 + 1) + "",
                2,
                "level_" + (9 + 1)
        );
        stage.addActor(levels[9].get());

        left = new BLButton(
                game,
                edgeMerge,
                0.65f*game.height - 3f*1.25f*sizeXY,
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
                    if (Technical.isSoundOn) game.sounds.click.play();
                } else {
                    left.get().setChecked(false);
                }
            }
        });
        stage.addActor(left.get());
        right = new BLButton(
                game,
                game.width - edgeMerge - sizeXY,
                0.65f*game.height - 3f*1.25f*sizeXY,
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
                    if (Technical.isSoundOn) game.sounds.click.play();
                } else {
                    right.get().setChecked(false);
                }
            }
        });
        stage.addActor(right.get());
        records = new BLButton(
                game,
                0.35f*game.width,
                0.025f * game.width,
                0.3f * game.width,
                game.fonts.small.getFont(),
                game.textSystem.get("rcrds"),
                1,
                "records_button"
        );
        records.get().addListener(new ClickListener(){
            @Override
            public void clicked (InputEvent event, float x, float y) {
                if (!obf.isActive() && isLevel) {
                    nextRecords = true;
                    if (Technical.isSoundOn) game.sounds.click.play();
                } else {
                    records.get().setChecked(false);
                }
            }
        });
        stage.addActor(records.get());
        shop = new BLButton(
                game,
                0.025f*game.width,
                0.025f * game.width,
                0.3f * game.width,
                game.fonts.small.getFont(),
                game.textSystem.get("shop_button"),
                1,
                "shop_button"
        );
        shop.get().addListener(new ClickListener(){
            @Override
            public void clicked (InputEvent event, float x, float y) {
                if (!obf.isActive() && isLevel) {
                    nextShop = true;
                    if (Technical.isSoundOn) game.sounds.click.play();
                } else {
                    shop.get().setChecked(false);
                }
            }
        });
        stage.addActor(shop.get());
        endless = new BLButton(
                game,
                0.5f*(game.width - 2f * sizeXY),
                0.65f*game.height - 4f*1.25f*sizeXY,
                2f * sizeXY,
                game.fonts.exxSmall.getFont(),
                game.textSystem.get("endline"),
                1,
                "endless_button"
        );
        endless.get().addListener(new ClickListener(){
            @Override
            public void clicked (InputEvent event, float x, float y) {
                if (!obf.isActive() && isLevel) {
                    if (Technical.isSoundOn) game.sounds.click.play();
                    curLevel = 101;
                    nextStart = true;
                } else {
                    endless.get().setChecked(false);
                }
            }
        });
        stage.addActor(endless.get());
        endless.get().setVisible(false);
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
        game.fonts.mediumS.draw(
                stage.getBatch(),
                text[0],
                0.5f * (game.width - game.fonts.mediumS.getWidth(text[0])),
                0.5f * (game.height + 14.0f*game.fonts.mediumS.getHeight("A"))
        );
        game.fonts.mediumS.draw(
                stage.getBatch(),
                text[1],
                0.5f * (game.width - game.fonts.mediumS.getWidth(text[1])),
                0.5f * (game.height + 10.0f*game.fonts.mediumS.getHeight("A"))
        );
        game.fonts.mediumS.draw(
                stage.getBatch(),
                text[2],
                0.5f * (game.width - game.fonts.mediumS.getWidth(text[2])),
                0.5f * (game.height + 3.0f*game.fonts.mediumS.getHeight("A"))
        );
        game.fonts.mediumS.draw(
                stage.getBatch(),
                text[3],
                0.5f * (game.width - game.fonts.mediumS.getWidth(text[3])),
                0.5f * (game.height - 1.0f*game.fonts.mediumS.getHeight("A"))
        );
        game.fonts.smallS.draw(
                stage.getBatch(),
                text[4],
                0.5f * (game.width - game.fonts.smallS.getWidth(text[4])),
                0.025f*game.width + 1.25f*game.fonts.smallS.getHeight("A")
        );
        game.fonts.smallS.draw(
                stage.getBatch(),
                text[5],
                0.5f * (game.width - game.fonts.smallS.getWidth(text[5])),
                0.5f * (game.height - 8.0f*game.fonts.smallS.getHeight("A"))
        );
        game.fonts.smallS.draw(
                stage.getBatch(),
                text[6],
                0.5f * (game.width - game.fonts.smallS.getWidth(text[6])),
                0.5f * (game.height - 11.0f*game.fonts.smallS.getHeight("A"))
        );
        game.fonts.smallS.draw(
                stage.getBatch(),
                text[7],
                0.5f * (game.width - game.fonts.smallS.getWidth(text[7])),
                0.5f * (game.height - 14.0f*game.fonts.smallS.getHeight("A"))
        );
        game.fonts.exSmallS.draw(
                stage.getBatch(),
                text[8],
                0.5f * (game.width - game.fonts.exSmallS.getWidth(text[8])),
                0.5f * (game.height - 19.0f*game.fonts.exSmallS.getHeight("A"))
        );
        game.fonts.exSmallS.draw(
                stage.getBatch(),
                text[9],
                0.5f * (game.width - game.fonts.exSmallS.getWidth(text[9])),
                0.5f * (game.height - 22.0f*game.fonts.exSmallS.getHeight("A"))
        );
        game.fonts.exxSmallS.draw(
                stage.getBatch(),
                text[10],
                0.5f * (game.width - game.fonts.exxSmallS.getWidth(text[10])),
                0.5f * (game.height - 25.0f*game.fonts.exSmallS.getHeight("A"))
        );
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void pause() {
        game.prefs.putInteger("max_level", Technical.maxLevel);
        game.prefs.putInteger("current_skin", Technical.curSkin);
        game.prefs.putInteger("money", Technical.money);
        game.prefs.putInteger("time_level", Technical.timeLevel);
        game.prefs.putInteger("direction_level", Technical.dirLevel);
        game.prefs.putBoolean("is_sound_on", Technical.isSoundOn);
        game.prefs.putBoolean("is_music_on", Technical.isMusicOn);
        game.prefs.flush();
        if (game.sounds.mainTheme.isPlaying()) {
            game.sounds.mainTheme.pause();
            game.sounds.mainTheme.stop();
        }
    }

    @Override
    public void resume() {
        if (!game.sounds.mainTheme.isPlaying()) if (Technical.isMusicOn) game.sounds.mainTheme.play();
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        game.prefs.putInteger("max_level", Technical.maxLevel);
        game.prefs.putInteger("current_skin", Technical.curSkin);
        game.prefs.putInteger("money", Technical.money);
        game.prefs.putInteger("time_level", Technical.timeLevel);
        game.prefs.putInteger("direction_level", Technical.dirLevel);
        game.prefs.putBoolean("is_sound_on", Technical.isSoundOn);
        game.prefs.putBoolean("is_music_on", Technical.isMusicOn);
        game.prefs.flush();
    }
}
