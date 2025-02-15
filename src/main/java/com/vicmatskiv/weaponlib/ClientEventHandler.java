package com.vicmatskiv.weaponlib;

import static com.vicmatskiv.weaponlib.compatibility.CompatibilityProvider.compatibility;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vicmatskiv.weaponlib.compatibility.CompatibleClientEventHandler;
import com.vicmatskiv.weaponlib.compatibility.CompatibleClientTickEvent;
import com.vicmatskiv.weaponlib.compatibility.CompatibleClientTickEvent.Phase;
import com.vicmatskiv.weaponlib.compatibility.CompatibleExposureCapability;
import com.vicmatskiv.weaponlib.compatibility.CompatibleExtraEntityFlags;
import com.vicmatskiv.weaponlib.compatibility.CompatibleRenderHandEvent;
import com.vicmatskiv.weaponlib.compatibility.CompatibleRenderPlayerPreEvent;
import com.vicmatskiv.weaponlib.compatibility.CompatibleRenderTickEvent;
import com.vicmatskiv.weaponlib.perspective.Perspective;
import com.vicmatskiv.weaponlib.shader.DynamicShaderContext;
import com.vicmatskiv.weaponlib.shader.DynamicShaderGroupManager;
import com.vicmatskiv.weaponlib.shader.DynamicShaderGroupSource;
import com.vicmatskiv.weaponlib.shader.DynamicShaderPhase;
import com.vicmatskiv.weaponlib.tracking.PlayerEntityTracker;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;

public class ClientEventHandler extends CompatibleClientEventHandler {

	private static final int DEFAULT_RECONCILE_TIMEOUT_MILLIS = 5000;

    private static final float SLOW_DOWN_WHEN_POISONED_DOSE_THRESHOLD = 0.4f;
	
    private static final UUID SLOW_DOWN_WHILE_ZOOMING_ATTRIBUTE_MODIFIER_UUID = UUID.fromString("8efa8469-0256-4f8e-bdd9-3e7b23970663");
	private static final AttributeModifier SLOW_DOWN_WHILE_ZOOMING_ATTRIBUTE_MODIFIER = (new AttributeModifier(SLOW_DOWN_WHILE_ZOOMING_ATTRIBUTE_MODIFIER_UUID, "Slow Down While Zooming", -0.3, 2)).setSaved(false);
    
	private static final UUID SLOW_DOWN_WHILE_PRONING_ATTRIBUTE_MODIFIER_UUID = UUID.fromString("a3fa1751-953d-4b6c-955b-6824a193d271");
	private static final AttributeModifier SLOW_DOWN_WHILE_PRONING_ATTRIBUTE_MODIFIER = (new AttributeModifier(SLOW_DOWN_WHILE_PRONING_ATTRIBUTE_MODIFIER_UUID, "Slow Down While Proning", -0.7, 2)).setSaved(false);

	private static final UUID SLOW_DOWN_WHILE_POISONED_ATTRIBUTE_MODIFIER_UUID = UUID.fromString("9d2eac95-9b9c-4942-8287-7952c6de353e");
    private static final AttributeModifier SLOW_DOWN_WHILE_POISONED_ATTRIBUTE_MODIFIER = (new AttributeModifier(SLOW_DOWN_WHILE_POISONED_ATTRIBUTE_MODIFIER_UUID, "Slow Down While Poisoned", -0.7, 2)).setSaved(false);

	@SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger(ClientEventHandler.class);

	private Lock mainLoopLock = new ReentrantLock();
	private SafeGlobals safeGlobals;
	private Queue<Runnable> runInClientThreadQueue;

	private ClientModContext modContext;
    private DynamicShaderGroupManager shaderGroupManager;
    private PipelineShaderGroupSourceProvider pipelineShaderGroupSourceProvider = new PipelineShaderGroupSourceProvider();

    private int currentSlotIndex;

	public ClientEventHandler(ClientModContext modContext, Lock mainLoopLock, SafeGlobals safeGlobals,
			Queue<Runnable> runInClientThreadQueue /*, ReloadAspect reloadAspect*/) {
		this.modContext = modContext;
		this.mainLoopLock = mainLoopLock;
		this.safeGlobals = safeGlobals;
		this.runInClientThreadQueue = runInClientThreadQueue;
        this.shaderGroupManager = new DynamicShaderGroupManager();
        //this.reloadAspect = reloadAspect;
	}

	public void onCompatibleClientTick(CompatibleClientTickEvent event) {
		if(event.getPhase() == Phase.START) {
			mainLoopLock.lock();
			updateOnStartTick();
		} else if(event.getPhase() == Phase.END) {
			update();
			modContext.getSyncManager().run();

			PlayerEntityTracker tracker = PlayerEntityTracker.getTracker(compatibility.clientPlayer());
			if(tracker != null) {
			    tracker.update();
			}
			
			mainLoopLock.unlock();
			processRunInClientThreadQueue();
			safeGlobals.objectMouseOver.set(compatibility.getObjectMouseOver());
			if(compatibility.clientPlayer() != null) {
				safeGlobals.currentItemIndex.set(compatibility.clientPlayer().inventory.currentItem);
				//reloadAspect.updateMainHeldItem(compatibility.clientPlayer());
			}
		}
	}
	
	private void updateOnStartTick() {
	    EntityPlayer player = compatibility.clientPlayer();
	    if(player != null) {
	        int newSlotIndex = compatibility.getCurrentInventoryItemIndex((EntityPlayer) player);
	        if(currentSlotIndex != newSlotIndex) {
	            //modContext.getWeaponReloadAspect().updateMainHeldItem(player);
	            currentSlotIndex = newSlotIndex;
//	            System.out.println("Changed active slot to " + newSlotIndex);
	            modContext.getWeaponReloadAspect().drawMainHeldItem(player);
	        }
	    }
	}

    private void update() {
		EntityPlayer player = compatibility.clientPlayer();
		modContext.getPlayerItemInstanceRegistry().update(player);
		PlayerWeaponInstance mainHandHeldWeaponInstance = modContext.getMainHeldWeapon();
		if(player != null) {
		    if(isProning(player)) {
	            slowPlayerDown(player, SLOW_DOWN_WHILE_PRONING_ATTRIBUTE_MODIFIER);
	        } else {
	            restorePlayerSpeed(player, SLOW_DOWN_WHILE_PRONING_ATTRIBUTE_MODIFIER);
	        }
		}
		
		if(mainHandHeldWeaponInstance != null) {
			if(player.isSprinting()) {
				mainHandHeldWeaponInstance.setAimed(false);
			}
			if(mainHandHeldWeaponInstance.isAimed()) {
				slowPlayerDown(player, SLOW_DOWN_WHILE_ZOOMING_ATTRIBUTE_MODIFIER);
			} else {
				restorePlayerSpeed(player, SLOW_DOWN_WHILE_ZOOMING_ATTRIBUTE_MODIFIER);
			}
			
			if(mainHandHeldWeaponInstance != null
			        && mainHandHeldWeaponInstance.getState() == WeaponState.READY
			        && mainHandHeldWeaponInstance.getStateUpdateTimestamp() + DEFAULT_RECONCILE_TIMEOUT_MILLIS < System.currentTimeMillis()
			        && mainHandHeldWeaponInstance.getSyncStartTimestamp() == 0
			        && mainHandHeldWeaponInstance.getUpdateTimestamp() + DEFAULT_RECONCILE_TIMEOUT_MILLIS < System.currentTimeMillis()) {
			    mainHandHeldWeaponInstance.reconcile();
			}
		} else if(player != null){
			restorePlayerSpeed(player, SLOW_DOWN_WHILE_ZOOMING_ATTRIBUTE_MODIFIER);
		}
		
		if(player != null) {
		    //ItemStack helmet = compatibility.getHelmet();
		    
		    SpreadableExposure spreadableExposure = CompatibleExposureCapability.getExposure(compatibility.clientPlayer(), SpreadableExposure.class);
	        if(spreadableExposure != null && spreadableExposure.getTotalDose() > SLOW_DOWN_WHEN_POISONED_DOSE_THRESHOLD) {
	            slowPlayerDown(player, SLOW_DOWN_WHILE_POISONED_ATTRIBUTE_MODIFIER);
	        } else {
	            restorePlayerSpeed(player, SLOW_DOWN_WHILE_POISONED_ATTRIBUTE_MODIFIER);
	        }
	        
	        LightExposure lightExposure = CompatibleExposureCapability.getExposure(compatibility.clientPlayer(), LightExposure.class);
	        if(lightExposure != null) {
	            lightExposure.update(compatibility.clientPlayer());
	        }
		}
	}
    
    private static boolean isProning(EntityPlayer player) {
        return (CompatibleExtraEntityFlags.getFlags(player) & CompatibleExtraEntityFlags.PRONING) != 0;
    }

	// TODO: create player utils, move this method
	private void restorePlayerSpeed(EntityPlayer entityPlayer, AttributeModifier modifier) {
		if(entityPlayer.getEntityAttribute(compatibility.getMovementSpeedAttribute())
				.getModifier(modifier.getID()) != null) {
			entityPlayer.getEntityAttribute(compatibility.getMovementSpeedAttribute())
				.removeModifier(modifier);
		}
	}

	// TODO: create player utils, move this method
	private void slowPlayerDown(EntityPlayer entityPlayer, AttributeModifier modifier) {
		if(entityPlayer.getEntityAttribute(compatibility.getMovementSpeedAttribute())
				.getModifier(modifier.getID()) == null) {
			entityPlayer.getEntityAttribute(compatibility.getMovementSpeedAttribute())
				.applyModifier(modifier);
		}
	}

	private void processRunInClientThreadQueue() {
		Runnable r;
		while((r = runInClientThreadQueue.poll()) != null) {
			r.run();
		}
	}

	@Override
	public void onCompatibleRenderHand(CompatibleRenderHandEvent event) {
	    
	    Minecraft minecraft = Minecraft.getMinecraft();
	    if(minecraft.gameSettings.thirdPersonView == 0 && !compatibility.isShadersModEnabled()) {
	        PlayerWeaponInstance weaponInstance = modContext.getMainHeldWeapon();
	        
	        DynamicShaderContext shaderContext = new DynamicShaderContext(DynamicShaderPhase.PRE_ITEM_RENDER,
	                null,
	                minecraft.getFramebuffer(),
	                event.getPartialTicks())
	                .withProperty("weaponInstance", weaponInstance);
	        shaderGroupManager.applyShader(shaderContext, weaponInstance);
	    }
	}

	@Override
    protected void onCompatibleRenderTickEvent(CompatibleRenderTickEvent event) {

        Minecraft minecraft = Minecraft.getMinecraft();
        DynamicShaderContext shaderContext = new DynamicShaderContext(DynamicShaderPhase.POST_WORLD_RENDER,
                minecraft.entityRenderer,
                minecraft.getFramebuffer(), event.getRenderTickTime());

        EntityPlayer clientPlayer = compatibility.clientPlayer();
        
        if(event.getPhase() == CompatibleRenderTickEvent.Phase.START ) {
            ClientModContext.currentContext.set(modContext);
            mainLoopLock.lock();
            if(clientPlayer != null) {
                PlayerItemInstance<?> instance = modContext.getPlayerItemInstanceRegistry()
                        .getMainHandItemInstance(clientPlayer);

                //if(minecraft.gameSettings.thirdPersonView == 0) {
                    DynamicShaderGroupSource source = pipelineShaderGroupSourceProvider.getShaderSource(shaderContext.getPhase());
                    if(source != null) {
                        shaderGroupManager.loadFromSource(shaderContext, source);
//                        shaderGroupManager.removeAllShaders(shaderContext);
                    }
                //}

                if(instance != null) {
                    Perspective<?> view = modContext.getViewManager().getPerspective(instance, true);
                    if(view != null) {
                        view.update(event);
                    }
                }
            }

        } else if(event.getPhase() == CompatibleRenderTickEvent.Phase.END) {
            safeGlobals.renderingPhase.set(null);
            shaderGroupManager.removeStaleShaders(shaderContext);
            mainLoopLock.unlock();
            ClientModContext.currentContext.remove();
        }
    }

    @Override
    protected ModContext getModContext() {
        return modContext;
    }

    @Override
    protected void onCompatibleRenderPlayerPreEvent(CompatibleRenderPlayerPreEvent event) {
        
//        CustomPlayerInventory capability = CompatibleCustomPlayerInventoryCapability.getInventory(event.getPlayer());
//        
//        if(capability != null) {
//            ItemStack vestStack = capability.getStackInSlot(1); 
//            if(vestStack != null) {
//                compatibility.renderItem(event.getPlayer(), vestStack);
////                IItemRenderer customRenderer = MinecraftForgeClient.getItemRenderer(vestStack, null);
////                if(customRenderer instanceof StaticModelSourceRenderer) {
////                    ((StaticModelSourceRenderer) customRenderer).renderCustomEquipped(event.getPlayer(), vestStack);
////                }
//            }
//            ItemStack backpackStack = capability.getStackInSlot(0); // TODO: replace 0 with constant for backpack slot 
//            if(backpackStack != null) {
////                IItemRenderer customRenderer = MinecraftForgeClient.getItemRenderer(backpackStack, null);
////                if(customRenderer instanceof StaticModelSourceRenderer) {
////                    ((StaticModelSourceRenderer) customRenderer).renderCustomEquipped(event.getPlayer(), backpackStack);
////                }
//                compatibility.renderItem(event.getPlayer(), backpackStack);
//            }
//        }
    }
}
