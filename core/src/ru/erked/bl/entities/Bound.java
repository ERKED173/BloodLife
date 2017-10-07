package ru.erked.bl.entities;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

import ru.erked.bl.utils.AdvSprite;

public class Bound extends AdvSprite {

    private Body body;
    private World world;

    public Bound(AdvSprite spr, World world) {
        super(spr.getSprite(), spr.getX(), spr.getY(), spr.getWidth(), spr.getHeight());
        this.world = world;

        BodyDef bDef = new BodyDef();
        bDef.type = BodyDef.BodyType.StaticBody;
        bDef.position.set(spr.getX(), spr.getY());
        body = world.createBody(bDef);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(getWidth() / 2f, getHeight() / 2f);

        FixtureDef fDef = new FixtureDef();
        fDef.shape = shape;
        body.createFixture(fDef);

        sprite.setSize(meter * spr.getWidth(), meter * spr.getHeight());
        sprite.setOriginCenter();
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
        setPosition(
                meter * body.getPosition().x - camera.position.x * meter - 0.5f * sprite.getWidth() + 0.5f*camera.viewportWidth * meter,
                meter * body.getPosition().y - camera.position.y * meter - 0.5f * sprite.getHeight() + 0.5f*camera.viewportHeight * meter
        );
        setRotation((float)Math.toDegrees(body.getAngle()));
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

}
