package pappi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.us.isa.ppinot.evaluation.GroupByTemporalMeasureScopeImpl;
import es.us.isa.ppinot.evaluation.Measure;
import es.us.isa.ppinot.evaluation.MeasureInstance;
import es.us.isa.ppinot.evaluation.MeasureScope;
import es.us.isa.ppinot.evaluation.TemporalMeasureScope;
import es.us.isa.ppinot.evaluation.computers.AbstractBaseMeasureComputer;
import es.us.isa.ppinot.evaluation.computers.AggregatedMeasureComputer;
import es.us.isa.ppinot.evaluation.computers.DataMeasureComputer;
import es.us.isa.ppinot.evaluation.computers.MeasureComputer;
import es.us.isa.ppinot.evaluation.computers.MeasureComputerFactory;
import es.us.isa.ppinot.evaluation.logs.LogEntry;
import es.us.isa.ppinot.evaluation.scopes.ScopeClassifier;
import es.us.isa.ppinot.evaluation.scopes.ScopeClassifierFactory;
import es.us.isa.ppinot.model.MeasureDefinition;
import es.us.isa.ppinot.model.ProcessInstanceFilter;
import es.us.isa.ppinot.model.aggregated.AggregatedMeasure;
import es.us.isa.ppinot.model.base.DataMeasure;
//TODO add correct parameter
public class PrivacyAwareAggregatedMeasureComputer implements MeasureComputer{

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
        final MeasureComputerFactory measureComputerFactory = new MeasureComputerFactory();
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
                if (definition.isIncludeEvidences()) {
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
