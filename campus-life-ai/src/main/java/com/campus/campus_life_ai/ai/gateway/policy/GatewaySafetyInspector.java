package com.campus.campus_life_ai.ai.gateway.policy;

import com.campus.campus_life_ai.ai.entity.AiGatewaySafetyRule;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayRequest;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayResponse;
import com.campus.campus_life_ai.ai.mapper.AiGatewaySafetyRuleMapper;
import com.campus.campus_life_ai.ai.provider.ChatCompletionCommand;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component
public class GatewaySafetyInspector {

    private static final String INPUT = "INPUT";
    private static final String OUTPUT = "OUTPUT";
    private static final String BOTH = "BOTH";
    private static final String BLOCK = "BLOCK";
    private static final String REGEX = "REGEX";

    private final AiGatewaySafetyRuleMapper aiGatewaySafetyRuleMapper;

    public GatewaySafetyInspector(AiGatewaySafetyRuleMapper aiGatewaySafetyRuleMapper) {
        this.aiGatewaySafetyRuleMapper = aiGatewaySafetyRuleMapper;
    }

    public SafetyDecision inspectInput(GatewayRequest request) {
        return inspect(request, INPUT, collectInputText(request));
    }

    public SafetyDecision inspectOutput(GatewayRequest request, GatewayResponse response) {
        return inspect(request, OUTPUT, response == null ? null : response.getContent());
    }

    private SafetyDecision inspect(GatewayRequest request, String direction, String text) {
        if (!StringUtils.hasText(text)) {
            return SafetyDecision.allow();
        }

        List<String> categories = new ArrayList<>();
        List<String> matchedRules = new ArrayList<>();
        for (AiGatewaySafetyRule rule : aiGatewaySafetyRuleMapper.findEnabled()) {
            if (!scopeMatches(rule, request) || !directionMatches(rule, direction) || !matches(rule, text)) {
                continue;
            }
            categories.add(rule.getCategory());
            matchedRules.add(rule.getRuleName());
            if (BLOCK.equals(normalize(rule.getAction()))) {
                return SafetyDecision.block(
                        "AI 安全策略拦截: " + rule.getRuleName(),
                        categories,
                        matchedRules
                );
            }
        }
        return categories.isEmpty()
                ? SafetyDecision.allow()
                : SafetyDecision.audit(categories, matchedRules);
    }

    private String collectInputText(GatewayRequest request) {
        ChatCompletionCommand command = request == null ? null : request.getCommand();
        if (command == null) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        append(text, command.getSystemPrompt());
        if (command.getMessages() != null) {
            for (ChatCompletionCommand.PromptMessage message : command.getMessages()) {
                append(text, message == null ? null : message.getContent());
            }
        }
        return text.toString();
    }

    private void append(StringBuilder text, String value) {
        if (StringUtils.hasText(value)) {
            if (!text.isEmpty()) {
                text.append('\n');
            }
            text.append(value);
        }
    }

    private boolean scopeMatches(AiGatewaySafetyRule rule, GatewayRequest request) {
        if (rule == null || request == null) {
            return false;
        }
        if (StringUtils.hasText(rule.getCapabilityCode())
                && !rule.getCapabilityCode().trim().equals(request.getCapabilityCode())) {
            return false;
        }
        return !StringUtils.hasText(rule.getSceneCode())
                || rule.getSceneCode().trim().equals(request.getSceneCode());
    }

    private boolean directionMatches(AiGatewaySafetyRule rule, String direction) {
        String configured = normalize(rule.getDirection());
        return BOTH.equals(configured) || direction.equals(configured);
    }

    private boolean matches(AiGatewaySafetyRule rule, String text) {
        if (!StringUtils.hasText(rule.getPatternText())) {
            return false;
        }
        if (REGEX.equals(normalize(rule.getMatchType()))) {
            try {
                return Pattern.compile(rule.getPatternText(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                        .matcher(text)
                        .find();
            } catch (PatternSyntaxException e) {
                return false;
            }
        }
        return text.toLowerCase(Locale.ROOT).contains(rule.getPatternText().toLowerCase(Locale.ROOT));
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
    }

    public record SafetyDecision(boolean blocked,
                                 boolean audited,
                                 String message,
                                 List<String> categories,
                                 List<String> matchedRules) {
        public static SafetyDecision allow() {
            return new SafetyDecision(false, false, null, List.of(), List.of());
        }

        public static SafetyDecision audit(List<String> categories, List<String> matchedRules) {
            return new SafetyDecision(false, true, null, List.copyOf(categories), List.copyOf(matchedRules));
        }

        public static SafetyDecision block(String message, List<String> categories, List<String> matchedRules) {
            return new SafetyDecision(true, true, message, List.copyOf(categories), List.copyOf(matchedRules));
        }
    }
}
