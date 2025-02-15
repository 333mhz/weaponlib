package com.vicmatskiv.weaponlib;

import static com.vicmatskiv.weaponlib.compatibility.CompatibilityProvider.compatibility;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.vicmatskiv.weaponlib.compatibility.CompatibleItem;
import com.vicmatskiv.weaponlib.inventory.GuiHandler;

import net.minecraft.client.model.ModelBase;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class ItemStorage extends CompatibleItem implements ModelSource {
    
    public static class Builder {
        
        private static final int DEFAULT_GUI_TEXTURE_WIDTH = 176;
        
        private String name;
        private CreativeTabs tab;
        private ModelBase model;
        private String textureName;
        
        private Consumer<ItemStack> entityPositioning;
        private Consumer<ItemStack> inventoryPositioning;
        private BiConsumer<EntityPlayer, ItemStack> thirdPersonPositioning;
        private BiConsumer<EntityPlayer, ItemStack> customEquippedPositioning;
        private BiConsumer<EntityPlayer, ItemStack> firstPersonPositioning;
        private BiConsumer<ModelBase, ItemStack> firstPersonModelPositioning;
        private BiConsumer<ModelBase, ItemStack> thirdPersonModelPositioning;
        private BiConsumer<ModelBase, ItemStack> inventoryModelPositioning;
        private BiConsumer<ModelBase, ItemStack> entityModelPositioning;
        private Consumer<RenderContext<RenderableState>> firstPersonLeftHandPositioning;
        private Consumer<RenderContext<RenderableState>> firstPersonRightHandPositioning;
        private int size;
        private String guiTextureName;
        private int guiTextureWidth = DEFAULT_GUI_TEXTURE_WIDTH;
        
        private Predicate<Item> validItemPredicate = item -> true;
        
        public Builder withName(String name) {
            this.name = name;
            return this;
        }
        
        public Builder withSize(int size) {
            this.size = size;
            return this;
        }
        
        public Builder withValidItemPredicate(Predicate<Item> validItemPredicate) {
            this.validItemPredicate = validItemPredicate;
            return this;
        }
        
        public Builder withTab(CreativeTabs tab) {
            this.tab = tab;
            return this;
        }
        
        public Builder withModel(ModelBase model) {
            this.model = model;
            return this;
        }
        
        public Builder withGuiTextureName(String guiTextureName) {
            this.guiTextureName = guiTextureName;
            return this;
        }
        
        public Builder withGuiTextureWidth(int guiTextureWidth) {
            this.guiTextureWidth = guiTextureWidth;
            return this;
        }
        
        public Builder withModelTextureName(String textureName) {
            this.textureName = textureName;
            return this;
        }
        
        public Builder withEntityPositioning(Consumer<ItemStack> entityPositioning) {
            this.entityPositioning = entityPositioning;
            return this;
        }

        public Builder withInventoryPositioning(Consumer<ItemStack> inventoryPositioning) {
            this.inventoryPositioning = inventoryPositioning;
            return this;
        }

        public  Builder withThirdPersonPositioning(BiConsumer<EntityPlayer, ItemStack> thirdPersonPositioning) {
            this.thirdPersonPositioning = thirdPersonPositioning;
            return this;
        }
        
        public  Builder withCustomEquippedPositioning(BiConsumer<EntityPlayer, ItemStack> customEquippedPositioning) {
            this.customEquippedPositioning = customEquippedPositioning;
            return this;
        }

        public Builder withFirstPersonPositioning(BiConsumer<EntityPlayer, ItemStack> firstPersonPositioning) {
            this.firstPersonPositioning = firstPersonPositioning;
            return this;
        }

        public Builder withFirstPersonModelPositioning(BiConsumer<ModelBase, ItemStack> firstPersonModelPositioning) {
            this.firstPersonModelPositioning = firstPersonModelPositioning;
            return this;
        }

        public Builder withEntityModelPositioning(BiConsumer<ModelBase, ItemStack> entityModelPositioning) {
            this.entityModelPositioning = entityModelPositioning;
            return this;
        }

        public Builder withInventoryModelPositioning(BiConsumer<ModelBase, ItemStack> inventoryModelPositioning) {
            this.inventoryModelPositioning = inventoryModelPositioning;
            return this;
        }

        public Builder withThirdPersonModelPositioning(BiConsumer<ModelBase, ItemStack> thirdPersonModelPositioning) {
            this.thirdPersonModelPositioning = thirdPersonModelPositioning;
            return this;
        }

        public Builder withFirstPersonHandPositioning(
                Consumer<RenderContext<RenderableState>> leftHand,
                Consumer<RenderContext<RenderableState>> rightHand)
        {
            this.firstPersonLeftHandPositioning = leftHand;
            this.firstPersonRightHandPositioning = rightHand;
            return this;
        }

        private static class RendererRegistrationHelper {
            private static Object registerRenderer(Builder builder, ModContext modContext) {
                return new StaticModelSourceRenderer.Builder()
                .withHiddenInventory(builder.tab == null)
                .withEntityPositioning(builder.entityPositioning)
                .withFirstPersonPositioning(builder.firstPersonPositioning)
                .withThirdPersonPositioning(builder.thirdPersonPositioning)
                .withCustomEquippedPositioning(builder.customEquippedPositioning)
                .withInventoryPositioning(builder.inventoryPositioning)
                .withEntityModelPositioning(builder.entityModelPositioning)
                .withFirstPersonModelPositioning(builder.firstPersonModelPositioning)
                .withThirdPersonModelPositioning(builder.thirdPersonModelPositioning)
                .withInventoryModelPositioning(builder.inventoryModelPositioning)
                .withFirstPersonHandPositioning(builder.firstPersonLeftHandPositioning, builder.firstPersonRightHandPositioning)
                .withModContext(modContext)
                .withModId(modContext.getModId())
                .build();
            }
        }
       
        public ItemStorage build(ModContext modContext) {
            if(name == null) {
                throw new IllegalStateException("ItemStorage name not set");
            }
            
            if(size <= 0) {
                throw new IllegalStateException("ItemStorage size must be greater than 0");
            }
            
            if(guiTextureName == null) {
                throw new IllegalStateException("ItemStorage gui texture not set");
            }
            
            if(!guiTextureName.startsWith("textures/gui/")) {
                guiTextureName = "textures/gui/" + guiTextureName;
            }
            ResourceLocation guiTextureLocation = new ResourceLocation(modContext.getModId(), 
                    addFileExtension(guiTextureName, ".png"));
            
            ItemStorage item = new ItemStorage(modContext, size, validItemPredicate, guiTextureLocation, this.guiTextureWidth);
            
            item.setUnlocalizedName(modContext.getModId() + "_" + name);
            
            if(model != null) {
                item.texturedModels.add(new Tuple<>(model, addFileExtension(textureName, ".png")));
            }
            
            if(tab != null) {
                item.setCreativeTab(tab);
            }
            
            modContext.registerRenderableItem(name, item, compatibility.isClientSide() ? RendererRegistrationHelper.registerRenderer(this, modContext) : null);
            
            return item;
        }
    }
    
    
    private List<Tuple<ModelBase, String>> texturedModels = new ArrayList<>();
    private int size;
    private ResourceLocation guiTextureLocation;
    private int guiTextureWidth;
    private Predicate<Item> validItemPredicate;
    
    public ItemStorage(ModContext context, int size,
            Predicate<Item> validItemPredicate,
            ResourceLocation guiTextureLocation, 
            int guiTextureWidth) {
        this.validItemPredicate = validItemPredicate;
        this.size = size;
        this.guiTextureLocation = guiTextureLocation;
        this.guiTextureWidth = guiTextureWidth;
    }

    // Without this method, your inventory will NOT work!!!
    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 1; // return any value greater than zero
    }
    
//    protected ItemStack onCompatibleItemRightClick(ItemStack itemStack, World world, EntityPlayer player, boolean mainHand) {
//        if (!world.isRemote) {
//            // If player not sneaking, open the inventory gui
//            if (!player.isSneaking()) {
//                player.openGui(context.getMod(), GuiHandler.STORAGE_ITEM_INVENTORY_GUI_ID, world, 0, 0, 0);
//            }
//        }
//        return itemStack;
//    }
    
//    @SideOnly(Side.CLIENT)
//    public void registerIcons(IIconRegister iconRegister) {
//        this.itemIcon = iconRegister.registerIcon(this.getIconString());
//    }
    
    @Override
    public List<Tuple<ModelBase, String>> getTexturedModels() {
        return texturedModels;
    }

    @Override
    public CustomRenderer<?> getPostRenderer() {
        return null;
    }
    
    public int getSize() {
        return size;
    }
    
    public ResourceLocation getGuiTextureLocation() {
        return guiTextureLocation;
    }
    
    private static String addFileExtension(String s, String ext) {
        return s != null && !s.endsWith(ext) ? s + ext : s;
    }

    public int getGuiTextureWidth() {
        return guiTextureWidth;
    }

    public Predicate<Item> getValidItemPredicate() {
        return validItemPredicate;
    }
    
}