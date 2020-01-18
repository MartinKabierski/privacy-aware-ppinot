package pappi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.*;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.distribution.EnumeratedRealDistribution;
import org.apache.commons.math3.distribution.LaplaceDistribution;
import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.apache.commons.math3.geometry.euclidean.oned.Interval;
import org.codehaus.jackson.map.ser.std.StdArraySerializers.ShortArraySerializer;

import es.us.isa.ppinot.evaluation.Aggregator;

public class PrivacyAwareAggregator extends Aggregator{
	//TODO Refactor classes
	//TODO check if sensitivity values are actually correct
	//TODO check if sensitivity is actually global and not local - avg
	//TODO parameterize everything
	//TODO invalidate old functions, keep Laplace, LDP(?), Interval-based, Aggregation-based, Smooth
	//TODO proper code refactoring
	//TODO set hidden parameters into config file
	//TODO return truncated results
	
	//laplace mechanism
    public static final String SUM_LAP = "Sum_lap";
    public static final String AVG_LAP = "Avg_lap";
    public static final String MAX_LAP = "Max_lap";
    public static final String MIN_LAP = "Min_lap";
    
    //local differential privacy
    public static final String SUM_LDP = "Sum_ldp";
    public static final String AVG_LDP = "Avg_ldp";
    public static final String MAX_LDP = "Max_ldp";
    public static final String MIN_LDP = "Min_ldp";
    
    //interval-based methods
    public static final String SUM_EXP = "Sum_exp";//exponential interval based
    public static final String AVG_EXP = "Avg_exp";//exponential interval based
    public static final String MAX_EXP = "Max_exp";//exponential interval based
    public static final String MIN_EXP = "Min_exp";//exponential interval based
    
    //same as prior, but with target falloff
    public static final String SUM_EXP_FALLOFF = "Sum_exp_falloff";
    public static final String AVG_EXP_FALLOFF = "Avg_exp_falloff";
    public static final String MAX_EXP_FALLOFF = "Max_exp_falloff";
    public static final String MIN_EXP_FALLOFF = "Min_exp_falloff";
    
    //subsample aggregate using average
    public static final String SUM_AGGREGATE = "Sum_aggregate";
    //subsample aggregate using average and falloff
    public static final String SUM_AGGREGATE_FALLOFF = "Sum_aggregate_falloff";
    
    //special cases where smooth sensitivity may be used
    public static final String AVG_SMOOTH = "Avg_smooth";
    public static final String MAX_SMOOTH = "Max_smooth";
    public static final String MIN_SMOOTH = "Min_smooth";
    
    public static final String PERCENTAGE = "Percentage";

    
	private String aggregationFunction;

	public PrivacyAwareAggregator() {
	    }
	
    public PrivacyAwareAggregator(String aggregationFunction) {
        this.aggregationFunction = aggregationFunction;
    }
    
    public double aggregate(
    		Collection<Double> values,
    		String aggregationFunction,
    		String boundaryEstimation,
    		double epsilon,
    		double target,
    		double falloff,
    		double extensionFactor,
    		int minimalSize
    		) {
    	
    	minimalSize=values.size();
    	if(values.size()<minimalSize)return Double.NaN;
    	
    	double lowerBound;
    	double upperBound;
    	Bounds bounds=new Bounds(0, 0);
    	if (BoundaryEstimator.MINMAX.equals(boundaryEstimation)){
        	bounds = BoundaryEstimator.estimateBoundsMinMax(values);
    	}
    	else if (BoundaryEstimator.FILTER.equals(boundaryEstimation)){
        	bounds = BoundaryEstimator.estimateBoundsFilter(values);
    	}
    	else if (BoundaryEstimator.EXTEND.equals(boundaryEstimation)){
    		bounds = BoundaryEstimator.estimateBoundsExtend(values, extensionFactor);
    	}
    	//else bounds = BoundaryEstimator.estimateBoundsMinMax(values);
    	
    	lowerBound=bounds.lowerBound;
    	upperBound=bounds.upperBound;
    	//Data Sanitization
    	if (lowerBound<0 && upperBound <0 || lowerBound > upperBound)
    		return Double.NaN;
    	//filter out Nan from set
    	Predicate<Double> nanFilter = x -> !x.isNaN();
    	values = values.stream().filter(nanFilter).collect(Collectors.toList());
    	
    	//apply aggregation scheme
        double aggregation = Double.NaN;
        if (SUM_LAP.equals(aggregationFunction)) {
            aggregation = sum_lap(values, epsilon, lowerBound, upperBound);
            aggregation = Math.max(lowerBound*values.size(), Math.min(aggregation, upperBound*values.size()));
            } 
        else if (AVG_LAP.equals(aggregationFunction)) {
            aggregation = avg_lap(values, epsilon, lowerBound, upperBound, minimalSize);
            aggregation = Math.max(lowerBound, Math.min(aggregation, upperBound));

        	} 
        else if (MAX_LAP.equals(aggregationFunction)) {
            aggregation = max_lap(values, epsilon, lowerBound, upperBound);
            aggregation = Math.max(lowerBound, Math.min(aggregation, upperBound));

        	} 
        else if (MIN_LAP.equals(aggregationFunction)) {
            aggregation = min_lap(values, epsilon, lowerBound, upperBound);
            aggregation = Math.max(lowerBound, Math.min(aggregation, upperBound));

        	}

        
        else if (AVG_EXP_FALLOFF.equals(aggregationFunction)) {
        aggregation = avg_exp_falloff(values, epsilon, lowerBound, upperBound, target, falloff, minimalSize);
        aggregation = Math.max(lowerBound, Math.min(aggregation, upperBound));
        	} 
        else if (MAX_EXP_FALLOFF.equals(aggregationFunction)) {
        aggregation = max_exp_falloff(values, epsilon, lowerBound, upperBound, target, falloff);
        aggregation = Math.max(lowerBound, Math.min(aggregation, upperBound));
        	} 
        else if (MIN_EXP_FALLOFF.equals(aggregationFunction)) {
        aggregation = min_exp_falloff(values, epsilon, lowerBound, upperBound, target, falloff);
        aggregation = Math.max(lowerBound, Math.min(aggregation, upperBound));
        	} 
        else if (SUM_EXP_FALLOFF.equals(aggregationFunction)) {
            aggregation = sum_exp_falloff(values, epsilon, lowerBound, upperBound, target, falloff);
            aggregation = Math.max(lowerBound*values.size(), Math.min(aggregation, upperBound*values.size()));

        }

        else if (SUM_LDP.equals(aggregationFunction)) {
            aggregation = sum_ldp(values, epsilon, lowerBound, upperBound);
            aggregation = Math.max(lowerBound*values.size(), Math.min(aggregation, upperBound*values.size()));

            }
        else if (AVG_LDP.equals(aggregationFunction)) {
            aggregation = avg_ldp(values, epsilon, lowerBound, upperBound, minimalSize);
            aggregation = Math.max(lowerBound, Math.min(aggregation, upperBound));
        	}
        else if (MAX_LDP.equals(aggregationFunction)) {
            aggregation = max_ldp(values, epsilon, lowerBound, upperBound);
            aggregation = Math.max(lowerBound, Math.min(aggregation, upperBound));
        	} 
        else if (MIN_LDP.equals(aggregationFunction)) {
            aggregation = min_ldp(values, epsilon, lowerBound, upperBound);
            aggregation = Math.max(lowerBound, Math.min(aggregation, upperBound));
        	}
        
        else if (AVG_SMOOTH.equals(aggregationFunction)) {
            aggregation = avg_smooth(values, epsilon, lowerBound, upperBound);
            aggregation = Math.max(lowerBound, Math.min(aggregation, upperBound));
        	}
        else if (MAX_SMOOTH.equals(aggregationFunction)) {
            aggregation = max_smooth(values, epsilon, lowerBound, upperBound);
            aggregation = Math.max(lowerBound, Math.min(aggregation, upperBound));
        	} 
        else if (MIN_SMOOTH.equals(aggregationFunction)) {
            aggregation = min_smooth(values, epsilon, lowerBound, upperBound);
            aggregation = Math.max(lowerBound, Math.min(aggregation, upperBound));
        	}
        
        else if (MIN_EXP.equals(aggregationFunction)) {
        	aggregation = min_interval(values, epsilon, lowerBound, upperBound);
            aggregation = Math.max(lowerBound, Math.min(aggregation, upperBound));
        }
        else if (MAX_EXP.equals(aggregationFunction)) {
        	aggregation = max_interval(values, epsilon, lowerBound, upperBound);
            aggregation = Math.max(lowerBound, Math.min(aggregation, upperBound));
        }
        else if (AVG_EXP.equals(aggregationFunction)) {
        	aggregation = avg_interval(values, epsilon, lowerBound, upperBound, minimalSize);
            aggregation = Math.max(lowerBound, Math.min(aggregation, upperBound));
        }
        else if (SUM_EXP.equals(aggregationFunction)) {
        	aggregation = sum_interval(values, epsilon, lowerBound, upperBound);
            aggregation = Math.max(lowerBound*values.size(), Math.min(aggregation, upperBound*values.size()));

        }
        else if (SUM_AGGREGATE.equals(aggregationFunction)) {
        	aggregation = sum_sample_aggregate(values, epsilon, lowerBound, upperBound, minimalSize);
            aggregation = Math.max(lowerBound*values.size(), Math.min(aggregation, upperBound*values.size()));

        }
        else if (SUM_AGGREGATE_FALLOFF.equals(aggregationFunction)) {
        	aggregation = sum_sample_aggregate_falloff(values, epsilon, lowerBound, upperBound, minimalSize, target, falloff);
            aggregation = Math.max(lowerBound*values.size(), Math.min(aggregation, upperBound*values.size()));

        }
        else if (PERCENTAGE.equals(aggregationFunction)) {
        	aggregation = derived_subsample_and_aggregate(values, boundaryEstimation, epsilon, lowerBound, upperBound, minimalSize, target, falloff);
            aggregation = Math.max(0, Math.min(aggregation, 100));
            if(aggregation>100)System.out.println(aggregation);

        }
        
        //else normal aggregate function, i.e. sum, min, max, avg
        else return super.aggregate(aggregationFunction, values);
        
        return aggregation;
		//return Math.max(lowerBound,Math.min(aggregation,upperBound));
    }



	/*
	 * Laplace Mechanisms
     */
	public double sum_lap(Collection<Double> values, double epsilon, double lowerBound, double upperBound) {
		double sensitivity = upperBound;
		//catch corner cases, where all cases have same value or bounds are the same
		if(sensitivity == 0)return Double.NaN;
		
		LaplaceDistribution laplace = new LaplaceDistribution(0,sensitivity/epsilon);
		
        double result = 0;
        boolean processed = false;
        for (Double v: values) {
            if (!v.isNaN()) {
                result += v;
                processed = true;
            }
        }
        result+=laplace.sample();
        		
		return processed ? result : Double.NaN;
	}
	
	public double avg_lap(Collection<Double> values, double epsilon, double lowerBound, double upperBound, int minimalSize) {
		Double sensitivity = (upperBound-lowerBound)/minimalSize;
		//catch corner cases, where all cases have same value or bounds are the same
		if(sensitivity == 0 || sensitivity.isNaN())return Double.NaN;
		LaplaceDistribution laplace = new LaplaceDistribution(0,sensitivity/epsilon);
		//System.out.println(sensitivity);
		
		double sum = 0;
        double count = 0;
        for (Double v: values) {
            if (!v.isNaN()) {
                sum += v;
                count += 1;
            }
            //System.out.println(laplace.sample());
        }
        //System.out.println();
        double avg = sum/count;
        avg = avg + laplace.sample();

        return (count > 0 ? avg : Double.NaN);
	}
	
	public double max_lap(Collection<Double> values, double epsilon, double lowerBound, double upperBound) {
		double sensitivity = upperBound-lowerBound;
		LaplaceDistribution laplace = new LaplaceDistribution(0,sensitivity/epsilon);
		
		
		double max = Double.MIN_VALUE;
        for (Double v: values) {
            if (v > max) {
                max = v;
            }
        }
        
        max +=(int)laplace.sample();
        
        return max;
	}
	
	public double min_lap(Collection<Double> values, double epsilon, double lowerBound, double upperBound) {
		double sensitivity = upperBound-lowerBound;
		LaplaceDistribution laplace = new LaplaceDistribution(0,sensitivity/epsilon);

		
		double min = Double.MAX_VALUE;
        for (Double v : values) {
            if (v < min) {
                min = v;
            }
        }
        min += (int)laplace.sample();
        
        return min;
	}

	/*
	 * Local differential Privacy approaches
	 */
	private double sum_ldp(Collection<Double> values, double epsilon, double lowerBound, double upperBound) {
		double sensitivity = upperBound;
		//catch corner cases, where all cases have same value or bounds are the same
		if(sensitivity == 0)return Double.NaN;
		
		LaplaceDistribution laplace = new LaplaceDistribution(0,sensitivity/epsilon);
		List<Double>noisyValues=new ArrayList<Double>();
		values.stream().forEach(x-> noisyValues.add(x+laplace.sample()));
		
		
        double result = 0;
        boolean processed = false;
        for (Double v: noisyValues) {
            if (!v.isNaN()) {
                result += v;
                processed = true;
            }
        }
        		
		return processed ? result : Double.NaN;
	}

	private double avg_ldp(Collection<Double> values, double epsilon, double lowerBound, double upperBound, int minimalSize) {
		double sensitivity = (upperBound-lowerBound)/(double)minimalSize;
		//catch corner cases, where all cases have same value or bounds are the same
		if(sensitivity == 0)return Double.NaN;
		LaplaceDistribution laplace = new LaplaceDistribution(0,sensitivity/epsilon);
		List<Double>noisyValues=new ArrayList<Double>();
		values.stream().forEach(x-> noisyValues.add(x+laplace.sample()));
		
		double sum = 0;
        double count = 0;
        for (Double v: noisyValues) {
            if (!v.isNaN()) {
                sum += v;
                count += 1;
            }
        }


        
        return (count > 0 ? sum / count : Double.NaN);
	}

	private double max_ldp(Collection<Double> values, double epsilon, double lowerBound, double upperBound) {
		double sensitivity = upperBound;
		LaplaceDistribution laplace = new LaplaceDistribution(0,sensitivity/epsilon);
		List<Double>noisyValues=new ArrayList<Double>();
		values.stream().forEach(x-> noisyValues.add(x+laplace.sample()));
		
		
		double max = Double.MIN_VALUE;
        for (Double v: noisyValues) {
            if (v > max) {
                max = v;
            }
        }
                
        return max;
	}

	private double min_ldp(Collection<Double> values, double epsilon, double lowerBound, double upperBound) {
		double sensitivity = upperBound;
		LaplaceDistribution laplace = new LaplaceDistribution(0,sensitivity/epsilon);
		List<Double>noisyValues=new ArrayList<Double>();
		values.stream().forEach(x-> noisyValues.add(x+laplace.sample()));
		
		
		double min = Double.MAX_VALUE;
        for (Double v : noisyValues) {
            if (v < min) {
                min = v;
            }
        }
        return min;
	}
	
	
	private double min_smooth(Collection<Double> values, double epsilon, double lowerBound, double upperBound) {
		int gamma =3;
		List<Double> sortedValues = new ArrayList<Double>(values);
		Collections.sort(sortedValues);
		double s= 0;
		
		//calculate smooth sensitivity
		double beta_cauchy=epsilon/gamma;
		double delta = 1/values.size(); //thus delta can be assumed secure
		double beta_laplace=epsilon/(2*Math.log(1/delta));
		for(int i=0;i<sortedValues.size()-1;i++) {
			double localSensAtK = Math.max(sortedValues.get(i), sortedValues.get(i+1)-sortedValues.get(0)) * Math.exp(-(i+1)*beta_laplace);
			if (localSensAtK>s)s=localSensAtK;
		}
		
		//build probability distribution and sample
		double[] range = new double[((int)upperBound-(int)lowerBound)*1000];
		double[] Z = new double[((int)upperBound-(int)lowerBound)*1000];
		
		for (int i=0; i<range.length;i++) {
			double position = ((int)upperBound-(int)lowerBound)/2*-1 + (i*0.001);
			range[i] = position;
			Z[i] = 1/Math.pow(1+(Math.abs(position)),gamma);
		}		
		
		double total = Arrays.stream(Z).sum();
		for (int i=0; i<Z.length;i++) {
			Z[i]=Z[i]/total;
			//System.out.println(range[i]+" -> "+Z[i]);
		}

		//release result
		double min = Double.MAX_VALUE;
        for (Double v : values) {
            if (v < min) {
                min = v;
            }
        }
        
        //use Laplacian for (\epsilon,\delta)-dp
		LaplaceDistribution laplace = new LaplaceDistribution(0,1);
		

        
		EnumeratedRealDistribution test = new EnumeratedRealDistribution(range, Z);
		double alpha_cauchy=epsilon/(4*gamma);
		double alpha_laplace = epsilon/2;
		//System.out.println(s/alpha);
		return min + (s/alpha_laplace) * test.sample();
        //return min;
	}

	private double max_smooth(Collection<Double> values, double epsilon, double lowerBound, double upperBound) {
		return 0;
	}

	private double avg_smooth(Collection<Double> values, double epsilon, double lowerBound, double upperBound) {
		return 0;
	}
	
	
	private double min_interval(Collection<Double> values, double epsilon, double lowerBound, double upperBound) {
		//sort list, then min is at index 0
		List<Double> sortedValues = new ArrayList<Double>(values);
		sortedValues.removeIf(x -> x.isNaN());
		Collections.sort(sortedValues);
		sortedValues = sortedValues.stream().distinct().collect(Collectors.toList());
        //build inter value intervals and rank down the further from min-interval away
		double intervalStart=lowerBound;
		double intervalEnd=lowerBound;
		List<Interval> intervals = new ArrayList<Interval>();
		for (int i=0; i<sortedValues.size(); i++) {
			if(i==sortedValues.size()-1) {
				intervalEnd = upperBound;
			}
			else {
				double current = sortedValues.get(i);
				double next = sortedValues.get(i+1);
				intervalEnd = (next+current)/2;
			}
			intervals.add(new Interval(intervalStart, intervalEnd));
			intervalStart=intervalEnd;
		}
		//for(int i=0;i<intervals.size();i++) {
		//	System.out.print(intervals.get(i).getInf()+" - "+intervals.get(i).getSup()+"; ");
		//}
		//System.out.println();
		
		double[] scores = IntStream.range(0, intervals.size()).mapToDouble( x -> (double)x).map(x -> -x*1).toArray();
		double lowestScore = Math.abs(Arrays.stream(scores).min().getAsDouble());
		for(int i=0;i<scores.length;i++) {
			scores[i]=scores[i]+lowestScore;
		}
		//Arrays.stream(scores).forEach(x->System.out.print(x+","));
		//System.out.println();
		
		int[] outputRanges = IntStream.range(0, intervals.size()).toArray();

		double sensitivity = 1.0; //changing a value changes score by at most 1
		
		for (int i=0;i<scores.length;i++) {
			double intervalSize=intervals.get(i).getSize();
			scores[i]=intervalSize*Math.exp((epsilon*scores[i])/(2*sensitivity));
		}
		//scores = Arrays.stream(scores).map(score-> Math.exp((epsilon*score)/(2*sensitivity))).toArray();

		
		double new_total = Arrays.stream(scores).sum();
		double[] probabilities = Arrays.stream(scores).map(score-> score/new_total).toArray();
		//Arrays.stream(probabilities).forEach(x->System.out.print(x+","));
		//System.out.println();
		//for(double score : probabilities)System.out.print(score+",");
		//System.out.println("");
		
		EnumeratedIntegerDistribution distribution = new EnumeratedIntegerDistribution(outputRanges, probabilities);
		Interval sampledInterval=intervals.get(distribution.sample());
		
		//System.out.println(sampledInterval.getInf()+" - "+sampledInterval.getSup());
		UniformIntegerDistribution finalOutput =
				new UniformIntegerDistribution((int)sampledInterval.getInf(),(int)sampledInterval.getSup());
		return finalOutput.sample();
	}
	
	private double max_interval(Collection<Double> values, double epsilon, double lowerBound, double upperBound) {
		//sort list, then min is at index 0
		List<Double> sortedValues = new ArrayList<Double>(values);
		sortedValues.removeIf(x -> x.isNaN());
		Collections.sort(sortedValues);
		sortedValues = sortedValues.stream().distinct().collect(Collectors.toList());

        //build inter value intervals and rank down the further from min-interval away
		double intervalStart=lowerBound;
		double intervalEnd=lowerBound;
		List<Interval> intervals = new ArrayList<Interval>();
		for (int i=0; i<sortedValues.size(); i++) {
			if(i==sortedValues.size()-1) {
				intervalEnd = upperBound;
			}
			else {
				double current = sortedValues.get(i);
				double next = sortedValues.get(i+1);
				intervalEnd = (next+current)/2;
			}
			intervals.add(new Interval(intervalStart,intervalEnd));
			intervalStart=intervalEnd;
		}
		double[] scores = IntStream.range(0, intervals.size()).mapToDouble( x -> (double)x).toArray();
		double lowestScore = Math.abs(Arrays.stream(scores).min().getAsDouble());
		for(int i=0;i<scores.length;i++) {
			scores[i]=scores[i]+lowestScore;
		}
		
		int[] outputRanges = IntStream.range(0, intervals.size()).toArray();

		double sensitivity = 1.0; //changing a value changes score by at most 1
		
		for (int i=0;i<scores.length;i++) {
			double intervalSize=intervals.get(i).getSize();
			scores[i]=intervalSize*Math.exp((epsilon*scores[i])/(2*sensitivity));
		}
		//scores = Arrays.stream(scores).map(score-> Math.exp((epsilon*score)/(2*sensitivity))).toArray();

		double new_total = Arrays.stream(scores).sum();
		double[] probabilities = Arrays.stream(scores).map(score-> score/new_total).toArray();
		//for(double score : probabilities)System.out.print(score+",");
		//System.out.println("");
		
		EnumeratedIntegerDistribution distribution = new EnumeratedIntegerDistribution(outputRanges, probabilities);
		Interval sampledInterval=intervals.get(distribution.sample());
		
		//System.out.println(sampledInterval.getInf()+" - "+sampledInterval.getSup());
		UniformIntegerDistribution finalOutput = new UniformIntegerDistribution((int)sampledInterval.getInf(),(int)sampledInterval.getSup());
		return finalOutput.sample();
	}
	
	private double avg_interval(Collection<Double> values, double epsilon, double lowerBound, double upperBound, int minimalSize) {
		List<Double> sortedValues = new ArrayList<Double>(values);
		sortedValues.removeIf(x -> x.isNaN());
		Collections.sort(sortedValues);
//		sortedValues = sortedValues.stream().distinct().collect(Collectors.toList());

		
		double intervalStart=lowerBound;
		double intervalEnd=lowerBound;
		
		double avg=sortedValues.stream().reduce(0.0, (a, b)-> a+b)/values.size();
		double maxChange=(upperBound-lowerBound)/minimalSize;
		
		List<Interval> intervals = new ArrayList<Interval>();

		List<Interval> lowerIntervals 	= new ArrayList<Interval>();
		List<Interval> upperIntervals 	= new ArrayList<Interval>();
		Double lowerIntervalEnd  		= avg-maxChange/2;
		Double upperIntervalStart		= avg+maxChange/2;
		Interval averageInterval = new Interval(lowerIntervalEnd,upperIntervalStart);
		for(;lowerIntervalEnd>lowerBound && upperIntervalStart<upperBound; lowerIntervalEnd=lowerIntervalEnd-maxChange,upperIntervalStart=upperIntervalStart+maxChange) {
			if (lowerIntervalEnd>0)
				lowerIntervals.add(new Interval(Math.max(lowerBound,lowerIntervalEnd-maxChange),lowerIntervalEnd));
			if (upperIntervalStart<upperBound)
				upperIntervals.add(new Interval(upperIntervalStart,Math.min(upperBound,upperIntervalStart+maxChange)));
		}
		
		Collections.reverse(lowerIntervals);
		intervals.addAll(lowerIntervals);
		intervals.add(averageInterval);
		intervals.addAll(upperIntervals);
		
		
		double[]scores=new double[intervals.size()];
		int currentScore=0;
		for(int i=0;i<scores.length;i++) {
			if(intervals.get(i).getInf()<avg) {
				scores[i]=currentScore;
				currentScore++;
			}
			else {
				currentScore--;
				scores[i]=currentScore;
			}
		}
		double lowestScore = Math.abs(Arrays.stream(scores).min().getAsDouble());
		for(int i=0;i<scores.length;i++) {
			scores[i]=scores[i]+lowestScore;
		}
		
		int[] outputRanges = IntStream.range(0, intervals.size()).toArray();

		double sensitivity =1.0; //changing a value changes score by at most 1
		
		for (int i=0;i<scores.length;i++) {
			double intervalSize=intervals.get(i).getSize();
			scores[i]=intervalSize*Math.exp((epsilon*scores[i])/(2*sensitivity));
		}

		double new_total = Arrays.stream(scores).sum();
		double[] probabilities = Arrays.stream(scores).map(score-> score/new_total).toArray();
		//for(double score : probabilities)System.out.print(score+",");
		//System.out.println("");
		
		EnumeratedIntegerDistribution distribution = new EnumeratedIntegerDistribution(outputRanges, probabilities);
		Interval sampledInterval=intervals.get(distribution.sample());
		
		//System.out.println(sampledInterval.getInf()+" - "+sampledInterval.getSup());
		UniformIntegerDistribution finalOutput = new UniformIntegerDistribution((int)sampledInterval.getInf(),(int)sampledInterval.getSup());
		return finalOutput.sample();
	}
	
	private double sum_interval(Collection<Double> values, double epsilon, double lowerBound, double upperBound) {
		List<Double> sortedValues = new ArrayList<Double>(values);
		sortedValues.removeIf(x -> x.isNaN());
		Collections.sort(sortedValues);
//		sortedValues = sortedValues.stream().distinct().collect(Collectors.toList());

		
		double sum=sortedValues.stream().reduce(0.0, (a, b)-> a+b);
		double maxChange=upperBound;
		if(maxChange==0)maxChange=1;
		
		List<Interval> intervals = new ArrayList<Interval>();
		
		List<Interval> lowerIntervals 	= new ArrayList<Interval>();
		List<Interval> upperIntervals 	= new ArrayList<Interval>();
		Double lowerIntervalEnd  		= sum-maxChange/2;
		Double upperIntervalStart		= sum+maxChange/2;
		Interval averageInterval = new Interval(lowerIntervalEnd,upperIntervalStart);
		for(;lowerIntervalEnd>lowerBound*sortedValues.size() || upperIntervalStart<upperBound*sortedValues.size(); lowerIntervalEnd=lowerIntervalEnd-maxChange,upperIntervalStart=upperIntervalStart+maxChange) {
			if (lowerIntervalEnd>lowerBound*sortedValues.size())
				lowerIntervals.add(new Interval(Math.max(lowerBound*sortedValues.size(),lowerIntervalEnd-maxChange),lowerIntervalEnd));
			if (upperIntervalStart<upperBound*sortedValues.size())
				upperIntervals.add(new Interval(upperIntervalStart,Math.min(upperBound*sortedValues.size(),upperIntervalStart+maxChange)));
		}
		
		Collections.reverse(lowerIntervals);
		intervals.addAll(lowerIntervals);
		intervals.add(averageInterval);
		intervals.addAll(upperIntervals);
		//for(Interval i : intervals) {
		//	System.out.print(i.getInf()+"-"+i.getSup()+"; "); 
		//}
		
		//System.out.println();
		
		double[]scores=new double[intervals.size()];
		int currentScore=0;
		for(int i=0;i<scores.length;i++) {
			if(intervals.get(i).getInf()<sum) {
				currentScore++;
				scores[i]=currentScore;
			}
			else {
				currentScore--;
				scores[i]=currentScore;
			}
		}
		double lowestScore = Math.abs(Arrays.stream(scores).min().getAsDouble());
		for(int i=0;i<scores.length;i++) {
			scores[i]=scores[i]+lowestScore;
		}
		//for(double score : scores)System.out.print(score+",");
		//System.out.println("");
		
		int[] outputRanges = IntStream.range(0, intervals.size()).toArray();

		double sensitivity =1.0; //changing a value changes score by at most 1
		
		for (int i=0;i<scores.length;i++) {
			double intervalSize=intervals.get(i).getSize();
			scores[i]=intervalSize*Math.exp((epsilon*scores[i])/(2*sensitivity));
		}
		double new_total = Arrays.stream(scores).sum();
		double[] probabilities = Arrays.stream(scores).map(score-> score/new_total).toArray();
		//for(double score : probabilities)System.out.print(score+",");
		//System.out.println("");

		
		
		EnumeratedIntegerDistribution distribution = new EnumeratedIntegerDistribution(outputRanges, probabilities);
		Interval sampledInterval=intervals.get(distribution.sample());
		
		UniformIntegerDistribution finalOutput = new UniformIntegerDistribution((int)sampledInterval.getInf(),(int)sampledInterval.getSup());
		return finalOutput.sample();
	}

	
	private double sum_exp_falloff(Collection<Double> values, double epsilon, double lowerBound, double upperBound,
			double target, double falloff) {
		if (falloff==0.0) return sum_exp_falloff(values, epsilon, lowerBound, upperBound, -10, 1);		
		//if target is irelevant make sure that intervals are built without using falloff to avoid unneccessary noise
		if ((target<lowerBound*values.size() || target>upperBound*values.size()) && falloff!=1.0) {
			return sum_exp_falloff(values, epsilon, lowerBound, upperBound, target, 1);
		}
		List<Double> sortedValues = new ArrayList<Double>(values);
		sortedValues.removeIf(x -> x.isNaN());
		Collections.sort(sortedValues);
//		sortedValues = sortedValues.stream().distinct().collect(Collectors.toList());

		
		double intervalStart=lowerBound;
		double intervalEnd=lowerBound;
		
		double sum=sortedValues.stream().reduce(0.0, (a, b)-> a+b);
		double maxChange=upperBound;
		
		List<Interval> intervals = new ArrayList<Interval>();

		List<Interval> lowerIntervals 	= new ArrayList<Interval>();
		List<Interval> upperIntervals 	= new ArrayList<Interval>();
		Double lowerIntervalEnd  		= sum-maxChange/2;
		Double upperIntervalStart		= sum+maxChange/2;
		if(lowerIntervalEnd<target && upperIntervalStart>=target) {
			upperIntervals.add(new Interval(lowerIntervalEnd,target));
			upperIntervals.add(new Interval(target, upperIntervalStart));
		}
		else {
			upperIntervals.add(new Interval(lowerIntervalEnd,upperIntervalStart));
			
		}
		for(;lowerIntervalEnd>lowerBound*values.size() || upperIntervalStart<upperBound*values.size(); lowerIntervalEnd=lowerIntervalEnd-maxChange,upperIntervalStart=upperIntervalStart+maxChange) {
			if (lowerIntervalEnd>lowerBound*values.size())
				if(Math.max(lowerBound*values.size(),lowerIntervalEnd-maxChange)<target && lowerIntervalEnd >=target) {
					lowerIntervals.add(new Interval(target, lowerIntervalEnd));
					lowerIntervals.add(new Interval(Math.max(lowerBound*values.size(),lowerIntervalEnd-maxChange), target));
				}
				else {
					lowerIntervals.add(new Interval(Math.max(lowerBound*values.size(),lowerIntervalEnd-maxChange),lowerIntervalEnd));
				}
			if (upperIntervalStart<upperBound*values.size())
				if(upperIntervalStart<target && Math.min(upperBound*values.size(),upperIntervalStart+maxChange)>=target) {
					upperIntervals.add(new Interval(upperIntervalStart,target));
					upperIntervals.add(new Interval(target,Math.min(upperBound*values.size(),upperIntervalStart+maxChange)));
				}
				else {
					upperIntervals.add(new Interval(upperIntervalStart,Math.min(upperBound*values.size(),upperIntervalStart+maxChange)));
				}
		}
		Collections.reverse(lowerIntervals);
		intervals.addAll(lowerIntervals);
		intervals.addAll(upperIntervals);
		//for(Interval i : intervals) {
		//	System.out.print(i.getInf()+"-"+i.getSup()+"; "); 
		//}
		//System.out.println();
		
		double[]scores=new double[intervals.size()];
		double currentScore=0;
		
		for(int i=0;i<scores.length;i++) {
			if(intervals.get(i).getInf()<sum) {
				if(sum >target && intervals.get(i).getSup()<=target) {
					currentScore=currentScore+falloff;
					scores[i]=currentScore;
				}
				else {
					currentScore++;
					scores[i]=currentScore;
				}
			}
			else {
				if(sum <=target && intervals.get(i).getInf()>=target) {
					currentScore=currentScore-falloff;
					scores[i]=currentScore;
				}
				else {
					currentScore--;
					scores[i]=currentScore;
				}
			}
		}
		
		
		double lowestScore = Math.abs(Arrays.stream(scores).min().getAsDouble());
		for(int i=0;i<scores.length;i++) {
			scores[i]=scores[i]+lowestScore;
		}
		//for(double score : scores)System.out.print(score+",");
		//System.out.println("");
		
		int[] outputRanges = IntStream.range(0, intervals.size()).toArray();
		double sensitivity =falloff; //changing a value changes score by at most the falloff
		
		for (int i=0;i<scores.length;i++) {
			double intervalSize=intervals.get(i).getSize();
			scores[i]=intervalSize*Math.exp((epsilon*scores[i])/(2*sensitivity));
		}

		double new_total = Arrays.stream(scores).sum();
		double[] probabilities = Arrays.stream(scores).map(score-> score/new_total).toArray();
		//for(double score : probabilities)System.out.print(score+",");
		//System.out.println("");
		
		EnumeratedIntegerDistribution distribution = new EnumeratedIntegerDistribution(outputRanges, probabilities);
		Interval sampledInterval=intervals.get(distribution.sample());
		
		//System.out.println(sampledInterval.getInf()+" - "+sampledInterval.getSup());
		UniformIntegerDistribution finalOutput = new UniformIntegerDistribution((int)sampledInterval.getInf(),(int)sampledInterval.getSup());
		return finalOutput.sample();
	}

	private double min_exp_falloff(Collection<Double> values, double epsilon, double lowerBound, double upperBound,
			double target, double falloff) {
		if (falloff==0.0) return min_exp_falloff(values, epsilon, lowerBound, upperBound, -10, 1);
		
		//if target is irelevant make sure that intervals are built without using falloff to avoid unneccessary noise
		if ((target<lowerBound || target>upperBound) && falloff!=1.0) {
			return min_exp_falloff(values, epsilon, lowerBound, upperBound, target, 1);
		}
		double min = Double.MAX_VALUE;
        for (Double v : values) {
            if (v < min) {
                min = v;
            }
        }
		
		
		//sort list, then min is at index 0
		List<Double> sortedValues = new ArrayList<Double>(values);
		sortedValues.removeIf(x -> x.isNaN());
		Collections.sort(sortedValues);
		sortedValues = sortedValues.stream().distinct().collect(Collectors.toList());

		
        //build inter value intervals and rank down the further from min-interval away
		double intervalStart=lowerBound;
		double intervalEnd=lowerBound;
		List<Interval> intervals = new ArrayList<Interval>();
		for (int i=0; i<sortedValues.size(); i++) {
			if(i==sortedValues.size()-1) {
				intervalEnd=upperBound;
			}
			else {
				double current = sortedValues.get(i);
				double next = sortedValues.get(i+1);
				intervalEnd = (next+current)/2;
			}
				
			if(intervalStart<target && intervalEnd >=target) {
				intervals.add(new Interval(intervalStart,target));
				intervals.add(new Interval(target,intervalEnd));
			}
			else {
				intervals.add(new Interval(intervalStart, intervalEnd));
			}
			intervalStart=intervalEnd;
		}
		//for(int i=0;i<intervals.size();i++) {
		//	System.out.print(intervals.get(i).getInf()+" - "+intervals.get(i).getSup()+"; ");
		//}
		//System.out.println();
		
		//double[] scores = IntStream.range(0, intervals.size()).mapToDouble( x -> (double)x).map(x -> -x*1).toArray();
		double[] scores = new double[intervals.size()];
		double interval_score =0;
		for(int i=0;i<scores.length;i++) {
			if((min<target && intervals.get(i).getInf()>=target)) {
				interval_score = interval_score -(int)falloff;
				scores[i]=interval_score;
			}
			else if	(min>target && intervals.get(i).getSup()<target) {
				interval_score = interval_score +(int)falloff;
				scores[i]=interval_score;

			}
			else {
				interval_score--;
				scores[i]=interval_score;
			}
				
		}
		double lowestScore = Math.abs(Arrays.stream(scores).min().getAsDouble());
		for(int i=0;i<scores.length;i++) {
			scores[i]=scores[i]+lowestScore;
		}
		//Arrays.stream(scores).forEach(x->System.out.print(x+","));
		//System.out.println();
		
		
		int[] outputRanges = IntStream.range(0, intervals.size()).toArray();

		double sensitivity = falloff; //changing a value changes score by at most 2
		
		for (int i=0;i<scores.length;i++) {
			double intervalSize=intervals.get(i).getSize();
			scores[i]=intervalSize*Math.exp((epsilon*scores[i])/(2*sensitivity));
		}
		//scores = Arrays.stream(scores).map(score-> Math.exp((epsilon*score)/(2*sensitivity))).toArray();

		
		double new_total = Arrays.stream(scores).sum();
		double[] probabilities = Arrays.stream(scores).map(score-> score/new_total).toArray();
		//Arrays.stream(probabilities).forEach(x->System.out.print(x+","));
		//System.out.println();
		//for(double score : probabilities)System.out.print(score+",");
		//System.out.println("");
		
		EnumeratedIntegerDistribution distribution = new EnumeratedIntegerDistribution(outputRanges, probabilities);
		Interval sampledInterval=intervals.get(distribution.sample());
		
		//System.out.println(sampledInterval.getInf()+" - "+sampledInterval.getSup());
		UniformIntegerDistribution finalOutput =
				new UniformIntegerDistribution((int)sampledInterval.getInf(),(int)sampledInterval.getSup());
		return finalOutput.sample();
	}

	private double max_exp_falloff(Collection<Double> values, double epsilon, double lowerBound, double upperBound,
			double target, double falloff) {
		
		if (falloff==0.0) return max_exp_falloff(values, epsilon, lowerBound, upperBound, -10, 1);
		
		//if target is irelevant make sure that intervals are built without using falloff to avoid unneccessary noise
		if ((target<lowerBound || target>upperBound) && falloff!=1.0) {
			return max_exp_falloff(values, epsilon, lowerBound, upperBound, target, 1);
		}
		double max = Double.MIN_VALUE;
        for (Double v: values) {
            if (v > max) {
                max = v;
            }
        }
        
		//sort list, then min is at index 0
		List<Double> sortedValues = new ArrayList<Double>(values);
		sortedValues.removeIf(x -> x.isNaN());
		Collections.sort(sortedValues);
		sortedValues = sortedValues.stream().distinct().collect(Collectors.toList());

		
        //build inter value intervals and rank down the further from min-interval away
		double intervalStart=lowerBound;
		double intervalEnd=lowerBound;
		List<Interval> intervals = new ArrayList<Interval>();
		for (int i=0; i<sortedValues.size(); i++) {
			if(i==sortedValues.size()-1) {
				intervalEnd = upperBound;
			}
			else {
				double current = sortedValues.get(i);
				double next = sortedValues.get(i+1);
				intervalEnd = (next+current)/2;
			}

			if(intervalStart<target && intervalEnd >=target) {
				intervals.add(new Interval(intervalStart,target));
				intervals.add(new Interval(target,intervalEnd));
			}
			else {
				intervals.add(new Interval(intervalStart, intervalEnd));
			}
			intervalStart=intervalEnd;
		}
			
			
		double[] scores = new double[intervals.size()];
		double interval_score =0;
		for(int i=0;i<scores.length;i++) {
			if((max<target && intervals.get(i).getInf()>=target)) {
				interval_score = interval_score -(int)falloff;
				scores[i]=interval_score;
			}
			else if( max>target && intervals.get(i).getSup()<=target) {
				interval_score = interval_score +(int)falloff;
				scores[i]=interval_score;
			}
			else {
				interval_score++;
				scores[i]=interval_score;
			}	
		}
		double lowestScore = Math.abs(Arrays.stream(scores).min().getAsDouble());
		for(int i=0;i<scores.length;i++) {
			scores[i]=scores[i]+lowestScore;
		}
		
		int[] outputRanges = IntStream.range(0, intervals.size()).toArray();

		double sensitivity = falloff; //changing a value changes score by at most 2
		
		for (int i=0;i<scores.length;i++) {
			double intervalSize=intervals.get(i).getSize();
			scores[i]=intervalSize*Math.exp((epsilon*scores[i])/(2*sensitivity));
		}
		//scores = Arrays.stream(scores).map(score-> Math.exp((epsilon*score)/(2*sensitivity))).toArray();

		double new_total = Arrays.stream(scores).sum();
		double[] probabilities = Arrays.stream(scores).map(score-> score/new_total).toArray();
		//for(double score : probabilities)System.out.print(score+",");
		//System.out.println("");
		
		EnumeratedIntegerDistribution distribution = new EnumeratedIntegerDistribution(outputRanges, probabilities);
		Interval sampledInterval=intervals.get(distribution.sample());
		
		//System.out.println(sampledInterval.getInf()+" - "+sampledInterval.getSup());
		UniformIntegerDistribution finalOutput = new UniformIntegerDistribution((int)sampledInterval.getInf(),(int)sampledInterval.getSup());
		return finalOutput.sample();
	}

	private double avg_exp_falloff(Collection<Double> values, double epsilon, double lowerBound, double upperBound,
			double target, double falloff, int minimalSize) {
		if (falloff==0.0) return avg_exp_falloff(values, epsilon, lowerBound, upperBound, -10, 1, minimalSize);
		
		//if target is irelevant make sure that intervals are built without using falloff to avoid unneccessary noise
		if ((target<lowerBound || target>upperBound) && falloff!=1.0) {
			return avg_exp_falloff(values, epsilon, lowerBound, upperBound, target, 1, minimalSize);
		}
		
		List<Double> sortedValues = new ArrayList<Double>(values);
		sortedValues.removeIf(x -> x.isNaN());
		Collections.sort(sortedValues);
//		sortedValues = sortedValues.stream().distinct().collect(Collectors.toList());

		
		double intervalStart=lowerBound;
		double intervalEnd=lowerBound;
		
		double avg=sortedValues.stream().reduce(0.0, (a, b)-> a+b)/values.size();
		double maxChange=(upperBound-lowerBound)/(double)minimalSize;
		
		List<Interval> intervals = new ArrayList<Interval>();

		List<Interval> lowerIntervals 	= new ArrayList<Interval>();
		List<Interval> upperIntervals 	= new ArrayList<Interval>();
		Double lowerIntervalEnd  		= avg-maxChange/2;
		Double upperIntervalStart		= avg+maxChange/2;
		if(lowerIntervalEnd<target && upperIntervalStart>=target) {
			upperIntervals.add(new Interval(lowerIntervalEnd,target));
			upperIntervals.add(new Interval(target, upperIntervalStart));
		}
		else {
			upperIntervals.add(new Interval(lowerIntervalEnd,upperIntervalStart));
			
		}
		for(;lowerIntervalEnd>lowerBound && upperIntervalStart<upperBound; lowerIntervalEnd=lowerIntervalEnd-maxChange,upperIntervalStart=upperIntervalStart+maxChange) {
			if (lowerIntervalEnd>0)
				if(Math.max(lowerBound,lowerIntervalEnd-maxChange)<target && lowerIntervalEnd >=target) {
					lowerIntervals.add(new Interval(target,lowerIntervalEnd));
					lowerIntervals.add(new Interval(Math.max(lowerBound,lowerIntervalEnd-maxChange),target));
				}
				else {
					lowerIntervals.add(new Interval(Math.max(lowerBound,lowerIntervalEnd-maxChange),lowerIntervalEnd));
				}
			if (upperIntervalStart<upperBound)
				if(upperIntervalStart<target && Math.min(upperBound,upperIntervalStart+maxChange)>=target) {
					upperIntervals.add(new Interval(upperIntervalStart,target));
					upperIntervals.add(new Interval(target,Math.min(upperBound,upperIntervalStart+maxChange)));
				}
				else {
					upperIntervals.add(new Interval(upperIntervalStart,Math.min(upperBound,upperIntervalStart+maxChange)));
				}
		}
		Collections.reverse(lowerIntervals);
		intervals.addAll(lowerIntervals);
		intervals.addAll(upperIntervals);
		
		double[]scores=new double[intervals.size()];
		int currentScore=0;
		for(int i=0;i<scores.length;i++) {
			if(intervals.get(i).getInf()<=avg) {
				if(avg >target && intervals.get(i).getSup()<=target) {
					currentScore=currentScore+(int)falloff;
					scores[i]=currentScore;
				}
				else {
					currentScore++;
					scores[i]=currentScore;
				}
			}
			else {
				if(avg <=target && intervals.get(i).getInf()>=target) {
					currentScore=currentScore-(int)falloff;
					scores[i]=currentScore;
				}
				else {
					currentScore--;
					scores[i]=currentScore;
				}
			}
		}
		double lowestScore = Math.abs(Arrays.stream(scores).min().getAsDouble());
		for(int i=0;i<scores.length;i++) {
			scores[i]=scores[i]+lowestScore;
		}
		
		int[] outputRanges = IntStream.range(0, intervals.size()).toArray();
		double sensitivity =falloff; //changing a value changes score by at most 1
		
		for (int i=0;i<scores.length;i++) {
			double intervalSize=intervals.get(i).getSize();
			scores[i]=intervalSize*Math.exp((epsilon*scores[i])/(2*sensitivity));
		}

		double new_total = Arrays.stream(scores).sum();
		double[] probabilities = Arrays.stream(scores).map(score-> score/new_total).toArray();
		//for(double score : probabilities)System.out.print(score+",");
		//System.out.println("");
		
		EnumeratedIntegerDistribution distribution = new EnumeratedIntegerDistribution(outputRanges, probabilities);
		Interval sampledInterval=intervals.get(distribution.sample());
		
		//System.out.println(sampledInterval.getInf()+" - "+sampledInterval.getSup());
		UniformIntegerDistribution finalOutput = new UniformIntegerDistribution((int)sampledInterval.getInf(),(int)sampledInterval.getSup());
		return finalOutput.sample();
	}

	
	//subsample and aggregate on 10 buckets
	private double sum_sample_aggregate(Collection<Double> values, double epsilon, double lowerBound, double upperBound, int minimalSize) {
		//if(values.size()<10) return Double.NaN;
		List<Double> sortedValues = new ArrayList<Double>(values);
		Collections.sort(sortedValues);
		int noBuckets = (int)Math.ceil(values.size()/minimalSize);
		int bucketSize = (int) Math.ceil(values.size()/noBuckets);
		double sensitivity = (upperBound*values.size()-lowerBound*values.size())/minimalSize;
		if(upperBound == lowerBound) sensitivity =1;
		List<Double[]> partitions =new ArrayList<Double[]>();
		for (int i=0;i<noBuckets;i++){
			Double[] partition = new Double[bucketSize];
			for(int j=0;j<bucketSize;j++) {
				if(sortedValues.size()<=0) {
					partition[j]=0.0;
					continue;
				}
			    int rnd = new Random().nextInt(sortedValues.size());
			    partition[j]=sortedValues.get(rnd);
			    sortedValues.remove(rnd);
			}
			partitions.add(partition);
		}
		
		Double[] partitionResults = new Double[partitions.size()];
		for (int i=0;i<partitions.size();i++) {
			partitionResults[i]=Arrays.stream(partitions.get(i)).reduce(0.0,(a,b) -> a+b)*noBuckets;
		}
		
		//windsorized mean
		//Arrays.sort(partitionResults);
		//partitionResults[0]=partitionResults[1];
		//partitionResults[9]=partitionResults[8];
		Arrays.sort(partitionResults);
		int tenPercent=noBuckets/10;
		for(int i=0;i<tenPercent;i++) {
			partitionResults[i]=partitionResults[tenPercent];
			partitionResults[partitionResults.length-1-i]=partitionResults[partitionResults.length-1-tenPercent];
		}
		
		
		LaplaceDistribution laplace = new LaplaceDistribution(0,(sensitivity/epsilon));
		//System.out.println( Arrays.stream(partitionResults).reduce(0.0,(a,b) -> a+b)/partitionResults.length + laplace.sample());
		return (Arrays.stream(partitionResults).reduce(0.0,(a,b) -> a+b)/partitionResults.length) + laplace.sample();
	}

	//subsample and aggregate on 10 buckets using falloff and target value
	private double sum_sample_aggregate_falloff(Collection<Double> values, double epsilon, double lowerBound,
			double upperBound, int minimalSize, double falloff, double target) {
		double sensitivity = (upperBound*values.size()-lowerBound*values.size())/minimalSize;
		if(upperBound == lowerBound) sensitivity =1;

		//if(values.size()<10) return Double.NaN;
		List<Double> sortedValues = new ArrayList<Double>(values);
		Collections.sort(sortedValues);
		List<Double[]> partitions =new ArrayList<Double[]>();
		int bucketSize = (int) Math.ceil(values.size()/10.0);

		for (int i=0;i<10;i++){
			Double[] partition = new Double[bucketSize];
			for(int j=0;j<bucketSize;j++) {
				if(sortedValues.size()<=0) {
					partition[j]=0.0;
					continue;
				}
			    int rnd = new Random().nextInt(sortedValues.size());
			    partition[j]=sortedValues.get(rnd);
			    sortedValues.remove(rnd);
			}
			partitions.add(partition);
		}
		
		Double[] partitionResults = new Double[partitions.size()];
		for (int i=0;i<partitions.size();i++) {
			partitionResults[i]=Arrays.stream(partitions.get(i)).reduce(0.0,(a,b) -> a+b)*partitions.size();
		}
		
		//windsorized mean
		Arrays.sort(partitionResults);
		partitionResults[0]=partitionResults[1];
		partitionResults[9]=partitionResults[8];
		//now determine average based on samples
		return avg_exp_falloff(new ArrayList<Double>(Arrays.asList(partitionResults)), epsilon, lowerBound, upperBound, target, falloff, minimalSize);
	}
	
	//subsample and aggregate on 10 buckets using falloff and target value
	private double derived_subsample_and_aggregate(Collection<Double> values, String boundaryEstimation, double epsilon, double lowerBound,
			double upperBound, int minimalSize, double falloff, double target) {
		//System.out.println(lowerBound+" , "+upperBound);
		
		//get output range of aggregation -> 0-100 as sensitivity
		//aggregate using stable statistics

		//if(values.size()<10) return Double.NaN;
		List<Double> sortedValues = new ArrayList<Double>(values);
		Collections.sort(sortedValues);
		List<Double[]> partitions =new ArrayList<Double[]>();
		int noBuckets =(int)Math.ceil(values.size()/20);
		if(values.size()<noBuckets)noBuckets=values.size();
		//if(values.size()/2.0>=50.0)
		//	noBuckets = (int) Math.ceil(values.size()/2.0);
		//if lower than 5 or so, use smth else instead
		//else noBuckets = values.size();
		int bucketSize = (int) Math.ceil(values.size()/noBuckets);
		double sensitivity = (100.0)/noBuckets;
		//if(upperBound == lowerBound) sensitivity =1;


		for (int i=0;i<noBuckets;i++){
			Double[] partition = new Double[bucketSize];
			for(int j=0;j<bucketSize;j++) {
				if(sortedValues.size()<=0) {
					partition[j]=0.0;
					continue;
				}
			    int rnd = new Random().nextInt(sortedValues.size());
			    partition[j]=sortedValues.get(rnd);
			    sortedValues.remove(rnd);
			}
			partitions.add(partition);
		}
		
		Double[] partitionResults = new Double[partitions.size()];
		for (int i=0;i<partitions.size();i++) {
			partitionResults[i]=(Arrays.stream(partitions.get(i)).reduce(0.0,(a,b) -> a+b)/partitions.get(i).length)*100;
		}
		Bounds bounds = BoundaryEstimator.estimateBoundsMinMax(Arrays.stream(partitionResults).collect(Collectors.toSet()));
		//System.out.println(bounds.upperBound+", "+bounds.lowerBound+", "+noBuckets +", "+values.size());


		//windsorized mean
		Arrays.sort(partitionResults);
		int tenPercent=noBuckets/10;
		for(int i=0;i<=tenPercent;i++) {
			partitionResults[i]=partitionResults[tenPercent];
			partitionResults[partitionResults.length-1-i]=partitionResults[partitionResults.length-1-tenPercent];
		}
		sensitivity = (partitionResults[partitionResults.length-1]-partitionResults[0])/noBuckets;
		//System.out.println(partitionResults[0]+" , "+partitionResults[partitionResults.length-1]);
		if (noBuckets==1) sensitivity=100.0;
		//now determine average based on samples
		LaplaceDistribution laplace = new LaplaceDistribution(0,(sensitivity/epsilon));
		//System.out.println( Arrays.stream(partitionResults).reduce(0.0,(a,b) -> a+b)/partitionResults.length + laplace.sample());
		return (Arrays.stream(partitionResults).reduce(0.0,(a,b) -> a+b)/partitionResults.length) + laplace.sample();	}
}
