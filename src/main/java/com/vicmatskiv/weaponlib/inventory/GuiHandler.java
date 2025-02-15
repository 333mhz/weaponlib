package com.vicmatskiv.weaponlib.inventory;

import static com.vicmatskiv.weaponlib.compatibility.CompatibilityProvider.compatibility;

import com.vicmatskiv.weaponlib.compatibility.CompatibleCustomPlayerInventoryCapability;
import com.vicmatskiv.weaponlib.compatibility.CompatibleGuiHandler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class GuiHandler extends CompatibleGuiHandler {

    public static final int STORAGE_ITEM_INVENTORY_GUI_ID = 1;
    public static final int CUSTOM_PLAYER_INVENTORY_GUI_ID = 2;
//    public static final int MISSIONS_MENU_GUI_ID = 3;

    @Override
    public Object getServerGuiElement(int guiId, EntityPlayer player, World world, int x, int y, int z) {
        Object container = null;
        switch (guiId) {
        case STORAGE_ITEM_INVENTORY_GUI_ID: {
            CustomPlayerInventory customInventory = CompatibleCustomPlayerInventoryCapability
                    .getInventory(player);
            if (customInventory != null && customInventory.getStackInSlot(0) != null) {
                container = new StorageItemContainer(player, player.inventory,
                        new StorageInventory(customInventory.getStackInSlot(0)));
            }
        }
            break;
        case CUSTOM_PLAYER_INVENTORY_GUI_ID:
            container = new CustomPlayerInventoryContainer(player, player.inventory,
                    CompatibleCustomPlayerInventoryCapability.getInventory(player));
            break;
        }
        return container;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Object getClientGuiElement(int guiId, EntityPlayer player, World world, int x, int y, int z) {
        Object guiContainer = null;
        switch (guiId) {
        case STORAGE_ITEM_INVENTORY_GUI_ID:
            CustomPlayerInventory customInventory = CompatibleCustomPlayerInventoryCapability
                    .getInventory(compatibility.getClientPlayer());
            if (customInventory != null && customInventory.getStackInSlot(0) != null) {
                guiContainer = new StorageItemGuiContainer((StorageItemContainer) new StorageItemContainer(player,
                        player.inventory, new StorageInventory(customInventory.getStackInSlot(0))));
            }
            break;
        case CUSTOM_PLAYER_INVENTORY_GUI_ID:
            guiContainer = new CustomPlayerInventoryGuiContainer(player, player.inventory,
                    CompatibleCustomPlayerInventoryCapability.getInventory(player));
            break;
//        case MISSIONS_MENU_GUI_ID:
//            guiContainer = new MissionsMenuGui();
//            break;
        }
        
        return guiContainer;
    }

}
