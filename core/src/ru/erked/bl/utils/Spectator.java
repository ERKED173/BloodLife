package ru.erked.bl.utils;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class Spectator extends Actor {

    private OrthographicCamera camera;

    public Spectator (float worldWidth, float worldHeight) {
        setSize(worldWidth, worldHeight);
        camera = new OrthographicCamera(getWidth(), getHeight());
    }

    public void update () {
        camera.position.x = getX();
        camera.position.y = getY();
        camera.viewportWidth = getWidth();
        camera.viewportHeight = getHeight();
        camera.update();
    }

    public OrthographicCamera get () {
        return camera;
    }

}
