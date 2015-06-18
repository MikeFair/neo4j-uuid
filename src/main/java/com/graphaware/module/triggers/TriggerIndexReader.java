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


import com.graphaware.module.triggers.index.LegacyTriggerIndexer;
import com.graphaware.module.triggers.index.TriggerIndexer;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;

public class TriggerIndexReader {

    private final LegacyTriggerIndexer indexer;
    private final GraphDatabaseService database;

    public TriggerIndexReader(TriggerModuleConfiguration configuration, GraphDatabaseService database) {
        this.database = database;
        this.indexer = new LegacyTriggerIndexer(database, configuration);
    }

    /**
     * Get a node by its UUID.
     *
     * @param triggerNodeLabel String.
     * @return nodes Index<Node>.
     * @throws org.neo4j.graphdb.NotFoundException in case no node exists with such UUID.
     */
    public Index<Node> getTriggerNodes(String triggerNodeLabel) {
        Index<Node> nodes;

        try (Transaction tx = database.beginTx()) {
            nodes = indexer.getTriggerNodes(triggerNodeLabel);
            tx.success();
        }

        if (nodes == null) {
            throw new NotFoundException("No triggers with label " + triggerNodeLabel + " found in the database");
        }

        return nodes;
    }
}
