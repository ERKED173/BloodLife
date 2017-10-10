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

public class Space implements Screen, GestureListener {

    private MainBL game;
    private Stage stage;
    private Obfuscation obf;
    private RandomXS128 rand;
    private Spectator spec;
    private LinkedList<AdvSprite> advSprites;
    private LinkedList<Entity> entities;
    private LinkedList<Bound> bounds;
    private Entity player;

    private World world;
    private ContactListener contactListener;
    private static final float STEP_TIME = 1f / 60f;
    private static final int VELOCITY_ITERATIONS = 5;
    private static final int POSITION_ITERATIONS = 3;
    private float accumulator = 0f;
    private float meter = Technical.METER;

    private static int level;
    private boolean isTrainOver = false;

    private float newVirusX;
    private float newVirusY;

    private boolean isGameOver = false;
    private int virusScore = 0;
    private int oldVirusScore = 0;
    private int playerScore = 0;

    private int virusNumber;
    private int redCellNumber;
    private int lymphNumber;

    private AdvSprite map;
    private AdvSprite point;
    private AdvSprite virusSign;
    private AdvSprite redCellSign;
    private LinkedList<AdvSprite> lymphPoints;
    private LinkedList<AdvSprite> virusPoints;

    private BLButton exit;
    private boolean toMenu = false;

    public Space (MainBL game, int level) {
        this.game = game;
        Space.level = level;
        stage = new Stage();
        rand = new RandomXS128();
        float worldWidth = (1f / Technical.METER) * Gdx.graphics.getWidth();
        float worldHeight = worldWidth*(game.height/game.width);
        spec = new Spectator(worldWidth, worldHeight);
        spec.setPosition(10f, 10f);
        world = new World(new Vector2(0, 0), true);
        entities = new LinkedList<Entity>();
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

        addBounds();
        for (int i = 0; i < rand.nextInt(10) + 20; i++) { addPart(); }

/*
        spec.addAction(Actions.sequence(
                Actions.delay(5f),
                Actions.moveBy(0f, 6f, 3f),
                Actions.delay(5f),
                Actions.moveBy(0f, 3f, 0.5f)
                ));
*/

        stage.addActor(spec);
        stage.addActor(player);
        entities.addFirst(player);

        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(stage);
        multiplexer.addProcessor(new GestureDetector(this));
        Gdx.input.setInputProcessor(multiplexer);
        world.setContactListener(contactListener);

        mapInit();
        initAllLevels();

        buttonInit();
        obf = new Obfuscation(game.atlas.createSprite("obfuscation"), true);
    }

    @Override
    public void render (float delta) {
        Gdx.gl.glClearColor(220f/255f, 150f/255f, 180f/255f, 0f);
        //Gdx.gl.glClearColor(255f/255f, 255f/255f, 255f/255f, 0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        redCellNumber = 0;
        virusNumber = 0;
        lymphNumber = 0;
        for (Entity e :entities) {
            if (e.getName().equals("red_cell")) redCellNumber++;
            if (e.getName().equals("virus") || e.getName().equals("virus_advanced")) virusNumber++;
            if (e.getName().equals("lymphocyte")) lymphNumber++;
        }

        gravity();
        lifeCycle(level);

        buttonUpdate();
        mapUpdate();

        stage.act();
        stage.draw();

        accumulator += Math.min(delta, 0.25f);
        if (accumulator >= STEP_TIME) {
            accumulator -= STEP_TIME;
            world.step(STEP_TIME, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
        }

        if (isTrainOver && !spec.hasActions()) {
            spec.setPosition(player.getPosition().x, player.getPosition().y);
        }
        spec.update();

        stage.getBatch().begin();
        if (isTrainOver) {
            drawText();
            redCellSign.draw(stage.getBatch(), 1f);
            virusSign.draw(stage.getBatch(), 1f);
            exit.get().draw(stage.getBatch(), 1f);
            map.draw(stage.getBatch(), 1f);
            point.draw(stage.getBatch(), 1f);
        }

        if(obf.isActive() && !toMenu){
            obf.deactivate(1f, delta);
        } else if (toMenu) {
            if (obf.isActive()) {
                game.setScreen(new Menu(game));
                dispose();
            } else {
                obf.activate(1f, delta);
            }
        }
        obf.draw(stage.getBatch(), obf.getAlpha());

        stage.getBatch().end();

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
        exit = new BLButton(
                game,
                0.25f*game.width,
                0.5f*game.height - 0.3f*game.width,
                0.25f*game.width,
                game.fonts.medium.getFont(),
                game.textSystem.get("to_menu_button"),
                1,
                "to_menu_button"
        );
        exit.get().addListener(new ClickListener(){
            @Override
            public void clicked (InputEvent event, float x, float y) {
                if (!obf.isActive() && isTrainOver) {
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
        if (!isTrainOver) {
            exit.get().setVisible(false);
        } else {
            exit.get().setVisible(true);
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
                Iterator<Entity> iteratorI = entities.iterator();
                while (iteratorI.hasNext()) {
                    Entity entityI = iteratorI.next();
                    if (contact.getFixtureA().getBody().equals(entityI.getBody())) {
                        Iterator<Entity> iteratorJ = entities.iterator();
                        while (iteratorJ.hasNext()) {
                            Entity entityJ = iteratorJ.next();
                            if (contact.getFixtureB().getBody().equals(entityJ.getBody())) {
                                if (entityI.getName()!= null && entityJ.getName() != null) {
                                    if (virusNumber > 0) {
                                        if ((entityI.getName().equals("virus") || entityI.getName().equals("virus_advanced") || entityI.getName().equals("virus_super_advanced")) && entityJ.getName().equals("red_cell")) {
                                            if (entityI.getLifeTime() > 0 && entityJ.getLifeTime() > 0) {
                                                virusScore++;
                                                newVirusX = entityJ.getPosition().x;
                                                newVirusY = entityJ.getPosition().y;
                                                entityJ.decreaseLT(10000);
                                                //System.out.println("Tupoy virus ubivaet nashih!");
                                            }
                                        } else if (entityI.getName().equals("red_cell") && (entityJ.getName().equals("virus") || entityJ.getName().equals("virus_advanced") || entityJ.getName().equals("virus_super_advanced"))) {
                                            if (entityI.getLifeTime() > 0 && entityJ.getLifeTime() > 0) {
                                                virusScore++;
                                                newVirusX = entityJ.getPosition().x;
                                                newVirusY = entityJ.getPosition().y;
                                                entityI.decreaseLT(10000);
                                                //System.out.println("Tupoy virus ubivaet nashih!");
                                            }
                                        }
                                        //
                                        if (lymphNumber > 0) {
                                            if ((entityI.getName().equals("virus") || entityI.getName().equals("virus_advanced") || entityI.getName().equals("virus_super_advanced")) && entityJ.getName().equals("lymphocyte")) {
                                                if (entityI.getLifeTime() > 0 && entityJ.getLifeTime() > 0) {
                                                    playerScore++;
                                                    entityI.decreaseLT(10000);
                                                    //System.out.println("Moy drug ubil virus!");
                                                }
                                            } else if (entityI.getName().equals("lymphocyte") && (entityJ.getName().equals("virus") || entityJ.getName().equals("virus_advanced") || entityJ.getName().equals("virus_super_advanced"))) {
                                                if (entityI.getLifeTime() > 0 && entityJ.getLifeTime() > 0) {
                                                    playerScore++;
                                                    entityJ.decreaseLT(10000);
                                                    //System.out.println("Moy drug ubil virus!");
                                                }
                                            }
                                        }
                                        //
                                        if (entityI.getName().equals("player") && (entityJ.getName().equals("virus") || entityJ.getName().equals("virus_advanced") || entityJ.getName().equals("virus_super_advanced"))) {
                                            if (entityJ.getLifeTime() > 0) {
                                                playerScore++;
                                                entityJ.decreaseLT(10000);
                                                game.sounds.death.play(1f, 1.25f - 0.5f * rand.nextFloat(), 0f);
                                                //System.out.println("Ya ubil virus!");
                                            }
                                        } else if ((entityI.getName().equals("virus") || entityI.getName().equals("virus_advanced") || entityI.getName().equals("virus_super_advanced")) && entityJ.getName().equals("player")) {
                                            if (entityI.getLifeTime() > 0) {
                                                playerScore++;
                                                entityI.decreaseLT(10000);
                                                game.sounds.death.play(1f, 1.25f - 0.5f * rand.nextFloat(), 0f);
                                                //System.out.println("Ya ubil virus!");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        };
    }
    private void mapInit () {
        map = new AdvSprite(game.atlas.createSprite("map"), 0f, 0f, 0.3f * game.width, 0.3f * game.width);
        point = new AdvSprite(game.atlas.createSprite("map_point"), 0f, 0f, 0.02f * game.width, 0.02f * game.width);
        lymphPoints = new LinkedList<AdvSprite>();
        virusPoints = new LinkedList<AdvSprite>();

        virusSign = new AdvSprite(game.atlas.createSprite("virus", 11), 0f, 0f, 0.1f*game.width, 0.1f*game.width);
        redCellSign = new AdvSprite(game.atlas.createSprite("red"), 0f, 0f, 0.1f*game.width, 0.1f*game.width);

        stage.addActor(virusSign);
        stage.addActor(redCellSign);
        stage.addActor(map);
        stage.addActor(point);
    }
    private void mapUpdate () {
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

        redCellSign.setPosition(
                0.1f*meter,
                map.getY() - 0.125f*game.width
        );
        redCellSign.addAction(Actions.rotateBy(1f));
        redCellSign.updateSprite();
        virusSign.setPosition(
                0.1f*meter,
                map.getY() - 0.25f*game.width
        );
        virusSign.addAction(Actions.rotateBy(1f));
        virusSign.updateSprite();

        if (lymphNumber > lymphPoints.size()) {
            AdvSprite lymphP = new AdvSprite(game.atlas.createSprite("map_point"), 0f, 0f, 0.015f * game.width, 0.015f * game.width);
            lymphP.setColor(Color.VIOLET);
            lymphPoints.add(lymphP);
            stage.addActor(lymphPoints.get(lymphPoints.size() - 1));
        } else if (lymphNumber < lymphPoints.size()) {
            lymphPoints.getFirst().remove();
            lymphPoints.removeFirst();
        }
        if (virusNumber > virusPoints.size()) {
            AdvSprite virusP = new AdvSprite(game.atlas.createSprite("map_point"), 0f, 0f, 0.015f * game.width, 0.015f * game.width);
            virusP.setColor(Color.RED);
            virusPoints.add(virusP);
            stage.addActor(virusPoints.get(virusPoints.size() - 1));
        } else if (virusNumber < virusPoints.size()) {
            virusPoints.getFirst().remove();
            virusPoints.removeFirst();
        }

        int lIterator = 0;
        int vIterator = 0;
        for (Entity e : entities) {
            if (lymphNumber > 0 && e.getName().equals("lymphocyte")) {
                if (lIterator < lymphPoints.size()) {
                    lymphPoints.get(lIterator).setPosition(
                            (0.1f * meter + (map.getWidth() / 16f)) - ((80f - e.getPosition().x) / 90f - 1f) * (map.getWidth() * 13f / 16f) + 0.25f * lymphPoints.get(lIterator).getWidth(),
                            (spec.get().viewportHeight * meter - point.getHeight() - 0.1f * meter) - ((80f - e.getPosition().y) / 90f) * (map.getWidth() * 13f / 16f) - 0.5f * lymphPoints.get(lIterator).getHeight()
                    );
                    lymphPoints.get(lIterator).updateSprite();
                    lIterator++;
                }
            }
            if (virusNumber > 0 && (e.getName().equals("virus") || e.getName().equals("virus_advanced"))) {
                if (vIterator < virusPoints.size()) {
                    virusPoints.get(vIterator).setPosition(
                            (0.1f * meter + (map.getWidth() / 16f)) - ((80f - e.getPosition().x) / 90f - 1f) * (map.getWidth() * 13f / 16f) + 0.25f * virusPoints.get(vIterator).getWidth(),
                            (spec.get().viewportHeight * meter - point.getHeight() - 0.1f * meter) - ((80f - e.getPosition().y) / 90f) * (map.getWidth() * 13f / 16f) - 0.5f * virusPoints.get(vIterator).getHeight()
                    );
                    virusPoints.get(vIterator).updateSprite();
                    vIterator++;
                }
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
        Color color = new Color((rand.nextInt(25) + 230)/255f, (rand.nextInt(15) + 1)/255f, (rand.nextInt(50) + 25)/255f, 1f);
        float x = meter*spec.get().position.x - meter*spec.get().viewportWidth + rand.nextInt((int)(2f*meter*spec.get().viewportWidth));
        float y = meter*spec.get().position.y - meter*spec.get().viewportHeight + rand.nextInt((int)(2f*meter*spec.get().viewportHeight));
        float length = rand.nextInt((int)(0.01f*meter*spec.get().viewportWidth)) + 0.01f*meter*spec.get().viewportWidth;
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

    private void spawnRedCell (int hp) {
        float x = spec.get().position.x - spec.get().viewportWidth + rand.nextInt((int)spec.get().viewportWidth) + rand.nextFloat();
        float y = spec.get().position.y - spec.get().viewportHeight + rand.nextInt((int)spec.get().viewportHeight) + rand.nextFloat();
        spawnRedCell(hp, new Vector2(x, y));
    }
    private void spawnRedCell (int hp, float x, float y) {
        spawnRedCell(hp, new Vector2(x, y));
    }
    private void spawnRedCell (int hp, Vector2 vec) {
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

        stage.addActor(redCell);
        entities.addFirst(redCell);
    } // Original
    private void spawnVirus (int hp, float speed) {
        float x = spec.get().position.x - spec.get().viewportWidth + rand.nextInt((int)spec.get().viewportWidth) + rand.nextFloat();
        float y = spec.get().position.y - spec.get().viewportHeight + rand.nextInt((int)spec.get().viewportHeight) + rand.nextFloat();
        spawnVirus(hp, speed, new Vector2(x, y));
    }
    private void spawnVirus (int hp, float speed, float x, float y) {
        spawnVirus(hp, speed, new Vector2(x, y));
    }
    private void spawnVirus (int hp, float speed, Vector2 vec) {
        spawnVirus(hp, speed, vec, rand.nextInt(13) + 1);
    }
    private void spawnVirus (int hp, float speed, Vector2 vec, int type) {
        AdvSprite virusEnt;
        Entity virusSub;
        if (type > 12) {
            virusEnt = new AdvSprite(game.atlas.createSprite("virus", type), 0, 0, 2f, 2f);
            virusSub = new Entity(virusEnt, world, 0f, 15f, 0.2f, "virus_super_advanced");
            hp += 200;
        } else if (type > 9) {
            virusEnt = new AdvSprite(game.atlas.createSprite("virus", type), 0, 0, 1f, 1f);
            virusSub = new Entity(virusEnt, world, 0f, 5f, 0.2f, "virus_advanced");
            hp += 50;
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
        entities.addFirst(virus);
    } // Original
    private void spawnLymph (int hp) {
        float x = spec.get().position.x - spec.get().viewportWidth + rand.nextInt((int)spec.get().viewportWidth) + rand.nextFloat();
        float y = spec.get().position.y - spec.get().viewportHeight + rand.nextInt((int)spec.get().viewportHeight) + rand.nextFloat();
        spawnLymph(hp, new Vector2(x, y));
    }
    private void spawnLymph (int hp, float x, float y) {
        spawnLymph(hp, new Vector2(x, y));
    }
    private void spawnLymph (int hp, Vector2 vec) {
        AdvSprite lymEntity = new AdvSprite(game.atlas.createSprite("purple"), 0, 0, 1f, 1f);
        Entity lymSubject = new Entity(lymEntity, world, 0f, 8f, 0.2f, "lymphocyte");
        Lymphocyte lymphocyte = new Lymphocyte(lymSubject, hp);
        lymphocyte.getBody().setTransform(vec.x, vec.y, 0f);
        if (rand.nextInt(2) == 0) lymphocyte.getBody().setAngularVelocity(rand.nextFloat() + 0.3f);
        else lymphocyte.getBody().setAngularVelocity(-rand.nextFloat() - 0.3f);

        float sizeX = lymphocyte.getWidth(), sizeY = lymphocyte.getHeight();
        lymphocyte.setSize(0f, 0f);
        lymphocyte.addAction(Actions.alpha(0f));
        lymphocyte.addAction(Actions.parallel(
                Actions.sizeTo(sizeX, sizeY, 1f),
                Actions.alpha(1f, 1f)
        ));

        stage.addActor(lymphocyte);
        entities.addFirst(lymphocyte);
    } // Original

    private void gravity () {
        for (int i = 0; i < entities.size(); i++) {
            float velX = entities.get(i).getBody().getLinearVelocity().x;
            float velY = entities.get(i).getBody().getLinearVelocity().y;
            float s = spec.get().viewportHeight;
            if (velX != 0f) {
                if (velX > 0f) {
                    if (velX - 0.005f * s <= 0f)
                        entities.get(i).getBody().setLinearVelocity(0f, velY);
                    else
                        entities.get(i).getBody().setLinearVelocity(velX - 0.005f * s, velY);
                }
                if (velX < 0f) {
                    if (velX + 0.005f * s >= 0f)
                        entities.get(i).getBody().setLinearVelocity(0f, velY);
                    else
                        entities.get(i).getBody().setLinearVelocity(velX + 0.005f * s, velY);
                }
            }
            velX = entities.get(i).getBody().getLinearVelocity().x;
            if (velY != 0f) {
                if (velY > 0f) {
                    if (velY - 0.005f * s <= 0f)
                        entities.get(i).getBody().setLinearVelocity(velX, 0f);
                    else
                        entities.get(i).getBody().setLinearVelocity(velX, velY - 0.005f * s);
                }
                if (velY < 0f) {
                    if (velY + 0.005f * s >= 0f)
                        entities.get(i).getBody().setLinearVelocity(velX, 0f);
                    else
                        entities.get(i).getBody().setLinearVelocity(velX, velY + 0.005f * s);
                }
            }
        }
    }
    private void lifeCycle (int level) {

        switch (level) {
            case -1: {
                idleLevelTest();
                break;
            }
            case 0: {
                idleLevel_0();
                break;
            }
            default: {
                break;
            }
        }

        Iterator<Entity> iteratorI = entities.iterator();
        while (iteratorI.hasNext()) {
            Entity entityI = iteratorI.next();
            entityI.updateSprite(spec.get());
            if (entityI.isAlive()) {
                if (entityI.getName() != null) {
                    if (lymphNumber > 0 && entityI.getName().equals("lymphocyte")) {
                        float x = entityI.getPosition().x;
                        float y = entityI.getPosition().y;
                        double minDist = 1000000;
                        Iterator<Entity> iteratorJ = entities.iterator();
                        while (iteratorJ.hasNext()) {
                            Entity entityJ = iteratorJ.next();
                            if (!entityI.equals(entityJ)) {
                                if (virusNumber > 0 && entityJ.getName() != null && (entityJ.getName().equals("virus") || entityJ.getName().equals("virus_advanced") || entityJ.getName().equals("virus_super_advanced"))) {
                                    float tempX = (entityJ.getPosition().x - entityI.getPosition().x);
                                    float tempY = (entityJ.getPosition().y - entityI.getPosition().y);
                                    double distance = Math.sqrt((double)(tempX * tempX + tempY * tempY));
                                    if (distance < minDist) {
                                        minDist = distance;
                                        x = entityJ.getPosition().x;
                                        y = entityJ.getPosition().y;
                                    }
                                }
                            }
                        }
                        if (virusNumber > 0) {
                            entityI.updateLife(x, y);
                        }
                    } else if (virusNumber > 0 && entityI.getName().equals("virus_advanced")) {
                        float x = entityI.getPosition().x;
                        float y = entityI.getPosition().y;
                        double minDist = 1000000;
                        Iterator<Entity> iteratorJ = entities.iterator();
                        while (iteratorJ.hasNext()) {
                            Entity entityJ = iteratorJ.next();
                            if (!entityI.equals(entityJ)) {
                                if (redCellNumber > 0 && entityJ.getName() != null && entityJ.getName().equals("red_cell")) {
                                    float tempX = (entityJ.getPosition().x - entityI.getPosition().x);
                                    float tempY = (entityJ.getPosition().y - entityI.getPosition().y);
                                    double distance = Math.sqrt((double)(tempX * tempX + tempY * tempY));
                                    if (distance < minDist) {
                                        minDist = distance;
                                        x = entityJ.getPosition().x;
                                        y = entityJ.getPosition().y;
                                    }
                                }
                            }
                        }
                        if (redCellNumber > 0) {
                            entityI.updateLife(x, y);
                        }
                    } else if (virusNumber > 0 && entityI.getName().equals("virus_super_advanced")) {
                        if (rand.nextInt(250) == 0){
                            newVirusX = entityI.getPosition().x;
                            newVirusY = entityI.getPosition().y;
                            virusScore++;
                        }
                        entityI.updateLife();
                    } else {
                        entityI.updateLife();
                    }
                    if (!entityI.getName().equals("player")) entityI.decreaseLT();
                }
            }
            if (entityI.getLifeTime() <= 0) {
                if (entityI.isAlive()) {
                    entityI.kill();
                }
                if (!entityI.hasActions()){
                    entityI.getWorld().destroyBody(entityI.getBody());
                    iteratorI.remove();
                }
            }
        }
        for (AdvSprite sprite : advSprites) {
            sprite.updateSprite(spec.get());
            if (!sprite.hasActions()) {
                changePart(sprite);
            }
        }
        for (Bound b : bounds) {
            b.updateSprite(spec.get());
        }
    }

    private void initAllLevels () {
        switch (level) {
            case -2: {
                initLevelRound();
                break;
            }
            case -1: {

                break;
            }
            case 0: {

                break;
            }
            default: {
                break;
            }
        }
    }
    private void initLevelRound () {
        for (int i = 0; i < 25; i++) spawnRedCell(500, getSpawnLocation());
        for (int i = 0; i < 15; i++) spawnVirus(500, 0.2f + 0.1f*rand.nextFloat(), getSpawnLocation());
        for (int i = 0; i < 5; i++) spawnLymph(500, getSpawnLocation());
    }

    private void idleLevelTest () {
        isTrainOver = true;
        if (rand.nextInt(100) == 0) spawnVirus(75, rand.nextFloat() / 2f + 0.3f, getSpawnLocation());
        if (rand.nextInt(50) == 0) spawnRedCell(150, getSpawnLocation());
        if (rand.nextInt(300) == 0) spawnLymph(125, getSpawnLocation());
        if (oldVirusScore < virusScore) {
            spawnVirus(75, rand.nextFloat() / 2f + 0.3f, newVirusX, newVirusY);
            oldVirusScore = virusScore;
        }
    }
    private void idleLevel_0 () {
        if (!isTrainOver) {
            if (spec.getY() == player.getPosition().y) {
                isTrainOver = true;
            }
            if (!spec.hasActions() && !isTrainOver) {
                spawnVirus(900, 0.75f, 0f, 12f);
                spawnLymph(9000, 0f, 18f);
                spec.addAction(Actions.sequence(
                        Actions.delay(10f),
                        Actions.moveTo(player.getPosition().x, player.getPosition().y, 2f)
                ));
            }

            float spawnX = -5f + rand.nextInt(10) + rand.nextFloat();
            float spawnY = -5f + rand.nextInt(10) + rand.nextFloat();
            if (rand.nextInt(50) == 0) spawnRedCell(150, spawnX, spawnY);
            spawnX = -5f + rand.nextInt(10) + rand.nextFloat();
            spawnY = -5f + rand.nextInt(10) + rand.nextFloat();
            if (rand.nextInt(250) == 0) spawnLymph(150, spawnX, spawnY);

            if (oldVirusScore < virusScore) {
                spawnVirus(50, rand.nextFloat() / 3f + 0.25f, newVirusX, newVirusY);
                oldVirusScore = virusScore;
            }
        } else {
            if (rand.nextInt(100) == 0) spawnRedCell(50);
            if (rand.nextInt(250) == 0) spawnLymph(50);
        }
    }

    private void drawText ()  {
        String fps = "FPS: " + Gdx.graphics.getFramesPerSecond();
        game.fonts.mediumB.draw(
                stage.getBatch(),
                fps,
                7.9f*meter - game.fonts.mediumB.getWidth(fps),
                spec.get().viewportHeight * meter - 0.1f*meter - 3f*game.fonts.mediumB.getHeight("A")
        );
        game.fonts.mediumB.draw(
                stage.getBatch(),
                "Entities: " + entities.size(),
                7.9f*meter - game.fonts.mediumB.getWidth("Entities: " + entities.size()),
                spec.get().viewportHeight * meter - 0.1f*meter - 4.5f*game.fonts.mediumB.getHeight("A")
        );
        game.fonts.mediumB.draw(
                stage.getBatch(),
                ": " + redCellNumber,
                0.1f*meter + 0.125f*game.width,
                map.getY() - 0.075f*game.width + 0.5f*game.fonts.mediumB.getHeight("A")
        );
        game.fonts.mediumB.draw(
                stage.getBatch(),
                ": " + virusNumber,
                0.1f*meter + 0.125f*game.width,
                map.getY() - 0.2f*game.width + 0.5f*game.fonts.mediumB.getHeight("A")
        );
    }

    private Vector2 getSpawnLocation () {
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
        game.sounds.mainTheme.pause();
        game.sounds.mainTheme.stop();
    }

    @Override
    public void resume () {
        if (!game.sounds.mainTheme.isPlaying()) game.sounds.mainTheme.play();
    }

    @Override
    public void hide () {

    }

    @Override
    public void dispose () {
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

        if (isTrainOver) {
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
