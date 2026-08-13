package com.nilambar.erp.controller;

import com.nilambar.erp.config.ErpProperties;
import com.nilambar.erp.dto.dashboard.DashboardSnapshot;
import com.nilambar.erp.dto.dashboard.TrendPoint;
import com.nilambar.erp.service.dashboard.DashboardService;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class DashboardController {

    private static final List<Integer> WINDOW_OPTIONS = List.of(7, 30, 90, 180);

    private final DashboardService dashboardService;
    private final ErpProperties properties;

    public DashboardController(DashboardService dashboardService, ErpProperties properties) {
        this.dashboardService = dashboardService;
        this.properties = properties;
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(value = "days", required = false) Integer days, Model model) {
        DashboardSnapshot snapshot = dashboardService.snapshot(windowDays(days));
        model.addAttribute("snapshot", snapshot);
        model.addAttribute("maxRevenue", maxValue(snapshot));
        model.addAttribute("windowOptions", WINDOW_OPTIONS);
        model.addAttribute("dashGeneratedAt", Date.from(snapshot.generatedAt()));
        return "dashboard/index";
    }

    @GetMapping("/dashboard/api")
    @ResponseBody
    public DashboardSnapshot dashboardJson(@RequestParam(value = "days", required = false) Integer days) {
        return dashboardService.snapshot(windowDays(days));
    }

    private int windowDays(Integer requested) {
        return requested == null ? properties.getDashboard().getDefaultWindowDays() : requested;
    }

    private double maxValue(DashboardSnapshot snapshot) {
        return Math.max(1d, Stream.concat(snapshot.revenueTrend().stream(), snapshot.revenueForecast().stream())
                .mapToDouble(TrendPoint::value)
                .max()
                .orElse(1d));
    }
}
