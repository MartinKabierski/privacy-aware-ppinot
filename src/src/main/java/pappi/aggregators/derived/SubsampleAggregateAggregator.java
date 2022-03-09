package pappi.aggregators.derived;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.commons.math3.distribution.LaplaceDistribution;

import pappi.aggregators.NoisyAggregator;
import pappi.boundary.BoundaryEstimator;
import pappi.boundary.Bounds;


//TODO add no of buckets as parameter
//TODO does this even work?

//TODO rename NoisyAggregator interface, as it is also needed for derivedm easures, that do not "aggregate"
@Deprecated
public class SubsampleAggregateAggregator implements NoisyAggregator{

	
    public static final String SUBSAMPLE_PERC = "Subsample_percentage";
	
	public SubsampleAggregateAggregator() {
	}

	public double aggregate(String function, Collection<Double> values, double epsilon, double lowerBound, double upperBound) {
		if (function == SUBSAMPLE_PERC) {
			return percentage(values, epsilon);
		}
		else return Double.NaN;
	}
	
	public double percentage(Collection<Double> values, double epsilon) {
		//System.out.println(lowerBound+" , "+upperBound);
		
		//get output range of aggregation -> 0-100 as sensitivity
		//aggregate using stable statistics

		//if(values.size()<10) return Double.NaN;
		List<Double> sortedValues = new ArrayList<Double>(values);
		Collections.sort(sortedValues);
		List<Double[]> partitions =new ArrayList<Double[]>();
		int noBuckets =10;
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
		//System.out.println(Arrays.toString(partitionResults));
		int tenPercent=noBuckets/10;
		//for(int i=0;i<=tenPercent;i++) {
		//	partitionResults[i]=partitionResults[tenPercent];
		//	partitionResults[partitionResults.length-1-i]=partitionResults[partitionResults.length-1-tenPercent];
		//}
		//sensitivity = (partitionResults[partitionResults.length-1]-partitionResults[0])/noBuckets;
		//System.out.println(partitionResults[0]+" , "+partitionResults[partitionResults.length-1]);
		if (noBuckets==1) sensitivity=100.0;
		//now determine average based on samples
		LaplaceDistribution laplace = new LaplaceDistribution(0,(sensitivity/epsilon));
		//System.out.println( Arrays.stream(partitionResults).reduce(0.0,(a,b) -> a+b)/partitionResults.length + laplace.sample());
		Double result = (Arrays.stream(partitionResults).reduce(0.0,(a,b) -> a+b)/partitionResults.length) + laplace.sample();
		result = Math.max(0, Math.min(result, 100));
		
		return (Arrays.stream(partitionResults).reduce(0.0,(a,b) -> a+b)/partitionResults.length) + laplace.sample();	
		
	}
}
