package ru.erked.bl.entities;

public class Lymphocyte  extends Entity {

    public Lymphocyte (Entity sub, int lifeTime) {
        super(sub, sub.getWorld(), sub.friction, sub.density, sub.restitution, sub.getName());
        sub.getWorld().destroyBody(sub.getBody());
        this.lifeTime = lifeTime;
    }

    @Override
    public void updateLife(float x, float y) {
        if (rand.nextInt(5) == 0) {
            float velX = ((x - getPosition().x) * meter) % 3f * meter;
            float velY = ((y - getPosition().y) * meter) % 3f * meter;
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
