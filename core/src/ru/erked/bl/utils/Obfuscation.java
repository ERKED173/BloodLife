package ru.erked.bl.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class Obfuscation extends Actor {

    private Sprite sprite;
    private float alpha;
    private boolean isActive;

    public Obfuscation(Sprite sprite, boolean isActive){
        this.sprite = sprite;
        sprite.setBounds(0.0f, 0.0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        this.isActive = isActive;
        if(isActive) alpha = 1.0f;
        else alpha = 0.0f;
    }

    public void activate(float time, float delta){
        if(alpha + delta/time < 1.0f) {
            alpha += delta/time;
            sprite.setAlpha(alpha);
        } else {
            alpha = 1.0f;
            sprite.setAlpha(alpha);
            isActive = true;
        }
    }

    public void deactivate(float time, float delta){
        if(alpha - delta/time > 0.0f) {
            alpha -= delta/time;
            sprite.setAlpha(alpha);
        } else {
            alpha = 0.0f;
            sprite.setAlpha(alpha);
            isActive = false;
        }
    }

    public float getAlpha(){
        return alpha;
    }

    public boolean isActive(){
        return isActive;
    }

    @Override
    public void draw(Batch batch, float parentAlpha){
        sprite.draw(batch);
    }

}
