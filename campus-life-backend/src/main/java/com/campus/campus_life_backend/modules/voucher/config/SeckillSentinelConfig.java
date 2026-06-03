package com.campus.campus_life_backend.modules.voucher.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SeckillSentinelConfig {

    public static final String SECKILL_RESOURCE = "couponSeckill";

    @Value("${seckill.sentinel.qps:80}")
    private double seckillQps;

    @PostConstruct
    public void initRules() {
        List<FlowRule> rules = new ArrayList<>();
        FlowRule rule = new FlowRule();
        rule.setResource(SECKILL_RESOURCE);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(seckillQps);
        rules.add(rule);
        FlowRuleManager.loadRules(rules);
    }
}
