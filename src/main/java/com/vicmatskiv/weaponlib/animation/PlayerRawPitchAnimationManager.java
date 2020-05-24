package com.vicmatskiv.weaponlib.animation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.vicmatskiv.weaponlib.PlayerWeaponInstance;
import com.vicmatskiv.weaponlib.RenderableState;
import com.vicmatskiv.weaponlib.Weapon;
import com.vicmatskiv.weaponlib.Weapon.ScreenShaking;

import net.minecraft.entity.player.EntityPlayer;

public class PlayerRawPitchAnimationManager {
    
    public static enum State { 
        SHOOTING(0, 0.1f), RELOADING(-5, 0f), AIMING(-10, 0f), DEFAULT(Integer.MIN_VALUE, 0f);
        
        private int priority;
        private float stepAdjustement;
        State(int priority, float stepAdjustement) {
            this.priority = priority;
        }
        
        int getPriority() {
            return priority;
        }
        
        public float getStepAdjustement() {
            return stepAdjustement;
        }
    }
    
    private static class Key {
        UUID playerId;
        State state;
        Weapon weapon;
        
        public Key(EntityPlayer player, State state, Weapon weapon) {
            this.playerId = player.getPersistentID();
            this.state = state;
            this.weapon = weapon;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((playerId == null) ? 0 : playerId.hashCode());
            result = prime * result + ((state == null) ? 0 : state.hashCode());
            result = prime * result + ((weapon == null) ? 0 : weapon.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Key other = (Key) obj;
            if (playerId == null) {
                if (other.playerId != null)
                    return false;
            } else if (!playerId.equals(other.playerId))
                return false;
            if (state != other.state)
                return false;
            if (weapon == null) {
                if (other.weapon != null)
                    return false;
            } else if (!weapon.equals(other.weapon))
                return false;
            return true;
        }
        
        
    }

    private Map<Key, PlayerAnimation> allPlayerAnimations = new HashMap<>();
    private Map<EntityPlayer, PlayerAnimation> activeAnimations = new HashMap<>();
    private float maxYaw = 2f;
    private float maxPitch = 2f;
    private long transitionDuration = 2000;

    public PlayerRawPitchAnimationManager setMaxYaw(float maxYaw) {
        this.maxYaw = maxYaw;
        return this;
    }

    public PlayerRawPitchAnimationManager setMaxPitch(float maxPitch) {
        this.maxPitch = maxPitch;
        return this;
    }

    public PlayerRawPitchAnimationManager setTransitionDuration(long transitionDuration) {
        this.transitionDuration = transitionDuration;
        return this;
    }
    
    public void update(EntityPlayer player, PlayerWeaponInstance weaponInstance, RenderableState weaponState) {
        State targetState = toManagedState(weaponState);
        PlayerAnimation activeAnimation = activeAnimations.get(player);
        activeAnimations.clear();
        boolean fadeOut = true;
        if(activeAnimation == null) {
            activeAnimation = getAnimationForManagedState(player, weaponInstance, targetState);
            activeAnimations.put(player, activeAnimation);
        } else {
            State currentState = activeAnimation.getState();
//            System.out.println("Current state: " + currentState);
            if(currentState == targetState) {
                activeAnimation.reset(player, false);
            } else if(currentState.getPriority() < targetState.getPriority() || activeAnimation.isCompleted()) {
                activeAnimation = getAnimationForManagedState(player, weaponInstance, targetState);
                activeAnimation.reset(player, true);
                activeAnimations.put(player, activeAnimation);
            }
        }
        
        activeAnimation.update(player, fadeOut);
    }

    public void reset(EntityPlayer player, RenderableState weaponState) {
//        PlayerAnimation activeAnimation = getActiveAnimation(player, weaponState);
//        activeAnimation.reset(player);
    }
    
    private State toManagedState(RenderableState weaponState) {
        if(weaponState == null) {
            return State.DEFAULT;
        }
        State managedState;
        switch(weaponState) {
        case SHOOTING: case RECOILED: case ZOOMING_SHOOTING: case ZOOMING_RECOILED:
            managedState = State.SHOOTING;
            break;
        case RELOADING:
            managedState = State.RELOADING;
            break;
        case ZOOMING:
            managedState = State.AIMING;
            break;
        default:
            managedState = State.DEFAULT;
        }
        return managedState;
    }
    
    private PlayerAnimation createAnimationForManagedState(EntityPlayer player, State managedState, Weapon weapon) {
        PlayerAnimation animation;
        switch(managedState) {
        case AIMING:
            animation = new PlayerRawPitchAnimation(managedState)
                    .setMaxPitch(maxPitch)
                    .setMaxYaw(maxYaw)
                    .setPlayer(player)
                    .setTransitionDuration(transitionDuration);
            break;
        case SHOOTING:
            ScreenShaking weaponScreenShaking = weapon.getScreenShaking(RenderableState.SHOOTING);
            animation = new ScreenShakeAnimation.Builder()
                    .withState(managedState)
                    .withRotationAttenuation(0.65f)
                    .withTranslationAttenuation(0.05f)
                    .withZRotationCoefficient(weaponScreenShaking != null ? weaponScreenShaking.getZRotationCoefficient(): 2f)
                    .withTransitionDuration(50)
                    .build();
            break;
        case DEFAULT: default:
            animation = PlayerAnimation.NO_ANIMATION;
            break;
        }
        return animation;
    }

    private PlayerAnimation getAnimationForManagedState(EntityPlayer player, PlayerWeaponInstance instance, State managedState) {
        return allPlayerAnimations.computeIfAbsent(new Key(player, managedState, instance.getWeapon()), 
                k -> createAnimationForManagedState(player, k.state, instance.getWeapon()));
    }

}
