package pappi.computers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.geometry.euclidean.oned.Interval;
import org.mvel2.MVEL;

import com.google.common.collect.Iterables;

import es.us.isa.ppinot.evaluation.Aggregator;
import es.us.isa.ppinot.evaluation.Measure;
import es.us.isa.ppinot.evaluation.MeasureInstance;
import es.us.isa.ppinot.evaluation.MeasureScope;
import es.us.isa.ppinot.evaluation.TemporalMeasureScope;
import es.us.isa.ppinot.evaluation.computers.AggregatedMeasureComputer;
import es.us.isa.ppinot.evaluation.computers.DerivedMeasureComputer;
import es.us.isa.ppinot.evaluation.computers.MeasureComputer;
import es.us.isa.ppinot.evaluation.computers.MeasureComputerFactory;
import es.us.isa.ppinot.evaluation.logs.LogEntry;
import es.us.isa.ppinot.model.MeasureDefinition;
import es.us.isa.ppinot.model.ProcessInstanceFilter;
import es.us.isa.ppinot.model.derived.DerivedMeasure;
import es.us.isa.ppinot.model.derived.DerivedSingleInstanceMeasure;
import pappi.aggregators.laplace.LaplaceAggregator;
import pappi.aggregators.laplace.LaplaceAggregatorDistribution;
import pappi.boundary.BoundaryEstimator;
import pappi.boundary.Bounds;
import pappi.computers.distributions.DistributionMediator;
import pappi.measureDefinitions.PrivacyAwareAggregatedMeasure;
import pappi.measureDefinitions.PrivacyAwareDerivedMultiInstanceMeasure;
/*
 * TODO make consistent with PPINOT 2.3-SNAPSHOT
 */
public class PrivacyAwareDerivedMeasureComputer extends DerivedMeasureComputer implements NoiseDistribution{
    private DerivedMeasure definition;
    private Map<String, MeasureComputer> computers;
    private Serializable expression;
    
    private int SUBSAMPLE_BUCKET_N = 10;

    public PrivacyAwareDerivedMeasureComputer(MeasureDefinition definition, ProcessInstanceFilter filter) {
    	super(definition, filter);
        if (!(definition instanceof DerivedMeasure)) {
            throw new IllegalArgumentException();
        }
        
        MeasureComputerFactory computerFactory = new PrivacyAwareMeasureComputerFactory();

        this.definition = (DerivedMeasure) definition;
        this.expression = MVEL.compileExpression(this.definition.getFunction());
        this.computers = new HashMap<String, MeasureComputer>();
        for (Map.Entry<String, MeasureDefinition> entry : this.definition.getUsedMeasureMap().entrySet()) {
            this.computers.put(entry.getKey(), computerFactory.create(entry.getValue(), filter));
        }
    }

    @Override
    public List<? extends Measure> compute() {
        List<Measure> results = new ArrayList<Measure>();
        Map<String, Map<MeasureScope, Measure>> variables = new HashMap<String, Map<MeasureScope, Measure>>();

        int size = Integer.MAX_VALUE;
        String oneVar = null;
        
        for (String varName : computers.keySet()) {
            Map<MeasureScope, Measure> measures = new HashMap<MeasureScope, Measure>();
            List<? extends Measure> compute = computers.get(varName).compute();
            for (Measure m : compute) {
                measures.put(m.getMeasureScope(), m);
            }

            variables.put(varName, measures);
            size = Math.min(size, compute.size());
            oneVar = varName;
        }

        if (oneVar == null) throw new RuntimeException("No variables defined");

        for (MeasureScope scope : variables.get(oneVar).keySet()) {
            Map<String, Object> expressionVariables = new HashMap<String, Object>();
            Map<String, Measure> expressionMeasures = new HashMap<String,Measure>();
            boolean ignoreScope = false;

            for (String varName : variables.keySet()) {
                Measure measure = variables.get(varName).get(scope);
                if (measure == null) {
                    ignoreScope = true;
                    break;
                }
                expressionVariables.put(varName, measure.getValueAsObject());
                expressionMeasures.put(varName, measure);
            }

            if (ignoreScope) {
                continue;
            }
        	//if (expressionVariables.keySet().size() > 1) {
        	//	System.out.println(((TemporalMeasureScope)scope).getStart()+ "- TRUE INPUTS: "+expressionVariables);
        	//}
            //dirty hack for specific function used in evaluation
            //TODO generalize this properly
            if (this.definition.getFunction().equals("a<b?(a/b)*100.0:100.0") && ((PrivacyAwareDerivedMultiInstanceMeasure)this.definition).getMode().equals(PrivacyAwareDerivedMultiInstanceMeasure.SUBSAMPLE)) {
            	Double value = computeSubsampleAggregate(scope);
            	results.add(buildMeasure(scope, value, expressionMeasures));
            }
            else {

            	Object value = MVEL.executeExpression(expression, expressionVariables);
            	results.add(buildMeasure(scope, value, expressionMeasures));
            }
        }

        return results;
    }
    
    
    
    /*
     * dirty hack to calculate subsample and aggregate her
     */
    	private Double computeSubsampleAggregate(MeasureScope scope) {
    		List<Measure> results = new ArrayList();
    		
            List<List<Double>> aPartitions = new ArrayList();
            for (int j=0;j<SUBSAMPLE_BUCKET_N;j++) {
            	aPartitions.add(new ArrayList());
            }

            List<? extends Measure> compute = null;

            
            for (String varName : computers.keySet()) {
            	
            	if (varName.equals("a")) {
    	            Collection<Double> instancesToAggregate = ((PrivacyAwareAggregatedMeasureComputer)computers.get(varName)).getInstancesToAggregate(scope);
		            
    	            if (SUBSAMPLE_BUCKET_N <= 1 || SUBSAMPLE_BUCKET_N > instancesToAggregate.size()/2) {
    	            	System.out.println("Number of buckets inappropriate for inputs!");
    	            	return Double.NaN;
    	            }
    	            
    	            
    	            
    	            int i = 0;
	                for(Double key : instancesToAggregate) {
	                	if (!key.isNaN()) {
			            	int bucket = i % SUBSAMPLE_BUCKET_N;
			            	aPartitions.get(bucket).add(key);
			            	i++;
	                	}
		            }
	                
	                List<Double> partitionResults = new ArrayList();
                	String baseFunction = ((PrivacyAwareAggregatedMeasure)((PrivacyAwareAggregatedMeasureComputer)computers.get(varName)).getDefinition()).getAggregationFunction();
                	Double epsilon = ((PrivacyAwareAggregatedMeasure)((PrivacyAwareAggregatedMeasureComputer)computers.get(varName)).getDefinition()).getEpsilon();
                	
                	//quick hack of sum aggregation
	                for (List<Double> part: aPartitions) {
	                	if(part.size()>0)
	                		partitionResults.add(((part.stream().reduce(0.0, (a, b) -> a + b))/part.size()) * 100.0);
	                	
	                	//call baseMeasure aggregation function
	                	//divide result by number of values, then times 100
	                	//put result into List
	                }
	                
	                //double sensitivity = partitionResults.size()/100.0;
	                //System.out.println("SENSITIVITY: "+sensitivity);
	                

	                double lowerBound = Collections.min(partitionResults);
	                double upperBound = Collections.max(partitionResults);
	        		/*if(upperBound == 0 && lowerBound == 0) {
	        			upperBound = 1;
	        		}
	        		if(upperBound == 1 && lowerBound == 1) {
	        			lowerBound = 0;
	        		}*/
	                if (upperBound == lowerBound) {
	                	return upperBound;
	                }
	                
	                
	                LaplaceAggregator privMean = new LaplaceAggregator();
	                double value = privMean.aggregate(LaplaceAggregatorDistribution.AVG_LAP, partitionResults, epsilon , lowerBound, upperBound);
	                return value;
	            }
	        }
            return null;
    	}     
    
    
    public List<? extends Measure> getMeasureDistributions() {
        List<Measure> results = new ArrayList<Measure>();
        Map<String, Map<MeasureScope, Measure>> variables = new HashMap<String, Map<MeasureScope, Measure>>();

        int size = Integer.MAX_VALUE;
        String oneVar = null;
        
        //dirty hack for specific function used in evaluation
        //TODO generalize this properly
        for (String varName : computers.keySet()) {
            Map<MeasureScope, Measure> measures = new HashMap<MeasureScope, Measure>();
            List<? extends Measure> compute = DistributionMediator.retrieve(computers.get(varName));

            
            for (Measure m : compute) {
                measures.put(m.getMeasureScope(), m);
            }

            variables.put(varName, measures);
            size = Math.min(size, compute.size());
            oneVar = varName;
        }
        System.out.println(this);

        if (oneVar == null) throw new RuntimeException("No variables defined");

        for (MeasureScope scope : variables.get(oneVar).keySet()) {
        	//Map<String, Object> expressionVariableDistributions = new HashMap<String, Object>();
        	
            Map<String, Object> expressionVariables = new HashMap<String, Object>();
            Map<String, Measure> expressionMeasures = new HashMap<String,Measure>();
            boolean ignoreScope = false;

            for (String varName : variables.keySet()) {

                Measure measure = variables.get(varName).get(scope);
                //System.out.println(varName + " : " + measure);

                if (measure == null) {
                    ignoreScope = true;
                    break;
                }
                expressionVariables.put(varName, measure.getValueAsObject());
                expressionMeasures.put(varName, measure);
            }

            if (ignoreScope) {
                continue;
            }
            
            //dirty hack for specific function used in evaluation
            //TODO generalize this properly
            if (this.definition.getFunction().equals("a<b?(a/b)*100.0:100.0") && ((PrivacyAwareDerivedMultiInstanceMeasure)this.definition).getMode().equals(PrivacyAwareDerivedMultiInstanceMeasure.SUBSAMPLE)) {
            	Map<Double, Double> dist = getSubsampleAggregateMeasureDistribution(scope);
            	/*if (dist.keySet().contains(Double.NaN)) {
            		results.add(buildMeasure(scope, dist, expressionMeasures));
            	}
	            else {
		            double lowerBound = Collections.min(dist.keySet());
		            double upperBound = Collections.max(dist.keySet());
		            
		            lowerBound = 0.0;
		            upperBound = 100.0;            
		            
	            	dist = convertToMap(dist, lowerBound, upperBound);
	            	//System.out.println("DIST: "+dist);
	            	//System.out.println(lowerBound + " - " + upperBound);
	            	results.add(buildMeasure(scope, dist, expressionMeasures));
	            }*/
            	results.add(buildMeasure(scope, dist, expressionMeasures));
            }
            else {
	            //expressionVariables contains for each variable a random variable
	            //for each combination of realisations of these random variables, execute the expression once
	            //the probability of each of these combinations is the product of their probabilites
	            //System.out.println(expressionVariables);
	            List<Map<String, Double>> realisations = joinMaps(expressionVariables);
	            Map<Double, Double> resultDistribution = new HashMap();
	            //System.out.println(expressionVariables);
	            for (Map<String, Double> realisation : realisations) {
	            	/*boolean containsNaN = false;
	            	for (Double value : realisation.values()) {
	            		if (value.isNaN())
	            			containsNaN = true;
	            	}
	            	if( containsNaN)
	            		continue;*/
	        		//System.out.println(realisation);
	            	
	            	double probability = getJointProbability(realisation, expressionVariables);
	                Object value = MVEL.executeExpression(expression, realisation);
	                //catch cases, where multiple inputs map to the same output
	                //System.out.println(value);
	                resultDistribution.put((Double)value, probability+resultDistribution.getOrDefault(value, 0.0));
	            }
	            
	            
	            double lowerBound = Collections.min(resultDistribution.keySet());
	            double upperBound = Collections.max(resultDistribution.keySet());

	            if (resultDistribution.size()>1)
	            	resultDistribution = convertToMap(resultDistribution, lowerBound, upperBound);
	            /*
	    		Double sum = 0.0;
	    		for (Double v : resultDistribution.values()) {
	    			sum += v;
	    		}
	            System.out.println("DERIVED-PROB: "+sum);
	            */
	            results.add(buildMeasure(scope, resultDistribution, expressionMeasures));
            }
        }
        //System.out.println(results);
        return results;
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
	
    
/*
 * dirty hack to calculate subsample and aggregate her
 * TODO bucket size must be larger than 1, and at most half the size ofn umber of inputs
 */
	Map<Double, Double> getSubsampleAggregateMeasureDistribution(MeasureScope scope) {
		List<Measure> results = new ArrayList();

        List<List<Double>> aPartitions = new ArrayList();
        for (int j=0;j<SUBSAMPLE_BUCKET_N;j++) {
        	aPartitions.add(new ArrayList());
        }

        List<? extends Measure> compute = null;
        
        for (String varName : computers.keySet()) {
        	if (varName.equals("a")) {	            
            //skip the aggregated measure and get the original values
            //compute = (List<? extends Measure>) ((PrivacyAwareDerivedMeasureComputer)((PrivacyAwareAggregatedMeasureComputer)computers.get(varName)).getComputer()).getMeasureDistributions();
            //for (Measure m : compute) {
	            Collection<Double> instancesToAggregate = ((PrivacyAwareAggregatedMeasureComputer)computers.get(varName)).getInstancesToAggregate(scope);
	            
	            if (SUBSAMPLE_BUCKET_N <= 1 || SUBSAMPLE_BUCKET_N > instancesToAggregate.size()/2) {
	            	System.out.println("Number of buckets inappropriate for inputs!");
	            	Map<Double, Double> nanMap = new HashMap<Double, Double>();
	            	nanMap.put(Double.NaN, 1.0);
	            	return nanMap;
	            }
	            
	            int i = 0;
                for(Double key : instancesToAggregate) {
                	if (!key.isNaN()) {
		            	int bucket = i % SUBSAMPLE_BUCKET_N;
		            	aPartitions.get(bucket).add(key);
		            	i++;
                	}
	            }
                //for (List<Double> bucket: aPartitions) {
                //	System.out.println("BUCKET SIZE: "+bucket.size());
                //}
                //System.out.println();
                
                List<Double> partitionResults = new ArrayList();
            	String baseFunction = ((PrivacyAwareAggregatedMeasure)((PrivacyAwareAggregatedMeasureComputer)computers.get(varName)).getDefinition()).getAggregationFunction();
            	Double epsilon = ((PrivacyAwareAggregatedMeasure)((PrivacyAwareAggregatedMeasureComputer)computers.get(varName)).getDefinition()).getEpsilon();
            	
            	//quick hack of sum aggregation
                for (List<Double> part: aPartitions) {
                	if(part.size()>0)
                		partitionResults.add(((part.stream().reduce(0.0, (a, b) -> a + b))/part.size()) * 100.0);
                	
                	//call baseMeasure aggregation function
                	//divide result by number of values, then times 100
                	//put result into List
                }
               // double sum= 0.0;
                //for (Double v: partitionResults) {
                //	System.out.println("PARTITION RESULT: "+v);
                //	sum += v;
                //}
                //System.out.println();
                
                //System.out.println("WHOLE MEAN: "+ sum/partitionResults.size());
                
                Aggregator agg = new Aggregator("Sum");
                //System.out.println("AGGREGATOR: "+agg.aggregate(instancesToAggregate)/instancesToAggregate.size());
                
                double lowerBound = Collections.min(partitionResults);
                double upperBound = Collections.max(partitionResults);
        		/*if(upperBound == 0 && lowerBound == 0) {
        			upperBound = 1;
        		}
        		if(upperBound == 1 && lowerBound == 1) {
        			lowerBound = 0;
        		}*/
                if(upperBound == lowerBound) {
                	Map oneVarMap = new HashMap();
                	oneVarMap.put(upperBound, 1.0);
                	return oneVarMap;
                }
                
                LaplaceAggregatorDistribution privMean = new LaplaceAggregatorDistribution();
                Map<Double,Double> outputDistribution = privMean.getOutputProbability(LaplaceAggregatorDistribution.AVG_LAP, partitionResults, epsilon , lowerBound, upperBound);
                return outputDistribution;

            }
        }
        return null;
	}

	private Map<Double, Double> convertToMap(Map<Double, Double> resultDistribution, Double lowerBound, Double upperBound) {
		int size =resultDistribution.size()/100;
		if (size<1) {
			size=1;
		}
		Map<Double,Double> discretized = new HashMap();
		
		TreeSet<Double> t = new TreeSet();
		t.addAll(resultDistribution.keySet());
		int i=0;
		while (!t.isEmpty()) {
			List<Double> keys = new ArrayList();
			List<Double> values = new ArrayList();
			for (int j=0;j<size;j++) {
				if(t.isEmpty())
					break;
				Double key = t.pollFirst();
				keys.add(key);
				values.add(resultDistribution.get(key));
			}
			Double keyMean = 0.0;
			for (Double k: keys) {
				keyMean += k;
			}
			keyMean = keyMean/keys.size();
			
			Double valuesMean = 0.0;
			for (Double v: values) {
				valuesMean += v;
			}
			valuesMean = valuesMean/values.size();
			
			discretized.put(keyMean, valuesMean);
		}
		
		Double sum = 0.0;
		for (Double value : discretized.values())
			sum += value;
		for (Double key : discretized.keySet())
			discretized.put(key, discretized.get(key)/sum);
		
		
		
		//System.out.println(discretized.size());
		/*double delta = (upperBound-lowerBound)/100.0;
		//System.out.println(lowerBound + " " + upperBound);
		Map<Double, Double> probabilities = new HashMap<Double, Double>();
		
		TreeSet<Double> orderedKeys = new TreeSet<Double>();
		orderedKeys.addAll(resultDistribution.keySet());
		
		Double currentKey = orderedKeys.pollFirst();
		Double x = currentKey;
		while (x<= upperBound && !orderedKeys.isEmpty()) {
			Double p = 0.0;
			while(currentKey < x+delta) {
				p += resultDistribution.get(currentKey);
				if (orderedKeys.isEmpty())
					break;
				currentKey = orderedKeys.pollFirst();
			}
			if (p> 0.0) {
				probabilities.put(x+ delta/2.0, p);
			}			
			x += delta;
		}
		//System.out.println(probabilities.size());
		
		//convert to proper probability distribution
		double sum = 0.0;
		for (Double prob : probabilities.values()) {
			sum+=prob;
		}
		
		for (Double key : probabilities.keySet()) {
			probabilities.put(key, probabilities.get(key)/sum);
			if (key<0)
				System.out.println(key);
		}
		return probabilities;*/
		return discretized;
	}

	private double getJointProbability(Map<String, Double> realisation, Map<String, Object> expressionVariables) {
		double probability = 1.0;
		for (String variableName : realisation.keySet()) {
			double value = realisation.get(variableName);
			//System.out.println("Searching for "+ value+ " in:");
			//System.out.println(expressionVariables);
			//System.out.println(realisation);
			probability = probability * ((Map<Double,Double>)expressionVariables.get(variableName)).get(value);
		}
		//System.out.println("Probability: "+probability);
		return probability;
	}

	private List<Map<String, Double>> joinMaps(Map<String, Object> expressionVariables) {
		//list of maps
		//for each map
			//merge map into list of maps
		//System.out.println("Joining maps");
		List<Map<String, Double>> joinedMaps = new ArrayList();
		joinedMaps.add(new HashMap());
		for (String variableName : expressionVariables.keySet()) {
			Map<Double, Double> probabilities = (Map<Double,Double>)expressionVariables.get(variableName);
			Collection<Double> values = probabilities.keySet();
			joinedMaps = mergeValuesIntoList(variableName, values, joinedMaps);
		}
		return joinedMaps;
	}

	private List<Map<String, Double>> mergeValuesIntoList(String variableName, Collection<Double> values, List<Map<String, Double>> mapList) {
		List<Map<String, Double>> mergedList = new ArrayList();
		for (Map map : mapList) {
			for (Double value : values) {
				Map mergedMap = new HashMap();
				mergedMap.putAll(map);
				mergedMap.put(variableName, value);
				mergedList.add(mergedMap);
			}
		}
		return mergedList;
	}

	private Measure buildMeasure(MeasureScope scope, Object value, Map<String, Measure> expressionVariables) {
        Measure measure;

        if (definition instanceof DerivedSingleInstanceMeasure) {
            measure = new MeasureInstance(definition, scope, value);
            if (true) {
            //TODO make consistent with PPINOT 2.3-SNAPSHOT - find out what commented out block actually does
            //if (definition.isIncludeEvidences()) {
                measure.addEvidence(((MeasureInstance) measure).getInstanceId(), expressionVariables);
            }
        } else {
            measure = new Measure(definition, scope, value);
            if (true) {
            //TODO make consistent with PPINOT 2.3-SNAPSHOT - find out what commented out block actually does
            //if (definition.isIncludeEvidences()) {
                measure.addEvidence(measure.getMeasureScope().getScopeInfo().toString(), expressionVariables);
            }
        }

        return measure;
    }


    @Override
    public void update(LogEntry entry) {
        for (MeasureComputer computer : computers.values()) {
            computer.update(entry);
        }
    }
}
