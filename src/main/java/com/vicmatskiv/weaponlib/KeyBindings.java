package com.vicmatskiv.weaponlib;

import static com.vicmatskiv.weaponlib.compatibility.CompatibilityProvider.compatibility;

import org.lwjgl.input.Keyboard;

import com.vicmatskiv.weaponlib.animation.DebugPositioner;

import net.minecraft.client.settings.KeyBinding;

public class KeyBindings {

	public static KeyBinding reloadKey;
	public static KeyBinding attachmentKey;
	public static KeyBinding upArrowKey;
	public static KeyBinding downArrowKey;
	public static KeyBinding leftArrowKey;
	public static KeyBinding rightArrowKey;
	public static KeyBinding laserSwitchKey;
	
	public static KeyBinding addKey;
	
	public static KeyBinding subtractKey;
	
	public static KeyBinding fireModeKey;
	
	public static KeyBinding jDebugKey;
	public static KeyBinding kDebugKey;
	
	public static KeyBinding minusDebugKey;
    public static KeyBinding equalsDebugKey;
	
	public static KeyBinding lBracketDebugKey;
	public static KeyBinding rBracketDebugKey;
	
	public static KeyBinding semicolonDebugKey;
    public static KeyBinding apostropheDebugKey;

    
    public static KeyBinding deleteDebugKey;

	public static void init() {
		
		reloadKey = new KeyBinding("key.reload", Keyboard.KEY_R,
				"key.categories.weaponlib");
		
		laserSwitchKey = new KeyBinding("key.laser", Keyboard.KEY_L,
				"key.categories.weaponlib");
		
		attachmentKey = new KeyBinding("key.attachment", Keyboard.KEY_M,
				"key.categories.weaponlib");
		
		upArrowKey = new KeyBinding("key.scope", Keyboard.KEY_UP,
				"key.categories.weaponlib");
		
		downArrowKey = new KeyBinding("key.recoil_fitter", Keyboard.KEY_DOWN,
				"key.categories.weaponlib");
		
		leftArrowKey = new KeyBinding("key.silencer", Keyboard.KEY_LEFT,
				"key.categories.weaponlib");
		
		rightArrowKey = new KeyBinding("key.texture_change", Keyboard.KEY_RIGHT,
				"key.categories.weaponlib");
		
		addKey = new KeyBinding("key.add", Keyboard.KEY_I,
				"key.categories.weaponlib");
		
		subtractKey = new KeyBinding("key.subtract", Keyboard.KEY_O,
				"key.categories.weaponlib");
		
		fireModeKey = new KeyBinding("key.fire_mode", Keyboard.KEY_RSHIFT,
				"key.categories.weaponlib");
		
		
		compatibility.registerKeyBinding(reloadKey);
		compatibility.registerKeyBinding(attachmentKey);
		compatibility.registerKeyBinding(upArrowKey);
		compatibility.registerKeyBinding(downArrowKey);
		compatibility.registerKeyBinding(leftArrowKey);
		compatibility.registerKeyBinding(rightArrowKey);
		compatibility.registerKeyBinding(laserSwitchKey);
		compatibility.registerKeyBinding(addKey);
		compatibility.registerKeyBinding(subtractKey);
		compatibility.registerKeyBinding(fireModeKey);
		
		

		if(DebugPositioner.isDebugModeEnabled()) {

		    jDebugKey = new KeyBinding("key.jDebugKey", Keyboard.KEY_J,
		            "key.categories.weaponlib");

		    kDebugKey = new KeyBinding("key.klDebugKey", Keyboard.KEY_K,
		            "key.categories.weaponlib");
		    
		    minusDebugKey = new KeyBinding("key.minusDebugKey", Keyboard.KEY_MINUS,
                    "key.categories.weaponlib");

            equalsDebugKey = new KeyBinding("key.equalsDebugKey", Keyboard.KEY_EQUALS,
                    "key.categories.weaponlib");

		    lBracketDebugKey = new KeyBinding("key.lBracketDebugKey", Keyboard.KEY_LBRACKET,
		            "key.categories.weaponlib");

		    rBracketDebugKey = new KeyBinding("key.rBracketDebugKey", Keyboard.KEY_RBRACKET,
		            "key.categories.weaponlib");

		    semicolonDebugKey = new KeyBinding("key.semicolonDebugKey", Keyboard.KEY_SEMICOLON,
		            "key.categories.weaponlib");

		    apostropheDebugKey = new KeyBinding("key.apostropheDebugKey", Keyboard.KEY_APOSTROPHE,
		            "key.categories.weaponlib");
		    
            
            deleteDebugKey = new KeyBinding("key.deleteDebugKey", Keyboard.KEY_BACK,
                    "key.categories.weaponlib");

		    compatibility.registerKeyBinding(jDebugKey);
		    compatibility.registerKeyBinding(kDebugKey);
		    
		    compatibility.registerKeyBinding(lBracketDebugKey);
            compatibility.registerKeyBinding(rBracketDebugKey);
            
            compatibility.registerKeyBinding(semicolonDebugKey);
            compatibility.registerKeyBinding(apostropheDebugKey);
            
            compatibility.registerKeyBinding(minusDebugKey);
            compatibility.registerKeyBinding(equalsDebugKey);
            
            compatibility.registerKeyBinding(deleteDebugKey);
		}
	}
}
