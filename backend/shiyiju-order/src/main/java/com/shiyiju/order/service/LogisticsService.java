package com.shiyiju.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiyiju.common.constant.OrderConstant;
import com.shiyiju.common.exception.BusinessException;
import com.shiyiju.common.result.ResultCode;
import com.shiyiju.order.dto.ShipOrderDTO;
import com.shiyiju.order.entity.Logistics;
import com.shiyiju.order.entity.LogisticsTrack;
import com.shiyiju.order.entity.Order;
import com.shiyiju.order.mapper.LogisticsMapper;
import com.shiyiju.order.mapper.LogisticsTrackMapper;
import com.shiyiju.order.mapper.OrderMapper;
import com.shiyiju.order.vo.LogisticsTrackVO;
import com.shiyiju.order.vo.LogisticsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 物流服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogisticsService {

    private final LogisticsMapper logisticsMapper;
    private final LogisticsTrackMapper logisticsTrackMapper;
    private final OrderMapper orderMapper;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 订单发货
     */
    @Transactional
    public Logistics shipOrder(ShipOrderDTO dto) {
        Order order = orderMapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (!OrderConstant.STATUS_PAID.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单状态不允许发货");
        }

        // 创建物流记录
        Logistics logistics = new Logistics();
        logistics.setOrderId(dto.getOrderId());
        logistics.setCompanyCode(dto.getCompanyCode());
        logistics.setCompanyName(dto.getCompanyName());
        logistics.setTrackingNo(dto.getTrackingNo());
        logistics.setShipTime(LocalDateTime.now());
        logistics.setStatus(1); // 已发货
        logistics.setReceiverName(order.getReceiverName());
        logistics.setReceiverPhone(order.getReceiverPhone());
        logistics.setReceiverAddress(order.getReceiverAddress());
        logistics.setRemark(dto.getRemark());
        logistics.setCreateTime(LocalDateTime.now());
        logistics.setUpdateTime(LocalDateTime.now());

        logisticsMapper.insert(logistics);

        // 添加初始物流轨迹
        addLogisticsTrack(logistics.getId(), dto.getTrackingNo(), 
                "包裹已发出，等待快递员取件", "已发货");

        // 更新订单状态
        order.setStatus(OrderConstant.STATUS_SHIPPED);
        order.setShipTime(LocalDateTime.now());
        orderMapper.updateById(order);

        log.info("订单{}发货成功，物流单号:{}", dto.getOrderId(), dto.getTrackingNo());
        return logistics;
    }

    /**
     * 获取订单物流信息
     */
    public LogisticsVO getOrderLogistics(Long orderId) {
        Logistics logistics = logisticsMapper.selectOne(
                new LambdaQueryWrapper<Logistics>()
                        .eq(Logistics::getOrderId, orderId)
                        .orderByDesc(Logistics::getCreateTime)
                        .last("LIMIT 1")
        );

        if (logistics == null) {
            return null;
        }

        return convertToVO(logistics);
    }

    /**
     * 根据运单号查询物流信息
     */
    public LogisticsVO getLogisticsByTrackingNo(String trackingNo) {
        Logistics logistics = logisticsMapper.selectOne(
                new LambdaQueryWrapper<Logistics>()
                        .eq(Logistics::getTrackingNo, trackingNo)
                        .last("LIMIT 1")
        );

        if (logistics == null) {
            return null;
        }

        return convertToVO(logistics);
    }

    /**
     * 同步物流状态（模拟从第三方物流API获取）
     */
    @Transactional
    public void syncLogisticsStatus(Long logisticsId) {
        Logistics logistics = logisticsMapper.selectById(logisticsId);
        if (logistics == null) {
            return;
        }

        // 模拟从物流API获取最新状态
        // 实际项目中需要调用第三方物流API（如快递100）
        String latestStatus = simulateQueryLogistics(logistics.getTrackingNo());
        
        if (latestStatus != null) {
            logistics.setStatus(parseLogisticsStatus(latestStatus));
            logistics.setUpdateTime(LocalDateTime.now());
            logisticsMapper.updateById(logistics);

            // 添加物流轨迹
            addLogisticsTrack(logisticsId, logistics.getTrackingNo(), 
                    latestStatus, getStatusFromText(latestStatus));
        }
    }

    /**
     * 确认收货
     */
    @Transactional
    public void confirmReceive(Long orderId, Long userId) {
        Logistics logistics = logisticsMapper.selectOne(
                new LambdaQueryWrapper<Logistics>()
                        .eq(Logistics::getOrderId, orderId)
                        .last("LIMIT 1")
        );

        if (logistics != null) {
            logistics.setStatus(4); // 已签收
            logistics.setReceiveTime(LocalDateTime.now());
            logistics.setUpdateTime(LocalDateTime.now());
            logisticsMapper.updateById(logistics);

            // 添加签收轨迹
            addLogisticsTrack(logistics.getId(), logistics.getTrackingNo(),
                    "买家已确认收货", "已签收");
        }
    }

    /**
     * 获取物流公司列表
     */
    public List<LogisticsCompany> getLogisticsCompanies() {
        List<LogisticsCompany> companies = new ArrayList<>();
        companies.add(new LogisticsCompany("SF", "顺丰速运"));
        companies.add(new LogisticsCompany("YTO", "圆通速递"));
        companies.add(new LogisticsCompany("ZTO", "中通快递"));
        companies.add(new LogisticsCompany("STO", "申通快递"));
        companies.add(new LogisticsCompany("YD", "韵达快递"));
        companies.add(new LogisticsCompany("JTSD", "极兔速递"));
        companies.add(new LogisticsCompany("EMS", "EMS"));
        companies.add(new LogisticsCompany("YZPY", "邮政快递包裹"));
        return companies;
    }

    /**
     * 添加物流轨迹
     */
    private void addLogisticsTrack(Long logisticsId, String trackingNo, 
            String description, String status) {
        LogisticsTrack track = new LogisticsTrack();
        track.setLogisticsId(logisticsId);
        track.setTrackingNo(trackingNo);
        track.setTrackTime(LocalDateTime.now());
        track.setStatus(status);
        track.setDescription(description);
        track.setCreateTime(LocalDateTime.now());
        logisticsTrackMapper.insert(track);
    }

    /**
     * 转换VO
     */
    private LogisticsVO convertToVO(Logistics logistics) {
        LogisticsVO vo = new LogisticsVO();
        vo.setId(logistics.getId());
        vo.setOrderId(logistics.getOrderId());
        vo.setCompanyCode(logistics.getCompanyCode());
        vo.setCompanyName(logistics.getCompanyName());
        vo.setTrackingNo(logistics.getTrackingNo());
        vo.setShipTime(logistics.getShipTime() != null ? 
                logistics.getShipTime().format(DATE_TIME_FORMATTER) : null);
        vo.setReceiveTime(logistics.getReceiveTime() != null ? 
                logistics.getReceiveTime().format(DATE_TIME_FORMATTER) : null);
        vo.setStatus(logistics.getStatus());
        vo.setStatusText(getStatusText(logistics.getStatus()));
        vo.setReceiverName(logistics.getReceiverName());
        vo.setReceiverPhone(logistics.getReceiverPhone());
        vo.setReceiverAddress(logistics.getReceiverAddress());
        vo.setRemark(logistics.getRemark());

        // 获取物流轨迹
        List<LogisticsTrack> tracks = logisticsTrackMapper.selectList(
                new LambdaQueryWrapper<LogisticsTrack>()
                        .eq(LogisticsTrack::getLogisticsId, logistics.getId())
                        .orderByDesc(LogisticsTrack::getTrackTime)
        );

        vo.setTracks(tracks.stream().map(track -> {
            LogisticsTrackVO trackVO = new LogisticsTrackVO();
            trackVO.setTrackTime(track.getTrackTime() != null ? 
                    track.getTrackTime().format(DATE_TIME_FORMATTER) : null);
            trackVO.setStatus(track.getStatus());
            trackVO.setDescription(track.getDescription());
            trackVO.setLocation(track.getLocation());
            return trackVO;
        }).collect(Collectors.toList()));

        return vo;
    }

    /**
     * 获取状态文本
     */
    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "暂无物流信息";
            case 1 -> "已发货";
            case 2 -> "运输中";
            case 3 -> "派送中";
            case 4 -> "已签收";
            case 5 -> "拒收";
            case 6 -> "退件";
            default -> "未知";
        };
    }

    /**
     * 解析物流状态
     */
    private Integer parseLogisticsStatus(String statusText) {
        if (statusText.contains("签收") || statusText.contains("已取件")) {
            return 4;
        } else if (statusText.contains("派送")) {
            return 3;
        } else if (statusText.contains("到达")) {
            return 2;
        }
        return 2;
    }

    /**
     * 从文本获取状态
     */
    private String getStatusFromText(String text) {
        if (text.contains("签收")) return "已签收";
        if (text.contains("派送")) return "派送中";
        if (text.contains("到达")) return "运输中";
        return "运输中";
    }

    /**
     * 模拟查询物流（实际项目中应调用第三方API）
     */
    private String simulateQueryLogistics(String trackingNo) {
        // 模拟返回物流状态
        log.info("查询物流单号: {}", trackingNo);
        return "包裹正在运输中";
    }

    /**
     * 物流公司内部类
     */
    @lombok.Data
    public static class LogisticsCompany {
        private String code;
        private String name;

        public LogisticsCompany(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }
}
