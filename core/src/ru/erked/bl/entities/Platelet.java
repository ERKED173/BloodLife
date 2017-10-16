package ru.erked.bl.entities;

public class Platelet extends Entity {

    public Platelet(Entity sub, int lifeTime) {
        super(sub, sub.getWorld(), sub.friction, sub.density, sub.restitution, sub.getName());
        sub.getWorld().destroyBody(sub.getBody());
        this.lifeTime = lifeTime;
    }

    @Override
    public void updateLife() {
        if (rand.nextInt(15) == 0) {
            float x = 0.5f * meter;
            float y = 0.5f * meter;
            if (rand.nextInt(2) == 0) x = -x;
            if (rand.nextInt(2) == 0) y = -y;
            applyForceToTheCentre(
                    x,
                    y,
                    true
            );
        }
    }

    @Override
    public void updateLife(float x, float y) {
        if (rand.nextInt(10) == 0) {
            float velX = ((x - getPosition().x) * meter) % 0.5f * meter;
            float velY = ((y - getPosition().y) * meter) % 0.5f * meter;
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
