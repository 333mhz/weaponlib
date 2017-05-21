package com.vicmatskiv.weaponlib.particle;

import com.vicmatskiv.weaponlib.compatibility.CompatibleMessage;

import io.netty.buffer.ByteBuf;

public class SpawnParticleMessage implements CompatibleMessage {

    public enum ParticleType { BLOOD, SHELL, SMOKE_GRENADE_SMOKE }

    private double posX;
    private double posY;
    private double posZ;
    private double motionX;
    private double motionY;
    private double motionZ;
    private int count;
    private ParticleType particleType;

    public SpawnParticleMessage() {}

    public SpawnParticleMessage(ParticleType particleType, int count, double posX, double posY, double posZ) {
        this.particleType = particleType;
        this.count = count;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
    }

    public SpawnParticleMessage(ParticleType particleType, int count, double posX, double posY, double posZ,
            double motionX, double motionY, double motionZ) {
        this.particleType = particleType;
        this.count = count;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.motionX = motionX;
        this.motionY = motionY;
        this.motionZ = motionZ;
    }

    public void fromBytes(ByteBuf buf) {
        particleType = ParticleType.values()[buf.readInt()];
        count = buf.readInt();
        posX = buf.readDouble();
        posY = buf.readDouble();
        posZ = buf.readDouble();
        if(particleType == ParticleType.SMOKE_GRENADE_SMOKE) {
            motionX = buf.readDouble();
            motionY = buf.readDouble();
            motionZ = buf.readDouble();
        }
    }

    public void toBytes(ByteBuf buf) {
        buf.writeInt(particleType.ordinal());
        buf.writeInt(count);
        buf.writeDouble(posX);
        buf.writeDouble(posY);
        buf.writeDouble(posZ);
        if(particleType == ParticleType.SMOKE_GRENADE_SMOKE) {
            buf.writeDouble(motionX);
            buf.writeDouble(motionY);
            buf.writeDouble(motionZ);
        }
    }

    public ParticleType getParticleType() {
        return particleType;
    }

    public double getPosX() {
        return posX;
    }

    public double getPosY() {
        return posY;
    }

    public double getPosZ() {
        return posZ;
    }

    public int getCount() {
        return count;
    }

    public double getMotionX() {
        return motionX;
    }

    public double getMotionY() {
       return motionY;
    }

    public double getMotionZ() {
        return motionZ;
    }
}
