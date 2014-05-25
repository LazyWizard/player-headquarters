package org.lazywizard.playerhq;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.OrbitalStationAPI;
import org.apache.log4j.Level;
import org.lazywizard.lazylib.campaign.CargoUtils;

// TODO: Module system so new features can be added simply
// TODO: Categorized cargo storage modules
// TODO: Integrate Omnifactory as a module
// TODO: Simulation battles w/ whole player fleet vs customized enemy fleet
class PlayerHQScript implements EveryFrameScript
{
    private final OrbitalStationAPI station;
    private transient CargoAPI cargo;
    private transient CampaignClockAPI clock;
    private transient int lastCheck;

    PlayerHQScript(OrbitalStationAPI station)
    {
        this.station = station;
        readResolve(); // Because I'm lazy
    }

    protected Object readResolve()
    {
        cargo = station.getCargo();
        clock = Global.getSector().getClock();
        lastCheck = clock.getDay();
        return this;
    }

    @Override
    public boolean isDone()
    {
        return false;
    }

    @Override
    public boolean runWhilePaused()
    {
        return false;
    }

    // FIXME: Respec item is included in this check (not in next Respec release)
    private void checkCargo()
    {
        if (!cargo.isEmpty())
        {
            Global.getLogger(PlayerHQScript.class).log(Level.DEBUG,
                    "Moving items to actual cargo.");
            CargoUtils.moveCargo(cargo, PlayerHQ.getCargo());
        }

        if (cargo.getMothballedShips().getFleetPointsUsed() > 0)
        {
            Global.getLogger(PlayerHQScript.class).log(Level.DEBUG,
                    "Moving ships to actual cargo.");
            CargoUtils.moveMothballedShips(cargo, PlayerHQ.getCargo());
        }
    }

    @Override
    public void advance(float amount)
    {
        if (clock.getMonth()!= lastCheck)
        {
            lastCheck = clock.getMonth();
            checkCargo();
        }
    }
}
