package com.vicmatskiv.weaponlib.numerical;

import com.vicmatskiv.weaponlib.animation.MatrixHelper;

import net.minecraft.client.Minecraft;

public class LerpedValue {
	
	public double previousValue;
	public double currentValue;
	
	
	
	public LerpedValue() {
		
	}
	
	
	/**
	 * Called before changes are made, sets the previous value to the
	 * current one.
	 */
	public void updatePrevious() {
		previousValue = currentValue;
	}
	
	public void update(double newValue) {
		this.previousValue = this.currentValue;
		this.currentValue = newValue;
	}
	
	public void add(double value) {
		this.currentValue += value;
	}
	
	public double getValue() {
		return this.currentValue;
	}
	
	public double getPreviousValue() {
		return this.previousValue;
	}
	
	public double getLerped() {
		return MatrixHelper.solveLerp(this.previousValue, this.currentValue, Minecraft.getMinecraft().getRenderPartialTicks());
	}
	
	public float getLerpedFloat() {
		return (float) getLerped();
	}
	

}
