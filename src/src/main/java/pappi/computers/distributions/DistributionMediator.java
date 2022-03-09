package pappi.computers.distributions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.us.isa.ppinot.evaluation.Measure;
import es.us.isa.ppinot.evaluation.MeasureInstance;
import es.us.isa.ppinot.evaluation.computers.MeasureComputer;
import pappi.computers.OptimizablePrivacyAwareMeasureComputer;
import pappi.computers.PrivacyAwareAggregatedMeasureComputer;
import pappi.computers.PrivacyAwareDerivedMeasureComputer;

public class DistributionMediator {
	
	
	public static List<? extends Measure>retrieve(MeasureComputer computer) {
		if (computer instanceof PrivacyAwareAggregatedMeasureComputer) {
			return ((PrivacyAwareAggregatedMeasureComputer) computer).getMeasureDistributions();
		}
		
		if (computer instanceof PrivacyAwareDerivedMeasureComputer) {
			//TODO have PrivacyAwareMeasureComputer decide on what kind of function to use, ust like in aggregated case
			return ((PrivacyAwareDerivedMeasureComputer) computer).getMeasureDistributions();
		}
		
		if (computer instanceof OptimizablePrivacyAwareMeasureComputer) {
			return ((OptimizablePrivacyAwareMeasureComputer) computer).getMeasureDistributions();
		}
		//else it is a base measure, simply calculate original value and wrap result with 100% occurence probability
		else {
			System.out.println(computer);
        	List<? extends Measure> measures = computer.compute();
        	
        	List<Measure> probabilityMeasures = new ArrayList<Measure>();
        	
        	for (Measure measure : measures) {
        		Map<Double,Double> wrappedValue = new HashMap<Double,Double>();
        		wrappedValue.put(measure.getValue(), 1.0);
        		
            	MeasureInstance probabilityMeasure = new MeasureInstance(measure.getDefinition(), measure.getMeasureScope(), wrappedValue);
            	probabilityMeasures.add(probabilityMeasure);
	       }
			return probabilityMeasures;
		}
	}
}
