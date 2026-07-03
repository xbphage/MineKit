package com.github.xiaobaphage.xbp;

import com.github.xiaobaphage.xbp.core.FeatureManager;
import com.github.xiaobaphage.xbp.core.command.XbpCommand;
import com.github.xiaobaphage.xbp.features.antitrample.AntiTrampleFeature;
import com.github.xiaobaphage.xbp.features.respawn.RespawnFeature;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Xbp 插件主入口。
 *
 * 所有功能以 Feature 模块形式注册，通过 FeatureManager 统一管理生命周期。
 */
public class XbpPlugin extends JavaPlugin {

    private FeatureManager featureManager;
    private XbpCommand xbpCommand;

    @Override
    public void onEnable() {
        // 保存默认配置
        saveDefaultConfig();

        // 创建功能管理器
        featureManager = new FeatureManager(this);

        // ==== 注册功能模块 ====
        RespawnFeature respawnFeature = new RespawnFeature();
        featureManager.register(respawnFeature);

        AntiTrampleFeature antiTrampleFeature = new AntiTrampleFeature();
        featureManager.register(antiTrampleFeature);

        // 创建主命令分发器
        xbpCommand = new XbpCommand(featureManager);

        // 注入 XbpCommand 引用给需要注册子命令的 Feature
        respawnFeature.setXbpCommand(xbpCommand);

        // 注册 /xbp 指令
        getCommand("xbp").setExecutor(xbpCommand);
        getCommand("xbp").setTabCompleter(xbpCommand);

        // 加载配置并同步
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
