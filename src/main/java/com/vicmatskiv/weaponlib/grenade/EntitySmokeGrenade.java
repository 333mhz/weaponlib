package com.vicmatskiv.weaponlib.grenade;

import static com.vicmatskiv.weaponlib.compatibility.CompatibilityProvider.compatibility;

import com.vicmatskiv.weaponlib.ModContext;
import com.vicmatskiv.weaponlib.compatibility.CompatibleRayTraceResult;
import com.vicmatskiv.weaponlib.particle.SpawnParticleMessage;
import com.vicmatskiv.weaponlib.particle.SpawnParticleMessage.ParticleType;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

public class EntitySmokeGrenade extends AbstractEntityGrenade {

    private long activationDelay;
    private long activationTimestamp;

    private float smokeAmount;
    private long activeDuration;

    public static class Builder {

        private long activationDelay;
        private long activationTimestamp;
        private long activeDuration;
        private float smokeAmount;
        private EntityLivingBase thrower;
        private ItemGrenade itemGrenade;
        private float velocity = ItemGrenade.DEFAULT_VELOCITY;
        private float gravityVelocity = ItemGrenade.DEFAULT_GRAVITY_VELOCITY;
        private float rotationSlowdownFactor = ItemGrenade.DEFAULT_ROTATION_SLOWDOWN_FACTOR;

        public Builder withActivationTimestamp(long activationTimestamp) {
            this.activationTimestamp = activationTimestamp;
            return this;
        }

        public Builder withActivationDelay(long activationDelay) {
            this.activationDelay = activationDelay;
            return this;
        }

        public Builder withThrower(EntityLivingBase thrower) {
            this.thrower = thrower;
            return this;
        }

        public Builder withSmokeAmount(float smokeAmount) {
            this.smokeAmount = smokeAmount;
            return this;
        }

        public Builder withGrenade(ItemGrenade itemGrenade) {
            this.itemGrenade = itemGrenade;
            return this;
        }

        public Builder withVelocity(float velocity) {
            this.velocity = velocity;
            return this;
        }

        public Builder withGravityVelocity(float gravityVelocity) {
            this.gravityVelocity = gravityVelocity;
            return this;
        }

        public Builder withRotationSlowdownFactor(float rotationSlowdownFactor) {
            this.rotationSlowdownFactor = rotationSlowdownFactor;
            return this;
        }

        public Builder withActiveDuration(long activeDuration) {
            this.activeDuration = activeDuration;
            return this;
        }

        public EntitySmokeGrenade build(ModContext modContext) {
            EntitySmokeGrenade entityGrenade = new EntitySmokeGrenade(modContext, itemGrenade, thrower, velocity,
                    gravityVelocity, rotationSlowdownFactor);
            entityGrenade.activationTimestamp = activationTimestamp;
            entityGrenade.activationDelay = activationDelay;
            entityGrenade.smokeAmount = smokeAmount;
            entityGrenade.activeDuration = activeDuration;
            return entityGrenade;
        }
    }

    private EntitySmokeGrenade(ModContext modContext, ItemGrenade itemGrenade, EntityLivingBase thrower, float velocity, float gravityVelocity, float rotationSlowdownFactor) {
        super(modContext, itemGrenade, thrower, velocity, gravityVelocity, rotationSlowdownFactor);
    }

    public EntitySmokeGrenade(World world) {
        super(world);
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        super.writeSpawnData(buffer);
        buffer.writeLong(activationTimestamp);
        buffer.writeLong(activationDelay);
        buffer.writeLong(activeDuration);
        buffer.writeFloat(smokeAmount);
    }

    @Override
    public void readSpawnData(ByteBuf buffer) {
        super.readSpawnData(buffer);
        activationTimestamp = buffer.readLong();
        activationDelay = buffer.readLong();
        activeDuration = buffer.readLong();
        smokeAmount = buffer.readFloat();
    }

    @Override
    public void onGrenadeUpdate() {

        long timeRemaining = activationTimestamp + activationDelay + activeDuration - System.currentTimeMillis();
        if(activationDelay == ItemGrenade.EXPLODE_ON_IMPACT) {
            // Do nothing
        } else if (timeRemaining < 0) {
            setDead();
        } else if(!compatibility.world(this).isRemote && timeRemaining <= activeDuration ) {

            double f = 0.4 + Math.sin(Math.PI * (1 - (double)timeRemaining / activeDuration)) * 0.3;
            if(rand.nextDouble() <= f) {
                for (Object o : compatibility.world(this).playerEntities) {
                    EntityPlayer player = (EntityPlayer) o;
                    if (player.getDistanceSq(posX, posY, posZ) < 4096.0D) {
                        ParticleType particleType = ParticleType.SMOKE_GRENADE_SMOKE;
                        double movement = bounceCount > 0 ? 0.007 : 0.001;
                        modContext.getChannel().getChannel().sendTo(
                                new SpawnParticleMessage(particleType, 1,
                                        posX + rand.nextGaussian() / 7,
                                        posY + rand.nextGaussian() / 10,
                                        posZ + rand.nextGaussian() / 7,
                                        rand.nextGaussian() * movement,
                                        rand.nextGaussian() * movement,
                                        rand.nextGaussian() * movement),
                                    (EntityPlayerMP) player);
                    }
                }
            }
        }
    }

    @Override
    public void onBounce(CompatibleRayTraceResult movingobjectposition) {
        if(activationDelay == ItemGrenade.EXPLODE_ON_IMPACT) {
            activationDelay = 0;
            activationTimestamp = System.currentTimeMillis();
        } else {
            super.onBounce(movingobjectposition);
        }
    }

    @Override
    public void onStop() {
        World world = compatibility.world(this);
        if(!world.isRemote) {
            compatibility.playSound(compatibility.world(this), posX, posY, posZ, itemGrenade.getStopAfterThrowingSound(), 2f,
                    (1.0F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F) * 0.7f);
        }

    }
}
