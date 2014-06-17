package org.lazywizard.playerhq;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.OrbitalStationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Publicly accessible methods, also handles persistent data
public class PlayerHQ
{
    public static void createHeadquarters(OrbitalStationAPI station)
    {
        // Only one headquarters can exist in the game world
        if (getHeadquarters() != null)
        {
            throw new RuntimeException(isHeadquarters(station)
                    ? "Tried to create headquarters multiple times!"
                    : "Player headquarters already exists!");
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

    public static List<String> getKnownSimOpponents()
    {
        return new ArrayList<>(getSimData().keySet());
    }

    static Map<String, HullSize> getSimData()
    {
        Map<String, Object> data = getDataMap();
        if (!data.containsKey(Constants.SIM_LIST))
        {
            Map<String, HullSize> simList = new HashMap<>();
            data.put(Constants.SIM_LIST, simList);
            return simList;
        }

        return (Map<String, HullSize>) data.get(Constants.SIM_LIST);
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
        Map<String, HullSize> knownOpponents = getSimData();

        // TODO: Keep track of all opponents faced for custom 'simulation' battles
        for (FleetMemberAPI member : opponent.getFleetData().getCombatReadyMembersListCopy())
        {
            String variantId = member.getSpecId();

            // Variant is already in known opponents list
            if (knownOpponents.containsKey(variantId))
            {
                continue;
            }

            System.out.println("New variant found: " + variantId
                    + " (" + member.getHullSpec().getHullSize().name() + ")");
            newOpponents.add(member.getVariant());
            knownOpponents.put(variantId, member.getHullSpec().getHullSize());
        }

        return newOpponents;
    }

    private PlayerHQ()
    {
    }
}
