package tutorial_island;

import api.bot_management.BotManagement;
import api.bot_management.data.LaunchedClient;
import api.bot_management.data.QuickLaunch;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.ui.Log;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientQuickLauncher {

    private String scriptName;
    private boolean isRepositoryScript;
    private int world;
    private String scriptArgs = "";
    private String killKey;

    private List<LaunchedClient> runningClients;

    public List<LaunchedClient> getRunningClients() {
        return runningClients;
    }

    public ClientQuickLauncher(String scriptName, boolean isRepositoryScript, int world) {
        this.scriptName = scriptName;
        this.isRepositoryScript = isRepositoryScript;
        this.world = world;
    }

    public ClientQuickLauncher(String scriptName, boolean isRepositoryScript, int world, String scriptArgs) {
        this.scriptName = scriptName;
        this.isRepositoryScript = isRepositoryScript;
        this.world = world;
        this.scriptArgs = scriptArgs;
    }

    public ClientQuickLauncher(String scriptName, boolean isRepositoryScript, int world, String scriptArgs, String killKey) {
        this.scriptName = scriptName;
        this.isRepositoryScript = isRepositoryScript;
        this.world = world;
        this.scriptArgs = scriptArgs;
        this.killKey = killKey;
    }

    public boolean isInstanceLimit() {
        try {
            runningClients = BotManagement.getRunningClients();
            Log.fine(runningClients.size() + " / " + TutorialIsland.ALLOWED_INSTANCES + " Clients Running");

            for (int t = 0; t < 2; t++) {
                if (runningClients.size() < TutorialIsland.ALLOWED_INSTANCES) {
                    Time.sleep(10000);
                    runningClients = BotManagement.getRunningClients();
                    Log.fine(runningClients.size() + " / " + TutorialIsland.ALLOWED_INSTANCES + " Clients Running");
                }
            }

            return runningClients.size() >= TutorialIsland.ALLOWED_INSTANCES;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void launchClient(String[] accountInfo) throws IOException {
        Log.fine("Launching Client  |  " + scriptName + "  |  World " + world);

        QuickLaunch quickLaunch = setupQuickLauncher(accountInfo);

        if (killKey != null && !killKey.isEmpty()) {
            BotManagement.startClient(0, quickLaunch.get().toString(), 0, null, 1, 10, killKey);
        } else {
            BotManagement.startClient(0, quickLaunch.get().toString(), 0, null, 1, 10);
        }
    }

    private QuickLaunch setupQuickLauncher(String[] accountInfo) {
        QuickLaunch qL = new QuickLaunch();
        ArrayList<QuickLaunch.Client> clientList = new ArrayList<>();

        QuickLaunch.Config config = qL.new Config(
                false, false, 0, false, false);
        QuickLaunch.Script script = qL.new Script(
                scriptArgs, scriptName, "", isRepositoryScript);
        QuickLaunch.Proxy proxy;
        if (accountInfo.length == 6 && accountInfo[4] != null && !accountInfo[4].isEmpty()) {
            proxy = qL.new Proxy(
                    "0", "9/29/2019", "DrScatman", "proxy1", accountInfo[2], Integer.parseInt(accountInfo[5]), accountInfo[3], accountInfo[4]);
        }
        else if (accountInfo.length == 4 && accountInfo[2] != null && !accountInfo[2].isEmpty()) {
            proxy = qL.new Proxy(
                    "0", "9/29/2019", "DrScatman", "proxy1", accountInfo[2], Integer.parseInt(accountInfo[3]), "", "");
        }
        else {
            proxy = qL.new Proxy("", "", "", "", "", 80, "", "");
        }

        QuickLaunch.Client qLClient = qL.new Client(
                accountInfo[0], accountInfo[1], world, proxy, script, config);

        clientList.add(qLClient);
        qL.setClients(clientList);
        return qL;
    }
}
