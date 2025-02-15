package com.vicmatskiv.weaponlib.compatibility;

import net.minecraftforge.common.BiomeDictionary;

public enum CompatibleBiomeType {
    
    PLAINS(BiomeDictionary.Type.PLAINS), 
    HILLS(BiomeDictionary.Type.HILLS), 
    FOREST(BiomeDictionary.Type.FOREST), 
    SWAMP(BiomeDictionary.Type.SWAMP);
    
//    list.add(new BiomeEntry(BiomeGenBase.forest, 10));
//    list.add(new BiomeEntry(BiomeGenBase.roofedForest, 10));
//    list.add(new BiomeEntry(BiomeGenBase.extremeHills, 10));
//    list.add(new BiomeEntry(BiomeGenBase.plains, 10));
//    list.add(new BiomeEntry(BiomeGenBase.birchForest, 10));
//    list.add(new BiomeEntry(BiomeGenBase.swampland, 10));
//    
//    currentBiomes[BiomeType.WARM.ordinal()] = new TrackedList(list);
//    list.clear();
//
//    list.add(new BiomeEntry(BiomeGenBase.forest, 10));
//    list.add(new BiomeEntry(BiomeGenBase.extremeHills, 10));
//    list.add(new BiomeEntry(BiomeGenBase.taiga, 10));
//    list.add(new BiomeEntry(BiomeGenBase.plains, 10));
//    
//    currentBiomes[BiomeType.COOL.ordinal()] = new TrackedList(list);
//    list.clear();


    private BiomeDictionary.Type types[];
    
    private CompatibleBiomeType(BiomeDictionary.Type... types) {
        this.types = types;
    }
    
    public BiomeDictionary.Type[] getTypes() {
        return types;
    }

}
