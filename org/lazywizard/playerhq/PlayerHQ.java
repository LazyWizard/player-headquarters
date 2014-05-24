package org.lazywizard.playerhq;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.OrbitalStationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import java.util.LinkedHashMap;
import java.util.Map;

// Publicly accessible methods, also handles persistent data
public class PlayerHQ
{
    public static void createHeadquarters(OrbitalStationAPI station)
    {
        PlayerHQScript playerHQ = new PlayerHQScript(station);
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

    static void checkForNewSimOpponents(SectorEntityToken opponent)
    {
        // TODO: Keep track of all opponents faced for custom 'simulation' battles
    }

    private PlayerHQ()
    {
    }
}
