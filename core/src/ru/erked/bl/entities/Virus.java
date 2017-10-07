package ru.erked.bl.entities;

public class Virus extends Entity {

    private float speed;

    public Virus (Entity sub, int lifeTime, float speed) {
        super(sub, sub.getWorld(), sub.friction, sub.density, sub.restitution, sub.getName());
        sub.getWorld().destroyBody(sub.getBody());
        this.lifeTime = lifeTime;
        this.speed = speed;
    }

    @Override
    public void updateLife() {
        if (rand.nextInt(15) == 0) {
            float x = speed * meter;
            float y = speed * meter;
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
