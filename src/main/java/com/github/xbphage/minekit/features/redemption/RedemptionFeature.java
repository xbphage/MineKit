package com.github.xbphage.minekit.features.redemption;

import com.github.xbphage.minekit.core.Feature;
import com.github.xbphage.minekit.core.FeatureManager;
import com.github.xbphage.minekit.core.SubCommand;
import com.github.xbphage.minekit.features.redemption.config.RedemptionConfig;
import com.github.xbphage.minekit.features.redemption.listener.RedemptionListener;
import com.github.xbphage.minekit.lang.ItemTranslator;
import com.github.xbphage.minekit.records.RecordManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RedemptionFeature implements Feature {

    private static final String NAME = "redemption";
    private static final String DESCRIPTION = "赎罪 — 消耗物品降低死亡惩罚";

    private boolean enabled;
    private RedemptionListener listener;
    private RedemptionConfig redemptionConfig;

    @Override
    public String getName() { return NAME; }
    @Override
    public String getDescription() { return DESCRIPTION; }
    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public void loadConfig(FileConfiguration config) {
        this.enabled = config.getBoolean("features." + NAME + ".启用", false);
    }

    @Override
    public void onEnable(JavaPlugin plugin) {
        this.redemptionConfig = new RedemptionConfig();
        this.redemptionConfig.load(plugin.getConfig().getConfigurationSection(NAME));

        RecordManager records = RecordManager.get();
        ItemTranslator translator = new ItemTranslator(plugin);

        this.listener = new RedemptionListener(plugin, records, redemptionConfig, translator,
                Arrays.asList(
                    new com.github.xbphage.minekit.features.respawn.config.FrequencyTier(0, 20, 20, parseEffects(Arrays.asList(
                            Arrays.asList("speed",60,1), Arrays.asList("haste",60,1), Arrays.asList("strength",60,1),
                            Arrays.asList("regeneration",30,1), Arrays.asList("night_vision",30,1), Arrays.asList("glowing",60,1)
                    )), "巅峰"),
                    new com.github.xbphage.minekit.features.respawn.config.FrequencyTier(3, 20, 20, Collections.emptyList(), "重生"),
                    new com.github.xbphage.minekit.features.respawn.config.FrequencyTier(7, 18, 18, parseEffects(Arrays.asList(
                            Arrays.asList("nausea",2,1), Arrays.asList("weakness",7,1), Arrays.asList("mining_fatigue",7,1)
                    )), "疲惫"),
                    new com.github.xbphage.minekit.features.respawn.config.FrequencyTier(10, 16, 16, parseEffects(Arrays.asList(
                            Arrays.asList("nausea",3,1), Arrays.asList("weakness",10,1), Arrays.asList("mining_fatigue",10,1)
                    )), "困倦"),
                    new com.github.xbphage.minekit.features.respawn.config.FrequencyTier(15, 12, 10, parseEffects(Arrays.asList(
                            Arrays.asList("nausea",5,1), Arrays.asList("weakness",60,1), Arrays.asList("mining_fatigue",30,1)
                    )), "力不从心"),
                    new com.github.xbphage.minekit.features.respawn.config.FrequencyTier(20, 10, 8, parseEffects(Arrays.asList(
                            Arrays.asList("blindness",5,1), Arrays.asList("weakness",90,1),
                            Arrays.asList("mining_fatigue",60,1), Arrays.asList("slowness",3,1)
                    )), "眼前一黑")
                ));

        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        plugin.getLogger().info("[Redemption] 功能已启用");
    }

    @Override
    public void onDisable(JavaPlugin plugin) {
        if (listener != null) {
            FeatureManager.unregisterListener(listener);
            listener = null;
        }
        plugin.getLogger().info("[Redemption] 功能已禁用");
    }

    @Override
    public List<SubCommand> getSubCommands() {
        // redemption 和 redeem 两个子命令
        return Arrays.asList(
            new SubCommand() {
                @Override public String getName() { return "redemption"; }
                @Override public String getPermission() { return null; }
                @Override public boolean execute(CommandSender sender, String[] args) {
                    if (!(sender instanceof Player)) return true;
                    if (listener == null) return true;
                    int deaths = args.length > 0 ? parseInt(args[0]) : RecordManager.get().getDeathsLastHour(((Player)sender).getUniqueId().toString());
                    int offset = args.length > 1 ? parseInt(args[1]) : RecordManager.get().getRedemptionOffset(((Player)sender).getUniqueId().toString());
                    listener.sendRedemptionPanel((Player)sender, deaths, offset);
                    return true;
                }
                @Override public List<String> tabComplete(CommandSender sender, String[] args) { return Collections.emptyList(); }
            },
            new SubCommand() {
                @Override public String getName() { return "redeem"; }
                @Override public String getPermission() { return null; }
                @Override public boolean execute(CommandSender sender, String[] args) {
                    if (!(sender instanceof Player)) return true;
                    if (listener == null || args.length < 3) return true;
                    listener.redeem((Player)sender, args[0], parseInt(args[1]), parseInt(args[2]));
                    return true;
                }
                @Override public List<String> tabComplete(CommandSender sender, String[] args) { return Collections.emptyList(); }
            }
        );
    }

    private List<com.github.xbphage.minekit.features.respawn.config.EffectData> parseEffects(List<List<?>> raw) {
        List<com.github.xbphage.minekit.features.respawn.config.EffectData> list = new java.util.ArrayList<>();
        for (List<?> e : raw) {
            String type = e.get(0).toString().toUpperCase();
            int d = ((Number)e.get(1)).intValue();
            int a = ((Number)e.get(2)).intValue();
            list.add(new com.github.xbphage.minekit.features.respawn.config.EffectData(type, d, a));
        }
        return list;
    }

    private int parseInt(String s) { try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; } }
}
