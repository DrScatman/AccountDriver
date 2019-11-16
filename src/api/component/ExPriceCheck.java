package api.component;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Map;

public class ExPriceCheck {

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();
    private static final OkHttpClient HTTP_CLIENT_2 = new OkHttpClient();
    private static final Gson GSON = new Gson().newBuilder().create();

    private static final String OLDSCHOOL_RUNESCAPE_API_URL = "http://services.runescape.com/m=itemdb_oldschool/api/";
    private static final String OSBUDDY_EXCHANGE_SUMMARY_URL = "https://storage.googleapis.com/osb-exchange/summary.json";
    private static final String RSBUDDY_EXCHANGE_SUMMARY_URL = "https://rsbuddy.com/exchange/summary.json";
    private static JsonObject RSBUDDY_SUMMARY_JSON;
    private static JsonObject OSBUDDY_SUMMARY_JSON;

    /**
     * Fetches the price of an item from the RuneScape services catalogue details
     *
     * @param id the id of the item
     * @return the price of the item; -1 if failed
     */
    public static int getRSPrice(int id) throws IOException {
        final Request request = new Request.Builder()
                .url(OLDSCHOOL_RUNESCAPE_API_URL + "catalogue/detail.json?item=" + id)
                .get()
                .build();

        final Response response = HTTP_CLIENT.newCall(request).execute();

        if (!response.isSuccessful() || response.body() == null)
            return -1;

        final String priceText = GSON.fromJson(response.body().string(), JsonObject.class)
                .getAsJsonObject("item")
                .getAsJsonObject("current")
                .get("price")
                .getAsString();

        final int price = Integer.parseInt(priceText.replaceAll("\\D+", ""));

        return priceText.matches("[0-9]+") ? price : price * (priceText.charAt(0) == 'k' ? 1000 : 1000000);
    }

    /**
     * Fetches the price of an item from the RuneScape services graph.
     *
     * @param id the id of the item
     * @return the price of the item; -1 if failed
     */
    @SuppressWarnings("unchecked")
    public static int getAccurateRSPrice(int id) throws Exception {
        final Request request = new Request.Builder()
                .url(OLDSCHOOL_RUNESCAPE_API_URL + "graph/" + id + ".json")
                .get()
                .build();

        final Response response = HTTP_CLIENT.newCall(request).execute();

        if (!response.isSuccessful() || response.body() == null)
            return -1;

        final JsonObject jsonObject = GSON.fromJson(response.body().string(), JsonObject.class)
                .getAsJsonObject("daily")
                .getAsJsonObject();

        final int size = jsonObject.entrySet().size();
        final Map.Entry<String, JsonElement> entry = ((Map.Entry<String, JsonElement>) jsonObject.entrySet().toArray()[size - 1]);

        return Integer.parseInt(entry.getValue().getAsString());
    }

    /**
     * Sets the OSBuddy price summary json
     */
    private static void setOSBuddySummaryJson() throws IOException {
        final Request request = new Request.Builder()
                .url(OSBUDDY_EXCHANGE_SUMMARY_URL)
                .get()
                .build();

        final Response response = HTTP_CLIENT.newCall(request).execute();

        if (!response.isSuccessful() || response.body() == null)
            return;

        OSBUDDY_SUMMARY_JSON = GSON.fromJson(response.body().string(), JsonObject.class);
    }

    public static int getOSBuddySellPrice(int id, boolean refresh) throws IOException {
        if (OSBUDDY_SUMMARY_JSON == null || refresh) {
            setOSBuddySummaryJson();
        }
        if (OSBUDDY_SUMMARY_JSON == null) {
            return 0;
        }

        final JsonObject json_objects = OSBUDDY_SUMMARY_JSON.getAsJsonObject(Integer.toString(id));
        if (json_objects == null)
            return 0;

        return json_objects.get("sell_average").getAsInt();
    }

    public static int getOSBuddyBuyPrice(int id, boolean refresh) throws IOException {
        if (OSBUDDY_SUMMARY_JSON == null || refresh) {
            setOSBuddySummaryJson();
        }
        if (OSBUDDY_SUMMARY_JSON == null) {
            return 0;
        }

        final JsonObject json_objects = OSBUDDY_SUMMARY_JSON.getAsJsonObject(Integer.toString(id));
        if (json_objects == null)
            return 0;

        return json_objects.get("buy_average").getAsInt();
    }

    private static void setRSBuddySummaryJson() throws IOException {
        final Request request = new Request.Builder()
                .url(RSBUDDY_EXCHANGE_SUMMARY_URL)
                .get()
                .build();
        final Response response = HTTP_CLIENT_2.newCall(request).execute();
        if (!response.isSuccessful())
            return;

        if (response.body() == null)
            return;

        final Gson gson = new Gson().newBuilder().create();
        RSBUDDY_SUMMARY_JSON = gson.fromJson(response.body().string(), JsonObject.class);
    }

    public static int getRSBuddySellPrice(int id, boolean refresh) throws IOException {
        if (RSBUDDY_SUMMARY_JSON == null || refresh) {
            setRSBuddySummaryJson();
        }
        if (RSBUDDY_SUMMARY_JSON == null) {
            return 0;
        }

        final JsonObject json_objects = RSBUDDY_SUMMARY_JSON.getAsJsonObject(Integer.toString(id));
        if (json_objects == null)
            return 0;

        return json_objects.get("sell_average").getAsInt();
    }

    public static int getRSBuddyBuyPrice(int id, boolean refresh) throws IOException {
        if (RSBUDDY_SUMMARY_JSON == null || refresh) {
            setRSBuddySummaryJson();
        }
        if (RSBUDDY_SUMMARY_JSON == null) {
            return 0;
        }

        final JsonObject json_objects = RSBUDDY_SUMMARY_JSON.getAsJsonObject(Integer.toString(id));
        if (json_objects == null)
            return 0;

        return json_objects.get("buy_average").getAsInt();
    }
}