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

class Tutorial implements Screen {

    private Stage stage;
    private MainBL game;
    private Obfuscation obf;
    private boolean nextStart = false;
    private boolean backStart = false;

    private AdvSprite finger;
    private AdvSprite player;
    private AdvSprite lymph;
    private AdvSprite virus;
    private AdvSprite redCell;
    private AdvSprite platelet;

    private BLButton next;
    private BLButton back;

    private RandomXS128 rand;
    private LinkedList<AdvSprite> advSprites;

    private int curLevel;
    private int curScore;

    private String[] text;

    Tutorial (MainBL game, int curLevel) {
        this.game = game;
        this.curLevel = curLevel;
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

        if (curLevel == 1){
            type0Init();
        } else if (curLevel == 2) {
            type1Init();
        } else if (curLevel % 10 == 0) {
            initGameBoss();
        } else {
            switch (curLevel % 4) {
                case 1: {
                    initGame1();
                    break;
                }
                case 2: {
                    initGame2();
                    break;
                }
                case 3: {
                    initGame3();
                    break;
                }
                case 0: {
                    initGame4();
                    break;
                }
            }
        }
        buttonInit();

        obf = new Obfuscation(game.atlas.createSprite("obfuscation"), true);
        stage.addActor(obf);

        curScore = game.prefs.getInteger("level_score_" + curLevel, 0);

        text = new String[10];
        text[0] = game.textSystem.get("cursk");
        text[1] = game.textSystem.get("movit");
        text[2] = game.textSystem.get("atck");
        text[3] = game.textSystem.get("fst");
        text[4] = game.textSystem.get("rescue");
        text[5] = game.textSystem.get("togh");
        text[6] = game.textSystem.get("buch");
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(220f/255f, 150f/255f, 180f/255f, 0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if(obf.isActive() && !nextStart  && !backStart){
            obf.deactivate(0.5f, delta);
        } else if (nextStart) {
            if (obf.isActive()) {
                game.setScreen(new Space(game, curLevel));
            } else {
                if (curLevel == 2) {
                    virus.addAction(Actions.alpha(0f, 0.25f));
                }
                if (curLevel > 2) {
                    switch (curLevel % 4) {
                        case 1: {
                            virus.addAction(Actions.alpha(0f, 0.25f));
                            break;
                        }
                        case 2: {
                            virus.addAction(Actions.alpha(0f, 0.25f));
                            lymph.addAction(Actions.alpha(0f, 0.25f));
                            break;
                        }
                        case 3: {

                            break;
                        }
                        case 0: {
                            virus.addAction(Actions.alpha(0f, 0.25f));
                            lymph.addAction(Actions.alpha(0f, 0.25f));
                            break;
                        }
                    }
                }
                obf.activate(0.5f, delta);
            }
        } else if (backStart) {
            if (obf.isActive()) {
                game.setScreen(new Menu(game, true));
            } else {
                if (curLevel == 2) {
                    virus.addAction(Actions.alpha(0f, 0.25f));
                }
                if (curLevel > 2) {
                    switch (curLevel % 4) {
                        case 1: {
                            virus.addAction(Actions.alpha(0f, 0.25f));
                            break;
                        }
                        case 2: {
                            virus.addAction(Actions.alpha(0f, 0.25f));
                            lymph.addAction(Actions.alpha(0f, 0.25f));
                            break;
                        }
                        case 3: {

                            break;
                        }
                        case 0: {
                            virus.addAction(Actions.alpha(0f, 0.25f));
                            lymph.addAction(Actions.alpha(0f, 0.25f));
                            break;
                        }
                    }
                }
                obf.activate(0.5f, delta);
            }
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
        next = new BLButton(
                game,
                0.675f*game.width,
                0.025f*game.width,
                0.3f * game.width,
                game.fonts.small.getFont(),
                game.textSystem.get("next_button"),
                1,
                "next_button"
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

        back = new BLButton(
                game,
                0.025f*game.width,
                0.025f*game.width,
                0.3f * game.width,
                game.fonts.small.getFont(),
                game.textSystem.get("back_button"),
                1,
                "back_button"
        );
        back.get().addListener(new ClickListener(){
            @Override
            public void clicked (InputEvent event, float x, float y) {
                if (!obf.isActive()) {
                    if (Technical.isSoundOn) game.sounds.click.play();
                    backStart = true;
                } else {
                    back.get().setChecked(false);
                }
            }
        });
        stage.addActor(back.get());
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

    private void type0Init () {
        finger = new AdvSprite(game.atlas.createSprite("finger"), 0f, 0.15f*game.height, 0.5f*game.width, 0.5f*game.width);
        player = new AdvSprite(game.atlas.createSprite("white", Technical.curSkin), 0.05f*game.width, 0.4f*game.height, 0.15f*game.width, 0.15f*game.width);

        stage.addActor(player);
        stage.addActor(finger);
    }
    private void type1Init () {
        finger = new AdvSprite(game.atlas.createSprite("finger"), 0f, 0.15f*game.height, 0.5f*game.width, 0.5f*game.width);
        player = new AdvSprite(game.atlas.createSprite("white", Technical.curSkin), 0.05f*game.width, 0.4f*game.height, 0.15f*game.width, 0.15f*game.width);
        virus = new AdvSprite(game.atlas.createSprite("virus", rand.nextInt(19) + 1), 0.8f*game.width, 0.5f*game.height, 0.15f*game.width, 0.15f*game.width);

        stage.addActor(player);
        stage.addActor(virus);
        stage.addActor(finger);
    }

    private void drawText () {
        if (curLevel == 1) {
            drawType0();
        } else if (curLevel == 2) {
            drawType1();
        } else if (curLevel % 10 == 0) {
            drawGameBoss();
        } else {
            switch (curLevel % 4) {
                case 1: {
                    drawGame1();
                    break;
                }
                case 2: {
                    drawGame2();
                    break;
                }
                case 3: {
                    drawGame3();
                    break;
                }
                case 0: {
                    drawGame4();
                    break;
                }
            }
        }
        if (curLevel > 4) {
            game.fonts.smallS.draw(
                    stage.getBatch(),
                    text[0] + ": " + curScore,
                    0.5f*(game.width - game.fonts.smallS.getWidth(text[0] + ": " + curScore)),
                    0.15f*game.height
            );
        }
    }
    private void drawType0 () {
        if (!finger.hasActions()) {
            finger.addAction(Actions.sequence(
                    Actions.moveTo(0f, 0.25f*game.height),
                    Actions.alpha(1f),
                    Actions.moveTo(0.5f*game.width, 0.3f*game.height, 1.5f),
                    Actions.alpha(0f, 0.5f)
            ));
        }
        finger.updateSprite();
        if (!player.hasActions()) {
            player.addAction(Actions.rotateBy(360f, 5f));
        }
        player.updateSprite();

        game.fonts.mediumS.draw(
                stage.getBatch(),
                text[1],
                0.5f*(game.width - game.fonts.mediumS.getWidth(text[1])),
                0.95f*game.height
        );
    }
    private void drawType1 () {
        if (!finger.hasActions()) {
            finger.addAction(Actions.sequence(
                    Actions.moveTo(0f, 0.25f*game.height),
                    Actions.alpha(1f, 0.5f),
                    Actions.moveTo(0.5f*game.width, 0.3f*game.height, 1f),
                    Actions.alpha(0f, 0.5f),
                    Actions.delay(4f)
            ));
        }
        finger.updateSprite();

        if (!player.hasActions()) {
            player.addAction(Actions.parallel(
                    Actions.rotateBy(360f, 6f),
                    Actions.sequence(
                            Actions.moveTo(0.05f*game.width, 0.4f*game.height),
                            Actions.alpha(1f, 0.5f),
                            Actions.delay(1.5f),
                            Actions.moveTo(0.65f*game.width, 0.475f*game.height, 1f),
                            Actions.delay(2f),
                            Actions.alpha(0f, 1f)
                    )
            ));
        }
        player.updateSprite();

        if (!virus.hasActions()) {
            virus.remove();
            virus = new AdvSprite(game.atlas.createSprite("virus", rand.nextInt(19) + 1), 0.8f*game.width, 0.5f*game.height, 0.15f*game.width, 0.15f*game.width);
            stage.addActor(virus);
            virus.addAction(Actions.parallel(
                    Actions.rotateBy(360f, 6f),
                    Actions.sequence(
                            Actions.alpha(1f, 0.5f),
                            Actions.delay(2.5f),
                            Actions.color(Color.RED),
                            Actions.color(Color.WHITE, 0.5f),
                            Actions.alpha(0f, 1f),
                            Actions.delay(1.5f)
                    )
            ));
        }
        virus.updateSprite();

        game.fonts.mediumS.draw(
                stage.getBatch(),
                text[2],
                0.5f*(game.width - game.fonts.mediumS.getWidth(text[2])),
                0.95f*game.height
        );
    }

    private void initGame1 () {
        finger = new AdvSprite(game.atlas.createSprite("finger"), 0f, 0.15f*game.height, 0.5f*game.width, 0.5f*game.width);
        player = new AdvSprite(game.atlas.createSprite("white", Technical.curSkin), 0.05f*game.width, 0.4f*game.height, 0.15f*game.width, 0.15f*game.width);
        virus = new AdvSprite(game.atlas.createSprite("virus", rand.nextInt(19) + 1), 0.8f*game.width, 0.5f*game.height, 0.15f*game.width, 0.15f*game.width);

        stage.addActor(player);
        stage.addActor(virus);
        stage.addActor(finger);
    }
    private void initGame2 () {
        player = new AdvSprite(game.atlas.createSprite("white", Technical.curSkin), 0.05f*game.width, 0.7f*game.height, 0.15f*game.width, 0.15f*game.width);
        lymph = new AdvSprite(game.atlas.createSprite("lymphocyte", rand.nextInt(7) + 1), 0.05f*game.width, 0.3f*game.height, 0.15f*game.width, 0.15f*game.width);
        virus = new AdvSprite(game.atlas.createSprite("virus", rand.nextInt(19) + 1), 0.8f*game.width, 0.5f*game.height, 0.15f*game.width, 0.15f*game.width);

        stage.addActor(player);
        stage.addActor(virus);
        stage.addActor(lymph);
    }
    private void initGame3 () {
        player = new AdvSprite(game.atlas.createSprite("white", Technical.curSkin), 0.05f*game.width, 0.5f*game.height, 0.15f*game.width, 0.15f*game.width);
        platelet = new AdvSprite(game.atlas.createSprite("yellow"), 0.8f*game.width, 0.7f*game.height, 0.075f*game.width, 0.075f*game.width);
        redCell = new AdvSprite(game.atlas.createSprite("red"), 0.8f*game.width, 0.3f*game.height, 0.15f*game.width, 0.15f*game.width);
        redCell.setColor(Color.GRAY);

        stage.addActor(player);
        stage.addActor(redCell);
        stage.addActor(platelet);
    }
    private void initGame4 () {
        player = new AdvSprite(game.atlas.createSprite("white", Technical.curSkin), 0.05f*game.width, 0.7f*game.height, 0.15f*game.width, 0.15f*game.width);
        lymph = new AdvSprite(game.atlas.createSprite("lymphocyte", rand.nextInt(7) + 1), 0.05f*game.width, 0.3f*game.height, 0.15f*game.width, 0.15f*game.width);
        virus = new AdvSprite(game.atlas.createSprite("virus", rand.nextInt(19) + 1), 0.8f*game.width, 0.5f*game.height, 0.15f*game.width, 0.15f*game.width);

        stage.addActor(player);
        stage.addActor(virus);
        stage.addActor(lymph);
    }
    private void initGameBoss () {
        player = new AdvSprite(game.atlas.createSprite("white", Technical.curSkin), 0.05f*game.width, 0.3f*game.height, 0.15f*game.width, 0.15f*game.width);
        lymph = new AdvSprite(game.atlas.createSprite("lymphocyte", rand.nextInt(7) + 1), 0.8f*game.width, 0.3f*game.height, 0.15f*game.width, 0.15f*game.width);
        platelet = new AdvSprite(game.atlas.createSprite("yellow"), 0.05f*game.width, 0.35f*game.height, 0.075f*game.width, 0.075f*game.width);
        virus = new AdvSprite(game.atlas.createSprite("virus", 19 + curLevel / 10), 0.25f*game.width, 0.55f*game.height, 0.5f*game.width, 0.5f*game.width);

        stage.addActor(player);
        stage.addActor(virus);
        stage.addActor(platelet);
        stage.addActor(lymph);
    }

    private void drawGame1 () {
        if (!finger.hasActions()) {
            finger.addAction(Actions.sequence(
                    Actions.moveTo(0f, 0.25f*game.height),
                    Actions.alpha(1f, 0.5f),
                    Actions.moveTo(0.5f*game.width, 0.3f*game.height, 1f),
                    Actions.alpha(0f, 0.5f),
                    Actions.delay(4f)
            ));
        }
        finger.updateSprite();

        if (!player.hasActions()) {
            player.addAction(Actions.parallel(
                    Actions.rotateBy(360f, 6f),
                    Actions.sequence(
                            Actions.moveTo(0.05f*game.width, 0.4f*game.height),
                            Actions.alpha(1f, 0.5f),
                            Actions.delay(1.5f),
                            Actions.moveTo(0.65f*game.width, 0.475f*game.height, 1f),
                            Actions.delay(2f),
                            Actions.alpha(0f, 1f)
                    )
            ));
        }
        player.updateSprite();

        if (!virus.hasActions()) {
            virus.remove();
            virus = new AdvSprite(game.atlas.createSprite("virus", rand.nextInt(19) + 1), 0.8f*game.width, 0.5f*game.height, 0.15f*game.width, 0.15f*game.width);
            stage.addActor(virus);
            virus.addAction(Actions.parallel(
                    Actions.rotateBy(360f, 6f),
                    Actions.sequence(
                            Actions.alpha(1f, 0.5f),
                            Actions.delay(2.5f),
                            Actions.color(Color.RED),
                            Actions.color(Color.WHITE, 0.5f),
                            Actions.alpha(0f, 1f),
                            Actions.delay(1.5f)
                    )
            ));
        }
        virus.updateSprite();

        game.fonts.mediumS.draw(
                stage.getBatch(),
                text[2],
                0.5f*(game.width - game.fonts.mediumS.getWidth(text[2])),
                0.95f*game.height
        );
    }
    private void drawGame2 () {
        if (!player.hasActions()) {
            player.addAction(Actions.parallel(
                    Actions.rotateBy(360f, 5f),
                    Actions.sequence(
                            Actions.moveTo(0.05f*game.width, 0.7f*game.height),
                            Actions.alpha(1f, 0.5f),
                            Actions.delay(0.5f),
                            Actions.moveTo(0.65f*game.width, 0.55f*game.height, 1f),
                            Actions.delay(1.985f),
                            Actions.alpha(0f, 1f)
                    )
            ));
        }
        if (!lymph.hasActions()) {
            lymph.addAction(Actions.parallel(
                    Actions.rotateBy(360f, 5f),
                    Actions.sequence(
                            Actions.moveTo(0.05f*game.width, 0.3f*game.height),
                            Actions.alpha(1f, 0.5f),
                            Actions.moveTo(0.65f*game.width, 0.45f*game.height, 3f),
                            Actions.delay(0.5f),
                            Actions.alpha(0f, 1f)
                    )
            ));
        }
        if (!virus.hasActions()) {
            virus.addAction(Actions.parallel(
                    Actions.rotateBy(360f, 5f),
                    Actions.sequence(
                            Actions.alpha(1f, 0.5f),
                            Actions.delay(1.5f),
                            Actions.alpha(0f, 1f),
                            Actions.delay(2.0175f)
                    )
            ));
        }

        player.updateSprite();
        lymph.updateSprite();
        virus.updateSprite();

        game.fonts.mediumS.draw(
                stage.getBatch(),
                text[3],
                0.5f*(game.width - game.fonts.mediumS.getWidth(text[3])),
                0.95f*game.height
        );
    }
    private void drawGame3 () {
        if (!player.hasActions()) {
            player.addAction(Actions.parallel(
                    Actions.rotateBy(360f, 7f),
                    Actions.sequence(
                            Actions.moveTo(0.05f*game.width, 0.5f*game.height),
                            Actions.alpha(1f, 0.5f),
                            Actions.delay(1f),
                            Actions.moveTo(0.65f*game.width, 0.675f*game.height, 1f),
                            Actions.delay(0.5f),
                            Actions.moveTo(0.65f*game.width, 0.35f*game.height, 1f),
                            Actions.delay(1.5f),
                            Actions.alpha(0f, 1f),
                            Actions.delay(0.475f)
                    )
            ));
        }
        if (!platelet.hasActions()) {
            platelet.addAction(Actions.parallel(
                    Actions.rotateBy(-360f, 7f),
                    Actions.sequence(
                            Actions.moveTo(0.8f*game.width, 0.7f*game.height),
                            Actions.alpha(1f, 0.5f),
                            Actions.delay(2.5f),
                            Actions.moveTo(0.8f*game.width, 0.4f*game.height, 1f),
                            Actions.alpha(0f, 1f),
                            Actions.delay(2f)
                    )
            ));
        }
        if (!redCell.hasActions()) {
            redCell.addAction(Actions.parallel(
                    Actions.rotateBy(360f, 7f),
                    Actions.sequence(
                            Actions.alpha(1f, 0.5f),
                            Actions.color(Color.GRAY, 1f),
                            Actions.delay(2.5f),
                            Actions.color(Color.WHITE, 1f),
                            Actions.delay(0.5f),
                            Actions.alpha(0f, 1f),
                            Actions.delay(0.5f)
                    )
            ));
        }

        player.updateSprite();
        platelet.updateSprite();
        redCell.updateSprite();

        game.fonts.mediumS.draw(
                stage.getBatch(),
                text[4],
                0.5f*(game.width - game.fonts.mediumS.getWidth(text[4])),
                0.95f*game.height
        );
    }
    private void drawGame4 () {
        if (!player.hasActions()) {
            player.addAction(Actions.parallel(
                    Actions.rotateBy(360f, 5f),
                    Actions.sequence(
                            Actions.moveTo(0.05f*game.width, 0.7f*game.height),
                            Actions.alpha(1f, 0.5f),
                            Actions.delay(0.5f),
                            Actions.moveTo(0.65f*game.width, 0.55f*game.height, 1f),
                            Actions.delay(1.5f),
                            Actions.alpha(0f, 1f),
                            Actions.delay(0.5f)
                    )
            ));
        }
        if (!lymph.hasActions()) {
            lymph.addAction(Actions.parallel(
                    Actions.rotateBy(360f, 5f),
                    Actions.sequence(
                            Actions.alpha(0f),
                            Actions.moveTo(0.05f*game.width, 0.3f*game.height),
                            Actions.alpha(1f, 0.5f),
                            Actions.delay(0.5f),
                            Actions.moveTo(0.65f*game.width, 0.45f*game.height, 1f),
                            Actions.delay(1.5f),
                            Actions.alpha(0f, 1f),
                            Actions.delay(0.4675f)
                    )
            ));
        }
        if (!virus.hasActions()) {
            virus.addAction(Actions.parallel(
                    Actions.rotateBy(360f, 5f),
                    Actions.sequence(
                            Actions.alpha(0f),
                            Actions.alpha(1f, 0.5f),
                            Actions.delay(1.5f),
                            Actions.alpha(0f, 1f),
                            Actions.delay(2.0175f)
                    )
            ));
        }

        player.updateSprite();
        lymph.updateSprite();
        virus.updateSprite();

        game.fonts.mediumS.draw(
                stage.getBatch(),
                text[5],
                0.5f*(game.width - game.fonts.mediumS.getWidth(text[5])),
                0.95f*game.height
        );
    }
    private void drawGameBoss () {
        if (!player.hasActions()) {
            player.addAction(Actions.parallel(
                    Actions.rotateBy(360f, 7f),
                    Actions.sequence(
                            Actions.moveTo(0.05f*game.width, 0.3f*game.height),
                            Actions.alpha(1f, 0.5f),
                            Actions.delay(0.5f),
                            Actions.moveTo(0.65f*game.width, 0.3f*game.height, 1f),
                            Actions.delay(1f),
                            Actions.moveTo(0.35f*game.width, 0.475f*game.height, 1f),
                            Actions.delay(1.5f),
                            Actions.alpha(0f, 1f),
                            Actions.delay(0.5f)
                    )
            ));
        }
        if (!lymph.hasActions()) {
            lymph.addAction(Actions.parallel(
                    Actions.rotateBy(360f, 7f),
                    Actions.sequence(
                            Actions.moveTo(0.8f*game.width, 0.3f*game.height),
                            Actions.alpha(1f, 0.5f),
                            Actions.color(Color.GRAY, 0.5f),
                            Actions.delay(1f),
                            Actions.color(Color.WHITE, 1f),
                            Actions.moveTo(0.65f*game.width, 0.475f*game.height, 1f),
                            Actions.delay(1.5f),
                            Actions.alpha(0f, 1f),
                            Actions.delay(0.47f)
                    )
            ));
        }
        if (!platelet.hasActions()) {
            platelet.addAction(Actions.parallel(
                    Actions.rotateBy(360f, 7f),
                    Actions.sequence(
                            Actions.moveTo(0.05f*game.width, 0.375f*game.height),
                            Actions.alpha(1f, 0.5f),
                            Actions.delay(0.5f),
                            Actions.moveTo(0.65f*game.width, 0.375f*game.height, 1f),
                            Actions.alpha(0f, 1f),
                            Actions.delay(4.0185f)
                    )
            ));
        }
        if (!virus.hasActions()) {
            virus.addAction(Actions.parallel(
                    Actions.rotateBy(360f, 7f),
                    Actions.sequence(
                            Actions.alpha(1f, 0.5f),
                            Actions.delay(4f),
                            Actions.alpha(0f, 1f),
                            Actions.delay(1.02f)
                    )
            ));
        }
        player.updateSprite();
        lymph.updateSprite();
        platelet.updateSprite();
        virus.updateSprite();

        game.fonts.mediumS.draw(
                stage.getBatch(),
                text[6],
                0.5f*(game.width - game.fonts.mediumS.getWidth(text[6])),
                0.95f*game.height
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
