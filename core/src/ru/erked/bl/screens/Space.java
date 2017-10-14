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

    private LinkedList<AdvSprite> advSprites;
    private LinkedList<Entity> redCells;
    private LinkedList<Entity> thrombocytes;
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

    private AdvSprite map;
    private AdvSprite point;
    private AdvSprite lymphSign;
    private AdvSprite lAndPSign;
    private AdvSprite playerSign;
    private AdvSprite virusSign;
    private AdvSprite redCellSign;
    private LinkedList<AdvSprite> lymphPoints;
    private LinkedList<AdvSprite> virusPoints;
    private LinkedList<AdvSprite> redCellPoints;
    private LinkedList<AdvSprite> thrombPoints;

    private long internalTime = 0;
    private long timer = 0;
    private int score = 0;
    private boolean hasThromb = false;

    private BLButton exit;
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
        thrombocytes = new LinkedList<Entity>();
        bounds = new LinkedList<Bound>();
        advSprites = new LinkedList<AdvSprite>();
    }

    @Override
    public void show () {
        Box2D.init();
        contactListenerInit();

        AdvSprite playerEnt = new AdvSprite(game.atlas.createSprite("white"), 0f, 0f, 1f, 1f);
        player = new Entity(playerEnt, world, 0f, 5f, 0.2f, "player");
        player.getBody().setAngularVelocity(0.25f);

        initLevels();
        addBounds();
        for (int i = 0; i < rand.nextInt(10) + 20; i++) { addPart(); }

        stage.addActor(spec);
        stage.addActor(player);

        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(stage);
        multiplexer.addProcessor(new GestureDetector(this));
        Gdx.input.setInputProcessor(multiplexer);
        world.setContactListener(contactListener);

        UIInit();

        buttonInit();
        obf = new Obfuscation(game.atlas.createSprite("obfuscation"), true);

        text = new String[2];
        text[0] = game.textSystem.get("lose");
        text[1] = game.textSystem.get("sex");
    }

    @Override
    public void render (float delta) {
        internalTime++;
        Gdx.gl.glClearColor(220f/255f, 150f/255f, 180f/255f, 0f);
        //Gdx.gl.glClearColor(255f/255f, 255f/255f, 255f/255f, 0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float s = 0.01f * spec.get().viewportHeight;
        gravity(player.getBody(), s);
        for (Entity r : redCells) gravity(r.getBody(), s);
        for (Entity l : lymphocytes) gravity(l.getBody(), s);
        for (Entity v : viruses) gravity(v.getBody(), s);
        for (Entity t : thrombocytes) gravity(t.getBody(), s);

        if (!isGameOver) lifeCycle();

        buttonUpdate();
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

        stage.getBatch().begin();

        if (!isGameOver) {
            drawText();
            if (curLevel == 4) {
                lAndPSign.draw(stage.getBatch(), 1f);
            } else if (curLevel == 3) {
                redCellSign.draw(stage.getBatch(), 1f);
            } else if (curLevel == 2) {
                virusSign.draw(stage.getBatch(), 1f);
            } else if (curLevel != 1) {
                switch (curLevel % 4) {
                    case 1: {
                        virusSign.draw(stage.getBatch(), 1f);
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
            exit.get().setVisible(true);
            exit.get().draw(stage.getBatch(), 1f);
            map.draw(stage.getBatch(), 1f);
            point.draw(stage.getBatch(), 1f);
        } else {
            if (curLevel == 4) {
                lAndPSign.setVisible(false);
            } else if (curLevel == 3) {
                redCellSign.setVisible(false);
            } else if (curLevel == 2) {
                virusSign.setVisible(false);
            } else if (curLevel != 1) {
                switch (curLevel % 4) {
                    case 1: {
                        virusSign.setVisible(false);
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
            map.setVisible(false);
            point.setVisible(false);
            for (AdvSprite adv : virusPoints)   adv.setVisible(false);
            for (AdvSprite adv : lymphPoints)   adv.setVisible(false);
            for (AdvSprite adv : redCellPoints) adv.setVisible(false);
            for (AdvSprite adv : thrombPoints)  adv.setVisible(false);
            exit.get().setVisible(false);
        }

        obf.draw(stage.getBatch(), obf.getAlpha());

        if(obf.isActive() && !toMenu && !isGameOver){
            obf.deactivate(1f, delta);
        } else if (toMenu) {
            if (obf.isActive()) {
                game.setScreen(new Menu(game, true));
            } else {
                obf.activate(1f, delta);
            }
        } else if (isGameOver) {
            if (obf.isActive()) {
                if (isVictory && Menu.maxLevel == curLevel){
                    Menu.maxLevel++;
                }
                game.setScreen(new Menu(game, true));
            } else {
                obf.activate(4f, delta);
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
            game.prefs.flush();
            dispose();
            Gdx.app.exit();
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.HOME)){
            game.prefs.putInteger("max_level", Menu.maxLevel);
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
                if (curLevel % 4 == 2 || curLevel % 4 == 0) lymphVersusViruses(contact);
                playerVersusViruses(contact);
                if (curLevel % 4 == 3 && !hasThromb) playerVersusThrombs(contact);
                if (curLevel % 4 == 3 && hasThromb) playerVersusRedCells(contact);
            }
        };
    }
    private void lymphVersusViruses (Contact contact) {
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
                        //System.out.println("Moy drug ubil virus!");
                        break;
                    }
                } else if (contact.getFixtureB().getBody().equals(lymph.getBody()) && contact.getFixtureA().getBody().equals(virus.getBody())) {
                    if (lymph.getLifeTime() > 0 && virus.getLifeTime() > 0) {
                        virus.decreaseLT(50);
                        virus.addAction(Actions.color(Color.RED));
                        virus.addAction(Actions.color(Color.WHITE, 0.5f));
                        if (virus.getLifeTime() == 0) lymphScore++;
                        //System.out.println("Moy drug ubil virus!");
                        break;
                    }
                }
            }
            if (iVirus.hasNext()) break;
        }
    }
    private void playerVersusViruses (Contact contact) {
        Iterator<Entity> iVirus = viruses.iterator();
        while (iVirus.hasNext()) {
            Entity virus = iVirus.next();
            if (contact.getFixtureA().getBody().equals(player.getBody()) && contact.getFixtureB().getBody().equals(virus.getBody())) {
                if (virus.getLifeTime() > 0) {
                    switch (curLevel % 4) {
                        case 1: {
                            virus.decreaseLT(1000);
                            break;
                        }
                        case 0: case 2: {
                            virus.decreaseLT(60);
                            break;
                        }
                    }
                    virus.addAction(Actions.color(Color.RED));
                    virus.addAction(Actions.color(Color.WHITE, 0.5f));
                    if (virus.getLifeTime() == 0) {
                        playerScore++;
                        game.sounds.death.play(1f, 1.25f - rand.nextFloat() / 2f, 0f);
                    }
                    //System.out.println("Ya ubil virus!");
                    break;
                }
            } else if (contact.getFixtureB().getBody().equals(player.getBody()) && contact.getFixtureA().getBody().equals(virus.getBody())) {
                if (virus.getLifeTime() > 0) {
                    switch (curLevel % 4) {
                        case 1: {
                            virus.decreaseLT(1000);
                            break;
                        }
                        case 0: case 2: {
                            virus.decreaseLT(60);
                            break;
                        }
                    }
                    virus.addAction(Actions.color(Color.RED));
                    virus.addAction(Actions.color(Color.WHITE, 0.5f));
                    if (virus.getLifeTime() == 0) {
                        playerScore++;
                        game.sounds.death.play(1f, 1.25f - rand.nextFloat() / 2f, 0f);
                    }
                    //System.out.println("Ya ubil virus!");
                    break;
                }
            }
        }
    }
    private void playerVersusThrombs (Contact contact) {
        Iterator<Entity> iThromb = thrombocytes.iterator();
        while (iThromb.hasNext()) {
            Entity thromb = iThromb.next();
            if (contact.getFixtureA().getBody().equals(player.getBody()) && contact.getFixtureB().getBody().equals(thromb.getBody())) {
                if (thromb.getLifeTime() > 0) {
                    thromb.setName("thrombocyte_s");
                    hasThromb = true;
                    break;
                }
            }else if (contact.getFixtureB().getBody().equals(player.getBody()) && contact.getFixtureA().getBody().equals(thromb.getBody())) {
                if (thromb.getLifeTime() > 0) {
                    thromb.setName("thrombocyte_s");
                    hasThromb = true;
                    break;
                }
            }
        }
    }
    private void playerVersusRedCells (Contact contact) {
        Iterator<Entity> iRedCell = redCells.iterator();
        while (iRedCell.hasNext()) {
            Entity redCell = iRedCell.next();
            if (contact.getFixtureA().getBody().equals(player.getBody()) && contact.getFixtureB().getBody().equals(redCell.getBody())) {
                if (redCell.getLifeTime() > 0 && redCell.getName().equals("red_cell_sick")) {
                    redCell.addAction(Actions.color(Color.WHITE, 1f));
                    redCell.setName("red_cell");
                    for (int i = 0; i < thrombocytes.size(); i++){
                        if (thrombocytes.get(i).getName().equals("thrombocyte_s"))
                            thrombocytes.get(i).decreaseLT(1000);
                    }
                    score--;
                    hasThromb = false;
                    break;
                }
            }else if (contact.getFixtureB().getBody().equals(player.getBody()) && contact.getFixtureA().getBody().equals(redCell.getBody())) {
                if (redCell.getLifeTime() > 0 && redCell.getName().equals("red_cell_sick")) {
                    redCell.addAction(Actions.color(Color.WHITE, 1f));
                    redCell.setName("red_cell");
                    for (int i = 0; i < thrombocytes.size(); i++) {
                        if (thrombocytes.get(i).getName().equals("thrombocyte_s"))
                            thrombocytes.get(i).decreaseLT(1000);
                    }
                    score--;
                    hasThromb = false;
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
                    game.sounds.click.play();
                    toMenu = true;
                } else {
                    exit.get().setChecked(false);
                }
            }
        });
        stage.addActor(exit.get());
    }
    private void buttonUpdate () {
        exit.get().setPosition(
                7.9f*meter - exit.get().getWidth(),
                spec.get().viewportHeight * meter - exit.get().getHeight() - 0.1f*meter
        );
    }
    private void UIInit() {
        map = new AdvSprite(game.atlas.createSprite("map"), 0f, 0f, 0.3f * game.width, 0.3f * game.width);
        point = new AdvSprite(game.atlas.createSprite("map_point"), 0f, 0f, 0.02f * game.width, 0.02f * game.width);
        lymphPoints = new LinkedList<AdvSprite>();
        virusPoints = new LinkedList<AdvSprite>();
        redCellPoints = new LinkedList<AdvSprite>();
        thrombPoints = new LinkedList<AdvSprite>();

        if (curLevel == 4) {
            lAndPSign = new AdvSprite(game.atlas.createSprite("lymphocytes_and_player"), 0f, 0f, 0.1f * game.width, 0.1f * game.width);
            stage.addActor(lAndPSign);
        } else if (curLevel == 3) {
            redCellSign = new AdvSprite(game.atlas.createSprite("red"), 0f, 0f, 0.1f * game.width, 0.1f * game.width);
            redCellSign.setColor(Color.GRAY);
            stage.addActor(redCellSign);
        } else if (curLevel == 2) {
            virusSign = new AdvSprite(game.atlas.createSprite("virus", 11), 0f, 0f, 0.1f * game.width, 0.1f * game.width);
            stage.addActor(virusSign);
        } else if (curLevel != 1) {
            switch (curLevel % 4) {
                case 1: {
                    virusSign = new AdvSprite(game.atlas.createSprite("virus", 11), 0f, 0f, 0.1f * game.width, 0.1f * game.width);
                    stage.addActor(virusSign);
                    break;
                }
                case 2: {
                    lymphSign = new AdvSprite(game.atlas.createSprite("lymphocytes"), 0f, 0f, 0.1f * game.width, 0.1f * game.width);
                    playerSign = new AdvSprite(game.atlas.createSprite("white"), 0f, 0f, 0.1f * game.width, 0.1f * game.width);

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
                    lAndPSign = new AdvSprite(game.atlas.createSprite("lymphocytes_and_player"), 0f, 0f, 0.1f * game.width, 0.1f * game.width);
                    stage.addActor(lAndPSign);
                    break;
                }
            }
        }

        stage.addActor(map);
        stage.addActor(point);
    }
    private void UIUpdate() {
        map.setPosition(
                0.1f*meter,
                spec.get().viewportHeight * meter - map.getHeight() - 0.1f*meter
        );
        map.updateSprite();
        point.setPosition(
                (0.1f*meter + (map.getWidth() / 16f)) - ((80f - player.getPosition().x) / 90f - 1f) * (map.getWidth() * 13f / 16f) + 0.25f * point.getWidth(),
                (spec.get().viewportHeight * meter - point.getHeight() - 0.1f*meter) - ((80f - player.getPosition().y) / 90f)  * (map.getWidth() * 13f / 16f) - 0.5f * point.getHeight()
        );
        point.updateSprite();

        if (curLevel == 4) {

            lAndPSign.setPosition(
                    0.1f * meter,
                    map.getY() - 0.125f * game.width
            );
            lAndPSign.addAction(Actions.rotateBy(1f));
            lAndPSign.updateSprite();

        }else if (curLevel == 3) {

            redCellSign.setPosition(
                    0.1f * meter,
                    map.getY() - 0.125f * game.width
            );
            redCellSign.addAction(Actions.rotateBy(1f));
            redCellSign.updateSprite();

            if (redCells.size() > redCellPoints.size()) {
                AdvSprite redCellP = new AdvSprite(game.atlas.createSprite("map_point"), 0f, 0f, 0.015f * game.width, 0.015f * game.width);
                redCellP.setColor(Color.SALMON);
                redCellPoints.add(redCellP);
                stage.addActor(redCellPoints.get(redCellPoints.size() - 1));
            } else if (redCells.size() < redCellPoints.size()) {
                redCellPoints.getFirst().remove();
                redCellPoints.removeFirst();
            }
            int rIterator = 0;
            for (Entity l : redCells) {
                if (rIterator < redCellPoints.size()) {
                    redCellPoints.get(rIterator).setPosition(
                            (0.1f * meter + (map.getWidth() / 16f)) - ((80f - l.getPosition().x) / 90f - 1f) * (map.getWidth() * 13f / 16f) + 0.25f * redCellPoints.get(rIterator).getWidth(),
                            (spec.get().viewportHeight * meter - point.getHeight() - 0.1f * meter) - ((80f - l.getPosition().y) / 90f) * (map.getWidth() * 13f / 16f) - 0.5f * redCellPoints.get(rIterator).getHeight()
                    );
                    redCellPoints.get(rIterator).updateSprite();
                    rIterator++;
                }
            }
            //
            if (thrombocytes.size() > thrombPoints.size()) {
                AdvSprite redCellP = new AdvSprite(game.atlas.createSprite("map_point"), 0f, 0f, 0.015f * game.width, 0.015f * game.width);
                redCellP.setColor(Color.YELLOW);
                thrombPoints.add(redCellP);
                stage.addActor(thrombPoints.get(thrombPoints.size() - 1));
            } else if (thrombocytes.size() < thrombPoints.size()) {
                thrombPoints.getFirst().remove();
                thrombPoints.removeFirst();
            }
            int tIterator = 0;
            for (Entity l : thrombocytes) {
                if (tIterator < thrombPoints.size()) {
                    thrombPoints.get(tIterator).setPosition(
                            (0.1f * meter + (map.getWidth() / 16f)) - ((80f - l.getPosition().x) / 90f - 1f) * (map.getWidth() * 13f / 16f) + 0.25f * thrombPoints.get(tIterator).getWidth(),
                            (spec.get().viewportHeight * meter - point.getHeight() - 0.1f * meter) - ((80f - l.getPosition().y) / 90f) * (map.getWidth() * 13f / 16f) - 0.5f * thrombPoints.get(tIterator).getHeight()
                    );
                    thrombPoints.get(tIterator).updateSprite();
                    tIterator++;
                }
            }

        } else if (curLevel == 2) {
            virusSign.setPosition(
                    0.1f * meter,
                    map.getY() - 0.125f * game.width
            );
            virusSign.addAction(Actions.rotateBy(1f));
            virusSign.updateSprite();

        } else if (curLevel != 1) {
            switch (curLevel % 4) {
                case 1: {
                    virusSign.setPosition(
                            0.1f * meter,
                            map.getY() - 0.125f * game.width
                    );
                    virusSign.addAction(Actions.rotateBy(1f));
                    virusSign.updateSprite();
                    break;
                }
                case 2: {
                    playerSign.setPosition(
                            0.1f * meter,
                            map.getY() - 0.125f * game.width
                    );
                    playerSign.addAction(Actions.rotateBy(1f));
                    playerSign.updateSprite();
                    lymphSign.setPosition(
                            0.1f * meter,
                            map.getY() - 0.25f * game.width
                    );
                    lymphSign.addAction(Actions.rotateBy(1f));
                    lymphSign.updateSprite();

                    if (lymphocytes.size() > lymphPoints.size()) {
                        AdvSprite lymphP = new AdvSprite(game.atlas.createSprite("map_point"), 0f, 0f, 0.015f * game.width, 0.015f * game.width);
                        lymphP.setColor(Color.CYAN);
                        lymphPoints.add(lymphP);
                        stage.addActor(lymphPoints.get(lymphPoints.size() - 1));
                    } else if (lymphocytes.size() < lymphPoints.size()) {
                        lymphPoints.getFirst().remove();
                        lymphPoints.removeFirst();
                    }
                    int lIterator = 0;
                    for (Entity l : lymphocytes) {
                        if (lIterator < lymphPoints.size()) {
                            lymphPoints.get(lIterator).setPosition(
                                    (0.1f * meter + (map.getWidth() / 16f)) - ((80f - l.getPosition().x) / 90f - 1f) * (map.getWidth() * 13f / 16f) + 0.25f * lymphPoints.get(lIterator).getWidth(),
                                    (spec.get().viewportHeight * meter - point.getHeight() - 0.1f * meter) - ((80f - l.getPosition().y) / 90f) * (map.getWidth() * 13f / 16f) - 0.5f * lymphPoints.get(lIterator).getHeight()
                            );
                            lymphPoints.get(lIterator).updateSprite();
                            lIterator++;
                        }
                    }

                    break;
                }
                case 3: {
                    redCellSign.setPosition(
                            0.1f * meter,
                            map.getY() - 0.125f * game.width
                    );
                    redCellSign.addAction(Actions.rotateBy(1f));
                    redCellSign.updateSprite();

                    if (redCells.size() > redCellPoints.size()) {
                        AdvSprite redCellP = new AdvSprite(game.atlas.createSprite("map_point"), 0f, 0f, 0.015f * game.width, 0.015f * game.width);
                        redCellP.setColor(Color.SALMON);
                        redCellPoints.add(redCellP);
                        stage.addActor(redCellPoints.get(redCellPoints.size() - 1));
                    } else if (redCells.size() < redCellPoints.size()) {
                        redCellPoints.getFirst().remove();
                        redCellPoints.removeFirst();
                    }
                    int rIterator = 0;
                    for (Entity l : redCells) {
                        if (rIterator < redCellPoints.size()) {
                            redCellPoints.get(rIterator).setPosition(
                                    (0.1f * meter + (map.getWidth() / 16f)) - ((80f - l.getPosition().x) / 90f - 1f) * (map.getWidth() * 13f / 16f) + 0.25f * redCellPoints.get(rIterator).getWidth(),
                                    (spec.get().viewportHeight * meter - point.getHeight() - 0.1f * meter) - ((80f - l.getPosition().y) / 90f) * (map.getWidth() * 13f / 16f) - 0.5f * redCellPoints.get(rIterator).getHeight()
                            );
                            redCellPoints.get(rIterator).updateSprite();
                            rIterator++;
                        }
                    }
                    //
                    if (thrombocytes.size() > thrombPoints.size()) {
                        AdvSprite redCellP = new AdvSprite(game.atlas.createSprite("map_point"), 0f, 0f, 0.015f * game.width, 0.015f * game.width);
                        redCellP.setColor(Color.YELLOW);
                        thrombPoints.add(redCellP);
                        stage.addActor(thrombPoints.get(thrombPoints.size() - 1));
                    } else if (thrombocytes.size() < thrombPoints.size()) {
                        thrombPoints.getFirst().remove();
                        thrombPoints.removeFirst();
                    }
                    int tIterator = 0;
                    for (Entity l : thrombocytes) {
                        if (tIterator < thrombPoints.size()) {
                            thrombPoints.get(tIterator).setPosition(
                                    (0.1f * meter + (map.getWidth() / 16f)) - ((80f - l.getPosition().x) / 90f - 1f) * (map.getWidth() * 13f / 16f) + 0.25f * thrombPoints.get(tIterator).getWidth(),
                                    (spec.get().viewportHeight * meter - point.getHeight() - 0.1f * meter) - ((80f - l.getPosition().y) / 90f) * (map.getWidth() * 13f / 16f) - 0.5f * thrombPoints.get(tIterator).getHeight()
                            );
                            thrombPoints.get(tIterator).updateSprite();
                            tIterator++;
                        }
                    }

                    break;
                }
                case 0: {
                    lAndPSign.setPosition(
                            0.1f * meter,
                            map.getY() - 0.125f * game.width
                    );
                    lAndPSign.addAction(Actions.rotateBy(1f));
                    lAndPSign.updateSprite();

                    if (lymphocytes.size() > lymphPoints.size()) {
                        AdvSprite lymphP = new AdvSprite(game.atlas.createSprite("map_point"), 0f, 0f, 0.015f * game.width, 0.015f * game.width);
                        lymphP.setColor(Color.CYAN);
                        lymphPoints.add(lymphP);
                        stage.addActor(lymphPoints.get(lymphPoints.size() - 1));
                    } else if (lymphocytes.size() < lymphPoints.size()) {
                        lymphPoints.getFirst().remove();
                        lymphPoints.removeFirst();
                    }
                    int lIterator = 0;
                    for (Entity l : lymphocytes) {
                        if (lIterator < lymphPoints.size()) {
                            lymphPoints.get(lIterator).setPosition(
                                    (0.1f * meter + (map.getWidth() / 16f)) - ((80f - l.getPosition().x) / 90f - 1f) * (map.getWidth() * 13f / 16f) + 0.25f * lymphPoints.get(lIterator).getWidth(),
                                    (spec.get().viewportHeight * meter - point.getHeight() - 0.1f * meter) - ((80f - l.getPosition().y) / 90f) * (map.getWidth() * 13f / 16f) - 0.5f * lymphPoints.get(lIterator).getHeight()
                            );
                            lymphPoints.get(lIterator).updateSprite();
                            lIterator++;
                        }
                    }

                    break;
                }
            }
        }

        if ((viruses.size()) > virusPoints.size()) {
            AdvSprite virusP = new AdvSprite(game.atlas.createSprite("map_point"), 0f, 0f, 0.015f * game.width, 0.015f * game.width);
            virusP.setColor(Color.RED);
            virusPoints.add(virusP);
            stage.addActor(virusPoints.get(virusPoints.size() - 1));
        } else if ((viruses.size()) < virusPoints.size()) {
            virusPoints.getFirst().remove();
            virusPoints.removeFirst();
        }
        int vIterator = 0;
        for (Entity v : viruses) {
            if (vIterator < virusPoints.size()) {
                virusPoints.get(vIterator).setPosition(
                        (0.1f * meter + (map.getWidth() / 16f)) - ((80f - v.getPosition().x) / 90f - 1f) * (map.getWidth() * 13f / 16f) + 0.25f * virusPoints.get(vIterator).getWidth(),
                        (spec.get().viewportHeight * meter - point.getHeight() - 0.1f * meter) - ((80f - v.getPosition().y) / 90f) * (map.getWidth() * 13f / 16f) - 0.5f * virusPoints.get(vIterator).getHeight()
                );
                virusPoints.get(vIterator).updateSprite();
                vIterator++;
            }
        }
    }

    private void addPart () {
        Color color = new Color((rand.nextInt(25) + 230)/255f, (rand.nextInt(15) + 1)/255f, (rand.nextInt(50) + 25)/255f, 1f);
        float x = meter*spec.get().position.x - meter*spec.get().viewportWidth + rand.nextInt((int)(2f*meter*spec.get().viewportWidth));
        float y = meter*spec.get().position.y - meter*spec.get().viewportHeight + rand.nextInt((int)(2f*meter*spec.get().viewportHeight));
        float length = rand.nextInt((int)(0.01f*meter*spec.get().viewportWidth)) + 0.01f*meter*spec.get().viewportWidth;
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
    private void spawnThromb (int hp, Vector2 vec) {
        AdvSprite thrombEnt = new AdvSprite(game.atlas.createSprite("yellow"), 0, 0, 0.5f, 0.5f);
        Entity thrombSub = new Entity(thrombEnt, world, 0f, 10f, 0.2f, "thrombocyte");
        RedCell thromb = new RedCell(thrombSub, hp);
        thromb.getBody().setTransform(vec.x, vec.y, 0f);
        if (rand.nextInt(2) == 0) thromb.getBody().setAngularVelocity(rand.nextFloat() + 0.1f);
        else thromb.getBody().setAngularVelocity(-rand.nextFloat() - 0.1f);

        float sizeX = thromb.getWidth(), sizeY = thromb.getHeight();
        thromb.setSize(0f, 0f);
        thromb.addAction(Actions.alpha(0f));
        thromb.addAction(Actions.parallel(
                Actions.sizeTo(sizeX, sizeY, 1f),
                Actions.alpha(1f, 1f)
        ));

        stage.addActor(thromb);
        thrombocytes.addFirst(thromb);
    }
    private void spawnVirus (int hp, float speed, Vector2 vec, int type) {
        AdvSprite virusEnt;
        Entity virusSub;
        if (type > 14) {
            virusEnt = new AdvSprite(game.atlas.createSprite("virus", type), 0, 0, 2f, 2f);
            virusSub = new Entity(virusEnt, world, 0f, 15f, 0.2f, "virus_super_advanced");
            hp += 300;
        } else if (type > 9) {
            virusEnt = new AdvSprite(game.atlas.createSprite("virus", type), 0, 0, 1f, 1f);
            virusSub = new Entity(virusEnt, world, 0f, 5f, 0.2f, "virus_advanced");
            hp += 150;
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
            AdvSprite lymEntity = new AdvSprite(game.atlas.createSprite("lymphocyte", rand.nextInt(8) + 1), 0, 0, 1f, 1f);
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
        Iterator<Entity> v1 = viruses.iterator();
        while (v1.hasNext()) {
            Entity v = v1.next();
            v.updateSprite(spec.get());
            v.updateLife();
            v.decreaseLT();
            if (v.getLifeTime() <= 0) {
                if (v.isAlive()) {
                    v.kill();
                }
                if (!v.hasActions()){
                    v.getWorld().destroyBody(v.getBody());
                    v1.remove();
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
        Iterator<Entity> thrombI = thrombocytes.iterator();
        while (thrombI.hasNext()) {
            Entity t = thrombI.next();
            t.updateSprite(spec.get());
            if (t.getName().equals("thrombocyte_s"))
                t.updateLife(player.getPosition().x, player.getPosition().y);
            else
                t.updateLife();
            t.decreaseLT();
            if (t.getLifeTime() <= 0) {
                if (t.isAlive()) {
                    t.kill();
                }
                if (!t.hasActions()){
                    t.getWorld().destroyBody(t.getBody());
                    thrombI.remove();
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
        for (int i = 0; i < advSprites.size(); i++) {
            advSprites.get(i).updateSprite(spec.get());
            if (!advSprites.get(i).hasActions()) {
                changePart(advSprites.get(i));
            }
        }

        for (int i = 0; i < bounds.size(); i++) {
            bounds.get(i).updateSprite(spec.get());
        }
    }
    private void lymphSearch() {
        Iterator<Entity> lympI = lymphocytes.iterator();
        while (lympI.hasNext()) {
            Entity lymp = lympI.next();
            lymp.updateSprite(spec.get());
            if (lymp.isAlive()) {
                float x = lymp.getPosition().x;
                float y = lymp.getPosition().y;
                double minDist = 1000000;
                Iterator<Entity> virusI = viruses.iterator();
                while (virusI.hasNext()) {
                    Entity virus = virusI.next();
                    float tempX = (virus.getPosition().x - lymp.getPosition().x);
                    float tempY = (virus.getPosition().y - lymp.getPosition().y);
                    double distance = Math.sqrt((double)(tempX * tempX + tempY * tempY));
                    if (distance < minDist) {
                        minDist = distance;
                        x = virus.getPosition().x;
                        y = virus.getPosition().y;
                    }
                }
                lymp.updateLife(x, y);
            }
        }
    }

    private void initLevels () {
        if (curLevel == 4) {
            timer = 60 * 360;
            spawnLymph(1000000, getRandSpawnLoc(), 1);
            score = 5;
        } else if (curLevel == 3) {
            for (int i = 0; i < 5; i++) spawnRedCell(1000000, getRandSpawnLoc(), true);
            timer = 60 * 360;
            score = 5;
        } else if (curLevel == 2) {
            for (int i = 0; i < 5; i++) spawnVirus(150, 1.25f - rand.nextFloat() / 2f, getRandSpawnLoc(), rand.nextInt(17) + 1);
            timer = 60 * 360;
        } else if (curLevel != 1) {
            switch (curLevel % 4) {
                case 1: {
                    int number = 5, k = 20, h = 2;
                    timer = 60 * 45 - (curLevel % 21) * 30;
                    while (curLevel - k > 0) {
                        number += 2;
                        k += 20;
                        timer += (24 - h);
                        h += 2;
                    }
                    number += (curLevel % 21) / 4;
                    for (int i = 0; i < number; i++) spawnVirus(1500, 1.25f - rand.nextFloat() / 2f, getRandSpawnLoc(), rand.nextInt(17) + 1);
                    break;
                }
                case 2: {
                    int number = 1, k = 20;
                    score = 10;
                    while (curLevel - k > 0) {
                        number++;
                        k += 20;
                        score += 5;
                    }
                    score += (curLevel % 21) / 4;
                    number += (curLevel % 21) / 4;
                    spawnLymph(1000000, getRandSpawnLoc(), number);
                    break;
                }
                case 3: {
                    int k = 20, h = 2;
                    score = 5;
                    timer = 60 * 60 - (curLevel % 21) * 30;
                    while (curLevel - k > 0) {
                        score += 2;
                        timer += (30 - h);
                        k += 20;
                        h += 2;
                    }
                    score += (curLevel % 21) / 4;
                    for (int i = 0; i < score; i++) spawnRedCell(1000000, getRandSpawnLoc(), true);
                    break;
                }
                case 0: {
                    int number = 6, k = 20, h = 2;
                    score = 10;
                    timer = 60 * 45;
                    while (curLevel - k > 0) {
                        k += 20;
                        timer += (30 - h);
                        score += 5;
                    }
                    score += (curLevel % 21) / 4;
                    number -= (curLevel % 21) / 4;
                    spawnLymph(1000000, getRandSpawnLoc(), number);
                    break;
                }
            }
        }
    }

    private void idleLevelTrain1 () {
        if (rand.nextInt(50) == 0) spawnRedCell(100, getRandSpawnLoc(), false);
        if (rand.nextInt(75) == 0) spawnThromb(100, getRandSpawnLoc());
        if (internalTime / 60f > 5f) {
            isVictory = true;
            isGameOver = true;
        }
    }
    private void idleLevelTrain2 () {
        if (rand.nextInt(100) == 0) spawnRedCell(100, getRandSpawnLoc(), false);
        if (rand.nextInt(100) == 0) spawnThromb(100, getRandSpawnLoc());
        timer--;
        if (viruses.size() == 0) {
            isVictory = true;
            isGameOver = true;
        }
        if (timer < 0) {
            isGameOver = true;
        }
    }
    private void idleLevelTrain3 () {
        if (rand.nextInt(100) == 0) spawnThromb(100, getRandSpawnLoc());
        timer--;
        if (score <= 0) {
            isVictory = true;
            isGameOver = true;
        }
        if (timer < 0) {
            isGameOver = true;
        }
    }
    private void idleLevelTrain4 () {
        if (rand.nextInt(100) == 0) spawnRedCell(100, getRandSpawnLoc(), false);
        if (rand.nextInt(100) == 0) spawnThromb(100, getRandSpawnLoc());
        if (rand.nextInt(75) == 0) spawnVirus(150, 1.25f - rand.nextFloat() / 2f, getRandSpawnLoc(), rand.nextInt(17) + 1);
        timer--;
        if (playerScore + lymphScore >= score) {
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
                if (rand.nextInt(100) == 0) spawnRedCell(100, getRandSpawnLoc(), false);
                if (rand.nextInt(100) == 0) spawnThromb(100, getRandSpawnLoc());
                timer--;
                if (viruses.size() == 0) {
                    isVictory = true;
                    isGameOver = true;
                }
                if (timer < 0) {
                    isGameOver = true;
                }
                break;
            }
            case 2: {
                if (rand.nextInt(100) == 0) spawnRedCell(100, getRandSpawnLoc(), false);
                if (rand.nextInt(100) == 0) spawnThromb(100, getRandSpawnLoc());
                if (rand.nextInt(100) == 0) spawnVirus(150, 1.25f - rand.nextFloat() / 2f, getRandSpawnLoc(), rand.nextInt(17) + 1);
                if (playerScore >= score) {
                    isGameOver = true;
                    isVictory = true;
                }
                if (lymphScore >= score) {
                    isGameOver = true;
                }
                break;
            }
            case 3: {
                timer--;
                if (rand.nextInt(100) == 0) spawnThromb(100, getRandSpawnLoc());
                if (score <= 0) {
                    isVictory = true;
                    isGameOver = true;
                }
                if (timer < 0) {
                    isGameOver = true;
                }
                break;
            }
            case 0: {
                if (rand.nextInt(100) == 0) spawnRedCell(100, getRandSpawnLoc(), false);
                if (rand.nextInt(100) == 0) spawnThromb(100, getRandSpawnLoc());
                if (rand.nextInt(100) == 0) spawnVirus(150, 1.25f - rand.nextFloat() / 2f, getRandSpawnLoc(), rand.nextInt(17) + 1);
                timer--;
                if (playerScore + lymphScore >= score) {
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
                    ": " + (playerScore + lymphScore) + "/" + score,
                    0.1f * meter + 0.125f * game.width,
                    map.getY() - 0.075f * game.width + 0.5f * game.fonts.smallB.getHeight("A")
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
                    ": " + (viruses.size()),
                    0.1f*meter + 0.125f*game.width,
                    map.getY() - 0.075f*game.width + 0.5f*game.fonts.smallB.getHeight("A")
            );
        } else if (curLevel != 1) {
            switch (curLevel % 4) {
                case 1: {
                    int sec = (int)(timer/60f);
                    game.fonts.smallB.draw(
                            stage.getBatch(),
                            ": " + (viruses.size()),
                            0.1f*meter + 0.125f*game.width,
                            map.getY() - 0.075f*game.width + 0.5f*game.fonts.smallB.getHeight("A")
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
                            ": " + playerScore + "/" + score,
                            0.1f * meter + 0.125f * game.width,
                            map.getY() - 0.075f * game.width + 0.5f * game.fonts.smallB.getHeight("A")
                    );
                    game.fonts.smallB.draw(
                            stage.getBatch(),
                            ": " + lymphScore + "/" + score,
                            0.1f * meter + 0.125f * game.width,
                            map.getY() - 0.2f * game.width + 0.5f * game.fonts.smallB.getHeight("A")
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
                            ": " + score,
                            0.1f*meter + 0.125f*game.width,
                            map.getY() - 0.075f*game.width + 0.5f*game.fonts.smallB.getHeight("A")
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
                            ": " + (playerScore + lymphScore) + "/" + score,
                            0.1f * meter + 0.125f * game.width,
                            map.getY() - 0.075f * game.width + 0.5f * game.fonts.smallB.getHeight("A")
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
        game.prefs.flush();
        game.sounds.mainTheme.pause();
        game.sounds.mainTheme.stop();
    }

    @Override
    public void resume () {
        if (!game.sounds.mainTheme.isPlaying()) game.sounds.mainTheme.play();
    }

    @Override
    public void hide () {
        dispose();
    }

    @Override
    public void dispose () {
        game.prefs.putInteger("max_level", Menu.maxLevel);
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
        if (!isGameOver) {
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
