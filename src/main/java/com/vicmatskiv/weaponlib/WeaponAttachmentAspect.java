package com.vicmatskiv.weaponlib;

import static com.vicmatskiv.weaponlib.compatibility.CompatibilityProvider.compatibility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vicmatskiv.weaponlib.ItemAttachment.ApplyHandler2;
import com.vicmatskiv.weaponlib.network.TypeRegistry;
import com.vicmatskiv.weaponlib.state.Aspect;
import com.vicmatskiv.weaponlib.state.Permit;
import com.vicmatskiv.weaponlib.state.Permit.Status;
import com.vicmatskiv.weaponlib.state.PermitManager;
import com.vicmatskiv.weaponlib.state.StateManager;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class WeaponAttachmentAspect implements Aspect<WeaponState, PlayerWeaponInstance> {

	private static final Logger logger = LogManager.getLogger(WeaponAttachmentAspect.class);


	static {
		TypeRegistry.getInstance().register(EnterAttachmentModePermit.class);
		TypeRegistry.getInstance().register(ExitAttachmentModePermit.class);
		TypeRegistry.getInstance().register(ChangeAttachmentPermit.class);
	}

	private static class AttachmentLookupResult {
		CompatibleAttachment<Weapon> compatibleAttachment;
		int index = -1;
	}

	public static class EnterAttachmentModePermit extends Permit<WeaponState> {

		public EnterAttachmentModePermit() {}

		public EnterAttachmentModePermit(WeaponState state) {
			super(state);
		}
	}

	public static class ExitAttachmentModePermit extends Permit<WeaponState> {

		public ExitAttachmentModePermit() {}

		public ExitAttachmentModePermit(WeaponState state) {
			super(state);
		}
	}

	public static class ChangeAttachmentPermit extends Permit<WeaponState> {

		AttachmentCategory attachmentCategory;

		public ChangeAttachmentPermit() {}

		public ChangeAttachmentPermit(AttachmentCategory attachmentCategory) {
			super(WeaponState.NEXT_ATTACHMENT);
			this.attachmentCategory = attachmentCategory;
		}

		@Override
		public void init(ByteBuf buf) {
			super.init(buf);
			attachmentCategory = AttachmentCategory.values()[buf.readInt()];
		}

		@Override
		public void serialize(ByteBuf buf) {
			super.serialize(buf);
			buf.writeInt(attachmentCategory.ordinal());
		}
	}

	private ModContext modContext;
	private PermitManager permitManager;
	private StateManager<WeaponState, ? super PlayerWeaponInstance> stateManager;

	private long clickSpammingTimeout = 150;

	private Predicate<PlayerWeaponInstance> clickSpammingPreventer = es ->
		System.currentTimeMillis() >= es.getStateUpdateTimestamp() + clickSpammingTimeout;
		
    private Predicate<PlayerWeaponInstance> clickSpammingPreventer2 = es ->
        System.currentTimeMillis() >= es.getStateUpdateTimestamp() + clickSpammingTimeout * 2;

	private Collection<WeaponState> allowedUpdateFromStates = Arrays.asList(WeaponState.MODIFYING_REQUESTED);
    private static final int INVENTORY_SIZE = 36;

	WeaponAttachmentAspect(ModContext modContext) {
		this.modContext = modContext;
	}

	@Override
	public void setStateManager(StateManager<WeaponState, ? super PlayerWeaponInstance> stateManager) {

		if(permitManager == null) {
			throw new IllegalStateException("Permit manager not initialized");
		}

		this.stateManager = stateManager

		.in(this)
			.change(WeaponState.READY).to(WeaponState.MODIFYING)
			.when(clickSpammingPreventer)
			.withPermit((s, es) -> new EnterAttachmentModePermit(s),
					modContext.getPlayerItemInstanceRegistry()::update,
					permitManager)
			.manual()

		.in(this)
			.change(WeaponState.MODIFYING).to(WeaponState.READY)
			.when(clickSpammingPreventer2)
			.withAction((instance) -> {permitManager.request(new ExitAttachmentModePermit(WeaponState.READY),
					instance, (p, e) -> { /* do nothing on callback */});})
			.manual()

		.in(this)
			.change(WeaponState.MODIFYING).to(WeaponState.NEXT_ATTACHMENT)
			.when(clickSpammingPreventer)
			.withPermit(null,
					modContext.getPlayerItemInstanceRegistry()::update,
					permitManager)
			.manual()

		.in(this)
			.change(WeaponState.NEXT_ATTACHMENT).to(WeaponState.MODIFYING)
			.automatic()
		;
	}

	@Override
	public void setPermitManager(PermitManager permitManager) {
		this.permitManager = permitManager;
		permitManager.registerEvaluator(EnterAttachmentModePermit.class, PlayerWeaponInstance.class,
				this::enterAttachmentSelectionMode);
		permitManager.registerEvaluator(ExitAttachmentModePermit.class, PlayerWeaponInstance.class,
				this::exitAttachmentSelectionMode);
		permitManager.registerEvaluator(ChangeAttachmentPermit.class, PlayerWeaponInstance.class,
				this::changeAttachment);

	}

	public void toggleClientAttachmentSelectionMode(EntityPlayer player) {

		PlayerWeaponInstance weaponInstance = modContext.getPlayerItemInstanceRegistry().getMainHandItemInstance(player, PlayerWeaponInstance.class);
		if(weaponInstance != null) {
			stateManager.changeState(this, weaponInstance, WeaponState.MODIFYING, WeaponState.READY);
		}
	}

	void updateMainHeldItem(EntityPlayer player) {
		PlayerWeaponInstance instance = modContext.getPlayerItemInstanceRegistry().getMainHandItemInstance(player, PlayerWeaponInstance.class);
		if(instance != null) {
			stateManager.changeStateFromAnyOf(this, instance, allowedUpdateFromStates); // no target state specified, will trigger auto-transitions
		}
	}


	private void enterAttachmentSelectionMode(EnterAttachmentModePermit permit, PlayerWeaponInstance weaponInstance) {
		logger.debug("Entering attachment mode");
		byte selectedAttachmentIndexes[] = new byte[AttachmentCategory.values.length];
		Arrays.fill(selectedAttachmentIndexes, (byte)-1);
		weaponInstance.setSelectedAttachmentIndexes(selectedAttachmentIndexes);

		permit.setStatus(Status.GRANTED);
	}

	private void exitAttachmentSelectionMode(ExitAttachmentModePermit permit, PlayerWeaponInstance weaponInstance) {
		logger.debug("Exiting attachment mode");
		weaponInstance.setSelectedAttachmentIndexes(new byte[0]);

		permit.setStatus(Status.GRANTED);
	}

	List<CompatibleAttachment<? extends AttachmentContainer>> getActiveAttachments(EntityLivingBase player, ItemStack itemStack) {

		compatibility.ensureTagCompound(itemStack);

		List<CompatibleAttachment<? extends AttachmentContainer>> activeAttachments = new ArrayList<>();

		PlayerItemInstance<?> itemInstance = modContext.getPlayerItemInstanceRegistry()
				.getItemInstance(player, itemStack);


		int[] activeAttachmentsIds;
		if(!(itemInstance instanceof PlayerWeaponInstance)) {
			activeAttachmentsIds = new int[AttachmentCategory.values.length];
			for(CompatibleAttachment<Weapon> attachment: ((Weapon) itemStack.getItem()).getCompatibleAttachments().values()) {
				if(attachment.isDefault()) {
					activeAttachmentsIds[attachment.getAttachment().getCategory().ordinal()] = Item.getIdFromItem(attachment.getAttachment());
				}
			}
		} else {
			activeAttachmentsIds = ((PlayerWeaponInstance) itemInstance).getActiveAttachmentIds();
		}

		Weapon weapon = (Weapon) itemStack.getItem();

		for(int activeIndex: activeAttachmentsIds) {
			if(activeIndex == 0) continue; // is this a bug?
			Item item = Item.getItemById(activeIndex);
			if(item instanceof ItemAttachment) {
				CompatibleAttachment<? extends AttachmentContainer> compatibleAttachment = weapon.getCompatibleAttachments().get(item);
				if(compatibleAttachment != null) {
					activeAttachments.add(compatibleAttachment);
				}
			}

		}
		return activeAttachments;
	}

	void changeAttachment(AttachmentCategory attachmentCategory, PlayerWeaponInstance weaponInstance) {
		if(weaponInstance != null) {
			stateManager.changeState(this, weaponInstance, new ChangeAttachmentPermit(attachmentCategory),
					WeaponState.NEXT_ATTACHMENT);
		}
	}

	@SuppressWarnings("unchecked")
	private void changeAttachment(ChangeAttachmentPermit permit, PlayerWeaponInstance weaponInstance) {
	    if(!(weaponInstance.getPlayer() instanceof EntityPlayer)) {
	        return;
	    }
	    
	    EntityPlayer player = (EntityPlayer) weaponInstance.getPlayer();
	    
		AttachmentCategory attachmentCategory = permit.attachmentCategory;
		int[] originalActiveAttachmentIds = weaponInstance.getActiveAttachmentIds();
		int[] activeAttachmentIds = Arrays.copyOf(originalActiveAttachmentIds, originalActiveAttachmentIds.length);
		int activeAttachmentIdForThisCategory = activeAttachmentIds[attachmentCategory.ordinal()];
		ItemAttachment<Weapon> currentAttachment = null;
		if(activeAttachmentIdForThisCategory > 0) {
			currentAttachment = (ItemAttachment<Weapon>) Item.getItemById(activeAttachmentIdForThisCategory);
		}
		
		if(currentAttachment != null) {
		    CompatibleAttachment<Weapon> currentCompatibleAttachment = weaponInstance.getWeapon().getCompatibleAttachments().get(currentAttachment);
		    if(currentCompatibleAttachment.isPermanent()) {
		        return;
		    }
		    
		    if(isAttachmentInUse(currentCompatibleAttachment.getAttachment(), weaponInstance)) {
		        return;
		    }
		}
		

		AttachmentLookupResult lookupResult = next(attachmentCategory, currentAttachment, weaponInstance);
		
        if(currentAttachment != null) {
			// Need to apply removal functions first before applying addition functions
			if(currentAttachment.getRemove() != null) {
				currentAttachment.getRemove().apply(currentAttachment, weaponInstance.getWeapon(), player);
			}
			if(currentAttachment.getRemove2() != null) {
				currentAttachment.getRemove2().apply(currentAttachment, weaponInstance);
			}
		}

		if(lookupResult.index >= 0) {
			ItemStack slotItemStack = player.inventory.getStackInSlot(lookupResult.index);
			ItemAttachment<Weapon> nextAttachment = (ItemAttachment<Weapon>) slotItemStack.getItem();

			if(nextAttachment.getApply() != null) {
				nextAttachment.getApply().apply(nextAttachment, weaponInstance.getWeapon(), player);
			} else if(nextAttachment.getApply2() != null) {
				nextAttachment.getApply2().apply(nextAttachment, weaponInstance);
			} else if(lookupResult.compatibleAttachment.getApplyHandler() != null) {
				lookupResult.compatibleAttachment.getApplyHandler().apply(nextAttachment, weaponInstance);
			} else {
				ApplyHandler2<Weapon> handler = weaponInstance.getWeapon().getEquivalentHandler(attachmentCategory);
				if(handler != null) {
					handler.apply(null, weaponInstance);
				}
			}
			compatibility.consumeInventoryItemFromSlot(player, lookupResult.index);

			activeAttachmentIds[attachmentCategory.ordinal()] = Item.getIdFromItem(nextAttachment);
		} else if(weaponInstance.getWeapon().isCategoryRemovable(attachmentCategory)){
			activeAttachmentIds[attachmentCategory.ordinal()] = -1;
			ApplyHandler2<Weapon> handler = weaponInstance.getWeapon().getEquivalentHandler(attachmentCategory);
			if(handler != null) {
				handler.apply(null, weaponInstance);
			}
		} else {
		    return;
		}

		if(currentAttachment != null) {
			// Item must be added to the same spot the next attachment comes from or to any spot if there is no next attachment
			compatibility.addItemToPlayerInventory(player, currentAttachment, lookupResult.index);
		}

	    Tags.setAttachmentIds(weaponInstance.getItemStack(), activeAttachmentIds);
		weaponInstance.setActiveAttachmentIds(activeAttachmentIds);
	}

	private AttachmentLookupResult next(AttachmentCategory category, Item currentAttachment, PlayerWeaponInstance weaponInstance) {
		/*
		 * Start with selected index -1 (current attachment).
		 * Current index = selected index
		 * Iterate through the inventory until found a compatible attachment
		 * If hit the end, reset the counter to 0 and continue
		 *
		 * If had current attachment and no other attachment found, nothing changes
		 * e.g.
		 * 	   selectedIndex = -1, currentIndex starts with selectedIndex + 1 = 0
		 * 	   currentIndex: from 0 -> 36
		 *
		 * If had current attachment and other attachment found at index 10:
		 * e.g.
		 * 	   selectedIndex = -1
		 *     currentIndex: from 0 -> 10
		 *     		addItemToPlayerInventory
		 *     selectedIndex = 10
		 *
		 * when entering attachment mode, all selected indexes are set to -1
		 * selected indexes are really startSearchFromIndexes
		 */

		AttachmentLookupResult result = new AttachmentLookupResult();

		byte[] originallySelectedAttachmentIndexes = weaponInstance.getSelectedAttachmentIds();
		if(originallySelectedAttachmentIndexes == null || originallySelectedAttachmentIndexes.length != AttachmentCategory.values.length) {
			return result;
		}

		byte[] selectedAttachmentIndexes = Arrays.copyOf(originallySelectedAttachmentIndexes, originallySelectedAttachmentIndexes.length);
		int activeIndex = selectedAttachmentIndexes[category.ordinal()];


		boolean isCategoryRemovable = weaponInstance.getWeapon().isCategoryRemovable(category);
		result.index = -1;
		int offset = activeIndex + 1;
		
		int endIndex = isCategoryRemovable ? (INVENTORY_SIZE + 1): INVENTORY_SIZE;
		
		for(int i = 0; i < endIndex; i++) {
			// i = inventorySize corresponds to "no attachment"
			int currentIndex = i + offset;

			if(currentIndex >= INVENTORY_SIZE) {
				currentIndex -= INVENTORY_SIZE + (isCategoryRemovable ? 1 : 0);
			}

			logger.debug("Searching for an attachment in slot {}", currentIndex);

			if(currentIndex == -1) {
				result.index = -1;
				break;
			}

			ItemStack slotItemStack = ((EntityPlayer)weaponInstance.getPlayer()).inventory.getStackInSlot(currentIndex);
			if(slotItemStack != null && slotItemStack.getItem() instanceof ItemAttachment) {
				@SuppressWarnings("unchecked")
				ItemAttachment<Weapon> attachmentItemFromInventory = (ItemAttachment<Weapon>) slotItemStack.getItem();
				CompatibleAttachment<Weapon> compatibleAttachment;
				if(attachmentItemFromInventory.getCategory() == category
						&& (compatibleAttachment = weaponInstance.getWeapon().getCompatibleAttachments().get(attachmentItemFromInventory)) != null
						&& attachmentItemFromInventory != currentAttachment
						&& hasRequiredAttachments(compatibleAttachment.getAttachment(), weaponInstance)) {

					result.index = currentIndex;
					result.compatibleAttachment = compatibleAttachment;
					break;
				}
			}
		}

		selectedAttachmentIndexes[category.ordinal()] = (byte)result.index;
		weaponInstance.setSelectedAttachmentIndexes(selectedAttachmentIndexes);

		return result;
	}
	
	public static boolean hasRequiredAttachments(ItemAttachment<Weapon> attachmentItemFromInventory, PlayerWeaponInstance weaponInstance) {
	    List<ItemAttachment<Weapon>> requiredAttachments = attachmentItemFromInventory.getRequiredAttachments();
	    if(requiredAttachments.isEmpty()) {
	        return true;
	    }
	    boolean result = false;
	    for(int currentAttachmentId: weaponInstance.getActiveAttachmentIds()) {
	        Item attachmentItem = Item.getItemById(currentAttachmentId);
	        result = attachmentItem != null 
	                && requiredAttachments.contains(attachmentItem);
	        if(result) {
	            break;
	        }
	    }
	    return result;
	}
	
	private static boolean isAttachmentInUse(ItemAttachment<Weapon> attachmentItem, PlayerWeaponInstance weaponInstance) {
	    return isRequired(attachmentItem, weaponInstance);
	}
	
	private static boolean isRequired(ItemAttachment<Weapon> attachmentItem, PlayerWeaponInstance weaponInstance) {
	    boolean result = false;
	    for(int currentAttachmentId: weaponInstance.getActiveAttachmentIds()) {
	        Item otherAttachmentItem = Item.getItemById(currentAttachmentId);
	        if(otherAttachmentItem instanceof ItemAttachment) {
	            result = ((ItemAttachment<?>)otherAttachmentItem).getRequiredAttachments().contains(attachmentItem);
	            if(result) {
	                break;
	            }
	        }
	    }
	    return result;
	}


	@SuppressWarnings("unchecked")
	/**
	 * Adds the attachment to the weapon identified by the itemStack without removing the attachment from the inventory.
	 *
	 * @param nextAttachment
	 * @param itemStack
	 * @param player
	 */
	public static void addAttachment(ItemAttachment<Weapon> attachment, PlayerWeaponInstance weaponInstance) {

		int[] activeAttachmentsIds = weaponInstance.getActiveAttachmentIds();
		int activeAttachmentIdForThisCategory = activeAttachmentsIds[attachment.getCategory().ordinal()];
		ItemAttachment<Weapon> currentAttachment = null;
		if(activeAttachmentIdForThisCategory > 0) {
			currentAttachment = (ItemAttachment<Weapon>) Item.getItemById(activeAttachmentIdForThisCategory);
		}

		if(currentAttachment == null) {
			if(attachment != null) {
			    if(attachment.getApply() != null) {
			        attachment.getApply().apply(attachment, weaponInstance.getWeapon(), weaponInstance.getPlayer());
			    } else if(attachment.getApply2() != null) {
                    attachment.getApply2().apply(attachment, weaponInstance);
               }
			}
			activeAttachmentsIds[attachment.getCategory().ordinal()] = Item.getIdFromItem(attachment);;
		} else {
			System.err.println("Attachment of category " + attachment.getCategory() + " installed, remove it first");
		}
	}

	@SuppressWarnings("unchecked")
	/**
	 * Removes the attachment from the weapon identified by the itemStack without adding the attachment to the inventory.
	 *
	 * @param attachmentCategory
	 * @param itemStack
	 * @param player
	 * @return
	 */
	ItemAttachment<Weapon> removeAttachment(AttachmentCategory attachmentCategory, PlayerWeaponInstance weaponInstance) {

		int[] activeAttachmentIds = weaponInstance.getActiveAttachmentIds();
		int activeAttachmentIdForThisCategory = activeAttachmentIds[attachmentCategory.ordinal()];
		ItemAttachment<Weapon> currentAttachment = null;
		if(activeAttachmentIdForThisCategory > 0) {
			currentAttachment = (ItemAttachment<Weapon>) Item.getItemById(activeAttachmentIdForThisCategory);
			
			if(isAttachmentInUse(currentAttachment, weaponInstance)) {
			    return null;
			}
		}

		if(currentAttachment != null && currentAttachment.getRemove() != null) {
			currentAttachment.getRemove().apply(currentAttachment, weaponInstance.getWeapon(), weaponInstance.getPlayer());
		}

		if(currentAttachment != null) {
			activeAttachmentIds[attachmentCategory.ordinal()] = -1;
			weaponInstance.setActiveAttachmentIds(activeAttachmentIds);
		}

		return currentAttachment;
	}

	public static ItemAttachment<Weapon> getActiveAttachment(AttachmentCategory category, PlayerWeaponInstance weaponInstance) {


		ItemAttachment<Weapon> itemAttachment = null;

		int[] activeAttachmentIds = weaponInstance.getActiveAttachmentIds();

		for(int activeIndex: activeAttachmentIds) {
			if(activeIndex == 0) continue;
			Item item = Item.getItemById(activeIndex);
			if(item instanceof ItemAttachment) {
				CompatibleAttachment<Weapon> compatibleAttachment = weaponInstance.getWeapon().getCompatibleAttachments().get(item);
				if(compatibleAttachment != null && category == compatibleAttachment.getAttachment().getCategory()) {
					itemAttachment = compatibleAttachment.getAttachment();
					break;
				}
			}

		}
		return itemAttachment;
	}

	static boolean isActiveAttachment(ItemAttachment<Weapon> attachment, PlayerWeaponInstance weaponInstance) {
		int[] activeAttachmentIds = weaponInstance.getActiveAttachmentIds();
		return Arrays.stream(activeAttachmentIds).anyMatch((attachmentId) -> attachment == Item.getItemById(attachmentId));
	}

	boolean isSilencerOn(PlayerWeaponInstance weaponInstance) {
		int[] activeAttachmentsIds = weaponInstance.getActiveAttachmentIds();
		int activeAttachmentIdForThisCategory = activeAttachmentsIds[AttachmentCategory.SILENCER.ordinal()];
		return activeAttachmentIdForThisCategory > 0;
	}

	ItemAttachment<Weapon> getActiveAttachment(PlayerWeaponInstance weaponInstance, AttachmentCategory category) {
		return weaponInstance.getAttachmentItemWithCategory(category);
	}
}
