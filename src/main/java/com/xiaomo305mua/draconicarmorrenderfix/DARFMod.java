package com.xiaomo305mua.draconicarmorrenderfix;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;

@Mod(modid = DARFMod.MODID,
        name = DARFMod.NAME,
        version = DARFMod.VERSION,
        dependencies = "@modDependencies@")
public class DARFMod {

    public static final String MODID = "@modId@";
    public static final String NAME = "@modName@";
    public static final String VERSION = "@modVersion@";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
    }
}