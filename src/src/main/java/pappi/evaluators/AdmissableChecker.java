package pappi.evaluators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.us.isa.ppinot.model.MeasureDefinition;
import es.us.isa.ppinot.model.aggregated.AggregatedMeasure;
import es.us.isa.ppinot.model.base.BaseMeasure;
import es.us.isa.ppinot.model.derived.DerivedMeasure;
import pappi.measureDefinitions.OptimizablePrivacyAwareMeasure;
import pappi.measureDefinitions.PrivacyAwareDerivedMultiInstanceMeasure;

public class AdmissableChecker {
	
	public boolean isAdmissable(MeasureDefinition measure) {
		Map<MeasureDefinition, String> functionSet = getFunctionSet(measure);
		//System.out.println("The list contains " + functionSet.size() + " function sets");
		//System.out.println("The agg measure code is: " + ((AggregatedMeasure) measure).getAggregationFunction());
		//System.out.println("\t" + isAdmissible(functionSet, measure, false));
		return isAdmissible(functionSet, measure, false);
	}
	
	
	private boolean isAdmissible(Map<MeasureDefinition, String> functionSet, MeasureDefinition definition, boolean treePrivatized) {
    	// CHECK BASE MEASRUES
		if (definition instanceof BaseMeasure) {
    		return treePrivatized;
    	
		} else if (definition instanceof OptimizablePrivacyAwareMeasure) {
			return isAdmissible(functionSet ,((OptimizablePrivacyAwareMeasure) definition).getBaseMeasure(), treePrivatized);
		
		} else if (definition instanceof PrivacyAwareDerivedMultiInstanceMeasure) {
			((PrivacyAwareDerivedMultiInstanceMeasure)definition).setMode(functionSet.get(definition));
			
			if (((PrivacyAwareDerivedMultiInstanceMeasure) definition).mode.equals(PrivacyAwareDerivedMultiInstanceMeasure.SUBSAMPLE)) {
				if (treePrivatized) {
					return false;
				} else {
		    		for (MeasureDefinition child : ((DerivedMeasure) definition).getUsedMeasureMap().values()) {
		    			if (!isAdmissible(functionSet, child, true)) {
		    				return false;
		    			}
		    		}
		    		return true;
				}
			} else {
	    		for (MeasureDefinition child : ((DerivedMeasure) definition).getUsedMeasureMap().values()) {
	    			if (!isAdmissible(functionSet, child, treePrivatized)) {
	    				return false;
	    			}
	    		}
				return true;
			}
		// CHECK DEREIVED MEASURES
		} else if (definition instanceof DerivedMeasure) {
    		for (MeasureDefinition child : ((DerivedMeasure) definition).getUsedMeasureMap().values()) {
    			if (!isAdmissible(functionSet, child, treePrivatized)) {
    				return false;
    			}
    		}
			return true;
		// CHECK AGGGREGATED MEASURES	
    	} else {
			((AggregatedMeasure)definition).setAggregationFunction(functionSet.get(definition));
			
			if (((AggregatedMeasure) definition).getAggregationFunction().contains("lap") || ((AggregatedMeasure) definition).getAggregationFunction().contains("exp")) {
				if (treePrivatized) {
					return false;
				} else {
					return isAdmissible(functionSet, ((AggregatedMeasure) definition).getBaseMeasure(), true);
				}
			} else {
				return isAdmissible(functionSet, ((AggregatedMeasure) definition).getBaseMeasure(), treePrivatized);
			}
		}
	}
	
	
	private Map<MeasureDefinition, String> getFunctionSet(MeasureDefinition measure) {
		Map<MeasureDefinition,String> functionSet = new HashMap();
		Map<MeasureDefinition, String> childFunction = new HashMap();
		
		// CHECK BASE MEASURE
		if (measure instanceof BaseMeasure) {
    		return functionSet;  // return empty set
    	
    	// CHECK OPTIMIZABLE PRIVACY AWARE MEASURE
    	} else if (measure instanceof OptimizablePrivacyAwareMeasure) {
			return getFunctionSet( ((OptimizablePrivacyAwareMeasure) measure).getBaseMeasure() );
		
		// CHECK DERIVED MEASURE
		// TODO: check whicht string values to add to the map for derived measures
    	} else if (measure instanceof DerivedMeasure) {
    		
    		// DERIVED MEASURES WITH > 1
    		if ( ((DerivedMeasure) measure).getUsedMeasureMap().size() > 1 ) {
				Map<MeasureDefinition, String> functionMap = new HashMap();
				functionMap.put(measure, "SubsampleAggregate");
				//functionList.add(functionMap);

				
				functionMap = new HashMap();
				functionMap.put(measure, "Standard");
				//functionList.add(functionMap);
			
			// DERIVED MEASURE WITH <= 1 
    		} else {
    			//functionList.add(new HashMap());
    		}
    		
    		for (MeasureDefinition child : ((DerivedMeasure) measure).getUsedMeasureMap().values()) {
    			childFunction = getFunctionSet(child);
    			functionSet = join(functionSet, childFunction);
    		}
    	
    	// CHECK AGGREGATED MEASURE
    	} else if (measure instanceof AggregatedMeasure) {
    		functionSet.put(measure, ((AggregatedMeasure) measure).getAggregationFunction());
			childFunction = getFunctionSet(((AggregatedMeasure) measure).getBaseMeasure());
			functionSet = join(functionSet, childFunction);  // TODO: maybe here we need to create a list of all child functions, i.e. use getFunctionSets in the line above
    	}
		
		return functionSet;
	}
	
	
	private Map<MeasureDefinition, String> join(Map<MeasureDefinition, String> first, Map<MeasureDefinition, String> second){
    	Map<MeasureDefinition, String> mergedMaps = new HashMap();
    	mergedMaps.putAll(first);
    	mergedMaps.putAll(second);
    	return mergedMaps;
    }

	
	
	// THIS CODE IS USED TO CREATE THE TWO FUNCTIONS ABOVE FROM
	// DELETE AFTER FUCNTION CREATION IS FINISHED

	private List<Map<MeasureDefinition, String>> getFunctionSets(MeasureDefinition definition) {
		List<Map<MeasureDefinition,String>> functionList = new ArrayList();
		List<Map<MeasureDefinition, String>> childFunctions = new ArrayList();
    	
		if (definition instanceof BaseMeasure) {
    		Map<MeasureDefinition,String> emptyMap= new HashMap();
    		functionList.add(emptyMap);
    	} else if (definition instanceof OptimizablePrivacyAwareMeasure) {
			return getFunctionSets(((OptimizablePrivacyAwareMeasure) definition).getBaseMeasure());
		} else if (definition instanceof DerivedMeasure) {
    		if (((DerivedMeasure) definition).getUsedMeasureMap().size()>1) {
				Map<MeasureDefinition, String> functionMap = new HashMap();
				functionMap.put(definition, "SubsampleAggregate");
				functionList.add(functionMap);
				
				functionMap = new HashMap();
				functionMap.put(definition, "Standard");
				functionList.add(functionMap);
    		} else {
    			functionList.add(new HashMap());
    		}
    		
    		for (MeasureDefinition child : ((DerivedMeasure) definition).getUsedMeasureMap().values()) {
    			childFunctions = getFunctionSets(child);
    			functionList = join(functionList, childFunctions);
    		}
    	} else if (definition instanceof AggregatedMeasure) {
    		String[] options = new String[] {};
    		
    		if (((AggregatedMeasure) definition).getAggregationFunction().contains("Sum")) {
    			options = new String[]{"Sum_lap", "Sum_exp", "Sum"};
    		}
        	
    		if (((AggregatedMeasure) definition).getAggregationFunction().contains("Average")) {
    			options = new String[]{"Average_lap", "Average_exp", "Average"};
        	}
        	
    		if (((AggregatedMeasure) definition).getAggregationFunction().contains("Minimum")) {
    			options = new String[]{"Minimum_lap", "Minimum_exp", "Minimum"};
        	}
            
    		if (((AggregatedMeasure) definition).getAggregationFunction().contains("Maximum")) {
    			options = new String[]{"Maximum_lap", "Maximum_exp", "Maximum"};
            }
            
			for (String option : options) {
    				Map<MeasureDefinition, String> functionMap = new HashMap();
    				functionMap.put(definition, option);
    				functionList.add(functionMap);
			}
			childFunctions = getFunctionSets(((AggregatedMeasure) definition).getBaseMeasure());
			functionList = join(functionList, childFunctions);
    	}
		return functionList;
    }
	
	
	private List<Map<MeasureDefinition, String>> join(List<Map<MeasureDefinition, String>> first, List<Map<MeasureDefinition, String>> second){
    	List<Map<MeasureDefinition, String>> mergedMeasures = new ArrayList();
    	for (Map<MeasureDefinition, String> one : first) {
        	for (Map<MeasureDefinition, String> two : second) {
        		Map<MeasureDefinition, String> onetwo = new HashMap();
        		onetwo.putAll(one);
        		onetwo.putAll(two);
        		mergedMeasures.add(onetwo);
        	}    		
    	}
    	return mergedMeasures;
    }
}
