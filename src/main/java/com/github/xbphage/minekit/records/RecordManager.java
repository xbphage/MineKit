package com.github.xbphage.minekit.records;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.logging.Logger;

public class RecordManager {

    private static RecordManager instance;
    private final Logger logger;
    private Connection conn;
    private int rank = 0;

    public RecordManager(JavaPlugin plugin) {
        instance = this;
        this.logger = plugin.getLogger();
        try {
            File dbFile = new File(plugin.getDataFolder(), "records.db");
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            createTables();
            logger.info("[记录] 数据库已连接");
        } catch (Exception e) {
            logger.severe("[记录] 数据库连接失败: " + e.getMessage());
        }
    }

    public static RecordManager get() { return instance; }

    private void createTables() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS death_records (id INTEGER PRIMARY KEY AUTOINCREMENT, player_uuid TEXT NOT NULL, player_name TEXT NOT NULL, death_time TEXT NOT NULL, reason TEXT, world TEXT, x REAL, y REAL, z REAL, killer_uuid TEXT, killer_name TEXT)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_death_player ON death_records(player_uuid)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_death_killer ON death_records(killer_uuid)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_death_time ON death_records(death_time)");
            st.execute("CREATE TABLE IF NOT EXISTS redemption_records (id INTEGER PRIMARY KEY AUTOINCREMENT, player_uuid TEXT NOT NULL, player_name TEXT NOT NULL, redeem_time TEXT NOT NULL, amount INTEGER NOT NULL)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_redeem_player ON redemption_records(player_uuid)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_redeem_time ON redemption_records(redeem_time)");
        }
    }

    public void recordDeath(String playerUuid, String playerName, String reason, Location location, String killerUuid, String killerName) {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO death_records (player_uuid, player_name, death_time, reason, world, x, y, z, killer_uuid, killer_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, playerUuid); ps.setString(2, playerName);
            ps.setString(3, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            ps.setString(4, reason != null ? reason : "unknown");
            ps.setString(5, location != null ? location.getWorld().getName() : "?");
            ps.setDouble(6, location != null ? location.getX() : 0);
            ps.setDouble(7, location != null ? location.getY() : 0);
            ps.setDouble(8, location != null ? location.getZ() : 0);
            ps.setString(9, killerUuid); ps.setString(10, killerName);
            ps.executeUpdate();
        } catch (SQLException e) { logger.warning("[记录] 写入失败: " + e.getMessage()); }
    }

    public void recordRedemption(String playerUuid, String playerName, int amount) {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO redemption_records (player_uuid, player_name, redeem_time, amount) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, playerUuid); ps.setString(2, playerName);
            ps.setString(3, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            ps.setInt(4, amount);
            ps.executeUpdate();
        } catch (SQLException e) { logger.warning("[记录] 写入赎罪记录失败: " + e.getMessage()); }
    }

    public int getRedemptionAmount(String playerUuid) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(amount), 0) FROM redemption_records WHERE player_uuid = ? AND redeem_time >= datetime('now', '-60 minutes')")) {
            ps.setString(1, playerUuid);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { logger.warning("[记录] 查询赎罪额失败: " + e.getMessage()); }
        return 0;
    }

    public int getRedemptionOffset(String playerUuid) { return getRedemptionAmount(playerUuid); }

    public int getDeathsLastHour(String playerUuid) {
        return queryInt("SELECT COUNT(*) FROM death_records WHERE player_uuid = ? AND killer_uuid IS NULL AND death_time >= datetime('now', '-60 minutes')", playerUuid);
    }

    public int getDeathsToday(String playerUuid) { return getDeathsLastHour(playerUuid); }

    public int getTotalDeaths(String playerUuid) {
        return queryInt("SELECT COUNT(*) FROM death_records WHERE player_uuid = ? AND killer_uuid IS NULL", playerUuid);
    }

    public int getKillsLastHour(String playerUuid) {
        return queryInt("SELECT COUNT(*) FROM death_records WHERE killer_uuid = ? AND death_time >= datetime('now', '-60 minutes')", playerUuid);
    }

    public int getKillsToday(String playerUuid) { return getKillsLastHour(playerUuid); }

    public int getTotalKills(String playerUuid) {
        return queryInt("SELECT COUNT(*) FROM death_records WHERE killer_uuid = ?", playerUuid);
    }

    public int getKillsOfPlayer(String killerUuid, String victimUuid) {
        return queryInt("SELECT COUNT(*) FROM death_records WHERE killer_uuid = ? AND player_uuid = ?", killerUuid, victimUuid);
    }

    public List<RankEntry> getKillsTop(int limit) {
        return queryTop("SELECT killer_uuid, killer_name, COUNT(*) AS cnt FROM death_records WHERE killer_uuid IS NOT NULL GROUP BY killer_uuid ORDER BY cnt DESC LIMIT ?", limit);
    }

    public List<RankEntry> getDeathsTop(int limit) {
        return queryTop("SELECT player_uuid, player_name, COUNT(*) AS cnt FROM death_records WHERE killer_uuid IS NULL GROUP BY player_uuid ORDER BY cnt DESC LIMIT ?", limit);
    }

    public List<DetailEntry> getKillDetails(String killerUuid) {
        List<DetailEntry> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT player_uuid, player_name, COUNT(*) AS cnt FROM death_records WHERE killer_uuid = ? GROUP BY player_uuid ORDER BY cnt DESC")) {
            ps.setString(1, killerUuid);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(new DetailEntry(rs.getString("player_uuid"), rs.getString("player_name"), rs.getInt("cnt"))); }
        } catch (SQLException e) { logger.warning("[记录] 查询击杀明细失败: " + e.getMessage()); }
        return list;
    }

    public List<DeathRecord> getRecentDeaths(String playerUuid, int limit) {
        List<DeathRecord> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM death_records WHERE player_uuid = ? ORDER BY id DESC LIMIT ?")) {
            ps.setString(1, playerUuid); ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(new DeathRecord(rs.getString("death_time"), rs.getString("reason"), rs.getString("world"), rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"), rs.getString("killer_name"))); }
        } catch (SQLException e) { logger.warning("[记录] 查询死亡记录失败: " + e.getMessage()); }
        return list;
    }

    private int queryInt(String sql, String... params) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setString(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { logger.warning("[记录] 查询失败: " + e.getMessage()); }
        return 0;
    }

    private List<RankEntry> queryTop(String sql, int limit) {
        List<RankEntry> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) { rank = 0; while (rs.next()) list.add(new RankEntry(++rank, rs.getString(1), rs.getString(2), rs.getInt(3))); }
        } catch (SQLException e) { logger.warning("[记录] 排行查询失败: " + e.getMessage()); }
        return list;
    }

    public void close() { try { if (conn != null && !conn.isClosed()) conn.close(); } catch (SQLException ignored) {} }

    public static class RankEntry {
        private final int rank; private final String uuid; private final String name; private final int count;
        public RankEntry(int rank, String uuid, String name, int count) { this.rank = rank; this.uuid = uuid; this.name = name; this.count = count; }
        public int getRank() { return rank; } public String getUuid() { return uuid; } public String getName() { return name; } public int getCount() { return count; }
    }
    public static class DetailEntry {
        private final String uuid; private final String name; private final int count;
        public DetailEntry(String uuid, String name, int count) { this.uuid = uuid; this.name = name; this.count = count; }
        public String getUuid() { return uuid; } public String getName() { return name; } public int getCount() { return count; }
    }
    public static class DeathRecord {
        private final String time, reason, world; private final double x, y, z; private final String killerName;
        public DeathRecord(String time, String reason, String world, double x, double y, double z, String killerName) {
            this.time = time; this.reason = reason; this.world = world; this.x = x; this.y = y; this.z = z; this.killerName = killerName;
        }
        public String getTime() { return time; } public String getReason() { return reason; } public String getWorld() { return world; }
        public String getLocation() { return String.format("%s %.0f %.0f %.0f", world, x, y, z); }
        public String getKillerName() { return killerName; }
    }
}
