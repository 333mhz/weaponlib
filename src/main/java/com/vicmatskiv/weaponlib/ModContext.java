package com.vicmatskiv.weaponlib;

import com.vicmatskiv.weaponlib.compatibility.CompatibleChannel;
import com.vicmatskiv.weaponlib.compatibility.CompatibleSound;
import com.vicmatskiv.weaponlib.crafting.RecipeGenerator;

import net.minecraft.item.Item;

public interface ModContext {
	
	public void init(Object mod, String modId, CompatibleChannel channel);

	public void registerWeapon(String name, Weapon weapon, WeaponRenderer renderer);
	
	public CompatibleChannel getChannel();
	
	public void runSyncTick(Runnable runnable);
		
	public void registerRenderableItem(String name, Item weapon, Object renderer);

	//TODO: append mod id in 1.7.10
	public CompatibleSound registerSound(String sound);

	public void runInMainThread(Runnable runnable);

	public PlayerItemInstanceRegistry getPlayerItemInstanceRegistry();

	public WeaponReloadAspect getWeaponReloadAspect();
	
	public WeaponFireAspect getWeaponFireAspect();
	
	public WeaponAttachmentAspect getAttachmentAspect();

	public MagazineReloadAspect getMagazineReloadAspect();
	
	public PlayerWeaponInstance getMainHeldWeapon();

	public StatusMessageCenter getStatusMessageCenter();
	
	public RecipeGenerator getRecipeGenerator();

}
