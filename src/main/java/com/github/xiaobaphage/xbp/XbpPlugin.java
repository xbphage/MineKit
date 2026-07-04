package com.github.xiaobaphage.xbp;

import com.github.xiaobaphage.xbp.core.FeatureManager;
import com.github.xiaobaphage.xbp.core.command.XbpCommand;
import com.github.xiaobaphage.xbp.features.antitrample.AntiTrampleFeature;
import com.github.xiaobaphage.xbp.features.deathback.DeathBackFeature;
import com.github.xiaobaphage.xbp.features.killstats.KillStatsFeature;
import com.github.xiaobaphage.xbp.features.pvp.PvpFeature;
import com.github.xiaobaphage.xbp.features.respawn.RespawnFeature;
import org.bukkit.plugin.java.JavaPlugin;

public class XbpPlugin extends JavaPlugin {

    private FeatureManager featureManager;
    private XbpCommand xbpCommand;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        featureManager = new FeatureManager(this);

        featureManager.register(new RespawnFeature());
        featureManager.register(new AntiTrampleFeature());
        featureManager.register(new PvpFeature());
        featureManager.register(new KillStatsFeature());
        featureManager.register(new DeathBackFeature());

        xbpCommand = new XbpCommand(this, featureManager);

        getCommand("xbp").setExecutor(xbpCommand);
        getCommand("xbp").setTabCompleter(xbpCommand);

        featureManager.reloadAll();
        getLogger().info("Xbp 插件已启用");
    }

    @Override
    public void onDisable() {
        if (featureManager != null) {
            featureManager.disableAll();
        }
        getLogger().info("Xbp 插件已禁用");
    }
}
