package org.lazywizard.playerhq;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.OrbitalStationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Level;

// Publicly accessible methods, also handles persistent data
public class PlayerHQ
{
    public static void createHeadquarters(OrbitalStationAPI station)
    {
        // Only one headquarters can exist in the game world
        if (getHeadquarters() != null)
        {
            Global.getLogger(PlayerHQ.class).log(Level.ERROR, (isHeadquarters(station)
                    ? "Tried to create headquarters multiple times!"
                    : "Player headquarters already exists!"));
            return;
        }

        PlayerHQScript playerHQ = new PlayerHQScript(station);
        station.setFreeTransfer(true);
        // TODO: Register modules
        station.getContainingLocation().addScript(playerHQ);
        getDataMap().put(Constants.STATION_ID, station);
    }

    public static OrbitalStationAPI getHeadquarters()
    {
        return (OrbitalStationAPI) getDataMap().get(Constants.STATION_ID);
    }

    public static boolean isHeadquarters(SectorEntityToken token)
    {
        return (token instanceof OrbitalStationAPI)
                && (token == getDataMap().get(Constants.STATION_ID));
    }

    public static CargoAPI getCargo()
    {
        Map<String, Object> data = getDataMap();
        if (!data.containsKey(Constants.CARGO_ID))
        {
            CargoAPI storage = Global.getFactory().createCargo(true);
            storage.initMothballedShips("player");
            data.put(Constants.CARGO_ID, storage);
            return storage;
        }

        return (CargoAPI) data.get(Constants.CARGO_ID);
    }

    public static Set<String> getKnownSimOpponents()
    {
        Map<String, Object> data = getDataMap();
        if (!data.containsKey(Constants.SIM_LIST))
        {
            Set<String> simList = new HashSet<>();
            data.put(Constants.SIM_LIST, simList);
            return simList;
        }

        return (Set<String>) data.get(Constants.SIM_LIST);
    }

    static Map<String, Object> getDataMap()
    {
        Map<String, Object> persistentData, dataMap;
        persistentData = Global.getSector().getPersistentData();
        if (!persistentData.containsKey(Constants.PERSISTENT_DATA_ID))
        {
            dataMap = new LinkedHashMap<>();
            persistentData.put(Constants.PERSISTENT_DATA_ID, dataMap);
        }
        else
        {
            dataMap = (Map<String, Object>) persistentData.get(Constants.PERSISTENT_DATA_ID);
        }

        return dataMap;
    }

    static List<ShipVariantAPI> checkForNewSimOpponents(CampaignFleetAPI opponent)
    {
        List<ShipVariantAPI> newOpponents = new ArrayList<>();
        Set<String> knownOpponents = getKnownSimOpponents();

        // TODO: Keep track of all opponents faced for custom 'simulation' battles
        for (FleetMemberAPI member : opponent.getFleetData().getCombatReadyMembersListCopy())
        {
            // TODO: Add fighter wing support
            if (member.isFighterWing())
            {
                continue;
            }

            ShipVariantAPI variant = member.getVariant();
            String variantId = variant.getHullVariantId();
            if (knownOpponents.contains(variantId))
            {
                continue;
            }

            System.out.println("New variant found: " + variantId);
            newOpponents.add(variant);
            knownOpponents.add(variantId);
        }

        return newOpponents;
    }

    private PlayerHQ()
    {
    }
}
