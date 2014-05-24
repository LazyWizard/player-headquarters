package org.lazywizard.playerhq.commands;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import java.awt.Color;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class TestPlayerHQ implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (context != CommandContext.CAMPAIGN_MAP)
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        Global.getSector().addScript(new EveryFrameScript()
        {
            private boolean isDone = false;

            @Override
            public boolean isDone()
            {
                return isDone;
            }

            @Override
            public boolean runWhilePaused()
            {
                return false;
            }

            @Override
            public void advance(float amount)
            {
                if (!isDone)
                {
                    isDone = true;
                    Global.getSector().getCampaignUI().showInteractionDialog(
                            new TestPlayerHQInteractionDialogPlugin(), null);
                }
            }
        });

        return CommandResult.SUCCESS;
    }

    private static class TestPlayerHQInteractionDialogPlugin implements InteractionDialogPlugin
    {
        private InteractionDialogAPI dialog;

        private enum Options
        {
            TEST_VS_PIRATES_ARMADA,
            TEST_VS_HEGEMONY_SDF,
            TEST_VS_TRITACHYON_DETACHMENT,
            TEST_VS_CUSTOM_FLEET,
            LEAVE
        }

        private CampaignFleetAPI copyFleet(CampaignFleetAPI other)
        {
            // Spawn a dummy fleet
            CampaignFleetAPI fleet = Global.getSector().createFleet(
                    "playerhq", "simFleet");
            fleet.getCommanderStats().getLogistics().modifyFlat("lw", 999f);
            fleet.getCargo().addSupplies(other.getTotalSupplyCostPerDay());

            // Remove existing ships (only there for initial stats/cargo)
            for (FleetMemberAPI ship : fleet.getFleetData().getMembersListCopy())
            {
                fleet.getFleetData().removeFleetMember(ship);
            }

            // Add a copy of all ships in the fleet
            for (FleetMemberAPI ship : other.getFleetData().getCombatReadyMembersListCopy())
            {
                // Create a copy of the ship
                FleetMemberAPI tmp = Global.getFactory().createFleetMember(
                        ship.getType(), ship.getSpecId());
                fleet.getFleetData().addFleetMember(tmp);

                // Copy stats over
                tmp.getRepairTracker().setMothballed(false);
                tmp.getCrewComposition().addAll(ship.getCrewComposition());
                tmp.getRepairTracker().setCR(ship.getRepairTracker().getCR());
                tmp.setShipName(ship.getShipName());
                tmp.setCaptain(ship.getCaptain());

                // Make sure flagship is retained
                if (ship.isFlagship())
                {
                    tmp.setFlagship(true);
                }
            }

            return fleet;
        }

        private void testSimBattle(String faction, String fleet)
        {
            LocationAPI loc = Global.getSector().getCurrentLocation();
            CampaignFleetAPI simPlayer = copyFleet(Global.getSector().getPlayerFleet());
            loc.spawnFleet(loc.createToken(0f, 0f), 0f, 0f, simPlayer);
            CampaignFleetAPI simEnemy = Global.getSector().createFleet(
                    faction, fleet);
            BattleCreationContext context = new BattleCreationContext(
                    simPlayer, FleetGoal.ATTACK, simEnemy, FleetGoal.ATTACK);
            dialog.startBattle(context);
            loc.removeEntity(simPlayer);
        }

        @Override
        public void init(InteractionDialogAPI dialog)
        {
            this.dialog = dialog;

            dialog.getOptionPanel().addOption("Sim: Player vs Pirate Armada",
                    Options.TEST_VS_PIRATES_ARMADA);
            dialog.getOptionPanel().addOption("Sim: Player vs Hegemony Defense Fleet",
                    Options.TEST_VS_HEGEMONY_SDF);
            dialog.getOptionPanel().addOption("Sim: Player vs Tri-Tachyon Security Detachment",
                    Options.TEST_VS_TRITACHYON_DETACHMENT);
            dialog.getOptionPanel().addOption("Sim: Player vs Custom Fleet",
                    Options.TEST_VS_CUSTOM_FLEET);
            dialog.getOptionPanel().addOption("Leave", Options.LEAVE);
            dialog.setOptionOnEscape("Leave", Options.LEAVE);
        }

        @Override
        public void optionSelected(String optionText, Object optionData)
        {
            if (optionData instanceof Options)
            {
                Options option = (Options) optionData;
                switch (option)
                {
                    case TEST_VS_PIRATES_ARMADA:
                        testSimBattle("pirates", "armada");
                        break;
                    case TEST_VS_HEGEMONY_SDF:
                        testSimBattle("hegemony", "systemDefense");
                        break;
                    case TEST_VS_TRITACHYON_DETACHMENT:
                        testSimBattle("tritachyon", "securityDetachment");
                        break;
                    case LEAVE:
                        dialog.dismiss();
                        break;
                    default:
                        dialog.getTextPanel().addParagraph("This option is not implemented yet.", Color.RED);
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
        }

        @Override
        public Object getContext()
        {
            return null;
        }
    }
}
