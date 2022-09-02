package pappi.computers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import es.us.isa.ppinot.evaluation.Measure;
import es.us.isa.ppinot.evaluation.MeasureInstance;
import es.us.isa.ppinot.evaluation.MeasureScope;
import es.us.isa.ppinot.evaluation.TemporalMeasureScope;
import es.us.isa.ppinot.evaluation.computers.MeasureComputer;
import es.us.isa.ppinot.evaluation.computers.MeasureComputerFactory;
import es.us.isa.ppinot.evaluation.logs.LogEntry;
import es.us.isa.ppinot.evaluation.scopes.ScopeClassifier;
import es.us.isa.ppinot.evaluation.scopes.ScopeClassifierFactory;
import es.us.isa.ppinot.model.MeasureDefinition;
import es.us.isa.ppinot.model.ProcessInstanceFilter;
import es.us.isa.ppinot.model.aggregated.AggregatedMeasure;
import es.us.isa.ppinot.model.base.BaseMeasure;
import es.us.isa.ppinot.model.base.DataMeasure;
import es.us.isa.ppinot.model.derived.DerivedMeasure;
import pappi.aggregators.PrivacyAwareAggregator;
import pappi.computers.distributions.DistributionMediator;
import pappi.measureDefinitions.PrivacyAwareAggregatedMeasure;
import pappi.measureDefinitions.PrivacyAwareDerivedMultiInstanceMeasure;
import pappi.util.FileHandler;
import pappi.measureDefinitions.OptimizablePrivacyAwareMeasure;


public class OptimizablePrivacyAwareMeasureComputer implements MeasureComputer, NoiseDistribution{

    private PrivacyAwareAggregatedMeasure definition;
    private MeasureComputer baseComputer;
    private MeasureComputer filterComputer;
    private ScopeClassifier classifier;
    private PrivacyAwareAggregator agg;
    private final List<MeasureComputer> listGroupByMeasureComputer;
    
    ProcessInstanceFilter filter;
        
	public OptimizablePrivacyAwareMeasureComputer(MeasureDefinition definition, ProcessInstanceFilter filter) {
		if (!(definition instanceof AggregatedMeasure)) {
            throw new IllegalArgumentException();
        }
		
		this.filter = filter;

        this.definition = (OptimizablePrivacyAwareMeasure) definition;

        this.listGroupByMeasureComputer = new ArrayList<MeasureComputer>();
        final MeasureComputerFactory measureComputerFactory = new PrivacyAwareMeasureComputerFactory();
        if (this.definition.getGroupedBy() != null) {	
            for (DataMeasure dm : this.definition.getGroupedBy()) {
                listGroupByMeasureComputer.add(measureComputerFactory.create(dm, filter));
            }
        }
                
        this.baseComputer = measureComputerFactory.create(this.definition.getBaseMeasure(), filter);
        if (this.definition.getFilter() != null) {
            this.filterComputer = measureComputerFactory.create(this.definition.getFilter(), filter);
        }
        this.classifier = new ScopeClassifierFactory().create(filter, this.definition.getPeriodReferencePoint());
        this.agg = new PrivacyAwareAggregator(this.definition.getAggregationFunction());
	}
	

	public void update(LogEntry entry) {
        classifier.update(entry);
        for (MeasureComputer c : listGroupByMeasureComputer) {
            c.update(entry);
        }
        baseComputer.update(entry);
        if (filterComputer != null) {
            filterComputer.update(entry);
        }
    }

	
    public List<? extends Measure> compute(){
    	System.out.println("===== "+this.definition.getId()+" =====\n");
        //get admissible function sets
        List<Map<MeasureDefinition, String>> functionSets = new ArrayList();
        functionSets = getFunctionSets(this.definition);
        //get true result, and calculate difference to that instead of using stddev
        Map<MeasureDefinition, String> trueResultFunctionSet = getUnprivatizedFunctions(functionSets);
    	setFunctions(this.definition, trueResultFunctionSet, this.definition.getEpsilon());
    	List<? extends Measure> trueResults = baseComputer.compute();
    	
        functionSets = filterAdmissibleFunctionSets(this.definition, functionSets);
    	System.out.println("=== ADMISSIBLE FUNCTION SETS ===");
        for (Map<MeasureDefinition, String> functionset : functionSets) {
        	System.out.println(functionset.toString());
        	//System.out.println("Found it!");
        }
        System.out.println();
        
        //simulation - for each admissible function set, get the pdf of the output range
        Collection<? extends Measure> measures;
        int noTimeScopes = 0;
        
        Map<Map<MeasureDefinition, String>, List<? extends Measure>> outputDistributions = new HashMap();
        Map<Map<MeasureDefinition, String>, List<? extends Measure>> sampleOutputPerFunctionSet = new HashMap();
        
        
        for(Map<MeasureDefinition, String> admissibleFunction : functionSets) {
        	System.out.println("=== FUNCTION SET ANALYSIS ===");
        	System.out.println(admissibleFunction+"\n");

        	setFunctions(this.definition, admissibleFunction, this.definition.getEpsilon());
        	
        	System.out.println("FUNCTION TREE TRAVERSAL");
        	List<? extends Measure> outputDistribution = getMeasureDistributions();
        	
        	List<? extends Measure> distributionResult = baseComputer.compute();
        	//System.out.println("\nOUTPUT DISTRIBUTIONS FOR ALL TIMESCOPES");
        	//for (Measure m : outputDistribution) {
        		//System.out.println(m.getValueAsObject().toString());	
        	//}
        	System.out.println();
        	
        	noTimeScopes = distributionResult.size();
        	outputDistributions.put(admissibleFunction, outputDistribution);
        	sampleOutputPerFunctionSet.put(admissibleFunction, distributionResult);
        }
        
        System.out.println("=== RMSE OF DISTRIBUTIONS PER TIMESCOPE ===");
                        
        List<Measure> chosenMeasures = new ArrayList();
        
        
        Collection<MeasureScope> allScopes = classifier.listScopes(definition.isIncludeUnfinished());
        /*
        for (MeasureScope m : allScopes) {
        	System.out.println("TIME: "+((TemporalMeasureScope)m).getStart());
        }
        */
        
        //for (int i=0; i < noTimeScopes; i++) {
        for (MeasureScope m : allScopes) {        	
        	String from = ((TemporalMeasureScope)m).getStart().toString();
        	String to = ((TemporalMeasureScope)m).getEnd().toString();
        	//TODO print number of instances
    		Double trueValueAtScope=0.0;// = (Map<Double, Double>) functionSetMeasures.get(i).getValueAsObject();
    		Integer n = 0;

    		for (Measure t : trueResults) {
    			if (((TemporalMeasureScope)t.getMeasureScope()).getStart().toString().equals(from)) {
    				trueValueAtScope = t.getValue();
    				n = t.getInstances().size();
    				System.out.println(((TemporalMeasureScope)m).getStart().toString()+" (true value: "+trueValueAtScope+")");
    			}
    		}
        	
        	Map currentBestFunctionSet = null;
        	Measure currentBest = null;
        	Double minrmse = Double.MAX_VALUE;
        	
        	for (Map functionSet : outputDistributions.keySet()) {
        		String functions = functionSet.values().toString();
        		
        		List<? extends Measure> functionSetMeasures = outputDistributions.get(functionSet);        		
        		Map<Double,Double> distributionAtScope=new HashMap();// = (Map<Double, Double>) functionSetMeasures.get(i).getValueAsObject();
        		for (Measure t : functionSetMeasures) {
        			if (((TemporalMeasureScope)t.getMeasureScope()).getStart().toString().equals(from)) {
        				distributionAtScope = (Map<Double,Double>) t.getValueAsObject();
        			}
        		}
        		
        		Measure sampledValueAtScope = null;
        		List<? extends Measure> sampledOutputMeasures = sampleOutputPerFunctionSet.get(functionSet);
        		//double sampledOutput = sampledOutputMeasures.get(i).getValue();
        		for (Measure t : sampledOutputMeasures) {
        			if (((TemporalMeasureScope)t.getMeasureScope()).getStart().toString().equals(from)) {
        				sampledValueAtScope = t;
        			}
        		}

        		
        		Double rmse = rmse(distributionAtScope, trueValueAtScope);
        		Double stddev = stddev(distributionAtScope);
        		Double mean = mean(distributionAtScope);
        		System.out.println(rmse + " \t---\t (" + functionSet + ")");
        		
        		if (rmse<minrmse) {
        			minrmse = rmse;
        			currentBest = sampledValueAtScope;
        			currentBestFunctionSet = functionSet;
        		}
        		
        		if (((OptimizablePrivacyAwareMeasure)this.definition).writeToFile) {
        			double min= Double.NaN;
        			double max = Double.NaN;
        			double delta = Double.NaN;
        			
                	if (distributionAtScope.keySet().size() > 1) {
                		
                		min = Collections.min(distributionAtScope.keySet());
            			max = Collections.max(distributionAtScope.keySet());
            		//System.out.println(min +" <-> "+ max);
            		//double stepSize = (max-min)/Math.min(100, distributionAtScope.size());
            			delta = (max-min)/(100.0);
                	}
	        		for (Double key : distributionAtScope.keySet()) {

	        			if (currentBest== null) {
	        				((OptimizablePrivacyAwareMeasure)this.definition).getFileHandler("distributions").writeLine(String.join(";", 
		        					this.definition.getId(),
		        					String.valueOf(this.definition.getEpsilon()),
		        					from,
		        					to,
		        					key.toString(), 
		        					String.valueOf(0.0),
		        					String.valueOf(delta),
		        					String.valueOf(Double.NaN),
		        					n.toString(),
		        					String.valueOf(Double.NaN),
		        					String.valueOf(Double.NaN),
		        					String.valueOf(Double.NaN),
		        					trueValueAtScope.toString(),
		        					functionSet.values().toString()));
	        			}
		        			else {
		        			//List< String > list = new ArrayList< String >(functionSet.values());
		        			//Collections.sort(list);
		        			((OptimizablePrivacyAwareMeasure)this.definition).getFileHandler("distributions").writeLine(String.join(";", 
		        					this.definition.getId(),
		        					String.valueOf(this.definition.getEpsilon()),
		        					from,
		        					to,
		        					key.toString(), 
		        					distributionAtScope.get(key).toString(),
		        					String.valueOf(delta),
		        					String.valueOf(currentBest.getValue()),
		        					n.toString(),
		        					mean.toString(),
		        					stddev.toString(),
		        					rmse.toString(),
		        					trueValueAtScope.toString(),
		        					functionSet.values().toString()));
		        		}
	        		}
        		}
        	}
    		chosenMeasures.add(currentBest);
    		System.out.println("Optimal (" + minrmse + ", " + currentBest.getValue() + "): " + currentBestFunctionSet.toString());
        	System.out.println();
        }
		System.out.println("=== OUTPUT VALUES ===");
		for (Measure m : chosenMeasures)
			System.out.println(((TemporalMeasureScope)m.getMeasureScope()).getStart() + " - " + m.getValueAsObject().toString());
		System.out.println("");
        return chosenMeasures;
    }


	//TODO get rid of return null
    //remove dependency on "lap" and "exp" in values
    private Map<MeasureDefinition, String> getUnprivatizedFunctions(List<Map<MeasureDefinition, String>> functionSets) {
		for (Map<MeasureDefinition, String> functionSet : functionSets) {
			boolean priv = false;
			for(String function : functionSet.values()) {
				if (function.contains("lap") || function.contains("exp") || function.contains("SubsampleAggregate")) {
					priv = true;
				}
			}
			if (!priv) {
				return functionSet;
			}
		}
		return null;
	}
    
    
    private Double rmse(Map<Double, Double> distribution, Double trueValueAtScope) {
    	//System.out.println(distribution.keySet().size());
    	if (distribution.keySet().size() < 1) {
    		return Double.POSITIVE_INFINITY;
    	}
		double sum = 0.0;
		for (double key : distribution.keySet()) {
			sum += distribution.get(key) * Math.pow(key -trueValueAtScope,2);
		}
		return Math.sqrt(sum);
	}
    
    
    public double mean(Map<Double,Double> distribution) {
    	if (distribution.size() < 1) {
    		return Double.POSITIVE_INFINITY;
    	}
    	//mean
    	double mean= 0.0;
    	for (Double key : distribution.keySet()) {
    		mean += key * distribution.get(key);
    	}
    	//System.out.println("MEAN: "+mean);
    	return mean;// * (1.0/(double)distribution.size());
    }
    
    
    public double stddev(Map<Double,Double> distribution) {
    	if (distribution.size() < 1) {
    		return Double.POSITIVE_INFINITY;
    	}
    	double sum = 0.0;
    	for (Double v : distribution.values()) {
    		sum += v ;
    	}
    	
    	double mean = mean(distribution);
    	double var= 0.0;
    	for (Double key : distribution.keySet()) {
    		var += Math.pow(mean - key,2) * distribution.get(key);
    	}
    	
    	return Math.sqrt(var);
    }
    
    
    public List<? extends Measure> getMeasureDistributions() {
    	List<? extends Measure> result = DistributionMediator.retrieve(baseComputer);
    	//TODO find a general way of doing this, idea: first get distribution of all function sets, and discretize based on largest output interval!
		//if (("ReturnedIn28Days,TimeUntilAntibiotics<1h,TimeUntilLactidAcid<3h").contains(this.definition.getId())) {
			/*for (Measure m : result) {
		    	if (((Map<Double,Double>)m.getValueAsObject()).keySet().size()<=1)
		    		continue;
				Map<Double, Double> discretized = new HashMap();
				
	    		Map<Double, Double> cdf = new HashMap();
	    		Map<Double, Double> currentM = (Map<Double,Double>)m.getValueAsObject();
	    		Set keys = currentM.keySet();
	    		TreeSet<Double> t = new TreeSet();
	    		t.addAll(keys);

	    		double sum = 0.0;
	    		while (!t.isEmpty()) {
	    			Double currentKey = t.pollFirst();
	    			sum += currentM.get(currentKey);
	    			cdf.put(currentKey, sum);
	    		}
	    		//System.out.println(Collections.max(currentM.values()));
	    		
	    		double delta = .5;
	    		t = new TreeSet();
	    		t.addAll(currentM.keySet());
	    		
    			double startKey = t.pollFirst();
    			double z = currentM.get(startKey);
	    		while(!t.isEmpty()) {
	    			double currentDist= 0.0;
	    			double endKey = startKey;
	    			double currentDistprob = 0.0;
	    			while (currentDist< delta && !t.isEmpty()) {
		    			endKey = t.pollFirst();
		    			currentDistprob += currentM.get(endKey);
		    			currentDist = endKey - startKey;
		    		}
	    			//discretized.put(startKey+(endKey-startKey)/2.0, currentDistprob);
	    			discretized.put(endKey, currentDistprob);
	    			startKey = endKey;
	    		}
	    		//System.out.println("KEYS: "+discretized.entrySet());
	    		//System.out.println("KEYS: "+discretized.entrySet());
	    		///System.out.println(discretized.entrySet());
	    		sum = 0.0;
	    		for (double v : discretized.values())
	    			sum += v;
	    		///System.out.println(Collections.max(discretized.values()) + " " + sum);
	    		
	    		for (double key : discretized.keySet())
	    			discretized.put(key, discretized.get(key)/sum);
	    		
	    		sum = 0.0;
	    		for (double v : discretized.values())
	    			sum += v;
	    		//System.out.println("PROBSUM - "+sum);
	    		//System.out.println("KEYS: "+discretized.entrySet());

	    		//System.out.println(Collections.max(discretized.values()) + " " + sum);
	    		m.setValue(discretized);
	    	}*/
    	//}
    	return result;
    } 


	private void setFunctions(MeasureDefinition definition, Map<MeasureDefinition, String> functionSet, double epsilon) {
    	if (definition instanceof BaseMeasure) {
    		return;
    	}
		else if (definition instanceof OptimizablePrivacyAwareMeasure) {
			setFunctions(((OptimizablePrivacyAwareMeasure) definition).getBaseMeasure(), functionSet, epsilon);
		}
		else if (definition instanceof PrivacyAwareDerivedMultiInstanceMeasure) {
			((PrivacyAwareDerivedMultiInstanceMeasure)definition).setEpsilon(epsilon);
			((PrivacyAwareDerivedMultiInstanceMeasure)definition).setMode(functionSet.get(definition));
    		for (MeasureDefinition child : ((DerivedMeasure) definition).getUsedMeasureMap().values()) {
    			setFunctions(child, functionSet, epsilon);
    		}
		}
		else if (definition instanceof DerivedMeasure) {
    		for (MeasureDefinition child : ((DerivedMeasure) definition).getUsedMeasureMap().values()) {
    			setFunctions(child, functionSet, epsilon);
    		}
		}
		else {
			((AggregatedMeasure)definition).setAggregationFunction(functionSet.get(definition));
			((PrivacyAwareAggregatedMeasure)definition).setEpsilon(epsilon);
			setFunctions(((AggregatedMeasure)definition).getBaseMeasure(), functionSet, epsilon);
		}
	}


	private List<Map<MeasureDefinition, String>> getFunctionSets(MeasureDefinition definition) {
		List<Map<MeasureDefinition,String>> functionList = new ArrayList();
		List<Map<MeasureDefinition, String>> childFunctions = new ArrayList();
    	if (definition instanceof BaseMeasure) {
    		Map<MeasureDefinition,String> emptyMap= new HashMap();
    		functionList.add(emptyMap);
    	}
    	
		else if (definition instanceof OptimizablePrivacyAwareMeasure) {
			return getFunctionSets(((OptimizablePrivacyAwareMeasure) definition).getBaseMeasure());
		}
		
    	else if (definition instanceof DerivedMeasure) {
    		if (((DerivedMeasure) definition).getUsedMeasureMap().size()>1) {
				Map<MeasureDefinition, String> functionMap = new HashMap();
				functionMap.put(definition, "SubsampleAggregate");
				functionList.add(functionMap);
				
				functionMap = new HashMap();
				functionMap.put(definition, "Standard");
				functionList.add(functionMap);
    		}
    		else {
    			functionList.add(new HashMap());
    		}
    		
    		for (MeasureDefinition child : ((DerivedMeasure) definition).getUsedMeasureMap().values()) {
    			childFunctions = getFunctionSets(child);
    			functionList = join(functionList, childFunctions);
    		}
    	}
    	
    	else if (definition instanceof AggregatedMeasure) {
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
	
	
    private List<Map<MeasureDefinition, String>> filterAdmissibleFunctionSets(MeasureDefinition definition,
			List<Map<MeasureDefinition, String>> functionSets) {
    	List<Map<MeasureDefinition, String>> admissibleFunctionSets = new ArrayList();
		for (Map functionSet: functionSets) {
			if (isAdmissible(functionSet, definition, false)) {
				admissibleFunctionSets.add(functionSet);
			}
		}
		return admissibleFunctionSets;
	}

    
    //TODO clean up and into own class
    private boolean isAdmissible(Map<MeasureDefinition, String> functionSet, MeasureDefinition definition, boolean treePrivatized) {
    	if (definition instanceof BaseMeasure) {
    		return treePrivatized;
    	}
		else if (definition instanceof OptimizablePrivacyAwareMeasure) {
			return isAdmissible(functionSet ,((OptimizablePrivacyAwareMeasure) definition).getBaseMeasure(), treePrivatized);
		}
    	
		else if (definition instanceof PrivacyAwareDerivedMultiInstanceMeasure) {
			((PrivacyAwareDerivedMultiInstanceMeasure)definition).setMode(functionSet.get(definition));
			
			if (((PrivacyAwareDerivedMultiInstanceMeasure) definition).mode.equals(PrivacyAwareDerivedMultiInstanceMeasure.SUBSAMPLE)) {
				if (treePrivatized) {
					return false;
				}
				else {
		    		for (MeasureDefinition child : ((DerivedMeasure) definition).getUsedMeasureMap().values()) {
		    			if (!isAdmissible(functionSet, child, true)) {
		    				return false;
		    			}
		    		}
		    		return true;
				}
			}
			else {
	    		for (MeasureDefinition child : ((DerivedMeasure) definition).getUsedMeasureMap().values()) {
	    			if (!isAdmissible(functionSet, child, treePrivatized)) {
	    				return false;
	    			}
	    		}
				return true;
			}
		}
    	
		else if (definition instanceof DerivedMeasure) {
    		for (MeasureDefinition child : ((DerivedMeasure) definition).getUsedMeasureMap().values()) {
    			if (!isAdmissible(functionSet, child, treePrivatized)) {
    				return false;
    			}
    		}
			return true;
    	}
    	
		else {
			((AggregatedMeasure)definition).setAggregationFunction(functionSet.get(definition));
			
			if (((AggregatedMeasure) definition).getAggregationFunction().contains("lap") || ((AggregatedMeasure) definition).getAggregationFunction().contains("exp")) {
				if (treePrivatized) {
					return false;
				}
				else {
					return isAdmissible(functionSet, ((AggregatedMeasure) definition).getBaseMeasure(), true);
				}
			}
			else {
				return isAdmissible(functionSet, ((AggregatedMeasure) definition).getBaseMeasure(), treePrivatized);
			}
		}
	}


	private List<Map<MeasureDefinition, String>> join(List<Map<MeasureDefinition, String>> first,List<Map<MeasureDefinition, String>> second){
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
    
    
    private Collection<Double> chooseMeasuresToAggregate(MeasureScope scope, Map<String, MeasureInstance> measureMap) {
        Collection<Double> toAggregate = new ArrayList<Double>();
        for (String instance : scope.getInstances()) {
            if (measureMap.containsKey(instance)) {
                toAggregate.add(measureMap.get(instance).getValue());
            }
        }
        return toAggregate;
    }

    
    private Map<String, MeasureInstance> buildMeasureMap(Collection<? extends Measure> measures) {
    	return MeasureInstance.buildMeasureMap(measures);
    }

    
    // Filter instances whose filter value is false
    private Collection<String> filterTrueInstances(Collection<String> instances, Map<String, MeasureInstance> filterMap) {
        Collection<String> found = new ArrayList<String>();
        if (filterMap.size() > 0) {
            for (String instance : instances) {
                if (filterMap.get(instance).getValueAsBoolean()) {
                    found.add(instance);
                }
            }
        }
        return found;
    }

}