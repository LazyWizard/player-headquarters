package org.lazywizard.playerhq;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.OrbitalStationAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;

public class PlayerHQModPlugin extends BaseModPlugin
{
    @Override
    public void onGameLoad()
    {
        Global.getSector().registerPlugin(new PlayerHQCampaignPlugin());
    }

    @Override
    public void onEnabled(boolean wasEnabledBefore)
    {
        if (!wasEnabledBefore)
        {
            // TODO: create station intelligently and assign it as headquarters
            StarSystemAPI system = Global.getSector().getStarSystems().get(0);
            OrbitalStationAPI station = (OrbitalStationAPI) system.addOrbitalStation(
                    system.getStar(), 5f, 200f, 50f, Constants.PLAYERHQ_NAME, "player");
            PlayerHQ.createHeadquarters(station);
        }
    }
}
