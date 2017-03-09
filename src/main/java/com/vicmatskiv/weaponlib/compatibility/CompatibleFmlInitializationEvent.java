package com.vicmatskiv.weaponlib.compatibility;

import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class CompatibleFmlInitializationEvent {

	private FMLPreInitializationEvent event;

	public CompatibleFmlInitializationEvent(FMLPreInitializationEvent event) {
		this.event = event;
	}

	public FMLPreInitializationEvent getEvent() {
		return event;
	}

}
