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
            st.execute("CREATE TABLE IF NOT EXISTS death_records (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "player_uuid TEXT NOT NULL," +
                    "player_name TEXT NOT NULL," +
                    "death_time TEXT NOT NULL," +
                    "reason TEXT," +
                    "world TEXT," +
                    "x REAL, y REAL, z REAL," +
                    "killer_uuid TEXT," +
                    "killer_name TEXT" +
                    ")");
            st.execute("CREATE INDEX IF NOT EXISTS idx_death_player ON death_records(player_uuid)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_death_killer ON death_records(killer_uuid)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_death_time ON death_records(death_time)");
        }
    }

    public void recordDeath(String playerUuid, String playerName, String reason,
                            Location location, String killerUuid, String killerName) {
        String sql = "INSERT INTO death_records (player_uuid, player_name, death_time, reason, world, x, y, z, killer_uuid, killer_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
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

    // ═══════════════════════════════════════════
    //  死亡/击杀查询
    // ═══════════════════════════════════════════

    /** 60 分钟内非 PvP 死亡次数（频率惩罚 + 占位符使用） */
    public int getDeathsLastHour(String playerUuid) {
        return queryInt("SELECT COUNT(*) FROM death_records WHERE player_uuid = ? AND killer_uuid IS NULL AND death_time >= datetime('now', '-60 minutes')", playerUuid);
    }

    /** 本天非 PvP 死亡次数（每天 4:00 为界） */
    public int getDeathsToday(String playerUuid) {
        return queryInt("SELECT COUNT(*) FROM death_records WHERE player_uuid = ? AND killer_uuid IS NULL AND datetime(death_time, '-4 hours', 'start of day') = datetime('now', '-4 hours', 'start of day')", playerUuid);
    }

    /** 总非 PvP 死亡次数 */
    public int getTotalDeaths(String playerUuid) {
        return queryInt("SELECT COUNT(*) FROM death_records WHERE player_uuid = ? AND killer_uuid IS NULL", playerUuid);
    }

    /** 60 分钟内击杀数 */
    public int getKillsLastHour(String playerUuid) {
        return queryInt("SELECT COUNT(*) FROM death_records WHERE killer_uuid = ? AND death_time >= datetime('now', '-60 minutes')", playerUuid);
    }

    /** 本天击杀数 */
    public int getKillsToday(String playerUuid) {
        return queryInt("SELECT COUNT(*) FROM death_records WHERE killer_uuid = ? AND datetime(death_time, '-4 hours', 'start of day') = datetime('now', '-4 hours', 'start of day')", playerUuid);
    }

    /** 总击杀数 */
    public int getTotalKills(String playerUuid) {
        return queryInt("SELECT COUNT(*) FROM death_records WHERE killer_uuid = ?", playerUuid);
    }

    /** 击杀某玩家的次数 */
    public int getKillsOfPlayer(String killerUuid, String victimUuid) {
        return queryInt("SELECT COUNT(*) FROM death_records WHERE killer_uuid = ? AND player_uuid = ?", killerUuid, victimUuid);
    }

    /** 击杀排行 Top N */
    public List<RankEntry> getKillsTop(int limit) {
        return queryTop("SELECT killer_uuid, killer_name, COUNT(*) AS cnt FROM death_records WHERE killer_uuid IS NOT NULL GROUP BY killer_uuid ORDER BY cnt DESC LIMIT ?", limit);
    }

    /** 死亡排行 Top N */
    public List<RankEntry> getDeathsTop(int limit) {
        return queryTop("SELECT player_uuid, player_name, COUNT(*) AS cnt FROM death_records WHERE killer_uuid IS NULL GROUP BY player_uuid ORDER BY cnt DESC LIMIT ?", limit);
    }

    /** 击杀明细 */
    public List<DetailEntry> getKillDetails(String killerUuid) {
        List<DetailEntry> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT player_uuid, player_name, COUNT(*) AS cnt FROM death_records WHERE killer_uuid = ? GROUP BY player_uuid ORDER BY cnt DESC")) {
            ps.setString(1, killerUuid);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(new DetailEntry(rs.getString("player_uuid"), rs.getString("player_name"), rs.getInt("cnt"))); }
        } catch (SQLException e) { logger.warning("[记录] 查询击杀明细失败: " + e.getMessage()); }
        return list;
    }

    /** 最近死亡记录 */
    public List<DeathRecord> getRecentDeaths(String playerUuid, int limit) {
        List<DeathRecord> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM death_records WHERE player_uuid = ? ORDER BY id DESC LIMIT ?")) {
            ps.setString(1, playerUuid); ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new DeathRecord(rs.getString("death_time"), rs.getString("reason"), rs.getString("world"), rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"), rs.getString("killer_name")));
            }
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
            try (ResultSet rs = ps.executeQuery()) {
                int rank = 1;
                while (rs.next()) list.add(new RankEntry(rank++, rs.getString(1), rs.getString(2), rs.getInt(3)));
            }
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
