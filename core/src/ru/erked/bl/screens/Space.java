package ru.erked.bl.screens;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.ParallelAction;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.input.GestureDetector.GestureListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import java.util.Iterator;
import java.util.LinkedList;

import ru.erked.bl.MainBL;
import ru.erked.bl.entities.Bound;
import ru.erked.bl.entities.Lymphocyte;
import ru.erked.bl.entities.RedCell;
import ru.erked.bl.entities.Platelet;
import ru.erked.bl.entities.Virus;
import ru.erked.bl.systems.BLButton;
import ru.erked.bl.utils.AdvSprite;
import ru.erked.bl.entities.Entity;
import ru.erked.bl.utils.Obfuscation;
import ru.erked.bl.utils.Spectator;

class Space implements Screen, GestureListener {

    private MainBL game;
    private Stage stage;
    private Obfuscation obf;
    private RandomXS128 rand;
    private Spectator spec;

    private LinkedList<AdvSprite> partSpec;
    private LinkedList<AdvSprite> partPlayer;
    private LinkedList<Entity> redCells;
    private LinkedList<Entity> platelets;
    private LinkedList<Entity> lymphocytes;
    private LinkedList<Entity> viruses;
    private LinkedList<Entity> bonuses;
    private LinkedList<Bound> bounds;
    private Entity player;

    private World world;
    private ContactListener contactListener;
    private float accumulator = 0f;
    private float meter = Technical.METER;

    private boolean isGameOver = false;
    private boolean isVictory = false;
    private int playerScore = 0;
    private int lymphScore = 0;
    private int moneyScore = 0;

    private int curLevel;

    private AdvSprite timeIcon;
    private AdvSprite directIcon;
    private AdvSprite moneyIcon;

    private AdvSprite lymphSign;
    private AdvSprite lAndPSign;
    private AdvSprite playerSign;
    private AdvSprite virusSign1;
    private AdvSprite virusSign2;
    private AdvSprite redCellSign;
    private AdvSprite pointer;

    private long dirTimer = 0;
    private int score = 0;
    private long timer = 0;
    private long givenTime = 0;
    private int target = 0;
    private boolean hasPlatelet = false;

    private BLButton exit;
    private BLButton pause;
    private AdvSprite pauseObf;
    private AdvSprite timeLine;
    private boolean isPaused = false;
    private boolean toMenu = false;

    private String[] text;

    Space (MainBL game, int curLevel) {
        this.game = game;
        this.curLevel = curLevel;
        stage = new Stage();
        rand = new RandomXS128();
        float worldWidth = (1f / Technical.METER) * Gdx.graphics.getWidth();
        float worldHeight = worldWidth*(game.height/game.width);
        spec = new Spectator(worldWidth, worldHeight);
        spec.setPosition(10f, 10f);
        world = new World(new Vector2(0, 0), true);
        redCells = new LinkedList<>();
        viruses = new LinkedList<>();
        lymphocytes = new LinkedList<>();
        platelets = new LinkedList<>();
        bounds = new LinkedList<>();
        partSpec = new LinkedList<>();
        partPlayer = new LinkedList<>();
        bonuses = new LinkedList<>();
    }

    @Override
    public void show () {
        game.sounds.mainTheme.setLooping(true);
        game.sounds.mainTheme.setVolume(0.1f);
        if (Technical.isMusicOn) game.sounds.mainTheme.play();

        Box2D.init();
        contactListenerInit();

        AdvSprite playerEnt = new AdvSprite(game.atlas.createSprite("white", Technical.curSkin), 0f, 0f, 1f, 1f);
        player = new Entity(playerEnt, world, 0f, 5f, 0.2f, "player");
        player.getBody().setAngularVelocity(0.25f);

        addBounds();
        for (int i = 0; i < rand.nextInt(10) + 20; i++) { spawnPart(); }
        for (int i = 0; i < rand.nextInt(5) + 10; i++) { spawnPartPlayer(); }

        stage.addActor(spec);
        stage.addActor(player);

        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(stage);
        multiplexer.addProcessor(new GestureDetector(this));
        Gdx.input.setInputProcessor(multiplexer);
        world.setContactListener(contactListener);

        UIInit();
        initLevels();

        buttonInit();
        obf = new Obfuscation(game.atlas.createSprite("obfuscation"), true);
        pauseObf = new AdvSprite(game.atlas.createSprite("obfuscation"), 0f, 0f, game.width, game.height);

        text = new String[3];
        text[0] = game.textSystem.get("lose");
        text[1] = game.textSystem.get("sex");
        text[2] = game.textSystem.get("payza");
    }

    @Override
    public void render (float delta) {
        Gdx.gl.glClearColor(220f/255f, 150f/255f, 180f/255f, 0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        buttonUpdate();
        if (!isPaused) {
            float s = 0.01f * spec.get().viewportHeight;
            gravity(player.getBody(), s);
            for (Entity r : redCells) gravity(r.getBody(), s);
            for (Entity l : lymphocytes) gravity(l.getBody(), s);
            for (Entity v : viruses) gravity(v.getBody(), s);
            for (Entity t : platelets) gravity(t.getBody(), s);
            for (Entity b : bonuses) gravity(b.getBody(), s);

            if (!isGameOver) lifeCycle();

            UIUpdate();

            stage.act();
            stage.draw();

            accumulator += Math.min(delta, 0.25f);
            float STEP_TIME = 1f / 60f;
            if (accumulator >= STEP_TIME) {
                accumulator -= STEP_TIME;
                int VELOCITY_ITERATIONS = 5;
                int POSITION_ITERATIONS = 3;
                world.step(STEP_TIME, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
            }

            spec.setPosition(player.getPosition().x, player.getPosition().y);
            spec.update();
        }
        stage.getBatch().begin();

        if (!isGameOver) {
            if (!isPaused) {
                drawText();
                if (curLevel == 4) {
                    virusSign1.draw(stage.getBatch(), 1f);
                    lAndPSign.draw(stage.getBatch(), 1f);
                } else if (curLevel == 3) {
                    redCellSign.draw(stage.getBatch(), 1f);
                } else if (curLevel == 2) {
                    virusSign1.draw(stage.getBatch(), 1f);
                } else if (curLevel != 1) {
                    if (curLevel % 10 != 0) {
                        switch (curLevel % 4) {
                            case 1: {
                                virusSign1.draw(stage.getBatch(), 1f);
                                break;
                            }
                            case 2: {
                                virusSign1.draw(stage.getBatch(), 1f);
                                virusSign2.draw(stage.getBatch(), 1f);
                                playerSign.draw(stage.getBatch(), 1f);
                                lymphSign.draw(stage.getBatch(), 1f);
                                break;
                            }
                            case 3: {
                                redCellSign.draw(stage.getBatch(), 1f);
                                break;
                            }
                            case 0: {
                                virusSign1.draw(stage.getBatch(), 1f);
                                lAndPSign.draw(stage.getBatch(), 1f);
                                break;
                            }
                        }
                    }
                }
                pointer.setVisible(true);
                pointer.draw(stage.getBatch(), pointer.getColor().a);
                timeLine.setVisible(true);
                timeLine.draw(stage.getBatch(), 1f);
            } else {
                pauseObf.draw(stage.getBatch(), 0.5f);
                game.fonts.largeS.draw(
                        stage.getBatch(),
                        text[2],
                        0.5f * (game.width - game.fonts.largeS.getWidth(text[2])),
                        0.5f * (game.height + game.fonts.largeS.getHeight(text[2]))

                );
                pointer.setVisible(false);
                timeLine.setVisible(false);
                if (curLevel == 4) {
                    lAndPSign.setVisible(false);
                    virusSign1.setVisible(false);
                } else if (curLevel == 3) {
                    redCellSign.setVisible(false);
                } else if (curLevel == 2) {
                    virusSign1.setVisible(false);
                } else if (curLevel != 1) {
                    if (curLevel % 10 != 0) {
                        switch (curLevel % 4) {
                            case 1: {
                                virusSign1.setVisible(false);
                                break;
                            }
                            case 2: {
                                playerSign.setVisible(false);
                                lymphSign.setVisible(false);
                                virusSign1.setVisible(false);
                                virusSign2.setVisible(false);
                                break;
                            }
                            case 3: {
                                redCellSign.setVisible(false);
                                break;
                            }
                            case 0: {
                                virusSign1.setVisible(false);
                                lAndPSign.setVisible(false);
                                break;
                            }
                        }
                    }
                }
            }
            pause.get().setVisible(true);
            pause.get().draw(stage.getBatch(), 1f);
            exit.get().setVisible(true);
            exit.get().draw(stage.getBatch(), 1f);
        } else {
            if (curLevel == 4) {
                lAndPSign.setVisible(false);
                virusSign1.setVisible(false);
            } else if (curLevel == 3) {
                redCellSign.setVisible(false);
            } else if (curLevel == 2) {
                virusSign1.setVisible(false);
            } else if (curLevel != 1) {
                if (curLevel % 10 != 0) {
                    switch (curLevel % 4) {
                        case 1: {
                            virusSign1.setVisible(false);
                            break;
                        }
                        case 2: {
                            playerSign.setVisible(false);
                            lymphSign.setVisible(false);
                            virusSign1.setVisible(false);
                            virusSign2.setVisible(false);
                            break;
                        }
                        case 3: {
                            redCellSign.setVisible(false);
                            break;
                        }
                        case 0: {
                            lAndPSign.setVisible(false);
                            virusSign1.setVisible(false);
                            break;
                        }
                    }
                }
            }
            pointer.setVisible(false);
            timeLine.setVisible(false);
            exit.get().setVisible(false);
            pause.get().setVisible(false);
        }

        obf.draw(stage.getBatch(), obf.getAlpha());

        if(obf.isActive() && !toMenu && !isGameOver){
            obf.deactivate(0.5f, delta);
        } else if (toMenu) {
            if (obf.isActive()) {
                game.setScreen(new Menu(game, true));
            } else {
                obf.activate(0.5f, delta);
            }
        } else if (isGameOver) {
            if (obf.isActive()) {
                if (isVictory){
                    int stars = 0;
                    Technical.money += moneyScore;
                    if (Technical.maxLevel == curLevel) Technical.maxLevel++;
                    score = (int)((score + (timer / 6)) - (score + (timer / 6)) % 10);
                    if (curLevel > 4) {
                        if (curLevel % 10 == 0) {
                            if ((float) timer / (float) givenTime >= 0.05f - (float) curLevel / 2000f)
                                stars++;
                            if ((float) timer / (float) givenTime >= 0.1f - (float) curLevel / 2000f)
                                stars++;
                            if ((float) timer / (float) givenTime >= 0.15f - (float) curLevel / 2000f)
                                stars++;
                        } else {
                            switch (curLevel % 4) {
                                case 1: {
                                    if ((float) timer / (float) givenTime >= (0.15f - (curLevel / 2000f)))
                                        stars++;
                                    if ((float) timer / (float) givenTime >= (0.3f - (curLevel / 2000f)))
                                        stars++;
                                    if ((float) timer / (float) givenTime >= (0.45f - (curLevel / 2000f)))
                                        stars++;
                                    break;
                                }
                                case 2: {
                                    if ((float) playerScore / (lymphScore + 1f) >= 1.1f) stars++;
                                    if ((float) playerScore / (lymphScore + 1f) >= 1.2f) stars++;
                                    if ((float) playerScore / (lymphScore + 1f) >= 1.3f) stars++;
                                    break;
                                }
                                case 3: {
                                    if ((float) timer / (float) givenTime >= (0.15f - (curLevel / 2000f)))
                                        stars++;
                                    if ((float) timer / (float) givenTime >= (0.3f - (curLevel / 2000f)))
                                        stars++;
                                    if ((float) timer / (float) givenTime >= (0.45f - (curLevel / 2000f)))
                                        stars++;
                                    break;
                                }
                                case 0: {
                                    if ((float) timer / (float) givenTime >= (0.15f - (curLevel / 2000f)))
                                        stars++;
                                    if ((float) timer / (float) givenTime >= (0.3f - (curLevel / 2000f)))
                                        stars++;
                                    if ((float) timer / (float) givenTime >= (0.45f - (curLevel / 2000f)))
                                        stars++;
                                    break;
                                }
                            }
                        }
                    } else {
                        stars = 3;
                    }
                    game.setScreen(new Results(game, curLevel, score, stars, moneyScore));
                } else {
                    game.setScreen(new Menu(game, true));
                }
            } else {
                obf.activate(3f, delta);
                if (isVictory) {
                    game.fonts.large.draw(
                            stage.getBatch(),
                            game.textSystem.get("rivera"),
                            0.5f*(game.width - game.fonts.large.getWidth(game.textSystem.get("rivera"))),
                            0.5f*(game.height + game.fonts.large.getHeight("A"))
                    );
                } else {
                    game.fonts.large.draw(
                            stage.getBatch(),
                            game.textSystem.get("end_of_life"),
                            0.5f*(game.width - game.fonts.large.getWidth(game.textSystem.get("end_of_life"))),
                            0.5f*(game.height + game.fonts.large.getHeight("A"))
                    );
                }
            }
        }

        stage.getBatch().end();

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

    private void contactListenerInit () {
        contactListener = new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
            }

            @Override
            public void endContact(Contact contact) {
            }

            @Override
            public void preSolve(Contact contact, Manifold oldManifold) {
            }

            @Override
            public void postSolve(Contact contact, ContactImpulse impulse) {
                if (curLevel % 4 == 2 || curLevel % 4 == 0) lymphVsViruses(contact);
                if (curLevel % 4 == 3) {
                    if (!hasPlatelet) playerVsPlatelets(contact);
                    if (hasPlatelet) playerVsRedCells(contact);
                }
                if (curLevel % 10 == 0) {
                    boolean hasSick = false;
                    for (Entity e : lymphocytes) if (e.getName().equals("lymphocyte_sick")) {hasSick = true; break;}
                    if (hasSick) {
                        if (!hasPlatelet) playerVsPlatelets(contact);
                        if (hasPlatelet) playerVsLymph(contact);
                    }
                }
                playerVsBonus(contact);
                playerVsViruses(contact);
            }
        };
    }
    private void lymphVsViruses (Contact contact) {
        Iterator<Entity> iLymph = lymphocytes.iterator();
        while (iLymph.hasNext()) {
            Entity lymph = iLymph.next();
            Iterator<Entity> iVirus = viruses.iterator();
            while (iVirus.hasNext()) {
                Entity virus = iVirus.next();
                if (contact.getFixtureA().getBody().equals(lymph.getBody()) && contact.getFixtureB().getBody().equals(virus.getBody())) {
                    if (lymph.getLifeTime() > 0 && virus.getLifeTime() > 0) {
                        if (virus.getName().equals("virus_boss")) {
                            float velX = -2f*(virus.getPosition().x - lymph.getPosition().x);
                            float velY = -2f*(virus.getPosition().y - lymph.getPosition().y);
                            lymph.applyLinearImpulse(
                                    velX,
                                    velY,
                                    lymph.getPosition().x,
                                    lymph.getPosition().y,
                                    true);
                        }
                        virus.decreaseLT(50);
                        virus.addAction(Actions.color(Color.RED));
                        virus.addAction(Actions.color(Color.WHITE, 0.5f));
                        if (virus.getLifeTime() == 0) lymphScore++;
                        break;
                    }
                } else if (contact.getFixtureB().getBody().equals(lymph.getBody()) && contact.getFixtureA().getBody().equals(virus.getBody())) {
                    if (lymph.getLifeTime() > 0 && virus.getLifeTime() > 0) {
                        if (virus.getName().equals("virus_boss")) {
                            float velX = -2f*(virus.getPosition().x - lymph.getPosition().x);
                            float velY = -2f*(virus.getPosition().y - lymph.getPosition().y);
                            lymph.applyLinearImpulse(
                                    velX,
                                    velY,
                                    lymph.getPosition().x,
                                    lymph.getPosition().y,
                                    true);
                        }
                        virus.decreaseLT(50);
                        virus.addAction(Actions.color(Color.RED));
                        virus.addAction(Actions.color(Color.WHITE, 0.5f));
                        if (virus.getLifeTime() == 0) lymphScore++;
                        break;
                    }
                }
            }
            if (iVirus.hasNext()) break;
        }
    }
    private void playerVsViruses (Contact contact) {
        Iterator<Entity> iVirus = viruses.iterator();
        while (iVirus.hasNext()) {
            Entity virus = iVirus.next();
            if (contact.getFixtureA().getBody().equals(player.getBody()) && contact.getFixtureB().getBody().equals(virus.getBody())) {
                if (virus.getLifeTime() > 0) {
                    if (virus.getName().equals("virus_boss")) {
                        float velX = -2f*(virus.getPosition().x - player.getPosition().x);
                        float velY = -2f*(virus.getPosition().y - player.getPosition().y);
                        player.applyLinearImpulse(
                                velX,
                                velY,
                                player.getPosition().x,
                                player.getPosition().y,
                                true);
                    }
                    score += 10;
                    virus.decreaseLT(60);
                    virus.addAction(Actions.color(Color.RED));
                    virus.addAction(Actions.color(Color.WHITE, 0.5f));
                    if (Technical.isSoundOn) game.sounds.hit.play(1f, 1.1f - rand.nextFloat() / 5f, 0f);
                    if (virus.getLifeTime() == 0) {
                        playerScore++;
                        if (Technical.isSoundOn) game.sounds.hit.play(1f, 0.7f - rand.nextFloat() / 5f, 0f);
                    }
                    break;
                }
            } else if (contact.getFixtureB().getBody().equals(player.getBody()) && contact.getFixtureA().getBody().equals(virus.getBody())) {
                if (virus.getLifeTime() > 0) {
                    if (virus.getName().equals("virus_boss")) {
                        float velX = -2f*(virus.getPosition().x - player.getPosition().x);
                        float velY = -2f*(virus.getPosition().y - player.getPosition().y);
                        player.applyLinearImpulse(
                                velX,
                                velY,
                                player.getPosition().x,
                                player.getPosition().y,
                                true);
                    }
                    score += 10;
                    virus.decreaseLT(60);
                    virus.addAction(Actions.color(Color.RED));
                    virus.addAction(Actions.color(Color.WHITE, 0.5f));
                    if (Technical.isSoundOn) game.sounds.hit.play(1f, 1.1f - rand.nextFloat() / 5f, 0f);
                    if (virus.getLifeTime() == 0) {
                        playerScore++;
                        if (Technical.isSoundOn) game.sounds.hit.play(1f, 0.7f - rand.nextFloat() / 5f, 0f);
                    }
                    break;
                }
            }
        }
    }
    private void playerVsPlatelets (Contact contact) {
        Iterator<Entity> iPlat = platelets.iterator();
        while (iPlat.hasNext()) {
            Entity platelet = iPlat.next();
            if (contact.getFixtureA().getBody().equals(player.getBody()) && contact.getFixtureB().getBody().equals(platelet.getBody())) {
                if (platelet.getLifeTime() > 0) {
                    score += 10;
                    platelet.setName("platelet_s");
                    hasPlatelet = true;
                    if (Technical.isSoundOn) game.sounds.hit.play(1f, 1.25f - rand.nextFloat() / 2f, 0f);
                    break;
                }
            }else if (contact.getFixtureB().getBody().equals(player.getBody()) && contact.getFixtureA().getBody().equals(platelet.getBody())) {
                if (platelet.getLifeTime() > 0) {
                    score += 10;
                    platelet.setName("platelet_s");
                    hasPlatelet = true;
                    if (Technical.isSoundOn) game.sounds.hit.play(1f, 1.25f - rand.nextFloat() / 2f, 0f);
                    break;
                }
            }
        }
    }
    private void playerVsRedCells (Contact contact) {
        Iterator<Entity> iRedCell = redCells.iterator();
        while (iRedCell.hasNext()) {
            Entity redCell = iRedCell.next();
            if (contact.getFixtureA().getBody().equals(player.getBody()) && contact.getFixtureB().getBody().equals(redCell.getBody())) {
                if (redCell.getLifeTime() > 0 && redCell.getName().equals("red_cell_sick")) {
                    redCell.addAction(Actions.color(Color.WHITE, 1f));
                    redCell.setName("red_cell");
                    score += 10;
                    if (Technical.isSoundOn) game.sounds.heal.play(1f, 1.25f - rand.nextFloat() / 2f, 0f);
                    for (int i = 0; i < platelets.size(); i++){
                        if (platelets.get(i).getName().equals("platelet_s"))
                            platelets.get(i).decreaseLT(1000);
                    }
                    playerScore++;
                    hasPlatelet = false;
                    break;
                }
            }else if (contact.getFixtureB().getBody().equals(player.getBody()) && contact.getFixtureA().getBody().equals(redCell.getBody())) {
                if (redCell.getLifeTime() > 0 && redCell.getName().equals("red_cell_sick")) {
                    redCell.addAction(Actions.color(Color.WHITE, 1f));
                    redCell.setName("red_cell");
                    score += 10;
                    if (Technical.isSoundOn) game.sounds.heal.play(1f, 1.25f - rand.nextFloat() / 2f, 0f);
                    for (int i = 0; i < platelets.size(); i++) {
                        if (platelets.get(i).getName().equals("platelet_s"))
                            platelets.get(i).decreaseLT(1000);
                    }
                    playerScore++;
                    hasPlatelet = false;
                    break;
                }
            }
        }
    }
    private void playerVsLymph (Contact contact) {
        Iterator<Entity> iLymph = lymphocytes.iterator();
        while (iLymph.hasNext()) {
            Entity lymph = iLymph.next();
            if (contact.getFixtureA().getBody().equals(player.getBody()) && contact.getFixtureB().getBody().equals(lymph.getBody())) {
                if (lymph.getLifeTime() > 0 && lymph.getName().equals("lymphocyte_sick")) {
                    lymph.addAction(Actions.color(Color.WHITE, 1f));
                    lymph.setName("lymphocyte");
                    score += 10;
                    if (Technical.isSoundOn) game.sounds.heal.play(1f, 1.25f - rand.nextFloat() / 2f, 0f);
                    for (int i = 0; i < platelets.size(); i++){
                        if (platelets.get(i).getName().equals("platelet_s"))
                            platelets.get(i).decreaseLT(1000);
                    }
                    hasPlatelet = false;
                    break;
                }
            }else if (contact.getFixtureB().getBody().equals(player.getBody()) && contact.getFixtureA().getBody().equals(lymph.getBody())) {
                if (lymph.getLifeTime() > 0 && lymph.getName().equals("lymphocyte_sick")) {
                    lymph.addAction(Actions.color(Color.WHITE, 1f));
                    lymph.setName("lymphocyte");
                    score += 10;
                    if (Technical.isSoundOn) game.sounds.heal.play(1f, 1.25f - rand.nextFloat() / 2f, 0f);
                    for (int i = 0; i < platelets.size(); i++){
                        if (platelets.get(i).getName().equals("platelet_s"))
                            platelets.get(i).decreaseLT(1000);
                    }
                    hasPlatelet = false;
                    break;
                }
            }
        }
    }
    private void playerVsBonus (Contact contact) {
        Iterator<Entity> iBonus = bonuses.iterator();
        while (iBonus.hasNext()) {
            Entity bonus = iBonus.next();
            if (contact.getFixtureA().getBody().equals(player.getBody()) && contact.getFixtureB().getBody().equals(bonus.getBody())) {
                if (bonus.getLifeTime() > 0) {
                    switch (bonus.getName()) {
                        case "green_bonus": {
                            bonus.decreaseLT(1000);
                            if (bonus.getLifeTime() == 0) {
                                moneyScore++;
                                score += 10;
                                if (Technical.isSoundOn) game.sounds.bonus.play(1f, 1.1f - rand.nextFloat() / 5f, 0f);
                                moneyIcon.setPosition(0.5f*game.width, 0.5f*game.height);
                                moneyIcon.addAction(Actions.alpha(1f));
                                float x, y;
                                x = 0.3f * game.width - rand.nextFloat()*0.6f * game.width;
                                y = 0.1f * game.height + rand.nextFloat()*0.2f * game.height;
                                moneyIcon.addAction(Actions.parallel(
                                        Actions.moveBy(x, y, 1.5f),
                                        Actions.alpha(0f, 1.5f)
                                ));
                            }
                            break;
                        }
                        case "time_bonus": {
                            bonus.decreaseLT(1000);
                            if (bonus.getLifeTime() == 0) {
                                score += 10;
                                if (Technical.isSoundOn) game.sounds.bonus.play(1f, 1.1f - rand.nextFloat() / 5f, 0f);
                                timer += 120 + Technical.timeLevel * 90;
                                timeIcon.setPosition(0.5f*game.width, 0.5f*game.height);
                                timeIcon.addAction(Actions.alpha(1f));
                                float x, y;
                                x = 0.3f * game.width - rand.nextFloat()*0.6f * game.width;
                                y = 0.1f * game.height + rand.nextFloat()*0.2f * game.height;
                                timeIcon.addAction(Actions.parallel(
                                        Actions.moveBy(x, y, 1.5f),
                                        Actions.alpha(0f, 1.5f)
                                ));
                            }
                            break;
                        }
                        case "direction_bonus": {
                            bonus.decreaseLT(1000);
                            if (bonus.getLifeTime() == 0) {
                                score += 10;
                                if (Technical.isSoundOn) game.sounds.bonus.play(1f, 1.1f - rand.nextFloat() / 5f, 0f);
                                dirTimer += 120 + Technical.timeLevel * 90;
                                directIcon.setPosition(0.5f*game.width, 0.5f*game.height);
                                directIcon.addAction(Actions.alpha(1f));
                                float x, y;
                                x = 0.3f * game.width - rand.nextFloat()*0.6f * game.width;
                                y = 0.1f * game.height + rand.nextFloat()*0.2f * game.height;
                                directIcon.addAction(Actions.parallel(
                                        Actions.moveBy(x, y, 1.5f),
                                        Actions.alpha(0f, 1.5f)
                                ));
                            }
                            break;
                        }
                    }
                }
            }else if (contact.getFixtureB().getBody().equals(player.getBody()) && contact.getFixtureA().getBody().equals(bonus.getBody())) {
                if (bonus.getLifeTime() > 0) {
                    switch (bonus.getName()) {
                        case "green_bonus": {
                            bonus.decreaseLT(1000);
                            if (bonus.getLifeTime() == 0) {
                                moneyScore++;
                                score += 10;
                                if (Technical.isSoundOn) game.sounds.bonus.play(1f, 1.1f - rand.nextFloat() / 5f, 0f);
                                moneyIcon.setPosition(0.5f*game.width, 0.5f*game.height);
                                moneyIcon.addAction(Actions.alpha(1f));
                                float x, y;
                                x = 0.3f * game.width - rand.nextFloat()*0.6f * game.width;
                                y = 0.1f * game.height + rand.nextFloat()*0.2f * game.height;
                                moneyIcon.addAction(Actions.parallel(
                                        Actions.moveBy(x, y, 1.5f),
                                        Actions.alpha(0f, 1.5f)
                                ));
                            }
                            break;
                        }
                        case "time_bonus": {
                            bonus.decreaseLT(1000);
                            if (bonus.getLifeTime() == 0) {
                                score += 10;
                                if (Technical.isSoundOn) game.sounds.bonus.play(1f, 1.1f - rand.nextFloat() / 5f, 0f);
                                timer += 120 + Technical.timeLevel * 90;
                                timeIcon.setPosition(0.5f*game.width, 0.5f*game.height);
                                timeIcon.addAction(Actions.alpha(1f));
                                float x, y;
                                x = 0.3f * game.width - rand.nextFloat()*0.6f * game.width;
                                y = 0.1f * game.height + rand.nextFloat()*0.2f * game.height;
                                timeIcon.addAction(Actions.parallel(
                                        Actions.moveBy(x, y, 1.5f),
                                        Actions.alpha(0f, 1.5f)
                                ));
                            }
                            break;
                        }
                        case "direction_bonus": {
                            bonus.decreaseLT(1000);
                            if (bonus.getLifeTime() == 0) {
                                score += 10;
                                if (Technical.isSoundOn) game.sounds.bonus.play(1f, 1.1f - rand.nextFloat() / 5f, 0f);
                                dirTimer += 120 + Technical.timeLevel * 90;
                                directIcon.setPosition(0.5f*game.width, 0.5f*game.height);
                                directIcon.addAction(Actions.alpha(1f));
                                float x, y;
                                x = 0.3f * game.width - rand.nextFloat()*0.6f * game.width;
                                y = 0.1f * game.height + rand.nextFloat()*0.2f * game.height;
                                directIcon.addAction(Actions.parallel(
                                        Actions.moveBy(x, y, 1.5f),
                                        Actions.alpha(0f, 1.5f)
                                ));
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    private void buttonInit () {
        exit = new BLButton(
                game,
                0.25f*game.width,
                0.5f*game.height - 0.3f*game.width,
                0.25f*game.width,
                game.fonts.small.getFont(),
                game.textSystem.get("to_menu_button"),
                1,
                "to_menu_button"
        );
        exit.get().addListener(new ClickListener(){
            @Override
            public void clicked (InputEvent event, float x, float y) {
                if (!obf.isActive()) {
                    if (Technical.isSoundOn) game.sounds.click.play();
                    toMenu = true;
                } else {
                    exit.get().setChecked(false);
                }
            }
        });
        stage.addActor(exit.get());

        pause = new BLButton(
                game,
                0.25f*game.width,
                0.5f*game.height - 0.3f*game.width,
                0.125f*game.width,
                game.fonts.small.getFont(),
                "||",
                2,
                "pause_button"
        );
        pause.get().addListener(new ClickListener(){
            @Override
            public void clicked (InputEvent event, float x, float y) {
                if (!obf.isActive()) {
                    if (Technical.isSoundOn) game.sounds.click.play();
                    isPaused = !isPaused;
                    game.prefs.putInteger("max_level", Technical.maxLevel);
                    game.prefs.putBoolean("is_sound_on", Technical.isSoundOn);
                    game.prefs.flush();
                } else {
                    pause.get().setChecked(false);
                }
            }
        });
        stage.addActor(pause.get());
    }
    private void buttonUpdate () {
        exit.get().setPosition(
                7.9f*meter - exit.get().getWidth(),
                spec.get().viewportHeight * meter - exit.get().getHeight() - 0.1f*meter
        );
        pause.get().setPosition(
                7.9f*meter - exit.get().getWidth() - 0.025f*game.width - pause.get().getWidth(),
                spec.get().viewportHeight * meter - exit.get().getHeight() - 0.1f*meter
        );
    }
    private void UIInit () {
        pointer = new AdvSprite(game.atlas.createSprite("pointer"), 0f, 0f, 2f*meter, 2f*meter);
        timeLine = new AdvSprite(game.atlas.createSprite("particle"), 0f, 0f, game.width, 0.025f*game.height);
        timeLine.setColor(Color.RED);
        if (curLevel < 5) {
            switch (curLevel) {
                case 4: {
                    virusSign1 = new AdvSprite(game.atlas.createSprite("virus", rand.nextInt(5) + 11), 0f, 0f, 0.1f * game.width, 0.1f * game.width);
                    lAndPSign = new AdvSprite(game.atlas.createSprite("lymphocytes_and_player"), 0f, 0f, 0.05f * game.width, 0.05f * game.width);
                    stage.addActor(virusSign1);
                    stage.addActor(lAndPSign);
                    break;
                }
                case 3: {
                    redCellSign = new AdvSprite(game.atlas.createSprite("red"), 0f, 0f, 0.1f * game.width, 0.1f * game.width);
                    redCellSign.setColor(Color.GRAY);
                    stage.addActor(redCellSign);
                    break;
                }
                case 2: {
                    virusSign1 = new AdvSprite(game.atlas.createSprite("virus", rand.nextInt(5) + 11), 0f, 0f, 0.1f * game.width, 0.1f * game.width);
                    stage.addActor(virusSign1);
                    break;
                }
                case 1: {
                    pointer.addAction(Actions.alpha(0f));
                    pointer.addAction(Actions.sequence(
                            Actions.delay(20f),
                            Actions.alpha(1f, 5f)
                    ));
                    break;
                }
            }
        } else if (curLevel % 10 != 0) {
            switch (curLevel % 4) {
                case 1: {
                    virusSign1 = new AdvSprite(game.atlas.createSprite("virus", rand.nextInt(5) + 11), 0f, 0f, 0.1f * game.width, 0.1f * game.width);
                    stage.addActor(virusSign1);
                    break;
                }
                case 2: {
                    virusSign1 = new AdvSprite(game.atlas.createSprite("virus", rand.nextInt(5) + 11), 0f, 0f, 0.1f * game.width, 0.1f * game.width);
                    virusSign2 = new AdvSprite(game.atlas.createSprite("virus", rand.nextInt(5) + 11), 0f, 0f, 0.1f * game.width, 0.1f * game.width);
                    lymphSign = new AdvSprite(game.atlas.createSprite("lymphocytes"), 0f, 0f, 0.05f * game.width, 0.05f * game.width);
                    playerSign = new AdvSprite(game.atlas.createSprite("white", Technical.curSkin), 0f, 0f, 0.05f * game.width, 0.05f * game.width);

                    stage.addActor(virusSign1);
                    stage.addActor(virusSign2);
                    stage.addActor(lymphSign);
                    stage.addActor(playerSign);
                    break;
                }
                case 3: {
                    redCellSign = new AdvSprite(game.atlas.createSprite("red"), 0f, 0f, 0.1f * game.width, 0.1f * game.width);
                    redCellSign.setColor(Color.GRAY);
                    stage.addActor(redCellSign);
                    break;
                }
                case 0: {
                    lAndPSign = new AdvSprite(game.atlas.createSprite("lymphocytes_and_player"), 0f, 0f, 0.05f * game.width, 0.05f * game.width);
                    virusSign1 = new AdvSprite(game.atlas.createSprite("virus", rand.nextInt(5) + 11), 0f, 0f, 0.1f * game.width, 0.1f * game.width);
                    stage.addActor(virusSign1);
                    stage.addActor(lAndPSign);
                    break;
                }
            }
        }
        timeIcon = new AdvSprite(game.atlas.createSprite("time_icon"), 0f, 0f, 0.1f * game.width, 0.1f * game.width);
        timeIcon.addAction(Actions.alpha(0f));
        moneyIcon = new AdvSprite(game.atlas.createSprite("money_icon"), 0f, 0f, 0.1f * game.width, 0.1f * game.width);
        moneyIcon.addAction(Actions.alpha(0f));
        directIcon = new AdvSprite(game.atlas.createSprite("direction_icon"), 0f, 0f, 0.1f * game.width, 0.1f * game.width);
        directIcon.addAction(Actions.alpha(0f));
        stage.addActor(pointer);
        stage.addActor(timeLine);
        stage.addActor(timeIcon);
        stage.addActor(moneyIcon);
        stage.addActor(directIcon);
    }
    private void UIUpdate () {
        if (curLevel < 5) {
            switch (curLevel) {
                case 4: {
                    virusSign1.setPosition(
                            0.1f * meter,
                            game.height - 0.125f * game.width
                    );
                    lAndPSign.setPosition(
                            0.1f * meter + virusSign1.getWidth() - lAndPSign.getWidth(),
                            game.height - 0.125f * game.width
                    );
                    virusSign1.addAction(Actions.rotateBy(1f));
                    virusSign1.updateSprite();
                    lAndPSign.addAction(Actions.rotateBy(1f));
                    lAndPSign.updateSprite();
                    pointerUpdateVirus();
                    pointer.updateSprite(spec.get());
                    if (givenTime != 0) {
                        timeLine.setSize(game.width*((float)timer/(float)givenTime), timeLine.getHeight());
                    } else {
                        timeLine.setSize(0f, timeLine.getHeight());
                    }
                    timeLine.updateSprite();
                    break;
                }
                case 3: {
                    redCellSign.setPosition(
                            0.1f * meter,
                            game.height - 0.125f * game.width
                    );
                    redCellSign.addAction(Actions.rotateBy(1f));
                    redCellSign.updateSprite();
                    if (!hasPlatelet)
                        pointerUpdatePlat();
                    else
                        pointerUpdateRedCell();
                    pointer.updateSprite(spec.get());
                    if (givenTime != 0) {
                        timeLine.setSize(game.width*((float)timer/(float)givenTime), timeLine.getHeight());
                    } else {
                        timeLine.setSize(0f, timeLine.getHeight());
                    }
                    timeLine.updateSprite();
                    break;
                }
                case 2: {
                    virusSign1.setPosition(
                            0.1f * meter,
                            game.height - 0.125f * game.width
                    );
                    virusSign1.addAction(Actions.rotateBy(1f));
                    virusSign1.updateSprite();
                    pointerUpdateVirus();
                    pointer.updateSprite(spec.get());
                    if (givenTime != 0) {
                        timeLine.setSize(game.width*((float)timer/(float)givenTime), timeLine.getHeight());
                    } else {
                        timeLine.setSize(0f, timeLine.getHeight());
                    }
                    timeLine.updateSprite();
                    break;
                }
                case 1: {
                    pointerUpdateVirus();
                    pointer.updateSprite(spec.get());
                    if (givenTime != 0) {
                        timeLine.setSize(game.width*((float)timer/(float)givenTime), timeLine.getHeight());
                    } else {
                        timeLine.setSize(0f, timeLine.getHeight());
                    }
                    timeLine.updateSprite();
                    break;
                }
            }
        } else if (curLevel % 10 != 0) {
            switch (curLevel % 4) {
                case 1: {
                    virusSign1.setPosition(
                            0.1f * meter,
                            game.height - 0.125f * game.width
                    );
                    virusSign1.addAction(Actions.rotateBy(1f));
                    virusSign1.updateSprite();
                    pointerUpdateVirus();
                    if (dirTimer > 0) pointer.addAction(Actions.alpha(1f, 1f));
                    if (dirTimer <= 0) pointer.addAction(Actions.alpha(0f, 1f));
                    pointer.updateSprite(spec.get());
                    if (givenTime != 0) {
                        timeLine.setSize(game.width*((float)timer/(float)givenTime), timeLine.getHeight());
                    } else {
                        timeLine.setSize(0f, timeLine.getHeight());
                    }
                    timeLine.updateSprite();
                    break;
                }
                case 2: {
                    virusSign1.setPosition(
                            0.1f * meter,
                            game.height - 0.125f * game.width
                    );
                    virusSign1.addAction(Actions.rotateBy(1f));
                    virusSign1.updateSprite();

                    virusSign2.setPosition(
                            0.1f * meter,
                            game.height - 0.25f * game.width
                    );
                    virusSign2.addAction(Actions.rotateBy(1f));
                    virusSign2.updateSprite();

                    playerSign.setPosition(
                            0.1f * meter + virusSign1.getWidth() - playerSign.getWidth(),
                            game.height - 0.125f * game.width
                    );
                    playerSign.addAction(Actions.rotateBy(1f));
                    playerSign.updateSprite();

                    lymphSign.setPosition(
                            0.1f * meter + virusSign1.getWidth() - lymphSign.getWidth(),
                            game.height - 0.25f * game.width
                    );
                    lymphSign.addAction(Actions.rotateBy(1f));
                    lymphSign.updateSprite();

                    pointerUpdateVirus();
                    if (dirTimer > 0) pointer.addAction(Actions.alpha(1f, 1f));
                    if (dirTimer <= 0) pointer.addAction(Actions.alpha(0f, 1f));
                    pointer.updateSprite(spec.get());
                    if (givenTime != 0) {
                        timeLine.setSize(game.width*((float)timer/(float)givenTime), timeLine.getHeight());
                    } else {
                        timeLine.setSize(0f, timeLine.getHeight());
                    }
                    timeLine.updateSprite();
                    break;
                }
                case 3: {
                    redCellSign.setPosition(
                            0.1f * meter,
                            game.height - 0.125f * game.width
                    );
                    redCellSign.addAction(Actions.rotateBy(1f));
                    redCellSign.updateSprite();
                    if (!hasPlatelet)
                        pointerUpdatePlat();
                    else
                        pointerUpdateRedCell();
                    if (dirTimer > 0) pointer.addAction(Actions.alpha(1f, 1f));
                    if (dirTimer <= 0) pointer.addAction(Actions.alpha(0f, 1f));
                    pointer.updateSprite(spec.get());
                    if (givenTime != 0) {
                        timeLine.setSize(game.width*((float)timer/(float)givenTime), timeLine.getHeight());
                    } else {
                        timeLine.setSize(0f, timeLine.getHeight());
                    }
                    timeLine.updateSprite();
                    break;
                }
                case 0: {
                    virusSign1.setPosition(
                            0.1f * meter,
                            game.height - 0.125f * game.width
                    );
                    lAndPSign.setPosition(
                            0.1f * meter + virusSign1.getWidth() - lAndPSign.getWidth(),
                            game.height - 0.125f * game.width
                    );
                    virusSign1.addAction(Actions.rotateBy(1f));
                    virusSign1.updateSprite();
                    lAndPSign.addAction(Actions.rotateBy(1f));
                    lAndPSign.updateSprite();
                    pointerUpdateVirus();
                    if (dirTimer > 0) pointer.addAction(Actions.alpha(1f, 1f));
                    if (dirTimer <= 0) pointer.addAction(Actions.alpha(0f, 1f));
                    pointer.updateSprite(spec.get());
                    if (givenTime != 0) {
                        timeLine.setSize(game.width*((float)timer/(float)givenTime), timeLine.getHeight());
                    } else {
                        timeLine.setSize(0f, timeLine.getHeight());
                    }
                    timeLine.updateSprite();
                    break;
                }
            }
        } else {
            boolean hasSickLymph = false;
            for (Entity e : lymphocytes) if (e.getName().equals("lymphocyte_sick")) hasSickLymph = true;
            if (hasSickLymph) {
                if (!hasPlatelet)
                    pointerUpdatePlat();
                else
                    pointerUpdateLymph();
            } else {
                pointerUpdateVirus();
            }
            if (dirTimer > 0) pointer.addAction(Actions.alpha(1f, 1f));
            if (dirTimer <= 0) pointer.addAction(Actions.alpha(0f, 1f));
            pointer.updateSprite(spec.get());
            if (givenTime != 0) {
                timeLine.setSize(game.width*((float)timer/(float)givenTime), timeLine.getHeight());
            } else {
                timeLine.setSize(0f, timeLine.getHeight());
            }
            timeLine.updateSprite();
        }
        timeIcon.updateSprite();
        moneyIcon.updateSprite();
        directIcon.updateSprite();
    }
    private void pointerUpdateVirus () {
        pointer.setPosition(
                player.getPosition().x * meter - 0.5f * pointer.getWidth(),
                player.getPosition().y * meter - 0.5f * pointer.getHeight()
        );
        if (viruses.size() > 0) {
            float x = player.getPosition().x;
            float y = player.getPosition().y + 2f;
            double minDist = 1000000;
            Iterator<Entity> virusI = viruses.iterator();
            while (virusI.hasNext()) {
                Entity virus = virusI.next();
                if (virus.isAlive()) {
                    float tempX = (virus.getPosition().x - player.getPosition().x);
                    float tempY = (virus.getPosition().y - player.getPosition().y);
                    double distance = Math.sqrt((double) (tempX * tempX + tempY * tempY));
                    if (distance < minDist) {
                        minDist = distance;
                        x = virus.getPosition().x;
                        y = virus.getPosition().y;
                    }
                }
            }
            double angle;
            double k1 = y - player.getPosition().y;
            double k2 = x - player.getPosition().x;
            double tangent = k1 / k2;
            if (k1 > 0 && k2 > 0) {
                angle = (Math.toDegrees(Math.atan(tangent))) - 90f;
            } else if (k1 > 0 && k2 < 0) {
                angle = (Math.toDegrees(Math.atan(tangent))) + 90f;
            } else if (k1 < 0 && k2 > 0) {
                angle = (Math.toDegrees(Math.atan(tangent))) - 90f;
            } else {
                angle = (Math.toDegrees(Math.atan(tangent))) + 90f;
            }
            pointer.addAction(Actions.rotateTo((float) (angle)));
        } else {
            pointer.addAction(Actions.rotateBy(5f));
        }
    }
    private void pointerUpdatePlat () {
        pointer.setPosition(
                player.getPosition().x * meter - 0.5f * pointer.getWidth(),
                player.getPosition().y * meter - 0.5f * pointer.getHeight()
        );
        if (platelets.size() > 0) {
            float x = player.getPosition().x;
            float y = player.getPosition().y + 2f;
            double minDist = 1000000;
            Iterator<Entity> platI = platelets.iterator();
            while (platI.hasNext()) {
                Entity plat = platI.next();
                if (plat.isAlive()) {
                    float tempX = (plat.getPosition().x - player.getPosition().x);
                    float tempY = (plat.getPosition().y - player.getPosition().y);
                    double distance = Math.sqrt((double) (tempX * tempX + tempY * tempY));
                    if (distance < minDist) {
                        minDist = distance;
                        x = plat.getPosition().x;
                        y = plat.getPosition().y;
                    }
                }
            }
            double angle;
            double k1 = y - player.getPosition().y;
            double k2 = x - player.getPosition().x;
            double tangent = k1 / k2;
            if (k1 > 0 && k2 > 0) {
                angle = (Math.toDegrees(Math.atan(tangent))) - 90f;
            } else if (k1 > 0 && k2 < 0) {
                angle = (Math.toDegrees(Math.atan(tangent))) + 90f;
            } else if (k1 < 0 && k2 > 0) {
                angle = (Math.toDegrees(Math.atan(tangent))) - 90f;
            } else {
                angle = (Math.toDegrees(Math.atan(tangent))) + 90f;
            }
            pointer.addAction(Actions.rotateTo((float) (angle)));
        } else {
            pointer.addAction(Actions.rotateBy(5f));
        }
    }
    private void pointerUpdateRedCell () {
        pointer.setPosition(
                player.getPosition().x * meter - 0.5f * pointer.getWidth(),
                player.getPosition().y * meter - 0.5f * pointer.getHeight()
        );
        if (redCells.size() > 0) {
            float x = player.getPosition().x;
            float y = player.getPosition().y + 2f;
            double minDist = 1000000;
            Iterator<Entity> redCellI = redCells.iterator();
            while (redCellI.hasNext()) {
                Entity redCell = redCellI.next();
                if (redCell.isAlive()) {
                    if (redCell.getName().equals("red_cell_sick")) {
                        float tempX = (redCell.getPosition().x - player.getPosition().x);
                        float tempY = (redCell.getPosition().y - player.getPosition().y);
                        double distance = Math.sqrt((double) (tempX * tempX + tempY * tempY));
                        if (distance < minDist) {
                            minDist = distance;
                            x = redCell.getPosition().x;
                            y = redCell.getPosition().y;
                        }
                    }
                }
            }
            double angle;
            double k1 = y - player.getPosition().y;
            double k2 = x - player.getPosition().x;
            double tangent = k1 / k2;
            if (k1 > 0 && k2 > 0) {
                angle = (Math.toDegrees(Math.atan(tangent))) - 90f;
            } else if (k1 > 0 && k2 < 0) {
                angle = (Math.toDegrees(Math.atan(tangent))) + 90f;
            } else if (k1 < 0 && k2 > 0) {
                angle = (Math.toDegrees(Math.atan(tangent))) - 90f;
            } else {
                angle = (Math.toDegrees(Math.atan(tangent))) + 90f;
            }
            pointer.addAction(Actions.rotateTo((float) (angle)));
        } else {
            pointer.addAction(Actions.rotateBy(5f));
        }
    }
    private void pointerUpdateLymph () {
        pointer.setPosition(
                player.getPosition().x * meter - 0.5f * pointer.getWidth(),
                player.getPosition().y * meter - 0.5f * pointer.getHeight()
        );
        if (lymphocytes.size() > 0) {
            float x = player.getPosition().x;
            float y = player.getPosition().y + 2f;
            double minDist = 1000000;
            Iterator<Entity> lymphI = lymphocytes.iterator();
            while (lymphI.hasNext()) {
                Entity lymph = lymphI.next();
                if (lymph.isAlive()) {
                    if (lymph.getName().equals("lymphocyte_sick")) {
                        float tempX = (lymph.getPosition().x - player.getPosition().x);
                        float tempY = (lymph.getPosition().y - player.getPosition().y);
                        double distance = Math.sqrt((double) (tempX * tempX + tempY * tempY));
                        if (distance < minDist) {
                            minDist = distance;
                            x = lymph.getPosition().x;
                            y = lymph.getPosition().y;
                        }
                    }
                }
            }
            double angle;
            double k1 = y - player.getPosition().y;
            double k2 = x - player.getPosition().x;
            double tangent = k1 / k2;
            if (k1 > 0 && k2 > 0) {
                angle = (Math.toDegrees(Math.atan(tangent))) - 90f;
            } else if (k1 > 0 && k2 < 0) {
                angle = (Math.toDegrees(Math.atan(tangent))) + 90f;
            } else if (k1 < 0 && k2 > 0) {
                angle = (Math.toDegrees(Math.atan(tangent))) - 90f;
            } else {
                angle = (Math.toDegrees(Math.atan(tangent))) + 90f;
            }
            pointer.addAction(Actions.rotateTo((float) (angle)));
        } else {
            pointer.addAction(Actions.rotateBy(5f));
        }
    }

    private void spawnPart () {
        float spawnX = meter*spec.get().position.x - 0.5f*meter*spec.get().viewportWidth + rand.nextInt((int)(meter*spec.get().viewportWidth));
        float spawnY = meter*spec.get().position.y - 0.5f*meter*spec.get().viewportHeight + rand.nextInt((int)(meter*spec.get().viewportHeight));
        float length = rand.nextInt((int)(0.01f*meter*spec.get().viewportWidth)) + 0.01f*meter*spec.get().viewportWidth;
        AdvSprite particle = new AdvSprite(
                game.atlas.createSprite("particle"),
                spawnX,
                spawnY,
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
        particle.setColor(new Color((rand.nextInt(25) + 230)/255f, (rand.nextInt(15) + 1)/255f, (rand.nextInt(50) + 25)/255f, 1f));
        stage.addActor(particle);
        partSpec.addFirst(particle);
    }
    private void changePart (AdvSprite e) {
        e.addAction(Actions.alpha(0f));
        float x = meter*spec.get().position.x - meter*spec.get().viewportWidth + rand.nextInt((int)(2f*meter*spec.get().viewportWidth));
        float y = meter*spec.get().position.y - meter*spec.get().viewportHeight + rand.nextInt((int)(2f*meter*spec.get().viewportHeight));
        e.setPosition(x, y);
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
    }
    private void spawnPartPlayer () {
        float spawnX = meter*spec.get().position.x;
        float spawnY = meter*spec.get().position.y;
        float length = rand.nextInt((int)(0.01f*meter*spec.get().viewportWidth)) + 0.01f*meter*spec.get().viewportWidth;
        AdvSprite particle = new AdvSprite(
                game.atlas.createSprite("particle"),
                spawnX,
                spawnY,
                length,
                length);
        particle.getSprite().setAlpha(0f);
        float lifeTime = 1 + rand.nextFloat();
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
        particle.setColor(new Color((rand.nextInt(30) + 55)/255f, (rand.nextInt(30) + 155)/255f, (rand.nextInt(30) + 195)/255f, 1f));
        stage.addActor(particle);
        partPlayer.addFirst(particle);
    }
    private void changePartPlayer (AdvSprite e) {
        e.addAction(Actions.alpha(0f));
        float x = meter*spec.get().position.x;
        float y = meter*spec.get().position.y;
        e.setPosition(x, y);
        float lifeTime = 1 + rand.nextFloat();
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
    }
    private void addBounds () {
        for (int i = 0; i < 20; i++) {
            AdvSprite boundSpr = new AdvSprite(game.atlas.createSprite("bound_left"), -15f, -15f + i * 5f, 5f, 5f);
            bounds.addFirst(new Bound(boundSpr, world));
            boundSpr = new AdvSprite(game.atlas.createSprite("bound_right"), -20f, -15f + i * 5f, 5f, 5f);
            bounds.addFirst(new Bound(boundSpr, world));
        }
        for (int i = 0; i < 20; i++) {
            AdvSprite boundSpr = new AdvSprite(game.atlas.createSprite("bound_right"), 80f, -15f + i * 5f, 5f, 5f);
            bounds.addFirst(new Bound(boundSpr, world));
            boundSpr = new AdvSprite(game.atlas.createSprite("bound_left"), 85f, -15f + i * 5f, 5f, 5f);
            bounds.addFirst(new Bound(boundSpr, world));
        }
        for (int i = 0; i < 20; i++) {
            AdvSprite boundSpr = new AdvSprite(game.atlas.createSprite("bound_down"), -15f + i * 5f, -15f, 5f, 5f);
            bounds.addFirst(new Bound(boundSpr, world));
            boundSpr = new AdvSprite(game.atlas.createSprite("bound_up"), -15f + i * 5f, -20f, 5f, 5f);
            bounds.addFirst(new Bound(boundSpr, world));
        }
        for (int i = 0; i < 20; i++) {
            AdvSprite boundSpr = new AdvSprite(game.atlas.createSprite("bound_up"), -15f + i * 5f, 80f, 5f, 5f);
            bounds.addFirst(new Bound(boundSpr, world));
            boundSpr = new AdvSprite(game.atlas.createSprite("bound_down"), -15f + i * 5f, 85f, 5f, 5f);
            bounds.addFirst(new Bound(boundSpr, world));
        }

        for (Bound b : bounds) {
            stage.addActor(b);
        }
    }

    private void spawnRedCell (int hp, Vector2 vec, boolean isSick) {
        AdvSprite redEntity = new AdvSprite(game.atlas.createSprite("red"), 0, 0, 1f, 1f);
        Entity redSubject = new Entity(redEntity, world, 0f, 10f, 0.2f, "red_cell");
        RedCell redCell = new RedCell(redSubject, hp);
        redCell.getBody().setTransform(vec.x, vec.y, 0f);
        if (rand.nextInt(2) == 0)
            redCell.getBody().setAngularVelocity(rand.nextFloat() + 0.1f);
        else
            redCell.getBody().setAngularVelocity(-rand.nextFloat() - 0.1f);
        float sizeX = redCell.getWidth(), sizeY = redCell.getHeight();
        redCell.setSize(0f, 0f);
        redCell.addAction(Actions.alpha(0f));
        redCell.addAction(Actions.parallel(
                Actions.sizeTo(sizeX, sizeY, 1f),
                Actions.alpha(1f, 1f)
        ));
        if (isSick) {
            redCell.setColor(Color.GRAY);
            redCell.setName("red_cell_sick");
        }
        stage.addActor(redCell);
        redCells.addFirst(redCell);
    }
    private void spawnPlatelet (Vector2 vec) {
        AdvSprite platEnt = new AdvSprite(game.atlas.createSprite("yellow"), 0, 0, 0.5f, 0.5f);
        Entity platSub = new Entity(platEnt, world, 0f, 3f, 0.2f, "platelet");
        Platelet platelet = new Platelet(platSub, 100);
        platelet.getBody().setTransform(vec.x, vec.y, 0f);
        if (rand.nextInt(2) == 0)
            platelet.getBody().setAngularVelocity(rand.nextFloat() + 0.1f);
        else
            platelet.getBody().setAngularVelocity(-rand.nextFloat() - 0.1f);
        float sizeX = platelet.getWidth(), sizeY = platelet.getHeight();
        platelet.setSize(0f, 0f);
        platelet.addAction(Actions.alpha(0f));
        platelet.addAction(Actions.parallel(
                Actions.sizeTo(sizeX, sizeY, 1f),
                Actions.alpha(1f, 1f)
        ));
        stage.addActor(platelet);
        platelets.addFirst(platelet);
    }
    private void spawnVirus (int hp, float speed, Vector2 vec, int type) {
        AdvSprite virusEnt;
        Entity virusSub;
        if (type > 19) {
            virusEnt = new AdvSprite(game.atlas.createSprite("virus", type), 0, 0, 8f, 8f);
            virusSub = new Entity(virusEnt, world, 0f, 30f, 0.2f, "virus_boss");
        } else if (type > 15) {
            virusEnt = new AdvSprite(game.atlas.createSprite("virus", type), 0, 0, 2f, 2f);
            virusSub = new Entity(virusEnt, world, 0f, 15f, 0.2f, "virus_super_advanced");
            hp *= 2;
            speed += 45f;
        } else if (type > 10) {
            virusEnt = new AdvSprite(game.atlas.createSprite("virus", type), 0, 0, 1f, 1f);
            virusSub = new Entity(virusEnt, world, 0f, 5f, 0.2f, "virus_advanced");
            hp *= 1.5;
            speed += 3f;
        } else {
            virusEnt = new AdvSprite(game.atlas.createSprite("virus", type), 0, 0, 0.5f, 0.5f);
            virusSub = new Entity(virusEnt, world, 0f, 3f, 0.2f, "virus");
        }
        Virus virus = new Virus(virusSub, hp, speed);
        virus.getBody().setTransform(vec.x, vec.y, 0f);
        if (rand.nextInt(2) == 0)
            virus.getBody().setAngularVelocity(rand.nextFloat() + 1f);
        else
            virus.getBody().setAngularVelocity(-rand.nextFloat() - 1f);
        float sizeX = virus.getWidth(), sizeY = virus.getHeight();
        virus.setSize(0f, 0f);
        virus.addAction(Actions.alpha(0f));
        virus.addAction(Actions.parallel(
                Actions.sizeTo(sizeX, sizeY, 1f),
                Actions.alpha(1f, 1f)
        ));
        stage.addActor(virus);
        viruses.addFirst(virus);
    }
    private void spawnLymph (Vector2 vec, float speed, boolean isSick) {
        AdvSprite lymEntity = new AdvSprite(game.atlas.createSprite("lymphocyte", rand.nextInt(7) + 1), 0, 0, 1f, 1f);
        Entity lymSubject = new Entity(lymEntity, world, 0f, 8f, 0.2f, "lymphocyte");
        Lymphocyte lymphocyte = new Lymphocyte(lymSubject, 1000000, speed);
        lymphocyte.getBody().setTransform(vec.x, vec.y, 0f);
        if (rand.nextInt(2) == 0)
            lymphocyte.getBody().setAngularVelocity(rand.nextFloat() + 0.3f);
        else
            lymphocyte.getBody().setAngularVelocity(-rand.nextFloat() - 0.3f);
        float sizeX = lymphocyte.getWidth(), sizeY = lymphocyte.getHeight();
        lymphocyte.setSize(0f, 0f);
        lymphocyte.addAction(Actions.alpha(0f));
        lymphocyte.addAction(Actions.parallel(
                Actions.sizeTo(sizeX, sizeY, 1f),
                Actions.alpha(1f, 1f)
        ));
        if (isSick) {
            lymphocyte.setName("lymphocyte_sick");
            lymphocyte.setColor(Color.GRAY);
        }
        stage.addActor(lymphocyte);
        lymphocytes.addFirst(lymphocyte);
    }
    private void spawnGreenBonus (Vector2 vec) {
        AdvSprite gBonusE = new AdvSprite(game.atlas.createSprite("green_card"), 0, 0, 0.5f, 0.5f);
        Entity gBonusS = new Entity(gBonusE, world, 0f, 3f, 0.2f, "green_bonus");
        gBonusS.setLT(100);
        gBonusS.getBody().setTransform(vec.x, vec.y, 0f);
        if (rand.nextInt(2) == 0)
            gBonusS.getBody().setAngularVelocity(rand.nextFloat() + 0.1f);
        else
            gBonusS.getBody().setAngularVelocity(-rand.nextFloat() - 0.1f);
        float sizeX = gBonusS.getWidth(), sizeY = gBonusS.getHeight();
        gBonusS.setSize(0f, 0f);
        gBonusS.addAction(Actions.alpha(0f));
        gBonusS.addAction(Actions.parallel(
                Actions.sizeTo(sizeX, sizeY, 1f),
                Actions.alpha(1f, 1f)
        ));

        stage.addActor(gBonusS);
        bonuses.addFirst(gBonusS);
    }
    private void spawnTimeBonus (Vector2 vec) {
        AdvSprite gBonusE = new AdvSprite(game.atlas.createSprite("time"), 0, 0, 0.5f, 0.5f);
        Entity gBonusS = new Entity(gBonusE, world, 0f, 3f, 0.2f, "time_bonus");
        gBonusS.setLT(100);
        gBonusS.getBody().setTransform(vec.x, vec.y, 0f);
        if (rand.nextInt(2) == 0)
            gBonusS.getBody().setAngularVelocity(rand.nextFloat() + 0.1f);
        else
            gBonusS.getBody().setAngularVelocity(-rand.nextFloat() - 0.1f);
        float sizeX = gBonusS.getWidth(), sizeY = gBonusS.getHeight();
        gBonusS.setSize(0f, 0f);
        gBonusS.addAction(Actions.alpha(0f));
        gBonusS.addAction(Actions.parallel(
                Actions.sizeTo(sizeX, sizeY, 1f),
                Actions.alpha(1f, 1f)
        ));

        stage.addActor(gBonusS);
        bonuses.addFirst(gBonusS);
    }
    private void spawnDirBonus (Vector2 vec) {
        AdvSprite gBonusE = new AdvSprite(game.atlas.createSprite("direction"), 0, 0, 0.5f, 0.5f);
        Entity gBonusS = new Entity(gBonusE, world, 0f, 3f, 0.2f, "direction_bonus");
        gBonusS.setLT(100);
        gBonusS.getBody().setTransform(vec.x, vec.y, 0f);
        if (rand.nextInt(2) == 0)
            gBonusS.getBody().setAngularVelocity(rand.nextFloat() + 0.1f);
        else
            gBonusS.getBody().setAngularVelocity(-rand.nextFloat() - 0.1f);
        float sizeX = gBonusS.getWidth(), sizeY = gBonusS.getHeight();
        gBonusS.setSize(0f, 0f);
        gBonusS.addAction(Actions.alpha(0f));
        gBonusS.addAction(Actions.parallel(
                Actions.sizeTo(sizeX, sizeY, 1f),
                Actions.alpha(1f, 1f)
        ));

        stage.addActor(gBonusS);
        bonuses.addFirst(gBonusS);
    }

    private void gravity (Body b, float s) {
        float velX = b.getLinearVelocity().x;
        float velY = b.getLinearVelocity().y;
        if (velX > 0f) {
            b.setLinearVelocity(velX - s, velY);
        } else if (velX < 0f) {
            b.setLinearVelocity(velX + s, velY);
        }
        velX = b.getLinearVelocity().x;
        if (velY > 0f) {
            b.setLinearVelocity(velX, velY - s);
        } else if (velY < 0f) {
            b.setLinearVelocity(velX, velY + s);
        }
    }
    private void lifeCycle () {
        if (curLevel == 1){
            idleLevelTrain1();
        } else if (curLevel == 2) {
            idleLevelTrain2();
        } else if (curLevel == 3) {
            idleLevelTrain3();
        } else if (curLevel == 4) {
            idleLevelTrain4();
        } else {
            idleLevels();
        }
        player.updateSprite(spec.get());
        playerSearch();
        lymphSearch();
        Iterator<Entity> vI = viruses.iterator();
        while (vI.hasNext()) {
            Entity v = vI.next();
            v.updateSprite(spec.get());
            v.updateLife();
            v.decreaseLT();
            if (v.getLifeTime() <= 0) {
                if (v.isAlive()) {
                    v.kill();
                }
                if (!v.hasActions()){
                    v.getWorld().destroyBody(v.getBody());
                    vI.remove();
                }
            }
        }
        Iterator<Entity> redCellI = redCells.iterator();
        while (redCellI.hasNext()) {
            Entity r = redCellI.next();
            r.updateSprite(spec.get());
            r.updateLife();
            r.decreaseLT();
            if (r.getLifeTime() <= 0) {
                if (r.isAlive()) {
                    r.kill();
                }
                if (!r.hasActions()){
                    r.getWorld().destroyBody(r.getBody());
                    redCellI.remove();
                }
            }
        }
        Iterator<Entity> platI = platelets.iterator();
        while (platI.hasNext()) {
            Entity p = platI.next();
            p.updateSprite(spec.get());
            if (p.getName().equals("platelet_s")) {
                p.updateLife(player.getPosition().x, player.getPosition().y);
            } else {
                p.decreaseLT();
                p.updateLife();
            }
            if (p.getLifeTime() <= 0) {
                if (p.isAlive()) {
                    p.kill();
                }
                if (!p.hasActions()){
                    p.getWorld().destroyBody(p.getBody());
                    platI.remove();
                }
            }
        }
        Iterator<Entity> lymphI = lymphocytes.iterator();
        while (lymphI.hasNext()) {
            Entity l = lymphI.next();
            l.updateSprite(spec.get());
            l.updateLife();
            l.decreaseLT();
            if (l.getLifeTime() <= 0) {
                if (l.isAlive()) {
                    l.kill();
                }
                if (!l.hasActions()){
                    l.getWorld().destroyBody(l.getBody());
                    lymphI.remove();
                }
            }
        }
        Iterator<Entity> bonusI = bonuses.iterator();
        while (bonusI.hasNext()) {
            Entity b = bonusI.next();
            b.updateSprite(spec.get());
            b.updateLife();
            b.decreaseLT();
            if (b.getLifeTime() <= 0) {
                if (b.isAlive()) {
                    b.kill();
                }
                if (!b.hasActions()){
                    b.getWorld().destroyBody(b.getBody());
                    bonusI.remove();
                }
            }
        }
        for (int i = 0; i < partSpec.size(); i++) {
            partSpec.get(i).updateSprite(spec.get());
            if (!partSpec.get(i).hasActions()) {
                changePart(partSpec.get(i));
            }
        }
        for (int i = 0; i < partPlayer.size(); i++) {
            partPlayer.get(i).updateSprite(spec.get());
            if (!partPlayer.get(i).hasActions()) {
                changePartPlayer(partPlayer.get(i));
            }
        }
        for (int i = 0; i < bounds.size(); i++) {
            bounds.get(i).updateSprite(spec.get());
        }
    }
    private void lymphSearch () {
        Iterator<Entity> lymphI = lymphocytes.iterator();
        while (lymphI.hasNext()) {
            Entity lymph = lymphI.next();
            lymph.updateSprite(spec.get());
            if (lymph.isAlive() && lymph.getName().equals("lymphocyte")) {
                float x = lymph.getPosition().x;
                float y = lymph.getPosition().y;
                double minDist = 1000000;
                Iterator<Entity> virusI = viruses.iterator();
                while (virusI.hasNext()) {
                    Entity virus = virusI.next();
                    float tempX = (virus.getPosition().x - lymph.getPosition().x);
                    float tempY = (virus.getPosition().y - lymph.getPosition().y);
                    double distance = Math.sqrt((double)(tempX * tempX + tempY * tempY));
                    if (distance < minDist) {
                        minDist = distance;
                        x = virus.getPosition().x;
                        y = virus.getPosition().y;
                    }
                }
                lymph.updateLife(x, y);
            }
        }
    }
    private void playerSearch () {
        float x = player.getPosition().x;
        float y = player.getPosition().y;
        double minDist = 1000000;
        Iterator<Entity> virusI = viruses.iterator();
        while (virusI.hasNext()) {
            Entity virus = virusI.next();
            if (virus.isAlive()) {
                float tempX = (virus.getPosition().x - player.getPosition().x);
                float tempY = (virus.getPosition().y - player.getPosition().y);
                double distance = Math.sqrt((double) (tempX * tempX + tempY * tempY));
                if (distance < minDist) {
                    minDist = distance;
                    x = virus.getPosition().x;
                    y = virus.getPosition().y;
                }
            }
        }
        if (minDist * meter < 2f * meter)
            player.updateLife(x, y);
    }

    private void initLevelTrain1 () {
        spawnVirus(2000, 1f - rand.nextFloat() / 2f, new Vector2(60f, 60f), 18);
    }
    private void initLevelTrain2 () {
        target = 5;
        timer = 60 * 360;
        givenTime =  60 * 360;
    }
    private void initLevelTrain3 () {
        for (int i = 0; i < 5; i++) {
            spawnPlatelet(getRandSpawnLoc());
            spawnRedCell(1000000, getRandSpawnLoc(), true);
        }
        timer = 60 * 360;
        givenTime = 60 * 360;
        target = 5;
    }
    private void initLevelTrain4 () {
        timer = 60 * 360;
        givenTime = 60 * 360;
        spawnLymph(getRandSpawnLoc(), 6f, false);
        target = 5;
    }
    private void initLevels () {
        if (curLevel < 5) {
          switch (curLevel) {
              case 4: {
                  initLevelTrain4();
                  break;
              }
              case 3: {
                  initLevelTrain3();
                  break;
              }
              case 2: {
                  initLevelTrain2();
                  break;
              }
              case 1: {
                  initLevelTrain1 ();
                  break;
              }
          }
        } else if (curLevel == 101) {
            timer = 60 * 120;
            givenTime = 60 * 120;
            for (int i = 0; i < 5; i++) {
                spawnPlatelet(getRandSpawnLoc());
                spawnVirus(100, 1f - rand.nextFloat() / 2f, getRandSpawnLoc(), rand.nextInt(19) + 1);
                spawnRedCell(100, getRandSpawnLoc(), false);
            }
            isVictory = true;
        } else  if (curLevel % 10 == 0) {
            timer = 60 * 120;
            givenTime = 60 * 120;
            spawnVirus(50000, 0.1f, new Vector2(50f, 50f), curLevel / 10 + 19);
            for (int i = 0; i < 5; i++) {
                spawnLymph(getRandSpawnLoc(), 5f - (curLevel / 100f), true);
                spawnPlatelet(getRandSpawnLoc());
                spawnRedCell(100, getRandSpawnLoc(), false);
            }
        } else {
            switch (curLevel % 4) {
                case 1: {
                    for (int i = 0; i < 5; i++) spawnVirus(150 + curLevel, 1f - rand.nextFloat() / 2f, getRandSpawnLoc(), rand.nextInt(19) + 1);
                    target = 5 + (curLevel % 21) / 4 + (curLevel / 20);
                    timer = 60 * 60 + (curLevel / 10) * 60 + (curLevel < 21 ? 60 * (20 - curLevel) : 0);
                    givenTime = 60 * 60 + (curLevel / 10) * 60 + (curLevel < 21 ? 60 * (20 - curLevel) : 0);
                    break;
                }
                case 2: {
                    for (int i = 0; i < 5; i++) spawnVirus(150 + curLevel, 1f - rand.nextFloat() / 2f, getRandSpawnLoc(), rand.nextInt(19) + 1);
                    int number = 1;
                    target = 5 + (curLevel % 21) / 4 + (curLevel / 20);
                    number += (curLevel % 21) / 10;
                    for (int i = 0; i < number; i++) spawnLymph(getRandSpawnLoc(), 3f + (curLevel / 100f), false);
                    break;
                }
                case 3: {
                    target = 5 + (curLevel % 21) / 4 + (curLevel / 20);
                    timer = 60 * 80 + (curLevel / 10) * 60 + (curLevel < 21 ? 60 * (20 - curLevel) : 0);
                    givenTime = 60 * 80 + (curLevel / 10) * 60 + (curLevel < 21 ? 60 * (20 - curLevel) : 0);
                    for (int i = 0; i < 5; i++) {
                        spawnPlatelet(getRandSpawnLoc());
                        spawnRedCell(100, getRandSpawnLoc(), true);
                    }
                    break;
                }
                case 0: {
                    int number = 6;
                    target = 10 + (curLevel % 21) / 4 + (curLevel / 20);
                    timer = 60 * 60 + (curLevel / 10) * 60;
                    givenTime = 60 * 60 + (curLevel / 10) * 60 + (curLevel < 21 ? 60 * (20 - curLevel) : 0);
                    number -= (curLevel % 21) / 4 + curLevel / 60 + (curLevel < 21 ? 60 * (20 - curLevel) : 0);
                    for (int i = 0; i < number; i++) spawnLymph(getRandSpawnLoc(), 4f - (curLevel / 100f), false);
                    for (int i = 0; i < 5; i++) spawnVirus(150 + curLevel, 1f - rand.nextFloat() / 2f, getRandSpawnLoc(), rand.nextInt(19) + 1);
                    break;
                }
            }
        }
    }

    private void idleLevelTrain1 () {
        if (rand.nextInt(50) == 0) spawnRedCell(100, getRandSpawnLoc(), false);
        if (rand.nextInt(75) == 0) spawnPlatelet(getRandSpawnLoc());
        if (viruses.size() == 0) {
            isVictory = true;
            isGameOver = true;
        }
    }
    private void idleLevelTrain2 () {
        if (rand.nextInt(100) == 0) spawnRedCell(100, getRandSpawnLoc(), false);
        if (rand.nextInt(100) == 0) spawnPlatelet(getRandSpawnLoc());
        if (rand.nextInt(150) == 0) spawnVirus(150, 1f - rand.nextFloat() / 2f, getRandSpawnLoc(), rand.nextInt(19) + 1);
        timer--;
        if (playerScore >= target) {
            isVictory = true;
            isGameOver = true;
        }
        if (timer < 0) {
            isGameOver = true;
        }
    }
    private void idleLevelTrain3 () {
        timer--;
        if (rand.nextInt(100) == 0) spawnPlatelet(getRandSpawnLoc());
        if (rand.nextInt(100) == 0) spawnRedCell(100, getRandSpawnLoc(), true);
        if (playerScore >= target) {
            isVictory = true;
            isGameOver = true;
        }
        if (timer < 0) {
            isGameOver = true;
        }
    }
    private void idleLevelTrain4 () {
        if (rand.nextInt(100) == 0) spawnRedCell(100, getRandSpawnLoc(), false);
        if (rand.nextInt(100) == 0) spawnPlatelet(getRandSpawnLoc());
        if (rand.nextInt(75) == 0) spawnVirus(150, 1f - rand.nextFloat() / 2f, getRandSpawnLoc(), rand.nextInt(19) + 1);
        timer--;
        if (playerScore + lymphScore >= target) {
            isGameOver = true;
            isVictory = true;
        }
        if (timer < 0) {
            isGameOver = true;
        }
    }
    private void idleLevels () {
        if (curLevel == 101) {
            if (rand.nextInt(150) == 0) spawnRedCell(100, getRandSpawnLoc(), false);
            if (rand.nextInt(150) == 0) spawnPlatelet(getRandSpawnLoc());
            if (rand.nextInt(150) == 0) spawnVirus(150 + curLevel, 1f - rand.nextFloat() / 2f, getRandSpawnLoc(), rand.nextInt(19) + 1);
            if (rand.nextInt(500) == 0) spawnGreenBonus(getRandSpawnLoc());
            if (rand.nextInt(500 + 25 * Technical.dirLevel) == 0) spawnDirBonus(getRandSpawnLoc());
            if (rand.nextInt(250) == 0) spawnTimeBonus(getRandSpawnLoc());
            timer--;
            if (dirTimer > 0) dirTimer--;
            if (timer < 0) {
                isGameOver = true;
            }
        } else if (curLevel % 10 == 0) {
            if (rand.nextInt(150) == 0) spawnRedCell(100, getRandSpawnLoc(), false);
            if (rand.nextInt(150) == 0) spawnPlatelet(getRandSpawnLoc());
            if (rand.nextInt(500) == 0) spawnGreenBonus(getRandSpawnLoc());
            if (rand.nextInt(500 + 25 * Technical.dirLevel) == 0) spawnDirBonus(getRandSpawnLoc());
            if (rand.nextInt(500 + 25 * Technical.timeLevel) == 0) spawnTimeBonus(getRandSpawnLoc());
            boolean isBossKilled = true;
            float bossX = 0, bossY = 0;
            for (Entity e : viruses) {
                if (e.getName().equals("virus_boss")) {
                    bossX = e.getPosition().x;
                    bossY = e.getPosition().y;
                    isBossKilled = false;
                    break;
                }
            }
            if (isBossKilled) {
                isVictory = true;
                isGameOver = true;
            }
            int x = 4 - (rand.nextInt(9));
            bossX += x;
            bossY = rand.nextInt(2) == 0 ? bossY + (float)Math.sqrt(16 - x * x) : bossY - (float)Math.sqrt(16 - x * x);
            if (rand.nextInt(100) == 0) spawnVirus(30, 0f, new Vector2(bossX, bossY), rand.nextInt(10) + 1);
            timer--;
            if (dirTimer > 0) dirTimer--;
            if (timer < 0) {
                isGameOver = true;
            }
        } else {
            switch (curLevel % 4) {
                case 1: {
                    if (rand.nextInt(150) == 0) spawnRedCell(100, getRandSpawnLoc(), false);
                    if (rand.nextInt(150) == 0) spawnPlatelet(getRandSpawnLoc());
                    if (rand.nextInt(150) == 0) spawnVirus(150 + curLevel, 1f - rand.nextFloat() / 2f, getRandSpawnLoc(), rand.nextInt(19) + 1);
                    if (rand.nextInt(500) == 0) spawnGreenBonus(getRandSpawnLoc());
                    if (rand.nextInt(500 + 25 * Technical.timeLevel) == 0) spawnTimeBonus(getRandSpawnLoc());
                    if (rand.nextInt(500 + 25 * Technical.dirLevel) == 0) spawnDirBonus(getRandSpawnLoc());
                    timer--;
                    if (dirTimer > 0) dirTimer--;
                    if (playerScore >= target) {
                        isVictory = true;
                        isGameOver = true;
                    }
                    if (timer < 0) {
                        isGameOver = true;
                    }
                    break;
                }
                case 2: {
                    if (rand.nextInt(150) == 0) spawnRedCell(100, getRandSpawnLoc(), false);
                    if (rand.nextInt(150) == 0) spawnPlatelet(getRandSpawnLoc());
                    if (rand.nextInt(100) == 0) spawnVirus(150 + curLevel, 1f - rand.nextFloat() / 2f, getRandSpawnLoc(), rand.nextInt(19) + 1);
                    if (rand.nextInt(500) == 0) spawnGreenBonus(getRandSpawnLoc());
                    if (rand.nextInt(500 + 25 * Technical.dirLevel) == 0) spawnDirBonus(getRandSpawnLoc());
                    if (dirTimer > 0) dirTimer--;
                    if (playerScore >= target) {
                        isGameOver = true;
                        isVictory = true;
                    }
                    if (lymphScore >= target) {
                        isGameOver = true;
                    }
                    break;
                }
                case 3: {
                    timer--;
                    if (dirTimer > 0) dirTimer--;
                    if (rand.nextInt(100) == 0) spawnPlatelet(getRandSpawnLoc());
                    if (rand.nextInt(100) == 0) spawnRedCell(100, getRandSpawnLoc(), true);
                    if (rand.nextInt(500) == 0) spawnGreenBonus(getRandSpawnLoc());
                    if (rand.nextInt(500 + 25 * Technical.timeLevel) == 0) spawnTimeBonus(getRandSpawnLoc());
                    if (rand.nextInt(500 + 25 * Technical.dirLevel) == 0) spawnDirBonus(getRandSpawnLoc());
                    if (playerScore >= target) {
                        isVictory = true;
                        isGameOver = true;
                    }
                    if (timer < 0) {
                        isGameOver = true;
                    }
                    break;
                }
                case 0: {
                    if (rand.nextInt(150) == 0) spawnRedCell(100, getRandSpawnLoc(), false);
                    if (rand.nextInt(150) == 0) spawnPlatelet(getRandSpawnLoc());
                    if (rand.nextInt(100) == 0) spawnVirus(150 + curLevel, 1f - rand.nextFloat() / 2f, getRandSpawnLoc(), rand.nextInt(19) + 1);
                    if (rand.nextInt(500) == 0) spawnGreenBonus(getRandSpawnLoc());
                    if (rand.nextInt(500 + 25 * Technical.timeLevel) == 0) spawnTimeBonus(getRandSpawnLoc());
                    if (rand.nextInt(500 + 25 * Technical.dirLevel) == 0) spawnDirBonus(getRandSpawnLoc());
                    timer--;
                    if (dirTimer > 0) dirTimer--;
                    if (playerScore + lymphScore >= target) {
                        isGameOver = true;
                        isVictory = true;
                    }
                    if (timer < 0) {
                        isGameOver = true;
                    }
                    break;
                }
            }
        }
    }

    private void drawText ()  {
        String fps = "FPS: " + Gdx.graphics.getFramesPerSecond();
        game.fonts.smallB.draw(
                stage.getBatch(),
                fps,
                7.9f*meter - game.fonts.smallB.getWidth(fps),
                spec.get().viewportHeight * meter - 0.1f*meter - 5f*game.fonts.smallB.getHeight("A")
        );
        if (curLevel == 4) {
            game.fonts.smallB.draw(
                    stage.getBatch(),
                    ": " + (playerScore + lymphScore) + "/" + target,
                    0.1f * meter + 0.125f * game.width,
                    game.height - 0.075f * game.width + 0.5f * game.fonts.smallB.getHeight("A")
            );
        } else if (curLevel == 2) {
            game.fonts.smallB.draw(
                    stage.getBatch(),
                    ": " + (playerScore) + "/" + target,
                    0.1f*meter + 0.125f*game.width,
                    game.height - 0.075f*game.width + 0.5f*game.fonts.smallB.getHeight("A")
            );
        } else if (curLevel != 1) {
            if (curLevel % 10 != 0) {
                switch (curLevel % 4) {
                    case 1: {
                        game.fonts.smallB.draw(
                                stage.getBatch(),
                                ": " + (playerScore) + "/" + target,
                                0.1f * meter + 0.125f * game.width,
                                game.height - 0.075f * game.width + 0.5f * game.fonts.smallB.getHeight("A")
                        );
                        break;
                    }
                    case 2: {
                        game.fonts.smallB.draw(
                                stage.getBatch(),
                                ": " + playerScore + "/" + target,
                                0.1f * meter + 0.125f * game.width,
                                game.height - 0.075f * game.width + 0.5f * game.fonts.smallB.getHeight("A")
                        );
                        game.fonts.smallB.draw(
                                stage.getBatch(),
                                ": " + lymphScore + "/" + target,
                                0.1f * meter + 0.125f * game.width,
                                game.height - 0.2f * game.width + 0.5f * game.fonts.smallB.getHeight("A")
                        );
                        break;
                    }
                    case 3: {
                        game.fonts.smallB.draw(
                                stage.getBatch(),
                                ": " + playerScore + "/" + target,
                                0.1f * meter + 0.125f * game.width,
                                game.height - 0.075f * game.width + 0.5f * game.fonts.smallB.getHeight("A")
                        );
                        break;
                    }
                    case 0: {
                        game.fonts.smallB.draw(
                                stage.getBatch(),
                                ": " + (playerScore + lymphScore) + "/" + target,
                                0.1f * meter + 0.125f * game.width,
                                game.height - 0.075f * game.width + 0.5f * game.fonts.smallB.getHeight("A")
                        );
                        break;
                    }
                }
            }
        }
    }

    private Vector2 getRandSpawnLoc () {
        return new Vector2(
                rand.nextInt(60) + rand.nextFloat(),
                rand.nextInt(60) + rand.nextFloat()
        );
    }

    @Override
    public void resize (int width, int height) {

    }

    @Override
    public void pause () {
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
    public void resume () {
        if (!game.sounds.mainTheme.isPlaying()) if (Technical.isMusicOn) game.sounds.mainTheme.play();
    }

    @Override
    public void hide () {
        dispose();
    }

    @Override
    public void dispose () {
        game.prefs.putInteger("max_level", Technical.maxLevel);
        game.prefs.putInteger("current_skin", Technical.curSkin);
        game.prefs.putInteger("money", Technical.money);
        game.prefs.putInteger("time_level", Technical.timeLevel);
        game.prefs.putInteger("direction_level", Technical.dirLevel);
        game.prefs.putBoolean("is_sound_on", Technical.isSoundOn);
        game.prefs.putBoolean("is_music_on", Technical.isMusicOn);
        game.prefs.flush();
    }

    @Override
    public boolean touchDown (float x, float y, int pointer, int button) {
        return false;
    }

    @Override
    public boolean tap (float x, float y, int count, int button) {
        return false;
    }

    @Override
    public boolean longPress (float x, float y) {
        return false;
    }

    @Override
    public boolean fling (float velocityX, float velocityY, int button) {
        if (!isGameOver && !isPaused) {
            if (Gdx.app.getType().equals(Application.ApplicationType.Android)) {
                if (Technical.isSoundOn) game.sounds.swim.play();
                player.applyForceToTheCentre(
                        0.5f * velocityX,
                        -(0.5f * velocityY),
                        true
                );
            } else {
                if (Technical.isSoundOn) game.sounds.swim.play();
                player.applyForceToTheCentre(
                        0.75f * velocityX,
                        -(0.75f * velocityY),
                        true
                );
            }
        }
        return true;
    }

    @Override
    public boolean pan (float x, float y, float deltaX, float deltaY) {
        return false;
    }

    @Override
    public boolean panStop (float x, float y, int pointer, int button) {
        return false;
    }

    @Override
    public boolean zoom (float initialDistance, float distance) {
        return false;
    }

    @Override
    public boolean pinch (Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
        return false;
    }

    @Override
    public void pinchStop () {

    }
}
