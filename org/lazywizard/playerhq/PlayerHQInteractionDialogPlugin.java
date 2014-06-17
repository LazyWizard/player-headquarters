package org.lazywizard.playerhq;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.InteractionDialogImageVisual;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CoreInteractionListener;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.OrbitAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import java.awt.Color;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.campaign.CargoUtils;
import org.lwjgl.util.vector.Vector2f;

class PlayerHQInteractionDialogPlugin implements InteractionDialogPlugin, CoreInteractionListener
{
    private Menu currentMenu;
    private InteractionDialogAPI dialog;
    private TextPanelAPI text;
    private OptionPanelAPI options;
    private final SectorEntityToken station;
    private DelayedScript waitScript = null;

    private enum Menu
    {
        MAIN,
        SIM_LIST,
        SIM_BUILD_CUSTOM_FLEET,
        WAITING
    }

    private enum Option
    {
        MENU_MAIN,
        MENU_SIM_LIST,
        STORAGE,
        REPAIR,
        SIM_TEST_VS_PIRATES_ARMADA,
        SIM_TEST_VS_HEGEMONY_SDF,
        SIM_TEST_VS_TRITACHYON_DETACHMENT,
        SIM_TEST_VS_CUSTOM_FLEET,
        LEAVE
    }

    PlayerHQInteractionDialogPlugin(SectorEntityToken station)
    {
        this.station = station;
    }

    private void waitFor(float duration, Script runWhenDone)
    {
        goToMenu(Menu.WAITING);
        waitScript = new DelayedScript(runWhenDone, duration);
    }

    // TODO: Copy commander stat bonuses if setCaptain() doesn't transfer them
    private CampaignFleetAPI copyFleet(CampaignFleetAPI toCopy)
    {
        // Spawn a dummy fleet
        CampaignFleetAPI fleet = Global.getSector().createFleet(
                "playerhq", "simFleet");
        fleet.getFleetData().clear();
        fleet.getCommanderStats().getLogistics().modifyFlat("lw_simbonus", 999f);
        fleet.getCargo().addSupplies(toCopy.getTotalSupplyCostPerDay() * 5f);

        // Add a copy of all ships in the fleet
        for (FleetMemberAPI tmp : toCopy.getFleetData().getMembersListCopy())
        {
            // Create a new copy of the ship's variant
            FleetMemberAPI ship = Global.getFactory().createFleetMember(
                    tmp.getType(), tmp.getSpecId());
            fleet.getFleetData().addFleetMember(ship);

            // Copy old ship's stats over to the new ship
            RepairTrackerAPI status = ship.getRepairTracker(),
                    tmpStatus = tmp.getRepairTracker();
            ship.getCrewComposition().addAll(tmp.getCrewComposition());
            status.setMothballed(tmpStatus.isMothballed());
            status.setCR(tmpStatus.getCR());
            status.setLogisticalPriority(tmpStatus.isLogisticalPriority());
            ship.setShipName(tmp.getShipName());
            ship.setCaptain(tmp.getCaptain());

            // Make sure flagship status is retained
            if (tmp.isFlagship())
            {
                ship.setFlagship(true);
            }
        }

        return fleet;
    }

    private void testSimBattle(String faction, String fleet)
    {
        // Set up both sides of the simulation battle
        final LocationAPI loc = Global.getSector().getCurrentLocation();
        final CampaignFleetAPI simPlayer = copyFleet(Global.getSector().getPlayerFleet()),
                simEnemy = Global.getSector().createFleet(faction, fleet);
        final BattleCreationContext context = new BattleCreationContext(
                simPlayer, FleetGoal.ATTACK, simEnemy, FleetGoal.ATTACK);

        // Register the dummy player fleet and start the battle
        loc.spawnFleet(loc.createToken(0f, 0f), 0f, 0f, simPlayer);
        text.addParagraph("Starting battle vs " + simEnemy.getFullName() + "...");
        dialog.flickerStatic(3f, 1f);

        waitFor(3f, new Script()
        {
            @Override
            public void run()
            {
                dialog.startBattle(context);
                loc.removeEntity(simPlayer);
            }
        });
    }

    @Override
    public void init(InteractionDialogAPI dialog)
    {
        this.dialog = dialog;
        text = dialog.getTextPanel();
        options = dialog.getOptionPanel();

        goToMenu(Menu.MAIN);
    }

    private void goToMenu(Menu menu)
    {
        currentMenu = menu;
        options.clearOptions();

        switch (menu)
        {
            case MAIN:
                options.addOption("Cargo", Option.STORAGE);
                options.addOption("Simulation battles", Option.MENU_SIM_LIST);
                options.addOption("Repair fleet", Option.REPAIR);
                options.setEnabled(Option.REPAIR, false);
                options.addOption("Leave", Option.LEAVE);
                dialog.setPromptText("Choose an option:");
                dialog.setOptionOnEscape("Leave", Option.LEAVE);
                break;
            case SIM_LIST:
                options.addOption("Sim: Player vs Pirate Armada",
                        Option.SIM_TEST_VS_PIRATES_ARMADA);
                options.addOption("Sim: Player vs Hegemony Defense Fleet",
                        Option.SIM_TEST_VS_HEGEMONY_SDF);
                options.addOption("Sim: Player vs Tri-Tachyon Security Detachment",
                        Option.SIM_TEST_VS_TRITACHYON_DETACHMENT);
                options.addOption("Sim: Player vs Custom Fleet",
                        Option.SIM_TEST_VS_CUSTOM_FLEET);
                options.addOption("Return to main menu",
                        Option.MENU_MAIN);
                dialog.setPromptText("Choose a simulation to load:");
                dialog.setOptionOnEscape(null, Option.MENU_MAIN);
                break;
            case SIM_BUILD_CUSTOM_FLEET:
                // TODO
                text.addParagraph("Known sim opponents: " + CollectionUtils.implode(
                        PlayerHQ.getKnownSimOpponents()), Color.LIGHT_GRAY);
                options.addOption("Return to simulation list",
                        Option.MENU_SIM_LIST);
                break;
            case WAITING:
                dialog.setPromptText("LOADING...");
                dialog.setOptionOnEscape(null, null);
            default:
                text.addParagraph("This menu is not implemented yet.", Color.RED);
                dialog.setPromptText("Choose an option:");
                options.addOption("Return to main menu",
                        Option.MENU_MAIN);
        }
    }

    @Override
    public void optionSelected(String optionText, Object optionData)
    {
        if (optionData instanceof Option)
        {
            Option option = (Option) optionData;
            switch (option)
            {
                case MENU_MAIN:
                    goToMenu(Menu.MAIN);
                    break;
                case MENU_SIM_LIST:
                    goToMenu(Menu.SIM_LIST);
                    break;
                case STORAGE:
                    dialog.getVisualPanel().showCore(
                            CoreUITabId.CARGO, new CargoToken(), this);
                    break;
                case SIM_TEST_VS_PIRATES_ARMADA:
                    testSimBattle("pirates", "armada");
                    break;
                case SIM_TEST_VS_HEGEMONY_SDF:
                    testSimBattle("hegemony", "systemDefense");
                    break;
                case SIM_TEST_VS_TRITACHYON_DETACHMENT:
                    testSimBattle("tritachyon", "securityDetachment");
                    break;
                case SIM_TEST_VS_CUSTOM_FLEET:
                    goToMenu(Menu.SIM_BUILD_CUSTOM_FLEET);
                    break;
                case LEAVE:
                    dialog.dismiss();
                    break;
                default:
                    text.addParagraph("This option is not implemented yet.", Color.RED);
            }
        }
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData)
    {
    }

    @Override
    public void advance(float amount)
    {
        if (waitScript != null)
        {
            waitScript.advance(amount);
            if (waitScript.isElapsed)
            {
                waitScript = null;
            }
        }
    }

    @Override
    public void backFromEngagement(EngagementResultAPI battleResult)
    {
        goToMenu(Menu.SIM_LIST);
        // TODO: Show detailed battle results and stats
    }

    @Override
    public Object getContext()
    {
        return null;
    }

    @Override
    public void coreUIDismissed()
    {
    }

    // Nothing like writing an entire dummy implementation because setCargo() doesn't exist
    private class CargoToken implements SectorEntityToken
    {
        @Override
        public CargoAPI getCargo()
        {
            CargoAPI cargo = PlayerHQ.getCargo();
            CargoUtils.moveCargo(station.getCargo(), cargo);
            CargoUtils.moveMothballedShips(station.getCargo(), cargo);
            return cargo;
        }

        @Override
        public Vector2f getLocation()
        {
            return station.getLocation();
        }

        @Override
        public OrbitAPI getOrbit()
        {
            return station.getOrbit();
        }

        @Override
        public void setOrbit(OrbitAPI orbit)
        {
            station.setOrbit(orbit);
        }

        @Override
        public Object getName()
        {
            return station.getName();
        }

        @Override
        public String getFullName()
        {
            return station.getFullName();
        }

        @Override
        public void setFaction(String factionId)
        {
            station.setFaction(factionId);
        }

        @Override
        public LocationAPI getContainingLocation()
        {
            return station.getContainingLocation();
        }

        @Override
        public float getRadius()
        {
            return station.getRadius();
        }

        @Override
        public FactionAPI getFaction()
        {
            return station.getFaction();
        }

        @Override
        public String getCustomDescriptionId()
        {
            return station.getCustomDescriptionId();
        }

        @Override
        public void setCustomDescriptionId(String customDescriptionId)
        {
            station.setCustomDescriptionId(customDescriptionId);
        }

        @Override
        public void setCustomInteractionDialogImageVisual(InteractionDialogImageVisual visual)
        {
            station.setCustomInteractionDialogImageVisual(visual);
        }

        @Override
        public InteractionDialogImageVisual getCustomInteractionDialogImageVisual()
        {
            return station.getCustomInteractionDialogImageVisual();
        }

        @Override
        public void setFreeTransfer(boolean freeTransfer)
        {
            station.setFreeTransfer(freeTransfer);
        }

        @Override
        public boolean isFreeTransfer()
        {
            return station.isFreeTransfer();
        }
    }

    private class DelayedScript
    {
        private final Script script;
        private boolean isElapsed = false;
        private float countdown;

        private DelayedScript(Script script, float countdown)
        {
            this.script = script;
            this.countdown = countdown;
        }

        private void advance(float amount)
        {
            if (isElapsed)
            {
                return;
            }

            countdown -= amount;
            if (countdown <= 0f)
            {
                isElapsed = true;
                script.run();
            }
        }
    }
}
