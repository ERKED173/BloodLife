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

class Shop implements Screen {

    private Stage stage;
    private MainBL game;
    private Obfuscation obf;
    private boolean nextStart = false;

    private BLButton next;
    private BLButton leftSkin;
    private BLButton rightSkin;
    private BLButton[] skinBuy;
    private BLButton[] bonusBuy;

    private AdvSprite money;
    private AdvSprite timeBonus;
    private AdvSprite dirBonus;
    private AdvSprite[] skins;
    private AdvSprite[] gCardsSkins;
    private AdvSprite[] gCardsBonus;
    private AdvSprite[] bonusLevel;
    private int page = 0;

    private RandomXS128 rand;
    private LinkedList<AdvSprite> advSprites;

    private String[] text;

    Shop (MainBL game) {
        this.game = game;
    }

    @Override
    public void show() {
        game.sounds.mainTheme.setLooping(true);
        game.sounds.mainTheme.setVolume(0.1f);
        if (Technical.isSoundOn) game.sounds.mainTheme.play();

        rand = new RandomXS128();
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        advSprites = new LinkedList<>();
        for (int i = 0; i < rand.nextInt(10) + 20; i++) {
            addPart();
        }

        skins = new AdvSprite[10];
        for (int i = 0; i < 10; i++) {
            if (game.prefs.getBoolean("skin_" + (i + 1), false)) {
                skins[i] = new AdvSprite(
                        game.atlas.createSprite("white", i + 1),
                        0.025f*game.width + i*0.325f*game.width,
                        0.3f*game.height,
                        0.3f*game.width,
                        0.3f*game.width);
                stage.addActor(skins[i]);
            } else {
                skins[i] = new AdvSprite(
                        game.atlas.createSprite("white_unknown"),
                        0.025f*game.width + i*0.325f*game.width,
                        0.3f*game.height,
                        0.3f*game.width,
                        0.3f*game.width);
                stage.addActor(skins[i]);
            }
        }

        buttonInit();

        gCardsSkins = new AdvSprite[3];
        for (int i = 0; i < 3; i++) {
            gCardsSkins[i] = new AdvSprite(
                    game.atlas.createSprite("green_card"),
                    0.16f*game.width + i*0.35f*game.width,
                    0.38125f*game.width,
                    0.0625f * game.width,
                    0.0625f * game.width
            );
            stage.addActor(gCardsSkins[i]);
        }
        gCardsBonus = new AdvSprite[2];
        for (int i = 0; i < 2; i++) {
            gCardsBonus[i] = new AdvSprite(
                    game.atlas.createSprite("green_card"),
                    0.865f*game.width,
                    0.76f*game.height - i*0.15f*game.height,
                    0.0625f * game.width,
                    0.0625f * game.width
            );
            stage.addActor(gCardsBonus[i]);
        }
        money = new AdvSprite(
                game.atlas.createSprite("green_card"),
                0.025f * game.width,
                0.025f * game.height,
                0.1f * game.width,
                0.1f * game.width
        );
        stage.addActor(money);

        timeBonus = new AdvSprite(
                game.atlas.createSprite("time"),
                0.025f * game.width,
                0.735f * game.height,
                0.15f * game.width,
                0.15f * game.width
        );
        stage.addActor(timeBonus);
        dirBonus = new AdvSprite(
                game.atlas.createSprite("direction"),
                0.025f * game.width,
                0.585f * game.height,
                0.15f * game.width,
                0.15f * game.width
        );
        stage.addActor(dirBonus);

        bonusLevel = new AdvSprite[10];
        for (int i = 0; i < 10; ++i) {
            bonusLevel[i] = new AdvSprite(
                    game.atlas.createSprite("star"),
                    0.2215f * game.width + (i % 5) * (0.1f * game.width),
                    0.755f * game.height - (i / 5) * (0.15f * game.height),
                    0.075f * game.width,
                    0.075f * game.width
            );
            stage.addActor(bonusLevel[i]);
        }
        for (int i = 0; i < Technical.timeLevel; ++i) bonusLevel[i].setColor(Color.PINK);
        for (int i = 5; i < 5 + Technical.dirLevel; ++i) bonusLevel[i].setColor(Color.PINK);

        text = new String[7];
        text[0] = game.textSystem.get("shop_button");
        text[1] = game.textSystem.get("sikins");
        text[2] = game.textSystem.get("chika");
        text[3] = game.textSystem.get("chikaed");
        text[4] = game.textSystem.get("tm_ll");
        text[5] = game.textSystem.get("dir_ll");
        text[6] = game.textSystem.get("sikins");

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

        for (int i = 0; i < 10; i++) {
            skins[i].updateSprite();
        }
        for (int i = 0; i < 3; i++) {
            gCardsSkins[i].addAction(Actions.rotateBy(1f));
            gCardsSkins[i].setVisible(false);
            gCardsSkins[i].updateSprite();
            if (game.prefs.getBoolean("skin_" + (i + 1 + page), false)) {
                skinBuy[i].get().setText(text[2]);
            } else {
                skinBuy[i].get().setText((10 * (page + i)) + "         ");
                gCardsSkins[i].setVisible(true);
            }

            if (i + page == Technical.curSkin - 1) {
                skinBuy[i].get().setChecked(true);
                skinBuy[i].get().setText(text[3]);
            } else {
                skinBuy[i].get().setChecked(false);
            }
        }

        for (int i = 0; i < 2; ++i) {
            gCardsBonus[i].addAction(Actions.rotateBy(1f));
            gCardsBonus[i].updateSprite();
        }
        if (Technical.timeLevel == 5) gCardsBonus[0].setVisible(false);
        if (Technical.dirLevel == 5) gCardsBonus[1].setVisible(false);

        money.addAction(Actions.rotateBy(1f));
        money.updateSprite();

        timeBonus.addAction(Actions.rotateBy(1f));
        timeBonus.updateSprite();
        dirBonus.addAction(Actions.rotateBy(1f));
        dirBonus.updateSprite();

        for (int i = 0; i < 10; ++i) {
            if (i < 5) {
                if (i < Technical.timeLevel) bonusLevel[i].setColor(Color.CORAL);
            } else {
                if (i - 5 < Technical.dirLevel) bonusLevel[i].setColor(Color.CORAL);
            }
            bonusLevel[i].addAction(Actions.rotateBy(1f));
            bonusLevel[i].updateSprite();
        }

        if (Technical.timeLevel < 5)
            bonusBuy[0].get().setText(((Technical.timeLevel + 1) * 10) + "         ");
        else
            bonusBuy[0].get().setText("---");
        if (Technical.dirLevel < 5)
            bonusBuy[1].get().setText(((Technical.dirLevel + 1) * 10) + "         ");
        else
            bonusBuy[1].get().setText("---");

        stage.getBatch().begin();
        drawText();
        stage.getBatch().end();

        stage.act(delta);
        stage.draw();

        if(Gdx.input.isKeyJustPressed(Input.Keys.BACK)){
            game.prefs.putInteger("max_level", Technical.maxLevel);
            game.prefs.putInteger("current_skin", Technical.curSkin);
            game.prefs.putInteger("money", Technical.money);
            game.prefs.putInteger("time_level", Technical.timeLevel);
            game.prefs.putInteger("direction_level", Technical.dirLevel);
            game.prefs.putBoolean("is_sound_on", Technical.isSoundOn);
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
            game.prefs.flush();
            dispose();
            Gdx.app.exit();
        }
    }

    private void buttonInit () {
        next = new BLButton(
                game,
                0.675f*game.width,
                0.025f*game.width,
                0.3f * game.width,
                game.fonts.small.getFont(),
                game.textSystem.get("back_button"),
                1,
                "back_button"
        );
        next.get().addListener(new ClickListener(){
            @Override
            public void clicked (InputEvent event, float x, float y) {
                if (!obf.isActive()) {
                    if (Technical.isSoundOn) game.sounds.click.play();
                    nextStart = true;
                } else {
                    next.get().setChecked(false);
                }
            }
        });
        stage.addActor(next.get());

        leftSkin = new BLButton(
                game,
                0.025f*game.width,
                0.2f*game.width,
                0.125f * game.width,
                game.fonts.large.getFont(),
                "<",
                2,
                "left_button"
        );
        leftSkin.get().addListener(new ClickListener(){
            @Override
            public void clicked (InputEvent event, float x, float y) {
                if (!obf.isActive() && page > 0) {
                    if (Technical.isSoundOn) game.sounds.click.play();
                    page--;
                    for (int i = 0; i < 10; i++) {
                        skins[i].addAction(Actions.moveBy(0.325f*game.width, 0f, 1f));
                    }
                    leftSkin.get().setChecked(false);
                } else {
                    leftSkin.get().setChecked(false);
                }
            }
        });
        stage.addActor(leftSkin.get());

        rightSkin = new BLButton(
                game,
                0.85f*game.width,
                0.2f*game.width,
                0.125f * game.width,
                game.fonts.large.getFont(),
                ">",
                2,
                "right_button"
        );
        rightSkin.get().addListener(new ClickListener(){
            @Override
            public void clicked (InputEvent event, float x, float y) {
                if (!obf.isActive() && page < 7) {
                    if (Technical.isSoundOn) game.sounds.click.play();
                    page++;
                    for (int i = 0; i < 10; i++) {
                        skins[i].addAction(Actions.moveBy(-0.325f*game.width, 0f, 1f));
                    }
                    rightSkin.get().setChecked(false);
                } else {
                    rightSkin.get().setChecked(false);
                }
            }
        });
        stage.addActor(rightSkin.get());

        skinBuy = new BLButton[3];
        for (int i = 0; i < 3; i++) {
            skinBuy[i] = new BLButton(
                    game,
                    0.025f*game.width + i*0.35f*game.width,
                    0.35f*game.width,
                    0.25f * game.width,
                    game.fonts.exSmall.getFont(),
                    "null_" + (i + 1),
                    1,
                    "skin_buy_button_" + i
            );
            final int TEMP_I = i;
            skinBuy[i].get().addListener(new ClickListener() {
                @Override
                public void clicked (InputEvent event, float x, float y) {
                    if (!obf.isActive()) {
                        skinBuy[TEMP_I].get().setChecked(false);
                        if (game.prefs.getBoolean("skin_" + (page + TEMP_I + 1), false)) {
                            if (Technical.isSoundOn) game.sounds.click.play();
                            Technical.curSkin = page + TEMP_I + 1;
                        } else {
                            if (Technical.money  >= 10 * (page  + TEMP_I)) {
                                if (Technical.isSoundOn) game.sounds.cash.play();
                                Technical.money -= 10 * (page  + TEMP_I);
                                game.prefs.putBoolean("skin_" + (page  + TEMP_I + 1), true);
                                game.prefs.flush();
                                skins[page  + TEMP_I].sprite = game.atlas.createSprite("white", page + TEMP_I + 1);
                                Technical.curSkin = page + TEMP_I + 1;
                            }
                        }
                    } else {
                        skinBuy[TEMP_I].get().setChecked(false);
                    }
                }
            });
            stage.addActor(skinBuy[i].get());
        }
        bonusBuy = new BLButton[2];
        bonusBuy[0] = new BLButton(
                game,
                0.725f*game.width,
                0.7425f*game.height,
                0.25f * game.width,
                game.fonts.exSmall.getFont(),
                "null",
                1,
                "bonus_buy_button_1"
        );
        bonusBuy[0].get().addListener(new ClickListener(){
            @Override
            public void clicked (InputEvent event, float x, float y) {
                if (!obf.isActive()) {
                    if (Technical.timeLevel < 5 && Technical.money >= (Technical.timeLevel + 1) * 10) {
                        if (Technical.isSoundOn) game.sounds.cash.play();
                        Technical.money -= (Technical.timeLevel + 1) * 10;
                        Technical.timeLevel++;
                    }
                    bonusBuy[0].get().setChecked(false);
                } else {
                    bonusBuy[0].get().setChecked(false);
                }
            }
        });
        stage.addActor(bonusBuy[0].get());
        bonusBuy[1] = new BLButton(
                game,
                0.725f*game.width,
                0.5925f*game.height,
                0.25f * game.width,
                game.fonts.exSmall.getFont(),
                "null",
                1,
                "bonus_buy_button_2"
        );
        bonusBuy[1].get().addListener(new ClickListener(){
            @Override
            public void clicked (InputEvent event, float x, float y) {
                if (!obf.isActive()) {
                    if (Technical.dirLevel < 5 && Technical.money >= (Technical.dirLevel + 1) * 10) {
                        if (Technical.isSoundOn) game.sounds.cash.play();
                        Technical.money -= (Technical.dirLevel + 1) * 10;
                        Technical.dirLevel++;
                    }
                    bonusBuy[1].get().setChecked(false);
                } else {
                    bonusBuy[1].get().setChecked(false);
                }
            }
        });
        stage.addActor(bonusBuy[1].get());
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
                text[0],
                0.5f*(game.width - (game.fonts.largeS.getWidth(text[0]))),
                0.95f*game.height
        );
        game.fonts.mediumS.draw(
                stage.getBatch(),
                ": " + Technical.money,
                0.15f*game.width,
                0.025f*game.height + 0.05f*game.width + 0.5f*game.fonts.mediumS.getHeight("A")
        );
        game.fonts.smallS.draw(
                stage.getBatch(),
                text[4] + ":",
                0.025f*game.width,
                0.825f*game.height + 0.05f*game.width + 0.5f*game.fonts.smallS.getHeight("A")
        );
        game.fonts.smallS.draw(
                stage.getBatch(),
                text[5] + ":",
                0.025f*game.width,
                0.675f*game.height + 0.05f*game.width + 0.5f*game.fonts.smallS.getHeight("A")
        );
        game.fonts.mediumS.draw(
                stage.getBatch(),
                text[6],
                0.5f*(game.width - (game.fonts.mediumS.getWidth(text[6]))),
                0.535f*game.height
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
        game.prefs.flush();
        if (game.sounds.mainTheme.isPlaying()) {
            game.sounds.mainTheme.pause();
            game.sounds.mainTheme.stop();
        }
    }

    @Override
    public void resume() {
        if (!game.sounds.mainTheme.isPlaying()) if (Technical.isSoundOn) game.sounds.mainTheme.play();
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
        game.prefs.flush();
    }
}
