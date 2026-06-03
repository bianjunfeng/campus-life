package com.campus.campus_life_ai.ai.gateway.route;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class RoutePlan {
    List<RouteCandidate> candidates;
}
