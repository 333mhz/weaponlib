package com.vicmatskiv.weaponlib.shader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import com.vicmatskiv.weaponlib.Tuple;

import net.minecraft.util.ResourceLocation;

public class DynamicShaderGroupSource {

    private ResourceLocation shaderLocation;

    private List<Tuple<String, Function<DynamicShaderContext, Object>>> uniforms;
    private UUID sourceId;

    public DynamicShaderGroupSource(UUID sourceId, ResourceLocation location) {
        this.sourceId = sourceId;
        this.shaderLocation = location;
        this.uniforms = new ArrayList<>();
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public DynamicShaderGroupSource withUniform(String name, Function<DynamicShaderContext, Object> value) {
        uniforms.add(new Tuple<>(name, value));
        return this;
    }

    public ResourceLocation getShaderLocation() {
        return shaderLocation;
    }

    public List<Tuple<String, Function<DynamicShaderContext, Object>>> getUniforms(DynamicShaderContext context) {
        return Collections.unmodifiableList(uniforms);
    }
}
