package com.github.xbphage.minekit;

import com.github.xbphage.minekit.config.ConfigUpgrader;
import com.github.xbphage.minekit.config.ConfigValidator;
import com.github.xbphage.minekit.core.FeatureManager;
import com.github.xbphage.minekit.core.command.MineKitCommand;
import com.github.xbphage.minekit.debug.DebugUtil;
import com.github.xbphage.minekit.features.antitrample.AntiTrampleFeature;
import com.github.xbphage.minekit.features.deathback.DeathBackFeature;
import com.github.xbphage.minekit.features.killstats.KillStatsFeature;
import com.github.xbphage.minekit.features.pvp.PvpFeature;
import com.github.xbphage.minekit.features.report.ReportFeature;
import com.github.xbphage.minekit.features.redemption.RedemptionFeature;
import com.github.xbphage.minekit.features.respawn.RespawnFeature;
import com.github.xbphage.minekit.records.RecordListener;
import com.github.xbphage.minekit.records.RecordManager;
import com.github.xbphage.minekit.records.expansion.StatsExpansion;
import com.github.xbphage.minekit.sudo.SudoCommand;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("all")
public class MineKitPlugin extends JavaPlugin {

    private FeatureManager featureManager;
    private RecordManager recordManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ConfigUpgrader.upgrade(this);
        ConfigValidator.validate(this);
        DebugUtil.init(this);

        recordManager = new RecordManager(this);
        getServer().getPluginManager().registerEvents(new RecordListener(recordManager), this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new StatsExpansion(recordManager).register();
            getLogger().info("[占位符] %minekit_deaths_hour% %minekit_deaths_total% 等已注册");
        }

        featureManager = new FeatureManager(this);
        featureManager.register(new RespawnFeature());
        featureManager.register(new AntiTrampleFeature());
        featureManager.register(new PvpFeature());
        featureManager.register(new KillStatsFeature());
        featureManager.register(new DeathBackFeature());
        featureManager.register(new ReportFeature());
        featureManager.register(new RedemptionFeature());

        MineKitCommand cmd = new MineKitCommand(this, featureManager);
        featureManager.setMineKitCommand(cmd);

        getCommand("minekit").setExecutor(cmd);
        getCommand("minekit").setTabCompleter(cmd);

        SudoCommand sudoCommand = new SudoCommand();
        getCommand("sudo").setExecutor(sudoCommand);
        getCommand("sudo").setTabCompleter(sudoCommand);

        featureManager.reloadAll();
        getLogger().info("MineKit 插件已启用");
    }

    @Override
    public void onDisable() {
        if (featureManager != null) featureManager.disableAll();
        if (recordManager != null) recordManager.close();
        getLogger().info("MineKit 插件已禁用");
    }
}
