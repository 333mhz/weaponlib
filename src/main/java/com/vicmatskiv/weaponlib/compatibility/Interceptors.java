package com.vicmatskiv.weaponlib.compatibility;

import static com.vicmatskiv.weaponlib.compatibility.CompatibilityProvider.compatibility;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.core.lookup.Interpolator;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import com.vicmatskiv.weaponlib.OptimizedCubeList;
import com.vicmatskiv.weaponlib.ClientModContext;
import com.vicmatskiv.weaponlib.KeyBindings;
import com.vicmatskiv.weaponlib.Part;
import com.vicmatskiv.weaponlib.PlayerRenderer;
import com.vicmatskiv.weaponlib.PlayerWeaponInstance;
import com.vicmatskiv.weaponlib.RenderContext;
import com.vicmatskiv.weaponlib.RenderableState;
import com.vicmatskiv.weaponlib.SpreadableExposure;
import com.vicmatskiv.weaponlib.Weapon;
import com.vicmatskiv.weaponlib.animation.MatrixHelper;
import com.vicmatskiv.weaponlib.animation.MultipartRenderStateManager;
import com.vicmatskiv.weaponlib.animation.ScreenShakingAnimationManager;
import com.vicmatskiv.weaponlib.inventory.CustomPlayerInventory;
import com.vicmatskiv.weaponlib.vehicle.EntityVehicle;
import com.vicmatskiv.weaponlib.vehicle.RenderVehicle2;
import com.vicmatskiv.weaponlib.vehicle.VehicleSuspensionStrategy;
import com.vicmatskiv.weaponlib.vehicle.jimphysics.InterpolationKit;
import com.vicmatskiv.weaponlib.vehicle.jimphysics.stability.InertialStabilizer;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
//import net.minecraft.util.MathHelper;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;

public class Interceptors {
	
	public static InertialStabilizer thirdPersonCameraStabilizer = new InertialStabilizer(new Vec3d(1,1,1));
	
    
    public static boolean is3dRenderableItem(Item item) {
        return compatibility.is3dRenderable(item);
    }

    public static void setupCameraTransformAfterHurtCameraEffect(float partialTicks) {
       
    	PlayerWeaponInstance weaponInstance = getPlayerWeaponInstance();
        EntityPlayer player = compatibility.getClientPlayer();
    	
        
        if(player.isRiding() && player.getRidingEntity() instanceof EntityVehicle && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0) {
        	EntityVehicle vehicle = (EntityVehicle) player.getRidingEntity();
        	// DEBUG //
        	//GL11.glRotated(-vehicle.rotationPitch*0.1, 1, 0, 0);
        	//GL11.glRotated(vehicle.sideLean, 0.0, 0.0, 1.0);
        	
        	
        	//Minecraft.getMinecraft().setRenderViewEntity(vehicle);
        	
        	if(Minecraft.getMinecraft().gameSettings.thirdPersonView == 0) {
        		
        		//MatrixHelper.applyMatrix(RenderVehicle2.tm);
        		//vehicle.rotationPitch = 0;
        		
        		

        		player.rotationYaw = vehicle.rotationYaw;
        		player.prevRotationYaw = vehicle.prevRotationYaw;
        		
        		
        		player.rotationPitch = -vehicle.rotationPitch;
        		player.prevRotationPitch = -vehicle.prevRotationPitch;
        		
        		
        		
        		double dist = vehicle.rotationPitch;
        		
        		GL11.glTranslated(0.0, 0.0, -dist*0.025);
        		//GL11.glTranslated(0.0, Math.abs(0.8*(vehicle.rotationPitch/45)), 0.0);

        		
        		float muRoll = (float) ((1 - Math.cos(Minecraft.getMinecraft().getRenderPartialTicks() * Math.PI)) / 2f);
        		float roll = (vehicle.prevRotationRollH+vehicle.prevRotationRoll) + ((vehicle.rotationRoll+vehicle.rotationRollH)-(vehicle.prevRotationRoll+vehicle.prevRotationRollH))*muRoll;
        		GL11.glRotatef(-roll, 0.0f, 0.0f, 1.0f);
        		
        	}

        }
        
        
    	if(player.isRiding() && player.getRidingEntity() instanceof EntityVehicle && Minecraft.getMinecraft().gameSettings.thirdPersonView == 1) {
    		EntityVehicle vehicle = (EntityVehicle) player.getRidingEntity();
    		
    		/*
    		double cRY = player.rotationYaw-vehicle.rotationYaw;
    		double cPRY = player.rotationYaw-vehicle.rotationYaw;
    		if(cRY > 90) cRY = 0;
    		if(cPRY > 90) cPRY = 0;
    		
    		player.rotationYaw -= cRY*0.005;
    		player.prevRotationYaw -= cPRY*0.005;*/
    		/*
    		player.rotationYaw = vehicle.rotationYaw;
    		player.prevRotationYaw = vehicle.prevRotationYaw;*/
    		
    		
    		//Vec3d vcv = vehicle.getSolver().getVelocityVector().scale(0.1);
    		//GL11.glTranslated(vcv.x, vcv.y, vcv.z);
    		
    		//Minecraft.getMinecraft().gameSettings.fovSetting = (float) (70f + ((vehicle.getSolver().currentRPM)/500.0f) + (vehicle.getRealSpeed()/2));
    		Vec3d pV = player.getPositionVector();
    		//GL11.glTranslated(-pV.x, -pV.y, -pV.z);
    		
    		//thirdPersonCameraStabilizer.position = Vec3d.ZERO;
    		/*
    		if(thirdPersonCameraStabilizer.position.lengthVector() == 0.0) {
    			thirdPersonCameraStabilizer.setPosition(vehicle.getPositionVector());
    
    		} else {
    			thirdPersonCameraStabilizer.setPositionTarget(vehicle.getPositionVector());
    		}
    		
    		thirdPersonCameraStabilizer.tensor = vehicle.getSolver().getOreintationVector();
    		
    	
    		thirdPersonCameraStabilizer.rotationYaw = vehicle.rotationYaw;
    		thirdPersonCameraStabilizer.updateCameraTransforms();
    		*/
    		
    		//GL11.glTranslated(0.0, vehicle.getInterpolatedLiftOffset(), 0.0);
    		
    		/*
    		if(vehicle.rotationPitch > 5) {
    			Vec3d startLift = InterpolationKit.interpolatedEntityPosition(vehicle);
    			Vec3d endLift = startLift.subtract(new Vec3d(0, 10, 0).rotatePitch((float) Math.toRadians(vehicle.rotationPitch)).rotateYaw((float) Math.toRadians(-vehicle.rotationYaw)));
    			RayTraceResult rtr = vehicle.world.rayTraceBlocks(startLift, endLift, false, true, false);
    			if(rtr != null) {
    				GL11.glTranslated(0.0, rtr.hitVec.subtract(startLift).lengthVector(), 0.0);
    			}
    		}*/
    		
    		GL11.glTranslated(-0.525, 1.0 /*+ vehicle.getInterpolatedLiftOffset()/2*/, -4.0);
    		GL11.glTranslated(0.0, 0.5, -2.5);
    		
    		
    		
    		
    		/*
    		float muRoll = (float) ((1 - Math.cos(Minecraft.getMinecraft().getRenderPartialTicks() * Math.PI)) / 2f);
    		float roll = (vehicle.prevRotationRollH+vehicle.prevRotationRoll) + ((vehicle.rotationRoll+vehicle.rotationRollH)-(vehicle.prevRotationRoll+vehicle.prevRotationRollH))*muRoll;
    		
    		GL11.glRotated(-roll, 0.0, 0.0, 1.0);
    		//System.out.println(vehicle.liftOffset);
    		GL11.glTranslated(-0.525, 1.0 + vehicle.getInterpolatedLiftOffset()/2, -3.0);
    		GL11.glTranslated(0.0, 0.5, -2.5);
    		*/
    	
    		/*
    		vehicle.rotationYaw = 0;
    		vehicle.prevRotationYaw = 0;
    		
    		
    		if(player.rotationYaw == vehicle.rotationYaw) {
    			
    		} else if(player.rotationYaw > vehicle.rotationYaw) {
    			player.rotationYaw += (player.rotationYaw-vehicle.rotationYaw)*0.05;
    		} else if(player.rotationYaw < vehicle.rotationYaw) {
    			player.rotationYaw -= (player.rotationYaw-vehicle.rotationYaw)*0.05;
    		}
    		
    		if(player.prevRotationYaw == vehicle.prevRotationYaw) {
    			
    		} else if(player.prevRotationYaw > vehicle.prevRotationYaw) {
    			player.prevRotationYaw += (player.prevRotationYaw-vehicle.prevRotationYaw)*0.05;
    		} else if(player.prevRotationYaw < vehicle.prevRotationYaw) {
    			player.prevRotationYaw -= (player.prevRotationYaw-vehicle.prevRotationYaw)*0.05;
    		}
    		
    		System.out.println(player.rotationYaw + " | " + player.prevRotationYaw);
    		//sysou
    		*/
    		
    		
    		double diff = player.rotationYaw - vehicle.rotationYaw;
    		if(diff != 0.0) {
    			double mod = 0.12*(Math.min(vehicle.getRealSpeed()/60.0, 1.0));
    			if(vehicle.getRealSpeed() > 5) {

    				
    				if(Math.abs(diff) > 120) mod = 1;
    				
    			}
    			if(player.rotationYaw < vehicle.rotationYaw) {
    				
    				player.rotationYaw -= diff*mod;
    			} else if(player.rotationYaw > vehicle.rotationYaw) {
    				

    				player.rotationYaw -= diff*mod;
    			}
    		}
    		
    		
    		
    		
    		player.prevRotationYaw = player.rotationYaw;
    		//player.prevRotationYaw = vehicle.prevRotationYaw;
    		
    		
    		double targ = -vehicle.rotationPitch + 15;
    		
    		double pitchDiff = player.rotationPitch - targ;
    		if(pitchDiff != 0.0) {
    			double mod = 0.12*(Math.min(vehicle.getRealSpeed()/60.0, 1.0));
    			if(vehicle.getRealSpeed() > 5) {
    				if(Math.abs(diff) > 90) mod = 1;
    			}
    			if(player.rotationPitch < targ) {
    				player.rotationPitch -= pitchDiff*mod;
    			} else if(player.rotationPitch > targ) {
    				player.rotationPitch -= pitchDiff*mod;
    			}
    		}
    		player.prevRotationPitch = player.rotationPitch;
    		
    		
    		//player.rotationPitch = 15;
    		//player.prevRotationPitch = 15;
    		
    		
    	}
    	
        
        if(weaponInstance != null ) {
            ClientModContext context = (ClientModContext) weaponInstance.getWeapon().getModContext();
            MultipartRenderStateManager<RenderableState, Part, RenderContext<RenderableState>> stateManager = weaponInstance.getWeapon().getRenderer().getStateManager(player);
//            if(stateManager != null) {
//                RenderableState lastState = stateManager.getLastState();
//                if(lastState != RenderableState.NORMAL && lastState != RenderableState.ZOOMING) {
//                    System.out.println("Last state " + lastState);
//                }
//            }
            ScreenShakingAnimationManager yawPitchAnimationManager = context.getPlayerRawPitchAnimationManager();
            yawPitchAnimationManager.update(player, weaponInstance, stateManager != null ? stateManager.getLastState() : null);
//            if(weaponInstance.isAimed() && !isProning(player)) {
//                yawPitchAnimationManager.update(player, stateManager != null ? stateManager.getLastState() : null);
////                GL11.glRotatef(5f * partialTicks, 1.0F, 0.0F, 1.0F);
//            } else {
//                yawPitchAnimationManager.reset(player, stateManager != null ? stateManager.getLastState() : null);
//            }
        }
    }
    
    private static PlayerWeaponInstance getPlayerWeaponInstance() {
        EntityPlayer player = compatibility.getClientPlayer();
        ItemStack itemStack = compatibility.getHeldItemMainHand(player);
        PlayerWeaponInstance weaponInstance = null;
        if(itemStack != null) {
            Item item = itemStack.getItem();
            if(item != null && item instanceof Weapon) {
                Weapon weapon = (Weapon) item;
                ClientModContext context = (ClientModContext) weapon.getModContext();
                weaponInstance = context.getMainHeldWeapon();
            }
        }
        return weaponInstance;
    }
    
    public static boolean setupViewBobbing(float partialTicks) {
    	
    	
        
        if(!(compatibility.getRenderViewEntity() instanceof EntityPlayer)) {
            return true;
        }
        
        EntityPlayer entityplayer = (EntityPlayer)compatibility.getRenderViewEntity();

        {
            float f =entityplayer.distanceWalkedModified - entityplayer.prevDistanceWalkedModified;
            float f1 = -(entityplayer.distanceWalkedModified + f * partialTicks);
            float f2 = entityplayer.prevCameraYaw + (entityplayer.cameraYaw - entityplayer.prevCameraYaw) * partialTicks;
            float f3 = entityplayer.prevCameraPitch + (entityplayer.cameraPitch - entityplayer.prevCameraPitch) * partialTicks;
            GL11.glTranslatef(CompatibleMathHelper.sin(f1 * (float)Math.PI) * f2 * 0.5F, -Math.abs(CompatibleMathHelper.cos(f1 * (float)Math.PI) * f2), 0.0F);
            GL11.glRotatef(CompatibleMathHelper.sin(f1 * (float)Math.PI) * f2 * 3.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(Math.abs(CompatibleMathHelper.cos(f1 * (float)Math.PI - 0.2F) * f2) * 5.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(f3, 1.0F, 0.0F, 0.0F);
        }
        
        
        {
            SpreadableExposure spreadableExposure = CompatibleExposureCapability.getExposure(entityplayer, SpreadableExposure.class);

            if(spreadableExposure != null) {
                float totalDose = spreadableExposure.getTotalDose();
                
                float f1 = totalDose; // * partialTicks;
                if(f1 > 1f) {
                    f1 = 1f;
                }
                float speed = 0.4f;//

                float f2 = 5f / (f1 * f1 + 5f) - f1 * 0.01F;
                f2 = f2 * f2;
                GL11.glRotatef(((float)spreadableExposure.getTickCount() + partialTicks) * speed, 0.0F, 1.0F, 1.0F);
                GL11.glScalef(1.0F / f2, 1.0F, 1.0F);
                GL11.glRotatef(-((float)spreadableExposure.getTickCount() + partialTicks) * speed, 0.0F, 1.0F, 1.0F);
                spreadableExposure.incrementTickCount();
            }
        }
        
        if(entityplayer.getRidingEntity() instanceof EntityVehicle) {
        	//if(1+1==2) return false;
            EntityVehicle vehicle = (EntityVehicle) entityplayer.getRidingEntity();
            double lastYawDelta = vehicle.getLastYawDelta();
            double speed = vehicle.getSpeed();
            
            
            VehicleSuspensionStrategy suspensionStrategy = vehicle.getSuspensionStrategy();
            //System.out.printf("Rate: %.5f, amp: %.5f\n", suspensionStrategy.getRate(), suspensionStrategy.getAmplitude());
            //Matrix4f transformMatrix = vehicle.getRandomizer().update(suspensionStrategy.getRate(),  suspensionStrategy.getAmplitude());
            
            // jim hack
            
            float amplitude = 0.03f;
            float frequency = 15f;
            
            double hillFrac = (vehicle.getSolver().getVelocityVector().lengthVector()*(vehicle.rotationPitch/2))/20;
            amplitude += Math.abs(hillFrac)/250;
            frequency += Math.abs(hillFrac)/250;
            
            
            if(vehicle.getSolver().materialBelow != Material.ROCK) {
            	amplitude += 0.1f;
            	frequency += 2f;
    
            	amplitude *= vehicle.getRealSpeed()/25.0;
            	frequency *= vehicle.getRealSpeed()/25.0;
            	
            	
            }
            
            float appliedAmplitude = 0.0f;
            if(Minecraft.getMinecraft().gameSettings.thirdPersonView != 0) {
            	appliedAmplitude = amplitude;
            } else appliedAmplitude = amplitude/10.0f;
            
            
            
            
            Matrix4f transformMatrix = vehicle.getRandomizer().update(frequency,  appliedAmplitude);
           //
            
            //RenderVehicle2.captureCameraTransform(transformMatrix);
            //System.out.printf("Yaw delta: %.5f, speed: %.5f\n", lastYawDelta, speed);
            
            
            if(Math.abs(lastYawDelta) > 0.3) {
              //  GL11.glRotatef(-(float)lastYawDelta * 2f, 0.0F, 1.0f, 0.0f);
            }
        } else {
            RenderVehicle2.captureCameraTransform(null);
        }
        
        return false;
    }
    
    public static boolean hurtCameraEffect(float partialTicks) {
        
        if(!(compatibility.getRenderViewEntity() instanceof EntityPlayer)) {
            return true;
        }
        
        boolean allowDefaultEffect = false;

        EntityPlayer entitylivingbase = (EntityPlayer)compatibility.getRenderViewEntity();
        float f = (float) entitylivingbase.hurtTime - partialTicks;

        if (entitylivingbase.getHealth() <= 0.0F) {
            float f1 = (float) entitylivingbase.deathTime + partialTicks;
            GL11.glRotatef(40.0F - 8000.0F / (f1 + 200.0F), 0.0F, 0.0F, 1.0F);
        }

        if (f < 0.0F) {
            return allowDefaultEffect;
        }

        f = f / (float) entitylivingbase.maxHurtTime;
        f = CompatibleMathHelper.sin(f * f * f * f * (float) Math.PI);
        float f2 = entitylivingbase.attackedAtYaw;
        GL11.glRotatef(-f2, 0.0F, 1.0F, 0.0F);
        SpreadableExposure spreadableExposure = CompatibleExposureCapability.getExposure(entitylivingbase, SpreadableExposure.class);

        if(spreadableExposure != null) {
            GL11.glRotatef(-f * 4.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(-f * 1.0F, 0.0F, 0.0F, 1.0F);
        } else {
            GL11.glRotatef(-f * 14.0F, 0.0F, 0.0F, 1.0F);
        }
        
        GL11.glRotatef(f2, 0.0F, 1.0F, 0.0F);
        
        

        return allowDefaultEffect;
    }
        
    public static boolean nauseaCameraEffect(float partialTicks) {
        boolean allowDefaultEffect = false;

//        Minecraft mc = Minecraft.getMinecraft();
//
//        float f1 = mc.thePlayer.prevTimeInPortal + (mc.thePlayer.timeInPortal - mc.thePlayer.prevTimeInPortal) * partialTicks;
//
//        int i = 1;
//
//        float f2 = 5.0F / (f1 * f1 + 5.0F) - f1 * 0.04F;
//        f2 = f2 * f2;
//        GlStateManager.rotate(((float)rendererUpdateCount + partialTicks) * (float)i, 0.0F, 1.0F, 1.0F);
//        GlStateManager.scale(1.0F / f2, 1.0F, 1.0F);
//        GlStateManager.rotate(-((float)rendererUpdateCount + partialTicks) * (float)i, 0.0F, 1.0F, 1.0F);

        //rendererUpdateCount++;
        return allowDefaultEffect;
    }
    

    private static Map<Entity, PlayerRenderer> renderers = new HashMap<>();
    
    public static PlayerRenderer getPlayerRenderer(Entity entity) {
        return renderers.get(entity);
    }
    
    public static void render2(ModelBase modelBase, Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
    	
    	
    	
    	if(entityIn instanceof EntityPlayer) {
    		EntityPlayer player = (EntityPlayer) entityIn;
    		if(player.isRiding() && player.getRidingEntity() instanceof EntityVehicle) {
    			double b = ((EntityPlayer) entityIn).limbSwing;
    			if(b != 39.0) return;    			
    		}
    	}
    	
        if(entityIn instanceof EntityPlayer && modelBase instanceof ModelPlayer) {
            
            ModelPlayer modelPlayer = (ModelPlayer) modelBase;
            EntityPlayer player = (EntityPlayer) entityIn;

            PlayerRenderer playerRenderer = renderers.computeIfAbsent(entityIn, 
                    e -> new PlayerRenderer((EntityPlayer) entityIn, ClientModContext.getContext()));
            
            
            playerRenderer.renderModel(modelPlayer, player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            
            
            
            CustomPlayerInventory capability = CompatibleCustomPlayerInventoryCapability.getInventory(player);
            if(capability != null) {
                ItemStack backpackStack = capability.getStackInSlot(0); // TODO: replace 0 with constant for backpack slot 
                if(backpackStack != null) {
                    GL11.glPushMatrix();
                    adjustBodyWearablePosition(player);
                    compatibility.renderItem(player, backpackStack);
                    GL11.glPopMatrix();
                }
                ItemStack vestStack = capability.getStackInSlot(1); // TODO: replace 0 with constant for backpack slot 
                if(vestStack != null) {
                    GL11.glPushMatrix();
                    adjustBodyWearablePosition(player);
                    compatibility.renderItem(player, vestStack);
                    GL11.glPopMatrix();
                }
            }
        } else {
            modelBase.render(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        }
        
        
    }
    
    private static void adjustBodyWearablePosition(EntityPlayer player) {
//        GL11.glScalef(0.8f, 0.8f, 0.8f);
//        GL11.glTranslatef(-0.02f, 0.69f, -0.35f);
//        GL11.glRotatef(180f, 0, 0, 1);
//        if(isProning(player)) {
//            GL11.glScalef(0.8f, 0.8f, 0.8f);
//            GL11.glTranslatef(-0.02f, -3f, -0.35f);
//            GL11.glRotatef(180f, 0, 0, 1);
//        } else {
//            GL11.glScalef(0.8f, 0.8f, 0.8f);
//            GL11.glTranslatef(-0.02f, 0.69f, -0.35f);
//            GL11.glRotatef(180f, 0, 0, 1);
//        }
        
    }

    public static void renderArmorLayer(ModelBase modelBase, Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
       
        if(entityIn instanceof EntityPlayer) { 
            PlayerRenderer playerRenderer = renderers.get(entityIn);
            EntityPlayer player = (EntityPlayer) entityIn;
            if(playerRenderer == null || !playerRenderer.renderArmor((ModelBiped) modelBase, player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale)) {
                modelBase.render(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            }
        } else {
            modelBase.render(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        }
    }

    public static void positionItemSide(RenderLivingBase<?> livingEntityRenderer, EntityLivingBase entity,
            ItemStack itemStack, TransformType transformType, EnumHandSide handSide) {
    	
        if(entity instanceof EntityPlayer /* && isProning((EntityPlayer) entity)*/) { 
            PlayerRenderer playerRenderer = renderers.get(entity);
            EntityPlayer player = (EntityPlayer) entity;
            if(playerRenderer == null || !playerRenderer.positionItemSide(player, itemStack, CompatibleTransformType.fromItemRenderType(transformType), 
                    CompatibleEnumHandSide.fromEnumHandSide(handSide))) {
                ((ModelBiped)livingEntityRenderer.getMainModel()).postRenderArm(0.0625F, handSide);
            }
        } else {
            ((ModelBiped)livingEntityRenderer.getMainModel()).postRenderArm(0.0625F, handSide);
        }
    }
    
    public static boolean isProning(EntityPlayer player) {
        return (CompatibleExtraEntityFlags.getFlags(player) & CompatibleExtraEntityFlags.PRONING) != 0;
    }
    
    public static float adjustCameraPosition(EntityLivingBase player, float position) {
    
        return player instanceof EntityPlayer && isProning((EntityPlayer) player) 
                && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0 ? position 
                + player.getEyeHeight() * 1.6f : position;
    }
    
    public static void turn(EntityPlayer player, float yawDelta, float pitchDelta) {
        float originalPitch = player.rotationPitch;
        float originalYaw = player.rotationYaw;
        //System.out.println("Yaw delta: " + yawDelta);
        
       
        
        float maxPitch = 90f;
        float maxYawDelta = 40f;
        
        yawDelta *= 0.15;
        
       
       
        boolean canChangeRotationYaw = true;
        if(player.getRidingEntity() instanceof EntityVehicle && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0) {
        	
            maxPitch = 90f;
//            EntityVehicle entityVehicle = (EntityVehicle) player.ridingEntity;
////            maxYawDelta = 10f + 200f * (float)entityVehicle.getSpeed();
////            if(maxYawDelta > 35f) {
////                maxYawDelta = 35f;
////            }
////            System.out.println("Speed: " + entityVehicle.getSpeed() + ", maxYawD: " + maxYawDelta);
//            //canChangeRotationYaw = entityVehicle.getState() != VehicleState.STOPPING;
//            float vehicleRiderYawDelta = CompatibleMathHelper.wrapAngleTo180Float(player.ridingEntity.rotationYaw - player.rotationYaw);
//            if(vehicleRiderYawDelta > maxYawDelta) {
//                vehicleRiderYawDelta = maxYawDelta;
//                yawDelta = 1f;
//            }
            
            player.rotationYaw = (float) ((double) player.rotationYaw + (double) yawDelta);
            float vehicleRiderYawDelta = CompatibleMathHelper.wrapAngleTo180Float(player.getRidingEntity().rotationYaw - player.rotationYaw);
            //System.out.println("Proposed delta: " + yawDelta + ", allowed: " + vehicleRiderYawDelta);

            if(vehicleRiderYawDelta > maxYawDelta) {
                player.rotationYaw = player.getRidingEntity().rotationYaw - maxYawDelta;
            } 
            else if(-vehicleRiderYawDelta > maxYawDelta) {
                player.rotationYaw = player.getRidingEntity().rotationYaw + maxYawDelta;
            }
            
          // player.rotationPitch = -player.getRidingEntity().rotationPitch * 1.3f;
            
            // extra jim code :)
            //player.rotationPitch = -player.getRidingEntity().rotationPitch;
            
            
            // start
player.rotationYaw = (float) ((double) player.rotationYaw + (double) yawDelta);
            
            player.rotationPitch = (float) ((double) player.rotationPitch - (double) pitchDelta * 0.15);

            if (player.rotationPitch < -maxPitch) {
                player.rotationPitch = -maxPitch;
            }

            if (player.rotationPitch > maxPitch) {
                player.rotationPitch = maxPitch;
            }
            
            player.prevRotationPitch += player.rotationPitch - originalPitch;
            
            // end
            

        } else {
        	
            player.rotationYaw = (float) ((double) player.rotationYaw + (double) yawDelta);
            
            player.rotationPitch = (float) ((double) player.rotationPitch - (double) pitchDelta * 0.15);

            if (player.rotationPitch < -maxPitch) {
                player.rotationPitch = -maxPitch;
            }

            if (player.rotationPitch > maxPitch) {
                player.rotationPitch = maxPitch;
            }
            
            player.prevRotationPitch += player.rotationPitch - originalPitch;
        }
       
       
        player.prevRotationYaw += player.rotationYaw - originalYaw;
    }

    private static double volumeThreshold;
    
    public static void setRenderVolumeThreshold(double d) {
        volumeThreshold = d;
    }
    
    public static boolean shouldRender(List<ModelBox> cubeList) {
        if(volumeThreshold <= 0.1) {
            return true;
        }
        return ((OptimizedCubeList) cubeList).getMaxVol() > volumeThreshold;
    }
}
