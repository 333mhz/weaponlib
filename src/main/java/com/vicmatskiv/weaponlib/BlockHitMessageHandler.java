package com.vicmatskiv.weaponlib;

import static com.vicmatskiv.weaponlib.compatibility.CompatibilityProvider.compatibility;

import com.vicmatskiv.weaponlib.ModContext;
import com.vicmatskiv.weaponlib.compatibility.CompatibleMessage;
import com.vicmatskiv.weaponlib.compatibility.CompatibleMessageContext;
import com.vicmatskiv.weaponlib.compatibility.CompatibleMessageHandler;

public class BlockHitMessageHandler implements CompatibleMessageHandler<BlockHitMessage, CompatibleMessage>  {
    
    private ModContext modContext;
    
    //private double yOffset = 1;

    public BlockHitMessageHandler(ModContext modContext) {
        this.modContext = modContext;
    }

    @Override
    public <T extends CompatibleMessage> T onCompatibleMessage(BlockHitMessage message, CompatibleMessageContext ctx) {
        if(!ctx.isServerSide()) {
            compatibility.runInMainClientThread(() -> {
                compatibility.addBlockHitEffect(message.getPosX(), message.getPosY(), message.getPosZ(), message.getSideHit());
            });
        }
        return null;
    }
}
