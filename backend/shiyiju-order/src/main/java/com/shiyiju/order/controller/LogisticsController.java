package com.shiyiju.order.controller;

import com.shiyiju.common.result.Result;
import com.shiyiju.order.dto.ShipOrderDTO;
import com.shiyiju.order.service.LogisticsService;
import com.shiyiju.order.vo.LogisticsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 物流控制器
 */
@Slf4j
@RestController
@RequestMapping("/logistics")
@RequiredArgsConstructor
public class LogisticsController {

    private final LogisticsService logisticsService;

    /**
     * 订单发货 (POST /logistics/ship)
     */
    @PostMapping("/ship")
    public Result<LogisticsVO> shipOrder(@RequestBody ShipOrderDTO dto) {
        logisticsService.shipOrder(dto);
        return Result.success(logisticsService.getOrderLogistics(dto.getOrderId()));
    }

    /**
     * 获取订单物流信息 (GET /logistics/order/{orderId})
     */
    @GetMapping("/order/{orderId}")
    public Result<LogisticsVO> getOrderLogistics(@PathVariable Long orderId) {
        LogisticsVO logistics = logisticsService.getOrderLogistics(orderId);
        if (logistics == null) {
            return Result.fail(404, "暂无物流信息");
        }
        return Result.success(logistics);
    }

    /**
     * 根据运单号查询物流 (GET /logistics/tracking/{trackingNo})
     */
    @GetMapping("/tracking/{trackingNo}")
    public Result<LogisticsVO> getLogisticsByTrackingNo(@PathVariable String trackingNo) {
        LogisticsVO logistics = logisticsService.getLogisticsByTrackingNo(trackingNo);
        if (logistics == null) {
            return Result.fail(404, "未找到物流信息");
        }
        return Result.success(logistics);
    }

    /**
     * 同步物流状态 (POST /logistics/{id}/sync)
     */
    @PostMapping("/{id}/sync")
    public Result<Void> syncLogisticsStatus(@PathVariable Long id) {
        logisticsService.syncLogisticsStatus(id);
        return Result.success();
    }

    /**
     * 获取物流公司列表 (GET /logistics/companies)
     */
    @GetMapping("/companies")
    public Result<List<LogisticsService.LogisticsCompany>> getLogisticsCompanies() {
        return Result.success(logisticsService.getLogisticsCompanies());
    }
}
