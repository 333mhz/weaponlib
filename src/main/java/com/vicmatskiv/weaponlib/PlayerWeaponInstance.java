package com.vicmatskiv.weaponlib;

import static com.vicmatskiv.weaponlib.compatibility.CompatibilityProvider.compatibility;

import java.util.Arrays;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingDeque;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vicmatskiv.weaponlib.network.TypeRegistry;
import com.vicmatskiv.weaponlib.perspective.OpticalScopePerspective;
import com.vicmatskiv.weaponlib.perspective.Perspective;
import com.vicmatskiv.weaponlib.shader.DynamicShaderGroupSource;
import com.vicmatskiv.weaponlib.shader.DynamicShaderGroupSourceProvider;
import com.vicmatskiv.weaponlib.shader.DynamicShaderPhase;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;


public class PlayerWeaponInstance extends PlayerItemInstance<WeaponState> implements DynamicShaderGroupSourceProvider {


    private static final int SERIAL_VERSION = 9;

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger(PlayerWeaponInstance.class);

    static {
        TypeRegistry.getInstance().register(PlayerWeaponInstance.class);
    }

    private static final UUID NIGHT_VISION_SOURCE_UUID = UUID.randomUUID();
    private static final UUID VIGNETTE_SOURCE_UUID = UUID.randomUUID();
    private static final UUID BLUR_SOURCE_UUID = UUID.randomUUID();

    public final DynamicShaderGroupSource BLUR_SOURCE = new DynamicShaderGroupSource(BLUR_SOURCE_UUID,
            new ResourceLocation("weaponlib:/com/vicmatskiv/weaponlib/resources/blur.json"))
            .withUniform("Radius", context -> hasOpticScope() ? 10f : 5f)
            .withUniform("Progress", context -> getAimChangeProgress());

    public final DynamicShaderGroupSource NIGHT_VISION_SOURCE = new DynamicShaderGroupSource(NIGHT_VISION_SOURCE_UUID,
            new ResourceLocation("weaponlib:/com/vicmatskiv/weaponlib/resources/night-vision.json"))
            .withUniform("IntensityAdjust", context -> 40f - Minecraft.getMinecraft().gameSettings.gammaSetting * 38)
            .withUniform("NoiseAmplification", context ->  2f + 3f * Minecraft.getMinecraft().gameSettings.gammaSetting);

    public final DynamicShaderGroupSource VIGNETTE_SOURCE = new DynamicShaderGroupSource(VIGNETTE_SOURCE_UUID,
            new ResourceLocation("weaponlib:/com/vicmatskiv/weaponlib/resources/vignette.json"))
            .withUniform("Radius", context -> getOpticScopeVignetteRadius(context.getPartialTicks()));

    private static final long AIM_CHANGE_DURATION = 1200;

	private int ammo;
	private float recoil;
	private int seriesShotCount;
	private long lastFireTimestamp;
	private boolean aimed;
	private int maxShots;
	private float zoom = 1f;
	private byte activeTextureIndex;
	private boolean laserOn;
	private long aimChangeTimestamp;
	private boolean nightVisionOn;
	private boolean seriesResetAllowed;
	private long lastBurstEndTimestamp;
	private boolean altModificationModeEnabled;
	
	private int loadIterationCount;
	private boolean loadAfterUnloadEnabled;

	/*
	 * Upon adding an element to the head of the queue, all existing elements with lower priority are removed
	 * from the queue. Elements with the same priority are not removed.
	 * This ensures the queue is always sorted by priority, lowest (head) to highest (tail).
	 */
	private Deque<AsyncWeaponState> filteredStateQueue = new LinkedBlockingDeque<>();
	private int[] activeAttachmentIds = new int[0];
	private byte[] selectedAttachmentIndexes = new byte[0];

	public PlayerWeaponInstance() {
		super();
	}


    public PlayerWeaponInstance(int itemInventoryIndex, EntityLivingBase player, ItemStack itemStack) {
		super(itemInventoryIndex, player, itemStack);
	}

	public PlayerWeaponInstance(int itemInventoryIndex, EntityLivingBase player) {
		super(itemInventoryIndex, player);
	}

	@Override
	protected int getSerialVersion() {
		return SERIAL_VERSION;
	}

	private void addStateToHistory(WeaponState state) {
		AsyncWeaponState t;
		// Remove existing items from lower priorities from the top of the stack; stop when same or higher priority item is found
		while((t = filteredStateQueue.peekFirst()) != null) {
			if(t.getState().getPriority() < state.getPriority()) {
				filteredStateQueue.pollFirst();
			} else {
				break;
			}
		}

		long expirationTimeout;

		if(state == WeaponState.FIRING || state == WeaponState.RECOILED || state == WeaponState.PAUSED) {
			if(isAutomaticModeEnabled() && !getWeapon().hasRecoilPositioning()) {
				expirationTimeout = (long) (50f / getFireRate());
			} else {
				expirationTimeout = 500;
			}
			expirationTimeout = 500;
		} else {
			expirationTimeout = Integer.MAX_VALUE;
		}
		filteredStateQueue.addFirst(new AsyncWeaponState(state, this.stateUpdateTimestamp, expirationTimeout));
	}

	@Override
	public boolean setState(WeaponState state) {
		boolean result = super.setState(state);
		addStateToHistory(state);
		return result;
	}

	public AsyncWeaponState nextHistoryState() {
		AsyncWeaponState result = filteredStateQueue.pollLast();
		if(result == null) {
		    result = new AsyncWeaponState(getState(), stateUpdateTimestamp);
		}
		return result;
	}

	public int getAmmo() {
		return ammo;
	}

	public void setAmmo(int ammo) {
		if(ammo != this.ammo) {
			this.ammo = ammo;
			markDirty();
		}
	}

	@Override
	public void init(ByteBuf buf) {
		super.init(buf);
		activeAttachmentIds = initIntArray(buf);
		selectedAttachmentIndexes = initByteArray(buf);
		ammo = buf.readInt();
		aimed = buf.readBoolean();
		recoil = buf.readFloat();
		maxShots = buf.readInt();
		zoom = buf.readFloat();
		activeTextureIndex = buf.readByte();
		laserOn = buf.readBoolean();
		nightVisionOn = buf.readBoolean();
		loadIterationCount = buf.readInt();
		loadAfterUnloadEnabled = buf.readBoolean();
		altModificationModeEnabled = buf.readBoolean();
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		serializeIntArray(buf, activeAttachmentIds);
		serializeByteArray(buf, selectedAttachmentIndexes);
		buf.writeInt(ammo);
		buf.writeBoolean(aimed);
		buf.writeFloat(recoil);
		buf.writeInt(maxShots);
		buf.writeFloat(zoom);
		buf.writeByte(activeTextureIndex);
		buf.writeBoolean(laserOn);
		buf.writeBoolean(nightVisionOn);
		buf.writeInt(loadIterationCount);
		buf.writeBoolean(loadAfterUnloadEnabled);
		buf.writeBoolean(altModificationModeEnabled);
	}

	private static void serializeIntArray(ByteBuf buf, int a[]) {
		buf.writeByte(a.length);
		for(int i = 0; i < a.length; i++) {
			buf.writeInt(a[i]);
		}
	}

	private static void serializeByteArray(ByteBuf buf, byte a[]) {
		buf.writeByte(a.length);
		for(int i = 0; i < a.length; i++) {
			buf.writeByte(a[i]);
		}
	}

	private static int[] initIntArray(ByteBuf buf) {
		int length = buf.readByte();
		int a[] = new int[length];
		for(int i = 0; i < length; i++) {
			a[i] = buf.readInt();
		}
		return a;
	}

	private static byte[] initByteArray(ByteBuf buf) {
		int length = buf.readByte();
		byte a[] = new byte[length];
		for(int i = 0; i < length; i++) {
			a[i] = buf.readByte();
		}
		return a;
	}

	@Override
	protected void updateWith(PlayerItemInstance<WeaponState> otherItemInstance, boolean updateManagedState) {
		super.updateWith(otherItemInstance, updateManagedState);
		PlayerWeaponInstance otherWeaponInstance = (PlayerWeaponInstance) otherItemInstance;

		setAmmo(otherWeaponInstance.ammo);
		setZoom(otherWeaponInstance.zoom);
		setRecoil(otherWeaponInstance.recoil);
		setSelectedAttachmentIndexes(otherWeaponInstance.selectedAttachmentIndexes);
		setActiveAttachmentIds(otherWeaponInstance.activeAttachmentIds);
		setActiveTextureIndex(otherWeaponInstance.activeTextureIndex);
		setLaserOn(otherWeaponInstance.laserOn);
		setMaxShots(otherWeaponInstance.maxShots);
		setLoadIterationCount(otherWeaponInstance.loadIterationCount);
		setLoadAfterUnloadEnabled(otherWeaponInstance.loadAfterUnloadEnabled);
	}

	public Weapon getWeapon() {
		return (Weapon)item;
	}

	public float getRecoil() {
		return recoil;
	}

	public void setRecoil(float recoil) {
		if(recoil != this.recoil) {
			this.recoil = recoil;
			markDirty();
		}
	}
	
	public boolean isLoadAfterUnloadEnabled() {
        return loadAfterUnloadEnabled;
    }
	
	public void setLoadAfterUnloadEnabled(boolean loadAfterUnloadEnabled) {
        this.loadAfterUnloadEnabled = loadAfterUnloadEnabled;
    }

	public int getMaxShots() {
		return maxShots;
	}

	void setMaxShots(int maxShots) {
		if(this.maxShots != maxShots) {
			this.maxShots = maxShots;
			markDirty();
		}
	}

	public int getSeriesShotCount() {
	    //System.out.println("Series shot count: " + seriesShotCount);
		return seriesShotCount;
	}

	public void setSeriesShotCount(int seriesShotCount) {
		this.seriesShotCount = seriesShotCount;
	}

	public long getLastFireTimestamp() {
		return lastFireTimestamp;
	}

	public void setLastFireTimestamp(long lastFireTimestamp) {
		this.lastFireTimestamp = lastFireTimestamp;
	}

	public void resetCurrentSeries() {
		seriesShotCount = 0;
		seriesResetAllowed = false;
	}
	
	public void setLastBurstEndTimestamp(long lastBurstEndTimestamp) {
        this.lastBurstEndTimestamp = lastBurstEndTimestamp;
    }
	
	public long getLastBurstEndTimestamp() {
	    return lastBurstEndTimestamp;
	}
	
	public void setSeriesResetAllowed(boolean seriesResetAllowed) {
	    this.seriesResetAllowed = seriesResetAllowed;
	}
	
	public boolean isSeriesResetAllowed() {
	    return seriesResetAllowed;
	}
	
//	public void resetCurrentSeriesEventually() {
//	    if(isOneClickBurstAllowed()) {
//	        seriesResetAllowed = true;
//	    } else {
//	        seriesShotCount = 0;
//	    }
//    }

	public float getFireRate() {
		return getWeapon().builder.fireRate;
	}
	
    public boolean isOneClickBurstAllowed() {
        //System.out.println("One click burst allowed: " + getWeapon().builder.isOneClickBurstAllowed);
        return getWeapon().builder.isOneClickBurstAllowed;
    }

	public boolean isAutomaticModeEnabled() {
		return maxShots > 1;
	}

	public boolean isAimed() {
		return aimed;
	}

	public void setAimed(boolean aimed) {
		if(aimed != this.aimed) {
			this.aimed = aimed;
			markDirty();
			aimChangeTimestamp = System.currentTimeMillis();
		}
	}

	public int[] getActiveAttachmentIds() {
		if(activeAttachmentIds == null || activeAttachmentIds.length != AttachmentCategory.values.length) {
			activeAttachmentIds = new int[AttachmentCategory.values.length];
			for(CompatibleAttachment<Weapon> attachment: getWeapon().getCompatibleAttachments().values()) {
				if(attachment.isDefault()) {
					activeAttachmentIds[attachment.getAttachment().getCategory().ordinal()] = Item.getIdFromItem(attachment.getAttachment());
				}
			}
		}
		return activeAttachmentIds;
	}

	void setActiveAttachmentIds(int[] activeAttachmentIds) {
		if(!Arrays.equals(this.activeAttachmentIds, activeAttachmentIds)) {
			this.activeAttachmentIds = activeAttachmentIds;
			markDirty();
		}
	}

	public byte[] getSelectedAttachmentIds() {
		return selectedAttachmentIndexes;
	}

	void setSelectedAttachmentIndexes(byte[] selectedAttachmentIndexes) {
		if(!Arrays.equals(this.selectedAttachmentIndexes, selectedAttachmentIndexes)) {
			this.selectedAttachmentIndexes = selectedAttachmentIndexes;
			markDirty();
		}
	}

	public boolean isAttachmentZoomEnabled() {
		Item scopeItem = getAttachmentItemWithCategory(AttachmentCategory.SCOPE);
		return scopeItem instanceof ItemScope;
	}

	@SuppressWarnings("unchecked")
	public ItemAttachment<Weapon> getAttachmentItemWithCategory(AttachmentCategory category) {
		if(activeAttachmentIds == null || activeAttachmentIds.length <= category.ordinal()) {
			return null;
		}
		Item activeAttachment = Item.getItemById(activeAttachmentIds[category.ordinal()]);
		if(activeAttachment instanceof ItemAttachment) {
		    return (ItemAttachment<Weapon>) activeAttachment;
		}
		return null;
	}

	public float getZoom() {
		return zoom;
	}

	public void setZoom(float zoom) {
		if(this.zoom != zoom) {
			this.zoom = zoom;
			markDirty();
		}
	}

	public boolean isLaserOn() {
		return laserOn;
	}

	public void setLaserOn(boolean laserOn) {
		if(this.laserOn != laserOn) {
			this.laserOn = laserOn;
			markDirty();
		}
	}

	public boolean isNightVisionOn() {
        return nightVisionOn;
    }

    public void setNightVisionOn(boolean nightVisionOn) {
        if(this.nightVisionOn != nightVisionOn) {
            this.nightVisionOn = nightVisionOn;
            markDirty();
        }
    }

	public int getActiveTextureIndex() {
		return activeTextureIndex;
	}

	public void setActiveTextureIndex(int activeTextureIndex) {
		if(this.activeTextureIndex != activeTextureIndex) {
			if(activeTextureIndex > Byte.MAX_VALUE) {
				throw new IllegalArgumentException("activeTextureIndex must be less than " + Byte.MAX_VALUE);
			}
			this.activeTextureIndex = (byte)activeTextureIndex;
			markDirty();
		}
	}

	@Override
	public Class<? extends Perspective<?>> getRequiredPerspectiveType() {
	    Class<? extends Perspective<?>> result = null;
	    if(isAimed()) {
	        ItemAttachment<Weapon> scope = getAttachmentItemWithCategory(AttachmentCategory.SCOPE);
	        if(scope instanceof ItemScope && ((ItemScope) scope).isOptical()) {
	            result = OpticalScopePerspective.class;
	        }
	    }
	    return result;
	}

    private boolean hasOpticScope() {
        ItemAttachment<Weapon> scope = getAttachmentItemWithCategory(AttachmentCategory.SCOPE);
        return scope instanceof ItemScope && ((ItemScope) scope).isOptical();
    }

    public ItemScope getScope() {
        ItemAttachment<Weapon> scope = getAttachmentItemWithCategory(AttachmentCategory.SCOPE);
        return scope instanceof ItemScope ? (ItemScope) scope : null;
    }
    

    private float getOpticScopeVignetteRadius(float partialTicks) {
        //ItemAttachment<Weapon> scope = getAttachmentItemWithCategory(AttachmentCategory.SCOPE);
        EntityPlayer player = compatibility.getClientPlayer();
//        float f = player.distanceWalkedModified - player.prevDistanceWalkedModified;
//        float f1 = -(player.distanceWalkedModified + f * partialTicks);
        float f2 = player.prevCameraYaw + (player.cameraYaw - player.prevCameraYaw) * partialTicks;
        return -2f * f2 + 0.55f;
    }

    private float getAimChangeProgress() {
        float p = MiscUtils.clamp((float)(System.currentTimeMillis() - aimChangeTimestamp) / AIM_CHANGE_DURATION, 0f, 1f);
        if(!isAimed()) {
            p = 1f - p;
        }

        return p;
    }

   	@Override
    public DynamicShaderGroupSource getShaderSource(DynamicShaderPhase phase) {
	    if(isAimed() && phase == DynamicShaderPhase.POST_WORLD_OPTICAL_SCOPE_RENDER) {
	        ItemScope scope = getScope();
	        if(scope.isOptical()) {
	            return scope.hasNightVision() && nightVisionOn ? NIGHT_VISION_SOURCE 
	                    : VIGNETTE_SOURCE;
	        }
	    }

	    Boolean blurOnAim = true;
	    if(getWeapon() != null && getWeapon().getModContext() != null && getWeapon().getModContext().getConfigurationManager().getProjectiles() != null) {
	        blurOnAim = getWeapon().getModContext().getConfigurationManager().getProjectiles().isBlurOnAim();
	        if(blurOnAim == null) {
	            blurOnAim = true;
	        }
	    }
	    float progress = getAimChangeProgress();
        return blurOnAim
                && phase == DynamicShaderPhase.PRE_ITEM_RENDER
                && (isAimed() || (progress > 0f && progress < 1f)) ? BLUR_SOURCE : null;
    }
   	
   	public void setLoadIterationCount(int loadIterationCount) {
        this.loadIterationCount = loadIterationCount;
    }
   	
   	public int getLoadIterationCount() {
        return loadIterationCount;
    }
   	
   	@Override
   	protected void reconcile() {
        ItemStack itemStack = getItemStack();
        if(itemStack != null) {
            int expectedStackAmmo = Tags.getAmmo(itemStack);
            if(this.ammo > expectedStackAmmo) {
                logger.debug("Reconciling. Expected ammo: {}, actual: {}", expectedStackAmmo, this.ammo);
                this.ammo = expectedStackAmmo;
            }
            
//            int[] expectedAttachmentIds = Tags.getAttachmentIds(itemStack);
//            if(!Arrays.equals(expectedAttachmentIds, this.activeAttachmentIds)) {
//                logger.debug("Reconciling. Expected attachments: {}, actual: {}", 
//                        Arrays.toString(expectedAttachmentIds), Arrays.toString(this.activeAttachmentIds));
//                this.activeAttachmentIds = expectedAttachmentIds;
//            }
            updateTimestamp = System.currentTimeMillis();
        }
   	}

	@Override
	public String toString() {
		return getWeapon().builder.name + "[" + getUuid() + "]";
	}


    public boolean isAltMofificationModeEnabled() {
        return altModificationModeEnabled;
    }
    
    public void setAltModificationModeEnabled(boolean altModificationModeEnabled) {
        this.altModificationModeEnabled = altModificationModeEnabled;
    }
}
