package com.vicmatskiv.weaponlib;

import org.lwjgl.opengl.GL11;

import com.vicmatskiv.weaponlib.compatibility.CompatibleEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

public class ShellCasingRenderer extends CompatibleEntityRenderer {

	@Override
	public void doCompatibleRender(Entity entity, double x, double y, double z, float yaw, float tick) {
		EntityShellCasing entityShellCasing = (EntityShellCasing) entity;
		Weapon weapon = entityShellCasing.getWeapon();
		if(weapon == null) {
    		return;
    	}
    	ModelBase model = weapon.getShellCasingModel();
    	if(model != null) {
    		String shellCasingTextureName = weapon.getShellCasingTextureName();
			ResourceLocation textureLocation = shellCasingTextureName != null ? new ResourceLocation(shellCasingTextureName) : null;
        	if(model != null) {
        		GL11.glPushMatrix();
        		if(textureLocation != null) {
        			bindTexture(textureLocation);
        		}
        		GL11.glTranslated(x, y, z);

        		float fov = Minecraft.getMinecraft().gameSettings.fovSetting;
        		/*
        		 * fov    scale
        		 *  30    0.01
        		 * 100    0.075
        		 *
        		 * 30 * x + y = 0.01
        		 * 100 * x + y = 0.075
        		 *
        		 * x = 0.001
        		 * y = -0.02
        		 *
        		 * scale = fov * 0.001 - 0.02
        		 */
        		float scale = (fov * 0.001f - 0.02f) * 0.6f;
        		GL11.glScalef(scale, scale, scale);


        		GL11.glRotatef(entityShellCasing.getXRotation(), 1f, 0f, 0f);
        		GL11.glRotatef(entityShellCasing.getYRotation(), 0f, 1f, 0f);
        		GL11.glRotatef(entityShellCasing.getZRotation(), 0f, 0f, 1f);
        		GL11.glRotatef(90, 0f, 0f, 1f);
        		//Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale)
        		model.render(entity, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F);
        		GL11.glPopMatrix();
        	}
    	}
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		WeaponSpawnEntity weaponSpawnEntity = (WeaponSpawnEntity) entity;
        return new ResourceLocation(weaponSpawnEntity.getWeapon().getAmmoModelTextureName());
	}


}