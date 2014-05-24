package org.lazywizard.playerhq;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import java.awt.Color;

class PlayerHQInteractionDialogPlugin implements InteractionDialogPlugin
{
    private InteractionDialogAPI dialog;
    private TextPanelAPI text;
    private OptionPanelAPI options;
    private final SectorEntityToken station;

    private enum Menu
    {
        MAIN,
        SIM_LIST
    }

    private enum Option
    {
        MENU_MAIN,
        MENU_SIM,
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

    // TODO: Copy commander stat bonuses
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
        LocationAPI loc = Global.getSector().getCurrentLocation();
        CampaignFleetAPI simPlayer = copyFleet(Global.getSector().getPlayerFleet()),
                simEnemy = Global.getSector().createFleet(faction, fleet);
        BattleCreationContext context = new BattleCreationContext(
                simPlayer, FleetGoal.ATTACK, simEnemy, FleetGoal.ATTACK);

        // Register the dummy player fleet and start the battle
        loc.spawnFleet(loc.createToken(0f, 0f), 0f, 0f, simPlayer);
        text.addParagraph("Starting battle vs " + simEnemy.getFullName() + ".");
        dialog.startBattle(context);
        loc.removeEntity(simPlayer);
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
        options.clearOptions();

        switch (menu)
        {
            case MAIN:
                options.addOption("Simulation battles", Option.MENU_SIM);
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
            default:
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
                case MENU_SIM:
                    goToMenu(Menu.SIM_LIST);
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
    }

    @Override
    public void backFromEngagement(EngagementResultAPI battleResult)
    {
        // TODO: Show detailed battle results and stats
    }

    @Override
    public Object getContext()
    {
        return null;
    }
}