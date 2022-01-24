package pappi.boundary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.mapdb.DBException.VolumeClosedByInterrupt;


public class BoundaryEstimator {
	
	//TODO return Interval instead of Bounds
    public static final String MINMAX = "minmax";
    public static final String FILTER = "filter";
    public static final String EXTEND = "extend";
	
    public static Bounds estimateBounds(Collection<Double> values, String method, double factor) {
    	if(MINMAX.equals(method)){
    		return  estimateBoundsMinMax(values);
    	}
    	if(FILTER.equals(method)){
    		return  estimateBoundsFilter(values);
    	}
    	if(EXTEND.equals(method)){
    		return  estimateBoundsExtend(values, factor);
    	}
    	return new Bounds(-1,-1);
    }
    
    
	/**
	 * @param values, a collection of doubles
	 * @return the boundaries of the values as a tuple (min, max)
	 */
	public static Bounds estimateBoundsMinMax(Collection<Double> vals) {
		Bounds minMax = minMax(vals);
		
		if (minMax.getLowerBound()==Double.MAX_VALUE && minMax.getUpperBound()==0) {
			return new Bounds(-1,-1);
		}
		
		return minMax;
	}
	
	
	
	public static Bounds estimateBoundsFilter(Collection<Double> vals) {
    	Bounds minMax = minMax(vals);
    	double min = minMax.getLowerBound();
    	double max = minMax.getUpperBound();
		
		double toFilter = ((max-min)*1.2-(max-min))/2.0;
		double newMin = min + toFilter;
		double newMax = max - toFilter;
		minMax.setLowerBound(Math.max(0, newMax));
		minMax.setUpperBound(newMax);
		//right now actually decreases set size of input set is used multiple times
		//TODO do not change original representation
		vals.removeIf(x -> (x>newMax||x<newMin));
		
		//allowing negative bounds increases performance for minimum queries if minimum is close to 0
		return minMax;
		//return new Bounds(min-toExtend,max+toExtend);
	}
	
	
	
	public static Bounds estimateBoundsExtend(Collection<Double> vals, double factor) {
		//int toExtend = (int)Math.ceil((((values.size()*1.1) - values.size())/2.0));
		
		Bounds minMax = minMax(vals);
    	double min = minMax.getLowerBound();
    	double max = minMax.getUpperBound();
		
		/*List<Double> sortedValues = new ArrayList<Double>(values);
		Collections.sort(sortedValues);
		double intervalRange=0;
		for(int i=0;i<sortedValues.size()-1;i++) {
			intervalRange=intervalRange+(sortedValues.get(i+1)-sortedValues.get(i));
		}
		intervalRange=intervalRange/sortedValues.size()-2;
		return new Bounds(min - (sortedValues.get(1)-sortedValues.get(0)), max + (sortedValues.get(sortedValues.size()-2)-sortedValues.get(sortedValues.size()-1)));
		System.out.println(intervalRange);
*/
		double toExtend = ((max-min)*factor-(max-min))/2.0;
		minMax.setLowerBound(min - toExtend);
		minMax.setUpperBound(max + toExtend);
		//allowing negative bounds increases performance for minimum queries if minimum is close to 0
		//return new Bounds(Math.max(0,min-toExtend),max+toExtend);
		return minMax;
	}
	
	/**
	 * @param vals: a list of double values, possibly containing null pointers
	 * @return the boundaries of the values, i.e. the minimum, maximum as a Bounds object
	 */
	public static Bounds minMax(Collection<Double> vals) {
		double min=Double.MAX_VALUE;
    	double max=0;
    	
		for(Double value : vals) {
    		if(!value.isNaN()) {
	    		if(value < min) min = value;
	    		if(value > max) max = value;
    		}
    	}
		
		return new Bounds(min, max);
	}
}
