package ru.erked.bl.utils;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.Actor;

import ru.erked.bl.screens.Technical;

public class AdvSprite extends Actor {

    public Sprite sprite;
    public float meter = Technical.METER;

    public AdvSprite (Sprite sprite, float x, float y, float width, float height) {
        setX(x);
        setY(y);
        setWidth(width);
        setHeight(height);
        setOrigin(width / 2f, height / 2f);
        this.sprite = sprite;
        this.sprite.setBounds(x, y, width, height);
        this.sprite.setOriginCenter();
    }

    @Override
    public void draw (Batch batch, float parentAlpha) {
        sprite.draw(batch, parentAlpha);
    }

    public void updateSprite () {
        sprite.setBounds(getX(), getY(), getWidth(), getHeight());
        sprite.setRotation(getRotation());
        sprite.setColor(getColor());
    }

    public void updateSprite (Camera camera) {
        sprite.setBounds(
                getX() - camera.position.x*meter + 0.5f*camera.viewportWidth*meter,
                getY() - camera.position.y*meter + 0.5f*camera.viewportHeight*meter,
                getWidth(),
                getHeight()
        );
        sprite.setRotation(getRotation());
        sprite.setColor(getColor());
    }

    public Sprite getSprite () {
        return sprite;
    }

}
