package com.vicmatskiv.weaponlib.tile;

import java.util.function.Consumer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.vicmatskiv.weaponlib.compatibility.CompatibleTileEntitySpecialRenderer;

import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

public class CustomTileEntityRenderer extends CompatibleTileEntitySpecialRenderer<CustomTileEntity<?>> {
    
    private ModelBase model;
    private ResourceLocation textureResource;
    private Consumer<TileEntity> positioning;

    public CustomTileEntityRenderer(ModelBase model, ResourceLocation textureResource,
            Consumer<TileEntity> positioning) {
        this.model = model;
        this.textureResource = textureResource;
        this.positioning = positioning;
    }

    @Override
    public void render(CustomTileEntity<?> tileEntity, double posX, double posY, double posZ, float partialTicks, int destroyStage,
            float alpha) {
        GL11.glPushMatrix();
        this.bindTexture(textureResource);
        
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glTranslatef((float)posX, (float)posY + 1.0F, (float)posZ + 1.0F);
        GL11.glScalef(1.0F, -1.0F, -1.0F);
        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
        GL11.glRotatef(90f * tileEntity.getSide(), 0, 1f, 0);
        GL11.glRotatef(-90f, 0, 1f, 0);

        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        
        GL11.glTranslatef(0F, -0.5F, 0F);
        
        //System.out.println("Rendering side " + ((CustomTileEntity<?>) tileEntity).getSide());
//        GL11.glRotatef(-90f * ((CustomTileEntity<?>) tileEntity).getSide(), 0, 1f, 0);
        positioning.accept(tileEntity);
        //GL11.glEnable(GL11.GL_CULL_FACE);
        model.render((Entity)null, 0f, 0f, 0f, 0f, 0f, 0.0625f);
        
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glPopMatrix();
    }
}
