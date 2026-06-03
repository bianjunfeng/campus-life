package com.campus.campus_life_backend.modules.admin.service;

import com.campus.campus_life_backend.modules.admin.dto.AdminApiInfoDTO;
import com.campus.campus_life_backend.modules.admin.dto.AdminApiParamDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Service
public class AdminApiCatalogService {

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    public AdminApiCatalogService(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
    }

    public List<AdminApiInfoDTO> listApis() {
        List<AdminApiInfoDTO> result = new ArrayList<>();
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMappingHandlerMapping.getHandlerMethods();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            RequestMappingInfo mappingInfo = entry.getKey();
            HandlerMethod handlerMethod = entry.getValue();

            Set<String> patterns = new TreeSet<>(mappingInfo.getPatternValues());
            if (patterns.isEmpty()) {
                continue;
            }

            Set<String> methods = resolveMethods(mappingInfo);
            for (String pattern : patterns) {
                if (!pattern.startsWith("/api/")) {
                    continue;
                }
                for (String method : methods) {
                    AdminApiInfoDTO item = new AdminApiInfoDTO();
                    item.setPath(pattern);
                    item.setMethod(method);
                    item.setModule(resolveModule(pattern));
                    item.setDescription(resolveDescription(pattern, handlerMethod));
                    item.setParams(resolveParams(handlerMethod));
                    result.add(item);
                }
            }
        }

        result.sort(Comparator
                .comparing(AdminApiInfoDTO::getModule, Comparator.nullsLast(String::compareTo))
                .thenComparing(AdminApiInfoDTO::getPath, Comparator.nullsLast(String::compareTo))
                .thenComparing(AdminApiInfoDTO::getMethod, Comparator.nullsLast(String::compareTo)));
        return result;
    }

    private Set<String> resolveMethods(RequestMappingInfo mappingInfo) {
        if (mappingInfo.getMethodsCondition().getMethods().isEmpty()) {
            return Set.of("ALL");
        }
        return mappingInfo.getMethodsCondition().getMethods().stream()
                .map(Enum::name)
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
    }

    private String resolveModule(String path) {
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        String[] segments = normalized.split("/");
        if (segments.length >= 2) {
            return segments[1];
        }
        return "common";
    }

    private String resolveDescription(String path, HandlerMethod handlerMethod) {
        String beanName = handlerMethod.getBeanType().getSimpleName().replace("Controller", "");
        String methodName = handlerMethod.getMethod().getName();
        return beanName + "." + methodName + " - " + path;
    }

    private List<AdminApiParamDTO> resolveParams(HandlerMethod handlerMethod) {
        List<AdminApiParamDTO> params = new ArrayList<>();
        for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
            Class<?> parameterType = parameter.getParameterType();
            if (shouldSkip(parameterType)) {
                continue;
            }

            AdminApiParamDTO item = new AdminApiParamDTO();
            item.setName(resolveParamName(parameter));
            item.setType(resolveType(parameter));
            item.setRequired(resolveRequired(parameter));
            item.setDescription(resolveParamDescription(parameter));
            params.add(item);
        }
        return params;
    }

    private boolean shouldSkip(Class<?> parameterType) {
        return HttpServletRequest.class.isAssignableFrom(parameterType)
                || org.springframework.http.HttpHeaders.class.isAssignableFrom(parameterType)
                || org.springframework.validation.BindingResult.class.isAssignableFrom(parameterType);
    }

    private String resolveParamName(MethodParameter parameter) {
        RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
        if (requestParam != null && !requestParam.name().isBlank()) {
            return requestParam.name();
        }

        PathVariable pathVariable = parameter.getParameterAnnotation(PathVariable.class);
        if (pathVariable != null && !pathVariable.name().isBlank()) {
            return pathVariable.name();
        }

        RequestHeader requestHeader = parameter.getParameterAnnotation(RequestHeader.class);
        if (requestHeader != null && !requestHeader.name().isBlank()) {
            return requestHeader.name();
        }

        String discovered = parameter.getParameterName();
        return discovered == null || discovered.isBlank() ? "body" : discovered;
    }

    private String resolveType(MethodParameter parameter) {
        Class<?> parameterType = parameter.getParameterType();
        if (parameter.hasParameterAnnotation(RequestBody.class)) {
            return "body<" + parameterType.getSimpleName() + ">";
        }
        return parameterType.getSimpleName();
    }

    private boolean resolveRequired(MethodParameter parameter) {
        RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
        if (requestParam != null) {
            return requestParam.required();
        }

        PathVariable pathVariable = parameter.getParameterAnnotation(PathVariable.class);
        if (pathVariable != null) {
            return pathVariable.required();
        }

        RequestHeader requestHeader = parameter.getParameterAnnotation(RequestHeader.class);
        if (requestHeader != null) {
            return requestHeader.required();
        }

        RequestBody requestBody = parameter.getParameterAnnotation(RequestBody.class);
        if (requestBody != null) {
            return requestBody.required();
        }

        return true;
    }

    private String resolveParamDescription(MethodParameter parameter) {
        if (parameter.hasParameterAnnotation(RequestBody.class)) {
            return "请求体";
        }
        if (parameter.hasParameterAnnotation(PathVariable.class)) {
            return "路径参数";
        }
        if (parameter.hasParameterAnnotation(RequestParam.class)) {
            return "查询参数";
        }
        if (parameter.hasParameterAnnotation(RequestHeader.class)) {
            return "请求头";
        }
        return "方法参数";
    }
}
