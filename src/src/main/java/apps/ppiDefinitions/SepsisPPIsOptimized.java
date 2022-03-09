package apps.ppiDefinitions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import es.us.isa.ppinot.model.MeasureDefinition;
import es.us.isa.ppinot.model.TimeUnit;
import es.us.isa.ppinot.model.base.CountMeasure;
import es.us.isa.ppinot.model.base.TimeMeasure;
import es.us.isa.ppinot.model.condition.TimeInstantCondition;
import es.us.isa.ppinot.model.derived.DerivedMultiInstanceMeasure;
import es.us.isa.ppinot.model.derived.DerivedSingleInstanceMeasure;
import es.us.isa.ppinot.model.state.GenericState;
import pappi.aggregators.PrivacyAwareAggregator;
import pappi.boundary.BoundaryEstimator;
import pappi.measureDefinitions.OptimizablePrivacyAwareMeasure;
import pappi.measureDefinitions.PrivacyAwareAggregatedMeasure;
import pappi.measureDefinitions.PrivacyAwareDerivedMultiInstanceMeasure;

public class SepsisPPIsOptimized {
	
	public static List<MeasureDefinition> buildAll(double epsilon) {
		return new ArrayList<MeasureDefinition>(Arrays.asList(
				SepsisPPIsOptimized.buildAvgWaitingTimeUntilAdmission(epsilon),
				SepsisPPIsOptimized.buildAvgLengthOfStay(epsilon),
				SepsisPPIsOptimized.buildMaxLengthOfStay(epsilon),
				SepsisPPIsOptimized.buildReturnedIn28days(epsilon),
				SepsisPPIsOptimized.buildTimeUntilAntibiotics(epsilon),
				SepsisPPIsOptimized.buildTimeUntilLacticAcid(epsilon)));
	}
	
	public static MeasureDefinition buildAvgWaitingTimeUntilAdmission(double epsilon) {
		TimeMeasure cycleTime=new TimeMeasure();
		cycleTime.setFrom(new TimeInstantCondition("ER Registration", GenericState.END));
		cycleTime.setTo(new TimeInstantCondition("Admission NC", GenericState.END));
		cycleTime.setUnitOfMeasure(TimeUnit.HOURS);
		
		PrivacyAwareAggregatedMeasure avgWaitingTime = new PrivacyAwareAggregatedMeasure();	
		avgWaitingTime.setEpsilon(0.1);
		avgWaitingTime.setBaseMeasure(cycleTime);
		avgWaitingTime.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		avgWaitingTime.setAggregationFunction(PrivacyAwareAggregator.AVG);
		
		OptimizablePrivacyAwareMeasure root = new OptimizablePrivacyAwareMeasure();
		root.setId("AvgWaitingTimeUntilAdmission");
		root.setBaseMeasure(avgWaitingTime);
		root.setEpsilon(epsilon);
		
		return root;
	}
	
	
	public static MeasureDefinition buildAvgLengthOfStay(double epsilon) {
		TimeMeasure cycleTime=new TimeMeasure();
		cycleTime.setFrom(new TimeInstantCondition("Admission NC", GenericState.END));
		cycleTime.setTo(new TimeInstantCondition("Release A", GenericState.END));
		cycleTime.setUnitOfMeasure(TimeUnit.DAYS);
		
		PrivacyAwareAggregatedMeasure avgStayTime = new PrivacyAwareAggregatedMeasure();	
		avgStayTime.setEpsilon(0.1);
		avgStayTime.setBaseMeasure(cycleTime);
		avgStayTime.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		avgStayTime.setAggregationFunction(PrivacyAwareAggregator.AVG);
		
		OptimizablePrivacyAwareMeasure root = new OptimizablePrivacyAwareMeasure();
		root.setId("AvgLengthOfStay");
		root.setBaseMeasure(avgStayTime);
		root.setEpsilon(epsilon);
		
		return root;
	}
	
	
	public static MeasureDefinition buildMaxLengthOfStay(double epsilon) {
		TimeMeasure cycleTime=new TimeMeasure();
		cycleTime.setFrom(new TimeInstantCondition("Admission NC", GenericState.END));
		cycleTime.setTo(new TimeInstantCondition("Release A", GenericState.END));
		cycleTime.setUnitOfMeasure(TimeUnit.DAYS);
		
		PrivacyAwareAggregatedMeasure maxStayTime = new PrivacyAwareAggregatedMeasure();	
		maxStayTime.setEpsilon(0.1);
		maxStayTime.setBaseMeasure(cycleTime);
		maxStayTime.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		maxStayTime.setExtensionFactor(1.6);
		maxStayTime.setAggregationFunction(PrivacyAwareAggregator.MAX);


		OptimizablePrivacyAwareMeasure root = new OptimizablePrivacyAwareMeasure();
		root.setId("MaxLengthOfStay");
		root.setBaseMeasure(maxStayTime);
		root.setEpsilon(epsilon);
		
		return root;
	}
	
	public static MeasureDefinition buildReturnedIn28days(double epsilon) {
		CountMeasure instanceCount=new CountMeasure();
		instanceCount.setId("instances");
		instanceCount.setWhen(new TimeInstantCondition("Release A",GenericState.END));
		
		DerivedSingleInstanceMeasure staticOne = new DerivedSingleInstanceMeasure();
		staticOne.setFunction("1.0");
		staticOne.addUsedMeasure("returnTime", instanceCount);
		
		PrivacyAwareAggregatedMeasure totalCases = new PrivacyAwareAggregatedMeasure();
		totalCases.setEpsilon(0.1);
		totalCases.setBaseMeasure(staticOne);
		totalCases.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		totalCases.setAggregationFunction(PrivacyAwareAggregator.SUM);
			
		
		TimeMeasure returnTime=new TimeMeasure();
		returnTime.setFrom(new TimeInstantCondition("Release A", GenericState.END));
		returnTime.setTo(new TimeInstantCondition("Return ER", GenericState.END));
		returnTime.setUnitOfMeasure(TimeUnit.DAYS);
		
		//TODO make it work with boolean results. Now every results needs to be a Double in the end!
		DerivedSingleInstanceMeasure returnedIn28Days = new DerivedSingleInstanceMeasure();
		returnedIn28Days.setFunction("returnTime<28?1.0:0.0");
		returnedIn28Days.addUsedMeasure("returnTime", returnTime);
		
		PrivacyAwareAggregatedMeasure noOfReturnedPatients = new PrivacyAwareAggregatedMeasure();
		noOfReturnedPatients.setEpsilon(0.1);
		noOfReturnedPatients.setBaseMeasure(returnedIn28Days);
		noOfReturnedPatients.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		noOfReturnedPatients.setAggregationFunction(PrivacyAwareAggregator.SUM);
		
		PrivacyAwareDerivedMultiInstanceMeasure percentage = new PrivacyAwareDerivedMultiInstanceMeasure();
		percentage.setFunction("a<b?(a/b)*100.0:100.0");
		percentage.addUsedMeasure("a", noOfReturnedPatients);
		percentage.addUsedMeasure("b", totalCases);

		OptimizablePrivacyAwareMeasure root = new OptimizablePrivacyAwareMeasure();
		root.setId("ReturnedIn28Days");
		root.setBaseMeasure(percentage);
		root.setEpsilon(epsilon);
		
		return root;
	}
	
	public static MeasureDefinition buildTimeUntilAntibiotics(double epsilon) {
		CountMeasure instanceCount=new CountMeasure();
		instanceCount.setId("instances");
		instanceCount.setWhen(new TimeInstantCondition("Start",GenericState.END));
		
		DerivedSingleInstanceMeasure staticOne = new DerivedSingleInstanceMeasure();
		staticOne.setFunction("1.0");
		staticOne.addUsedMeasure("returnTime", instanceCount);
		
		PrivacyAwareAggregatedMeasure totalCases = new PrivacyAwareAggregatedMeasure();
		totalCases.setEpsilon(0.1);
		totalCases.setBaseMeasure(staticOne);
		totalCases.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		totalCases.setAggregationFunction(PrivacyAwareAggregator.SUM);
		
		TimeMeasure cycleTime=new TimeMeasure();
		cycleTime.setFrom(new TimeInstantCondition("Start", GenericState.END));
		cycleTime.setTo(new TimeInstantCondition("IV Antibiotics", GenericState.END));
		cycleTime.setUnitOfMeasure(TimeUnit.MINUTES);
		
		DerivedSingleInstanceMeasure lowerThanAnHour = new DerivedSingleInstanceMeasure();
		lowerThanAnHour.setId("lower");
		lowerThanAnHour.setFunction("duration<60?1.0:0.0");
		lowerThanAnHour.addUsedMeasure("duration", cycleTime);
		
		PrivacyAwareAggregatedMeasure avgWaitingTime = new PrivacyAwareAggregatedMeasure();
		avgWaitingTime.setEpsilon(0.1);
		avgWaitingTime.setBaseMeasure(lowerThanAnHour);
		avgWaitingTime.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		avgWaitingTime.setAggregationFunction(PrivacyAwareAggregator.SUM);

		PrivacyAwareDerivedMultiInstanceMeasure percentage = new PrivacyAwareDerivedMultiInstanceMeasure();
		percentage.setFunction("a<b?(a/b)*100.0:100.0");
		percentage.addUsedMeasure("a", avgWaitingTime);
		percentage.addUsedMeasure("b", totalCases);

		OptimizablePrivacyAwareMeasure root = new OptimizablePrivacyAwareMeasure();
		root.setId("TimeUntilAntibiotics<1h");
		root.setBaseMeasure(percentage);
		root.setEpsilon(epsilon);
		
		return root;
	}
	
	public static MeasureDefinition buildTimeUntilLacticAcid(double epsilon) {
		CountMeasure instanceCount=new CountMeasure();
		instanceCount.setId("instances");
		instanceCount.setWhen(new TimeInstantCondition("Start",GenericState.END));
		
		DerivedSingleInstanceMeasure staticOne = new DerivedSingleInstanceMeasure();
		staticOne.setFunction("1.0");
		staticOne.addUsedMeasure("returnTime", instanceCount);
		
		PrivacyAwareAggregatedMeasure totalCases = new PrivacyAwareAggregatedMeasure();
		totalCases.setEpsilon(0.1);
		totalCases.setBaseMeasure(staticOne);
		totalCases.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		totalCases.setAggregationFunction(PrivacyAwareAggregator.SUM);
		
		TimeMeasure cycleTime=new TimeMeasure();
		cycleTime.setFrom(new TimeInstantCondition("Start", GenericState.END));
		cycleTime.setTo(new TimeInstantCondition("LacticAcid", GenericState.END));
		cycleTime.setUnitOfMeasure(TimeUnit.MINUTES);
		
		DerivedSingleInstanceMeasure lowerThanThreeHours = new DerivedSingleInstanceMeasure();
		lowerThanThreeHours.setId("lower");
		lowerThanThreeHours.setFunction("duration<180?1.0:0.0");
		lowerThanThreeHours.addUsedMeasure("duration", cycleTime);
		
		PrivacyAwareAggregatedMeasure avgWaitingTime = new PrivacyAwareAggregatedMeasure();
		avgWaitingTime.setId("time");
		avgWaitingTime.setEpsilon(0.1);
		avgWaitingTime.setBaseMeasure(lowerThanThreeHours);
		avgWaitingTime.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		avgWaitingTime.setAggregationFunction(PrivacyAwareAggregator.SUM);
		
		PrivacyAwareDerivedMultiInstanceMeasure percentage = new PrivacyAwareDerivedMultiInstanceMeasure();
		percentage.setId("percentage");
		percentage.setFunction("a<b?(a/b)*100.0:100.0");
		percentage.addUsedMeasure("a", avgWaitingTime);
		percentage.addUsedMeasure("b", totalCases);
		
		OptimizablePrivacyAwareMeasure root = new OptimizablePrivacyAwareMeasure();
		root.setId("TimeUntilLactidAcid<3h");
		root.setBaseMeasure(percentage);
		root.setEpsilon(epsilon);
		
		return root;
	}
}
