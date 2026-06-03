package com.campus.campus_life_ai.ai.service;

import com.campus.campus_life_ai.ai.dto.AiCapabilityConfigDTO;
import com.campus.campus_life_ai.ai.dto.AiCapabilityUpsertRequest;
import com.campus.campus_life_ai.ai.dto.AiGatewaySafetyRuleDTO;
import com.campus.campus_life_ai.ai.dto.AiGatewaySafetyRuleUpsertRequest;
import com.campus.campus_life_ai.ai.dto.AiGatewayRouteRuleDTO;
import com.campus.campus_life_ai.ai.dto.AiGatewayRouteRuleUpsertRequest;
import com.campus.campus_life_ai.ai.dto.AiModelConfigDTO;
import com.campus.campus_life_ai.ai.dto.AiModelUpsertRequest;
import com.campus.campus_life_ai.ai.dto.AiProviderConfigDTO;
import com.campus.campus_life_ai.ai.dto.AiProviderUpsertRequest;
import com.campus.campus_life_ai.ai.dto.AiSceneConfigDTO;
import com.campus.campus_life_ai.ai.dto.AiSceneUpsertRequest;
import com.campus.campus_life_ai.ai.dto.AiUsageQuotaDTO;
import com.campus.campus_life_ai.ai.dto.AiUsageQuotaUpsertRequest;
import com.campus.campus_life_ai.ai.entity.AiCapabilityConfig;
import com.campus.campus_life_ai.ai.entity.AiGatewaySafetyRule;
import com.campus.campus_life_ai.ai.entity.AiGatewayRouteRule;
import com.campus.campus_life_ai.ai.entity.AiModelConfig;
import com.campus.campus_life_ai.ai.entity.AiProviderConfig;
import com.campus.campus_life_ai.ai.entity.AiSceneConfig;
import com.campus.campus_life_ai.ai.entity.AiUsageQuota;
import com.campus.campus_life_ai.ai.mapper.AiCapabilityConfigMapper;
import com.campus.campus_life_ai.ai.mapper.AiGatewaySafetyRuleMapper;
import com.campus.campus_life_ai.ai.mapper.AiGatewayRouteRuleMapper;
import com.campus.campus_life_ai.ai.mapper.AiModelConfigMapper;
import com.campus.campus_life_ai.ai.mapper.AiProviderConfigMapper;
import com.campus.campus_life_ai.ai.mapper.AiSceneConfigMapper;
import com.campus.campus_life_ai.ai.mapper.AiUsageQuotaMapper;
import com.campus.campus_life_ai.common.security.ApiKeyCipherService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AdminAiConfigService {

    private final AiProviderConfigMapper aiProviderConfigMapper;
    private final AiModelConfigMapper aiModelConfigMapper;
    private final AiGatewayRouteRuleMapper aiGatewayRouteRuleMapper;
    private final AiGatewaySafetyRuleMapper aiGatewaySafetyRuleMapper;
    private final AiCapabilityConfigMapper aiCapabilityConfigMapper;
    private final AiSceneConfigMapper aiSceneConfigMapper;
    private final AiUsageQuotaMapper aiUsageQuotaMapper;
    private final ApiKeyCipherService apiKeyCipherService;
    private final ObjectMapper objectMapper;

    public AdminAiConfigService(AiProviderConfigMapper aiProviderConfigMapper,
                                AiModelConfigMapper aiModelConfigMapper,
                                AiGatewayRouteRuleMapper aiGatewayRouteRuleMapper,
                                AiGatewaySafetyRuleMapper aiGatewaySafetyRuleMapper,
                                AiCapabilityConfigMapper aiCapabilityConfigMapper,
                                AiSceneConfigMapper aiSceneConfigMapper,
                                AiUsageQuotaMapper aiUsageQuotaMapper,
                                ApiKeyCipherService apiKeyCipherService,
                                ObjectMapper objectMapper) {
        this.aiProviderConfigMapper = aiProviderConfigMapper;
        this.aiModelConfigMapper = aiModelConfigMapper;
        this.aiGatewayRouteRuleMapper = aiGatewayRouteRuleMapper;
        this.aiGatewaySafetyRuleMapper = aiGatewaySafetyRuleMapper;
        this.aiCapabilityConfigMapper = aiCapabilityConfigMapper;
        this.aiSceneConfigMapper = aiSceneConfigMapper;
        this.aiUsageQuotaMapper = aiUsageQuotaMapper;
        this.apiKeyCipherService = apiKeyCipherService;
        this.objectMapper = objectMapper;
    }

    public List<AiProviderConfigDTO> listProviders() {
        return aiProviderConfigMapper.findAll().stream()
                .map(this::toProviderDto)
                .toList();
    }

    public AiProviderConfigDTO getProvider(Long id) {
        return toProviderDto(requireProvider(id));
    }

    @Transactional
    public AiProviderConfigDTO createProvider(AiProviderUpsertRequest request) {
        ensureProviderCodeUnique(normalizeRequired(request.getProviderCode()), null);
        AiProviderConfig config = new AiProviderConfig();
        applyProviderRequest(config, request, false);
        LocalDateTime now = LocalDateTime.now();
        config.setCreatedAt(now);
        config.setUpdatedAt(now);
        aiProviderConfigMapper.insert(config);
        return toProviderDto(config);
    }

    @Transactional
    public AiProviderConfigDTO updateProvider(Long id, AiProviderUpsertRequest request) {
        AiProviderConfig config = requireProvider(id);
        ensureProviderCodeUnique(normalizeRequired(request.getProviderCode()), id);
        applyProviderRequest(config, request, true);
        config.setUpdatedAt(LocalDateTime.now());
        aiProviderConfigMapper.update(config);
        return toProviderDto(config);
    }

    @Transactional
    public void deleteProvider(Long id) {
        AiProviderConfig config = requireProvider(id);
        if (aiSceneConfigMapper.countByProviderCode(config.getProviderCode()) > 0) {
            throw new IllegalArgumentException("该供应商下仍有关联场景，不能删除");
        }
        if (aiModelConfigMapper.countByProviderCode(config.getProviderCode()) > 0) {
            throw new IllegalArgumentException("该供应商下仍有关联模型，不能删除");
        }
        aiProviderConfigMapper.deleteById(id);
    }

    public List<AiModelConfigDTO> listModels() {
        return aiModelConfigMapper.findAll().stream()
                .map(this::toModelDto)
                .toList();
    }

    public AiModelConfigDTO getModel(Long id) {
        return toModelDto(requireModel(id));
    }

    @Transactional
    public AiModelConfigDTO createModel(AiModelUpsertRequest request) {
        validateModelReferences(request);
        ensureModelUnique(normalizeRequired(request.getProviderCode()), normalizeRequired(request.getModelCode()), null);
        AiModelConfig config = new AiModelConfig();
        applyModelRequest(config, request);
        LocalDateTime now = LocalDateTime.now();
        config.setCreatedAt(now);
        config.setUpdatedAt(now);
        aiModelConfigMapper.insert(config);
        return toModelDto(config);
    }

    @Transactional
    public AiModelConfigDTO updateModel(Long id, AiModelUpsertRequest request) {
        AiModelConfig config = requireModel(id);
        validateModelReferences(request);
        ensureModelUnique(normalizeRequired(request.getProviderCode()), normalizeRequired(request.getModelCode()), id);
        applyModelRequest(config, request);
        config.setUpdatedAt(LocalDateTime.now());
        aiModelConfigMapper.update(config);
        return toModelDto(config);
    }

    @Transactional
    public void deleteModel(Long id) {
        requireModel(id);
        aiModelConfigMapper.deleteById(id);
    }

    public List<AiGatewayRouteRuleDTO> listRouteRules() {
        return aiGatewayRouteRuleMapper.findAll().stream()
                .map(this::toRouteRuleDto)
                .toList();
    }

    public AiGatewayRouteRuleDTO getRouteRule(Long id) {
        return toRouteRuleDto(requireRouteRule(id));
    }

    @Transactional
    public AiGatewayRouteRuleDTO createRouteRule(AiGatewayRouteRuleUpsertRequest request) {
        validateRouteRuleReferences(request);
        AiGatewayRouteRule rule = new AiGatewayRouteRule();
        applyRouteRuleRequest(rule, request);
        LocalDateTime now = LocalDateTime.now();
        rule.setCreatedAt(now);
        rule.setUpdatedAt(now);
        aiGatewayRouteRuleMapper.insert(rule);
        return toRouteRuleDto(rule);
    }

    @Transactional
    public AiGatewayRouteRuleDTO updateRouteRule(Long id, AiGatewayRouteRuleUpsertRequest request) {
        AiGatewayRouteRule rule = requireRouteRule(id);
        validateRouteRuleReferences(request);
        applyRouteRuleRequest(rule, request);
        rule.setUpdatedAt(LocalDateTime.now());
        aiGatewayRouteRuleMapper.update(rule);
        return toRouteRuleDto(rule);
    }

    @Transactional
    public void deleteRouteRule(Long id) {
        requireRouteRule(id);
        aiGatewayRouteRuleMapper.deleteById(id);
    }

    public List<AiGatewaySafetyRuleDTO> listSafetyRules() {
        return aiGatewaySafetyRuleMapper.findAll().stream()
                .map(this::toSafetyRuleDto)
                .toList();
    }

    public AiGatewaySafetyRuleDTO getSafetyRule(Long id) {
        return toSafetyRuleDto(requireSafetyRule(id));
    }

    @Transactional
    public AiGatewaySafetyRuleDTO createSafetyRule(AiGatewaySafetyRuleUpsertRequest request) {
        validateSafetyRuleReferences(request);
        AiGatewaySafetyRule rule = new AiGatewaySafetyRule();
        applySafetyRuleRequest(rule, request);
        LocalDateTime now = LocalDateTime.now();
        rule.setCreatedAt(now);
        rule.setUpdatedAt(now);
        aiGatewaySafetyRuleMapper.insert(rule);
        return toSafetyRuleDto(rule);
    }

    @Transactional
    public AiGatewaySafetyRuleDTO updateSafetyRule(Long id, AiGatewaySafetyRuleUpsertRequest request) {
        AiGatewaySafetyRule rule = requireSafetyRule(id);
        validateSafetyRuleReferences(request);
        applySafetyRuleRequest(rule, request);
        rule.setUpdatedAt(LocalDateTime.now());
        aiGatewaySafetyRuleMapper.update(rule);
        return toSafetyRuleDto(rule);
    }

    @Transactional
    public void deleteSafetyRule(Long id) {
        requireSafetyRule(id);
        aiGatewaySafetyRuleMapper.deleteById(id);
    }

    public List<AiCapabilityConfigDTO> listCapabilities() {
        return aiCapabilityConfigMapper.findAll().stream()
                .map(this::toCapabilityDto)
                .toList();
    }

    public AiCapabilityConfigDTO getCapability(Long id) {
        return toCapabilityDto(requireCapability(id));
    }

    @Transactional
    public AiCapabilityConfigDTO createCapability(AiCapabilityUpsertRequest request) {
        ensureCapabilityCodeUnique(normalizeRequired(request.getCapabilityCode()), null);
        AiCapabilityConfig config = new AiCapabilityConfig();
        applyCapabilityRequest(config, request);
        LocalDateTime now = LocalDateTime.now();
        config.setCreatedAt(now);
        config.setUpdatedAt(now);
        aiCapabilityConfigMapper.insert(config);
        return toCapabilityDto(config);
    }

    @Transactional
    public AiCapabilityConfigDTO updateCapability(Long id, AiCapabilityUpsertRequest request) {
        AiCapabilityConfig config = requireCapability(id);
        ensureCapabilityCodeUnique(normalizeRequired(request.getCapabilityCode()), id);
        applyCapabilityRequest(config, request);
        config.setUpdatedAt(LocalDateTime.now());
        aiCapabilityConfigMapper.update(config);
        return toCapabilityDto(config);
    }

    @Transactional
    public void deleteCapability(Long id) {
        AiCapabilityConfig config = requireCapability(id);
        if (aiSceneConfigMapper.countByCapabilityCode(config.getCapabilityCode()) > 0) {
            throw new IllegalArgumentException("该能力下仍有关联场景，不能删除");
        }
        if (aiGatewayRouteRuleMapper.countByCapabilityCode(config.getCapabilityCode()) > 0) {
            throw new IllegalArgumentException("该能力下仍有关联路由规则，不能删除");
        }
        if (aiUsageQuotaMapper.countByCapabilityCode(config.getCapabilityCode()) > 0) {
            throw new IllegalArgumentException("该能力下仍有关联配额，不能删除");
        }
        if (aiGatewaySafetyRuleMapper.countByCapabilityCode(config.getCapabilityCode()) > 0) {
            throw new IllegalArgumentException("该能力下仍有关联安全规则，不能删除");
        }
        aiCapabilityConfigMapper.deleteById(id);
    }

    public List<AiUsageQuotaDTO> listQuotas() {
        return aiUsageQuotaMapper.findAll().stream()
                .map(this::toQuotaDto)
                .toList();
    }

    public AiUsageQuotaDTO getQuota(Long id) {
        return toQuotaDto(requireQuota(id));
    }

    @Transactional
    public AiUsageQuotaDTO createQuota(AiUsageQuotaUpsertRequest request) {
        validateQuotaReferences(request);
        AiUsageQuota quota = new AiUsageQuota();
        applyQuotaRequest(quota, request);
        LocalDateTime now = LocalDateTime.now();
        quota.setCreatedAt(now);
        quota.setUpdatedAt(now);
        aiUsageQuotaMapper.insert(quota);
        return toQuotaDto(quota);
    }

    @Transactional
    public AiUsageQuotaDTO updateQuota(Long id, AiUsageQuotaUpsertRequest request) {
        AiUsageQuota quota = requireQuota(id);
        validateQuotaReferences(request);
        applyQuotaRequest(quota, request);
        quota.setUpdatedAt(LocalDateTime.now());
        aiUsageQuotaMapper.update(quota);
        return toQuotaDto(quota);
    }

    @Transactional
    public void deleteQuota(Long id) {
        requireQuota(id);
        aiUsageQuotaMapper.deleteById(id);
    }

    public List<AiSceneConfigDTO> listScenes() {
        return aiSceneConfigMapper.findAll().stream()
                .map(this::toSceneDto)
                .toList();
    }

    public AiSceneConfigDTO getScene(Long id) {
        return toSceneDto(requireScene(id));
    }

    @Transactional
    public AiSceneConfigDTO createScene(AiSceneUpsertRequest request) {
        ensureSceneCodeUnique(normalizeRequired(request.getSceneCode()), null);
        validateSceneReferences(request);
        AiSceneConfig config = new AiSceneConfig();
        applySceneRequest(config, request);
        LocalDateTime now = LocalDateTime.now();
        config.setCreatedAt(now);
        config.setUpdatedAt(now);
        aiSceneConfigMapper.insert(config);
        return toSceneDto(config);
    }

    @Transactional
    public AiSceneConfigDTO updateScene(Long id, AiSceneUpsertRequest request) {
        AiSceneConfig config = requireScene(id);
        ensureSceneCodeUnique(normalizeRequired(request.getSceneCode()), id);
        validateSceneReferences(request);
        applySceneRequest(config, request);
        config.setUpdatedAt(LocalDateTime.now());
        aiSceneConfigMapper.update(config);
        return toSceneDto(config);
    }

    @Transactional
    public void deleteScene(Long id) {
        AiSceneConfig config = requireScene(id);
        if (aiGatewayRouteRuleMapper.countBySceneCode(config.getSceneCode()) > 0) {
            throw new IllegalArgumentException("该场景下仍有关联路由规则，不能删除");
        }
        if (aiUsageQuotaMapper.countBySceneCode(config.getSceneCode()) > 0) {
            throw new IllegalArgumentException("该场景下仍有关联配额，不能删除");
        }
        if (aiGatewaySafetyRuleMapper.countBySceneCode(config.getSceneCode()) > 0) {
            throw new IllegalArgumentException("该场景下仍有关联安全规则，不能删除");
        }
        aiSceneConfigMapper.deleteById(id);
    }

    private void applyProviderRequest(AiProviderConfig config, AiProviderUpsertRequest request, boolean preserveSecretWhenMissing) {
        config.setProviderCode(normalizeRequired(request.getProviderCode()));
        config.setProviderName(normalizeRequired(request.getProviderName()));
        config.setBaseUrl(normalizeRequired(request.getBaseUrl()));
        if (request.getApiKeyCipher() != null || !preserveSecretWhenMissing) {
            config.setApiKeyCipher(encryptApiKey(normalizeOptional(request.getApiKeyCipher())));
        }
        config.setDefaultModelCode(normalizeRequired(request.getDefaultModelCode()));
        config.setEnabled(toFlag(request.getEnabled()));
        config.setTimeoutMs(defaultIfNull(request.getTimeoutMs(), 30000));
        config.setMaxContextMessages(defaultIfNull(request.getMaxContextMessages(), 20));
        config.setTemperature(defaultIfNull(request.getTemperature(), 0.7D));
        config.setTopP(request.getTopP());
        config.setMaxOutputTokens(defaultIfNull(request.getMaxOutputTokens(), 1024));
        config.setSystemPromptTemplate(normalizeOptional(request.getSystemPromptTemplate()));
    }

    private void applyCapabilityRequest(AiCapabilityConfig config, AiCapabilityUpsertRequest request) {
        config.setCapabilityCode(normalizeRequired(request.getCapabilityCode()));
        config.setCapabilityName(normalizeRequired(request.getCapabilityName()));
        config.setEnabled(toFlag(request.getEnabled()));
        config.setGrayEnabled(toFlag(request.getGrayEnabled()));
        config.setGrayRuleJson(normalizeJson(request.getGrayRuleJson(), "灰度规则"));
        config.setRateLimitJson(normalizeJson(request.getRateLimitJson(), "限流规则"));
        config.setQuotaRuleJson(normalizeJson(request.getQuotaRuleJson(), "配额规则"));
    }

    private void applyModelRequest(AiModelConfig config, AiModelUpsertRequest request) {
        config.setProviderCode(normalizeRequired(request.getProviderCode()));
        config.setModelCode(normalizeRequired(request.getModelCode()));
        config.setModelName(normalizeRequired(request.getModelName()));
        config.setCapabilitiesJson(normalizeJson(request.getCapabilitiesJson(), "模型能力"));
        config.setContextWindow(request.getContextWindow());
        config.setMaxOutputTokens(request.getMaxOutputTokens());
        config.setInputPricePer1k(request.getInputPricePer1k());
        config.setOutputPricePer1k(request.getOutputPricePer1k());
        config.setEnabled(toFlag(request.getEnabled()));
        config.setPriority(defaultIfNull(request.getPriority(), 100));
    }

    private void applyRouteRuleRequest(AiGatewayRouteRule rule, AiGatewayRouteRuleUpsertRequest request) {
        rule.setRuleName(normalizeRequired(request.getRuleName()));
        rule.setCapabilityCode(normalizeRequired(request.getCapabilityCode()));
        rule.setSceneCode(normalizeOptional(request.getSceneCode()));
        rule.setMatchRuleJson(normalizeJson(request.getMatchRuleJson(), "匹配规则"));
        rule.setRouteRuleJson(requireRouteRuleJson(request.getRouteRuleJson()));
        rule.setEnabled(toFlag(request.getEnabled()));
        rule.setPriority(defaultIfNull(request.getPriority(), 100));
    }

    private void applySafetyRuleRequest(AiGatewaySafetyRule rule, AiGatewaySafetyRuleUpsertRequest request) {
        rule.setRuleName(normalizeRequired(request.getRuleName()));
        rule.setCapabilityCode(normalizeOptional(request.getCapabilityCode()));
        rule.setSceneCode(normalizeOptional(request.getSceneCode()));
        rule.setDirection(normalizeUpper(request.getDirection()));
        rule.setAction(normalizeUpper(request.getAction()));
        rule.setMatchType(normalizeUpper(request.getMatchType()));
        rule.setPatternText(normalizeRequired(request.getPatternText()));
        rule.setCategory(normalizeRequired(request.getCategory()));
        rule.setSeverity(normalizeUpper(request.getSeverity()));
        rule.setEnabled(toFlag(request.getEnabled()));
        rule.setPriority(defaultIfNull(request.getPriority(), 100));
    }

    private void applySceneRequest(AiSceneConfig config, AiSceneUpsertRequest request) {
        config.setCapabilityCode(normalizeRequired(request.getCapabilityCode()));
        config.setSceneCode(normalizeRequired(request.getSceneCode()));
        config.setSceneName(normalizeRequired(request.getSceneName()));
        config.setProviderCode(normalizeOptional(request.getProviderCode()));
        config.setModelCode(normalizeOptional(request.getModelCode()));
        config.setEnabled(toFlag(request.getEnabled()));
        config.setSystemPromptTemplate(normalizeOptional(request.getSystemPromptTemplate()));
        config.setInputSchemaJson(normalizeOptional(request.getInputSchemaJson()));
        config.setOutputSchemaJson(normalizeOptional(request.getOutputSchemaJson()));
        config.setSafetyLevel(normalizeOptional(request.getSafetyLevel()));
        config.setTimeoutMs(request.getTimeoutMs());
    }

    private void applyQuotaRequest(AiUsageQuota quota, AiUsageQuotaUpsertRequest request) {
        String subjectType = normalizeUpper(request.getSubjectType());
        quota.setSubjectType(subjectType);
        quota.setSubjectId("GLOBAL".equals(subjectType) ? null : normalizeOptional(request.getSubjectId()));
        quota.setCapabilityCode(normalizeOptional(request.getCapabilityCode()));
        quota.setSceneCode(normalizeOptional(request.getSceneCode()));
        quota.setQuotaPeriod(normalizeUpper(request.getQuotaPeriod()));
        quota.setMaxCalls(request.getMaxCalls());
        quota.setMaxTokens(request.getMaxTokens());
        quota.setMaxCost(request.getMaxCost());
        quota.setEnabled(toFlag(request.getEnabled()));
    }

    private void validateSceneReferences(AiSceneUpsertRequest request) {
        String capabilityCode = normalizeRequired(request.getCapabilityCode());
        if (aiCapabilityConfigMapper.findByCapabilityCode(capabilityCode) == null) {
            throw new IllegalArgumentException("关联能力不存在");
        }
        String providerCode = normalizeOptional(request.getProviderCode());
        if (StringUtils.hasText(providerCode) && aiProviderConfigMapper.findByProviderCode(providerCode) == null) {
            throw new IllegalArgumentException("关联供应商不存在");
        }
    }

    private void validateModelReferences(AiModelUpsertRequest request) {
        String providerCode = normalizeRequired(request.getProviderCode());
        if (aiProviderConfigMapper.findByProviderCode(providerCode) == null) {
            throw new IllegalArgumentException("关联供应商不存在");
        }
        normalizeJson(request.getCapabilitiesJson(), "模型能力");
    }

    private void validateRouteRuleReferences(AiGatewayRouteRuleUpsertRequest request) {
        String capabilityCode = normalizeRequired(request.getCapabilityCode());
        if (aiCapabilityConfigMapper.findByCapabilityCode(capabilityCode) == null) {
            throw new IllegalArgumentException("关联能力不存在");
        }

        String sceneCode = normalizeOptional(request.getSceneCode());
        if (StringUtils.hasText(sceneCode)) {
            AiSceneConfig sceneConfig = aiSceneConfigMapper.findBySceneCode(sceneCode);
            if (sceneConfig == null) {
                throw new IllegalArgumentException("关联场景不存在");
            }
            if (!capabilityCode.equals(sceneConfig.getCapabilityCode())) {
                throw new IllegalArgumentException("关联场景与能力不匹配");
            }
        }

        normalizeJson(request.getMatchRuleJson(), "匹配规则");
        requireRouteRuleJson(request.getRouteRuleJson());
    }

    private void validateSafetyRuleReferences(AiGatewaySafetyRuleUpsertRequest request) {
        String direction = normalizeUpper(request.getDirection());
        if (!Set.of("INPUT", "OUTPUT", "BOTH").contains(direction)) {
            throw new IllegalArgumentException("安全规则方向不支持");
        }
        String action = normalizeUpper(request.getAction());
        if (!Set.of("BLOCK", "AUDIT").contains(action)) {
            throw new IllegalArgumentException("安全规则动作不支持");
        }
        String matchType = normalizeUpper(request.getMatchType());
        if (!Set.of("KEYWORD", "REGEX").contains(matchType)) {
            throw new IllegalArgumentException("安全规则匹配类型不支持");
        }
        String severity = normalizeUpper(request.getSeverity());
        if (!Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL").contains(severity)) {
            throw new IllegalArgumentException("安全规则风险等级不支持");
        }
        if ("REGEX".equals(matchType)) {
            try {
                java.util.regex.Pattern.compile(normalizeRequired(request.getPatternText()));
            } catch (Exception e) {
                throw new IllegalArgumentException("安全规则正则表达式不合法");
            }
        }

        String capabilityCode = normalizeOptional(request.getCapabilityCode());
        if (StringUtils.hasText(capabilityCode) && aiCapabilityConfigMapper.findByCapabilityCode(capabilityCode) == null) {
            throw new IllegalArgumentException("关联能力不存在");
        }

        String sceneCode = normalizeOptional(request.getSceneCode());
        if (!StringUtils.hasText(sceneCode)) {
            return;
        }
        AiSceneConfig sceneConfig = aiSceneConfigMapper.findBySceneCode(sceneCode);
        if (sceneConfig == null) {
            throw new IllegalArgumentException("关联场景不存在");
        }
        if (StringUtils.hasText(capabilityCode) && !capabilityCode.equals(sceneConfig.getCapabilityCode())) {
            throw new IllegalArgumentException("关联场景与能力不匹配");
        }
    }

    private void validateQuotaReferences(AiUsageQuotaUpsertRequest request) {
        String subjectType = normalizeUpper(request.getSubjectType());
        if (!Set.of("GLOBAL", "USER", "ROLE", "MERCHANT").contains(subjectType)) {
            throw new IllegalArgumentException("配额主体类型不支持");
        }
        if (!"GLOBAL".equals(subjectType) && !StringUtils.hasText(request.getSubjectId())) {
            throw new IllegalArgumentException("非全局配额必须指定主体");
        }
        String quotaPeriod = normalizeUpper(request.getQuotaPeriod());
        if (!Set.of("DAY", "MONTH").contains(quotaPeriod)) {
            throw new IllegalArgumentException("配额周期不支持");
        }
        if (request.getMaxCalls() == null && request.getMaxTokens() == null && request.getMaxCost() == null) {
            throw new IllegalArgumentException("配额至少需要一个上限");
        }

        String capabilityCode = normalizeOptional(request.getCapabilityCode());
        if (StringUtils.hasText(capabilityCode) && aiCapabilityConfigMapper.findByCapabilityCode(capabilityCode) == null) {
            throw new IllegalArgumentException("关联能力不存在");
        }

        String sceneCode = normalizeOptional(request.getSceneCode());
        if (!StringUtils.hasText(sceneCode)) {
            return;
        }
        AiSceneConfig sceneConfig = aiSceneConfigMapper.findBySceneCode(sceneCode);
        if (sceneConfig == null) {
            throw new IllegalArgumentException("关联场景不存在");
        }
        if (StringUtils.hasText(capabilityCode) && !capabilityCode.equals(sceneConfig.getCapabilityCode())) {
            throw new IllegalArgumentException("关联场景与能力不匹配");
        }
    }

    private void ensureProviderCodeUnique(String providerCode, Long currentId) {
        if (aiProviderConfigMapper.countByProviderCodeExcludingId(providerCode, currentId) > 0) {
            throw new IllegalArgumentException("供应商编码已存在");
        }
    }

    private void ensureCapabilityCodeUnique(String capabilityCode, Long currentId) {
        if (aiCapabilityConfigMapper.countByCapabilityCodeExcludingId(capabilityCode, currentId) > 0) {
            throw new IllegalArgumentException("能力编码已存在");
        }
    }

    private void ensureSceneCodeUnique(String sceneCode, Long currentId) {
        if (aiSceneConfigMapper.countBySceneCodeExcludingId(sceneCode, currentId) > 0) {
            throw new IllegalArgumentException("场景编码已存在");
        }
    }

    private void ensureModelUnique(String providerCode, String modelCode, Long currentId) {
        if (aiModelConfigMapper.countByProviderAndModelExcludingId(providerCode, modelCode, currentId) > 0) {
            throw new IllegalArgumentException("该供应商下模型编码已存在");
        }
    }

    private AiProviderConfig requireProvider(Long id) {
        AiProviderConfig config = aiProviderConfigMapper.findById(id);
        if (config == null) {
            throw new IllegalArgumentException("供应商配置不存在");
        }
        return config;
    }

    private AiCapabilityConfig requireCapability(Long id) {
        AiCapabilityConfig config = aiCapabilityConfigMapper.findById(id);
        if (config == null) {
            throw new IllegalArgumentException("能力配置不存在");
        }
        return config;
    }

    private AiModelConfig requireModel(Long id) {
        AiModelConfig config = aiModelConfigMapper.findById(id);
        if (config == null) {
            throw new IllegalArgumentException("模型配置不存在");
        }
        return config;
    }

    private AiGatewayRouteRule requireRouteRule(Long id) {
        AiGatewayRouteRule rule = aiGatewayRouteRuleMapper.findById(id);
        if (rule == null) {
            throw new IllegalArgumentException("路由规则不存在");
        }
        return rule;
    }

    private AiGatewaySafetyRule requireSafetyRule(Long id) {
        AiGatewaySafetyRule rule = aiGatewaySafetyRuleMapper.findById(id);
        if (rule == null) {
            throw new IllegalArgumentException("安全规则不存在");
        }
        return rule;
    }

    private AiUsageQuota requireQuota(Long id) {
        AiUsageQuota quota = aiUsageQuotaMapper.findById(id);
        if (quota == null) {
            throw new IllegalArgumentException("配额配置不存在");
        }
        return quota;
    }

    private AiSceneConfig requireScene(Long id) {
        AiSceneConfig config = aiSceneConfigMapper.findById(id);
        if (config == null) {
            throw new IllegalArgumentException("场景配置不存在");
        }
        return config;
    }

    private AiProviderConfigDTO toProviderDto(AiProviderConfig config) {
        AiProviderConfigDTO dto = new AiProviderConfigDTO();
        dto.setId(config.getId());
        dto.setProviderCode(config.getProviderCode());
        dto.setProviderName(config.getProviderName());
        dto.setBaseUrl(config.getBaseUrl());
        dto.setDefaultModelCode(config.getDefaultModelCode());
        dto.setEnabled(isEnabled(config.getEnabled()));
        dto.setTimeoutMs(config.getTimeoutMs());
        dto.setMaxContextMessages(config.getMaxContextMessages());
        dto.setTemperature(config.getTemperature());
        dto.setTopP(config.getTopP());
        dto.setMaxOutputTokens(config.getMaxOutputTokens());
        dto.setSystemPromptTemplate(config.getSystemPromptTemplate());
        dto.setHasApiKey(StringUtils.hasText(config.getApiKeyCipher()));
        dto.setCreatedAt(config.getCreatedAt());
        dto.setUpdatedAt(config.getUpdatedAt());
        return dto;
    }

    private AiCapabilityConfigDTO toCapabilityDto(AiCapabilityConfig config) {
        AiCapabilityConfigDTO dto = new AiCapabilityConfigDTO();
        dto.setId(config.getId());
        dto.setCapabilityCode(config.getCapabilityCode());
        dto.setCapabilityName(config.getCapabilityName());
        dto.setEnabled(isEnabled(config.getEnabled()));
        dto.setGrayEnabled(isEnabled(config.getGrayEnabled()));
        dto.setGrayRuleJson(config.getGrayRuleJson());
        dto.setRateLimitJson(config.getRateLimitJson());
        dto.setQuotaRuleJson(config.getQuotaRuleJson());
        dto.setStatus(isEnabled(config.getEnabled()) ? "ACTIVE" : "DISABLED");
        dto.setCreatedAt(config.getCreatedAt());
        dto.setUpdatedAt(config.getUpdatedAt());
        return dto;
    }

    private AiModelConfigDTO toModelDto(AiModelConfig config) {
        AiModelConfigDTO dto = new AiModelConfigDTO();
        dto.setId(config.getId());
        dto.setProviderCode(config.getProviderCode());
        dto.setModelCode(config.getModelCode());
        dto.setModelName(config.getModelName());
        dto.setCapabilitiesJson(config.getCapabilitiesJson());
        dto.setContextWindow(config.getContextWindow());
        dto.setMaxOutputTokens(config.getMaxOutputTokens());
        dto.setInputPricePer1k(config.getInputPricePer1k());
        dto.setOutputPricePer1k(config.getOutputPricePer1k());
        dto.setEnabled(isEnabled(config.getEnabled()));
        dto.setPriority(config.getPriority());
        dto.setCreatedAt(config.getCreatedAt());
        dto.setUpdatedAt(config.getUpdatedAt());
        return dto;
    }

    private AiGatewayRouteRuleDTO toRouteRuleDto(AiGatewayRouteRule config) {
        AiGatewayRouteRuleDTO dto = new AiGatewayRouteRuleDTO();
        dto.setId(config.getId());
        dto.setRuleName(config.getRuleName());
        dto.setCapabilityCode(config.getCapabilityCode());
        dto.setSceneCode(config.getSceneCode());
        dto.setMatchRuleJson(config.getMatchRuleJson());
        dto.setRouteRuleJson(config.getRouteRuleJson());
        dto.setEnabled(isEnabled(config.getEnabled()));
        dto.setPriority(config.getPriority());
        dto.setCreatedAt(config.getCreatedAt());
        dto.setUpdatedAt(config.getUpdatedAt());
        return dto;
    }

    private AiGatewaySafetyRuleDTO toSafetyRuleDto(AiGatewaySafetyRule rule) {
        AiGatewaySafetyRuleDTO dto = new AiGatewaySafetyRuleDTO();
        dto.setId(rule.getId());
        dto.setRuleName(rule.getRuleName());
        dto.setCapabilityCode(rule.getCapabilityCode());
        dto.setSceneCode(rule.getSceneCode());
        dto.setDirection(rule.getDirection());
        dto.setAction(rule.getAction());
        dto.setMatchType(rule.getMatchType());
        dto.setPatternText(rule.getPatternText());
        dto.setCategory(rule.getCategory());
        dto.setSeverity(rule.getSeverity());
        dto.setEnabled(isEnabled(rule.getEnabled()));
        dto.setPriority(rule.getPriority());
        dto.setCreatedAt(rule.getCreatedAt());
        dto.setUpdatedAt(rule.getUpdatedAt());
        return dto;
    }

    private AiUsageQuotaDTO toQuotaDto(AiUsageQuota quota) {
        AiUsageQuotaDTO dto = new AiUsageQuotaDTO();
        dto.setId(quota.getId());
        dto.setSubjectType(quota.getSubjectType());
        dto.setSubjectId(quota.getSubjectId());
        dto.setCapabilityCode(quota.getCapabilityCode());
        dto.setSceneCode(quota.getSceneCode());
        dto.setQuotaPeriod(quota.getQuotaPeriod());
        dto.setMaxCalls(quota.getMaxCalls());
        dto.setMaxTokens(quota.getMaxTokens());
        dto.setMaxCost(quota.getMaxCost());
        dto.setEnabled(isEnabled(quota.getEnabled()));
        dto.setCreatedAt(quota.getCreatedAt());
        dto.setUpdatedAt(quota.getUpdatedAt());
        return dto;
    }

    private AiSceneConfigDTO toSceneDto(AiSceneConfig config) {
        AiSceneConfigDTO dto = new AiSceneConfigDTO();
        dto.setId(config.getId());
        dto.setCapabilityCode(config.getCapabilityCode());
        dto.setSceneCode(config.getSceneCode());
        dto.setSceneName(config.getSceneName());
        dto.setProviderCode(config.getProviderCode());
        dto.setModelCode(config.getModelCode());
        dto.setEnabled(isEnabled(config.getEnabled()));
        dto.setSystemPromptTemplate(config.getSystemPromptTemplate());
        dto.setInputSchemaJson(config.getInputSchemaJson());
        dto.setOutputSchemaJson(config.getOutputSchemaJson());
        dto.setSafetyLevel(config.getSafetyLevel());
        dto.setTimeoutMs(config.getTimeoutMs());
        dto.setCreatedAt(config.getCreatedAt());
        dto.setUpdatedAt(config.getUpdatedAt());
        return dto;
    }

    private String normalizeRequired(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeUpper(String value) {
        return normalizeRequired(value).toUpperCase(Locale.ROOT);
    }

    private String normalizeJson(String value, String label) {
        String normalized = normalizeOptional(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        try {
            objectMapper.readTree(normalized);
            return normalized;
        } catch (Exception e) {
            throw new IllegalArgumentException(label + " JSON 不合法");
        }
    }

    private String requireRouteRuleJson(String value) {
        String normalized = normalizeJson(value, "路由规则");
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("路由规则 JSON 不能为空");
        }
        try {
            JsonNode root = objectMapper.readTree(normalized);
            JsonNode candidates = root.isArray() ? root : root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                throw new IllegalArgumentException("路由规则必须包含 candidates 数组");
            }
            return normalized;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("路由规则 JSON 不合法");
        }
    }

    private Integer toFlag(Boolean enabled) {
        return Boolean.TRUE.equals(enabled) ? 1 : 0;
    }

    private boolean isEnabled(Integer enabled) {
        return enabled != null && enabled == 1;
    }

    private Integer defaultIfNull(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    private Double defaultIfNull(Double value, Double defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String encryptApiKey(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return null;
        }
        return apiKeyCipherService.encrypt(apiKey);
    }
}
