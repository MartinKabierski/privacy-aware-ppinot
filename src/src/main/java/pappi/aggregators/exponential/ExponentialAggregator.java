package pappi.aggregators.exponential;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.apache.commons.math3.geometry.euclidean.oned.Interval;

import pappi.aggregators.NoisyAggregator;

//TODO refactor this beast
//TODO check if these results need to be clamped, i.e for 
//		mean, min, max result = Math.max(lowerBound, Math.min(result, upperBound));
//		sum result = Math.max(lowerBound*values.size(), Math.min(result, upperBound*values.size()));

public class ExponentialAggregator implements NoisyAggregator{
	
	public static final String SUM_EXP = "Sum_exp";//exponential interval based
    public static final String AVG_EXP = "Average_exp";//exponential interval based
    public static final String MAX_EXP = "Maximum_exp";//exponential interval based
    public static final String MIN_EXP = "Minimum_exp";//exponential interval based
    
	public ExponentialAggregator() {
	}

	public double aggregate(String function, Collection<Double> values, double epsilon, double lowerBound, double upperBound) {
		if (function == SUM_EXP) {
			if (lowerBound==upperBound) {
				return lowerBound*values.size();
			}
			return sum(values, epsilon, lowerBound, upperBound);
		}
		else if (function == AVG_EXP) {
			if (lowerBound==upperBound) {
				return lowerBound;
			}
			return mean(values, epsilon, lowerBound, upperBound);
		}
		else if (function == MAX_EXP) {
			if (lowerBound==upperBound) {
				return lowerBound;
			}
			return max(values, epsilon, lowerBound, upperBound);
		}
		else if (function == MIN_EXP) {
			if (lowerBound==upperBound) {
				return lowerBound;
			}
			return min(values, epsilon, lowerBound, upperBound);
		}
		else return Double.NaN;
	}

	private double min(Collection<Double> values, double epsilon, double lowerBound, double upperBound) {
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


	private double max(Collection<Double> values, double epsilon, double lowerBound, double upperBound) {
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
				intervalEnd = (next+current)/2.0;
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
		
		//since we get distribution directly from itnervals and numerical proing, we do not scale by interval size
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


	private double mean(Collection<Double> values, double epsilon, double lowerBound, double upperBound
			) {
		List<Double> sortedValues = new ArrayList<Double>(values);
		sortedValues.removeIf(x -> x.isNaN());
		Collections.sort(sortedValues);
//		sortedValues = sortedValues.stream().distinct().collect(Collectors.toList());
		
		double avg=sortedValues.stream().reduce(0.0, (a, b)-> a+b)/values.size();
		double maxChange=(upperBound-lowerBound)/values.size();
		
		List<Interval> intervals = new ArrayList<Interval>();

		List<Interval> lowerIntervals 	= new ArrayList<Interval>();
		List<Interval> upperIntervals 	= new ArrayList<Interval>();
		Double lowerIntervalEnd  		= avg-maxChange/2;
		Double upperIntervalStart		= avg+maxChange/2;
		Interval averageInterval = new Interval(Math.max(lowerBound,lowerIntervalEnd),Math.min(upperBound, upperIntervalStart));
		//Interval averageInterval = new Interval(lowerIntervalEnd,upperIntervalStart);
		for(;lowerIntervalEnd>lowerBound || upperIntervalStart<upperBound; lowerIntervalEnd=lowerIntervalEnd-maxChange,upperIntervalStart=upperIntervalStart+maxChange) {
			if (lowerIntervalEnd>lowerBound)
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
		
		//since we get distribution directly from itnervals and numerical proing, we do not scale by interval size
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


	private double sum(Collection<Double> values, double epsilon, double lowerBound, double upperBound
			) {
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
		Double lowerIntervalEnd  		= sum-maxChange/2.0;
		Double upperIntervalStart		= sum+maxChange/2.0;
		Interval averageInterval = new Interval(Math.max(lowerBound*values.size(),lowerIntervalEnd),Math.min(upperBound*values.size(), upperIntervalStart));
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
		for(Interval i : intervals) {
			if (i.getInf()<0)
			System.out.print(i.getInf()+"-"+i.getSup()+"; "); 
		}
		
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
		
		//since we get distribution directly from intervals and numerical proing, we do not scale by interval size
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

}
