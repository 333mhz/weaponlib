package com.vicmatskiv.weaponlib;

import static com.vicmatskiv.weaponlib.compatibility.CompatibilityProvider.compatibility;

import java.nio.FloatBuffer;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import com.vicmatskiv.weaponlib.animation.DebugPositioner;
import com.vicmatskiv.weaponlib.animation.MatrixHelper;
import com.vicmatskiv.weaponlib.animation.MultipartPositioning;
import com.vicmatskiv.weaponlib.animation.MultipartPositioning.Positioner;
import com.vicmatskiv.weaponlib.animation.MultipartRenderStateDescriptor;
import com.vicmatskiv.weaponlib.animation.MultipartRenderStateManager;
import com.vicmatskiv.weaponlib.animation.MultipartTransition;
import com.vicmatskiv.weaponlib.compatibility.CompatibleEnumHandSide;
import com.vicmatskiv.weaponlib.compatibility.CompatibleExtraEntityFlags;
import com.vicmatskiv.weaponlib.compatibility.CompatibleTransformType;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class PlayerRenderer {
    
    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger(PlayerRenderer.class);

    protected static class StateDescriptor implements MultipartRenderStateDescriptor<RenderableState, Part, RenderContext<RenderableState>>{
        protected MultipartRenderStateManager<RenderableState, Part, RenderContext<RenderableState>> stateManager;
        protected float rate;
        protected float amplitude = 0.04f;
        public StateDescriptor(MultipartRenderStateManager<RenderableState, Part, RenderContext<RenderableState>> stateManager,
                float rate, float amplitude) {
            this.stateManager = stateManager;
            this.rate = rate;
            this.amplitude = amplitude;
        }
        
        @Override
        public MultipartRenderStateManager<RenderableState, Part, RenderContext<RenderableState>> getStateManager() {
            return stateManager;
        }
    }
    
    private static class EquippedPlayerTransitionProvider extends PlayerTransitionProvider {

        private PlayerTransitionProvider delegate;
        private EntityPlayer player;

        EquippedPlayerTransitionProvider(EntityPlayer player, PlayerTransitionProvider delegate) {
            this.player = player;
            this.delegate = delegate;
        }

        public List<MultipartTransition<Part, RenderContext<RenderableState>>> getTransitions(RenderableState state) {
            ItemStack heldStack = compatibility.getHeldItemMainHand(player);
            if(heldStack != null && heldStack.getItem() instanceof Weapon) {
                //((Weapon)heldStack.getItem()).getRenderer().
            }
            return delegate.getTransitions(state);
        }
    }
     
    private PlayerTransitionProvider transitionProvider;// = new PlayerTransitionProvider();
    
    private class PositionerDescriptor {
        Positioner<Part, RenderContext<RenderableState>> positioner;

        boolean leftHandPositioned;
        boolean rightHandPositioned;
        PositionerDescriptor(Positioner<Part, RenderContext<RenderableState>> positioner) {
            super();
            this.positioner = positioner;
        }
    }
    
    private ThreadLocal<PositionerDescriptor> currentPositioner = new ThreadLocal<>();

//    private ThreadLocal<Positioner<Part, RenderContext<RenderableState>>> currentPositioner = new ThreadLocal<>();
    
    private int currentFlags;
    private int newFlags;
    private long renderingStartTimestamp;
    private long playerStopMovingTimestamp;
    private ClientModContext clientModContext;
    
    private MultipartRenderStateManager<RenderableState, Part, RenderContext<RenderableState>> generalPlayerStateManager;
    
    public PlayerRenderer(EntityPlayer player, ClientModContext clientModContext) {
        this.clientModContext = clientModContext;
        this.transitionProvider = new EquippedPlayerTransitionProvider(
                player,
                clientModContext.getPlayerTransitionProvider());
    }
    
    public Positioner<Part, RenderContext<RenderableState>> getCurrentPositioner() {
        PositionerDescriptor descriptor = currentPositioner.get();
        return descriptor != null ? descriptor.positioner : null;
    }
    
    private MultipartRenderStateDescriptor<RenderableState, Part, RenderContext<RenderableState>> getStateDescriptor(EntityPlayer player) {
        
        if(currentFlags != newFlags) {
            generalPlayerStateManager = null;
        }
        
        boolean isProning = (newFlags & CompatibleExtraEntityFlags.PRONING) != 0;
                
        
        if(generalPlayerStateManager == null) {
            //logger.trace("Creating state manager");
            generalPlayerStateManager = new MultipartRenderStateManager<>(RenderableState.NORMAL, transitionProvider,
                  () -> currentTime(player));
        } else if(isProning && player.distanceWalkedModified == player.prevDistanceWalkedModified
                /*|| player.motionX == 0 || player.motionZ == 0*/) {
            //logger.trace("Setting aiming state");
            generalPlayerStateManager.setState(RenderableState.PRONING_AIMING, true, true, true);
        } else if(isProning) {
            //logger.trace("Setting proning state");
            generalPlayerStateManager.setCycleState(RenderableState.PRONING, false);
        } else {
            ItemStack heldStack = compatibility.getHeldItemMainHand(player);
            
            if(heldStack != null && heldStack.getItem() instanceof Weapon) {
                /*
                 * if this player is not proning and holds a weapon, get a weapon state descriptor
                 * holding weapon state manager, i.e. animate as defined per weapon
                 */
                return ((Weapon)heldStack.getItem()).getRenderer().getThirdPersonStateDescriptor(player, heldStack);
            }
            
            generalPlayerStateManager.setState(RenderableState.NORMAL, true, false);
        }

        return new StateDescriptor(generalPlayerStateManager, 0f, 0f);
    }
    
    private long currentTime(EntityPlayer player) {
        long elapseRenderingStart = System.currentTimeMillis() - renderingStartTimestamp;
        int renderingStartThreshold = 400;
        if(elapseRenderingStart < renderingStartThreshold) {
            return elapseRenderingStart;
        }
        long afterStopMovingTimeout = 0;
        if(player.distanceWalkedModified == player.prevDistanceWalkedModified) {
            //logger.trace("Player stopped moving");
            if(playerStopMovingTimestamp == 0) {
                playerStopMovingTimestamp = System.currentTimeMillis();
            } else if(afterStopMovingTimeout < 1000){
                afterStopMovingTimeout = System.currentTimeMillis() - playerStopMovingTimestamp;
            }
        } else {
            playerStopMovingTimestamp = 0;
        }
        return (long)(renderingStartThreshold + player.distanceWalkedModified * 600 + (afterStopMovingTimeout));
    }

    public void renderModel(ModelBiped modelPlayer, EntityPlayer player, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        newFlags = CompatibleExtraEntityFlags.getFlags(player);
        if(newFlags != currentFlags) {
            renderingStartTimestamp = System.currentTimeMillis();
        }
        currentPositioner.remove();
        if(true) { //(newFlags & CompatibleExtraEntityFlags.PRONING) != 0) {
            renderAnimatedModel(modelPlayer, player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        } else {
            //currentPositioner.remove();
            renderBipedModel(modelPlayer, player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        }
        currentFlags = newFlags;
    }
    
    private void renderBipedModel(ModelBiped model, Entity entity, float limbSwing, float limbSwingAmount, float p_78088_4_, float p_78088_5_, float p_78088_6_, float p_78088_7_) {
        model.setRotationAngles(limbSwing, limbSwingAmount, p_78088_4_, p_78088_5_, p_78088_6_, p_78088_7_, entity);
//        model.bipedLeftArm.rotateAngleX = 0f;
//        model.bipedLeftArm.rotateAngleY = 0f;
//        model.bipedRightArm.rotateAngleZ = 0f;
//        model.bipedRightArm.rotateAngleX = 0f;
//        model.bipedRightArm.rotateAngleY = 0f;
//        model.bipedRightArm.rotateAngleZ = 0f;
        if (model.isChild)
        {
            float f6 = 2.0F;
            GL11.glPushMatrix();
            GL11.glScalef(1.5F / f6, 1.5F / f6, 1.5F / f6);
            GL11.glTranslatef(0.0F, 16.0F * p_78088_7_, 0.0F);
            model.bipedHead.render(p_78088_7_);
            GL11.glPopMatrix();
            GL11.glPushMatrix();
            GL11.glScalef(1.0F / f6, 1.0F / f6, 1.0F / f6);
            GL11.glTranslatef(0.0F, 24.0F * p_78088_7_, 0.0F);
            model.bipedBody.render(p_78088_7_);
            model.bipedRightArm.render(p_78088_7_);
            model.bipedLeftArm.render(p_78088_7_);
            model.bipedRightLeg.render(p_78088_7_);
            model.bipedLeftLeg.render(p_78088_7_);
            model.bipedHeadwear.render(p_78088_7_);
            GL11.glPopMatrix();
        }
        else
        {
            model.bipedHead.render(p_78088_7_);
            model.bipedBody.render(p_78088_7_);
            model.bipedRightArm.render(p_78088_7_);
            model.bipedLeftArm.render(p_78088_7_);
            model.bipedRightLeg.render(p_78088_7_);
            model.bipedLeftLeg.render(p_78088_7_);
            model.bipedHeadwear.render(p_78088_7_);
        }
    }

    private void renderAnimatedModel(ModelBiped modelPlayer, EntityPlayer player, 
            float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch, float scale) {
        MultipartRenderStateDescriptor<RenderableState, Part, RenderContext<RenderableState>> stateDescriptor = getStateDescriptor(player);
        MultipartPositioning<Part, RenderContext<RenderableState>> multipartPositioning = stateDescriptor.getStateManager().nextPositioning();
        Positioner<Part, RenderContext<RenderableState>> positioner = multipartPositioning.getPositioner();
        currentPositioner.set(new PositionerDescriptor(positioner));
        
        GL11.glPushMatrix();
        
        RenderContext<RenderableState> renderContext = new RenderContext<>(clientModContext, player, null);
        
        renderContext.setLimbSwing(limbSwing);
        renderContext.setFlimbSwingAmount(limbSwingAmount);
        
        renderContext.setAgeInTicks(ageInTicks);
        renderContext.setScale(scale);
        
        renderContext.setNetHeadYaw(netHeadYaw);
        renderContext.setHeadPitch(headPitch);
        renderContext.setCompatibleTransformType(CompatibleTransformType.NONE);
               
        positioner.position(Part.MAIN, renderContext);
        
        modelPlayer.setRotationAngles(renderContext.getLimbSwing(), renderContext.getFlimbSwingAmount(), 
                renderContext.getAgeInTicks(), renderContext.getNetHeadYaw(), 
                renderContext.getHeadPitch(), renderContext.getScale(), renderContext.getPlayer());

        renderBody(positioner, modelPlayer, renderContext);
        renderHead(positioner, modelPlayer, renderContext);
        renderLeftArm(positioner, modelPlayer, renderContext);
        renderRightArm(positioner, modelPlayer, renderContext);
        renderLeftLeg(positioner, modelPlayer, renderContext);
        renderRightLeg(positioner, modelPlayer, renderContext);
        
        GL11.glPopMatrix();
    }
    
//    private void setRotationAngles(ModelBiped model, float p_78087_1_, float p_78087_2_, float p_78087_3_, float p_78087_4_, float p_78087_5_, float p_78087_6_, Entity p_78087_7_)
//    {
//        model.bipedHead.rotateAngleY = p_78087_4_ / (180F / (float)Math.PI);
//        model.bipedHead.rotateAngleX = p_78087_5_ / (180F / (float)Math.PI);
//        model.bipedHeadwear.rotateAngleY = model.bipedHead.rotateAngleY;
//        model.bipedHeadwear.rotateAngleX = model.bipedHead.rotateAngleX;
//        model.bipedRightArm.rotateAngleX = MathHelper.cos(p_78087_1_ * 0.6662F + (float)Math.PI) * 2.0F * p_78087_2_ * 0.5F;
//        model.bipedLeftArm.rotateAngleX = MathHelper.cos(p_78087_1_ * 0.6662F) * 2.0F * p_78087_2_ * 0.5F;
//        model.bipedRightArm.rotateAngleZ = 0.0F;
//        model.bipedLeftArm.rotateAngleZ = 0.0F;
//        model.bipedRightLeg.rotateAngleX = MathHelper.cos(p_78087_1_ * 0.6662F) * 1.4F * p_78087_2_;
//        model.bipedLeftLeg.rotateAngleX = MathHelper.cos(p_78087_1_ * 0.6662F + (float)Math.PI) * 1.4F * p_78087_2_;
//        model.bipedRightLeg.rotateAngleY = 0.0F;
//        model.bipedLeftLeg.rotateAngleY = 0.0F;
//
//        if (model.isRiding)
//        {
//            model.bipedRightArm.rotateAngleX += -((float)Math.PI / 5F);
//            model.bipedLeftArm.rotateAngleX += -((float)Math.PI / 5F);
//            model.bipedRightLeg.rotateAngleX = -((float)Math.PI * 2F / 5F);
//            model.bipedLeftLeg.rotateAngleX = -((float)Math.PI * 2F / 5F);
//            model.bipedRightLeg.rotateAngleY = ((float)Math.PI / 10F);
//            model.bipedLeftLeg.rotateAngleY = -((float)Math.PI / 10F);
//        }
//
//        if (model.heldItemLeft != 0)
//        {
//            model.bipedLeftArm.rotateAngleX = model.bipedLeftArm.rotateAngleX * 0.5F - ((float)Math.PI / 10F) * (float)model.heldItemLeft;
//        }
//
//        if (model.heldItemRight != 0)
//        {
//            model.bipedRightArm.rotateAngleX = model.bipedRightArm.rotateAngleX * 0.5F - ((float)Math.PI / 10F) * (float)model.heldItemRight;
//        }
//
//        model.bipedRightArm.rotateAngleY = 0.0F;
//        model.bipedLeftArm.rotateAngleY = 0.0F;
//        float f6;
//        float f7;
//
//        if (model.onGround > -9990.0F)
//        {
//            f6 = model.onGround;
//            model.bipedBody.rotateAngleY = MathHelper.sin(MathHelper.sqrt_float(f6) * (float)Math.PI * 2.0F) * 0.2F;
//            model.bipedRightArm.rotationPointZ = MathHelper.sin(model.bipedBody.rotateAngleY) * 5.0F;
//            model.bipedRightArm.rotationPointX = -MathHelper.cos(model.bipedBody.rotateAngleY) * 5.0F;
//            model.bipedLeftArm.rotationPointZ = -MathHelper.sin(model.bipedBody.rotateAngleY) * 5.0F;
//            model.bipedLeftArm.rotationPointX = MathHelper.cos(model.bipedBody.rotateAngleY) * 5.0F;
//            model.bipedRightArm.rotateAngleY += model.bipedBody.rotateAngleY;
//            model.bipedLeftArm.rotateAngleY += model.bipedBody.rotateAngleY;
//            model.bipedLeftArm.rotateAngleX += model.bipedBody.rotateAngleY;
//            f6 = 1.0F - model.onGround;
//            f6 *= f6;
//            f6 *= f6;
//            f6 = 1.0F - f6;
//            f7 = MathHelper.sin(f6 * (float)Math.PI);
//            float f8 = MathHelper.sin(model.onGround * (float)Math.PI) * -(model.bipedHead.rotateAngleX - 0.7F) * 0.75F;
//            model.bipedRightArm.rotateAngleX = (float)((double)model.bipedRightArm.rotateAngleX - ((double)f7 * 1.2D + (double)f8));
//            model.bipedRightArm.rotateAngleY += model.bipedBody.rotateAngleY * 2.0F;
//            model.bipedRightArm.rotateAngleZ = MathHelper.sin(model.onGround * (float)Math.PI) * -0.4F;
//        }
//
//        if (model.isSneak)
//        {
//            model.bipedBody.rotateAngleX = 0.5F;
//            model.bipedRightArm.rotateAngleX += 0.4F;
//            model.bipedLeftArm.rotateAngleX += 0.4F;
//            model.bipedRightLeg.rotationPointZ = 4.0F;
//            model.bipedLeftLeg.rotationPointZ = 4.0F;
//            model.bipedRightLeg.rotationPointY = 9.0F;
//            model.bipedLeftLeg.rotationPointY = 9.0F;
//            model.bipedHead.rotationPointY = 1.0F;
//            model.bipedHeadwear.rotationPointY = 1.0F;
//        }
//        else
//        {
//            model.bipedBody.rotateAngleX = 0.0F;
//            model.bipedRightLeg.rotationPointZ = 0.1F;
//            model.bipedLeftLeg.rotationPointZ = 0.1F;
//            model.bipedRightLeg.rotationPointY = 12.0F;
//            model.bipedLeftLeg.rotationPointY = 12.0F;
//            model.bipedHead.rotationPointY = 0.0F;
//            model.bipedHeadwear.rotationPointY = 0.0F;
//        }
//
//        model.bipedRightArm.rotateAngleZ += MathHelper.cos(p_78087_3_ * 0.09F) * 0.05F + 0.05F;
//        model.bipedLeftArm.rotateAngleZ -= MathHelper.cos(p_78087_3_ * 0.09F) * 0.05F + 0.05F;
//        model.bipedRightArm.rotateAngleX += MathHelper.sin(p_78087_3_ * 0.067F) * 0.05F;
//        model.bipedLeftArm.rotateAngleX -= MathHelper.sin(p_78087_3_ * 0.067F) * 0.05F;
//
//        if (model.aimedBow)
//        {
//            f6 = 0.0F;
//            f7 = 0.0F;
//            model.bipedRightArm.rotateAngleZ = 0.0F;
//            model.bipedLeftArm.rotateAngleZ = 0.0F;
//            model.bipedRightArm.rotateAngleY = -(0.1F - f6 * 0.6F) + model.bipedHead.rotateAngleY;
//            model.bipedLeftArm.rotateAngleY = 0.1F - f6 * 0.6F + model.bipedHead.rotateAngleY + 0.4F;
//            model.bipedRightArm.rotateAngleX = -((float)Math.PI / 2F) + model.bipedHead.rotateAngleX;
//            model.bipedLeftArm.rotateAngleX = -((float)Math.PI / 2F) + model.bipedHead.rotateAngleX;
//            model.bipedRightArm.rotateAngleX -= f6 * 1.2F - f7 * 0.4F;
//            model.bipedLeftArm.rotateAngleX -= f6 * 1.2F - f7 * 0.4F;
//            model.bipedRightArm.rotateAngleZ += MathHelper.cos(p_78087_3_ * 0.09F) * 0.05F + 0.05F;
//            model.bipedLeftArm.rotateAngleZ -= MathHelper.cos(p_78087_3_ * 0.09F) * 0.05F + 0.05F;
//            model.bipedRightArm.rotateAngleX += MathHelper.sin(p_78087_3_ * 0.067F) * 0.05F;
//            model.bipedLeftArm.rotateAngleX -= MathHelper.sin(p_78087_3_ * 0.067F) * 0.05F;
//        }
//    }
    
    private void renderBody(Positioner<Part, RenderContext<RenderableState>> positioner, 
            ModelBiped modelPlayer, RenderContext<RenderableState> renderContext) {
        GL11.glPushMatrix();
        modelPlayer.bipedBody.render(renderContext.getScale());
        compatibility.renderBodywear(modelPlayer, renderContext.getScale());
        GL11.glPopMatrix();
    }
    
    private void renderHead(Positioner<Part, RenderContext<RenderableState>> positioner,
            ModelBiped modelPlayer, RenderContext<RenderableState> renderContext) {
        GL11.glPushMatrix();
        FloatBuffer preBuf = MatrixHelper.getModelViewMatrixBuffer();
        positioner.position(Part.HEAD, renderContext);
        FloatBuffer postBuf = MatrixHelper.getModelViewMatrixBuffer();
        if(!preBuf.equals(postBuf)) {
//            modelPlayer.bipedHead.rotateAngleX = modelPlayer.bipedHead.rotateAngleY = 
//                    modelPlayer.bipedHead.rotateAngleZ = 0f;
        }
        modelPlayer.bipedHead.render(renderContext.getScale());
        compatibility.renderHeadwear(modelPlayer, renderContext.getScale());
        GL11.glPopMatrix();
    }

    private void renderRightArm(Positioner<Part, RenderContext<RenderableState>> positioner,
            ModelBiped modelPlayer, RenderContext<RenderableState> renderContext) {
        GL11.glPushMatrix();
        FloatBuffer preBuf = MatrixHelper.getModelViewMatrixBuffer();
        positioner.position(Part.RIGHT_HAND, renderContext);
        if(DebugPositioner.isDebugModeEnabled()) {
            DebugPositioner.position(Part.RIGHT_HAND, renderContext);
        }
        FloatBuffer postBuf = MatrixHelper.getModelViewMatrixBuffer();
        if(!preBuf.equals(postBuf)) {
//            modelPlayer.bipedRightArm.rotateAngleX  = -0.27882767f;
//            modelPlayer.bipedRightArm.rotateAngleY = 0f;
//            modelPlayer.bipedRightArm.rotateAngleZ  = -0.014263712f;
            modelPlayer.bipedRightArm.rotateAngleX = modelPlayer.bipedRightArm.rotateAngleY = 
                    modelPlayer.bipedRightArm.rotateAngleZ = 0f;
            currentPositioner.get().rightHandPositioned = true;
        }
        modelPlayer.bipedRightArm.render(renderContext.getScale());
        compatibility.renderRightArmwear(modelPlayer, renderContext.getScale());
        GL11.glPopMatrix();
    }

    private void renderLeftArm(Positioner<Part, RenderContext<RenderableState>> positioner,
            ModelBiped modelPlayer, RenderContext<RenderableState> renderContext) {
        GL11.glPushMatrix();
        FloatBuffer preBuf = MatrixHelper.getModelViewMatrixBuffer();
        positioner.position(Part.LEFT_HAND, renderContext);
        if(DebugPositioner.isDebugModeEnabled()) {
            DebugPositioner.position(Part.LEFT_HAND, renderContext);
        }
        FloatBuffer postBuf = MatrixHelper.getModelViewMatrixBuffer();
        if(!preBuf.equals(postBuf)) {
//            modelPlayer.bipedLeftArm.rotateAngleX = 0.038862526f;
//            modelPlayer.bipedLeftArm.rotateAngleY = 0f;
//            modelPlayer.bipedLeftArm.rotateAngleZ = -0.09030269f;
            modelPlayer.bipedLeftArm.rotateAngleX = modelPlayer.bipedLeftArm.rotateAngleY = 
                    modelPlayer.bipedLeftArm.rotateAngleZ = 0f;
            currentPositioner.get().leftHandPositioned = true;
        }
        modelPlayer.bipedLeftArm.render(renderContext.getScale());
        compatibility.renderLeftArmwear(modelPlayer, renderContext.getScale());
        GL11.glPopMatrix();
    }
    
    private void renderRightLeg(Positioner<Part, RenderContext<RenderableState>> positioner,
            ModelBiped modelPlayer, RenderContext<RenderableState> renderContext) {
        GL11.glPushMatrix();
        FloatBuffer preBuf = MatrixHelper.getModelViewMatrixBuffer();
        positioner.position(Part.RIGHT_LEG, renderContext);
        FloatBuffer postBuf = MatrixHelper.getModelViewMatrixBuffer();
        if(!preBuf.equals(postBuf)) {
            modelPlayer.bipedRightLeg.rotateAngleX = modelPlayer.bipedRightLeg.rotateAngleY = 
                    modelPlayer.bipedRightLeg.rotateAngleZ = 0f;
        }
        modelPlayer.bipedRightLeg.render(renderContext.getScale());
        compatibility.renderRightLegwear(modelPlayer, renderContext.getScale());
        GL11.glPopMatrix();
    }

    private void renderLeftLeg(Positioner<Part, RenderContext<RenderableState>> positioner,
            ModelBiped modelPlayer, RenderContext<RenderableState> renderContext) {
        GL11.glPushMatrix();
//        GL11.glScalef(1.01f, 1.01f, 1.01f);
        FloatBuffer preBuf = MatrixHelper.getModelViewMatrixBuffer();
        positioner.position(Part.LEFT_LEG, renderContext);
        FloatBuffer postBuf = MatrixHelper.getModelViewMatrixBuffer();
        if(!preBuf.equals(postBuf)) {
            modelPlayer.bipedLeftLeg.rotateAngleX = modelPlayer.bipedLeftLeg.rotateAngleY = 
                    modelPlayer.bipedLeftLeg.rotateAngleZ = 0f;
        }
        modelPlayer.bipedLeftLeg.render(renderContext.getScale());
        compatibility.renderLeftLegwear(modelPlayer, renderContext.getScale());
        GL11.glPopMatrix();
    }

//    public boolean renderArmor(ModelBiped modelPlayer, EntityPlayer player, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
//        if((currentFlags & CompatibleExtraEntityFlags.PRONING) != 0) {
//            return renderArmor2(modelPlayer, player, ageInTicks, netHeadYaw, headPitch, scale);
//        } else {
//            return false;
//        }
//    }

    public boolean renderArmor(ModelBiped modelPlayer, EntityPlayer player, float limbSwing, float limbSwingAmount, 
            float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        
        PositionerDescriptor descriptor = currentPositioner.get();
        
        if(descriptor != null) {
            Positioner<Part, RenderContext<RenderableState>> positioner = descriptor.positioner;

            GL11.glPushMatrix();
            
            RenderContext<RenderableState> renderContext = new RenderContext<>(clientModContext, player, null);

            renderContext.setAgeInTicks(ageInTicks);
            renderContext.setScale(scale);
            renderContext.setLimbSwing(limbSwing); //limbSwing);
            renderContext.setFlimbSwingAmount(limbSwingAmount);
            renderContext.setNetHeadYaw(netHeadYaw);
            renderContext.setHeadPitch(headPitch);
            renderContext.setCompatibleTransformType(CompatibleTransformType.NONE);
            
            modelPlayer.setRotationAngles(renderContext.getLimbSwing(), renderContext.getFlimbSwingAmount(), 
                    renderContext.getAgeInTicks(), renderContext.getNetHeadYaw(), 
                    renderContext.getHeadPitch(), renderContext.getScale(), renderContext.getPlayer());

            positioner.position(Part.MAIN, renderContext);
            
            renderBody(positioner, modelPlayer, renderContext);
            renderHead(positioner, modelPlayer, renderContext);
            renderLeftArm(positioner, modelPlayer, renderContext);
            renderRightArm(positioner, modelPlayer, renderContext);
            renderLeftLeg(positioner, modelPlayer, renderContext);
            renderRightLeg(positioner, modelPlayer, renderContext);
            
            GL11.glPopMatrix();
        }
        return descriptor != null;
    }

    public boolean positionItemSide(EntityPlayer player, ItemStack itemStack, CompatibleTransformType transformType, CompatibleEnumHandSide handSide) {
        PositionerDescriptor descriptor = currentPositioner.get();
        if(descriptor != null) {
            
            if((handSide == null || handSide == CompatibleEnumHandSide.RIGHT) 
                    && !descriptor.rightHandPositioned) {
                return false;
            }
            
            if(handSide == CompatibleEnumHandSide.LEFT && !descriptor.leftHandPositioned) {
                return false;
            }

            
            Positioner<Part, RenderContext<RenderableState>> positioner = descriptor.positioner;

            RenderContext<RenderableState> renderContext = new RenderContext<>(clientModContext, player, null);

            positioner.position(Part.MAIN, renderContext);
            // Gun position
//            GL11.glTranslatef(0f, 0.1f, -0f);
//            GL11.glRotatef(5f, 1f, 0f, 0f);
            if(handSide == CompatibleEnumHandSide.LEFT) {
                positioner.position(Part.LEFT_HAND, renderContext);
            } else if(handSide == null || handSide == CompatibleEnumHandSide.RIGHT) {
                // Right hand position
                positioner.position(Part.RIGHT_HAND, renderContext);
//                GL11.glRotatef(-5f, 1f, 0f, 0f);
            }
            
            GL11.glTranslatef(-0.35f, 0.1f, -0f);
            GL11.glRotatef(-378f, 1f, 0f, 0f);
            GL11.glRotatef(360f, 0f, 1f, 0f);
            GL11.glRotatef(0f, 0f, 0f, 1f);
        }
        return descriptor != null;
    }
}
