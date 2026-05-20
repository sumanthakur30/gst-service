package com.shopmanagement.gstservice.engine.factory;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.shopmanagement.gstservice.engine.strategy.DefaultGstStrategy;
import com.shopmanagement.gstservice.engine.strategy.GstBusinessStrategy;
import com.shopmanagement.gstservice.model.BusinessType;

@Component
public class GstStrategyFactory {

    private final Map<BusinessType, GstBusinessStrategy> strategies;
    private final DefaultGstStrategy defaultStrategy;

    public GstStrategyFactory(List<GstBusinessStrategy> strategyList, DefaultGstStrategy defaultStrategy) {
        this.defaultStrategy = defaultStrategy;
        this.strategies = new EnumMap<>(BusinessType.class);
        for (GstBusinessStrategy strategy : strategyList) {
            strategies.put(strategy.businessType(), strategy);
        }
    }

    public GstBusinessStrategy resolve(BusinessType businessType) {
        BusinessType key = businessType == null ? BusinessType.DEFAULT : businessType;
        return strategies.getOrDefault(key, defaultStrategy);
    }
}
