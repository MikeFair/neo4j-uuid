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

package com.graphaware.module.triggers.index;

import com.graphaware.module.triggers.TriggerModuleConfiguration;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;

/**
 * Legacy Index implementation for indexing and finding nodes assigned a UUID
 */
public class LegacyTriggerIndexer implements TriggerIndexer {

    private final GraphDatabaseService database;
    private final TriggerModuleConfiguration configuration;

    public LegacyTriggerIndexer(GraphDatabaseService database, TriggerModuleConfiguration configuration) {
        this.database = database;
        this.configuration = configuration;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void indexNode(Node node) {
        database.index().forNodes(configuration.getTriggerIndexName()).add(node, configuration.getTriggerIndexName(), configuration.getTriggerNodeLabel());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Index<Node> getTriggerNodes(String triggerLabel) {
        //return database.index().forNodes(configuration.getTriggerIndex()).get(configuration.getUuidProperty(), uuid).getSingle();
        return database.index().forNodes(configuration.getTriggerIndexName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteNodeFromIndex(Node node) {
        database.index().forNodes(configuration.getTriggerIndexName()).remove(node, configuration.getTriggerNodeLabel());
    }
}
