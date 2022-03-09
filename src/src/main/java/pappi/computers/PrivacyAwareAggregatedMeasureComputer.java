package pappi.computers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.us.isa.ppinot.evaluation.Measure;
import es.us.isa.ppinot.evaluation.MeasureInstance;
import es.us.isa.ppinot.evaluation.MeasureScope;
import es.us.isa.ppinot.evaluation.computers.MeasureComputer;
import es.us.isa.ppinot.evaluation.computers.MeasureComputerFactory;
import es.us.isa.ppinot.evaluation.logs.LogEntry;
import es.us.isa.ppinot.evaluation.scopes.ScopeClassifier;
import es.us.isa.ppinot.evaluation.scopes.ScopeClassifierFactory;
import es.us.isa.ppinot.model.MeasureDefinition;
import es.us.isa.ppinot.model.ProcessInstanceFilter;
import es.us.isa.ppinot.model.aggregated.AggregatedMeasure;
import es.us.isa.ppinot.model.base.DataMeasure;
import pappi.aggregators.PrivacyAwareAggregator;
import pappi.computers.distributions.DistributionMediator;
import pappi.measureDefinitions.PrivacyAwareAggregatedMeasure;
//TODO add correct parameter
public class PrivacyAwareAggregatedMeasureComputer implements MeasureComputer, NoiseDistribution{

    private PrivacyAwareAggregatedMeasure definition;
    private MeasureComputer baseComputer;
    private MeasureComputer filterComputer;
    private ScopeClassifier classifier;
    private PrivacyAwareAggregator agg;
    private final List<MeasureComputer> listGroupByMeasureComputer;
    
    
	public PrivacyAwareAggregatedMeasureComputer(MeasureDefinition definition, ProcessInstanceFilter filter) {
		if (!(definition instanceof AggregatedMeasure)) {
            throw new IllegalArgumentException();
        }

        this.definition = (PrivacyAwareAggregatedMeasure) definition;

        this.listGroupByMeasureComputer = new ArrayList<MeasureComputer>();
        final MeasureComputerFactory measureComputerFactory = new PrivacyAwareMeasureComputerFactory();
        if (this.definition.getGroupedBy() != null) {	
            for (DataMeasure dm : this.definition.getGroupedBy()) {
                listGroupByMeasureComputer.add(measureComputerFactory.create(dm, filter));
            }
        }

        //replace this with a simulatorfactory?
        this.baseComputer = measureComputerFactory.create(this.definition.getBaseMeasure(), filter);
        if (this.definition.getFilter() != null) {
            this.filterComputer = measureComputerFactory.create(this.definition.getFilter(), filter);
        }
        this.classifier = new ScopeClassifierFactory().create(filter, this.definition.getPeriodReferencePoint());
        this.agg = new PrivacyAwareAggregator(this.definition.getAggregationFunction());
	}
	
	//hack for derivedMeasures
	public MeasureComputer getComputer() {
		return this.baseComputer;
	}
	
	public MeasureDefinition getDefinition() {
		return this.definition;
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

    public List<? extends Measure> compute() {
        List<Measure> result = new ArrayList<Measure>();
        Collection<? extends Measure> measures = baseComputer.compute();
        List<? extends Measure> filters = new ArrayList<Measure>();
        if (filterComputer != null) {
            filters = filterComputer.compute();
        }

        Map<String, MeasureInstance> measureMap = buildMeasureMap(measures);
        Map<String, MeasureInstance> filterMap = buildMeasureMap(filters);
        Collection<MeasureScope> allScopes = null;

        if (listGroupByMeasureComputer != null && listGroupByMeasureComputer.size() > 0) {

        } else {
            allScopes = classifier.listScopes(definition.isIncludeUnfinished());
        }


        for (MeasureScope scope : allScopes) {
            if (filterComputer != null) {
                Collection<String> filterScopeInstances = filterTrueInstances(scope.getInstances(), filterMap);
                scope.getInstances().retainAll(filterScopeInstances);
            }

            Collection<Double> toAggregate = chooseMeasuresToAggregate(scope, measureMap);
            
            //detect if this is the root node, and if so have all the optimization be handled here :)
            
            if (!toAggregate.isEmpty()) {
                double val = this.agg.aggregate(
                		toAggregate,
                		definition.getAggregationFunction(),
                		definition.getBoundaryEstimation(),
                		definition.getEpsilon(), 
                		definition.getTarget(),
                		definition.getFalloff(),
                		definition.getExtensionFactor(),
                		definition.getMinimalSize()
                		);
                Measure measure = new Measure(definition, scope, val);
                result.add(measure);
                //TODO make consistent with PPINOT 2.3-SNAPSHOT - find out what commented out block actually does
                if (true) {
                //if (definition.isIncludeEvidences()) {
                    for (String instance : scope.getInstances()) {
                        Map<String, Measure> evidence = new HashMap<String, Measure>();
                        evidence.put("base", measureMap.get(instance));
                        if (filterComputer != null) {
                            evidence.put("filter", filterMap.get(instance));
                        }
                        measure.addEvidence(instance, evidence);
                    }
                }
            }
        }


        return result;
    }
    
    
    
    public Collection<Double> getInstancesToAggregate(MeasureScope measureScope) {
    	List instances = new ArrayList();
    	
    	
        Collection<? extends Measure> measures = baseComputer.compute();
        List<? extends Measure> filters = new ArrayList<Measure>();
        if (filterComputer != null) {
            filters = filterComputer.compute();
        }

        Map<String, MeasureInstance> measureMap = buildMeasureMap(measures);
        Map<String, MeasureInstance> filterMap = buildMeasureMap(filters);
        Collection<MeasureScope> allScopes = null;

        if (listGroupByMeasureComputer != null && listGroupByMeasureComputer.size() > 0) {

        } else {
            allScopes = classifier.listScopes(definition.isIncludeUnfinished());
        }

        if (filterComputer != null) {
            Collection<String> filterScopeInstances = filterTrueInstances(measureScope.getInstances(), filterMap);
            measureScope.getInstances().retainAll(filterScopeInstances);
        }

        return chooseMeasuresToAggregate(measureScope, measureMap);
    }

    
    public List<? extends Measure> getMeasureDistributions() {
        List<Measure> result = new ArrayList<Measure>();
        Collection<? extends Measure> measures = DistributionMediator.retrieve(baseComputer);
        System.out.println(this);

        
        List<? extends Measure> filters = new ArrayList<Measure>();
        if (filterComputer != null) {
            filters = filterComputer.compute();
        }

        Map<String, MeasureInstance> measureMap = buildMeasureMap(measures);
        Map<String, MeasureInstance> filterMap = buildMeasureMap(filters);
        Collection<MeasureScope> allScopes = null;

        if (listGroupByMeasureComputer != null && listGroupByMeasureComputer.size() > 0) {

        } else {
            allScopes = classifier.listScopes(definition.isIncludeUnfinished());
        }


        for (MeasureScope scope : allScopes) {
            if (filterComputer != null) {
                Collection<String> filterScopeInstances = filterTrueInstances(scope.getInstances(), filterMap);
                scope.getInstances().retainAll(filterScopeInstances);
            }
            
            //DONE change to Collection<Map<Double, Double>>
            Collection<Map<Double, Double>> toAggregate = chooseMeasureDistributionsToAggregate(scope, measureMap);
            
            List<Collection<Double>> inputs = joinInputMeasures(toAggregate);
            //join all maps to get the potential input values and their probabilities
            
            Map<Double,Double> finalDistribution = new HashMap();
            for (Collection<Double> realisation : inputs) {
            	double realisationProbability = 1.0;
            	for (int i=0;i<realisation.size();i++) {
            		double value = (double) realisation.toArray()[i];
            		realisationProbability *= ((Map<Double, Double>)(toAggregate.toArray()[i])).get(value);
            	}
            	
                //TODO for each of these Collection<Double> call this.agg.getProbabilityDistribution.
                //TODO add this distribution to a map <Double, Double> and sanitize this map in the end (divide all values by number of input collections)
            	Map<Double,Double> distribution = this.agg.getOutputProbability(
            			realisation,
            			definition.getAggregationFunction(),
                		definition.getBoundaryEstimation(),
                		definition.getEpsilon(), 
                		definition.getTarget(),
                		definition.getFalloff(),
                		definition.getExtensionFactor(),
                		definition.getMinimalSize()
                		);
            	for (Double value : distribution.keySet()) {
            		finalDistribution.put(value, distribution.get(value)*realisationProbability + finalDistribution.getOrDefault(value, 0.0));
            	}
            }
            
            double probabilitySum= 0;
            for(double temp : finalDistribution.values()) {
            	probabilitySum +=temp;
            }
            for(double value : finalDistribution.keySet()) {
            	finalDistribution.put(value, finalDistribution.get(value)/probabilitySum);
            }

            //TODO check when exactly toAggregate.isEmpty() is true, and what to do in that case
            /*
            if (!toAggregate.isEmpty()) {
                double val = this.agg.aggregate(
                		toAggregate,
                		definition.getAggregationFunction(),
                		definition.getBoundaryEstimation(),
                		definition.getEpsilon(), 
                		definition.getTarget(),
                		definition.getFalloff(),
                		definition.getExtensionFactor(),
                		definition.getMinimalSize()
                		);
             */
            Measure measure = new Measure(definition, scope, finalDistribution);
		    result.add(measure);
		    //TODO make consistent with PPINOT 2.3-SNAPSHOT - find out what commented out block actually does
		    if (true) {
		    //if (definition.isIncludeEvidences()) {
		        for (String instance : scope.getInstances()) {
		            Map<String, Measure> evidence = new HashMap<String, Measure>();
		            evidence.put("base", measureMap.get(instance));
		            if (filterComputer != null) {
		                evidence.put("filter", filterMap.get(instance));
		            }
		            measure.addEvidence(instance, evidence);
		        }
		    }
        }
        return result;
    } 

    
    
    List<Collection<Double>> joinInputMeasures(Collection<Map<Double, Double>> toAggregate){
    	List<Collection<Double>> finalList = new ArrayList();
    	finalList.add(new ArrayList());
    	for (Map<Double, Double> randomVariable : toAggregate) {
    		finalList = mergeRandomVariableIntoList(randomVariable, finalList);
    	}
    	return finalList;
    }
    
    
    List<Collection<Double>> mergeRandomVariableIntoList(Map<Double,Double> vars, List<Collection<Double>> lists){
    	List<Collection<Double>> newList = new ArrayList();
    	for (Collection<Double> list : lists) {
    		for (Double var : vars.keySet()) {
        		List<Double> mergedEntry = new ArrayList();
        		mergedEntry.addAll(list);
        		mergedEntry.add(var);
        		newList.add(mergedEntry);
    		}
    	}
    	return newList;
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
    
    
    private Collection<Map<Double, Double>> chooseMeasureDistributionsToAggregate(MeasureScope scope, Map<String, MeasureInstance> measureMap) {
        Collection<Map<Double, Double>> toAggregate = new ArrayList<Map<Double, Double>>();
        for (String instance : scope.getInstances()) {
            if (measureMap.containsKey(instance)) {
                toAggregate.add((Map<Double, Double>)measureMap.get(instance).getValueAsObject());
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
