package com.github.xiaobaphage.xbp.core;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * 功能模块接口。
 * 每个独立功能实现此接口，通过 FeatureManager 统一管理生命周期。
 *
 * 关闭 = 零开销：禁用时不注册 Listener、不注册指令、不启动定时任务。
 */
public interface Feature {

    /** 唯一标识，如 "respawn" */
    String getName();

    /** 人类可读描述 */
    String getDescription();

    /** 当前是否启用（由 config.yml features.<name>.enabled 控制） */
    boolean isEnabled();

    /**
     * 启用时调用。
     * 在此注册 Listener、注册子命令到 XbpCommand、启动定时任务等。
     */
    void onEnable(JavaPlugin plugin);

    /**
     * 禁用时调用。
     * 在此注销 Listener、取消任务、从 XbpCommand 移除子命令。
     */
    void onDisable(JavaPlugin plugin);

    /** 从 config.yml 读取本模块的配置段 */
    void loadConfig(FileConfiguration config);

    /** 本模块的二级子命令列表，将注册到 /xbp 下 */
    List<SubCommand> getSubCommands();
}
