package com.jimholden.conomy.containers;

import com.jimholden.conomy.blocks.tileentity.TileEntityATM;
import com.jimholden.conomy.inventory.IInvCapa;
import com.jimholden.conomy.inventory.InvProvider;
import com.jimholden.conomy.items.LedgerBase;
import com.jimholden.conomy.items.OpenDimeBase;
import com.jimholden.conomy.vmwcompat.VMWSlotAccess;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiButtonImage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerLootBody extends Container {
	
	
	
	public ContainerLootBody(InventoryPlayer player, EntityPlayer target) {
		InventoryPlayer targetInventory = target.inventory;
		
		
		if(VMWSlotAccess.getVMWSlots(target) != null) {
			ItemStackHandler inv = VMWSlotAccess.getVMWSlots(target);
			this.addSlotToContainer(new SlotItemHandler(inv, 0, 184, -16));
			this.addSlotToContainer(new SlotItemHandler(inv, 1, 184, 2));
		}
		
		//ItemStackHandler handle = new ItemStackHandler(3);
		//this.addSlotToContainer(new SlotItemHandler(handle, 1, 96, 13));
		IInvCapa capa = target.getCapability(InvProvider.EXTRAINV, null);
	     this.addSlotToContainer(new SlotItemHandler(capa.getHandler(), 1, 96, 13));
		
		for(int y = 0; y < 3; y++)
		{
			for(int x = 0; x < 9; x++)
			{
				this.addSlotToContainer(new Slot(targetInventory, x + y*9 + 9, 8 + x*18, -16 + y*18));
				
			}
		}
		
		for(int x = 0; x < 9; x++)
		{
			this.addSlotToContainer(new Slot(targetInventory, x, 8 + x * 18, 41));
			
		}
		
		
		
		
		for(int y = 0; y < 3; y++)
		{
			for(int x = 0; x < 9; x++)
			{
				this.addSlotToContainer(new Slot(player, x + y*9 + 9, 8 + x*18, 74 + y*18));
				
			}
		}
		
		for(int x = 0; x < 9; x++)
		{
			this.addSlotToContainer(new Slot(player, x, 8 + x * 18, 135));
			
		}
		
	}
	
	@Override
	public NonNullList<ItemStack> getInventory() {
		// TODO Auto-generated method stub
		return super.getInventory();
	}
	
	@Override
	public void addListener(IContainerListener listener) {
		// TODO Auto-generated method stub
		super.addListener(listener);
		//listener.sendAllWindowProperties(this, this.tileentity);
	}
	
	
	
	
	@Override
	public void detectAndSendChanges() {
		// TODO Auto-generated method stub
		super.detectAndSendChanges();
		/*
		if(this.tileentity.getWorld().isRemote)
		{
			int x = this.tileentity.getPos().getX();
			int y = this.tileentity.getPos().getY();
			int z = this.tileentity.getPos().getZ();
			
			BlockPos pos = new BlockPos(x, y, z);
			TileEntity te = Minecraft.getMinecraft().world.getTileEntity(pos);
			
			ItemStack s = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).getStackInSlot(0);
			if(s.getItem() instanceof OpenDimeBase)
			{
				//System.out.println(s.getItem());
				((OpenDimeBase) this.inventorySlots.get(0).getStack().getItem()).setBalance(((OpenDimeBase) s.getItem()).getBalance(s), this.inventorySlots.get(0).getStack());
			}
			if(s.getItem() instanceof LedgerBase)
			{
				((LedgerBase) this.inventorySlots.get(0).getStack().getItem()).setBalance(((LedgerBase) s.getItem()).getBalance(s), this.inventorySlots.get(0).getStack());
			}
		}
		*/
		
		//System.out.println(this.handler.getStackInSlot(0).getItem());
		/*
		if(this.inventorySlots.get(0).getStack().getItem() instanceof OpenDimeBase) {
			System.out.println(((OpenDimeBase) this.handler.getStackInSlot(0).getItem()).getBalance(this.handler.getStackInSlot(0)));
		}
		*/
		//System.out.println((Ope))
		//System.out.println(this.inventorySlots);
		//System.out.println(this.inventorySlots.get(0).getStack().getItem());
		//if(this.inventorySlots.get(0).getStack().getItem() instanceof OpenDimeBase)
		//{
		//	((OpenDimeBase) this.inventorySlots.get(0).getStack().getItem()).setBalance(500, this.inventorySlots.get(0).getStack()); 
		//}
		
		
		//System.out.println("We think that it's " + this.tileentity.deviceBalance() + "We're lookin' at " + this.tileentity);
		//setDeviceBalance(this.tileentity.deviceBalance());
		/*System.out.println("ye: " + this.tileentity.getField(0));
		for(int i = 0; i < this.listeners.size(); ++i) 
		{
			IContainerListener listener = (IContainerListener)this.listeners.get(i);
			if(this.deviceBalance != this.tileentity.getField(0)) listener.sendWindowProperty(this, 0, this.tileentity.getField(0));
		}
		
		this.deviceBalance = this.tileentity.getField(0);
		*/
		
	}
	
	
	@Override
	public Slot getSlot(int slotId) {
		// TODO Auto-generated method stub
		return super.getSlot(slotId);
	}
	
	
	@Override
	public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) 
	{
		ItemStack stack = ItemStack.EMPTY;
		Slot slot = (Slot)this.inventorySlots.get(index);
		
		if(slot != null && slot.getHasStack()) 
		{
			ItemStack stack1 = slot.getStack();
			stack = stack1.copy();
			
			if(index == 3) 
			{
				if(!this.mergeItemStack(stack1, 4, 40, true)) return ItemStack.EMPTY;
				slot.onSlotChange(stack1, stack);
			}
			else if(index != 2 && index != 1 && index != 0) 
			{		
				Slot slot1 = (Slot)this.inventorySlots.get(index + 1);
			} 
			else if(!this.mergeItemStack(stack1, 4, 40, false)) 
			{
				return ItemStack.EMPTY;
			}
			if(stack1.isEmpty())
			{
				slot.putStack(ItemStack.EMPTY);
			}
			else
			{
				slot.onSlotChanged();

			}
			if(stack1.getCount() == stack.getCount()) return ItemStack.EMPTY;
			slot.onTake(playerIn, stack1);
		}
		return stack;
	}

	@Override
	public boolean canInteractWith(EntityPlayer playerIn) {
		// TODO Auto-generated method stub
		return true;
	}
	
	


}
