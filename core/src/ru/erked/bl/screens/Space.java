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
    private LinkedList<Entity> lymphocytes;
    private LinkedList<Entity> viruses;
    private LinkedList<Entity> virusesAdv;
    private LinkedList<Entity> virusesSupAdv;
    private LinkedList<Bound> bounds;
    private Entity player;

    private World world;
    private ContactListener contactListener;
    private static final float STEP_TIME = 1f / 60f;
    private static final int VELOCITY_ITERATIONS = 5;
    private static final int POSITION_ITERATIONS = 3;
    private float accumulator = 0f;
    private float meter = Technical.METER;

    private int playerScore = 0;
    private int lymphScore = 0;

    private AdvSprite map;
    private AdvSprite point;
    private AdvSprite lymphSign;
    private AdvSprite playerSign;
    private LinkedList<AdvSprite> redCellPoints;
    private LinkedList<AdvSprite> lymphPoints;
    private LinkedList<AdvSprite> virusPoints;

    private BLButton exit;
    private boolean toMenu = false;

    Space (MainBL game) {
        this.game = game;
        stage = new Stage();
        rand = new RandomXS128();
        float worldWidth = (1f / Technical.METER) * Gdx.graphics.getWidth();
        float worldHeight = worldWidth*(game.height/game.width);
        spec = new Spectator(worldWidth, worldHeight);
        spec.setPosition(10f, 10f);
        world = new World(new Vector2(0, 0), true);
        redCells = new LinkedList<Entity>();
        viruses = new LinkedList<Entity>();
        virusesAdv = new LinkedList<Entity>();
        virusesSupAdv = new LinkedList<Entity>();
        lymphocytes = new LinkedList<Entity>();
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
        spawnLymph(1000000, getRandSpawnLoc());

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
    }

    @Override
    public void render (float delta) {
        Gdx.gl.glClearColor(220f/255f, 150f/255f, 180f/255f, 0f);
        //Gdx.gl.glClearColor(255f/255f, 255f/255f, 255f/255f, 0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float s = 0.01f * spec.get().viewportHeight;
        gravity(player.getBody(), s);
        for (Entity r : redCells) gravity(r.getBody(), s);
        for (Entity l : lymphocytes) gravity(l.getBody(), s);
        for (Entity v : viruses) gravity(v.getBody(), s);
        for (Entity v : virusesAdv) gravity(v.getBody(), s);
        for (Entity v : virusesSupAdv) gravity(v.getBody(), s);

        lifeCycle();

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
        drawText();
        playerSign.draw(stage.getBatch(), 1f);
        lymphSign.draw(stage.getBatch(), 1f);
        exit.get().draw(stage.getBatch(), 1f);
        map.draw(stage.getBatch(), 1f);
        point.draw(stage.getBatch(), 1f);

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
                lymphVersusViruses(contact);
                redCellsVersusViruses(contact);
                playerVersusViruses(contact);
            }
        };
    }
    private void lymphVersusViruses (Contact contact) {
        Iterator<Entity> iLymph = lymphocytes.iterator();
        while (iLymph.hasNext()) {
            Entity lymph = iLymph.next();
            Iterator<Entity> iVirus = viruses.iterator();
            Iterator<Entity> iVirusAdv = virusesAdv.iterator();
            Iterator<Entity> iVirusSupAdv = virusesSupAdv.iterator();
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
            //
            while (iVirusAdv.hasNext()) {
                Entity virusAdv = iVirusAdv.next();
                if (contact.getFixtureA().getBody().equals(lymph.getBody()) && contact.getFixtureB().getBody().equals(virusAdv.getBody())) {
                    if (lymph.getLifeTime() > 0 && virusAdv.getLifeTime() > 0) {
                        virusAdv.decreaseLT(50);
                        virusAdv.addAction(Actions.color(Color.RED));
                        virusAdv.addAction(Actions.color(Color.WHITE, 0.5f));
                        if (virusAdv.getLifeTime() == 0) lymphScore++;
                        //System.out.println("Moy drug ubil virus!");
                        break;
                    }
                } else if (contact.getFixtureB().getBody().equals(lymph.getBody()) && contact.getFixtureA().getBody().equals(virusAdv.getBody())) {
                    if (lymph.getLifeTime() > 0 && virusAdv.getLifeTime() > 0) {
                        virusAdv.decreaseLT(50);
                        if (virusAdv.getLifeTime() == 0) lymphScore++;
                        //System.out.println("Moy drug ubil virus!");
                        break;
                    }
                }
            }
            if (iVirusAdv.hasNext()) break;
            //
            while (iVirusSupAdv.hasNext()) {
                Entity virusSupAdv = iVirusSupAdv.next();
                if (contact.getFixtureA().getBody().equals(lymph.getBody()) && contact.getFixtureB().getBody().equals(virusSupAdv.getBody())) {
                    if (lymph.getLifeTime() > 0 && virusSupAdv.getLifeTime() > 0) {
                        virusSupAdv.decreaseLT(50);
                        virusSupAdv.addAction(Actions.color(Color.RED));
                        virusSupAdv.addAction(Actions.color(Color.WHITE, 0.5f));
                        if (virusSupAdv.getLifeTime() == 0) lymphScore++;
                        //System.out.println("Moy drug ubil virus!");
                        break;
                    }
                } else if (contact.getFixtureB().getBody().equals(lymph.getBody()) && contact.getFixtureA().getBody().equals(virusSupAdv.getBody())) {
                    if (lymph.getLifeTime() > 0 && virusSupAdv.getLifeTime() > 0) {
                        virusSupAdv.decreaseLT(50);
                        virusSupAdv.addAction(Actions.color(Color.RED));
                        virusSupAdv.addAction(Actions.color(Color.WHITE, 0.5f));
                        if (virusSupAdv.getLifeTime() == 0) lymphScore++;
                        //System.out.println("Moy drug ubil virus!");
                        break;
                    }
                }
            }
            if (iVirusSupAdv.hasNext()) break;
        }
    }
    private void redCellsVersusViruses (Contact contact) {
        Iterator<Entity> virusI = viruses.iterator();
        Iterator<Entity> virusAdvI = virusesAdv.iterator();
        Iterator<Entity> virusSupAdvI = virusesSupAdv.iterator();
        while (virusI.hasNext()) {
            Entity virus = virusI.next();
            Iterator<Entity> redCellI = redCells.iterator();
            while (redCellI.hasNext()) {
                Entity redCell = redCellI.next();
                if (contact.getFixtureA().getBody().equals(virus.getBody()) && contact.getFixtureB().getBody().equals(redCell.getBody())) {
                    if (virus.getLifeTime() > 0 && redCell.getLifeTime() > 0) {
                        redCell.decreaseLT(10000);
                        //System.out.println("Tupoy virus ubivaet nashih!");
                        break;
                    }
                } else if (contact.getFixtureB().getBody().equals(virus.getBody()) && contact.getFixtureA().getBody().equals(redCell.getBody())) {
                    if (virus.getLifeTime() > 0 && redCell.getLifeTime() > 0) {
                        redCell.decreaseLT(10000);
                        //System.out.println("Tupoy virus ubivaet nashih!");
                        break;
                    }
                }
            }
        }

        while (virusAdvI.hasNext()) {
            Entity virusAdv = virusAdvI.next();
            Iterator<Entity> redCellI = redCells.iterator();
            while (redCellI.hasNext()) {
                Entity redCell = redCellI.next();
                if (contact.getFixtureA().getBody().equals(virusAdv.getBody()) && contact.getFixtureB().getBody().equals(redCell.getBody())) {
                    if (virusAdv.getLifeTime() > 0 && redCell.getLifeTime() > 0) {
                        redCell.decreaseLT(10000);
                        //System.out.println("Tupoy virus ubivaet nashih!");
                        break;
                    }
                } else if (contact.getFixtureB().getBody().equals(virusAdv.getBody()) && contact.getFixtureA().getBody().equals(redCell.getBody())) {
                    if (virusAdv.getLifeTime() > 0 && redCell.getLifeTime() > 0) {
                        redCell.decreaseLT(10000);
                        //System.out.println("Tupoy virus ubivaet nashih!");
                        break;
                    }
                }
            }
        }

        while (virusSupAdvI.hasNext()) {
            Entity virusSupAdv = virusSupAdvI.next();
            Iterator<Entity> redCellI = redCells.iterator();
            while (redCellI.hasNext()) {
                Entity redCell = redCellI.next();
                if (contact.getFixtureA().getBody().equals(virusSupAdv.getBody()) && contact.getFixtureB().getBody().equals(redCell.getBody())) {
                    if (virusSupAdv.getLifeTime() > 0 && redCell.getLifeTime() > 0) {
                        redCell.decreaseLT(10000);
                        //System.out.println("Tupoy virus ubivaet nashih!");
                        break;
                    }
                } else if (contact.getFixtureB().getBody().equals(virusSupAdv.getBody()) && contact.getFixtureA().getBody().equals(redCell.getBody())) {
                    if (virusSupAdv.getLifeTime() > 0 && redCell.getLifeTime() > 0) {
                        redCell.decreaseLT(10000);
                        //System.out.println("Tupoy virus ubivaet nashih!");
                        break;
                    }
                }
            }
        }
    }
    private void playerVersusViruses (Contact contact) {
        Iterator<Entity> iVirus = viruses.iterator();
        Iterator<Entity> iVirusAdv = virusesAdv.iterator();
        Iterator<Entity> iVirusSupAdv = virusesSupAdv.iterator();
        while (iVirus.hasNext()) {
            Entity virus = iVirus.next();
            if (contact.getFixtureA().getBody().equals(player.getBody()) && contact.getFixtureB().getBody().equals(virus.getBody())) {
                if (player.getLifeTime() > 0 && virus.getLifeTime() > 0) {
                    virus.decreaseLT(60);
                    virus.addAction(Actions.color(Color.RED));
                    virus.addAction(Actions.color(Color.WHITE, 0.5f));
                    if (virus.getLifeTime() == 0) {
                        playerScore++;
                        game.sounds.death.play(1f, 1.25f - rand.nextFloat() / 2f, 0f);
                    }
                    //System.out.println("Ya ubil virus!");
                    break;
                }
            }else if (contact.getFixtureB().getBody().equals(player.getBody()) && contact.getFixtureA().getBody().equals(virus.getBody())) {
                if (virus.getLifeTime() > 0) {
                    virus.decreaseLT(60);
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
        //
        while (iVirusAdv.hasNext()) {
            Entity virusAdv = iVirusAdv.next();
            if (contact.getFixtureA().getBody().equals(player.getBody()) && contact.getFixtureB().getBody().equals(virusAdv.getBody())) {
                if (virusAdv.getLifeTime() > 0) {
                    virusAdv.decreaseLT(60);
                    virusAdv.addAction(Actions.color(Color.RED));
                    virusAdv.addAction(Actions.color(Color.WHITE, 0.5f));
                    if (virusAdv.getLifeTime() == 0) {
                        playerScore++;
                        game.sounds.death.play(1f, 1.25f - rand.nextFloat() / 2f, 0f);
                    }
                    //System.out.println("Ya ubil virus!");
                    break;
                }
            } else if (contact.getFixtureB().getBody().equals(player.getBody()) && contact.getFixtureA().getBody().equals(virusAdv.getBody())) {
                if (virusAdv.getLifeTime() > 0) {
                    virusAdv.decreaseLT(60);
                    virusAdv.addAction(Actions.color(Color.RED));
                    virusAdv.addAction(Actions.color(Color.WHITE, 0.5f));
                    if (virusAdv.getLifeTime() == 0) {
                        playerScore++;
                        game.sounds.death.play(1f, 1.25f - rand.nextFloat() / 2f, 0f);
                    }
                    //System.out.println("Ya ubil virus!");
                    break;
                }
            }
        }
        //
        while (iVirusSupAdv.hasNext()) {
            Entity virusSupAdv = iVirusSupAdv.next();
            if (contact.getFixtureA().getBody().equals(player.getBody()) && contact.getFixtureB().getBody().equals(virusSupAdv.getBody())) {
                if (virusSupAdv.getLifeTime() > 0) {
                    virusSupAdv.decreaseLT(60);
                    virusSupAdv.addAction(Actions.color(Color.RED));
                    virusSupAdv.addAction(Actions.color(Color.WHITE, 0.5f));
                    if (virusSupAdv.getLifeTime() == 0) {
                        playerScore++;
                        game.sounds.death.play(1f, 1.25f - rand.nextFloat() / 2f, 0f);
                    }
                    //System.out.println("Ya ubil virus!");
                    break;
                }
            } else if (contact.getFixtureB().getBody().equals(player.getBody()) && contact.getFixtureA().getBody().equals(virusSupAdv.getBody())) {
                if (virusSupAdv.getLifeTime() > 0) {
                    virusSupAdv.decreaseLT(60);
                    virusSupAdv.addAction(Actions.color(Color.RED));
                    virusSupAdv.addAction(Actions.color(Color.WHITE, 0.5f));
                    if (virusSupAdv.getLifeTime() == 0) {
                        playerScore++;
                        game.sounds.death.play(1f, 1.25f - rand.nextFloat() / 2f, 0f);
                    }
                    //System.out.println("Ya ubil virus!");
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
                game.fonts.medium.getFont(),
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
        redCellPoints = new LinkedList<AdvSprite>();
        lymphPoints = new LinkedList<AdvSprite>();
        virusPoints = new LinkedList<AdvSprite>();

        lymphSign = new AdvSprite(game.atlas.createSprite("lymphocytes"), 0f, 0f, 0.1f*game.width, 0.1f*game.width);
        playerSign = new AdvSprite(game.atlas.createSprite("white"), 0f, 0f, 0.1f*game.width, 0.1f*game.width);

        stage.addActor(lymphSign);
        stage.addActor(playerSign);
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

        playerSign.setPosition(
                0.1f*meter,
                map.getY() - 0.125f*game.width
        );
        playerSign.addAction(Actions.rotateBy(1f));
        playerSign.updateSprite();
        lymphSign.setPosition(
                0.1f*meter,
                map.getY() - 0.25f*game.width
        );
        lymphSign.addAction(Actions.rotateBy(1f));
        lymphSign.updateSprite();

        if (redCells.size() > redCellPoints.size()) {
            AdvSprite redCellP = new AdvSprite(game.atlas.createSprite("map_point"), 0f, 0f, 0.015f * game.width, 0.015f * game.width);
            redCellP.setColor(Color.SCARLET);
            redCellPoints.add(redCellP);
            stage.addActor(redCellPoints.get(redCellPoints.size() - 1));
        } else if (redCells.size() < redCellPoints.size()) {
            redCellPoints.getFirst().remove();
            redCellPoints.removeFirst();
        }
        if (lymphocytes.size() > lymphPoints.size()) {
            AdvSprite lymphP = new AdvSprite(game.atlas.createSprite("map_point"), 0f, 0f, 0.015f * game.width, 0.015f * game.width);
            lymphP.setColor(Color.CYAN);
            lymphPoints.add(lymphP);
            stage.addActor(lymphPoints.get(lymphPoints.size() - 1));
        } else if (lymphocytes.size() < lymphPoints.size()) {
            lymphPoints.getFirst().remove();
            lymphPoints.removeFirst();
        }
        if ((viruses.size() + virusesAdv.size() + virusesSupAdv.size()) > virusPoints.size()) {
            AdvSprite virusP = new AdvSprite(game.atlas.createSprite("map_point"), 0f, 0f, 0.015f * game.width, 0.015f * game.width);
            virusP.setColor(Color.OLIVE);
            virusPoints.add(virusP);
            stage.addActor(virusPoints.get(virusPoints.size() - 1));
        } else if ((viruses.size() + virusesAdv.size() + virusesSupAdv.size()) < virusPoints.size()) {
            virusPoints.getFirst().remove();
            virusPoints.removeFirst();
        }

        int rIterator = 0;
        int lIterator = 0;
        int vIterator = 0;
        for (Entity r : redCells) {
            if (rIterator < redCellPoints.size()) {
                redCellPoints.get(rIterator).setPosition(
                        (0.1f * meter + (map.getWidth() / 16f)) - ((80f - r.getPosition().x) / 90f - 1f) * (map.getWidth() * 13f / 16f) + 0.25f * redCellPoints.get(rIterator).getWidth(),
                        (spec.get().viewportHeight * meter - point.getHeight() - 0.1f * meter) - ((80f - r.getPosition().y) / 90f) * (map.getWidth() * 13f / 16f) - 0.5f * redCellPoints.get(rIterator).getHeight()
                );
                redCellPoints.get(rIterator).updateSprite();
                rIterator++;
            }
        }
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
        for (Entity v : virusesAdv) {
            if (vIterator < virusPoints.size()) {
                virusPoints.get(vIterator).setPosition(
                        (0.1f * meter + (map.getWidth() / 16f)) - ((80f - v.getPosition().x) / 90f - 1f) * (map.getWidth() * 13f / 16f) + 0.25f * virusPoints.get(vIterator).getWidth(),
                        (spec.get().viewportHeight * meter - point.getHeight() - 0.1f * meter) - ((80f - v.getPosition().y) / 90f) * (map.getWidth() * 13f / 16f) - 0.5f * virusPoints.get(vIterator).getHeight()
                );
                virusPoints.get(vIterator).updateSprite();
                vIterator++;
            }
        }
        for (Entity v : virusesSupAdv) {
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
        redCells.addFirst(redCell);
    }
    private void spawnVirus (int hp, float speed, Vector2 vec, int type) {
        AdvSprite virusEnt;
        Entity virusSub;
        if (type > 12) {
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
        if (type > 12) {
            virusesSupAdv.addFirst(virus);
        } else if (type > 9) {
            virusesAdv.addFirst(virus);
        } else {
            viruses.addFirst(virus);
        }
    }
    private void spawnLymph (int hp, Vector2 vec) {
        AdvSprite lymEntity = new AdvSprite(game.atlas.createSprite("blue"), 0, 0, 1f, 1f);
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
        lymphocytes.addFirst(lymphocyte);
        //
        lymEntity = new AdvSprite(game.atlas.createSprite("coral"), 0, 0, 1f, 1f);
        lymSubject = new Entity(lymEntity, world, 0f, 8f, 0.2f, "lymphocyte");
        lymphocyte = new Lymphocyte(lymSubject, hp);
        lymphocyte.getBody().setTransform(vec.x, vec.y, 0f);
        if (rand.nextInt(2) == 0) lymphocyte.getBody().setAngularVelocity(rand.nextFloat() + 0.3f);
        else lymphocyte.getBody().setAngularVelocity(-rand.nextFloat() - 0.3f);

        sizeX = lymphocyte.getWidth();
        sizeY = lymphocyte.getHeight();
        lymphocyte.setSize(0f, 0f);
        lymphocyte.addAction(Actions.alpha(0f));
        lymphocyte.addAction(Actions.parallel(
                Actions.sizeTo(sizeX, sizeY, 1f),
                Actions.alpha(1f, 1f)
        ));

        stage.addActor(lymphocyte);
        lymphocytes.addFirst(lymphocyte);
        //
        lymEntity = new AdvSprite(game.atlas.createSprite("green"), 0, 0, 1f, 1f);
        lymSubject = new Entity(lymEntity, world, 0f, 8f, 0.2f, "lymphocyte");
        lymphocyte = new Lymphocyte(lymSubject, hp);
        lymphocyte.getBody().setTransform(vec.x, vec.y, 0f);
        if (rand.nextInt(2) == 0) lymphocyte.getBody().setAngularVelocity(rand.nextFloat() + 0.3f);
        else lymphocyte.getBody().setAngularVelocity(-rand.nextFloat() - 0.3f);

        sizeX = lymphocyte.getWidth();
        sizeY = lymphocyte.getHeight();
        lymphocyte.setSize(0f, 0f);
        lymphocyte.addAction(Actions.alpha(0f));
        lymphocyte.addAction(Actions.parallel(
                Actions.sizeTo(sizeX, sizeY, 1f),
                Actions.alpha(1f, 1f)
        ));

        stage.addActor(lymphocyte);
        lymphocytes.addFirst(lymphocyte);
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
        idleLevelTest();
        player.updateSprite(spec.get());
        lymphSearch();
        Iterator<Entity> v1 = viruses.iterator();
        Iterator<Entity> v2 = virusesAdv.iterator();
        Iterator<Entity> v3 = virusesSupAdv.iterator();
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
        while (v2.hasNext()) {
            Entity v = v2.next();
            v.updateSprite(spec.get());
            v.updateLife();
            v.decreaseLT();
            if (v.getLifeTime() <= 0) {
                if (v.isAlive()) {
                    v.kill();
                }
                if (!v.hasActions()){
                    v.getWorld().destroyBody(v.getBody());
                    v2.remove();
                }
            }
        }
        while (v3.hasNext()) {
            Entity v = v3.next();
            v.updateSprite(spec.get());
            v.updateLife();
            v.decreaseLT();
            if (v.getLifeTime() <= 0) {
                if (v.isAlive()) {
                    v.kill();
                }
                if (!v.hasActions()){
                    v.getWorld().destroyBody(v.getBody());
                    v3.remove();
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
                Iterator<Entity> virusAdvI = virusesAdv.iterator();
                Iterator<Entity> virusSupAdvI = virusesSupAdv.iterator();
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
                while (virusAdvI.hasNext()) {
                    Entity virusAdv = virusAdvI.next();
                    float tempX = (virusAdv.getPosition().x - lymp.getPosition().x);
                    float tempY = (virusAdv.getPosition().y - lymp.getPosition().y);
                    double distance = Math.sqrt((double)(tempX * tempX + tempY * tempY));
                    if (distance < minDist) {
                        minDist = distance;
                        x = virusAdv.getPosition().x;
                        y = virusAdv.getPosition().y;
                    }
                }
                while (virusSupAdvI.hasNext()) {
                    Entity virusSupAdv = virusSupAdvI.next();
                    float tempX = (virusSupAdv.getPosition().x - lymp.getPosition().x);
                    float tempY = (virusSupAdv.getPosition().y - lymp.getPosition().y);
                    double distance = Math.sqrt((double)(tempX * tempX + tempY * tempY));
                    if (distance < minDist) {
                        minDist = distance;
                        x = virusSupAdv.getPosition().x;
                        y = virusSupAdv.getPosition().y;
                    }
                }
                lymp.updateLife(x, y);
            }
        }
    }

    private void idleLevelTest () {
        if (rand.nextInt(100) == 0) spawnVirus(150, rand.nextFloat() / 2f + 0.3f, getRandSpawnLoc(), rand.nextInt(13) + 1);
        if (rand.nextInt(50) == 0) spawnRedCell(100, getRandSpawnLoc());
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
                "Entities: " + (viruses.size() + virusesAdv.size() + virusesSupAdv.size() + lymphocytes.size() + redCells.size() + 1),
                7.9f*meter - game.fonts.mediumB.getWidth("Entities: " + (viruses.size() + virusesAdv.size() + virusesSupAdv.size() + lymphocytes.size() + redCells.size() + 1)),
                spec.get().viewportHeight * meter - 0.1f*meter - 4.5f*game.fonts.mediumB.getHeight("A")
        );
        /*game.fonts.mediumB.draw(
                stage.getBatch(),
                "Time: " + (end - start) / 1000,
                7.9f*meter - game.fonts.mediumB.getWidth("Time: " + (end - start) / 1000),
                spec.get().viewportHeight * meter - 0.1f*meter - 6.0f*game.fonts.mediumB.getHeight("A")
        );*/
        game.fonts.mediumB.draw(
                stage.getBatch(),
                ": " + playerScore,
                0.1f*meter + 0.125f*game.width,
                map.getY() - 0.075f*game.width + 0.5f*game.fonts.mediumB.getHeight("A")
        );
        game.fonts.mediumB.draw(
                stage.getBatch(),
                ": " + lymphScore,
                0.1f*meter + 0.125f*game.width,
                map.getY() - 0.2f*game.width + 0.5f*game.fonts.mediumB.getHeight("A")
        );
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
