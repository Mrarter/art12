package com.shiyiju.auction.support;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        ensureAuctionSessionTable();
        ensureAuctionLotTable();
        ensureAuctionBidTable();
        ensureAuctionDepositTable();
        seedDefaultSession();
    }

    private void ensureAuctionSessionTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS auction_session (
              id BIGINT NOT NULL AUTO_INCREMENT,
              title VARCHAR(128) DEFAULT NULL,
              cover_image VARCHAR(512) DEFAULT NULL,
              description VARCHAR(500) DEFAULT NULL,
              start_time DATETIME DEFAULT NULL,
              end_time DATETIME DEFAULT NULL,
              status INT DEFAULT 1,
              total_lots INT DEFAULT 0,
              total_bids INT DEFAULT 0,
              create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
              update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              KEY idx_auction_session_status (status),
              KEY idx_auction_session_start_time (start_time)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
    }

    private void ensureAuctionLotTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS auction_lot (
              id BIGINT NOT NULL AUTO_INCREMENT,
              session_id BIGINT DEFAULT NULL,
              artwork_id BIGINT DEFAULT NULL,
              lot_no VARCHAR(32) DEFAULT NULL,
              title VARCHAR(128) DEFAULT NULL,
              cover_image VARCHAR(512) DEFAULT NULL,
              artist_name VARCHAR(100) DEFAULT NULL,
              start_price DECIMAL(12,2) DEFAULT 0.00,
              reserve_price DECIMAL(12,2) DEFAULT 0.00,
              current_price DECIMAL(12,2) DEFAULT 0.00,
              estimate_price VARCHAR(64) DEFAULT NULL,
              increment DECIMAL(12,2) DEFAULT 100.00,
              deposit_amount DECIMAL(12,2) DEFAULT 0.00,
              bid_count INT DEFAULT 0,
              buyer_id BIGINT DEFAULT NULL,
              status INT DEFAULT 1,
              start_time DATETIME DEFAULT NULL,
              end_time DATETIME DEFAULT NULL,
              create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
              update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              KEY idx_auction_lot_session_id (session_id),
              KEY idx_auction_lot_artwork_id (artwork_id),
              KEY idx_auction_lot_status (status)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
    }

    private void ensureAuctionBidTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS auction_bid (
              id BIGINT NOT NULL AUTO_INCREMENT,
              lot_id BIGINT DEFAULT NULL,
              user_id BIGINT DEFAULT NULL,
              bid_price DECIMAL(12,2) DEFAULT 0.00,
              bid_time DATETIME DEFAULT NULL,
              status INT DEFAULT 1,
              PRIMARY KEY (id),
              KEY idx_auction_bid_lot_id (lot_id),
              KEY idx_auction_bid_user_id (user_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
    }

    private void ensureAuctionDepositTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS auction_deposit (
              id BIGINT NOT NULL AUTO_INCREMENT,
              lot_id BIGINT DEFAULT NULL,
              user_id BIGINT DEFAULT NULL,
              amount DECIMAL(12,2) DEFAULT 0.00,
              pay_status INT DEFAULT 0,
              pay_time DATETIME DEFAULT NULL,
              refund_time DATETIME DEFAULT NULL,
              transaction_id VARCHAR(64) DEFAULT NULL,
              create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              KEY idx_auction_deposit_lot_id (lot_id),
              KEY idx_auction_deposit_user_id (user_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
    }

    private void seedDefaultSession() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM auction_session", Integer.class);
        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.update("""
            INSERT INTO auction_session (
              title, cover_image, description, start_time, end_time, status,
              total_lots, total_bids, create_time, update_time
            ) VALUES (
              '拾艺局春季艺术收藏专场',
              '/upload/images/2026/05/01/57de9acd0d67471596b0a719ebfc7a07.png',
              '精选平台认证艺术家作品，支持在线竞拍与收藏咨询。',
              DATE_SUB(NOW(), INTERVAL 1 DAY),
              DATE_ADD(NOW(), INTERVAL 7 DAY),
              1,
              2,
              0,
              NOW(),
              NOW()
            )
            """);
        Long sessionId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (sessionId == null) {
            return;
        }

        jdbcTemplate.update("""
            INSERT INTO auction_lot (
              session_id, artwork_id, lot_no, title, cover_image, artist_name,
              start_price, reserve_price, current_price, estimate_price, increment,
              deposit_amount, bid_count, status, start_time, end_time, create_time, update_time
            ) VALUES (?, 37, '001', '静物0752', '/upload/images/2026/05/01/57de9acd0d67471596b0a719ebfc7a07.png',
              '孟儒', 8000.00, 8000.00, 10100.00, '¥8,000 - ¥12,000', 500.00, 1000.00, 0, 1,
              DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY), NOW(), NOW())
            """, sessionId);
        jdbcTemplate.update("""
            INSERT INTO auction_lot (
              session_id, artwork_id, lot_no, title, cover_image, artist_name,
              start_price, reserve_price, current_price, estimate_price, increment,
              deposit_amount, bid_count, status, start_time, end_time, create_time, update_time
            ) VALUES (?, 19, '002', '启航', '/upload/images/2026/04/27/42309a412da343f3ba2f8a4269237d76.png',
              '李小龙', 9000.00, 9000.00, 12407.00, '¥9,000 - ¥15,000', 500.00, 1000.00, 0, 1,
              DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY), NOW(), NOW())
            """, sessionId);
        log.info("初始化默认拍卖专场完成: sessionId={}", sessionId);
    }
}
