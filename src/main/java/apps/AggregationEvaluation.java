package apps;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.ParetoDistribution;
import org.apache.commons.math3.distribution.PoissonDistribution;

import pappi.aggregators.PrivacyAwareAggregator;
import pappi.boundary.BoundaryEstimator;


public class AggregationEvaluation {
	
	public static String GAUSSIAN = "gaussian";
	public static String PARETO = "pareto";
	public String POISSON = "poisson";
	
	public static void main(String[] args) throws IOException {

		
		//System.out.println(values.toString());
		//System.out.println("Min: "+values.stream().min(Double::compare));
		//System.out.println("Min: "+values.stream().max(Double::compare));
		//System.out.println("Avg: "+values.stream().reduce(0.0, (a, b)-> a+b)/values.size());//values.size());
		//System.out.println("Sum: "+values.stream().reduce(0.0, (a, b)-> a+b));//values.size());
		
		//init result file
		BufferedWriter writer = new BufferedWriter(new FileWriter("evaluation_params.csv"));
		writer.write("Distribution;Function;Method;BoundEstimation;Epsilon;Target;Falloff Factor;Extension Factor;Minimal Size;NoOfValues;Result;Original\n");
		
	    List<EvaluationDistribution> distributions= new ArrayList<>();
	    String[] distribution = {"gaussian","pareto","poisson"};
	    for(String dist : distribution)
	    	for(EvaluationDistribution x : buildSamples(dist))
	    		distributions.add(x);


		//evaluate all methods under different parameter
		PrivacyAwareAggregator aggregator = new PrivacyAwareAggregator();
		
		int repetitions = 200;
		double[] epsilons = {1.0, 0.1, 0.01};
		
		//targets will be evaluated on gaussian with 100 samples
		double[] avg_targets = {-100,95};
		double[] sum_targets = {-100,9995};
		double[] min_targets = {-100,50};
		double[] max_targets = {-100,150};
		double[] falloffs = {1,5,10,20};
		double[] extension_factors = {1.0, 1.3, 1.6};
		int[] minimal_sizes = {1};
		String[] mechanisms = {
				PrivacyAwareAggregator.AVG_LAP,
				PrivacyAwareAggregator.AVG_LDP,
				PrivacyAwareAggregator.AVG_EXP,
				PrivacyAwareAggregator.AVG_EXP_FALLOFF,
				PrivacyAwareAggregator.SUM_LAP,
				PrivacyAwareAggregator.SUM_LDP,
				PrivacyAwareAggregator.SUM_EXP,
				PrivacyAwareAggregator.SUM_EXP_FALLOFF,
				PrivacyAwareAggregator.SUM_AGGREGATE,
				//PrivacyAwareAggregator.SUM_AGGREGATE_FALLOFF,
				PrivacyAwareAggregator.MIN_LAP,
				PrivacyAwareAggregator.MIN_LDP,
				PrivacyAwareAggregator.MIN_EXP,
				PrivacyAwareAggregator.MIN_EXP_FALLOFF,
				PrivacyAwareAggregator.MAX_LAP,
				PrivacyAwareAggregator.MAX_LDP,
				PrivacyAwareAggregator.MAX_EXP,
				PrivacyAwareAggregator.MAX_EXP_FALLOFF,
		};
		
		String[] avgMechanisms = {
				PrivacyAwareAggregator.AVG_LAP,
				PrivacyAwareAggregator.AVG_LDP,
				PrivacyAwareAggregator.AVG_EXP,
				PrivacyAwareAggregator.AVG_EXP_FALLOFF
		};
		String[] sumMechanisms = {
				PrivacyAwareAggregator.SUM_LAP,
				PrivacyAwareAggregator.SUM_LDP,
				PrivacyAwareAggregator.SUM_EXP,
				PrivacyAwareAggregator.SUM_EXP_FALLOFF,
				PrivacyAwareAggregator.SUM_AGGREGATE,
				PrivacyAwareAggregator.SUM_AGGREGATE_FALLOFF
		};
		String[] minMechanisms = {
				PrivacyAwareAggregator.MIN_LAP,
				PrivacyAwareAggregator.MIN_LDP,
				PrivacyAwareAggregator.MIN_EXP,
				PrivacyAwareAggregator.MIN_EXP_FALLOFF
		};
		String[] maxMechanisms = {
				PrivacyAwareAggregator.MAX_LAP,
				PrivacyAwareAggregator.MAX_LDP,
				PrivacyAwareAggregator.MAX_EXP,
				PrivacyAwareAggregator.MAX_EXP_FALLOFF
		};
		String[] boundaryEstimators = {
				BoundaryEstimator.MINMAX,
				BoundaryEstimator.EXTEND
		};
		
		
		for (EvaluationDistribution dist : distributions) {
			for (String mechanism : mechanisms) {
				System.out.println(dist.distribution+ " - Evaluating "+mechanism+" on "+dist.samples.size()+" samples");
				for (String boundaryEstimation : boundaryEstimators) {
					for(double epsilon : epsilons){
						double[] targets= {};
						double original=-1;
						String function="";
						if (Arrays.stream(avgMechanisms).anyMatch(mechanism::equals)) {
							targets=dist.avgTargets;
							original = dist.samples.stream().reduce(0.0, (a, b)-> a+b)/dist.samples.size();
							function="avg";
						}
						if (Arrays.stream(sumMechanisms).anyMatch(mechanism::equals)) {
							targets=dist.sumTargets;
							original = dist.samples.stream().reduce(0.0, (a, b)-> a+b);
							function="sum";
						}
						if (Arrays.stream(minMechanisms).anyMatch(mechanism::equals)) {
							targets=dist.minTargets;
							original = dist.samples.stream().min(Double::compare).get();
							function="min";

						}
						if (Arrays.stream(maxMechanisms).anyMatch(mechanism::equals)) {
							targets=dist.maxTargets;
							original = dist.samples.stream().max(Double::compare).get();
							function="max";
						}
						for (double target : targets) {
							for (double falloff : falloffs) {
								for(double extension_factor : extension_factors) {
									for (int minimalSize : minimal_sizes) {
										for (int i=0;i<repetitions;i++) {
											List<Double>valueCopy = new ArrayList<Double>(dist.samples);
											double result = aggregator.aggregate(valueCopy, mechanism, boundaryEstimation, epsilon, target, falloff, extension_factor, minimalSize);
											writer.write(String.join(";", 
													dist.distribution,
													function,
													mechanism,
													boundaryEstimation,
													Double.toString(epsilon),
													Double.toString(target),
													Double.toString(falloff),
													Double.toString(extension_factor),
													Double.toString(minimalSize),
													Double.toString(dist.samples.size()),
													Double.toString(result),
													Double.toString(original)));
											writer.write("\n");
											
										}
									}
								}
							}
						}
					}
				}
			}
		}
		
		
		Collection<Double> values =distributions.get(0).samples;
		Collection<Double> binary =new ArrayList();
		for (double value : values) {
			if(value<values.stream().reduce(0.0, (a, b)-> a+b)/values.size())
			binary.add(1.0);
			else binary.add(0.0);
		}
		double derived_original = (binary.stream().reduce(0.0,(a,b) -> a+b)/binary.size())*100;
		System.out.println(derived_original);
		for(double epsilon : epsilons) {
			for(int minimalSize : minimal_sizes) {
				for (int i=0;i<repetitions;i++) {
					double result = aggregator.aggregate(binary, PrivacyAwareAggregator.PERCENTAGE, BoundaryEstimator.MINMAX, epsilon, -100.0, 20.0, 1.3, minimalSize);
					//System.out.println(result);
					writer.write(String.join(";", 
							"binary",
							"derived",
							PrivacyAwareAggregator.PERCENTAGE,
							BoundaryEstimator.MINMAX,
							Double.toString(epsilon),
							Double.toString(-100.0),
							Double.toString(20.0),
							Double.toString(1.3),
							Double.toString(minimalSize),
							Double.toString(binary.size()),
							Double.toString(result),
							Double.toString(derived_original)));
					writer.write("\n");
				}	
			}
		}
		
		values =distributions.get(1).samples;
		binary =new ArrayList();
		for (double value : values) {
			if(value<values.stream().reduce(0.0, (a, b)-> a+b)/values.size())
			binary.add(1.0);
			else binary.add(0.0);
		}
		derived_original = (binary.stream().reduce(0.0,(a,b) -> a+b)/binary.size())*100;
		System.out.println(derived_original);
		for(double epsilon : epsilons) {
			for(int minimalSize : minimal_sizes) {
				for (int i=0;i<repetitions;i++) {
					double result = aggregator.aggregate(binary, PrivacyAwareAggregator.PERCENTAGE, BoundaryEstimator.MINMAX, epsilon, -100.0, 20.0, 1.3, minimalSize);
					//System.out.println(result);
					writer.write(String.join(";", 
							"binary",
							"derived",
							PrivacyAwareAggregator.PERCENTAGE,
							BoundaryEstimator.MINMAX,
							Double.toString(epsilon),
							Double.toString(-100.0),
							Double.toString(20.0),
							Double.toString(1.3),
							Double.toString(minimalSize),
							Double.toString(binary.size()),
							Double.toString(result),
							Double.toString(derived_original)));
					writer.write("\n");
				}	
			}
		}
		
		//BufferedWriter writer = new BufferedWriter(new FileWriter("derived.csv"));
		//finally derived measure evaluation
		values =distributions.get(2).samples;
		binary =new ArrayList();
		for (double value : values) {
			if(value<values.stream().reduce(0.0, (a, b)-> a+b)/values.size())
			binary.add(1.0);
			else binary.add(0.0);
		}
		derived_original = (binary.stream().reduce(0.0,(a,b) -> a+b)/binary.size())*100;
		System.out.println(derived_original);
		for(double epsilon : epsilons) {
			for(int minimalSize : minimal_sizes) {
				for (int i=0;i<repetitions;i++) {
					double result = aggregator.aggregate(binary, PrivacyAwareAggregator.PERCENTAGE, BoundaryEstimator.MINMAX, epsilon, -100.0, 20.0, 1.3, minimalSize);
					//System.out.println(result);
					writer.write(String.join(";", 
							"binary",
							"derived",
							PrivacyAwareAggregator.PERCENTAGE,
							BoundaryEstimator.MINMAX,
							Double.toString(epsilon),
							Double.toString(-100.0),
							Double.toString(20.0),
							Double.toString(1.3),
							Double.toString(minimalSize),
							Double.toString(binary.size()),
							Double.toString(result),
							Double.toString(derived_original)));
					writer.write("\n");
				}	
			}
		}
		
		values =distributions.get(3).samples;
		binary =new ArrayList();
		for (double value : values) {
			if(value<values.stream().reduce(0.0, (a, b)-> a+b)/values.size())
			binary.add(1.0);
			else binary.add(0.0);
		}
		derived_original = (binary.stream().reduce(0.0,(a,b) -> a+b)/binary.size())*100;
		System.out.println(derived_original);
		for(double epsilon : epsilons) {
			for(int minimalSize : minimal_sizes) {
				for (int i=0;i<repetitions;i++) {
					double result = aggregator.aggregate(binary, PrivacyAwareAggregator.PERCENTAGE, BoundaryEstimator.MINMAX, epsilon, -100.0, 20.0, 1.3, minimalSize);
					//System.out.println(result);
					writer.write(String.join(";", 
							"binary",
							"derived",
							PrivacyAwareAggregator.PERCENTAGE,
							BoundaryEstimator.MINMAX,
							Double.toString(epsilon),
							Double.toString(-100.0),
							Double.toString(20.0),
							Double.toString(1.3),
							Double.toString(minimalSize),
							Double.toString(binary.size()),
							Double.toString(result),
							Double.toString(derived_original)));
					writer.write("\n");
				}	
			}
		}
		
		writer.close();
		
	}
	
	public static List<EvaluationDistribution> buildSamples(String function){
		List<EvaluationDistribution> resultList = new ArrayList<EvaluationDistribution>();
		
		Collection<Double> ten = new ArrayList<Double>();
		Collection<Double> fifty = new ArrayList<Double>();
		Collection<Double> hundred = new ArrayList<Double>();
		Collection<Double> twohundred = new ArrayList<Double>();

		if (function.equals("gaussian")) {
			NormalDistribution normal = new NormalDistribution(100, 25);
			normal.reseedRandomGenerator(123);
			double MEAN = 100;
			double VARIANCE = 25;
			Random fRandom = new Random(1234);

			for (int idx = 1; idx <= 200; ++idx){
		    	//double value = MEAN + fRandom.nextGaussian() * VARIANCE;
				double value = normal.sample();
		    	value = Math.max(0,(int)value);
		    	if(idx<=10)ten.add(value);
		    	if(idx<=50)fifty.add(value);
		    	if(idx<=100)hundred.add(value);
		    	twohundred.add(value);
		    }
			resultList.add(new EvaluationDistribution(ten, "gaussian"));
			resultList.add(new EvaluationDistribution(fifty, "gaussian"));
			resultList.add(new EvaluationDistribution(hundred, "gaussian"));
			resultList.add(new EvaluationDistribution(twohundred, "gaussian"));

			
		}
		if (function.equals("pareto")) {
			ParetoDistribution pareto = new ParetoDistribution(10,2);
			pareto.reseedRandomGenerator(123);
			for (int idx = 1; idx <= 200; ++idx){
		    	double value = pareto.sample();
		    	value = Math.max(0,(int)value);
		    	if(idx<=10)ten.add(value);
		    	if(idx<=50)fifty.add(value);
		    	if(idx<=100)hundred.add(value);
		    	twohundred.add(value);

		    }
			resultList.add(new EvaluationDistribution(ten, "pareto"));
			resultList.add(new EvaluationDistribution(fifty, "pareto"));
			resultList.add(new EvaluationDistribution(hundred, "pareto"));
			resultList.add(new EvaluationDistribution(twohundred, "pareto"));

		}
		if (function.equals("poisson")) {
			PoissonDistribution poisson = new PoissonDistribution(5);
			poisson.reseedRandomGenerator(123);
			for (int idx = 1; idx <= 200; ++idx){
		    	double value = poisson.sample();
		    	value = Math.max(0,(int)value);
		    	if(idx<=10)ten.add(value);
		    	if(idx<=50)fifty.add(value);
		    	if(idx<=100)hundred.add(value);
		    	twohundred.add(value);

		    }
			resultList.add(new EvaluationDistribution(ten, "poisson"));
			resultList.add(new EvaluationDistribution(fifty, "poisson"));
			resultList.add(new EvaluationDistribution(hundred, "poisson"));
			resultList.add(new EvaluationDistribution(twohundred, "poisson"));

		}
		return resultList;
		
	}

}


class EvaluationDistribution {
	String distribution;
	Collection<Double> samples;
	double[] avgTargets;
	double[] sumTargets;
	double[] minTargets;
	double[] maxTargets;


	public EvaluationDistribution(Collection<Double> samples, String distribution) {
		this.distribution = distribution;
		this.samples = samples;
		double avg=samples.stream().reduce(0.0, (a, b)-> a+b)/samples.size();
		double sum=samples.stream().reduce(0.0, (a, b)-> a+b);
		double min=samples.stream().min(Double::compare).get();
		double max=samples.stream().max(Double::compare).get();
		System.out.println("AVG:"+avg+", SUM:"+sum +", MIN:"+min+", MAX:"+max);
		if(distribution.equals("gaussian")) {
			double[] avgGoals = {-100,avg-10.0, avg+10.0};
			double[] sumGoals = {-100,sum-100.0, sum+100.0};
			double[] minGoals = {-100,min-10.0,min+10.0};
			double[] maxGoals = {-100,max-10.0,max+10.0};
			this.setTargets(avgGoals, sumGoals, minGoals, maxGoals);
		}
		if(distribution.equals("pareto")) {
			double[] avgGoals = {-100,avg-5.0, avg+5.0};
			double[] sumGoals = {-100,sum-50.0, sum+50.0};
			double[] minGoals = {-100,min-5.0,min+5.0};
			double[] maxGoals = {-100,max-5.0,max+5.0};
			this.setTargets(avgGoals, sumGoals, minGoals, maxGoals);
		}
		if(distribution.equals("poisson")) {
			double[] avgGoals = {-100,avg-2.0, avg+2.0};
			double[] sumGoals = {-100,sum-20.0, sum+20.0};
			double[] minGoals = {-100,0,min+2.0};
			double[] maxGoals = {-100,max-2.0,max+2.0};
			this.setTargets(avgGoals, sumGoals, minGoals, maxGoals);
		}
		
	}
	
	public void setTargets(double[] avg, double[] sum, double[] min, double[] max) {
		this.avgTargets=avg;
		this.sumTargets=sum;
		this.minTargets=min;
		this.maxTargets=max;
	}
}




