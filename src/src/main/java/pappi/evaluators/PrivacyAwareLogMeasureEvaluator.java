package pappi.evaluators;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.scalified.tree.TraversalAction;
import com.scalified.tree.TreeNode;

//import apps.MeasureDefinitionTreeNode;
import es.us.isa.ppinot.evaluation.Measure;
import es.us.isa.ppinot.evaluation.TemporalMeasureScope;
import es.us.isa.ppinot.evaluation.computers.MeasureComputer;
import es.us.isa.ppinot.evaluation.evaluators.LogMeasureEvaluator;
import es.us.isa.ppinot.evaluation.logs.LogConfigurer;
import es.us.isa.ppinot.evaluation.logs.LogProvider;
import es.us.isa.ppinot.model.MeasureDefinition;
import es.us.isa.ppinot.model.ProcessInstanceFilter;
import pappi.computers.PrivacyAwareMeasureComputerFactory;
import pappi.util.FileHandler;

public class PrivacyAwareLogMeasureEvaluator extends LogMeasureEvaluator {

	    private static final Logger log = Logger.getLogger(PrivacyAwareLogMeasureEvaluator.class.getName());

	    private LogProvider logProvider;
	    private LogConfigurer configurer;
	    private PrivacyAwareMeasureComputerFactory factory;
	    
	    private boolean writeToCSV;

	    public PrivacyAwareLogMeasureEvaluator(LogProvider logProvider) {
	    	super(logProvider);
	        this.logProvider = logProvider;
	        this.factory = new PrivacyAwareMeasureComputerFactory();
	        this.writeToCSV = true;
	    }
	    
	    public PrivacyAwareLogMeasureEvaluator(LogProvider logProvider, boolean writeToCSV) {
	    	super(logProvider);
	        this.logProvider = logProvider;
	        this.factory = new PrivacyAwareMeasureComputerFactory();
	        this.writeToCSV = writeToCSV;
	    }

	    public List<Measure> eval(MeasureDefinition definition, ProcessInstanceFilter filter) {
	        return eval(Arrays.asList(definition), filter).get(definition);
	    }

	    public Map<MeasureDefinition, List<Measure>> eval(List<MeasureDefinition> definitions, ProcessInstanceFilter filter) {
	    	//date information for potential
	    	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd_HH:mm:ss");  
	    	LocalDateTime now = LocalDateTime.now();
	    	
	    	
	        LogProvider logToAnalyse;
	        Map<MeasureDefinition, List<Measure>> measures = new HashMap<MeasureDefinition, List<Measure>>();
	        Map<MeasureComputer, MeasureDefinition> computers = new HashMap<MeasureComputer, MeasureDefinition>();    
	        
	        if (configurer == null) {
	            logToAnalyse = logProvider;
	        } else {
	            logToAnalyse = configurer.configure(logProvider);
	        }

	        //List<List<MeasureDefinition>> sharedSubTrees = getSharedSubtrees(definitions);
	        
	        for (MeasureDefinition definition : definitions) {
	        	AdmissableChecker checker = new AdmissableChecker();
	        	if(!checker.isAdmissable(definition)) {
	        		log.info("Provided MeasureDefinition " + definition.getId() + " is not admissible! Stopping computation!\n");
	        		System.exit(-1);
	        	}

	        	
	            MeasureComputer computer = factory.create(definition, filter);
	            computers.put(computer, definition);
	            logToAnalyse.registerListener(computer);
	            measures.put(definition, new ArrayList<Measure>());
	        }

	        log.info("Processing log...");
	        logToAnalyse.processLog();

	        log.info("Computing measures...");
	        for (MeasureComputer computer : computers.keySet()) {
	            measures.get(computers.get(computer)).addAll(computer.compute());
	        }
	        
	        if(this.writeToCSV) {
	        	for(MeasureDefinition md : measures.keySet()) {
	        		List<Measure> results = measures.get(md);
	        		String id = md.getId();
	        		try {
						BufferedWriter writer = new BufferedWriter(new FileWriter("results/" + now + "_" + id + ".csv"));
						writer.write(String.join(";", "PPI", "from", "to", "value","\n"));
						for(Measure m : results) {
			        		TemporalMeasureScope tempScope = (TemporalMeasureScope) m.getMeasureScope();
			        		writer.write(String.join(";", id, 
			        				tempScope.getStart().toString(), 
			        				tempScope.getEnd().toString(),
			        				Double.toString(m.getValue()),
			        				"\n"));
						}
						writer.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

	        	}
	        }

	        return measures;
	    }
/*
		private Multimap<String, String> getSharedSubtrees(List<MeasureDefinitionTreeNode> PPITrees) {
	        //TODO Glenn: get sets of equivalent subtrees over all MeasureDefinitions in definition
			
			Multimap<String, String> subtreeMap = createMap(PPITrees);
			Multimap<String, String> sharedSubtreeMap = sharifySubtreeMap(subtreeMap);
			Multimap<String, String> minSubtreeMap = minSubtreeMap(sharedSubtreeMap);
			
			return minSubtreeMap;
		}
		*/
		
		
		/**
		 * @param PPITrees: a list of PPI trees, i.e the root nodes
		 * @return a multimap from cardinal form -> node id, for each node in each tree
		 */
		/*private Multimap<String, String> createMap(List<MeasureDefinitionTreeNode> PPITrees) {
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
		/*private Multimap<String, String> sharifySubtreeMap(Multimap<String, String> subtreeMap) {
			Multimap<String, String> sharedSubtreeMap = ArrayListMultimap.create();
			for(String key : subtreeMap.keySet()) {
				if (subtreeMap.get(key).size() > 1) {
					sharedSubtreeMap.putAll(key, subtreeMap.get(key));
				}
			}
			
			return sharedSubtreeMap;
		}*/
		
		
		
		/**
		 * @param sharedSubtreeMap: multimap containing only sharedSubtrees between the inital trees, i.e. cardinal forms with more than one entry
		 * @return the sanitized multimap, i.e. if a node and their parent were in the list, the parent is now removed
		 */
		private Multimap<String, String> minSubtreeMap(Multimap<String, String> sharedSubtreeMap) {
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
		}

	}

