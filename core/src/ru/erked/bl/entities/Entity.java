package ru.erked.bl.entities;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;

import ru.erked.bl.utils.AdvSprite;

public class Entity extends AdvSprite {

    private Body body;
    private World world;
    float friction;
    float density;
    float restitution;
    private boolean isAlive = true;
    int lifeTime = 10;
    RandomXS128 rand;

    private Entity (AdvSprite spr, World world, float friction, float density, float restitution) {
        super(spr.getSprite(), spr.getX(), spr.getY(), spr.getWidth(), spr.getHeight());
        this.world = world;
        this.friction = friction;
        this.density = density;
        this.restitution = restitution;

        isAlive = true;

        BodyDef bDef = new BodyDef();
        bDef.type = BodyDef.BodyType.DynamicBody;
        bDef.position.set(spr.getX() + 0.5f, spr.getY() + 0.5f);
        body = world.createBody(bDef);

        CircleShape shape = new CircleShape();
        shape.setRadius(0.5f * spr.getWidth());

        FixtureDef fDef = new FixtureDef();
        fDef.shape = shape;
        fDef.friction = friction;
        fDef.density = density;
        fDef.restitution = restitution;
        body.createFixture(fDef);

        sprite.setSize(meter * spr.getWidth(), meter * spr.getHeight());
        sprite.setOriginCenter();

        rand = new RandomXS128();
    }

    public Entity (AdvSprite e, World world, float friction, float density, float restitution, String name) {
        this(e, world, friction, density, restitution);
        setName(name);
    }

    @Override
    public void draw (Batch batch, float parentAlpha) {
        sprite.setBounds(getX(), getY(), meter * getWidth(), meter * getHeight());
        sprite.setRotation(getRotation());
        sprite.setColor(getColor());
        sprite.draw(batch, parentAlpha);
    }

    @Override
    public void updateSprite () {
        setPosition(
                meter * body.getPosition().x - 0.5f * sprite.getWidth(),
                meter * body.getPosition().y - 0.5f * sprite.getHeight()
        );
        setRotation((float)Math.toDegrees(body.getAngle()));
    }

    @Override
    public void updateSprite (Camera camera) {
        float x = meter * body.getPosition().x - camera.position.x * meter - 0.5f * sprite.getWidth() + 0.5f*camera.viewportWidth * meter;
        float y = meter * body.getPosition().y - camera.position.y * meter - 0.5f * sprite.getHeight() + 0.5f*camera.viewportHeight * meter;
        setPosition(x, y);
        setRotation((float)Math.toDegrees(body.getAngle()));
    }

    public void applyLinearImpulse (float xImp, float yImp, float xPoint, float yPoint, boolean wake) {
        body.applyLinearImpulse(new Vector2(xImp, yImp), new Vector2(xPoint, yPoint), wake);
    }

    public void applyForceToTheCentre (float x, float y, boolean wake) {
        body.applyForceToCenter(new Vector2(x, y), wake);
    }

    public Vector2 getPosition () {
        return body.getPosition();
    }

    public Body getBody (){
        return body;
    }

    public World getWorld () {
        return world;
    }

    public void kill () {
        isAlive = false;
        addAction(Actions.sequence(
                Actions.parallel(
                    Actions.alpha(0f, 1f),
                    Actions.sizeTo(0f, 0f, 1f)
                ),
                Actions.removeActor()
        ));
    }

    public boolean isAlive () {
        return isAlive;
    }

    public int getLifeTime () {
        return lifeTime;
    }

    public void setLT (int lifeTime) {
        this.lifeTime = lifeTime;
    }

    public void decreaseLT () {
        if (rand.nextInt(10) == 0) {
            if (lifeTime - 1 > 0) {
                lifeTime -= 1;
            } else {
                lifeTime = 0;
            }
        }
    }

    public void decreaseLT (int time) {
        if (lifeTime - time > 0) {
            lifeTime -= time;
        } else {
            lifeTime = 0;
        }
    }

    public void updateLife () {}
    public void updateLife (float x, float y) {
        if (rand.nextInt(5) == 0) {
            float velX = ((x - getPosition().x) * meter) * 2f;
            float velY = ((y - getPosition().y) * meter) * 2f;
            if (Math.abs(velX) < meter) if (velX > 0f) velX = meter; else velX = -meter;
            if (Math.abs(velY) < meter) if (velY > 0f) velY = meter; else velY = -meter;
            applyForceToTheCentre(
                    velX,
                    velY,
                    true
            );
        }
    }
}
