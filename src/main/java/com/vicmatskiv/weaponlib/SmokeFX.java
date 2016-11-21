package com.vicmatskiv.weaponlib;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class SmokeFX extends EntityFX {
	
	private static final double SMOKE_SCALE_FACTOR = 1.0005988079071D;
	
	private static final String DEFAULT_PARTICLES_TEXTURE = "textures/particle/particles.png";
	private static final String SMOKE_TEXTURE = "weaponlib:/com/vicmatskiv/weaponlib/resources/smoke.png";
		
	public SmokeFX(World par1World, double positionX, double positionY, double positionZ, float scale, 
			float motionX, float motionY, float motionZ)
	{
		super(par1World, positionX, positionY, positionZ, 0.0D, 0.0D, 0.0D);
		this.motionX = motionX;
		this.motionY = motionY;
		this.motionZ = motionZ;
		
		if (motionX == 0.0F) {
			motionX = 1.0F;
		}
		
		this.particleTextureIndexX = 0; 
		this.particleTextureIndexY = 0;
		this.particleRed = 1.0F;
		this.particleGreen = 1.0F;
		this.particleBlue = 1.0F;
		this.particleAlpha = 0.0F;
		this.particleScale *= 1.4F;
		this.particleScale *= scale;
		this.particleMaxAge = 50 + (int)(this.rand.nextFloat() * 30);
	}
	
	@Override
	public void onUpdate() {
		this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        if (this.particleAge++ >= this.particleMaxAge)
        {
            this.setDead();
        }

        this.motionY += 0.0005D;
        this.moveEntity(this.motionX, this.motionY, this.motionZ);
        
        this.motionX *= 0.599999785423279D;
        this.motionY *= 0.9999999785423279D;
        this.motionZ *= 0.599999785423279D;
        
        double alphaRadians = Math.PI / 4f + Math.PI * (float)this.particleAge / (float)this.particleMaxAge;
        this.particleAlpha = 0.2f * (float) Math.sin(alphaRadians > Math.PI ? Math.PI : alphaRadians);

        this.particleScale *= SMOKE_SCALE_FACTOR;
        
        if (this.onGround)
        {
            this.motionX *= 0.699999988079071D;
            this.motionZ *= 0.699999988079071D;
        }
	}
    
    @Override
    @SideOnly(Side.CLIENT)
    public void renderParticle(Tessellator tesselator, float par2, float par3, float par4, float par5, float par6, float par7)
    {
    	tesselator.draw();
    	
		Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation(SMOKE_TEXTURE));
    	tesselator.startDrawingQuads();
    	
    	tesselator.setBrightness(200);
    	
        float f10 = 0.1F * this.particleScale;

        float f11 = (float)(this.prevPosX + (this.posX - this.prevPosX) * (double)par2 - interpPosX);
        float f12 = (float)(this.prevPosY + (this.posY - this.prevPosY) * (double)par2 - interpPosY);
        float f13 = (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * (double)par2 - interpPosZ);
        
        tesselator.setColorRGBA_F(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha);
        
        tesselator.addVertexWithUV((double)(f11 - par3 * f10 - par6 * f10), (double)(f12 - par4 * f10), (double)(f13 - par5 * f10 - par7 * f10), 1, 1); //(double)f7, (double)f9); // a
        tesselator.addVertexWithUV((double)(f11 - par3 * f10 + par6 * f10), (double)(f12 + par4 * f10), (double)(f13 - par5 * f10 + par7 * f10), 1, 0); //(double)f7, (double)f8); // b
        tesselator.addVertexWithUV((double)(f11 + par3 * f10 + par6 * f10), (double)(f12 + par4 * f10), (double)(f13 + par5 * f10 + par7 * f10), 0, 0); //(double)f6, (double)f8); // c
        tesselator.addVertexWithUV((double)(f11 + par3 * f10 - par6 * f10), (double)(f12 - par4 * f10), (double)(f13 + par5 * f10 - par7 * f10), 0, 1); //(double)f6, (double)f9); // d
    	
    	tesselator.draw();
    	
    	Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation(DEFAULT_PARTICLES_TEXTURE));
    	tesselator.startDrawingQuads();
    }
}