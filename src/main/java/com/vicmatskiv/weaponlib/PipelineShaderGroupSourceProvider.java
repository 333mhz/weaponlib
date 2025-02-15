package com.vicmatskiv.weaponlib;

import static com.vicmatskiv.weaponlib.compatibility.CompatibilityProvider.compatibility;

import java.util.UUID;

import com.vicmatskiv.weaponlib.SpreadableExposure.Blackout;
import com.vicmatskiv.weaponlib.compatibility.CompatibleExposureCapability;
import com.vicmatskiv.weaponlib.shader.DynamicShaderGroupSource;
import com.vicmatskiv.weaponlib.shader.DynamicShaderGroupSourceProvider;
import com.vicmatskiv.weaponlib.shader.DynamicShaderPhase;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

class PipelineShaderGroupSourceProvider implements DynamicShaderGroupSourceProvider {
    
    private boolean flashEnabled;
    private boolean nightVisionEnabled;
    private boolean blurEnabled;
    private boolean vignetteEnabled;
    private float sepiaRatio;
    private float spreadableExposureProgress;
    private float vignetteRadius;
    private float brightness;
    private SpreadableExposure spreadableExposure;
    private LightExposure lightExposure;
    private float colorImpairmentR;
    private float colorImpairmentG;
    private float colorImpairmentB;
    	    
    final DynamicShaderGroupSource source = new DynamicShaderGroupSource(UUID.randomUUID(),
            new ResourceLocation("weaponlib:/com/vicmatskiv/weaponlib/resources/post-processing-pipeline.json"))
                .withUniform("NightVisionEnabled", context -> nightVisionEnabled ? 1.0f : 0.0f)
                .withUniform("BlurEnabled", context -> blurEnabled ? 1.0f : 0.0f)
                .withUniform("BlurVignetteRadius", context -> 0.0f)
                .withUniform("Radius", context -> 10f)
                .withUniform("Progress", context -> spreadableExposureProgress)
                .withUniform("VignetteEnabled", context -> vignetteEnabled ? 1.0f : 0.0f)
                .withUniform("VignetteRadius", context -> vignetteRadius)
                .withUniform("Brightness", context -> brightness)
                .withUniform("SepiaRatio", context -> sepiaRatio)
                .withUniform("SepiaColor", context -> new float[] {colorImpairmentR, colorImpairmentG, colorImpairmentB})
                .withUniform("IntensityAdjust", context -> 40f - Minecraft.getMinecraft().gameSettings.gammaSetting * 38)
                .withUniform("NoiseAmplification", context ->  2f + 3f * Minecraft.getMinecraft().gameSettings.gammaSetting);
    
    @Override
    public DynamicShaderGroupSource getShaderSource(DynamicShaderPhase phase) {
        lightExposure = CompatibleExposureCapability.getExposure(compatibility.clientPlayer(), LightExposure.class);
        spreadableExposure = CompatibleExposureCapability.getExposure(compatibility.clientPlayer(), SpreadableExposure.class);
        spreadableExposureProgress = MiscUtils.smoothstep(0, 1, spreadableExposure != null ? spreadableExposure.getTotalDose() : 0f);
        updateNightVision();
        updateVignette();
        updateBlur();
        updateSepia();
        updateBrightness();
        spreadableExposure = null;
        lightExposure = null;
        return nightVisionEnabled || blurEnabled || vignetteEnabled || sepiaRatio > 0 || flashEnabled ?
                source : null;
    }
    
    private void updateBrightness() {
        brightness = 1f;

//        System.out.println("Hello");
        long worldTime = compatibility.world(compatibility.clientPlayer()).getWorldTime();
//        System.out.println("Day brightness: " + dayBrightness + ", time: " + (worldTime % 24000));
        if(lightExposure != null && lightExposure.getTotalDose() > 0.0003f) { //lightExposure.isEffective(compatibility.world(compatibility.clientPlayer()))) {
            flashEnabled = true;
            float dayBrightness = (MathHelper.sin( (float)Math.PI * 2 * (float)(worldTime % 24000 - 24000f) / 24000f) + 1f) / 2f;
//            dayBrightness *= dayBrightness;
            brightness = 1f + (100f + (1 - dayBrightness) * 100f) * lightExposure.getTotalDose() ;
//            System.out.println("Brightness: " + brightness);
        }
        
        if(spreadableExposure != null && !compatibility.clientPlayer().isDead) {
            Blackout blackout = spreadableExposure.getBlackout();
            blackout.update();
            switch(blackout.getPhase()) {
            case ENTER:
                brightness = 1f - blackout.getEnterProgress();
                break;
            case EXIT:
                brightness = blackout.getExitProgress();
                break;
            case DARK:
                brightness = 0f;
                break;
            case NONE:
                brightness = 1f;
                break;
            }
        }
    }

    private void updateBlur() {
        blurEnabled = spreadableExposureProgress > 0.01f; // TODO: set min
    }

    private void updateVignette() {
        vignetteEnabled = nightVisionEnabled;
        ItemStack helmetStack = compatibility.getHelmet();
        if(nightVisionEnabled && helmetStack != null && helmetStack.getItem() instanceof CustomArmor) {
            CustomArmor helmet = (CustomArmor)helmetStack.getItem();
            vignetteEnabled = helmet.isVignetteEnabled();
        }
        vignetteRadius = 0.55f;            
    }

    private void updateNightVision() {
        ItemStack helmetStack = compatibility.getHelmet();
        if(helmetStack != null) {
            NBTTagCompound tagCompound = compatibility.getTagCompound(helmetStack);
            if(tagCompound != null) {
                nightVisionEnabled = tagCompound.getBoolean("nv");
            } else {
                nightVisionEnabled = false;
            }
        } else {
            nightVisionEnabled = false;
        }
    }
    
    private void updateSepia() {
        sepiaRatio = spreadableExposureProgress;
        if(spreadableExposure != null) {
            colorImpairmentR = spreadableExposure.getColorImpairmentR();
            colorImpairmentG = spreadableExposure.getColorImpairmentG();
            colorImpairmentB = spreadableExposure.getColorImpairmentB();
        }
    }

}