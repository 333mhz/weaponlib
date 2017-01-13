package com.vicmatskiv.weaponlib;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.model.ModelBase;

public class ItemBullet extends ItemAttachment<Weapon> {
	
	public static final class Builder extends AttachmentBuilder<Weapon> {
		
		private List<ItemMagazine> compatibleMagazines = new ArrayList<>();
		
		public void withCompatibleMagazine(ItemMagazine magazine) {
			compatibleMagazines.add(magazine);
		}
		
		@Override
		protected ItemAttachment<Weapon> createAttachment() {
			ItemBullet bullet = new ItemBullet(modId, AttachmentCategory.BULLET, model, textureName, null, null, null);
			bullet.compatibleMagazines = compatibleMagazines;
			return bullet;
		}
	}
	
	@SuppressWarnings("unused")
	private List<ItemMagazine> compatibleMagazines = new ArrayList<>();
	
	public ItemBullet(String modId, AttachmentCategory category, ModelBase model, String textureName, String crosshair,
			com.vicmatskiv.weaponlib.ItemAttachment.ApplyHandler<Weapon> apply,
			com.vicmatskiv.weaponlib.ItemAttachment.ApplyHandler<Weapon> remove) {
		super(modId, category, model, textureName, crosshair, apply, remove);
	}
	
//	@SuppressWarnings({ "rawtypes"})
//	@Override
//	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List info, boolean p_77624_4_) {
//		//info.add("Compatible guns:");
//
//		//compatibleMagazines.forEach((m) -> info.add(GameRegistry.));
//	}
}