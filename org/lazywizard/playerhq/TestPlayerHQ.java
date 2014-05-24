package org.lazywizard.playerhq;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
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
                            new PlayerHQInteractionDialogPlugin(
                                    PlayerHQ.getHeadquarters()), null);
                }
            }
        });

        return CommandResult.SUCCESS;
    }
}
