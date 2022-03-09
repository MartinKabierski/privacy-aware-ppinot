package pappi.aggregators.laplace;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.distribution.LaplaceDistribution;

import es.us.isa.ppinot.evaluation.Aggregator;
import pappi.aggregators.NoisyAggregator;
import pappi.aggregators.OutputDistribution;

public class LaplaceAggregatorDistribution implements OutputDistribution{
	
    public static final String SUM_LAP = "Sum_lap";
    public static final String AVG_LAP = "Average_lap";
    public static final String MAX_LAP = "Maximum_lap";
    public static final String MIN_LAP = "Minimum_lap";
	
	public LaplaceAggregatorDistribution() {
	}

	public Map<Double, Double> getOutputProbability(String function, Collection<Double> values, double epsilon, double lowerBound, double upperBound) {
		if (values.size()<=1) {
    		Map<Double,Double> nanMap = new HashMap();
    		nanMap.put(Double.NaN, 1.0);
    		return nanMap;
		}

		if (function == SUM_LAP) {
			return sum(values, epsilon, lowerBound, upperBound);
		}
		else if (function == AVG_LAP) {
			return mean(values, epsilon, lowerBound, upperBound);
		}
		else if (function == MAX_LAP) {
			return max(values, epsilon, lowerBound, upperBound);
		}
		else if (function == MIN_LAP) {
			return min(values, epsilon, lowerBound, upperBound);
		}
		else {
    		Map<Double,Double> nanMap = new HashMap();
    		nanMap.put(Double.NaN, 1.0);
    		return nanMap;
		}
	}
	
	public Map<Double, Double> mean(Collection<Double> values, double epsilon, double lowerBound, double upperBound) {
		//catch corner cases, where all cases have same value or bounds are the same, we assume this then is a summation of booleans, so upper bound is 1
		//TODO give indicator from what domain inputs are
		/*if(upperBound == 0 && lowerBound == 0) {
			upperBound = 1;
		}
		if(upperBound == 1 && lowerBound == 1) {
			lowerBound = 0;
		}*/
		Double sensitivity = (upperBound-lowerBound)/values.size();
		//catch corner cases, where all cases have same value or bounds are the same
		if(sensitivity == 0 || sensitivity.isNaN()) {
    		Map<Double,Double> nanMap = new HashMap();
    		nanMap.put(Double.NaN, 1.0);
    		return nanMap;
		}
		
		double mean = new Aggregator(Aggregator.AVG).aggregate(values);
//		System.out.println(mean);
		
		LaplaceDistribution laplace = new LaplaceDistribution(mean,sensitivity/epsilon);
		if (lowerBound==upperBound) {
			Map<Double,Double> oneVarMap = new HashMap();
    		oneVarMap.put(lowerBound, 1.0);
    		return oneVarMap;
		}
		
        return convertToMap(laplace, lowerBound, upperBound,values.size());
	}
	

	public Map<Double, Double> sum(Collection<Double> values, double epsilon, double lowerBound, double upperBound) {
		double sensitivity = upperBound;
		//catch corner cases, where all cases have same value or bounds are the same, we assume this then is a summation of booleans, so upper bound is 1
		//TODO give indicator from what domain inputs are
		if(sensitivity == 0)sensitivity=1; //
		
        double result = 0;
        boolean processed = false;
        for (Double v: values) {
            if (!v.isNaN()) {
                result += v;
                processed = true;
            }
        }

		LaplaceDistribution laplace = new LaplaceDistribution(result,sensitivity/epsilon);
		if (lowerBound==upperBound) {
			Map<Double,Double> oneVarMap = new HashMap();
    		oneVarMap.put(lowerBound * values.size(), 1.0);
    		return oneVarMap;
		}
		if (processed) {
			return convertToMap(laplace, lowerBound*values.size(), upperBound*values.size(), values.size());
		}
		else {
    		Map<Double,Double> nanMap = new HashMap();
    		nanMap.put(Double.NaN, 1.0);
    		return nanMap;
		}
	}
	
	
	public Map<Double, Double> min(Collection<Double> values, double epsilon, double lowerBound, double upperBound) {
		double sensitivity = upperBound-lowerBound;
		
		double min = Double.MAX_VALUE;
        for (Double v : values) {
            if (v < min) {
                min = v;
            }
        }
		LaplaceDistribution laplace = new LaplaceDistribution(min,sensitivity/epsilon);
		if (lowerBound==upperBound) {
			Map<Double,Double> oneVarMap = new HashMap();
    		oneVarMap.put(lowerBound, 1.0);
    		return oneVarMap;
		}
		
        return convertToMap(laplace, lowerBound, upperBound, values.size());
	}
	
	
	public Map<Double, Double> max(Collection<Double> values, double epsilon, double lowerBound, double upperBound) {
		double sensitivity = upperBound-lowerBound;
		
		
		double max = Double.MIN_VALUE;
        for (Double v: values) {
            if (v > max) {
                max = v;
            }
        }
        
		LaplaceDistribution laplace = new LaplaceDistribution(max,sensitivity/epsilon);
		if (lowerBound==upperBound) {
			Map<Double,Double> oneVarMap = new HashMap();
    		oneVarMap.put(lowerBound, 1.0);
    		return oneVarMap;
		}
		return convertToMap(laplace, lowerBound, upperBound, values.size());
	}

	private Map<Double, Double> convertToMap(LaplaceDistribution laplace, double lowerBound, double upperBound, int valueCount) {
		//catch corner cases, where all cases have same value or bounds are the same, we assume this then is a summation of booleans, so upper bound is 1
		//TODO give indicator from what domain inputs are
		if(upperBound == 0 && lowerBound == 0) {
			upperBound = 1;
		}
		if(upperBound == 1 && lowerBound == 1) {
			lowerBound = 0;
		}
		//replace with 100 values set
		double delta = (upperBound-lowerBound)/100.0;
		double x = lowerBound;
		Map<Double, Double> probabilities = new HashMap<Double, Double>();
		while (x<=upperBound) {
			probabilities.put(x, laplace.density(x));
			x += delta;
		}
		
		double sum = 0.0;
		for (Double prob : probabilities.values()) {
			sum+=prob;
		}
		//System.out.println("SUM PRIOR" + sum);
		
		for (Double key : probabilities.keySet()) {
			probabilities.put(key, probabilities.get(key)/sum);
		}
		
		sum = 0.0;
		for (Double prob : probabilities.values()) {
			sum+=prob;
		}
		//System.out.println("SUM " + sum);
		
		return probabilities;
	}
}
