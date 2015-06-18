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

import org.neo4j.graphdb.Node;

import java.util.Collection;
import java.util.ArrayList;
//import java.util.HashMap;

/**
 * {@link PropertyTrigger} for {@link com.graphaware.module.uuid.TriggerModule}.
 */

public class PropertyTrigger extends BaseTxDrivenModuleConfiguration<PropertyTrigger> {
	InclusionPolicies inclusionPolicies;  // This will be registered with the TriggerModule (How to make this without extending TxConfiguration?)
	Boolean bHasIndex = false; // Perhaps use a null or empty value on propIndexName instead?
	String propIndexName;      // Make this a Collection of PropertyName and IndexName?
	Collection<Node> createdNodes = new ArrayList<Node>();
	Collection<Node> deletedNodes = new ArrayList<Node>();
	Collection<Node> modifiedNodes = new ArrayList<Node>();
//	Collection<Change<Node>> changedNodes = new ArrayList<Change<Node>>;

	public final Collection<String> _whenPropertiesChange = new ArrayList<String>();
	public final Collection<String> _whenLabelsChange = new ArrayList<String>();

	String propName; // A Collection of properties?
	Boolean[] Events = {false, false, false};
	
    protected PropertyTrigger(InclusionPolicies inclusionPolicies) {
        super(inclusionPolicies);
    }

    public PropertyTrigger(InclusionPolicies inclusionPolicies, String name) {
        super(inclusionPolicies);
        this.propName = name;
    }

    public PropertyTrigger(InclusionPolicies inclusionPolicies, String name, String indexName) {
        super(inclusionPolicies);
        this.propName = name;
        this.bHasIndex = true;
        this.propIndexName = indexName;
    }

    /**
     */
    public static PropertyTrigger defaultConfiguration(String propName) {
        return new PropertyTrigger(InclusionPoliciesFactory.allBusiness(), propName);
    }
    public static PropertyTrigger defaultConfiguration(String propName, String indexName) {
        return new PropertyTrigger(InclusionPoliciesFactory.allBusiness(), propName, indexName);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected PropertyTrigger newInstance(InclusionPolicies inclusionPolicies) {
        return new PropertyTrigger(inclusionPolicies);
    }

    protected PropertyTrigger newInstance(InclusionPolicies inclusionPolicies, String propName) {
        return new PropertyTrigger(inclusionPolicies, propName);
    }

    protected PropertyTrigger newInstance(InclusionPolicies inclusionPolicies, String propName, String indexName) {
        return new PropertyTrigger(inclusionPolicies, propName, indexName);
    }

    public String getPropertyName() {  // Should be a Collection for multiple properties?
        return propName;
    }

    public String getIndexName() { // Should be a Collection for multiple indexes 
        return propIndexName;
    }
    
    public void addToCreatedNodes(Node node){
    	createdNodes.add(node);
    }
    
    public Boolean OnCreate(Node node) {
    	return true;
    }
    
    public Boolean OnCreate(Collection<Node> node) {
    	return true;
    }
    
    public Boolean OnModify(Node _previous, Node _current) {
    	return true;
    }
    
    public Boolean OnModify(Collection<Node> node) {
    	return true;
    }
    
    public Boolean OnDelete(Node node) {
    	return true;
    }
    
    public Boolean OnDelete(Collection<Node> node) {
    	return true;
    }
    
    public Boolean OnChange(int Event, Node _previous, Node _current) {
    	return true;
    }

    // OnChange for a Collection should work with the Collection<Change<Node>> type?
}
