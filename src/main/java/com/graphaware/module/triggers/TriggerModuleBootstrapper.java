/*
 * Copyright (c) 2014 GraphAware
 *
 * This file is part of GraphAware.
 *
 * GraphAware is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package com.graphaware.module.triggers;

import com.graphaware.common.policy.NodeInclusionPolicy;
import com.graphaware.runtime.config.function.StringToNodeInclusionPolicy;
import com.graphaware.runtime.module.RuntimeModule;
import com.graphaware.runtime.module.RuntimeModuleBootstrapper;

import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Bootstraps the {@link TriggerModule} in server mode.
 */
public class TriggerModuleBootstrapper implements RuntimeModuleBootstrapper {

    private static final Logger LOG = LoggerFactory.getLogger(TriggerModuleBootstrapper.class);

    //keys to use when configuring using neo4j.properties
    private static final String DIRECTORY_NAME = TriggerProperties.DIRECTORY_NAME;
    private static final String TRIGGER_NODE_LABEL = TriggerProperties.DATABASE_TRIGGER_NODE_LABEL;
    private static final String TRIGGER_INDEX_NAME = TriggerProperties.TRIGGER_NODEX_INDEX_NAME;
    private static final String NODE = "node";

    /**
     * @{inheritDoc}
     */
    @Override
    public RuntimeModule bootstrapModule(String moduleId, Map<String, String> config, GraphDatabaseService database) {
        TriggerModuleConfiguration configuration = TriggerModuleConfiguration.defaultConfiguration();
        String directoryName = config.get(DIRECTORY_NAME);
        String triggerNodeLabel = config.get(TRIGGER_NODE_LABEL);
        String triggerIndexName = config.get(TRIGGER_INDEX_NAME);

        directoryName = config.get(DIRECTORY_NAME);
        if ((config.get(DIRECTORY_NAME) != null && config.get(DIRECTORY_NAME).length() > 0)) {
            LOG.info(DIRECTORY_NAME + " set to {}", configuration.getDirectoryName());
        }

        triggerNodeLabel = config.get(TRIGGER_NODE_LABEL);
        if ((config.get(TRIGGER_NODE_LABEL) != null && config.get(TRIGGER_NODE_LABEL).length() > 0)) {
            LOG.info(TRIGGER_NODE_LABEL + " set to {}", configuration.getTriggerNodeLabel());
        }
    	
        triggerIndexName = config.get(TRIGGER_INDEX_NAME);
        if ((config.get(TRIGGER_INDEX_NAME) != null && config.get(TRIGGER_INDEX_NAME).length() > 0)) {
            LOG.info(TRIGGER_INDEX_NAME + " set to {}", configuration.getTriggerIndexName());
        }
    	configuration = configuration.withAllProperties(directoryName, triggerNodeLabel, triggerIndexName);
        if (config.get(NODE) != null) {
            NodeInclusionPolicy policy = StringToNodeInclusionPolicy.getInstance().apply(config.get(NODE));
            LOG.info(NODE + " Inclusion Strategy set to {}", policy);
            configuration = configuration.with(policy);
        }

        return new TriggerModule(moduleId, configuration, database);
    }
}
