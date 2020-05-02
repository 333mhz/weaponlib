package com.vicmatskiv.weaponlib.mission;
import static com.vicmatskiv.weaponlib.compatibility.CompatibilityProvider.compatibility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

import com.vicmatskiv.weaponlib.ModContext;
import com.vicmatskiv.weaponlib.compatibility.CompatibleMissionCapability;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class Missions {

    public static void update(EntityPlayer player, Action action, ModContext modContext) {
        Set<Mission> missions = CompatibleMissionCapability.getMissions(player);
        
        for(Mission mission: missions) {
            if(!mission.isCompleted(player) && mission.update(action, player)) {
                if(mission.isCompleted(player)) {
                    modContext.getStatusMessageCenter().addAlertMessage(
                            compatibility.getLocalizedString("Mission accomplished!"), 3, 250, 200);
                }
                CompatibleMissionCapability.updateMission(player, mission);
                
                modContext.getChannel().getChannel().sendTo(
                        new PlayerMissionSyncMessage(CompatibleMissionCapability.getMissions(player)),
                        (EntityPlayerMP)player);
                
                break; // Only one mission can be carried at a time 
            }
        }
    }
    
    public static void redeem(EntityPlayer player, Mission mission, Entity assigner, ModContext modContext) {
        
        
    }
    
    public static void assign(EntityPlayer player, Mission mission, ModContext modContext) {
        CompatibleMissionCapability.updateMission(player, mission);
        modContext.getChannel().getChannel().sendTo(
                new PlayerMissionSyncMessage(CompatibleMissionCapability.getMissions(player)),
                (EntityPlayerMP)player);
    }
    
    public static Collection<Mission> getMatchingMissions(MissionOffering missionOffering, 
            EntityPlayer player, Predicate<Mission> predicate) {
        Collection<Mission> result = new ArrayList<>();        
        Set<Mission> missions = CompatibleMissionCapability.getMissions(player);

        for(Mission mission: missions) {
            if(missionOffering.getId().equals(mission.getMissionOfferingId())
                    && predicate.test(mission)) {
                result.add(mission);
            }
        }
        return result;
    }
    
    public static Collection<Mission> getRedeemableMissions(MissionOffering missionOffering, EntityPlayer player) {
        return getMatchingMissions(missionOffering, player, 
                mission -> 
                    mission.isCompleted(player) 
                    && !mission.isExpired(compatibility.world(player).getTotalWorldTime())
                    && !mission.isRedeemed());
    }
    
    public static Collection<MissionOffering> getRedeemableMissionOfferings(MissionAssigner missionAssigner, EntityPlayer player) {
        Collection<MissionOffering> result = new ArrayList<>();      
        for(MissionOffering missionOffering: missionAssigner.getMissionOfferings().values()) {
            if(!getMatchingMissions(missionOffering, player, 
                mission -> mission.isCompleted(player)
                    && !mission.isExpired(compatibility.world(player).getTotalWorldTime())
                    && !mission.isRedeemed()).isEmpty()) {
                result.add(missionOffering);
            }
        }
        return result;
    }
    
    public static Collection<MissionOffering> getAvailableOfferings(MissionAssigner missionAssigner, EntityPlayer player) {
        Collection<MissionOffering> result = new ArrayList<>();      
        for(MissionOffering missionOffering: missionAssigner.getMissionOfferings().values()) {
            if(missionOffering.isAvailableFor(player)) {
                result.add(missionOffering);
            }
        }
        return result;
    }
}
