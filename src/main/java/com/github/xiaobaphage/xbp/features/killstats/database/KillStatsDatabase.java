package com.github.xiaobaphage.xbp.features.killstats.database;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class KillStatsDatabase {

    private final Logger logger;
    private Connection connection;

    public KillStatsDatabase(File dataFolder, Logger logger) {
        this.logger = logger;
        File dbFile = new File(dataFolder, "killstats.db");
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            createTables();
            logger.info("[KillStats] 数据库已连接");
        } catch (Exception e) {
            logger.severe("[KillStats] 数据库连接失败: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS player_stats (" +
                    "  uuid TEXT PRIMARY KEY," +
                    "  player_name TEXT NOT NULL," +
                    "  kills INTEGER NOT NULL DEFAULT 0," +
                    "  deaths INTEGER NOT NULL DEFAULT 0" +
                    ")"
            );
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS kill_records (" +
                    "  killer_uuid TEXT NOT NULL," +
                    "  victim_uuid TEXT NOT NULL," +
                    "  count INTEGER NOT NULL DEFAULT 0," +
                    "  PRIMARY KEY (killer_uuid, victim_uuid)" +
                    ")"
            );
        }
    }

    /** 记录一次击杀 */
    public void recordKill(UUID killerUuid, String killerName, UUID victimUuid, String victimName) {
        try {
            // player_stats: 击杀者 kills +1
            upsertPlayerStats(killerUuid, killerName, "kills", 1);
            // player_stats: 死者 deaths +1
            upsertPlayerStats(victimUuid, victimName, "deaths", 1);
            // kill_records: 击杀关系 +1
            upsertKillRecord(killerUuid, victimUuid);
        } catch (SQLException e) {
            logger.warning("[KillStats] 记录击杀失败: " + e.getMessage());
        }
    }

    private void upsertPlayerStats(UUID uuid, String name, String column, int increment) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO player_stats (uuid, player_name, kills, deaths) VALUES (?, ?, 0, 0) " +
                "ON CONFLICT(uuid) DO UPDATE SET player_name = ?, " + column + " = " + column + " + ?"
        )) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setString(3, name);
            ps.setInt(4, increment);
            ps.executeUpdate();
        }
    }

    private void upsertKillRecord(UUID killerUuid, UUID victimUuid) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO kill_records (killer_uuid, victim_uuid, count) VALUES (?, ?, 1) " +
                "ON CONFLICT(killer_uuid, victim_uuid) DO UPDATE SET count = count + 1"
        )) {
            ps.setString(1, killerUuid.toString());
            ps.setString(2, victimUuid.toString());
            ps.executeUpdate();
        }
    }

    /** 获取玩家的击杀/死亡数据 */
    public PlayerData getPlayerData(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT player_name, kills, deaths FROM player_stats WHERE uuid = ?"
        )) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PlayerData(
                            rs.getString("player_name"),
                            rs.getInt("kills"),
                            rs.getInt("deaths")
                    );
                }
            }
        } catch (SQLException e) {
            logger.warning("[KillStats] 查询失败: " + e.getMessage());
        }
        return new PlayerData(null, 0, 0);
    }

    /** 击杀排行 */
    public List<RankEntry> getKillsTop(int limit) {
        return getTop("kills", limit);
    }

    /** 死亡排行 */
    public List<RankEntry> getDeathsTop(int limit) {
        return getTop("deaths", limit);
    }

    private List<RankEntry> getTop(String column, int limit) {
        List<RankEntry> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT uuid, player_name, " + column + " FROM player_stats ORDER BY " + column + " DESC LIMIT ?"
        )) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    list.add(new RankEntry(rank++, rs.getString("uuid"),
                            rs.getString("player_name"), rs.getInt(column)));
                }
            }
        } catch (SQLException e) {
            logger.warning("[KillStats] 排行查询失败: " + e.getMessage());
        }
        return list;
    }

    /** 某玩家击杀过的玩家明细 */
    public List<KillDetail> getKillDetails(UUID killerUuid) {
        List<KillDetail> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT kr.victim_uuid, ps.player_name, kr.count " +
                "FROM kill_records kr LEFT JOIN player_stats ps ON kr.victim_uuid = ps.uuid " +
                "WHERE kr.killer_uuid = ? ORDER BY kr.count DESC"
        )) {
            ps.setString(1, killerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new KillDetail(rs.getString("victim_uuid"),
                            rs.getString("player_name"), rs.getInt("count")));
                }
            }
        } catch (SQLException e) {
            logger.warning("[KillStats] 明细查询失败: " + e.getMessage());
        }
        return list;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {}
    }

    /* ======== 数据类 ======== */

    public static class PlayerData {
        private final String name;
        private final int kills;
        private final int deaths;
        public PlayerData(String name, int kills, int deaths) {
            this.name = name; this.kills = kills; this.deaths = deaths;
        }
        public String getName() { return name; }
        public int getKills() { return kills; }
        public int getDeaths() { return deaths; }
        public double getKd() {
            return deaths == 0 ? (double) kills : (double) kills / deaths;
        }
    }

    public static class RankEntry {
        private final int rank;
        private final String uuid;
        private final String playerName;
        private final int value;

        public RankEntry(int rank, String uuid, String playerName, int value) {
            this.rank = rank; this.uuid = uuid; this.playerName = playerName; this.value = value;
        }
        public int getRank() { return rank; }
        public String getUuid() { return uuid; }
        public String getPlayerName() { return playerName; }
        public int getValue() { return value; }
    }

    public static class KillDetail {
        private final String victimUuid;
        private final String victimName;
        private final int count;
        public KillDetail(String victimUuid, String victimName, int count) {
            this.victimUuid = victimUuid; this.victimName = victimName; this.count = count;
        }
        public String getVictimUuid() { return victimUuid; }
        public String getVictimName() { return victimName; }
        public int getCount() { return count; }
    }
}
