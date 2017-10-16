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
    private LinkedList<Entity> redCells;
    private LinkedList<Entity> platelets;
    private LinkedList<Entity> lymphocytes;
    private LinkedList<Entity> viruses;
    private LinkedList<Bound> bounds;
    private Entity player;

    private World world;
    private ContactListener contactListener;
    private static final float STEP_TIME = 1f / 60f;
    private static final int VELOCITY_ITERATIONS = 5;
    private static final int POSITION_ITERATIONS = 3;
    private float accumulator = 0f;
    private float meter = Technical.METER;

    private boolean isGameOver = false;
    private boolean isVictory = false;
    private int playerScore = 0;
    private int lymphScore = 0;

    private int curLevel;

    private AdvSprite lymphSign;
    private AdvSprite lAndPSign;
    private AdvSprite playerSign;
    private AdvSprite virusSign1;
    private AdvSprite virusSign2;
    private AdvSprite redCellSign;
    private AdvSprite pointer;

    private long timer = 0;
    private int target = 0;
    private boolean hasPlatelet = false;

    private BLButton exit;
    private BLButton pause;
    private AdvSprite pauseObf;
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
        redCells = new LinkedList<Entity>();
        viruses = new LinkedList<Entity>();
        lymphocytes = new LinkedList<Entity>();
        platelets = new LinkedList<Entity>();
        bounds = new LinkedList<Bound>();
        partSpec = new LinkedList<AdvSprite>();
    }

    @Override
    public void show () {
        game.sounds.mainTheme.setLooping(true);
        game.sounds.mainTheme.setVolume(0.25f);
        if (Menu.isSoundOn) game.sounds.mainTheme.play();

        Box2D.init();
        contactListenerInit();

        AdvSprite playerEnt = new AdvSprite(game.atlas.createSprite("white"), 0f, 0f, 1f, 1f);
        player = new Entity(playerEnt, world, 0f, 5f, 0.2f, "player");
        player.getBody().setAngularVelocity(0.25f);

        addBounds();
        for (int i = 0; i < rand.nextInt(10) + 20; i++) { spawnPart(); }

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
        //Gdx.gl.glClearColor(255f/255f, 255f/255f, 255f/255f, 0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        buttonUpdate();
        if (!isPaused) {
            float s = 0.01f * spec.get().viewportHeight;
            gravity(player.getBody(), s);
            for (Entity r : redCells) gravity(r.getBody(), s);
            for (Entity l : lymphocytes) gravity(l.getBody(), s);
            for (Entity v : viruses) gravity(v.getBody(), s);
            for (Entity t : platelets) gravity(t.getBody(), s);

            if (!isGameOver) lifeCycle();

            UIUpdate();

            stage.act();
            stage.draw();

            accumulator += Math.min(delta, 0.25f);
            if (accumulator >= STEP_TIME) {
                accumulator -= STEP_TIME;
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
                    lAndPSign.draw(stage.getBatch(), 1f);
                } else if (curLevel == 3) {
                    redCellSign.draw(stage.getBatch(), 1f);
                } else if (curLevel == 2) {
                    virusSign1.draw(stage.getBatch(), 1f);
                } else if (curLevel != 1) {
                    switch (curLevel % 4) {
                        case 1: {
                            virusSign1.draw(stage.getBatch(), 1f);
                            break;
                        }
                        case 2: {
                            playerSign.draw(stage.getBatch(), 1f);
                            lymphSign.draw(stage.getBatch(), 1f);
                            break;
                        }
                        case 3: {
                            redCellSign.draw(stage.getBatch(), 1f);
                            break;
                        }
                        case 0: {
                            lAndPSign.draw(stage.getBatch(), 1f);
                            break;
                        }
                    }
                }
                pointer.setVisible(true);
                pointer.draw(stage.getBatch(), pointer.getColor().a);
            } else {
                pauseObf.draw(stage.getBatch(), 0.5f);
                game.fonts.largeS.draw(
                        stage.getBatch(),
                        text[2],
                        0.5f * (game.width - game.fonts.largeS.getWidth(text[2])),
                        0.5f * (game.height + game.fonts.largeS.getHeight(text[2]))

                );
                pointer.setVisible(false);
                if (curLevel == 4) {
                    lAndPSign.setVisible(false);
                } else if (curLevel == 3) {
                    redCellSign.setVisible(false);
                } else if (curLevel == 2) {
                    virusSign1.setVisible(false);
                } else if (curLevel != 1) {
                    switch (curLevel % 4) {
                        case 1: {
                            virusSign1.setVisible(false);
                            break;
                        }
                        case 2: {
                            playerSign.setVisible(false);
                            lymphSign.setVisible(false);
                            break;
                        }
                        case 3: {
                            redCellSign.setVisible(false);
                            break;
                        }
                        case 0: {
                            lAndPSign.setVisible(false);
                            break;
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
            } else if (curLevel == 3) {
                redCellSign.setVisible(false);
            } else if (curLevel == 2) {
                virusSign1.setVisible(false);
            } else if (curLevel != 1) {
                switch (curLevel % 4) {
                    case 1: {
                        virusSign1.setVisible(false);
                        break;
                    }
                    case 2: {
                        playerSign.setVisible(false);
                        lymphSign.setVisible(false);
                        break;
                    }
                    case 3: {
                        redCellSign.setVisible(false);
                        break;
                    }
                    case 0: {
                        lAndPSign.setVisible(false);
                        break;
                    }
                }
            }
            pointer.setVisible(false);
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
                if (isVictory && Menu.maxLevel == curLevel){
                    Menu.maxLevel++;
                }
                game.setScreen(new Menu(game, true));
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
                playerVsViruses(contact);
                if (curLevel % 4 == 3 && !hasPlatelet) playerVsPlatelets(contact);
                if (curLevel % 4 == 3 && hasPlatelet) playerVsRedCells(contact);
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
                        virus.decreaseLT(50);
                        virus.addAction(Actions.color(Color.RED));
                        virus.addAction(Actions.color(Color.WHITE, 0.5f));
                        if (virus.getLifeTime() == 0) lymphScore++;
                        break;
                    }
                } else if (contact.getFixtureB().getBody().equals(lymph.getBody()) && contact.getFixtureA().getBody().equals(virus.getBody())) {
                    if (lymph.getLifeTime() > 0 && virus.getLifeTime() > 0) {
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
                    virus.decreaseLT(60);
                    virus.addAction(Actions.color(Color.RED));
                    virus.addAction(Actions.color(Color.WHITE, 0.5f));
                    if (virus.getLifeTime() == 0) {
                        playerScore++;
                        if (Menu.isSoundOn) game.sounds.death.play(1f, 1.25f - rand.nextFloat() / 2f, 0f);
                    }
                    break;
                }
            } else if (contact.getFixtureB().getBody().equals(player.getBody()) && contact.getFixtureA().getBody().equals(virus.getBody())) {
                if (virus.getLifeTime() > 0) {
                    virus.decreaseLT(60);
                    virus.addAction(Actions.color(Color.RED));
                    virus.addAction(Actions.color(Color.WHITE, 0.5f));
                    if (virus.getLifeTime() == 0) {
                        playerScore++;
                        if (Menu.isSoundOn) game.sounds.death.play(1f, 1.25f - rand.nextFloat() / 2f, 0f);
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
                    platelet.setName("platelet_s");
                    hasPlatelet = true;
                    break;
                }
            }else if (contact.getFixtureB().getBody().equals(player.getBody()) && contact.getFixtureA().getBody().equals(platelet.getBody())) {
                if (platelet.getLifeTime() > 0) {
                    platelet.setName("platelet_s");
                    hasPlatelet = true;
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
                    if (Menu.isSoundOn) game.sounds.click.play();
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
                    if (Menu.isSoundOn) game.sounds.click.play();
                    isPaused = !isPaused;
                    game.prefs.putInteger("max_level", Menu.maxLevel);
                    game.prefs.putBoolean("is_sound_on", Menu.isSoundOn);
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
        if (curLevel < 5) {
            switch (curLevel) {
                case 4: {
                    virusSign1 = new AdvSprite(game.atlas.createSprite("virus", rand.nextInt(18) + 1), 0f, 0f, 0.1f * game.width, 0.1f * game.width);
                    lAndPSign = new AdvSprite(game.atlas.createSprite("lymphocytes_and_player"), 0f, 0f, 0.025f * game.width, 0.025f * game.width);
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
                    virusSign1 = new AdvSprite(game.atlas.createSprite("virus", rand.nextInt(18) + 1), 0f, 0f, 0.1f * game.width, 0.1f * game.width);
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
        } else {
            switch (curLevel % 4) {
                case 1: {
                    virusSign1 = new AdvSprite(game.atlas.createSprite("virus", rand.nextInt(18) + 1), 0f, 0f, 0.1f * game.width, 0.1f * game.width);
                    stage.addActor(virusSign1);
                    break;
                }
                case 2: {
                    virusSign1 = new AdvSprite(game.atlas.createSprite("virus", rand.nextInt(18) + 1), 0f, 0f, 0.1f * game.width, 0.1f * game.width);
                    virusSign2 = new AdvSprite(game.atlas.createSprite("virus", rand.nextInt(18) + 1), 0f, 0f, 0.1f * game.width, 0.1f * game.width);
                    lymphSign = new AdvSprite(game.atlas.createSprite("lymphocytes"), 0f, 0f, 0.05f * game.width, 0.05f * game.width);
                    playerSign = new AdvSprite(game.atlas.createSprite("white"), 0f, 0f, 0.05f * game.width, 0.05f * game.width);

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
                    virusSign1 = new AdvSprite(game.atlas.createSprite("virus", rand.nextInt(18) + 1), 0f, 0f, 0.1f * game.width, 0.1f * game.width);
                    stage.addActor(virusSign1);
                    stage.addActor(lAndPSign);
                    break;
                }
            }
        }
        stage.addActor(pointer);
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
                    break;
                }
                case 1: {
                    pointerUpdateVirus();
                    pointer.updateSprite(spec.get());
                    break;
                }
            }
        } else {
            switch (curLevel % 4) {
                case 1: {
                    virusSign1.setPosition(
                            0.1f * meter,
                            game.height - 0.125f * game.width
                    );
                    virusSign1.addAction(Actions.rotateBy(1f));
                    virusSign1.updateSprite();
                    pointerUpdateVirus();
                    pointer.updateSprite(spec.get());
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
                    pointer.updateSprite(spec.get());
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
                    pointer.updateSprite(spec.get());
                    break;
                }
            }
        }
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
            pointer.addAction(Actions.rotateTo(0f));
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
        if (rand.nextInt(2) == 0) redCell.getBody().setAngularVelocity(rand.nextFloat() + 0.1f);
        else redCell.getBody().setAngularVelocity(-rand.nextFloat() - 0.1f);

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
    private void spawnPlatelet (int hp, Vector2 vec) {
        AdvSprite platEnt = new AdvSprite(game.atlas.createSprite("yellow"), 0, 0, 0.5f, 0.5f);
        Entity platSub = new Entity(platEnt, world, 0f, 3f, 0.2f, "platelet");
        Platelet platelet = new Platelet(platSub, hp);
        platelet.getBody().setTransform(vec.x, vec.y, 0f);
        if (rand.nextInt(2) == 0) platelet.getBody().setAngularVelocity(rand.nextFloat() + 0.1f);
        else platelet.getBody().setAngularVelocity(-rand.nextFloat() - 0.1f);

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
        if (type > 15) {
            virusEnt = new AdvSprite(game.atlas.createSprite("virus", type), 0, 0, 2f, 2f);
            virusSub = new Entity(virusEnt, world, 0f, 15f, 0.2f, "virus_super_advanced");
            hp += 300;
            speed += 45f;
        } else if (type > 10) {
            virusEnt = new AdvSprite(game.atlas.createSprite("virus", type), 0, 0, 1f, 1f);
            virusSub = new Entity(virusEnt, world, 0f, 5f, 0.2f, "virus_advanced");
            hp += 150;
            speed += 3f;
        } else {
            virusEnt = new AdvSprite(game.atlas.createSprite("virus", type), 0, 0, 0.5f, 0.5f);
            virusSub = new Entity(virusEnt, world, 0f, 3f, 0.2f, "virus");
        }
        Virus virus = new Virus(virusSub, hp, speed);
        virus.getBody().setTransform(vec.x, vec.y, 0f);
        if (rand.nextInt(2) == 0) virus.getBody().setAngularVelocity(rand.nextFloat() + 1f);
        else virus.getBody().setAngularVelocity(-rand.nextFloat() - 1f);

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
    private void spawnLymph (int hp, Vector2 vec, int number) {
        for (int i = 0; i < number; i++) {
            AdvSprite lymEntity = new AdvSprite(game.atlas.createSprite("lymphocyte", rand.nextInt(7) + 1), 0, 0, 1f, 1f);
            Entity lymSubject = new Entity(lymEntity, world, 0f, 8f, 0.2f, "lymphocyte");
            Lymphocyte lymphocyte = new Lymphocyte(lymSubject, hp);
            lymphocyte.getBody().setTransform(vec.x, vec.y, 0f);
            if (rand.nextInt(2) == 0)
                lymphocyte.getBody().setAngularVelocity(rand.nextFloat() + 0.3f);
            else lymphocyte.getBody().setAngularVelocity(-rand.nextFloat() - 0.3f);

            float sizeX = lymphocyte.getWidth(), sizeY = lymphocyte.getHeight();
            lymphocyte.setSize(0f, 0f);
            lymphocyte.addAction(Actions.alpha(0f));
            lymphocyte.addAction(Actions.parallel(
                    Actions.sizeTo(sizeX, sizeY, 1f),
                    Actions.alpha(1f, 1f)
            ));

            stage.addActor(lymphocyte);
            lymphocytes.addFirst(lymphocyte);
        }
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
        for (int i = 0; i < partSpec.size(); i++) {
            partSpec.get(i).updateSprite(spec.get());
            if (!partSpec.get(i).hasActions()) {
                changePart(partSpec.get(i));
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
            if (lymph.isAlive()) {
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

    private void initLevelTrain1 () {
        spawnVirus(2000, 1f - rand.nextFloat() / 2f, new Vector2(60f, 60f), 18);
    }
    private void initLevelTrain2 () {
        target = 5;
        timer = 60 * 360;
    }
    private void initLevelTrain3 () {
        for (int i = 0; i < 5; i++) {
            spawnPlatelet(100, getRandSpawnLoc());
            spawnRedCell(1000000, getRandSpawnLoc(), true);
        }
        timer = 60 * 360;
        target = 5;
    }
    private void initLevelTrain4 () {
        timer = 60 * 360;
        spawnLymph(1000000, getRandSpawnLoc(), 1);
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
        } else {
            switch (curLevel % 4) {
                case 1: {
                    int k = 20, h = 2;
                    target = 5;
                    timer = 60 * 45 - (curLevel % 21) * 30;
                    while (curLevel - k > 0) {
                        target += 2;
                        k += 20;
                        timer += (24 - h);
                        h += 2;
                    }
                    target += (curLevel % 21) / 4;
                    break;
                }
                case 2: {
                    int number = 1, k = 20;
                    target = 10;
                    while (curLevel - k > 0) {
                        number++;
                        k += 20;
                        target += 5;
                    }
                    target += (curLevel % 21) / 4;
                    number += (curLevel % 21) / 4;
                    spawnLymph(1000000, getRandSpawnLoc(), number);
                    break;
                }
                case 3: {
                    int k = 20, h = 2;
                    target = 5;
                    timer = 60 * 60 - (curLevel % 21) * 30;
                    while (curLevel - k > 0) {
                        target += 2;
                        timer += (30 - h);
                        k += 20;
                        h += 2;
                    }
                    target += (curLevel % 21) / 4;
                    for (int i = 0; i < 5; i++) {
                        spawnPlatelet(100, getRandSpawnLoc());
                        spawnRedCell(100, getRandSpawnLoc(), true);
                    }
                    break;
                }
                case 0: {
                    int number = 6, k = 20, h = 2;
                    target = 10;
                    timer = 60 * 45;
                    while (curLevel - k > 0) {
                        k += 20;
                        timer += (30 - h);
                        target += 5;
                    }
                    target += (curLevel % 21) / 4;
                    number -= (curLevel % 21) / 4;
                    spawnLymph(1000000, getRandSpawnLoc(), number);
                    break;
                }
            }
        }
    }

    private void idleLevelTrain1 () {
        if (rand.nextInt(50) == 0) spawnRedCell(100, getRandSpawnLoc(), false);
        if (rand.nextInt(75) == 0) spawnPlatelet(100, getRandSpawnLoc());
        if (viruses.size() == 0) {
            isVictory = true;
            isGameOver = true;
        }
    }
    private void idleLevelTrain2 () {
        if (rand.nextInt(100) == 0) spawnRedCell(100, getRandSpawnLoc(), false);
        if (rand.nextInt(100) == 0) spawnPlatelet(100, getRandSpawnLoc());
        if (rand.nextInt(150) == 0) spawnVirus(150, 1f - rand.nextFloat() / 2f, getRandSpawnLoc(), rand.nextInt(18) + 1);
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
        if (rand.nextInt(100) == 0) spawnPlatelet(100, getRandSpawnLoc());
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
        if (rand.nextInt(100) == 0) spawnPlatelet(100, getRandSpawnLoc());
        if (rand.nextInt(75) == 0) spawnVirus(150, 1f - rand.nextFloat() / 2f, getRandSpawnLoc(), rand.nextInt(18) + 1);
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
        switch (curLevel % 4) {
            case 1: {
                if (rand.nextInt(150) == 0) spawnRedCell(100, getRandSpawnLoc(), false);
                if (rand.nextInt(150) == 0) spawnPlatelet(100, getRandSpawnLoc());
                if (rand.nextInt(150) == 0) spawnVirus(150, 1f - rand.nextFloat() / 2f, getRandSpawnLoc(), rand.nextInt(18) + 1);
                timer--;
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
                if (rand.nextInt(150) == 0) spawnPlatelet(100, getRandSpawnLoc());
                if (rand.nextInt(150) == 0) spawnVirus(150, 1f - rand.nextFloat() / 2f, getRandSpawnLoc(), rand.nextInt(18) + 1);
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
                if (rand.nextInt(100) == 0) spawnPlatelet(100, getRandSpawnLoc());
                if (rand.nextInt(100) == 0) spawnRedCell(100, getRandSpawnLoc(), true);
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
                if (rand.nextInt(150) == 0) spawnPlatelet(100, getRandSpawnLoc());
                if (rand.nextInt(150) == 0) spawnVirus(150, 1f - rand.nextFloat() / 2f, getRandSpawnLoc(), rand.nextInt(18) + 1);
                timer--;
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

    private void drawText ()  {
        String fps = "FPS: " + Gdx.graphics.getFramesPerSecond();
        game.fonts.smallB.draw(
                stage.getBatch(),
                fps,
                7.9f*meter - game.fonts.smallB.getWidth(fps),
                spec.get().viewportHeight * meter - 0.1f*meter - 5f*game.fonts.smallB.getHeight("A")
        );
        if (curLevel == 4) {
            int sec = (int)(timer/60f);
            game.fonts.smallB.draw(
                    stage.getBatch(),
                    text[0] + ": " + sec + " " + text[1],
                    0.5f*(game.width - game.fonts.smallB.getWidth(text[0] + ": " + sec + " " + text[1])),
                    1.25f * game.fonts.smallB.getHeight("A")
            );
            game.fonts.smallB.draw(
                    stage.getBatch(),
                    ": " + (playerScore + lymphScore) + "/" + target,
                    0.1f * meter + 0.125f * game.width,
                    game.height - 0.075f * game.width + 0.5f * game.fonts.smallB.getHeight("A")
            );
        } else if (curLevel == 2) {
            int sec = (int)(timer/60f);
            game.fonts.smallB.draw(
                    stage.getBatch(),
                    text[0] + ": " + sec + " " + text[1],
                    0.5f*(game.width - game.fonts.smallB.getWidth(text[0] + ": " + sec + " " + text[1])),
                    1.25f * game.fonts.smallB.getHeight("A")
            );
            game.fonts.smallB.draw(
                    stage.getBatch(),
                    ": " + (playerScore) + "/" + target,
                    0.1f*meter + 0.125f*game.width,
                    game.height - 0.075f*game.width + 0.5f*game.fonts.smallB.getHeight("A")
            );
        } else if (curLevel != 1) {
            switch (curLevel % 4) {
                case 1: {
                    int sec = (int)(timer/60f);
                    game.fonts.smallB.draw(
                            stage.getBatch(),
                            ": " + (playerScore) + "/" + target,
                            0.1f*meter + 0.125f*game.width,
                            game.height - 0.075f*game.width + 0.5f*game.fonts.smallB.getHeight("A")
                    );
                    game.fonts.smallB.draw(
                            stage.getBatch(),
                            text[0] + ": " + sec + " " + text[1],
                            0.5f*(game.width - game.fonts.smallB.getWidth(text[0] + ": " + sec + " " + text[1])),
                            1.25f * game.fonts.smallB.getHeight("A")
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
                    int sec = (int)(timer/60f);
                    game.fonts.smallB.draw(
                            stage.getBatch(),
                            text[0] + ": " + sec + " " + text[1],
                            0.5f*(game.width - game.fonts.smallB.getWidth(text[0] + ": " + sec + " " + text[1])),
                            1.25f * game.fonts.smallB.getHeight("A")
                    );
                    game.fonts.smallB.draw(
                            stage.getBatch(),
                            ": " + playerScore + "/" + target,
                            0.1f*meter + 0.125f*game.width,
                            game.height - 0.075f*game.width + 0.5f*game.fonts.smallB.getHeight("A")
                    );
                    break;
                }
                case 0: {
                    int sec = (int)(timer/60f);
                    game.fonts.smallB.draw(
                            stage.getBatch(),
                            text[0] + ": " + sec + " " + text[1],
                            0.5f*(game.width - game.fonts.smallB.getWidth(text[0] + ": " + sec + " " + text[1])),
                            1.25f * game.fonts.smallB.getHeight("A")
                    );
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
        game.prefs.putInteger("max_level", Menu.maxLevel);
        game.prefs.putBoolean("is_sound_on", Menu.isSoundOn);
        game.prefs.flush();
        if (game.sounds.mainTheme.isPlaying()) {
            game.sounds.mainTheme.pause();
            game.sounds.mainTheme.stop();
        }
    }

    @Override
    public void resume () {
        if (!game.sounds.mainTheme.isPlaying()) if (Menu.isSoundOn) game.sounds.mainTheme.play();
    }

    @Override
    public void hide () {
        dispose();
    }

    @Override
    public void dispose () {
        game.prefs.putInteger("max_level", Menu.maxLevel);
        game.prefs.putBoolean("is_sound_on", Menu.isSoundOn);
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
                player.applyForceToTheCentre(
                        0.5f * velocityX,
                        -(0.5f * velocityY),
                        true
                );
            } else {
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
