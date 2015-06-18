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

import com.graphaware.common.uuid.EaioUuidGenerator;
import com.graphaware.module.uuid.index.LegacyIndexer;
import com.graphaware.runtime.config.BaseTxDrivenModuleConfiguration;
import com.graphaware.runtime.module.BaseTxDrivenModule;
import com.graphaware.runtime.module.DeliberateTransactionRollbackException;
import com.graphaware.tx.event.improved.api.Change;
import com.graphaware.tx.event.improved.api.ImprovedTransactionData;
import com.graphaware.tx.executor.batch.IterableInputBatchTransactionExecutor;
import com.graphaware.tx.executor.batch.UnitOfWork;
import com.graphaware.tx.executor.single.TransactionCallback;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.tooling.GlobalGraphOperations;
import org.neo4j.graphdb.Relationship;
import org.neo4j.shell.kernel.apps.NodeOrRelationship;

//import com.graphaware.common.uuid.UuidGenerator;
import com.graphaware.module.triggers.TriggerModuleConfiguration;
import com.graphaware.module.triggers.index.TriggerIndexer;
import com.graphaware.module.triggers.index.LegacyTriggerIndexer;
import org.neo4j.graphdb.Label;


import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;


/**
 * {@link com.graphaware.runtime.module.TxDrivenModule} that instantiates a dynamic trigger framework for the graph.
 * Once installed, it should be possible to add Triggers as class extensions of the PropertyTrigger class in a jar file in a directory.
 * The directory name can be set in the module configuration. (Question:  Should these be implementations of the "PropertyTrigger Interface" instead??) 
 * It should also be possible to add TriggerNodes to the graph, with the proper label (DATABASE_TRIGGER); label can be set in the configuration;
 * Properties on the TriggerNode instruct the framework what to do, easy for creating "simple triggers" that just use framework code.
 * (TODO) properties identify the scripting language, and scripts used for CREATE/DELETE/MODIFY (CHANGE) trigger code.
 * 
 * Additionally, the framework also supports:
 * simple triggers like preventing create/modify/delete of properties outside the trigger;
 * filtering calls to triggers for only when certain properties have changed;
 * filtering calls to triggers for only when certain properties change to/from certain values (i.e. OnPropertyChanged); (TODO)
 * providing canCreate, canDelete, canModify functions in addition to defining simple property tests; (TODO)
 * calls to the onCreate, onDelete, onModify functions on either or both per node and (TODO) collections
 * 
 */
public class TriggerModule extends BaseTxDrivenModule<Void> {
	
	private final static Logger LOGGER = Logger.getLogger(TriggerModule.class.getName()); 

    public static final String DEFAULT_MODULE_ID = "TRGM";
    private static final int BATCH_SIZE = 1000;

    //private final UuidGenerator uuidGenerator;
    private final TriggerModuleConfiguration triggerModuleConfiguration;
    private final TriggerIndexer triggerIndexer;
    private final Collection<PropertyTrigger> OnCreateTriggers = new ArrayList<PropertyTrigger>();
    private final Collection<PropertyTrigger> OnDeleteTriggers = new ArrayList<PropertyTrigger>();
    private final Collection<PropertyTrigger> OnModifyTriggers = new ArrayList<PropertyTrigger>();
    private final Collection<PropertyTrigger> OnChangeTriggers = new ArrayList<PropertyTrigger>();

    //TODO: Make all of these a Map of String -> Collection<PropertyTrigger> (or String (triggerName)) for reporting which triggers blocked the action 
	// Block additions created outside of the trigger
	private final Collection<String> _preventPropertiesCreate = new ArrayList<String>();
	private final Collection<String> _preventLabelsCreate = new ArrayList<String>();

	// Block modifications made outside of the trigger
	private final Collection<String> _preventPropertiesModify = new ArrayList<String>();
	private final Collection<String> _preventLabelsModify = new ArrayList<String>();  // Does this belong here? Is it something other than a Delete and Add in the same change? 
	
	// Block deletes made outside of the trigger
	private final Collection<String> _preventPropertiesDelete = new ArrayList<String>();
	private final Collection<String> _preventLabelsDelete = new ArrayList<String>();

	// Maintain indexes for trigger?  - A list of index names and a list of property names?
	// TODO: There has to be something more elegant than this
	private final Collection<String> _indexLabels = new ArrayList<String>();
	private final Collection<String> _indexProperties = new ArrayList<String>();


	// Export what properties and labels this class modifies, use empty if property names are dynamically defined and so are unknown
	// Note these collections are within the PropertyTriggers themselves and a global consolidation is likely not needed in this class.
	// REMINDER: PropertyTriggers have info about their behavior, can use this to detect potential conflicts, provide info on request?
	//private final Collection<String> _modifiesProperties = new ArrayList<String>();
	//private final Collection<String> _modifiesLabels = new ArrayList<String>();
	
	// Note: These are also per trigger; 
	// The trigger will only be called "once" for any change in the entire set of listed filters
	//private final Collection<String> _whenPropertiesChange = new ArrayList<String>();
	//private final Collection<String> _whenLabelsChange = new ArrayList<String>();

    /**
     * Construct a new Trigger module.
     *
     * @param moduleId ID of the module.
     */
    public TriggerModule(String moduleId, TriggerModuleConfiguration configuration, GraphDatabaseService database) {
        super(moduleId);
        //this.uuidGenerator = new EaioUuidGenerator();
        this.triggerModuleConfiguration = configuration;
        this.triggerIndexer = new LegacyTriggerIndexer(database, configuration);  // Perhaps separate for CREATE, UPDATE, DELETE, ANY?
        
        //TODO: Iterate over jars in configuration.directoryName and populate list of PropertyTriggers
        //TODO: Search configuration.indexName for PropertyTrigger Nodes and populate list of PropertyTriggers
        //TODO: Perhaps this should be done in "initialize"?  Or is that a "per txn" initialize call
        //TODO: Initialize Scripting Engines for executing code on PropertyTrigger Nodes (Using JSR 223?) (Node props identify the languages used)

        //Populate PropertyName -> PropertyTrigger maps for create, delete, modify, change
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TriggerModuleConfiguration getConfiguration() {
        return triggerModuleConfiguration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(GraphDatabaseService database) {
        new IterableInputBatchTransactionExecutor<>(
                database,
                BATCH_SIZE,
                new TransactionCallback<Iterable<Node>>() {
                    @Override
                    public Iterable<Node> doInTransaction(GraphDatabaseService database) throws Exception {
                        return GlobalGraphOperations.at(database).getAllNodes();
                    }
                },
                new UnitOfWork<Node>() {
                    @Override
                    public void execute(GraphDatabaseService database, Node node, int batchNumber, int stepNumber) {
                	    // What does this execute function within initialize do versus the beforeCommit call? both call assignUuid(node)?
                    	// This test should move to a per PropertyTrigger inclusion test...
                	    LOGGER.finer("TRIGGER MODULE: looping over nodes in txn batch " +  String.valueOf(batchNumber));
                		for (PropertyTrigger trigger : OnCreateTriggers) {
                			if (trigger.getInclusionPolicies().getNodeInclusionPolicy().include(node)) {
                				//trigger.assignUuid(node);                			}                		
                			}
                		}
                    }
                }
        ).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Void beforeCommit(ImprovedTransactionData transactionData) throws DeliberateTransactionRollbackException {
	    LOGGER.finer("TRIGGER MODULE: entered beforeCommit");
		int CREATE = 0;
		int MODIFY = 1;
		int DELETE = 2;
	    
	    //TODO: iterate through the created nodes, test nodes for registered property/label existence, call per node OnCreate/OnChange Handlers
	    //TODO: iterate through the deleted nodes, test nodes for registered property/label existence, call per node OnDelete/OnChange Handlers
	    //TODO: iterate through the changed nodes, test nodes for registered property/label changes, call per node OnModify/OnChange Handlers

		
		
		/*
		 * OnCreate
		 */
        //Iterate through all created nodes, for each trigger enforce NoCreate, test for registered hasProperty, call OnCreate
	    LOGGER.finer("TRIGGER MODULE: looping over Created Nodes");
        for (Node node : transactionData.getAllCreatedNodes()) {
    	    LOGGER.finest("TRIGGER MODULE: looping through _preventPropertiesCreate");
    		for (String propName : this._preventPropertiesCreate) {
                //TODO: Enable PropertyTriggers to register for calling a "CanCreate" function
                if (node.hasProperty(propName)) {
            	    LOGGER.finest("TRIGGER MODULE: Found property: " + propName);
                    throw new DeliberateTransactionRollbackException("Creating nodes with the " + propName + " property blocked by trigger framework.");
                    //TODO: Add trigger name to above message
                }
    		}
// TODO: Enable triggers for only nodes with a particular label
//    		for (String labelName : this._preventLabelsCreate) {
//                if (node.hasLabel(labelName)) {
//                    throw new DeliberateTransactionRollbackException("Creating nodes with the " + labelName + " label has been blocked.");                    	
//                }
//    		}

    	    LOGGER.finer("TRIGGER MODULE: looping over OnCreate triggers for changed nodes");
    		for (PropertyTrigger trigger : this.OnCreateTriggers) {
  				trigger.OnCreate(node);
   				//trigger.OnCreate(NodeOrRelationship.wrap(node));
  	            //trigger.Indexer.addNodeToIndex(node);
        	}
    	    LOGGER.finer("TRIGGER MODULE: looping over OnChange per node triggers for created nodes");
    		for (PropertyTrigger trigger : this.OnChangeTriggers) {
  				trigger.OnChange(CREATE, null, node);
   				//trigger.OnCreate(NodeOrRelationship.wrap(node));
  	            //trigger.Indexer.indexNode(node);
        	}
        }
/*      
  		// Create on relationships
        for (Relationship rel : transactionData.getAllCreatedRelationships()) {
            assignUuid(rel);
        	assignRev(NodeOrRelationship.wrap(rel));
        }
*/        

        
        
		/*
		 * OnDelete
		 */
        LOGGER.finer("TRIGGER MODULE: looping over Deleted Nodes");
        for (Node node : transactionData.getAllDeletedNodes()) {
    	    LOGGER.finest("TRIGGER MODULE: looping through _preventPropertiesDelete");
    		for (String propName : this._preventPropertiesDelete) {
                //TODO: Enable PropertyTriggers to register for calling a "CanDelete" function 
                if (node.hasProperty(propName)) {
            	    LOGGER.finest("TRIGGER MODULE: Found property: " + propName);
                    throw new DeliberateTransactionRollbackException("Deleting nodes with the " + propName + " property blocked by trigger framework.");
                    //TODO: Add trigger name to above message
                }
    		}
// TODO: Enable triggers for only nodes with a particular label
//    		for (String labelName : this._preventLabelsCreate) {
//                if (node.hasLabel(labelName)) {
//                    throw new DeliberateTransactionRollbackException("Deleting nodes with the " + labelName + " label has been blocked.");                    	
//                }
//    		}
    		
    	    LOGGER.finer("TRIGGER MODULE: looping over OnDelete triggers for deleted nodes");
    		for (PropertyTrigger trigger : this.OnDeleteTriggers) {
  				trigger.OnDelete(node);
   				//trigger.OnDelete(NodeOrRelationship.wrap(node));
  				
  	            //trigger.Indexer.deleteNodeFromIndex(node);
        	}
    	    LOGGER.finer("TRIGGER MODULE: looping over OnChange per node triggers for deleted nodes");
    		for (PropertyTrigger trigger : this.OnChangeTriggers) {
  				trigger.OnChange(DELETE, node, null);
   				//trigger.OnCreate(NodeOrRelationship.wrap(node));
  	            //trigger.Indexer.addNodeToIndex(node);
        	}
        }

        
        
        /*
         * OnModify
         */
        //Check if the locked properties have been modified or removed from the node and throw an error
        LOGGER.finer("TRIGGER MODULE: looping over Changed Nodes");
        for (Change<Node> change : transactionData.getAllChangedNodes()) {
    	    LOGGER.finest("TRIGGER MODULE: looping through _preventPropertiesModify");
    		for (String propName : this._preventPropertiesModify) {
    			if (!change.getCurrent().hasProperty(propName)) {
    				throw new DeliberateTransactionRollbackException("Changing property " + propName + " has been blocked by Trigger framework.");  // Add Trigger Name
    			}
        
    			if (!change.getCurrent().hasProperty(propName)) {
    				throw new DeliberateTransactionRollbackException("Removing property " + propName + " has been blocked by Trigger framework."); // Add Trigger Name
    			}

                if (!change.getPrevious().getProperty(propName).equals(change.getCurrent().getProperty(propName))) {
                    throw new DeliberateTransactionRollbackException("Attempt to modify the property " + propName + " has been blocked by Trigger framework.");
                }
    		}
    		
    	    LOGGER.finer("TRIGGER MODULE: looping over OnModify triggers for changed nodes");
    		for (PropertyTrigger trigger : this.OnModifyTriggers) {
    			boolean bHasChanged = false;
    			for (String propName : trigger._whenPropertiesChange) {
    				bHasChanged = (!change.getPrevious().getProperty(propName).equals(change.getCurrent().getProperty(propName)));
    				if (bHasChanged) { break; }  // Should we add propName to a changed property list and not break?
    			}
    			if (bHasChanged || trigger._whenPropertiesChange.isEmpty()) {
    				trigger.OnModify(change.getPrevious(), change.getCurrent());
       				//trigger.OnModify(NodeOrRelationship.wrap(node));
    			}
  				
  	            //trigger.Indexer.indexNode(node);
        	}
    	    LOGGER.finer("TRIGGER MODULE: looping over OnChange per node triggers for changed nodes");
    		for (PropertyTrigger trigger : this.OnChangeTriggers) {
    			boolean bHasChanged = false;
    			for (String propName : trigger._whenPropertiesChange) {
    				bHasChanged = (!change.getPrevious().getProperty(propName).equals(change.getCurrent().getProperty(propName)));
    				if (bHasChanged) { break; }  // Should we add propName to a changed property list and not break?
    			}
    			if (bHasChanged || trigger._whenPropertiesChange.isEmpty()) {
    				trigger.OnChange(MODIFY, change.getPrevious(), change.getCurrent());
       				//trigger.OnChange(NodeOrRelationship.wrap(MODIFY, change.getPrevious(), change.getCurrent()));
    			}
  	            //trigger.Indexer.updateNodeOnIndex(node);
        	}
        }

        return null;
    }

    
// Originally pass at conception for example code; for reference only until it gets deleted 
/*
 * This now goes into class UuidTrigger extends PropertyTrigger class 
     
    private void assignUuid(Node node) {
        if (!node.hasProperty(uuidConfiguration.getUuidProperty())) {
            String uuid = uuidGenerator.generateUuid();
            node.setProperty(uuidConfiguration.getUuidProperty(), uuid);
        }
    }
}
*/


/*
// Triggering Events
public enum EVENT{ 
	CREATE(0), UPDATE(1), DELETE(2);
	private final int value;
	
	private EVENT(final int newValue) { value = newValue; }
	public int getValue() { return value; }
}

// Members of nodes/relationships
public enum MTYPE
{ 
	PROPERTY(0), LABEL(1);  // While an Index is technically a member of the graph;
	private final int value;
	
	private MTYPE(final int newValue) { value = newValue; }
	public int getValue() { return value; }
}

// Entity Type in the Graph 
public enum ETYPE 
{ 
	NODE(0), RELATIONSHIP(1), INDEX(2);
	private final int value;
	
	private ETYPE(final int newValue) { value = newValue; }
	public int getValue() { return value; }
}
*/
    
/*
// By default this TriggerImplementation class reacts to everything, but handles nothing
// I've tried to add values that create intelligent filters for the framework to put triggers onto lists in a change set
abstract class TriggerImplementation
{
	
	// How does this trigger get called, with a Collection or per Entity?  Does the subclass implement the interface?
	public boolean _handlesPerEntity = false;
	public boolean _handlesPerCollection = false;

	// Turn on/off calls for properties/labels
	// Arrays of booleans for Node/Relationship CREATE, UPDATE, DELETE of properties and/or labels
	// (Perhaps define subclasses for NodePropertyChanger/NodeLabelChangers/RelationshipPropertyChanger/RelationshipLabelChanger that set up defaults for these)
	boolean[] _handlesProperties = {false, false, false};
	boolean[] _handlesLabels = {false, false, false};
	

	// Let the framework block additions made outside of this class
	ArrayList<String> _preventPropertiesCreate = new ArrayList<String>();
	ArrayList<String> _preventLabelsCreate = new ArrayList<String>();

	// Let the framework block changes made outside of this class
	ArrayList<String> _preventPropertiesModify = new ArrayList<String>();
	ArrayList<String> _preventLabelsModify = new ArrayList<String>();  // Does this belong here? Is it something other than a Delete and Add in the same change? 
	
	// Let the framework block deletes made outside of this class
	ArrayList<String> _preventPropertiesDelete = new ArrayList<String>();
	ArrayList<String> _preventLabelsDelete = new ArrayList<String>();

	// Let the framework update an index for you?  - A list of labels for each property in another list?
	// TODO: There has to be something more elegant than this
	ArrayList<String> _indexLabels = new ArrayList<String>();
	ArrayList<String> _indexProperties = new ArrayList<String>();


	// Export what properties and labels this class modifies, use empty if dynamically defined
	ArrayList<String> _modifiesProperties = new ArrayList<String>();
	ArrayList<String> _modifiesLabels = new ArrayList<String>();
	
	// Provide what properties and labels this class wants to be notified on, used for "smart" calling
	// The trigger will only be called "once" for any change in the entire set of listed filters
	ArrayList<String> _whenPropertiesChange = new ArrayList<String>();
	ArrayList<String> _whenLabelsChange = new ArrayList<String>();
			
	public void TriggerImpl() { 		
	}
	
	// OnEvent - fires on any event that is handled
	public boolean OnChange(EVENT eventType, Change<NodeOrRelationship> _changes) {
		return true;
	}
	public boolean OnChange(EVENT eventType, NodeOrRelationship _before, NodeOrRelationship _after) {
		return true;
	}
	
	// OnCreate - called when a new graph entity is created and if the node/rel has the members from the "whenXXXChange" list  
	public boolean OnCreate(Collection<NodeOrRelationship> _created) {
		return true;
	}
	public boolean OnCreate(NodeOrRelationship _created) {
		return true;
	}
	
	// OnChange - called whenever a graph entity changes and only if the values on the "whenXXXChange" list have actually changed   
	public boolean OnModify(Change<NodeOrRelationship> _changes, List<String[]> changedProperties, List<String[]> deletedProperties, List<String[]> changedLabels) {
		return true;
	}
	public boolean OnModify(NodeOrRelationship _before, NodeOrRelationship _after, List<String[]> changedProperties, List<String[]> deletedProperties, List<String[]> changedLabels) {
		return true;
	}
	
	// OnDelete - called whenever a graph entity is deleted and only if the values on the "whenXXXChange" list have actually changed   
	public boolean OnDelete(Collection<NodeOrRelationship> _deleted) {
		return true;
	}
	public boolean OnDelete(NodeOrRelationship _deleted) {
		return true;
	}
}
*/

    
    
/*
public class TriggerUuidProperty extends TriggerImplementation 
{
	static final String propName = "uuid";
	
	public TriggerUuidProperty() {
		// Enable Create and Update Property events
		_handlesProperties[EVENT.CREATE.getValue()] =  true;
		_handlesPerEntity = true;
		
		// Turn on some Framework features
		//_preventPropertyCreate.add(propName); // Maybe we want to allow end users to set their own UUID?
    	_preventPropertiesModify.add(propName);
    	_preventPropertiesDelete.add(propName);
    	_indexLabels.add("UUID");
    	_indexProperties.add(propName);
    	
    	// Inform the engine about this trigger
    	_modifiesProperties.add(propName);
	}
	
    public boolean OnCreate(NodeOrRelationship _created) {
        if (!_created.hasProperty(uuidConfiguration.getUuidProperty())) {
            String uuid = uuidGenerator.generateUuid();
            _created.setProperty(uuidConfiguration.getUuidProperty(), uuid);
        }
    	return true;
    }
	
}

public class TriggerRevisionIdProperty extends TriggerImplementation 
{
	static final String revPropName = "_rev";
	
	public TriggerRevisionIdProperty() {
		// Enable Create and Update Property events
		_handlesPerEntity = true;
		_handlesProperties[EVENT.CREATE.getValue()] =  true;
		_handlesProperties[EVENT.UPDATE.getValue()] =  true;

    	// Inform the engine about this trigger
    	_modifiesProperties.add(revPropName);
		
		// Turn on some Framework features
		_preventPropertiesCreate.add(revPropName);
    	_preventPropertiesModify.add(revPropName);
    	_preventPropertiesDelete.add(revPropName);
	}
	
    public boolean OnChange(EVENT eventType, NodeOrRelationship _before, NodeOrRelationship _after) {
    	if (eventType != EVENT.DELETE) {
    		assignRev(_after);
    	}
    	return true;
    }
*/
}

