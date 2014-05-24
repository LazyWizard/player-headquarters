package org.lazywizard.playerhq;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OrbitalStationAPI;

// TODO: Module system so new features can be added simply
// TODO: Categorized cargo storage modules
// TODO: Integrate Omnifactory as a module
// TODO: Simulation battles w/ whole player fleet vs customized enemy fleet
class PlayerHQScript implements EveryFrameScript
{
    private final OrbitalStationAPI station;

    PlayerHQScript(OrbitalStationAPI station)
    {
        this.station = station;
    }

    InteractionDialogPlugin createDialog()
    {
        return null;
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

    @Override
    public void advance(float amount)
    {
        // TODO
    }
}
