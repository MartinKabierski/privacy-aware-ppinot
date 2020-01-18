package pappi;

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
    
    
	public static Bounds estimateBoundsMinMax(Collection<Double> values) {
    	double min=Double.MAX_VALUE;
    	double max=0;
		for(Double value : values) {
    		if(!value.isNaN()) {
	    		if(value < min) min = value;
	    		if(value > max) max = value;
    		}
    	}
		if (min==Double.MAX_VALUE && max==0) {
			return new Bounds(-1,-1);
		}
		return new Bounds(min, max);
	}
	
	
	
	public static Bounds estimateBoundsFilter(Collection<Double> values) {
		//int toExtend = (int)Math.ceil((((values.size()*1.1) - values.size())/2.0));
		
    	double min=Double.MAX_VALUE;
    	double max=0;
    	
		for(Double value : values) {
    		if(!value.isNaN()) {
	    		if(value < min) min = value;
	    		if(value > max) max = value;
    		}
    	}
		double toFilter = ((max-min)*1.2-(max-min))/2.0;
		double newMax =max-toFilter;
		double newMin =min+toFilter;
		//right now actually decreases set size of input set is used multiple times
		//TODO do not change original representation
		values.removeIf(x -> (x>newMax||x<newMin));
		
		//allowing negative bounds increases performance for minimum queries if minimum is close to 0
		return new Bounds(Math.max(0,min+toFilter),max-toFilter);
		//return new Bounds(min-toExtend,max+toExtend);
	}
	
	
	
	public static Bounds estimateBoundsExtend(Collection<Double> values, double factor) {
		//int toExtend = (int)Math.ceil((((values.size()*1.1) - values.size())/2.0));
		
    	double min=Double.MAX_VALUE;
    	double max=0;
    	
		for(Double value : values) {
    		if(!value.isNaN()) {
	    		if(value < min) min = value;
	    		if(value > max) max = value;
    		}
    	}
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
		//allowing negative bounds increases performance for minimum queries if minimum is close to 0
		//return new Bounds(Math.max(0,min-toExtend),max+toExtend);
		return new Bounds(min-toExtend,max+toExtend);
	}
}
