package com.vicmatskiv.weaponlib.compatibility;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

public abstract class CompatibleCommand extends CommandBase {

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        execCommand(sender, args);

    }

    protected abstract void execCommand(ICommandSender sender, String[] args);

    @Override
    public String getCommandName() {
        return getCompatibleName();
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return getCompatibleUsage(sender);
    }

    protected abstract String getCompatibleName();

    protected abstract String getCompatibleUsage(ICommandSender sender);
}
