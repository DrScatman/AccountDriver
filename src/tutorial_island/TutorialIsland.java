package tutorial_island;

import api.bot_management.BotManagement;
import api.bot_management.data.LaunchedClient;
import org.rspeer.RSPeer;
import org.rspeer.runetek.adapter.Interactable;
import org.rspeer.runetek.adapter.component.InterfaceComponent;
import org.rspeer.runetek.adapter.component.Item;
import org.rspeer.runetek.adapter.scene.Npc;
import org.rspeer.runetek.adapter.scene.SceneObject;
import org.rspeer.runetek.api.Game;
import org.rspeer.runetek.api.Login;
import org.rspeer.runetek.api.Varps;
import org.rspeer.runetek.api.Worlds;
import org.rspeer.runetek.api.commons.BankLocation;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.commons.math.Random;
import org.rspeer.runetek.api.component.Dialog;
import org.rspeer.runetek.api.component.Interfaces;
import org.rspeer.runetek.api.component.tab.*;
import org.rspeer.runetek.api.input.Keyboard;
import org.rspeer.runetek.api.input.menu.ActionOpcodes;
import org.rspeer.runetek.api.movement.Movement;
import org.rspeer.runetek.api.movement.position.Area;
import org.rspeer.runetek.api.movement.position.Position;
import org.rspeer.runetek.api.scene.Npcs;
import org.rspeer.runetek.api.scene.Players;
import org.rspeer.runetek.api.scene.SceneObjects;
import org.rspeer.runetek.event.listeners.LoginResponseListener;
import org.rspeer.runetek.event.types.LoginResponseEvent;
import org.rspeer.runetek.providers.RSTileDecor;
import org.rspeer.runetek.providers.RSWorld;
import org.rspeer.script.Script;
import org.rspeer.script.ScriptMeta;
import org.rspeer.ui.Log;

import java.io.*;
import java.util.*;
import java.util.function.Predicate;

import static org.rspeer.runetek.event.types.LoginResponseEvent.Response.INVALID_CREDENTIALS;
import static org.rspeer.runetek.event.types.LoginResponseEvent.Response.RUNESCAPE_UPDATE_2;

@ScriptMeta(name = "Account Driver", desc = "Generates and Does Tutorial", developer = "DrScatman")
public class TutorialIsland extends Script implements LoginResponseListener {

    public static final String API_KEY = "S1Z8S8QHPE0LST3E2H07T8YABM63L17AW738NN61LAT0CT9NQG38JLDUDY7FCX5YG0ZVZ4";
    public static final int ALLOWED_INSTANCES = 1;
    private static final int NUM_BACKLOG_ACCOUNTS = 5;
    private static final String ACCOUNTS_FILE_PATH = Script.getDataDirectory() + "\\AccountsGenerated.txt";
    private static final String ACCOUNTS_DONE_FILE_PATH = Script.getDataDirectory() + "\\AccountsDone.txt";
    private static final String PYTHON_3_EXE = System.getProperty("user.home") + "\\AppData\\Local\\Programs\\Python\\Python38-32\\python.exe";
    private static final String ACC_GEN_PY = Script.getDataDirectory() + "\\create_rs_account.py";
    private static final String[] PROXY_IP = new String[]{}; // { "209.163.117.212", "167.160.71.140", "172.96.82.150", "172.96.95.99", "DEFAULT" };//"108.187.189.123";
    private static final String PROXY_USER = "";
    private static final String PROXY_PASS = "";
    private static final String PROXY_PORT = "1080";

    private static final int VARP = 281;
    private static final Position STRIKE_POS = new Position(3139, 3091);
    private InterfaceComponent nameAcceptedWidget;//.getFirst()w -> w.getMessage().contains("Great!"));
    private InterfaceComponent nameLookupWidget;
    private InterfaceComponent nameInputWidget;//(w -> w.getMessage().contains("unique"));
    private InterfaceComponent nameSetWidget;
    private InterfaceComponent nameScreenDetectionWidget; //("Choose display name");
    private InterfaceComponent creationScreenWidget = Interfaces.getComponent(269, 120);
    private boolean isAudioDisabled;
    private final char[] vowels = "aeiouAEIOU".toCharArray();
    private final char[] nonVowels = "bcdfghjklmnpqrstvwxyzBCDFGHJKLMNPQRSTVWXYZ".toCharArray();
    private boolean doDefault;
    private Position tinPosition;
    private Position copperPosition;
    private int config;
    private boolean disableChain;

    @Override
    public void onStart() {
        Log.fine("Script Started");
    }

    private int stopRetries = 5;

    @Override
    public void onStop() {
        Log.severe("Script Stopped");
        if (!disableChain) {
            Log.fine("Chaining");

            accountGeneratorDriver(NUM_BACKLOG_ACCOUNTS);
            int world = getNextWorld();

            try {
                new ClientQuickLauncher("Account Driver", false, world).launchClient(readAccount(true));
                killClient();

            } catch (Exception e) {
                Log.severe(e);
                if (stopRetries > 0) {
                    stopRetries--;
                    onStop();
                }
            }
        }
    }

    public void writeCurrentAccount() {
        String[] info = new String[]{RSPeer.getGameAccount().getUsername(), RSPeer.getGameAccount().getPassword()};
        writeAccount(info);
    }

    private List<String> accountsList;

    public String[] readAccount(boolean readFirst) {
        String accounts = "";
        File file = new File(ACCOUNTS_FILE_PATH);

        if (!file.exists()) {
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                readAccount(readFirst);
            }
        }

        try {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();

                while (line != null) {
                    sb.append(line);
                    sb.append(System.lineSeparator());
                    line = br.readLine();
                }
                accounts = sb.toString();
            }
        } catch (IOException e) {
            Log.info("File not found");
            readAccount(readFirst);
        }

        //file.delete();

        accounts = accounts.trim();
        accountsList = Arrays.asList(accounts.split(System.lineSeparator()));
        String account = readFirst ? accountsList.get(0) : accountsList.get(accountsList.size() - 1);
        String[] info = account.trim().split(":");
        if (info.length == 1) {
            info = new String[]{info[0], "plmmlp"};
        }

        writeAccounts(file);

        return info;
    }

    private void writeAccounts(File file) {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        PrintWriter pw = null;
        try {
            pw = new PrintWriter(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        for (int i = 1; i < accountsList.size(); i++) {
            if (pw != null) {
                pw.println(accountsList.get(i));
            }
        }
        if (pw != null) {
            pw.close();
        }
    }

    public void writeAccount(String[] info) {
        try (FileWriter fw = new FileWriter(ACCOUNTS_DONE_FILE_PATH, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            if (info.length == 6) {
                out.println(info[0] + ":" + info[1] + ":" + info[2] + ":" + info[3] + ":" + info[4] + ":" + info[5]);
            } else if (info.length == 4) {
                out.println(info[0] + ":" + info[1] + ":" + info[2] + ":" + info[3]);
            } else {
                out.println(info[0] + ":" + info[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getNextWorld() {
        int nextBotWorld = getNextBotWorld(randInt(0, 500));

        if (nextBotWorld > 0) {
            return nextBotWorld;
        }

        int[] randWorld = new int[] {301, 393, 308};
        return randWorld[randInt(0, randWorld.length - 1)];
    }

    private int getNextBotWorld(int pop) {
        RSWorld newWorld = Worlds.get(x -> x.getPopulation() <= pop &&
                !x.isMembers() && !x.isBounty() && !x.isSkillTotal());

        if (newWorld != null && newWorld.getId() > 0) {
            return newWorld.getId();
        }
        if (pop <= 1000) {
            getNextBotWorld(pop + 50);
        }
        return 0;
    }

    private void killClient() throws IOException {
        RSPeer.shutdown();
        for (LaunchedClient client : BotManagement.getRunningClients()) {
            if (client.getRunescapeEmail().equals(RSPeer.getGameAccount().getUsername())) {
                client.kill();
            }
        }
        System.exit(0);
    }

    private void accountGeneratorDriver(int setNumBacklogged) {
        int numToGen = setNumBacklogged - getNumAccsBacklogged();
        if (numToGen <= 0)
            return;

        numToGen = Math.min(numToGen, 5);

        Log.fine("Generating " + numToGen + " Accounts");
        for (int g = 0; g < numToGen; g++) {
            pythonGenerator(10);
            Time.sleep(300);
        }
    }

    private void pythonGenerator(int retries) {
        final String EMAIL_ARG = "-e " + getVerificationEmailAlias();
        final String PASSWORD_ARG = "-p " + "qazzaq";//getRandString(true, 6, 20);
        final String PROXY_IP_ARG = "-i " + (PROXY_IP.length > 0 ? PROXY_IP[randInt(0, PROXY_IP.length - 1)] : "DEFAULT");
        final String PROXY_USER_ARG = "-u " + PROXY_USER;
        final String PROXY_PASS_ARG = "-x " + PROXY_PASS;
        final String PROXY_PORT_ARG = "-o " + PROXY_PORT;

        try {
            if (PROXY_IP == null || PROXY_IP.length < 1 || PROXY_IP_ARG.equals("-i DEFAULT")) {
                final String EMAIL_ARG_2 = "-e2 " + getVerificationEmailAlias();
                final String PASSWORD_ARG_2 = "-p2 " + "qazzaq";//getRandString(true, 6, 20);
                Runtime.getRuntime().exec(
                        "cmd /c start cmd.exe /K \"" + PYTHON_3_EXE + " " + ACC_GEN_PY + " " + EMAIL_ARG_2 + " " + PASSWORD_ARG_2 + " && exit" + "\"");
            } else {
                Runtime.getRuntime().exec(
                        "cmd /c start cmd.exe /K \"" + PYTHON_3_EXE + " " + ACC_GEN_PY + " " + EMAIL_ARG + " " + PASSWORD_ARG +
                                " " + PROXY_IP_ARG + " " + PROXY_USER_ARG + " " + PROXY_PASS_ARG + " " + PROXY_PORT_ARG + " && exit" + "\"");
            }
        } catch (Exception e) {
            Log.severe("executeGenerator()  |  " + e.getMessage());
            e.printStackTrace();
            if (retries > 0) {
                pythonGenerator(retries - 1);
            }
        }
    }

    private String getRandString(boolean caseSensitive, int min, int max) {
        String SALTCHARS = !caseSensitive ? "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890" :
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890";
        StringBuilder salt = new StringBuilder();
        java.util.Random rnd = new java.util.Random();
        int strLen = randInt(min, max);

        while (salt.length() < strLen) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }

        return salt.toString();
    }

    private String getVerificationEmailAlias() {
        final String DOMAIN = "@gmail.com";
        String base = "milleja115";

        String extension = "+" + getRandString(false, 3, 35);
        String newBase = base + extension;

        int numDots = randInt(1, newBase.length());
        for (int i = 0; i < numDots; i++) {
            int randomSplitIndex = randInt(1, newBase.length() - 2);

            String dotAttempt = newBase.substring(0, randomSplitIndex) + "." +
                    newBase.substring(randomSplitIndex);

            if (!dotAttempt.contains("..")) {
                newBase = dotAttempt;
            }
        }
        return newBase + DOMAIN;
    }

    private int getNumAccsBacklogged() {
        int numAccsBacklogged = 0;
        File file = new File(ACCOUNTS_FILE_PATH);

        try {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line = br.readLine();

                while (line != null && line.contains("@")) {
                    numAccsBacklogged++;
                    line = br.readLine();
                }
            }
        } catch (IOException e) {
            Log.info("File not found");
        }

        return numAccsBacklogged;
    }

    @Override
    public void notify(LoginResponseEvent loginResponseEvent) {
        if (loginResponseEvent.getResponse().equals(LoginResponseEvent.Response.ACCOUNT_DISABLED) ||
                loginResponseEvent.getResponse().equals(INVALID_CREDENTIALS)
        ) {

            disableChain = false;
            setStopping(true);

        } else if (loginResponseEvent.getResponse().equals(LoginResponseEvent.Response.RUNESCAPE_UPDATE) ||
                loginResponseEvent.getResponse().equals(RUNESCAPE_UPDATE_2)) {

            String[] info = new String[]{RSPeer.getGameAccount().getUsername(), RSPeer.getGameAccount().getPassword()};
            int world = getNextWorld();

            try {
                new ClientQuickLauncher("Account Driver", false, world).launchClient(info);
                killClient();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void getComponents() {
        nameAcceptedWidget = Interfaces.getComponent(558, 12);
        nameLookupWidget = Interfaces.getComponent(558, 17);
        nameInputWidget = Interfaces.getComponent(162, 44);
        nameSetWidget = Interfaces.getComponent(558, 18);
        nameScreenDetectionWidget = Interfaces.getComponent(558, 3, 1);
        creationScreenWidget = Interfaces.getComponent(269, 120);
    }

    @Override
    public int loop() {
        config = Varps.get(VARP);
        doDefault = false;
        Predicate<String> defaultAction = a -> true;

        //Log.info("" + config);

        if (!Game.isLoggedIn() || Players.getLocal() == null
                || Players.getLocal().getAnimation() != -1 || Players.getLocal().isMoving()) {
            return 1000;
        }

        switch (config) {
            case 0:
            case 1:
            case 2:
                getComponents();
                if (nameScreenDetectionWidget != null && nameScreenDetectionWidget.isVisible()) {
                    setDisplayName();
                } else if (isCreationScreenVisible()) {
                    try {
                        createRandomCharacter();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else if (Dialog.isViewingChatOptions()) {//experienceWidget.get(getWidgets()).isPresent()) {
                    if (Dialog.process(randInt(1, 3))) {
                        Time.sleep(400, 800);
                    }
                } else {
                    doDefault = true;
                }
                break;
            case 3:
                if (Tabs.getOpen() == Tab.OPTIONS) {
                    doDefault = true;
                } else {
                    Tabs.open(Tab.OPTIONS);
                    doDefault = true;
                }
                break;
            case 7:
            case 10:
                int componentIndex = -1;
                if (Varps.get(168) != 4) {
                    componentIndex = 24;
                } else if (Varps.get(169) != 4) {
                    componentIndex = 30;
                } else if (Varps.get(872) != 4) {
                    componentIndex = 36;
                }

                if (componentIndex != -1) {
                    doDefault = true;
                    break;
                }

                InterfaceComponent mute = Interfaces.getComponent(261, componentIndex);
                if (mute == null) {
                    Interfaces.getComponent(261, 1, 2).interact(a -> true);
                } else {
                    mute.interact(a -> true);
                }
                break;
            case 20:
                if (!Movement.isRunEnabled()) {
                    Movement.toggleRun(true);
                    Time.sleep(400, 800);
                }

                doDefault = true;
                break;
            case 30:
                if (!Tabs.isOpen(Tab.INVENTORY)) {
                    Tabs.open(Tab.INVENTORY);
                }
                if (Dialog.canContinue()) {
                    Dialog.processContinue();
                } else {
                    Interfaces.getComponent(164, 53).interact(a -> true);
                }
                break;
            case 50:
                Tabs.open(Tab.SKILLS);
                break;
            case 70:
                interact(InteractableType.SceneObject, "Tree", a -> a.equals("Chop down"), randInt(1, 6), true);
                break;
            case 80:
            case 90:
            case 100:
            case 110:
                if (!Tabs.isOpen(Tab.INVENTORY)) {
                    Tabs.open(Tab.INVENTORY);
                } else if (!Inventory.contains("Raw shrimps")) {
                    interact(InteractableType.Npc, "Fishing spot", a -> a.equals("Net"), randInt(1, 6), true);
                } else if (SceneObjects.getNearest("Fire") == null || (Interfaces.getComponent(263, 1, 0) != null &&
                        Interfaces.getComponent(263, 1, 0).getText().contains("time to light a fire"))) {
                    if (!Inventory.contains("Logs")) {
                        SceneObjects.getNearest("Tree").interact("Chop down");
                        Time.sleep(3000);
                    } else {
                        SceneObject fire = SceneObjects.getFirstAt(Players.getLocal().getPosition());
                        if (fire == null || !fire.getName().equals("Fire")) {
                            useItemOn("Logs", Inventory.getFirst("Tinderbox"));
                        } else {
                            Movement.walkTo(Players.getLocal().getPosition().translate(Random.nextInt(-3, 3), Random.nextInt(-3, 3)));
                            Time.sleep(3000);
                        }
                    }
                } else {
                    if (Inventory.contains("Raw shrimps")) {
                        useItemOn("Raw shrimps", SceneObjects.getNearest("Fire"));

                        if (Varps.get(VARP) != config) {
                            if (Time.sleepUntil(() -> Inventory.contains("Shrimps"), 1000, 6000)) {
                                interact(InteractableType.Npc, "Fishing spot", a -> a.equals("Net"), randInt(1, 6), true);
                                interact(InteractableType.SceneObject, "Tree", a -> a.equals("Chop down"), randInt(1, 6), true);
                            }
                        }
                    } else {
                        interact(InteractableType.Npc, "Fishing spot", a -> a.equals("Net"), randInt(1, 6), true);
                    }
                }
                break;
            case 150:
                useItemOn("Pot of flour", Inventory.getFirst("Bucket of water"));
                break;
            case 160:
                if (Inventory.isItemSelected()) {
                    interact(InteractableType.SceneObject, "Range", defaultAction, randInt(1, 4), false);
                    doDefault = true;
                } else {
                    Inventory.getFirst("Bread dough").interact("Use");
                }
                break;
            case 170:
                Tabs.open(Tab.INVENTORY);
                if (!Movement.isRunEnabled()) {
                    Movement.toggleRun(true);
                    Time.sleep(400, 800);
                } else if (new Position(3085, 3127).distance() > 20) {
                    Movement.walkToRandomized(new Position(3085, 3127));
                } else {
                    doDefault = true;
                    //randWalker(8);
                }
                break;
            case 183:
                Tabs.open(Tab.EMOTES);
                break;
            case 187:
                Interfaces.getComponent(216, 1, 0).interact("Yes");
                break;
            case 190:
                Tabs.open(Tab.OPTIONS);
                break;
            case 200:
                Interfaces.getComponent(160, 22).interact("Toggle Run");
                break;
            case 210:
                if (!Movement.isRunEnabled()) {
                    Movement.toggleRun(true);
                    Time.sleep(400, 800);
                } else if (new Position(3085, 3127).distance() > 20) {
                    Movement.walkToRandomized(new Position(3085, 3127));
                } else {
                    doDefault = true;
                    //randWalker(8);
                }
                break;
            case 230:
                Tabs.open(Tab.QUEST_LIST);
                doDefault = true;
                break;
            case 260:
                if (Players.getLocal().getY() > 9517) {
                    Movement.walkToRandomized(new Position(3081, 9509));
                } else {
                    doDefault = true;
                }
                break;
            case 270:
            case 280:
                defaultAction = a -> a.equals("Prospect");
                doDefault = true;
                break;
            case 320:
                if (Inventory.isItemSelected()) {
                    handleFurnaceButton();
                    doDefault = true;
                } else if (Inventory.getFirst("Tin ore") != null) {
                    Inventory.getFirst("Tin ore").interact("Use");
                } else {
                    interact(InteractableType.SceneObject, "Rocks",
                            a -> a.equals("Mine"), randInt(2, 6), tinPosition, true);
                }
                break;
            case 350:
                if (Interfaces.getComponent(312, 9) != null) {
                    Interfaces.getComponent(312, 9).interact("Smith");
                    Time.sleep(800, 1200);
                    Time.sleepUntil(() -> !Players.getLocal().isAnimating(), 1000, 5000);
                } else {
                    interact(InteractableType.SceneObject, "Rocks",
                            a -> a.equals("Mine"), randInt(2, 6), copperPosition, true);
                    interact(InteractableType.SceneObject, "Rocks",
                            a -> a.equals("Mine"), randInt(2, 6), tinPosition, true);
                    interact(InteractableType.SceneObject, "Furnace", a -> a.equals("Use"), 1, false);
                    handleFurnaceButton();
                    doDefault = true;
                }
                break;
            case 390:
                Tabs.open(Tab.EQUIPMENT);
                doDefault = true;
                break;
            case 400:
                Interfaces.getComponent(387, 17).interact("View equipment stats");
                break;
            case 405:
                wieldItem("Bronze dagger");
                break;
            case 420:
                if (!Equipment.contains("Bronze sword")) {
                    wieldItem("Bronze sword");
                } else if (!Equipment.contains("Wooden shield")) {
                    wieldItem("Wooden shield");
                }
                break;
            case 430:
                openRandomTab();
                Tabs.open(Tab.COMBAT);
                break;
            case 440:
                if (SceneObjects.getNearest(so -> so.getName().equalsIgnoreCase("Gate") && so.getY() > 9515) != null) {
                    SceneObjects.getNearest(so -> so.getName().equalsIgnoreCase("Gate") && so.getY() > 9515).interact("Open");
                } else {
                    Log.severe("Can't find gate");
                }
                break;
            case 450:
            case 460:
                if (Players.getLocal().getTargetIndex() == -1) {
                    interact(InteractableType.Npc, "Giant rat", a -> a.equals("Attack"), randInt(1, 2), false);
                }
                break;
            case 470:
                if (Movement.isInteractable(Npcs.getNearest(3307), false)) {
                    doDefault = true;
                } else {
                    SceneObjects.getNearest(so -> so.getName().equalsIgnoreCase("Gate") && so.getY() > 9515).interact("Open");
                }
                break;
            case 490:
                if (!Equipment.contains("Shortbow")) {
                    wieldItem("Shortbow");
                } else if (!Equipment.contains("Bronze arrow")) {
                    wieldItem("Bronze arrow");
                } else if (Players.getLocal().getTargetIndex() == -1) {
                    interact(InteractableType.Npc, "Giant rat", a -> a.equals("Attack"), randInt(1, 2), false);
                }
                break;
            case 510:
                if (Dialog.isViewingChatOptions()) {
                    Dialog.process(0);
                } else {
                    doDefault = true;
                }
                break;
            case 531:
                openRandomTab();
                Tabs.open(Tab.ACCOUNT_MANAGEMENT);
                doDefault = true;
                break;
            case 550:
                if (Players.getLocal().getY() > 3116) {
                    Movement.walkToRandomized(new Position(3134, 3116));

                } else if (Movement.isInteractable(new Position(3127, 3106), false)) {
                    doDefault = true;
                } else {
                    SceneObjects.getNearest(so -> so.distance(new Position(3127, 3106)) < 5 && so.containsAction("Open")).interact("Open");
                }
                break;
            case 560:
                openRandomTab();
                Tabs.open(Tab.PRAYER);
                doDefault = true;
                break;
            case 580:
                openRandomTab();
                Tabs.open(Tab.FRIENDS_LIST);
                doDefault = true;
                break;
            case 590:
                openRandomTab();
                Tabs.open(Tab.ACCOUNT_MANAGEMENT);
                break;
            case 620:
                if (Players.getLocal().getY() > 3100) {
                    Movement.walkToRandomized(new Position(3131, 3088));
                } else {
                    doDefault = true;
                }
                break;
            case 630:
                Tabs.open(Tab.MAGIC);
                break;
            case 650:
                if (STRIKE_POS.distance() <= 1) {
                    if (Magic.isSpellSelected()) {
                        //interact(InteractableType.Npc, "Chicken", a -> a.contains("Cast"), randInt(1, 2), false);
                        Npcs.getNearest("Chicken").interact(8);
                    } else {
                        Magic.cast(Spell.Modern.WIND_STRIKE);
                    }
                } else {
                    Movement.walkTo(STRIKE_POS.translate(Random.nextInt(-1, 1), Random.nextInt(-1, 1)));
                }
                break;
            case 670:
                if (Dialog.isViewingChatOptions()) {
                    if (Interfaces.getComponent(219, 1, 0).getText().contains("o you want to go to")) {
                        Dialog.process(0);
                    } else {
                        Dialog.process(2);
                    }
                } else {
                    doDefault = true;
                }
                break;
            case 1000:
                if (!onTutIsland()) {
                    randWalker(BankLocation.GRAND_EXCHANGE.getPosition());

                    writeCurrentAccount();
                    disableChain = false;
                    setStopping(true);
                }
                break;
            default:
                doDefault = true;
                break;
        }

        if (doDefault) {
            //Log.info("DEFAULT");
            InterfaceComponent wierdContinue = Interfaces.getComponent(162, 44);
            if (wierdContinue != null && wierdContinue.isVisible() && !wierdContinue.isExplicitlyHidden()) {
                Game.getClient().fireScriptEvent(299, 1, 1);
            } else if (Dialog.canContinue()) {
                Dialog.processContinue();
            } else if (!Dialog.isProcessing()) {
                switch (Game.getClient().getHintArrowType()) {
                    case 0:
                        Log.info("no hint arrow");
                        break;
                    case 1:
                        try {
                            Npcs.getAt(Game.getClient().getHintArrowNpcIndex()).interact(defaultAction);
                        } catch (NullPointerException ignored) {
                            Log.info("Random Walk");
                            getEmptyPosition(false, randInt(3, 8), false).ifPresent(this::randWalker);
                        }
                        break;
                    case 2:
                        Position hintPos = new Position(Game.getClient().getHintArrowX(), Game.getClient().getHintArrowY(), Players.getLocal().getFloorLevel());
                        //Log.info(hintPos.toString());
                        for (SceneObject so : SceneObjects.getAt(hintPos)) {
                            if (so.containsAction(defaultAction)) {
                                if (so.containsAction(a -> a.equals("Open") || a.contains("Climb") || a.equals("Use"))) {
                                    so.interact(defaultAction);
                                } else {
                                    interact(InteractableType.SceneObject, so.getName(), defaultAction, randInt(1, 3), so.getPosition(), false);

                                    if (tinPosition == null && Inventory.contains("Tin ore")) {
                                        tinPosition = so.getPosition();
                                    } else if (copperPosition == null && Inventory.contains("Copper ore")) {
                                        copperPosition = so.getPosition();
                                    }
                                }
                                break;
                            }
                        }
                        break;
                }
            }

            if (Inventory.isFull()) {
                dropDuplicateItems();
            }

            if (randInt(0, 250) == 0) {
                Log.fine("Opening Random Tab");
                openRandomTab();
            }
        }

        return org.rspeer.runetek.api.commons.math.Random.high(800, 3000);
    }

    public final Area TUTORIAL_ISLAND_AREA = Area.rectangular(3049, 3139, 3161, 3057);
    private static final int MAX_TUT_PROGRESS = 670;
    private static final int MAX_TUT_SECTION = 20;

    private boolean onTutIsland() {
        InterfaceComponent tutBar = Interfaces.getComponent(614, 21);

        return (Varps.get(281) <= MAX_TUT_PROGRESS && Varps.get(406) <= MAX_TUT_SECTION) ||
                (tutBar != null && tutBar.isVisible()) ||
                TUTORIAL_ISLAND_AREA.contains(Players.getLocal());
    }

    private void handleFurnaceButton() {
        InterfaceComponent furnaceButton = Interfaces.getComponent(270, 14);
        if (furnaceButton != null && furnaceButton.isVisible()) {
            Log.info("Clicking Furnace Button");
            furnaceButton.interact(ActionOpcodes.INTERFACE_ACTION);
            Time.sleep(500, 1000);
        }
    }

    private void dropDuplicateItems() {
        if (Inventory.getSelectedItem() != null) {
            Inventory.getSelectedItem().interact("Cancel");
        }

        HashSet<String> set = new HashSet<>();
        Item[] duplicates = Inventory.getItems(x -> !set.add(x.getName()));

        if (duplicates != null && duplicates.length > 0) {
            int randDropAmount = randInt(1, duplicates.length);
            Log.info("Dropping " + randDropAmount + " Duplicate Items");
            for (int i = 0; i < randDropAmount; i++) {
                if (duplicates[i] != null) {
                    duplicates[i].interact("Drop");
                    Time.sleep(500, 1500);
                }
            }
        } else {
            Log.fine("Dropping All Items");
            Inventory.getFirst(x -> x.interact("Drop"));
        }
        Time.sleepUntil(() -> !Inventory.isFull(), 1000, 5000);
    }

    private static final Tab[] SWITCH_TABS = new Tab[] {
            Tab.INVENTORY, Tab.MAGIC, Tab.COMBAT, Tab.SKILLS, Tab.FRIENDS_LIST, Tab.PRAYER, Tab.QUEST_LIST, Tab.ACCOUNT_MANAGEMENT } ;

    public void openRandomTab() {
        Tabs.open(SWITCH_TABS[randInt(0, SWITCH_TABS.length - 1)]);
    }

    private void interact(InteractableType type, String name, Predicate<String> action, int amount, boolean distinct) {
        interact(type, name, action, amount, null, distinct);
    }

    private void interact(InteractableType type, String name, Predicate<String> action, int amount, Position position, boolean distinct) {

        int i = 0;
        Log.fine("Interacting with" + " " + amount + " " + name + "(s)");

        while (i < amount && Game.isLoggedIn() && !this.isStopping()) {

            if (Inventory.isFull()) {
                dropDuplicateItems();
            }

            switch (type.key) {
                case 0:
                    SceneObject so = SceneObjects.getNearest(o -> o.getName().equals(name)
                            && (position == null || o.getPosition().equals(position)));

                    if (distinct && so != null) {
                        Position soPosition = so.getPosition();
                        SceneObject nextSo = SceneObjects.getNearest(o
                                -> o.getName().equals(name) && !o.getPosition().equals(soPosition));

                        if (i > 0 && nextSo != null) {
                            so = nextSo;
                        }
                    }

                    if (so != null && so.interact(action)) {
                        Time.sleepUntil(() -> Players.getLocal().isAnimating() || Players.getLocal().isMoving(),
                                1000, 6000);
                        Time.sleep(500, 800);
                        Time.sleepUntil(() -> !Players.getLocal().isAnimating() && !Players.getLocal().isMoving(),
                                2000, 8000);
                        i++;
                    }
                    break;

                case 1:
                    Npc npc = Npcs.getNearest(n -> n.getName().equals(name)
                            && n.getTargetIndex() == -1 && (position == null || n.getPosition().equals(position)));

                    if (distinct && npc != null) {
                        Position npcPosition = npc.getPosition();
                        Npc nextNpc = Npcs.getNearest(n
                                -> n.getName().equals(name) && !n.getPosition().equals(npcPosition) && (n.getTargetIndex() == -1));

                        if (i > 0 && nextNpc != null) {
                            npc = nextNpc;
                        }
                    }

                    if (npc != null && Players.getLocal().getTargetIndex() == -1) {
                        npc.interact(action);
                        Time.sleepUntil(() -> Players.getLocal().getTargetIndex() != -1 || Players.getLocal().isAnimating(),
                                1000, 6000);
                        Time.sleep(500, 800);
                        Time.sleepUntil(() -> Players.getLocal().getTargetIndex() == -1 && !Players.getLocal().isAnimating(),
                                2000, 8000);
                        i++;
                    }
                    break;
            }
            Time.sleep(600, 3000);
        }
        if (Dialog.canContinue()) {
            Dialog.processContinue();
            Time.sleepUntil(() -> !Dialog.canContinue() && !Dialog.isProcessing(), 5000);
        }
    }

    public enum InteractableType {
        SceneObject(0),
        Npc(1);

        private final int key;

        InteractableType(int key) {
            this.key = key;
        }
    }

    private boolean randWalker(int chanceIn100, int distance) {
        int chance = 100 / chanceIn100;
        if (randInt(1, chance) == 1 && !Players.getLocal().isMoving() && !Dialog.isOpen()) {
            Log.fine("Random Walk");
            //Movement.walkToRandomized(Players.getLocal().getPosition().randomize(distance));
            getEmptyPosition(false, randInt(1, distance), true).ifPresent(Movement::walkTo);
            Time.sleepUntil(() -> Players.getLocal().isMoving(), randInt(800, 1500));
            Time.sleepUntil(() -> !Players.getLocal().isMoving(), 1000, randInt(2000, 5000));
            return true;
        }
        return false;
    }

    private void useItemOn(String itemName, Interactable target) {
        Log.info("Using " + itemName + " on " + "target");
        if (Inventory.isItemSelected()) {
            if (target.interact("Use")) {
                Time.sleepUntil(() -> Varps.get(VARP) != config, 2000, 30 * 1000);
            }
        } else if (Inventory.contains(itemName)) {
            Inventory.getFirst(itemName).interact("Use");
        } else {
            doDefault = true;
        }
    }

    private void setDisplayName() {
        if (nameAcceptedWidget != null && nameAcceptedWidget.isVisible() && nameAcceptedWidget.getText().contains("Great!")) {
            if (nameSetWidget != null && nameSetWidget.isVisible() && nameSetWidget.interact(ActionOpcodes.INTERFACE_ACTION)) {
                Time.sleepUntil(() -> !nameScreenDetectionWidget.isVisible(), 2000, 8000);
                Time.sleep(400, 800);
            }
        } else if (nameInputWidget != null && nameInputWidget.isVisible()
                && !nameInputWidget.isExplicitlyHidden()
                && nameInputWidget.getText().contains("unique")) {

            Keyboard.sendText(getRandString(3, 7));
            //Keyboard.sendText(generateRandomString(7));
            Keyboard.pressEnter();
            final int configValue = Varps.get(1042);

            Time.sleepUntil(() -> Varps.get(1042) != configValue, 2000, 8000);
            Time.sleepUntil(() -> Varps.get(1042) == configValue || (nameAcceptedWidget != null && nameAcceptedWidget.isVisible()), 2000, 8000);
            Time.sleep(400, 800);
        } else if (nameLookupWidget != null && nameLookupWidget.isVisible()
                && nameLookupWidget.interact(ActionOpcodes.INTERFACE_ACTION)) {
            Time.sleepUntil(() -> nameInputWidget != null && nameInputWidget.isVisible() && !nameInputWidget.isExplicitlyHidden(), 2000, 8000);
            Time.sleep(400, 800);
        }
    }

    private boolean isCreationScreenVisible() {
        return creationScreenWidget != null && creationScreenWidget.isVisible();//.get(getWidgets()).filter(RS2Widget::isVisible).isPresent();
    }

    private void createRandomCharacter() throws InterruptedException {
        if (randInt(0 , 1) == 1) {
            Interfaces.getComponent(269, 137).interact(ActionOpcodes.INTERFACE_ACTION);//.getWidgetContainingText("Female").interact();
        }

        final InterfaceComponent[] childWidgets = Interfaces.get(creationScreenWidget.getRootIndex());//.getWidgets(creationScreenWidget.get(getWidgets()).get().getRootId());
        Collections.shuffle(Arrays.asList(childWidgets));

        for (final InterfaceComponent childWidget : childWidgets) {
            //Log.info(childWidget.getToolTip());
            if (childWidget.getToolTip() == null) {
                continue;
            }
            if (childWidget.getToolTip().contains("Change") || childWidget.getToolTip().contains("Recolour")) {
                clickRandomTimes(childWidget);
            }
        }

        if (Interfaces.getComponent(269, 99).interact(ActionOpcodes.INTERFACE_ACTION)) {
            Time.sleepUntil(() -> !isCreationScreenVisible(), 2000, 3000);
            Time.sleep(400, 800);
        }
    }

    private void clickRandomTimes(final InterfaceComponent widget) {
        int clickCount = randInt(0, 8);

        for (int i = 0; i < clickCount; i++) {
            if (widget.interact(ActionOpcodes.INTERFACE_ACTION)) {
                Time.sleep(300, 1000);
            }
        }
    }

    private int randInt(int min, int max) {
        java.util.Random rand = new java.util.Random();
        return rand.nextInt(max - min + 1) + min;
    }

    private String getRandString(int minLength, int maxLength) {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklmnopqrstuvwxyz";
        StringBuilder salt = new StringBuilder();
        salt.append(randomPrefix());
        java.util.Random rnd = new java.util.Random();
        int strLen = randInt(minLength, maxLength);

        while (salt.length() < strLen) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }

        return salt.toString();
    }

    private String randomPrefix() {
        StringBuilder salt = new StringBuilder();

        switch (randInt(0, 1)) {
            case 0:
                salt.append(vowels[randInt(0, (vowels.length - 1))]);
                salt.append(nonVowels[randInt(0, (nonVowels.length - 1))]);
                salt.append(vowels[randInt(0, (vowels.length - 1))]);
            case 1:
                salt.append(nonVowels[randInt(0, (nonVowels.length - 1))]);
                salt.append(vowels[randInt(0, (vowels.length - 1))]);
                salt.append(nonVowels[randInt(0, (nonVowels.length - 1))]);
        }
        return salt.toString();
    }

    private void wieldItem(String name) {
        if (Inventory.getFirst(name).interact("Wield") || Inventory.getFirst(name).interact("Equip")) {
            Time.sleepUntil(() -> Equipment.contains(name), 2000, 1500);
            Time.sleep(400, 800);
        }
    }

    private void randWalker(Position posRequired) {
        Log.info("Walking to position");
        while (!Players.getLocal().getPosition().equals(posRequired) && Game.isLoggedIn()) {
            if (!Time.sleepUntil(() -> Players.getLocal().getPosition().equals(posRequired), Random.low(600, 1800))) {
                Movement.walkTo(posRequired);
            }
        }
        if (posRequired.distance(Players.getLocal()) <= 3) {
            int times = randInt(1, 2);
            Log.info("Random walking " + times + " time(s)");
            for (int i = 0; i < times; i++) {
                //Movement.walkToRandomized(Players.getLocal().getPosition().randomize(8));
                getEmptyPosition(false, randInt(1, 9), true).ifPresent(Movement::walkTo);
                Time.sleepUntil(() -> Players.getLocal().isMoving(), randInt(800, 1500));
                Time.sleepUntil(() -> !Players.getLocal().isMoving(), 600, randInt(2000, 4000));
            }
        }
    }

    private Optional<Position> getEmptyPosition(boolean min, int distance, boolean removeNonInteractable) {
        List<Position> allPositions = Area.surrounding(Players.getLocal().getPosition(), distance).getTiles();

        // Remove any position with an object (except ground decorations, as they can be walked on)
        for (SceneObject object : SceneObjects.getLoaded()) {
            if (object.getProvider() instanceof RSTileDecor) {
                continue;
            }
            allPositions.removeIf(position -> object.getPosition().equals(position));
        }

        allPositions.removeIf(position -> !position.isPositionWalkable() || !Movement.isWalkable(position, false));
        allPositions.removeIf(position -> position.distance(Players.getLocal()) <= 0);
        if (removeNonInteractable) {
            allPositions.removeIf(position -> Movement.isInteractable(position, false));
        }

        if (min) {
            return allPositions.stream().min(Comparator.comparingInt(p -> (int) Players.getLocal().getPosition().distance(p)));
        } else {
            return allPositions.stream().max(Comparator.comparingInt(p -> (int) Players.getLocal().getPosition().distance(p)));
        }
    }
}

