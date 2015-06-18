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

import com.graphaware.common.policy.InclusionPolicies;
import com.graphaware.runtime.config.BaseTxDrivenModuleConfiguration;
import com.graphaware.runtime.policy.InclusionPoliciesFactory;

import java.util.ArrayList;

/**
 * {@link BaseTxDrivenModuleConfiguration} for {@link com.graphaware.module.uuid.UuidModule}.
 */
public class TriggerModuleConfiguration extends BaseTxDrivenModuleConfiguration<TriggerModuleConfiguration> {

    private static final String DEFAULT_DIRECTORY_NAME = TriggerProperties.DIRECTORY_NAME;
    private static final String DEFAULT_TRIGGER_NODE_LABEL = TriggerProperties.DATABASE_TRIGGER_NODE_LABEL;
    private static final String DEFAULT_INDEX_NAME = TriggerProperties.TRIGGER_NODEX_INDEX_NAME;

    private ArrayList<PropertyTrigger> propList = new ArrayList<PropertyTrigger>();
    private String directoryName;
    private String triggerNodeLabel;
    private String triggerIndexName;

    protected TriggerModuleConfiguration(InclusionPolicies inclusionPolicies) {
        super(inclusionPolicies);
    }

    protected TriggerModuleConfiguration(InclusionPolicies inclusionPolicies, String directoryName, String triggerNodeLabel, String triggerIndexName) {
        super(inclusionPolicies);
        this.directoryName = directoryName;
        this.triggerNodeLabel = triggerNodeLabel;
        this.triggerIndexName = triggerIndexName;
    }

    /**
     * Create a default configuration with default trigger_directory property = {@link #DEFAULT_DIRECTORY_NAME}, trigger index = {@link #DEFAULT_TRIGGER_NODEX_INDEX}
     * labels=all (including nodes with no labels)
     * inclusion strategies = {@link com.graphaware.runtime.policy.InclusionPoliciesFactory#allBusiness()},
     * (nothing is excluded except for framework-internal nodes and relationships)
     * TODO: Add triggers to framework-internal nodes and relationships
     * <p/>
     * Change this by calling {@link #withDirectoryProperty(String)}, with* other inclusion strategies
     * on the object, always using the returned object (this is a fluent interface).
     */
    public static TriggerModuleConfiguration defaultConfiguration() {
        return new TriggerModuleConfiguration(InclusionPoliciesFactory.allBusiness(), DEFAULT_DIRECTORY_NAME, DEFAULT_TRIGGER_NODE_LABEL, DEFAULT_INDEX_NAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected TriggerModuleConfiguration newInstance(InclusionPolicies inclusionPolicies) {
        return new TriggerModuleConfiguration(inclusionPolicies, getDirectoryName(), getTriggerNodeLabel(), getTriggerIndexName());
    }

    public String getDirectoryName() {
        return this.directoryName;
    }

    public String getTriggerNodeLabel() {
        return this.triggerNodeLabel;
    }

    public String getTriggerIndexName() {
        return this.triggerIndexName;
    }

    /**
     * Create a new instance of this {@link TriggerModuleConfiguration} with different directory, trigger, and indexName property.
     *
     * @param indexName of the new instance.
     * @return new instance.
     */

	public TriggerModuleConfiguration withAllProperties(String directoryName, String triggerNodeLabel, String triggerIndexName) {
		if (directoryName == null || directoryName.length() == 0){
			directoryName = getDirectoryName();
		}
		if (triggerNodeLabel == null || triggerNodeLabel.length() == 0) {
			triggerNodeLabel = getTriggerNodeLabel();
		}
		if (triggerIndexName == null || triggerIndexName.length() == 0) {
			triggerIndexName = getTriggerIndexName();
		}
		return new TriggerModuleConfiguration(getInclusionPolicies(), directoryName, triggerNodeLabel, triggerIndexName);
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        TriggerModuleConfiguration that = (TriggerModuleConfiguration) o;

        if (!directoryName.equals(that.directoryName)) return false;
        if (!triggerNodeLabel.equals(that.triggerNodeLabel)) return false;
        if (!triggerIndexName.equals(that.triggerIndexName)) return false;

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = super.hashCode();
        return result;
    }
}
