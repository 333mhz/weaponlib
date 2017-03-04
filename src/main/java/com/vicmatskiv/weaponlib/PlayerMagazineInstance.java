package com.vicmatskiv.weaponlib;

import com.vicmatskiv.weaponlib.network.TypeRegistry;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class PlayerMagazineInstance extends PlayerItemInstance<MagazineState> {
	
	static {
		TypeRegistry.getInstance().register(PlayerMagazineInstance.class);
	}
	
//	private int ammo;
		

	public PlayerMagazineInstance() {
		super();
	}

	public PlayerMagazineInstance(int itemInventoryIndex, EntityPlayer player, ItemStack itemStack) {
		super(itemInventoryIndex, player, itemStack);
	}

	public PlayerMagazineInstance(int itemInventoryIndex, EntityPlayer player) {
		super(itemInventoryIndex, player);
	}
	

//	public int getAmmo() {
//		return ammo;
//	}
//	
//	protected void setAmmo(int ammo) {
//		if(ammo != this.ammo) {
//			System.out.println("Updating instance with ammo " + ammo);
//			this.ammo = ammo;
//			this.updateId++;
//		}
//		
//	}
	
	@Override
	public void init(ByteBuf buf) {
		super.init(buf);
//		ammo = buf.readInt();
	}
	
	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
//		buf.writeInt(ammo);
	}
	
	@Override
	protected void updateWith(PlayerItemInstance<MagazineState> otherItemInstance, boolean updateManagedState) {
		super.updateWith(otherItemInstance, updateManagedState);
//		PlayerMagazineInstance otherMagazineInstance = (PlayerMagazineInstance) otherItemInstance;
//		setAmmo(otherMagazineInstance.ammo);
	}

	public ItemMagazine getMagazine() {
		return (ItemMagazine)item;
	}
}
