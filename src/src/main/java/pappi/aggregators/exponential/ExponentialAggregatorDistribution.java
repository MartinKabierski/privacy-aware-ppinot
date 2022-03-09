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

import org.apache.commons.math3.geometry.euclidean.oned.Interval;

import pappi.aggregators.OutputDistribution;

//TODO refactor this beast

public class ExponentialAggregatorDistribution implements OutputDistribution{ //TODO should implement an interface
	
	public static final String SUM_EXP = "Sum_exp";//exponential interval based
    public static final String AVG_EXP = "Average_exp";//exponential interval based
    public static final String MAX_EXP = "Maximum_exp";//exponential interval based
    public static final String MIN_EXP = "Minimum_exp";//exponential interval based
    
	public ExponentialAggregatorDistribution() {
	}
	
	public Map<Double,Double> getOutputProbability(String function, Collection<Double> values, double epsilon, double lowerBound, double upperBound) {
		if (values.size()<=1) {
    		Map<Double,Double> nanMap = new HashMap();
    		nanMap.put(Double.NaN, 1.0);
    		return nanMap;
		}

		if (function == SUM_EXP) {
			if (lowerBound==upperBound) {
				Map<Double,Double> oneVarMap = new HashMap();
	    		oneVarMap.put(lowerBound*values.size(), 1.0);
	    		return oneVarMap;
			}
			return sum(values, epsilon, lowerBound, upperBound);
		}
		else if (function == AVG_EXP) {
			if (lowerBound==upperBound) {
				Map<Double,Double> oneVarMap = new HashMap();
	    		oneVarMap.put(lowerBound, 1.0);
	    		return oneVarMap;
			}
			return mean(values, epsilon, lowerBound, upperBound);
		}
		else if (function == MAX_EXP) {
			if (lowerBound==upperBound) {
				Map<Double,Double> oneVarMap = new HashMap();
	    		oneVarMap.put(lowerBound, 1.0);
	    		return oneVarMap;
			}
			return max(values, epsilon, lowerBound, upperBound);
		}
		else if (function == MIN_EXP) {
			if (lowerBound==upperBound) {
				Map<Double,Double> oneVarMap = new HashMap();
	    		oneVarMap.put(lowerBound, 1.0);
	    		return oneVarMap;
			}
			return min(values, epsilon, lowerBound, upperBound);
		}
		else {
    		Map<Double,Double> nanMap = new HashMap();
    		nanMap.put(Double.NaN, 1.0);
    		return nanMap;
		}
	}


	private Map<Double,Double> min(Collection<Double> values, double epsilon, double lowerBound, double upperBound) {
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
		
		//since we get distribution directly from itnervals and numerical proing, we do not scale by interval size
		for (int i=0;i<scores.length;i++) {
			double intervalSize=intervals.get(i).getSize();
			scores[i]=/*intervalSize**/Math.exp((epsilon*scores[i])/(2*sensitivity));
		}
		//scores = Arrays.stream(scores).map(score-> Math.exp((epsilon*score)/(2*sensitivity))).toArray();

		
		double new_total = Arrays.stream(scores).sum();
		double[] probabilities = Arrays.stream(scores).map(score-> score/new_total).toArray();
		//Arrays.stream(probabilities).forEach(x->System.out.print(x+","));
		//System.out.println();
		//for(double score : probabilities)System.out.print(score+",");
		//System.out.println("");
		
		return convertToMap(intervals,probabilities, lowerBound, upperBound, values.size());
		
		/*
		EnumeratedIntegerDistribution distribution = new EnumeratedIntegerDistribution(outputRanges, probabilities);
		Interval sampledInterval=intervals.get(distribution.sample());
		
		//System.out.println(sampledInterval.getInf()+" - "+sampledInterval.getSup());
		UniformIntegerDistribution finalOutput =
				new UniformIntegerDistribution((int)sampledInterval.getInf(),(int)sampledInterval.getSup());
		return finalOutput.sample();*/
	}


	private Map<Double,Double> max(Collection<Double> values, double epsilon, double lowerBound, double upperBound) {
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
			scores[i]=/*intervalSize**/Math.exp((epsilon*scores[i])/(2*sensitivity));
		}
		//scores = Arrays.stream(scores).map(score-> Math.exp((epsilon*score)/(2*sensitivity))).toArray();

		double new_total = Arrays.stream(scores).sum();
		double[] probabilities = Arrays.stream(scores).map(score-> score/new_total).toArray();
		//for(double score : probabilities)System.out.print(score+",");
		//System.out.println("");
		
		return convertToMap(intervals,probabilities, lowerBound, upperBound, values.size());

		/*
		EnumeratedIntegerDistribution distribution = new EnumeratedIntegerDistribution(outputRanges, probabilities);
		Interval sampledInterval=intervals.get(distribution.sample());
		
		//System.out.println(sampledInterval.getInf()+" - "+sampledInterval.getSup());
		UniformIntegerDistribution finalOutput = new UniformIntegerDistribution((int)sampledInterval.getInf(),(int)sampledInterval.getSup());
		return finalOutput.sample();*/
	}


	private Map<Double,Double> mean(Collection<Double> values, double epsilon, double lowerBound, double upperBound
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
			scores[i]=/*intervalSize**/Math.exp((epsilon*scores[i])/(2*sensitivity));
		}

		double new_total = Arrays.stream(scores).sum();
		double[] probabilities = Arrays.stream(scores).map(score-> score/new_total).toArray();
		//for(double score : probabilities)System.out.print(score+",");
		//System.out.println("");
		
		return convertToMap(intervals,probabilities, lowerBound, upperBound, values.size());

		/*
		EnumeratedIntegerDistribution distribution = new EnumeratedIntegerDistribution(outputRanges, probabilities);
		Interval sampledInterval=intervals.get(distribution.sample());
		
		//System.out.println(sampledInterval.getInf()+" - "+sampledInterval.getSup());
		UniformIntegerDistribution finalOutput = new UniformIntegerDistribution((int)sampledInterval.getInf(),(int)sampledInterval.getSup());
		return finalOutput.sample();*/
	}


	private Map<Double,Double> sum(Collection<Double> values, double epsilon, double lowerBound, double upperBound
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
			scores[i]=/*intervalSize**/Math.exp((epsilon*scores[i])/(2*sensitivity));
		}
		double new_total = Arrays.stream(scores).sum();
		double[] probabilities = Arrays.stream(scores).map(score-> score/new_total).toArray();
		//for(double score : probabilities)System.out.print(score+",");
		//System.out.println("");

		return convertToMap(intervals,probabilities, lowerBound*values.size(), upperBound*values.size(), values.size());

		/*
		EnumeratedIntegerDistribution distribution = new EnumeratedIntegerDistribution(outputRanges, probabilities);
		Interval sampledInterval=intervals.get(distribution.sample());
		
		UniformIntegerDistribution finalOutput = new UniformIntegerDistribution((int)sampledInterval.getInf(),(int)sampledInterval.getSup());
		return finalOutput.sample();*/
	}

	private Map<Double, Double> convertToMap(List<Interval> intervals, double[] intervalCumulativeProbabilities, double lowerBound, double upperBound, int valueCount) {
		double smallestIntervalSize = Integer.MAX_VALUE;
		for (Interval i : intervals) {
			if (i.getSize() < smallestIntervalSize) {
				smallestIntervalSize = i.getSize();
			}
		}
		
		double delta = (upperBound-lowerBound)/100.0;
		Map<Double, Double> probabilities = new HashMap<Double, Double>();
		/*for (int j=0;j<intervals.size();j++) {
			Interval interval = intervals.get(j);
			double x = interval.getInf();
			while(x<interval.getSup()) {
				probabilities.put(x, intervalCumulativeProbabilities[j]);
				x += delta;
			}
		}*/
		double x = lowerBound;
		int intervalsIndex = 0;
		Interval currentInterval = intervals.get(intervalsIndex);
		while (x<= upperBound) {
			if (x > currentInterval.getSup()){
				//probabilities.put(currentInterval.getSup(), intervalCumulativeProbabilities[intervalsIndex]);
				intervalsIndex++;
				currentInterval = intervals.get(intervalsIndex);
				//dirty hack so that interval edges are properly considered
				//probabilities.put(currentInterval.getInf()+0.000000001, intervalCumulativeProbabilities[intervalsIndex]);
			}
			else {
				probabilities.put(x, intervalCumulativeProbabilities[intervalsIndex]);
				x += delta;
			}
		}
		
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
		return probabilities;
	}

}
