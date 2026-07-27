package com.shiyiju.auction.support;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Idempotent bootstrap for installations that do not yet run Flyway. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionSchemaInitializer {
    private final JdbcTemplate jdbc;
    @Value("${auction.seed-demo:false}") private boolean seedDemo;

    @PostConstruct
    public void init() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS auction_session (
              id BIGINT NOT NULL AUTO_INCREMENT, title VARCHAR(128) NOT NULL,
              cover_image VARCHAR(512), description VARCHAR(1000), rules TEXT,
              start_time DATETIME NOT NULL, end_time DATETIME NOT NULL, status INT NOT NULL DEFAULT 1,
              total_lots INT NOT NULL DEFAULT 0, total_bids INT NOT NULL DEFAULT 0,
              create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id), KEY idx_auction_session_status_time (status,start_time,end_time)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS auction_lot (
              id BIGINT NOT NULL AUTO_INCREMENT, session_id BIGINT NOT NULL, artwork_id BIGINT,
              lot_no VARCHAR(32) NOT NULL, title VARCHAR(128) NOT NULL, cover_image VARCHAR(512), artist_name VARCHAR(100),
              start_price DECIMAL(12,2) NOT NULL DEFAULT 0, reserve_price DECIMAL(12,2) NOT NULL DEFAULT 0,
              current_price DECIMAL(12,2) NOT NULL DEFAULT 0, estimate_price VARCHAR(64),
              increment DECIMAL(12,2) NOT NULL DEFAULT 100, deposit_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
              bid_count INT NOT NULL DEFAULT 0, buyer_id BIGINT, status INT NOT NULL DEFAULT 0,
              start_time DATETIME NOT NULL, end_time DATETIME NOT NULL,
              create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id), UNIQUE KEY uk_auction_lot_no (session_id,lot_no),
              KEY idx_auction_lot_session (session_id,status), KEY idx_auction_lot_end (status,end_time)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS auction_bid (
              id BIGINT NOT NULL AUTO_INCREMENT, lot_id BIGINT NOT NULL, user_id BIGINT NOT NULL,
              bid_price DECIMAL(12,2) NOT NULL, bid_time DATETIME NOT NULL, status INT NOT NULL DEFAULT 1,
              request_id VARCHAR(64), PRIMARY KEY (id), UNIQUE KEY uk_auction_bid_request (user_id,request_id),
              KEY idx_auction_bid_lot_time (lot_id,bid_time), KEY idx_auction_bid_user (user_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS auction_deposit (
              id BIGINT NOT NULL AUTO_INCREMENT, session_id BIGINT NOT NULL, lot_id BIGINT,
              user_id BIGINT NOT NULL, amount DECIMAL(12,2) NOT NULL, pay_status INT NOT NULL DEFAULT 0,
              pay_no VARCHAR(64) NOT NULL, pay_channel VARCHAR(16) NOT NULL,
              transaction_id VARCHAR(128), refund_no VARCHAR(64), pay_time DATETIME, refund_time DATETIME,
              expire_time DATETIME, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id), UNIQUE KEY uk_auction_deposit_session_user (session_id,user_id),
              UNIQUE KEY uk_auction_deposit_pay_no (pay_no), KEY idx_auction_deposit_status (pay_status,update_time)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
        // Upgrade developer databases created by the previous bootstrap.
        addColumn("auction_session", "rules", "TEXT DEFAULT NULL");
        addColumn("auction_bid", "request_id", "VARCHAR(64) DEFAULT NULL");
        addColumn("auction_deposit", "session_id", "BIGINT DEFAULT NULL");
        addColumn("auction_deposit", "pay_no", "VARCHAR(64) DEFAULT NULL");
        addColumn("auction_deposit", "pay_channel", "VARCHAR(16) DEFAULT NULL");
        addColumn("auction_deposit", "refund_no", "VARCHAR(64) DEFAULT NULL");
        addColumn("auction_deposit", "expire_time", "DATETIME DEFAULT NULL");
        addColumn("auction_deposit", "update_time", "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
        if (seedDemo) log.warn("AUCTION_SEED_DEMO 已开启；生产环境必须保持 false");
    }

    private void addColumn(String table, String column, String definition) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND COLUMN_NAME=?",
                Integer.class, table, column);
        if (count != null && count == 0) jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
    }
}
