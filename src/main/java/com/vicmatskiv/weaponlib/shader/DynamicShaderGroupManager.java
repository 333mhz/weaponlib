package com.vicmatskiv.weaponlib.shader;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonSyntaxException;
import com.vicmatskiv.weaponlib.PlayerItemInstance;
import com.vicmatskiv.weaponlib.TransformingResourceManager;
import com.vicmatskiv.weaponlib.TransformingTextureManager;
import com.vicmatskiv.weaponlib.compatibility.Framebuffers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class DynamicShaderGroupManager {
    
    private static final Logger logger = LogManager.getLogger(DynamicShaderGroupManager.class);

    private static final String PATH_RESOURCES = "/com/vicmatskiv/weaponlib/resources/";
    private static final String PATH_SHADER_PROGRAMS = PATH_RESOURCES + "shaders/programs/";

    private static final String RESOURCE_DOMAIN_WEAPONLIB = "weaponlib";
    private static final String RESOURCE_DOMAIN_TEXTURES_EFFECT_WEAPONLIB = "textures/effect/" + RESOURCE_DOMAIN_WEAPONLIB;
    private static final String RESOURCE_DOMAIN_SHADERS_PROGRAM_WEAPONLIB = "shaders/program/" + RESOURCE_DOMAIN_WEAPONLIB;

    private static class LoadedShaderGroup {
        DynamicShaderGroup group;
        DynamicShaderPhase phase;
        Framebuffer mainFrambuffer;
        boolean confirmed;

    }

    private Map<UUID, LoadedShaderGroup> loaded = new LinkedHashMap<>();

    public boolean hasActiveGroups() {
        return loaded.size() > 0;
    }
 
//    public void applyShader(DynamicShaderContext shaderContext, DynamicShaderGroupSourceProvider shaderSourceProvider) {
//        DynamicShaderGroupSource source = shaderSourceProvider.getShaderSource(shaderContext.getPhase());
//        if(source != null) {
//            loadFromSource(shaderContext, source);
//        }
//    }

    public void applyShader(DynamicShaderContext shaderContext, ItemStack itemStack) {
        if(itemStack != null) {
            Item item = itemStack.getItem();
            if(item instanceof DynamicShaderGroupSourceProvider) {
                DynamicShaderGroupSourceProvider shaderSourceProvider = (DynamicShaderGroupSourceProvider)item;
                DynamicShaderGroupSource source = shaderSourceProvider.getShaderSource(shaderContext.getPhase());
                if(source != null) {
                    loadFromSource(shaderContext, source);
                }
            }
        }
    }

    public void applyShader(DynamicShaderContext shaderContext, PlayerItemInstance<?> instance) {
        DynamicShaderGroupSourceProvider shaderSourceProvider = null;
        if(instance != null) {
            if(instance instanceof DynamicShaderGroupSourceProvider) {
                shaderSourceProvider = (DynamicShaderGroupSourceProvider)instance;
            } else if(instance.getItem() instanceof DynamicShaderGroupSourceProvider) {
                shaderSourceProvider = (DynamicShaderGroupSourceProvider)instance.getItem();
            }
        }

        if(shaderSourceProvider != null) {
            DynamicShaderGroupSource source = shaderSourceProvider.getShaderSource(shaderContext.getPhase());
            if(source != null) {
                loadFromSource(shaderContext, source);
            }
        }
    }

    public DynamicShaderGroup loadFromSource(DynamicShaderContext context, DynamicShaderGroupSource source) {
        int originalFramebufferId = Framebuffers.getCurrentFramebuffer();

        // Remove all other shaders of the same phase but different source or different framebuffer
        for(Iterator<Entry<UUID, LoadedShaderGroup>> it = loaded.entrySet().iterator(); it.hasNext();) {
            Entry<UUID, LoadedShaderGroup> e = it.next();
            LoadedShaderGroup loaded = e.getValue();
            if(loaded.phase == context.getPhase() &&
                    (!source.getSourceId().equals(e.getKey()) || context.getMainFramebuffer() != loaded.mainFrambuffer)) {
                loaded.phase.remove(context, e.getValue().group);
                it.remove();
            }
        }

        LoadedShaderGroup l = loaded.compute(source.getSourceId(), (s, currentLoaded) -> {
            if(currentLoaded != null && !currentLoaded.group.isDeleted()) {
                currentLoaded.confirmed = true;
                return currentLoaded;
            } else {
                LoadedShaderGroup v = new LoadedShaderGroup();
                v.confirmed = true;
                v.group = createShaderGroup(context, source, source.getShaderLocation());
                v.phase = context.getPhase();
                v.mainFrambuffer = context.getMainFramebuffer();
                return v;
            }
        });
        if(l != null && l.group != null) {
            source.getUniforms(context).forEach(u -> {l.group.setUniform(u.getU(), u.getV().apply(context));});
            context.getPhase().apply(context, l.group);
        }

        Minecraft mc = Minecraft.getMinecraft();
        Framebuffers.bindFramebuffer(originalFramebufferId, true,
                mc.getFramebuffer().framebufferWidth,
                mc.getFramebuffer().framebufferHeight);

        return l != null ? l.group : null;
    }

    private DynamicShaderGroup createShaderGroup(DynamicShaderContext context, DynamicShaderGroupSource source, ResourceLocation resourceLocation) {

        Minecraft mc = Minecraft.getMinecraft();
        IResourceManager resourceManager = new TransformingResourceManager(mc.getResourceManager(),
                DynamicShaderGroupManager::modifyResourceLocation);

        TextureManager textureManager = new TransformingTextureManager(mc.getTextureManager(),
                DynamicShaderGroupManager::modifyResourceLocation);

        try {
            DynamicShaderGroup group = new DynamicShaderGroup(textureManager, resourceManager, context.getMainFramebuffer(), resourceLocation);
            group.createBindFramebuffers(mc.displayWidth, mc.displayHeight);
            return group;
        } catch (JsonSyntaxException | IOException e) {
            logger.error("Failed to create shader due to " + e, e);
            return null;
        }
    }

    public void removeStaleShaders(DynamicShaderContext context) {
        for(Iterator<Entry<UUID, LoadedShaderGroup>> it = loaded.entrySet().iterator(); it.hasNext();) {
            Entry<UUID, LoadedShaderGroup> e = it.next();

            LoadedShaderGroup loaded = e.getValue();
            if(!loaded.confirmed) {
                it.remove();
                if(e.getValue().group != null) {
                    loaded.phase.remove(context, e.getValue().group);
                }
            } else {
                e.getValue().confirmed = false;
            }
        }
    }

    public void removeAllShaders(DynamicShaderContext context) {
        for(Iterator<Entry<UUID, LoadedShaderGroup>> it = loaded.entrySet().iterator(); it.hasNext();) {
            Entry<UUID, LoadedShaderGroup> e = it.next();
            LoadedShaderGroup loaded = e.getValue();
            if(e.getValue().group != null) {
                loaded.phase.remove(context, e.getValue().group);
            }
            it.remove();
        }
    }

    private static ResourceLocation modifyResourceLocation(ResourceLocation resourceLocation) {
        ResourceLocation result;

        switch (resourceLocation.getResourceDomain()) {
        case RESOURCE_DOMAIN_WEAPONLIB:
            if(resourceLocation.getResourcePath().startsWith("shaders/program/")) {
                result = new ResourceLocation(RESOURCE_DOMAIN_WEAPONLIB, PATH_SHADER_PROGRAMS 
                        + resourceLocation.getResourcePath().substring(16));
            } else {
                result = resourceLocation;
            }
            
            break;
        case RESOURCE_DOMAIN_SHADERS_PROGRAM_WEAPONLIB:
            result = new ResourceLocation(RESOURCE_DOMAIN_WEAPONLIB, PATH_SHADER_PROGRAMS
                    + resourceLocation.getResourcePath());
            break;
        case RESOURCE_DOMAIN_TEXTURES_EFFECT_WEAPONLIB:
            result = new ResourceLocation(RESOURCE_DOMAIN_WEAPONLIB, PATH_RESOURCES + resourceLocation.getResourcePath());
            break;
        default:
            result = resourceLocation;
        }

        return result;
    }

}
