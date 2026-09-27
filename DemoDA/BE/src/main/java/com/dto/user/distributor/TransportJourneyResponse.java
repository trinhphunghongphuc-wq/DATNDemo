package com.dto.user.distributor;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record TransportJourneyResponse(
        Long batchId,
        JsonNode plannedRoute,
        List<TransportPointResponse> points
) {}