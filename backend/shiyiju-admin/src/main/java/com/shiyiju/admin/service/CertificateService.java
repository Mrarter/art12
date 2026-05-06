package com.shiyiju.admin.service;

import com.shiyiju.admin.service.support.SchemaInspector;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 收藏证书管理服务
 */
@Service
public class CertificateService {

    private final JdbcTemplate jdbcTemplate;
    private final SchemaInspector schemaInspector;

    public CertificateService(JdbcTemplate jdbcTemplate, SchemaInspector schemaInspector) {
        this.jdbcTemplate = jdbcTemplate;
        this.schemaInspector = schemaInspector;
    }

    public Map<String, Object> getList(int page, int size, String keyword, String status) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (cert_no LIKE ? OR artwork_name LIKE ?)");
            String kw = "%" + keyword.trim() + "%";
            args.add(kw);
            args.add(kw);
        }
        if (status != null && !status.isBlank() && !"all".equals(status)) {
            where.append(" AND status = ?");
            args.add(status);
        }
        long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM collection_certificate" + where, Long.class, args.toArray()
        );
        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
            "SELECT * FROM collection_certificate" + where + " ORDER BY id DESC LIMIT ?, ?",
            queryArgs.toArray()
        );
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    public Map<String, Object> getById(Long id) {
        try {
            return jdbcTemplate.queryForMap("SELECT * FROM collection_certificate WHERE id = ?", id);
        } catch (Exception e) {
            return null;
        }
    }

    public Map<String, Object> generate(Map<String, Object> params) {
        String certNo = "CERT" + System.currentTimeMillis();
        String artworkName = Objects.toString(params.get("artworkName"), "");
        String holderName = Objects.toString(params.get("holderName"), "");
        jdbcTemplate.update(
            "INSERT INTO collection_certificate (cert_no, artwork_name, holder_name, status, issue_date, create_time) VALUES (?, ?, ?, 'ISSUED', ?, ?)",
            certNo, artworkName, holderName, LocalDateTime.now(), LocalDateTime.now()
        );
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("certNo", certNo);
        result.put("id", jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class));
        return result;
    }
}
