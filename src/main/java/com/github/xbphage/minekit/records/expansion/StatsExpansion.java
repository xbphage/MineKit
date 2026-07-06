package com.github.xbphage.minekit.records.expansion;

import com.github.xbphage.minekit.records.RecordManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StatsExpansion extends PlaceholderExpansion {

    private final RecordManager db;

    public StatsExpansion(RecordManager db) {
        this.db = db;
    }

    @Override
    public @NotNull String getIdentifier() { return "minekit"; }

    @Override
    public @NotNull String getAuthor() { return "xiaobaphage"; }

    @Override
    public @NotNull String getVersion() { return "1.0.0"; }

    @Override
    public boolean persist() { return true; }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return null;
        String uuid = player.getUniqueId().toString();

        switch (params.toLowerCase()) {
            // 死亡
            case "deaths_hour":
                return String.valueOf(db.getDeathsLastHour(uuid));
            case "deaths_today":
                return String.valueOf(db.getDeathsToday(uuid));
            case "deaths_total":
                return String.valueOf(db.getTotalDeaths(uuid));

            // 击杀
            case "kills_hour":
                return String.valueOf(db.getKillsLastHour(uuid));
            case "kills_today":
                return String.valueOf(db.getKillsToday(uuid));
            case "kills_total":
                return String.valueOf(db.getTotalKills(uuid));
        }
        return null;
    }
}
