package ru.erked.bl.entities;

public class RedCell extends Entity {

    public RedCell (Entity sub, int lifeTime) {
        super(sub, sub.getWorld(), sub.friction, sub.density, sub.restitution, sub.getName());
        sub.getWorld().destroyBody(sub.getBody());
        this.lifeTime = lifeTime;
    }

    @Override
    public void updateLife() {
        if (rand.nextInt(15) == 0) {
            float x = 3f * meter;
            float y = 3f * meter;
            if (rand.nextInt(2) == 0) x = -x;
            if (rand.nextInt(2) == 0) y = -y;
            applyForceToTheCentre(
                    x,
                    y,
                    true
            );
        }
    }

}
