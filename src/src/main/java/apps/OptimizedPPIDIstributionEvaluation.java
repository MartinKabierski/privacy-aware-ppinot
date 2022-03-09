package apps;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import es.us.isa.ppinot.evaluation.Aggregator;
import es.us.isa.ppinot.evaluation.Measure;
import es.us.isa.ppinot.evaluation.TemporalMeasureScope;
import es.us.isa.ppinot.evaluation.evaluators.MeasureEvaluator;
import es.us.isa.ppinot.evaluation.logs.LogProvider;
import es.us.isa.ppinot.evaluation.logs.MXMLLog;
import es.us.isa.ppinot.model.DataContentSelection;
import es.us.isa.ppinot.model.MeasureDefinition;
import es.us.isa.ppinot.model.TimeUnit;
import es.us.isa.ppinot.model.aggregated.AggregatedMeasure;
import es.us.isa.ppinot.model.base.CountMeasure;
import es.us.isa.ppinot.model.base.DataMeasure;
import es.us.isa.ppinot.model.base.TimeMeasure;
import es.us.isa.ppinot.model.condition.TimeInstantCondition;
import es.us.isa.ppinot.model.derived.DerivedMeasure;
import es.us.isa.ppinot.model.derived.DerivedMultiInstanceMeasure;
import es.us.isa.ppinot.model.derived.DerivedSingleInstanceMeasure;
import es.us.isa.ppinot.model.scope.Period;
import es.us.isa.ppinot.model.scope.SimpleTimeFilter;
import es.us.isa.ppinot.model.state.GenericState;
import pappi.aggregators.PrivacyAwareAggregator;
import pappi.boundary.BoundaryEstimator;
import pappi.evaluators.PrivacyAwareLogMeasureEvaluator;
import pappi.measureDefinitions.OptimizablePrivacyAwareMeasure;
import pappi.measureDefinitions.PrivacyAwareAggregatedMeasure;
import pappi.util.FileHandler;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.scalified.tree.TraversalAction;
import com.scalified.tree.TreeNode;
import com.scalified.tree.multinode.ArrayMultiTreeNode;
//import apps.MeasureDefinitionTreeNode;

import apps.ppiDefinitions.SepsisPPIsOptimized;

public class OptimizedPPIDIstributionEvaluation {
	
	public static void main(String[] args) throws Exception {
		FileHandler fileHandler = new FileHandler("distributions.csv");
        fileHandler.writeLine(String.join(";","ppi","epsilon","from","to","x","y","stepsize","sampledvalue","n","mean","stddev","rmse","truevalue","functionset"));
		
		OptimizedPPIDIstributionEvaluation app = new OptimizedPPIDIstributionEvaluation();
		LogProvider log = new MXMLLog(new FileInputStream(new File("input/Sepsis Cases - Event Log.mxml")),null);
		List<MeasureDefinition> m = SepsisPPIsOptimized.buildAll(0.1);
		for (MeasureDefinition d : m)
			((OptimizablePrivacyAwareMeasure)d).addFileHandler("distributions",fileHandler);

		Map<MeasureDefinition, List<Measure>> measures = null;
		MeasureEvaluator evaluator = new PrivacyAwareLogMeasureEvaluator(log);
		measures = evaluator.eval(m, new SimpleTimeFilter(Period.MONTHLY,4, false, false));		
		
		fileHandler.closeFile();
	}
/*	


	private MeasureDefinitionTreeNode testSharedTree() {
		CountMeasure instanceCount=new CountMeasure();
		instanceCount.setId("7");
		instanceCount.setWhen(new TimeInstantCondition("Release A",GenericState.END));
		MeasureDefinitionTreeNode n1 = new MeasureDefinitionTreeNode(instanceCount);
		
	
	TimeMeasure returnTime=new TimeMeasure();
	returnTime.setFrom(new TimeInstantCondition("Release A", GenericState.END));
	returnTime.setTo(new TimeInstantCondition("Return ER", GenericState.END));
	returnTime.setUnitOfMeasure(TimeUnit.DAYS);
	
	DerivedSingleInstanceMeasure returnedIn28Days = new DerivedSingleInstanceMeasure();
	returnedIn28Days.setFunction("returnTime<28?1.0:0.0");
	returnedIn28Days.addUsedMeasure("returnTime", returnTime);
	
	PrivacyAwareAggregatedMeasure noOfReturnedPatients = new PrivacyAwareAggregatedMeasure();
	noOfReturnedPatients.setEpsilon(0.1);
	noOfReturnedPatients.setBaseMeasure(returnedIn28Days);
	noOfReturnedPatients.setBoundaryEstimation(BoundaryEstimator.MINMAX);
	noOfReturnedPatients.setAggregationFunction(PrivacyAwareAggregator.SUM);
	
	DerivedMultiInstanceMeasure percentage = new DerivedMultiInstanceMeasure();

	percentage.setFunction("a<b?(a/b)*100.0:100.0");
	percentage.addUsedMeasure("a", noOfReturnedPatients);
	percentage.addUsedMeasure("b", totalCases);

	OptimizablePrivacyAwareMeasure test = new OptimizablePrivacyAwareMeasure();
	test.setId("13");
	test.setBaseMeasure(percentage);
	test.setEpsilon(0.1);
	MeasureDefinitionTreeNode n7 = new MeasureDefinitionTreeNode(test);
	n7.add(n6);
	
	System.out.println("TREE 1:");
	System.out.println(n7.toString());
	
	return test;
	}

	
	private MeasureDefinitionTreeNode testSharedTree2() {
		CountMeasure instanceCount=new CountMeasure();
		instanceCount.setId("1");
		instanceCount.setWhen(new TimeInstantCondition("Release A",GenericState.END));
		MeasureDefinitionTreeNode n1 = new MeasureDefinitionTreeNode(instanceCount);
		
		
		PrivacyAwareAggregatedMeasure totalCases = new PrivacyAwareAggregatedMeasure();
		totalCases.setId("2");
		totalCases.setEpsilon(0.1);
		totalCases.setBaseMeasure(instanceCount);
		totalCases.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		totalCases.setAggregationFunction(PrivacyAwareAggregator.SUM);
		MeasureDefinitionTreeNode n2 = new MeasureDefinitionTreeNode(totalCases);
		n2.add(n1);
			
		
		TimeMeasure returnTime=new TimeMeasure();
		returnTime.setId("3");
		returnTime.setFrom(new TimeInstantCondition("Release A", GenericState.END));
		returnTime.setTo(new TimeInstantCondition("Return ER", GenericState.END));
		returnTime.setUnitOfMeasure(TimeUnit.DAYS);
		MeasureDefinitionTreeNode n3 = new MeasureDefinitionTreeNode(returnTime);
		
		
		DerivedSingleInstanceMeasure returnedIn28Days = new DerivedSingleInstanceMeasure();
		returnedIn28Days.setId("4");
		returnedIn28Days.setFunction("returnTime<28");
		returnedIn28Days.addUsedMeasure("returnTime", returnTime);
		MeasureDefinitionTreeNode n4 = new MeasureDefinitionTreeNode(returnedIn28Days);
		n4.add(n3);
		
		
		PrivacyAwareAggregatedMeasure noOfReturnedPatients = new PrivacyAwareAggregatedMeasure();
		noOfReturnedPatients.setId("5");
		noOfReturnedPatients.setEpsilon(0.1);
		noOfReturnedPatients.setBaseMeasure(returnedIn28Days);
		noOfReturnedPatients.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		noOfReturnedPatients.setAggregationFunction(PrivacyAwareAggregator.SUM);
		MeasureDefinitionTreeNode n5 = new MeasureDefinitionTreeNode(noOfReturnedPatients);
		n5.add(n4);
		
		
		DerivedMultiInstanceMeasure percentage = new DerivedMultiInstanceMeasure();
		percentage.setId("6");
		percentage.setFunction("(a/b)*100.0");
		percentage.addUsedMeasure("a", noOfReturnedPatients);
		percentage.addUsedMeasure("b", totalCases);
		MeasureDefinitionTreeNode n6 = new MeasureDefinitionTreeNode(percentage);
		n6.add(n5);
		n6.add(n2);
		
		System.out.println("TREE 2:");
		System.out.println(n6.toString());
		return n6;
		//return percentage;
	}
	
	
	public Multimap<String, String> getSharedSubtrees(List<MeasureDefinitionTreeNode> definitions) {
        //TODO Glenn: get sets of equivalent subtrees over all MeasureDefinitions in definition
		
		Multimap<String, String> subtreeMap = createMap(definitions);
		System.out.println(subtreeMap);
		Multimap<String, String> sharedSubtreeMap = sharifySubtreeMap(subtreeMap);
		System.out.println(sharedSubtreeMap);
		Multimap<String, String> minSubtreeMap = minSubtreeMap(sharedSubtreeMap);
		System.out.println(minSubtreeMap);
		
		return minSubtreeMap;
	}
	
	/**
	 * @param PPITrees: a list of PPI trees, i.e the root nodes
	 * @return a multimap from cardinal form -> node id, for each node in each tree
	 */
	/*
	public Multimap<String, String> createMap(List<MeasureDefinitionTreeNode> PPITrees) {
		final Multimap<String, String> subtreeMap = ArrayListMultimap.create();
		
		for (MeasureDefinitionTreeNode node : PPITrees) {
			// Creating traversal action
			TraversalAction<TreeNode<MeasureDefinition>> action = new TraversalAction<TreeNode<MeasureDefinition>>() {
				@Override
				public void perform(TreeNode<MeasureDefinition> node) {
					String key = ((MeasureDefinitionTreeNode) node).getCardinalForm();
					String value = node.data().getId();
					subtreeMap.put(key, value);
				}
				
				@Override
				public boolean isCompleted() {
				    return false; // return true in order to stop traversing
				}
			};
			node.traversePostOrder(action);
		}
		
		return subtreeMap;
	}*/
	
	
	/**
	 * @param subtreeMap: a multimap from cardinal form -> node id, for each node in each tree
	 * @return the subset of the multimap containing only sharedSubtrees between the inital trees, i.e. cardinal forms with more than one entry
	 */
	/*
	public Multimap<String, String> sharifySubtreeMap(Multimap<String, String> subtreeMap) {
		Multimap<String, String> sharedSubtreeMap = ArrayListMultimap.create();
		for(String key : subtreeMap.keySet()) {
			if (subtreeMap.get(key).size() > 1) {
				sharedSubtreeMap.putAll(key, subtreeMap.get(key));
			}
		}
		
		return sharedSubtreeMap;
	}
	*/
	
	/**
	 * @param sharedSubtreeMap: multimap containing only sharedSubtrees between the inital trees, i.e. cardinal forms with more than one entry
	 * @return the sanitized multimap, i.e. if a node and their parent were in the list, the parent is now removed
	 */
	/*
	public Multimap<String, String> minSubtreeMap(Multimap<String, String> sharedSubtreeMap) {
		// check if keys, i.e. nodes, are parents of other nodes
		
		List<String> keys2Remove = new ArrayList<>();
		for(String key1: sharedSubtreeMap.keySet()) {
			for(String key2: sharedSubtreeMap.keySet()) {
				if(key1.contains(key2) && key1 != key2) {
					keys2Remove.add(key2);
				}
			}
		}
		sharedSubtreeMap.keySet().removeAll(keys2Remove);
		return sharedSubtreeMap;
	}*/
	
}
