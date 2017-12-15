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
    private int starScore;
    private int starScoreCopy;
    private int oldStars;
    private int moneyScore;
    private int starIterator = 0;
    private float soundTimer = 0f;

    private AdvSprite money;
    private AdvSprite[] stars;

    private String[] text;

    Results (MainBL game, int curLevel, int score, int stars, int moneyScore) {
        this.game = game;
        this.curLevel = curLevel;
        this.score = score;
        this.moneyScore = moneyScore;
        starScore = stars;
        starScoreCopy = stars;
    }

    @Override
    public void show() {
        game.sounds.mainTheme.setLooping(true);
        game.sounds.mainTheme.setVolume(0.1f);
        if (Technical.isMusicOn) game.sounds.mainTheme.play();

        oldStars = game.prefs.getInteger("level_star_" + curLevel, 0);
        prevScore = game.prefs.getInteger("level_score_" + curLevel, 0);
        if (oldStars < starScore) game.prefs.putInteger("level_star_" + curLevel, starScore);

        rand = new RandomXS128();
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        advSprites = new LinkedList<>();
        for (int i = 0; i < rand.nextInt(10) + 20; i++) {
            addPart();
        }

        buttonInit();

        stars = new AdvSprite[3];
        for (int i = 0; i < 3; i += 2) {
            stars[i] = new AdvSprite(
                    game.atlas.createSprite("star"),
                    0.1f * game.width + i * 0.3f * game.width,
                    0.5f * game.height,
                    0.2f * game.width,
                    0.2f * game.width
            );
            stars[i].setColor(Color.LIGHT_GRAY);
        }
        stars[1] = new AdvSprite(
                game.atlas.createSprite("star"),
                0.325f * game.width,
                0.5f * game.height,
                0.35f * game.width,
                0.35f * game.width
        );
        stars[1].setColor(Color.LIGHT_GRAY);
        for (int i = 0; i < 3; ++i) { stage.addActor(stars[i]); }
        for (int i = 0; i < starScore; ++i) {
            stars[i].addAction(Actions.sequence(
                    Actions.delay(1f + i),
                    Actions.color(Color.YELLOW)
            ));
        }
        money = new AdvSprite(
                game.atlas.createSprite("green_card"),
                0.475f * game.width,
                0.2f * game.height,
                0.2f * game.width,
                0.2f * game.width
        );
        if (curLevel > 4)
            Technical.money += (starScore - oldStars) + (score / 300);
        else
            Technical.money += (starScore - oldStars);
        stage.addActor(money);


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
        for (AdvSprite sprite : stars) {
            sprite.addAction(Actions.rotateBy(1f));
            sprite.updateSprite();
        }

        soundTimer += delta;
        if (starScore > 0 && soundTimer > 1f) {
            if (Technical.isSoundOn) game.sounds.star.play(1f, 1f + starIterator / 8f, 0f);
            starScore--;
            starIterator++;
            soundTimer -= 1f;
        }
        money.addAction(Actions.rotateBy(1f));
        money.updateSprite();

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
                    0.4f*(game.height + 1.25f*3f*game.fonts.mediumS.getHeight("A"))
            );
            game.fonts.mediumS.setColor(Color.GOLD);
            game.fonts.mediumS.draw(
                    stage.getBatch(),
                    text[5] + ": " + score,
                    0.5f*(game.width - (game.fonts.mediumS.getWidth(text[5] + ": " + score))),
                    0.4f*(game.height - 1.25f*game.fonts.mediumS.getHeight("A"))
            );
            game.fonts.mediumS.setColor(Color.WHITE);
            game.prefs.putInteger("level_score_" + curLevel, score);
        } else {
            game.fonts.mediumS.draw(
                    stage.getBatch(),
                    text[3] + ": " + prevScore,
                    0.5f*(game.width - (game.fonts.mediumS.getWidth(text[3] + ": " + prevScore))),
                    0.4f*(game.height + 1.25f*3f*game.fonts.mediumS.getHeight("A"))
            );
            game.fonts.mediumS.draw(
                    stage.getBatch(),
                    text[2] + ": " + score,
                    0.5f*(game.width - (game.fonts.mediumS.getWidth(text[2] + ": " + score))),
                    0.4f*(game.height - 1.25f*game.fonts.mediumS.getHeight("A"))
            );
        }
        if (curLevel > 4) {
            game.fonts.mediumS.draw(
                    stage.getBatch(),
                    "+ " + (((starScoreCopy - oldStars) + (score / 300)) + moneyScore),
                    0.325f * game.width,
                    0.2f * game.height + 0.1f * game.width + 0.5f * game.fonts.mediumS.getHeight("A")
            );
        } else {
            game.fonts.mediumS.draw(
                    stage.getBatch(),
                    "+ " + ((starScoreCopy - oldStars) + moneyScore),
                    0.325f * game.width,
                    0.2f * game.height + 0.1f * game.width + 0.5f * game.fonts.mediumS.getHeight("A")
            );
        }
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
