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

/**
 * Properties of nodes and relationships written by the {@link com.graphaware.module.uuid.UuidModule}.
 */
public final class TriggerProperties {

    public static final String DIRECTORY_NAME = "triggers";
    public static final String DATABASE_TRIGGER_NODE_LABEL = "DATABASE_TRIGGER";
    public static final String TRIGGER_NODEX_INDEX_NAME = "triggerNodeIndex";

    private TriggerProperties() {
    }
}
