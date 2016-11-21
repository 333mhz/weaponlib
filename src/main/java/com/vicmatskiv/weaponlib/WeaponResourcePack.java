package com.vicmatskiv.weaponlib;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;

public class WeaponResourcePack implements IResourcePack {
	
	private static final String WEAPONLIB_RESOURCE_DOMAIN = "weaponlib";
	
	private static final Set<?> RESOURCE_DOMAINS = Collections.unmodifiableSet(new HashSet<>(
			Collections.singleton(WEAPONLIB_RESOURCE_DOMAIN)));

	@Override
	public InputStream getInputStream(ResourceLocation resourceLocation) throws IOException {
		return getClass().getResourceAsStream(resourceLocation.getResourcePath());
	}

	@Override
	public boolean resourceExists(ResourceLocation resourceLocation) {
		boolean value = WEAPONLIB_RESOURCE_DOMAIN.equals(resourceLocation.getResourceDomain())
				&& getClass().getResource(resourceLocation.getResourcePath()) != null;
		return value;
	}

	@Override
	public Set<?> getResourceDomains() {
		return RESOURCE_DOMAINS;
	}

	@Override
	public IMetadataSection getPackMetadata(IMetadataSerializer p_135058_1_, String p_135058_2_) throws IOException {
		return null;
	}

	@Override
	public BufferedImage getPackImage() throws IOException {
		return null;
	}

	@Override
	public String getPackName() {
		return getClass().getSimpleName();
	}

}
