package com.vicmatskiv.weaponlib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import com.vicmatskiv.weaponlib.compatibility.CompatibleItem;
import com.vicmatskiv.weaponlib.melee.PlayerMeleeInstance;

import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemAttachment<T> extends CompatibleItem implements ModelSource {

	private AttachmentCategory category;
	private String crosshair;
	private ApplyHandler<T> apply;
	private ApplyHandler<T> remove;
	protected ApplyHandler2<T> apply2;
	protected ApplyHandler2<T> remove2;
	protected MeleeWeaponApplyHandler<T> apply3;
	protected MeleeWeaponApplyHandler<T> remove3;
	private List<Tuple<ModelBase, String>> texturedModels = new ArrayList<>();
	private CustomRenderer<?> postRenderer;
	private CustomRenderer<?> preRenderer;
	private Part renderablePart;
	private String name;
	private Function<ItemStack, String> informationProvider;
	protected int maxStackSize = 1;

	private List<CompatibleAttachment<T>> attachments = new ArrayList<>();

	private List<Weapon> compatibleWeapons = new ArrayList<>();
	
	private List<ItemAttachment<T>> requiredAttachments = new ArrayList<>();

	protected String textureName;

	public static interface ApplyHandler<T> {
		public void apply(ItemAttachment<T> itemAttachment, T target, EntityLivingBase player);
	}

	public static interface ApplyHandler2<T> {
		public void apply(ItemAttachment<T> itemAttachment, PlayerWeaponInstance instance);
	}

	public static interface MeleeWeaponApplyHandler<T> {
        public void apply(ItemAttachment<T> itemAttachment, PlayerMeleeInstance instance);
    }

	protected ItemAttachment(String modId, AttachmentCategory category, ModelBase model, String textureName, String crosshair,
			ApplyHandler<T> apply, ApplyHandler<T> remove) {
		//this.modId = modId;
		this.category = category;
//		if(model != null) {
//			this.texturedModels.add(new Tuple<ModelBase, String>(model, textureName));
//		}
		this.textureName = textureName.toLowerCase();
		this.crosshair = crosshair != null ? modId + ":" + "textures/crosshairs/" + crosshair + ".png" : null;
		this.apply = apply;
		this.remove = remove;
	}

	protected ItemAttachment(String modId, AttachmentCategory category, String crosshair,
			ApplyHandler<T> apply, ApplyHandler<T> remove) {
		//this.modId = modId;
		this.category = category;
		this.crosshair = crosshair != null ? modId + ":" + "textures/crosshairs/" + crosshair + ".png" : null;
		this.apply = apply;
		this.remove = remove;
	}

	@Override
	public int getItemStackLimit() {
		return maxStackSize;
	}

	public Item setTextureName(String name) {
		return this;
	}

	public Part getRenderablePart() {
		return renderablePart;
	}

	protected void setRenderablePart(Part renderablePart) {
		this.renderablePart = renderablePart;
	}

	protected Function<ItemStack, String> getInformationProvider() {
		return informationProvider;
	}

	protected void setInformationProvider(
			Function<ItemStack, String> informationProvider) {
		this.informationProvider = informationProvider;
	}
	
	protected void setRequiredAttachments(List<ItemAttachment<T>> requiredAttachments) {
        this.requiredAttachments = Collections.unmodifiableList(requiredAttachments);
    }
	
	public List<ItemAttachment<T>> getRequiredAttachments() {
        return requiredAttachments;
    }

	@Deprecated
	public ItemAttachment<T> addModel(ModelBase model, String textureName) {
		texturedModels.add(new Tuple<>(model, textureName));
		return this;
	}

	public ItemAttachment(String modId, AttachmentCategory category, String crosshair) {
		this(modId, category, crosshair, (a, w, p) -> {}, (a, w, p) -> {});
	}

	public ItemAttachment(String modId, AttachmentCategory category, ModelBase attachment, String textureName, String crosshair) {
		this(modId, category, attachment, textureName, crosshair, (a, w, p) -> {}, (a, w ,p) -> {});
	}

	public AttachmentCategory getCategory() {
		return category;
	}

	public List<Tuple<ModelBase, String>> getTexturedModels() {
		return texturedModels;
	}

	public String getCrosshair() {
		return crosshair;
	}

	public ApplyHandler<T> getApply() {
		return apply;
	}

	public ApplyHandler<T> getRemove() {
		return remove;
	}

	public void addCompatibleWeapon(Weapon weapon) {
		compatibleWeapons.add(weapon);
	}

	@Override
    public void addInformation(ItemStack itemStack, List<String> info, boolean flag) {
		if(info != null && informationProvider != null) {
		    info.add(informationProvider.apply(itemStack));
		}
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPostRenderer(CustomRenderer<?> postRenderer) {
		this.postRenderer = postRenderer;
	}

	public CustomRenderer<?> getPostRenderer() {
		return postRenderer;
	}

	public CustomRenderer<?> getPreRenderer() {
		return preRenderer;
	}

	public void setPreRenderer(CustomRenderer<?> preRenderer) {
		this.preRenderer = preRenderer;
	}

	protected void addCompatibleAttachment(CompatibleAttachment<T> attachment) {
		attachments.add(attachment);
	}

	public List<CompatibleAttachment<T>> getAttachments() {
		return Collections.unmodifiableList(attachments);
	}

	@Override
	public String toString() {
		return name != null ? "Attachment [" + name + "]" : super.toString();
	}

	public ApplyHandler2<T> getApply2() {
		return apply2;
	}

	protected ApplyHandler2<T> getRemove2() {
		return remove2;
	}

    public MeleeWeaponApplyHandler<T> getApply3() {
        return apply3;
    }

    public MeleeWeaponApplyHandler<T> getRemove3() {
        return remove3;
    }

}