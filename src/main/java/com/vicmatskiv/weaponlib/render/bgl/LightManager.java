package com.vicmatskiv.weaponlib.render.bgl;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Stack;

import com.vicmatskiv.weaponlib.ClientEventHandler;
import com.vicmatskiv.weaponlib.shader.jim.Shader;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import scala.actors.threadpool.Arrays;

public class LightManager {
	
	public static final int MAX_POINT_LIGHTS = 32;
	
	private ArrayList<PointLight> lights = new ArrayList<>();
	
	public static class PointLight {
		private boolean tempLight;
		private long creationDate;
		private double life;
		
		
		private float constant, linear, quadratic;
		private float[] position = new float[3];
		private float[] color = new float[3];
		
		
		public PointLight(float x, float y, float z, float r, float g, float b, float constant, float linear, float quadratic) {
			this.constant = constant;
			this.linear = linear;
			this.quadratic = quadratic;
			
			color = new float[] {r, g, b};
			position = new float[] {x, y, z};
		}
		
		public PointLight(float x, float y, float z, float r, float g, float b, float constant, float linear, float quadratic, double life) {
			this(x, y, z, r, g, b, constant, linear, quadratic);
			this.tempLight = true;
			this.creationDate = System.currentTimeMillis();
			this.life = life;
		}
			
	}
	
	public void update() {
		lights.clear();
		
		for(Entry<Integer, Stack<Long>> registry : ClientEventHandler.muzzleFlashMap.entrySet()) {
			
			if(!registry.getValue().empty() /*&& System.currentTimeMillis()-registry.getValue().peek() > 25*/) {
				EntityPlayer player = (EntityPlayer) Minecraft.getMinecraft().world.getEntityByID(registry.getKey());
				addLight((float) player.posX, (float) player.posY, (float) player.posZ, 1.0f, 0.623f, 0.262f, 0.03f, 0.009f, 0.032f);
			}
			
			
		}
		
		
	}
	
	public void addLight(float x, float y, float z, float r, float g, float b, float constant, float linear, float quadratic) {
		lights.clear();
		if(lights.size() < 1) {
			//System.out.println("yo");
			float lightlevel = Minecraft.getMinecraft().world.getLight(new BlockPos(x, y, z));
			
			// bad correction maths
			if(lightlevel != 0.0) constant *= lightlevel*8;
			lights.add(new PointLight(x, y, z, r, g, b, constant, linear, quadratic));
		}
	}
	

	
	public ArrayList<PointLight> getLights() {
		return lights;
	}
	
	public void updateUniforms(Shader shader) {
		shader.uniform1i("totalActiveLights", lights.size());
		for(int i = 0; i < lights.size(); ++i) {
			PointLight light = lights.get(i);
			shader.uniform3f("lights[" + i + "].attenuation", light.constant, light.linear, light.quadratic);
			shader.uniform3f("lights[" + i + "].position", light.position[0], light.position[1], light.position[2]);
			shader.uniform3f("lights[" + i + "].baseColor", light.color[0], light.color[1], light.color[2]);
		}
		
	}

}
