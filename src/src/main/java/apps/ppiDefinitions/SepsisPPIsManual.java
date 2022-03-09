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

public class SepsisPPIsManual {
	
	public static List<MeasureDefinition> buildAll() {
		return new ArrayList<MeasureDefinition>(Arrays.asList(
				SepsisPPIsManual.buildAvgWaitingTimeUntilAdmission(),
				SepsisPPIsManual.buildAvgLengthOfStay(),
				SepsisPPIsManual.buildMaxLengthOfStay(),
				SepsisPPIsManual.buildNoOfERReturns(false, false),
				SepsisPPIsManual.buildTimeUntilAntibiotics(false, false),
				SepsisPPIsManual.buildTimeUntilLacticAcid(false, false)));
	}
	
	public static MeasureDefinition buildAvgWaitingTimeUntilAdmission() {
		TimeMeasure cycleTime=new TimeMeasure();
		cycleTime.setFrom(new TimeInstantCondition("ER Registration", GenericState.END));
		cycleTime.setTo(new TimeInstantCondition("Admission NC", GenericState.END));
		cycleTime.setUnitOfMeasure(TimeUnit.HOURS);
		
		PrivacyAwareAggregatedMeasure avgWaitingTime = new PrivacyAwareAggregatedMeasure();	
		avgWaitingTime.setEpsilon(0.1);
		avgWaitingTime.setBaseMeasure(cycleTime);
		avgWaitingTime.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		avgWaitingTime.setAggregationFunction(PrivacyAwareAggregator.AVG_EXP);
		return avgWaitingTime;
	}
	
	
	public static MeasureDefinition buildAvgLengthOfStay() {
		TimeMeasure cycleTime=new TimeMeasure();
		cycleTime.setFrom(new TimeInstantCondition("Admission NC", GenericState.END));
		cycleTime.setTo(new TimeInstantCondition("Release A", GenericState.END));
		cycleTime.setUnitOfMeasure(TimeUnit.DAYS);
		
		PrivacyAwareAggregatedMeasure avgWaitingTime = new PrivacyAwareAggregatedMeasure();	
		avgWaitingTime.setEpsilon(0.1);
		avgWaitingTime.setBaseMeasure(cycleTime);
		avgWaitingTime.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		avgWaitingTime.setAggregationFunction(PrivacyAwareAggregator.AVG_EXP);
		return avgWaitingTime;
	}
	
	
	public static MeasureDefinition buildMaxLengthOfStay() {
		TimeMeasure cycleTime=new TimeMeasure();
		cycleTime.setFrom(new TimeInstantCondition("Admission NC", GenericState.END));
		cycleTime.setTo(new TimeInstantCondition("Release A", GenericState.END));
		cycleTime.setUnitOfMeasure(TimeUnit.DAYS);
		
		PrivacyAwareAggregatedMeasure avgWaitingTime = new PrivacyAwareAggregatedMeasure();	
		avgWaitingTime.setEpsilon(0.1);
		avgWaitingTime.setBaseMeasure(cycleTime);
		avgWaitingTime.setBoundaryEstimation(BoundaryEstimator.EXTEND);
		avgWaitingTime.setExtensionFactor(1.6);
		avgWaitingTime.setAggregationFunction(PrivacyAwareAggregator.MAX_EXP);
		return avgWaitingTime;
	}
	
	public static MeasureDefinition buildNoOfERReturns(boolean privatizeTotal, boolean aggregate) {
		CountMeasure instanceCount=new CountMeasure();
		instanceCount.setId("instances");
		instanceCount.setWhen(new TimeInstantCondition("Release A",GenericState.END));
		
		PrivacyAwareAggregatedMeasure totalCases = new PrivacyAwareAggregatedMeasure();
		totalCases.setEpsilon(0.1);
		totalCases.setBaseMeasure(instanceCount);
		totalCases.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		if(privatizeTotal)
			totalCases.setAggregationFunction(PrivacyAwareAggregator.SUM_LAP);
		else
			totalCases.setAggregationFunction(PrivacyAwareAggregator.SUM);
			
		
		TimeMeasure returnTime=new TimeMeasure();
		returnTime.setFrom(new TimeInstantCondition("Release A", GenericState.END));
		returnTime.setTo(new TimeInstantCondition("Return ER", GenericState.END));
		returnTime.setUnitOfMeasure(TimeUnit.DAYS);
		
		DerivedSingleInstanceMeasure returnedIn28Days = new DerivedSingleInstanceMeasure();
		returnedIn28Days.setFunction("returnTime<28");
		returnedIn28Days.addUsedMeasure("returnTime", returnTime);
		
		PrivacyAwareAggregatedMeasure noOfReturnedPatients = new PrivacyAwareAggregatedMeasure();
		noOfReturnedPatients.setEpsilon(0.1);
		noOfReturnedPatients.setBaseMeasure(returnedIn28Days);
		noOfReturnedPatients.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		if(aggregate)
			noOfReturnedPatients.setAggregationFunction(PrivacyAwareAggregator.SUM);
		else
			noOfReturnedPatients.setAggregationFunction(PrivacyAwareAggregator.SUM_LAP);//used in paper
		
		DerivedMultiInstanceMeasure percentage = new DerivedMultiInstanceMeasure();

		percentage.setFunction("System.out.println(Math.min(a,b)+\" \"+b);(Math.min(a,b)/b)*100.0");
		percentage.addUsedMeasure("a", noOfReturnedPatients);
		percentage.addUsedMeasure("b", totalCases);
		
		//hack that emulates derived measure in aggregated measure class
		PrivacyAwareAggregatedMeasure perc = new PrivacyAwareAggregatedMeasure();
		perc.setEpsilon(0.1);
		perc.setBaseMeasure(returnedIn28Days);
		perc.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		perc.setAggregationFunction(PrivacyAwareAggregator.PERCENTAGE);
		
		if(aggregate)
			return perc;
		else
			return percentage;
	}
	
	public static MeasureDefinition buildTimeUntilAntibiotics(boolean privatizeTotal, boolean aggregate) {
		CountMeasure instanceCount=new CountMeasure();
		instanceCount.setId("instances");
		instanceCount.setWhen(new TimeInstantCondition("Start",GenericState.END));
		
		PrivacyAwareAggregatedMeasure totalCases = new PrivacyAwareAggregatedMeasure();
		totalCases.setBaseMeasure(instanceCount);
		totalCases.setEpsilon(0.1);
		totalCases.setBaseMeasure(instanceCount);
		totalCases.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		if(privatizeTotal)
			totalCases.setAggregationFunction(PrivacyAwareAggregator.SUM_LAP);
		else
			totalCases.setAggregationFunction(PrivacyAwareAggregator.SUM);
		
		TimeMeasure cycleTime=new TimeMeasure();
		cycleTime.setFrom(new TimeInstantCondition("Start", GenericState.END));
		cycleTime.setTo(new TimeInstantCondition("IV Antibiotics", GenericState.END));
		cycleTime.setUnitOfMeasure(TimeUnit.MINUTES);
		
		DerivedSingleInstanceMeasure lowerThanAnHour = new DerivedSingleInstanceMeasure();
		lowerThanAnHour.setId("lower");
		lowerThanAnHour.setFunction("duration<60");
		lowerThanAnHour.addUsedMeasure("duration", cycleTime);
		
		PrivacyAwareAggregatedMeasure avgWaitingTime = new PrivacyAwareAggregatedMeasure();
		avgWaitingTime.setEpsilon(0.1);
		avgWaitingTime.setBaseMeasure(lowerThanAnHour);
		avgWaitingTime.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		if(aggregate)
			avgWaitingTime.setAggregationFunction(PrivacyAwareAggregator.SUM);
		else
			avgWaitingTime.setAggregationFunction(PrivacyAwareAggregator.SUM_LAP);//used in paper
		
		DerivedMultiInstanceMeasure percentage = new DerivedMultiInstanceMeasure();
		percentage.setFunction("System.out.println(Math.min(a,b)+\" \"+b);(Math.min(a,b)/b)*100.0");
		percentage.addUsedMeasure("a", avgWaitingTime);
		percentage.addUsedMeasure("b", totalCases);
		
		//also using hack
		PrivacyAwareAggregatedMeasure perc = new PrivacyAwareAggregatedMeasure();
		perc.setEpsilon(0.1);
		perc.setBaseMeasure(lowerThanAnHour);
		perc.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		perc.setAggregationFunction(PrivacyAwareAggregator.PERCENTAGE);
		if(aggregate)
			return perc;
		else
			return percentage;
	}
	
	public static MeasureDefinition buildTimeUntilLacticAcid(boolean privatizeTotal, boolean aggregate) {
		CountMeasure instanceCount=new CountMeasure();
		instanceCount.setId("instances");
		instanceCount.setWhen(new TimeInstantCondition("Start",GenericState.END));
		PrivacyAwareAggregatedMeasure totalCases = new PrivacyAwareAggregatedMeasure();
		totalCases.setBaseMeasure(instanceCount);
		totalCases.setEpsilon(0.1);
		totalCases.setBaseMeasure(instanceCount);
		totalCases.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		if(privatizeTotal)
			totalCases.setAggregationFunction(PrivacyAwareAggregator.SUM_LAP);
		else
			totalCases.setAggregationFunction(PrivacyAwareAggregator.SUM);
		
		TimeMeasure cycleTime=new TimeMeasure();
		cycleTime.setFrom(new TimeInstantCondition("Start", GenericState.END));
		cycleTime.setTo(new TimeInstantCondition("LacticAcid", GenericState.END));
		cycleTime.setUnitOfMeasure(TimeUnit.MINUTES);
		
		DerivedSingleInstanceMeasure lowerThanThreeHours = new DerivedSingleInstanceMeasure();
		lowerThanThreeHours.setId("lower");
		lowerThanThreeHours.setFunction("duration<180");
		lowerThanThreeHours.addUsedMeasure("duration", cycleTime);
		
		PrivacyAwareAggregatedMeasure avgWaitingTime = new PrivacyAwareAggregatedMeasure();
		avgWaitingTime.setId("time");
		avgWaitingTime.setEpsilon(0.1);
		avgWaitingTime.setBaseMeasure(lowerThanThreeHours);
		avgWaitingTime.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		if(aggregate)
			avgWaitingTime.setAggregationFunction(PrivacyAwareAggregator.SUM);
		else
			avgWaitingTime.setAggregationFunction(PrivacyAwareAggregator.SUM_LAP);//used in paper
		
		DerivedMultiInstanceMeasure percentage = new DerivedMultiInstanceMeasure();
		percentage.setId("percentage");
		percentage.setFunction("System.out.println(Math.min(a,b)+\" \"+b);(Math.min(a,b)/b)*100.0");
		percentage.addUsedMeasure("a", avgWaitingTime);
		percentage.addUsedMeasure("b", totalCases);
		
		
		//also using hack
		PrivacyAwareAggregatedMeasure perc = new PrivacyAwareAggregatedMeasure();
		perc.setEpsilon(0.1);
		perc.setBaseMeasure(lowerThanThreeHours);
		perc.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		perc.setAggregationFunction(PrivacyAwareAggregator.PERCENTAGE);
		
		if(aggregate)
			return perc;
		else
			return percentage;
	}
	
	
	//variants calculating the true result
	public static MeasureDefinition buildAvgWaitingTimeUntilAdmissionNoPrivacy() {
		TimeMeasure cycleTime=new TimeMeasure();
		cycleTime.setFrom(new TimeInstantCondition("ER Registration", GenericState.END));
		cycleTime.setTo(new TimeInstantCondition("Admission NC", GenericState.END));
		cycleTime.setUnitOfMeasure(TimeUnit.HOURS);
		
		PrivacyAwareAggregatedMeasure avgWaitingTime = new PrivacyAwareAggregatedMeasure();	
		avgWaitingTime.setBaseMeasure(cycleTime);
		avgWaitingTime.setAggregationFunction(PrivacyAwareAggregator.AVG);	
		
		return avgWaitingTime;
	}
	
	
	public static MeasureDefinition buildAvgLengthOfStayNoPrivacy() {
		TimeMeasure cycleTime=new TimeMeasure();
		cycleTime.setFrom(new TimeInstantCondition("Admission NC", GenericState.END));
		cycleTime.setTo(new TimeInstantCondition("Release A", GenericState.END));
		cycleTime.setUnitOfMeasure(TimeUnit.DAYS);
		
		PrivacyAwareAggregatedMeasure avgWaitingTime = new PrivacyAwareAggregatedMeasure();	
		avgWaitingTime.setBaseMeasure(cycleTime);
		avgWaitingTime.setAggregationFunction(PrivacyAwareAggregator.AVG);
		return avgWaitingTime;
	}
	
	
	public static MeasureDefinition buildMaxLengthOfStayNoPrivacy() {
		TimeMeasure cycleTime=new TimeMeasure();
		cycleTime.setFrom(new TimeInstantCondition("Admission NC", GenericState.END));
		cycleTime.setTo(new TimeInstantCondition("Release A", GenericState.END));
		cycleTime.setUnitOfMeasure(TimeUnit.DAYS);
		
		PrivacyAwareAggregatedMeasure avgWaitingTime = new PrivacyAwareAggregatedMeasure();	
		avgWaitingTime.setBaseMeasure(cycleTime);
		avgWaitingTime.setAggregationFunction(PrivacyAwareAggregator.MAX);
		return avgWaitingTime;
	}
	
	
	public static MeasureDefinition buildNoOfERReturnsNoPrivacy() {
		CountMeasure instanceCount=new CountMeasure();
		instanceCount.setId("instances");
		instanceCount.setWhen(new TimeInstantCondition("Release A",GenericState.END));
		
		PrivacyAwareAggregatedMeasure totalCases = new PrivacyAwareAggregatedMeasure();
		totalCases.setBaseMeasure(instanceCount);
		totalCases.setAggregationFunction(PrivacyAwareAggregator.SUM);
		
		TimeMeasure returnTime=new TimeMeasure();
		returnTime.setFrom(new TimeInstantCondition("Release A", GenericState.END));
		returnTime.setTo(new TimeInstantCondition("Return ER", GenericState.END));
		returnTime.setUnitOfMeasure(TimeUnit.DAYS);
		
		DerivedSingleInstanceMeasure returnedIn28Days = new DerivedSingleInstanceMeasure();
		returnedIn28Days.setFunction("returnTime<28");
		returnedIn28Days.addUsedMeasure("returnTime", returnTime);
		
		PrivacyAwareAggregatedMeasure noOfReturnedPatients = new PrivacyAwareAggregatedMeasure();
		noOfReturnedPatients.setBaseMeasure(returnedIn28Days);
		noOfReturnedPatients.setAggregationFunction(PrivacyAwareAggregator.SUM);
		
		
		DerivedMultiInstanceMeasure percentage = new DerivedMultiInstanceMeasure();
		percentage.setFunction("a/b*100");
		percentage.addUsedMeasure("a", noOfReturnedPatients);
		percentage.addUsedMeasure("b", totalCases);
		return percentage;
	}
	
	public static MeasureDefinition buildTimeUntilAntibioticsNoPrivacy() {
		CountMeasure instanceCount=new CountMeasure();
		instanceCount.setId("instances");
		instanceCount.setWhen(new TimeInstantCondition("Start",GenericState.END));
		
		PrivacyAwareAggregatedMeasure totalCases = new PrivacyAwareAggregatedMeasure();
		totalCases.setBaseMeasure(instanceCount);
		totalCases.setAggregationFunction(PrivacyAwareAggregator.SUM);
		
		TimeMeasure cycleTime=new TimeMeasure();
		cycleTime.setFrom(new TimeInstantCondition("Start", GenericState.END));
		cycleTime.setTo(new TimeInstantCondition("IV Antibiotics", GenericState.END));
		cycleTime.setUnitOfMeasure(TimeUnit.MINUTES);
		
		DerivedSingleInstanceMeasure lowerThanThreeHours = new DerivedSingleInstanceMeasure();
		lowerThanThreeHours.setFunction("duration<60");
		lowerThanThreeHours.addUsedMeasure("duration", cycleTime);
		
		PrivacyAwareAggregatedMeasure avgWaitingTime = new PrivacyAwareAggregatedMeasure();
		avgWaitingTime.setBaseMeasure(lowerThanThreeHours);
		avgWaitingTime.setAggregationFunction(PrivacyAwareAggregator.SUM);
		
		DerivedMultiInstanceMeasure percentage = new DerivedMultiInstanceMeasure();
		percentage.setFunction("a/b*100");
		percentage.addUsedMeasure("a", avgWaitingTime);
		percentage.addUsedMeasure("b", totalCases);
		return percentage;
	}
	
	public static MeasureDefinition buildTimeUntilLacticAcidNoPrivacy() {
		CountMeasure instanceCount=new CountMeasure();
		instanceCount.setId("instances");
		instanceCount.setWhen(new TimeInstantCondition("Start",GenericState.END));
		PrivacyAwareAggregatedMeasure totalCases = new PrivacyAwareAggregatedMeasure();
		totalCases.setBaseMeasure(instanceCount);
		totalCases.setAggregationFunction(PrivacyAwareAggregator.SUM);
		
		TimeMeasure cycleTime=new TimeMeasure();
		cycleTime.setFrom(new TimeInstantCondition("Start", GenericState.END));
		cycleTime.setTo(new TimeInstantCondition("LacticAcid", GenericState.END));
		cycleTime.setUnitOfMeasure(TimeUnit.MINUTES);
		
		DerivedSingleInstanceMeasure lowerThanThreeHours = new DerivedSingleInstanceMeasure();
		lowerThanThreeHours.setFunction("duration<180");
		lowerThanThreeHours.addUsedMeasure("duration", cycleTime);
		
		PrivacyAwareAggregatedMeasure avgWaitingTime = new PrivacyAwareAggregatedMeasure();
		avgWaitingTime.setBaseMeasure(lowerThanThreeHours);
		avgWaitingTime.setAggregationFunction(PrivacyAwareAggregator.SUM);
		
		DerivedMultiInstanceMeasure percentage = new DerivedMultiInstanceMeasure();
		percentage.setFunction("a/b*100");
		percentage.addUsedMeasure("a", avgWaitingTime);
		percentage.addUsedMeasure("b", totalCases);
		return percentage;
	}
}
