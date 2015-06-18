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

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;

/**
 * Indexer for nodes assigned a UUID
 */
public interface TriggerIndexer {

	/**
	 * Index a node based on the UUID property
	 * @param node the node to index
	 */
	void indexNode(Node node);

	/**
	 * Remove a node from the index based on the UUID property
	 * @param node the node
	 */
	void deleteNodeFromIndex(Node node);

	/**
	 * Find a node given its UUID
	 * @param uuid the uuid
	 * @return the Node with the given UUID or null
	 */
	Index<Node> getTriggerNodes(String triggerLabel);

}
