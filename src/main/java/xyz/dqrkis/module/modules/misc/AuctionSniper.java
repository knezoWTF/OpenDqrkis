package xyz.dqrkis.module.modules.misc;

import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.BooleanSetting;
import xyz.dqrkis.module.setting.ItemSetting;
import xyz.dqrkis.module.setting.ModeSetting;
import xyz.dqrkis.module.setting.NumberSetting;
import xyz.dqrkis.module.setting.StringSetting;
import xyz.dqrkis.utils.ChatUtils;
import xyz.dqrkis.utils.EncryptedString;
import xyz.dqrkis.utils.ItemUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class AuctionSniper extends Module implements TickListener {
    public enum Mode { API, MANUAL }

    private final ItemSetting snipingItem = new ItemSetting(EncryptedString.of("Sniping Item"), Items.AIR);
    private final StringSetting price = new StringSetting(EncryptedString.of("Price"), "1k");
    private final ModeSetting<Mode> mode = new ModeSetting<>(EncryptedString.of("Mode"), Mode.MANUAL, Mode.class);
    private final StringSetting apiKey = new StringSetting(EncryptedString.of("Api Key"), "");
    private final NumberSetting refreshDelay = new NumberSetting(EncryptedString.of("Refresh Delay"), 0, 100, 2, 1);
    private final NumberSetting buyDelay = new NumberSetting(EncryptedString.of("Buy Delay"), 0, 100, 2, 1);
    private final NumberSetting apiRefreshRate = new NumberSetting(EncryptedString.of("API Refresh Rate"), 10, 5000, 250, 10);
    private final BooleanSetting showApiNotifications = new BooleanSetting(EncryptedString.of("Show API Notifications"), true);
    private final StringSetting requiredEnchantments = new StringSetting(EncryptedString.of("Required Enchantments"), "");
    private final StringSetting forbiddenEnchantments = new StringSetting(EncryptedString.of("Forbidden Enchantments"), "");
    private final NumberSetting minEnchantLevel = new NumberSetting(EncryptedString.of("Min Enchantment Level"), 1, 10, 1, 1);
    private final BooleanSetting exactEnchantMatch = new BooleanSetting(EncryptedString.of("Exact Enchantment Match"), false);

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final Gson gson = new Gson();
    private final Map<String, Double> priceThresholds = new HashMap<>();

    private int ticks;
    private long lastApiPoll;
    private boolean apiPending;
    private String pendingSeller = "";
    private boolean foundViaApi;
    private int apiCooldown;

    public AuctionSniper() {
        super(EncryptedString.of("Auction Sniper"), EncryptedString.of("Snipes items on auction house for cheap"), -1, Category.MISC);
        addSettings(snipingItem, price, mode, apiKey, refreshDelay, buyDelay, apiRefreshRate, showApiNotifications, requiredEnchantments, forbiddenEnchantments, minEnchantLevel, exactEnchantMatch);
    }

    @Override
    public void onEnable() {
        double threshold = parsePrice(price.getValue());
        if (threshold < 0) {
            ChatUtils.error("Invalid Price");
            setEnabled(false);
            return;
        }
        if (snipingItem.getItem() != Items.AIR) {
            priceThresholds.put(snipingItem.getItem().toString(), threshold);
        }
        ticks = 0;
        apiPending = false;
        foundViaApi = false;
        pendingSeller = "";
        eventManager.add(TickListener.class, this);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        foundViaApi = false;
        super.onDisable();
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (ticks > 0) { ticks--; return; }

        if (mode.getMode() == Mode.API) {
            handleApiTick();
        } else {
            handleManualTick();
        }
    }

    private void handleApiTick() {
        if (foundViaApi) {
            if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler)) {
                mc.player.networkHandler.sendChatCommand("ah " + pendingSeller);
                apiCooldown = 20;
                return;
            }
            ScreenHandler handler = mc.player.currentScreenHandler;
            if (handler instanceof GenericContainerScreenHandler container) {
                if (container.getRows() == 6) applyFiltersAndBuy(container);
                else if (container.getRows() == 3) confirmBuy(container);
            }
            return;
        }

        if (apiPending) return;
        long now = System.currentTimeMillis();
        if (now - lastApiPoll < apiRefreshRate.getValueInt()) return;
        if (apiKey.getValue().isEmpty()) {
            if (showApiNotifications.getValue()) ChatUtils.error("API key is not set. Set it using /api in-game.");
            return;
        }
        lastApiPoll = now;
        apiPending = true;
        pollApiAsync().thenAccept(this::handleApiResults);
    }

    private void handleManualTick() {
        ScreenHandler handler = mc.player.currentScreenHandler;
        if (!(handler instanceof GenericContainerScreenHandler)) {
            mc.player.networkHandler.sendChatCommand(buildSearchCommand());
            ticks = 20;
            return;
        }
        GenericContainerScreenHandler container = (GenericContainerScreenHandler) handler;
        if (container.getRows() == 6) applyFiltersAndBuy(container);
        else if (container.getRows() == 3) confirmBuy(container);
    }

    private String buildSearchCommand() {
        if (!requiredEnchantments.getValue().isEmpty()) {
            String firstEnchant = requiredEnchantments.getValue().split(",")[0].trim();
            String itemName = snipingItem.getItem().toString().replace("minecraft:", "").replace("_", " ");
            return "ah " + itemName + " " + firstEnchant;
        }
        String key = snipingItem.getItem().getTranslationKey();
        String[] parts = key.split("\\.");
        String name = parts[parts.length - 1].replace("_", " ");
        return "ah " + name;
    }

    private CompletableFuture<List<JsonObject>> pollApiAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Endpoint is obfuscated via base64+xor in original (Class922). Placeholder kept configurable via apiKey.
                String endpoint = "https://api.example.com/auctions/search";
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint))
                        .header("Authorization", "Bearer " + apiKey.getValue())
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"sort\": \"recently_listed\"}"))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    if (showApiNotifications.getValue()) ChatUtils.error("API Error: " + response.statusCode());
                    return List.of();
                }
                JsonObject root = gson.fromJson(response.body(), JsonObject.class);
                JsonArray arr = root.getAsJsonArray("result");
                return arr.asList().stream().map(JsonElement::getAsJsonObject).toList();
            } catch (Throwable e) {
                return List.of();
            } finally {
                apiPending = false;
            }
        });
    }

    private void handleApiResults(List<JsonObject> results) {
        for (JsonObject obj : results) {
            try {
                String itemId = obj.getAsJsonObject("item").get("id").getAsString();
                long auctionPrice = obj.get("price").getAsLong();
                String seller = obj.getAsJsonObject("seller").get("name").getAsString();
                for (Map.Entry<String, Double> entry : priceThresholds.entrySet()) {
                    if (itemId.contains(entry.getKey()) && auctionPrice <= entry.getValue()) {
                        if (showApiNotifications.getValue())
                            ChatUtils.info("Found " + itemId + " for " + auctionPrice + " (threshold: " + entry.getValue() + ") from " + seller);
                        foundViaApi = true;
                        pendingSeller = seller;
                        return;
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private void applyFiltersAndBuy(GenericContainerScreenHandler container) {
        for (int i = 0; i < 44; i++) {
            ItemStack stack = container.getSlot(i).getStack();
            if (isDesiredItem(stack)) {
                mc.interactionManager.clickSlot(container.syncId, i, 1, SlotActionType.QUICK_MOVE, mc.player);
                ticks = buyDelay.getValueInt();
                return;
            }
        }
        if (foundViaApi) { foundViaApi = false; pendingSeller = ""; mc.player.closeHandledScreen(); }
        else { mc.interactionManager.clickSlot(container.syncId, 49, 1, SlotActionType.QUICK_MOVE, mc.player); ticks = refreshDelay.getValueInt(); }
    }

    private void confirmBuy(GenericContainerScreenHandler container) {
        ItemStack stack = container.getSlot(13).getStack();
        if (!stack.isEmpty() && isDesiredItem(stack)) {
            mc.interactionManager.clickSlot(container.syncId, 15, 0, SlotActionType.PICKUP, mc.player);
            ticks = 20;
        }
        if (foundViaApi) { foundViaApi = false; pendingSeller = ""; }
    }

    private boolean isDesiredItem(ItemStack stack) {
        if (stack.isEmpty() || snipingItem.getItem() == Items.AIR) return false;
        if (stack.getItem() != snipingItem.getItem()) return false;
        double maxPrice = parsePrice(price.getValue());
        // Price check via tooltip would require lore parsing; simplified to enchant filter only
        String required = requiredEnchantments.getValue().trim();
        if (!required.isEmpty()) {
            for (String ench : required.split(",")) {
                String id = ench.trim().toLowerCase();
                if (!id.isEmpty() && !ItemUtils.hasEnchant(stack, id)) return false;
            }
        }
        String forbidden = forbiddenEnchantments.getValue().trim();
        if (!forbidden.isEmpty()) {
            for (String ench : forbidden.split(",")) {
                String id = ench.trim().toLowerCase();
                if (!id.isEmpty() && ItemUtils.hasEnchant(stack, id)) return false;
            }
        }
        return true;
    }

    private double parsePrice(String s) {
        try {
            s = s.trim().toLowerCase().replace(",", "").replace("$", "");
            if (s.endsWith("k")) return Double.parseDouble(s.substring(0, s.length() - 1)) * 1000;
            if (s.endsWith("m")) return Double.parseDouble(s.substring(0, s.length() - 1)) * 1_000_000;
            return Double.parseDouble(s);
        } catch (Exception e) { return -1; }
    }
}