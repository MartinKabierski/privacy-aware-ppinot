package apps.ppiDefinitions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import es.us.isa.ppinot.evaluation.Aggregator;
import es.us.isa.ppinot.evaluation.Measure;
import es.us.isa.ppinot.evaluation.TemporalMeasureScope;
import es.us.isa.ppinot.evaluation.evaluators.LogMeasureEvaluator;
import es.us.isa.ppinot.evaluation.evaluators.MeasureEvaluator;
import es.us.isa.ppinot.evaluation.logs.LogProvider;
import es.us.isa.ppinot.evaluation.logs.MXMLLog;
import es.us.isa.ppinot.model.MeasureDefinition;
import es.us.isa.ppinot.model.TimeUnit;
import es.us.isa.ppinot.model.aggregated.AggregatedMeasure;
import es.us.isa.ppinot.model.base.CountMeasure;
import es.us.isa.ppinot.model.base.StateConditionMeasure;
import es.us.isa.ppinot.model.base.TimeMeasure;
import es.us.isa.ppinot.model.condition.StateCondition;
import es.us.isa.ppinot.model.condition.TimeInstantCondition;
import es.us.isa.ppinot.model.derived.DerivedMultiInstanceMeasure;
import es.us.isa.ppinot.model.derived.DerivedSingleInstanceMeasure;
import es.us.isa.ppinot.model.scope.Period;
import es.us.isa.ppinot.model.scope.SimpleTimeFilter;
import es.us.isa.ppinot.model.state.GenericState;
import pappi.aggregators.PrivacyAwareAggregator;
import pappi.boundary.BoundaryEstimator;
import pappi.evaluators.PrivacyAwareLogMeasureEvaluator;
import pappi.measureDefinitions.PrivacyAwareAggregatedMeasure;

@Deprecated
public class CalcPPI {
	
	//private static final Schedule WORKINGHOURS = new Schedule(DateTimeConstants.MONDAY, DateTimeConstants.FRIDAY, new LocalTime())
	
	//create PPI's based of real life log
	public static void main(String[] args) throws Exception {
		CalcPPI app = new CalcPPI();
		List<Measure> measures = null;
		List<Measure> trueMeasures = null;
		
		int repetitions = 10;
	    BufferedWriter writer = new BufferedWriter(new FileWriter("evaluation_sepsis.csv"));
	    writer.write("Kpi;From;To;NoOfValues;Result;Orig\n");
		
		//avg waiting time until admission
		LogProvider log = new MXMLLog(new FileInputStream(new File("../PRIPEL_logs/Sepsis Cases - Event Log.mxml")),null);
	    trueMeasures = app.computePPI(new MXMLLog(new FileInputStream(new File("../PRIPEL_logs/Sepsis Cases - Event Log.mxml")),null), app.buildAvgWaitingTimeUntilAdmissionNoPrivacy());
		for(int i=0;i<repetitions;i++) {
			System.out.println("Evaluating KPI 1 - Repetition "+i);
			log = new MXMLLog(new FileInputStream(new File("../PRIPEL_logs/Sepsis Cases - Event Log.mxml")),null);
			measures = app.computePPI(log, app.buildAvgWaitingTimeUntilAdmission());
			writeMeasuresToFile("AvgWaitingTimeUntilAdmission",measures,trueMeasures,repetitions, writer);
		}
		
		//avg length of stay
	    trueMeasures = app.computePPI(new MXMLLog(new FileInputStream(new File("../PRIPEL_logs/Sepsis Cases - Event Log.mxml")),null), app.buildAvgLengthOfStayNoPrivacy());
		for(int i=0;i<repetitions;i++) {
			System.out.println("Evaluating KPI 2 - Repetition "+i);
			log = new MXMLLog(new FileInputStream(new File("../PRIPEL_logs/Sepsis Cases - Event Log.mxml")),null);
			measures = app.computePPI(log, app.buildAvgLengthOfStay());
			writeMeasuresToFile("AvgLengthOfStay",measures,trueMeasures,repetitions, writer);
		}
		
		//max length of stay
	    trueMeasures = app.computePPI(new MXMLLog(new FileInputStream(new File("../PRIPEL_logs/Sepsis Cases - Event Log.mxml")),null), app.buildMaxLengthOfStayNoPrivacy());
		for(int i=0;i<repetitions;i++) {
			System.out.println("Evaluating KPI 3 - Repetition "+i);
			log = new MXMLLog(new FileInputStream(new File("../PRIPEL_logs/Sepsis Cases - Event Log.mxml")),null);
			measures = app.computePPI(log, app.buildMaxLengthOfStay());
			writeMeasuresToFile("MaxLengthOfStay",measures,trueMeasures,repetitions, writer);
		}
		
		
		//sum of patients that returned to er
		trueMeasures = app.computePPI(new MXMLLog(new FileInputStream(new File("../PRIPEL_logs/Sepsis Cases - Event Log.mxml")),null), app.buildNoOfERReturnsNoPrivacy());
		for(int i=0;i<repetitions;i++) {
			System.out.println("Evaluating KPI 4 - Repetition "+i);
			log = new MXMLLog(new FileInputStream(new File("../PRIPEL_logs/Sepsis Cases - Event Log.mxml")),null);
			measures = app.computePPI(log, app.buildNoOfERReturns());
			writeMeasuresToFile("PercentageOfReturningPatients",measures,trueMeasures,repetitions, writer);
		}
		
		//no of cases where antibiotics were given in one hour
		trueMeasures = app.computePPI(new MXMLLog(new FileInputStream(new File("../PRIPEL_logs/Sepsis Cases - Event Log.mxml")),null), app.buildTimeUntilAntibioticsNoPrivacy());
		for(int i=0;i<repetitions;i++) {
			System.out.println("Evaluating KPI 5 - Repetition "+i);
			log = new MXMLLog(new FileInputStream(new File("../PRIPEL_logs/Sepsis Cases - Event Log.mxml")),null);
			measures = app.computePPI(log, app.buildTimeUntilAntibiotics());
			writeMeasuresToFile("%AntibioticsWithinOneHour",measures,trueMeasures,repetitions, writer);
		}
		
		// percentage of cases where lactic acid check was done in one hour
		trueMeasures = app.computePPI(new MXMLLog(new FileInputStream(new File("../PRIPEL_logs/Sepsis Cases - Event Log.mxml")),null), app.buildTimeUntilLacticAcidNoPrivacy());
		for(int i=0;i<repetitions;i++) {
			System.out.println("Evaluating KPI 6 - Repetition "+i);
			log = new MXMLLog(new FileInputStream(new File("../PRIPEL_logs/Sepsis Cases - Event Log.mxml")),null);
			measures = app.computePPI(log, app.buildTimeUntilLacticAcid());
			
			writeMeasuresToFile("%LacticAcidWithinThreeHours",measures,trueMeasures,repetitions, writer);
		}

		/*
		System.out.println(measures.size());
		for (Measure m : measures) {
			if(m.getValueAsString()!="NaN") {
				System.out.println("Value: " + m.getValue());
	        	//System.out.println("Number of instances: " + m.getInstances().size());
	        	//System.out.println("Instances: " + m.getInstances());
	        	//if (m.getMeasureScope() instanceof TemporalMeasureScope) {
	        		//TemporalMeasureScope tempScope = (TemporalMeasureScope) m.getMeasureScope();
	        		//System.out.println("Start: " + tempScope.getStart().toString());
	        		//System.out.println("End: " + tempScope.getEnd().toString());
	        	//}
	        	//System.out.println("--");
			}
		}*/
		writer.close();
	}

	private static void writeMeasuresToFile(String kpi, List<Measure> measures, List<Measure> trueMeasures,
			int repetitions, BufferedWriter writer) throws IOException {
		for(int j=0;j<measures.size();j++) {
			if(measures.get(j).getValue()!=Double.NaN) {
				TemporalMeasureScope tempScope = (TemporalMeasureScope) measures.get(j).getMeasureScope();
				writer.write(String.join(";",
						kpi,
						tempScope.getStart().toString(),
						tempScope.getEnd().toString(),
						Integer.toString(measures.get(j).getInstances().size()),
						Double.toString(measures.get(j).getValue()),
						Double.toString(trueMeasures.get(j).getValue())
						));
				writer.write("\n");
			}
		}
	}


	
	private List<Measure> computePPI(LogProvider log, MeasureDefinition measure) throws Exception {
		//make sure normal queries can also be answered without changing Evaluator
		MeasureEvaluator evaluator = new PrivacyAwareLogMeasureEvaluator(log);
		return evaluator.eval(measure, new SimpleTimeFilter(Period.MONTHLY,1, false));
	}

	
	private MeasureDefinition buildAvgWaitingTimeUntilAdmission() {
		TimeMeasure cycleTime=new TimeMeasure();
		cycleTime.setFrom(new TimeInstantCondition("ER Registration", GenericState.END));
		cycleTime.setTo(new TimeInstantCondition("Admission NC", GenericState.END));
		cycleTime.setUnitOfMeasure(TimeUnit.HOURS);
		
		PrivacyAwareAggregatedMeasure avgWaitingTime = new PrivacyAwareAggregatedMeasure();	
		avgWaitingTime.setEpsilon(0.1);
		avgWaitingTime.setLowerBounds(0);
		avgWaitingTime.setUpperBound(150);
		avgWaitingTime.setBaseMeasure(cycleTime);
		avgWaitingTime.setTarget(24);
		avgWaitingTime.setFalloff(2);
		avgWaitingTime.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		avgWaitingTime.setMinimalSize(50);
		avgWaitingTime.setExtensionFactor(1.2);
		avgWaitingTime.setAggregationFunction(PrivacyAwareAggregator.AVG_EXP);
		return avgWaitingTime;
	}
	
	
	private MeasureDefinition buildAvgLengthOfStay() {
		TimeMeasure cycleTime=new TimeMeasure();
		cycleTime.setFrom(new TimeInstantCondition("Admission NC", GenericState.END));
		cycleTime.setTo(new TimeInstantCondition("Release", GenericState.END));
		cycleTime.setUnitOfMeasure(TimeUnit.DAYS);
		
		PrivacyAwareAggregatedMeasure avgWaitingTime = new PrivacyAwareAggregatedMeasure();	
		avgWaitingTime.setEpsilon(0.1);
		avgWaitingTime.setLowerBounds(0);
		avgWaitingTime.setUpperBound(150);
		avgWaitingTime.setBaseMeasure(cycleTime);
		avgWaitingTime.setTarget(30);
		avgWaitingTime.setFalloff(2);
		avgWaitingTime.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		avgWaitingTime.setMinimalSize(50);
		avgWaitingTime.setExtensionFactor(1.2);
		avgWaitingTime.setAggregationFunction(PrivacyAwareAggregator.AVG_EXP);
		return avgWaitingTime;
	}
	
	
	private MeasureDefinition buildMaxLengthOfStay() {
		TimeMeasure cycleTime=new TimeMeasure();
		cycleTime.setFrom(new TimeInstantCondition("Admission NC", GenericState.END));
		cycleTime.setTo(new TimeInstantCondition("Release", GenericState.END));
		cycleTime.setUnitOfMeasure(TimeUnit.DAYS);
		
		PrivacyAwareAggregatedMeasure avgWaitingTime = new PrivacyAwareAggregatedMeasure();	
		avgWaitingTime.setEpsilon(0.1);
		avgWaitingTime.setLowerBounds(0);
		avgWaitingTime.setUpperBound(150);
		avgWaitingTime.setBaseMeasure(cycleTime);
		avgWaitingTime.setTarget(35);
		avgWaitingTime.setFalloff(2);
		avgWaitingTime.setBoundaryEstimation(BoundaryEstimator.EXTEND);
		avgWaitingTime.setMinimalSize(50);
		avgWaitingTime.setExtensionFactor(1.6);
		avgWaitingTime.setAggregationFunction(PrivacyAwareAggregator.MAX_EXP);
		return avgWaitingTime;
	}
	
	
	private MeasureDefinition buildNoOfERReturns() {
		CountMeasure instanceCount=new CountMeasure();
		instanceCount.setId("instances");
		instanceCount.setWhen(new TimeInstantCondition("Start",GenericState.END));
		
		PrivacyAwareAggregatedMeasure totalCases = new PrivacyAwareAggregatedMeasure();
		totalCases.setBaseMeasure(instanceCount);
		totalCases.setEpsilon(0.1);
		totalCases.setLowerBounds(0);
		totalCases.setUpperBound(150);
		totalCases.setBaseMeasure(instanceCount);
		totalCases.setTarget(5);
		totalCases.setFalloff(2);
		totalCases.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		totalCases.setMinimalSize(50);
		totalCases.setAggregationFunction(PrivacyAwareAggregator.SUM);
		
		TimeMeasure returnTime=new TimeMeasure();
		returnTime.setFrom(new TimeInstantCondition("Release", GenericState.END));
		returnTime.setTo(new TimeInstantCondition("Return ER", GenericState.END));
		returnTime.setUnitOfMeasure(TimeUnit.DAYS);
		
		DerivedSingleInstanceMeasure returnedIn28Days = new DerivedSingleInstanceMeasure();
		returnedIn28Days.setFunction("returnTime<28");
		returnedIn28Days.addUsedMeasure("returnTime", returnTime);
		
		PrivacyAwareAggregatedMeasure noOfReturnedPatients = new PrivacyAwareAggregatedMeasure();
		noOfReturnedPatients.setEpsilon(0.1);
		noOfReturnedPatients.setLowerBounds(0);
		noOfReturnedPatients.setUpperBound(150);
		noOfReturnedPatients.setBaseMeasure(returnedIn28Days);
		noOfReturnedPatients.setTarget(180);
		noOfReturnedPatients.setFalloff(2);
		noOfReturnedPatients.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		noOfReturnedPatients.setMinimalSize(50);
		noOfReturnedPatients.setExtensionFactor(1.2);
		noOfReturnedPatients.setAggregationFunction(PrivacyAwareAggregator.SUM_LAP);
		
		DerivedMultiInstanceMeasure percentage = new DerivedMultiInstanceMeasure();

		percentage.setFunction("a/b*100");
		percentage.addUsedMeasure("a", noOfReturnedPatients);
		percentage.addUsedMeasure("b", totalCases);
		
		//hack that emulates derived measure in aggregated measure class
		PrivacyAwareAggregatedMeasure perc = new PrivacyAwareAggregatedMeasure();
		perc.setEpsilon(0.1);
		perc.setLowerBounds(0);
		perc.setUpperBound(150);
		perc.setBaseMeasure(returnedIn28Days);
		perc.setTarget(5.0);
		perc.setFalloff(2);
		perc.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		perc.setMinimalSize(50);
		perc.setExtensionFactor(1.2);
		perc.setAggregationFunction(PrivacyAwareAggregator.PERCENTAGE);
		return perc;

	}
	
	private MeasureDefinition buildTimeUntilAntibiotics() {
		CountMeasure instanceCount=new CountMeasure();
		instanceCount.setId("instances");
		instanceCount.setWhen(new TimeInstantCondition("START",GenericState.END));
		
		PrivacyAwareAggregatedMeasure totalCases = new PrivacyAwareAggregatedMeasure();
		totalCases.setBaseMeasure(instanceCount);
		totalCases.setEpsilon(0.1);
		totalCases.setLowerBounds(0);
		totalCases.setUpperBound(150);
		totalCases.setBaseMeasure(instanceCount);
		totalCases.setTarget(180);
		totalCases.setFalloff(2);
		totalCases.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		totalCases.setMinimalSize(50);
		totalCases.setAggregationFunction(PrivacyAwareAggregator.SUM);
		
		TimeMeasure cycleTime=new TimeMeasure();
		cycleTime.setFrom(new TimeInstantCondition("START", GenericState.END));
		cycleTime.setTo(new TimeInstantCondition("IV Antibiotics", GenericState.END));
		cycleTime.setUnitOfMeasure(TimeUnit.MINUTES);
		
		DerivedSingleInstanceMeasure lowerThanAnHour = new DerivedSingleInstanceMeasure();
		lowerThanAnHour.setId("lower");
		lowerThanAnHour.setFunction("duration<60");
		lowerThanAnHour.addUsedMeasure("duration", cycleTime);
		
		PrivacyAwareAggregatedMeasure avgWaitingTime = new PrivacyAwareAggregatedMeasure();
		avgWaitingTime.setEpsilon(0.1);
		avgWaitingTime.setLowerBounds(0);
		avgWaitingTime.setUpperBound(150);
		avgWaitingTime.setBaseMeasure(lowerThanAnHour);
		avgWaitingTime.setTarget(180);
		avgWaitingTime.setFalloff(2);
		avgWaitingTime.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		avgWaitingTime.setMinimalSize(50);
		avgWaitingTime.setExtensionFactor(1.2);
		avgWaitingTime.setAggregationFunction(PrivacyAwareAggregator.SUM_LAP);
		
		DerivedMultiInstanceMeasure percentage = new DerivedMultiInstanceMeasure();
		percentage.setFunction("a/b*100");
		percentage.addUsedMeasure("a", avgWaitingTime);
		percentage.addUsedMeasure("b", totalCases);
		
		//also using hack
		PrivacyAwareAggregatedMeasure perc = new PrivacyAwareAggregatedMeasure();
		perc.setEpsilon(0.1);
		perc.setLowerBounds(0);
		perc.setUpperBound(150);
		perc.setBaseMeasure(lowerThanAnHour);
		perc.setTarget(5.0);
		perc.setFalloff(2);
		perc.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		perc.setMinimalSize(50);
		perc.setExtensionFactor(1.2);
		perc.setAggregationFunction(PrivacyAwareAggregator.PERCENTAGE);
		
		return perc;
	}
	
	private MeasureDefinition buildTimeUntilLacticAcid() {
		CountMeasure instanceCount=new CountMeasure();
		instanceCount.setId("instances");
		instanceCount.setWhen(new TimeInstantCondition("START",GenericState.END));
		PrivacyAwareAggregatedMeasure totalCases = new PrivacyAwareAggregatedMeasure();
		totalCases.setBaseMeasure(instanceCount);
		totalCases.setEpsilon(0.1);
		totalCases.setLowerBounds(0);
		totalCases.setUpperBound(150);
		totalCases.setBaseMeasure(instanceCount);
		totalCases.setTarget(180);
		totalCases.setFalloff(2);
		totalCases.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		totalCases.setMinimalSize(50);
		totalCases.setAggregationFunction(PrivacyAwareAggregator.SUM);
		
		TimeMeasure cycleTime=new TimeMeasure();
		cycleTime.setFrom(new TimeInstantCondition("START", GenericState.END));
		cycleTime.setTo(new TimeInstantCondition("LacticAcid", GenericState.END));
		cycleTime.setUnitOfMeasure(TimeUnit.MINUTES);
		
		DerivedSingleInstanceMeasure lowerThanThreeHours = new DerivedSingleInstanceMeasure();
		lowerThanThreeHours.setId("lower");
		lowerThanThreeHours.setFunction("duration<180");
		lowerThanThreeHours.addUsedMeasure("duration", cycleTime);
		
		PrivacyAwareAggregatedMeasure avgWaitingTime = new PrivacyAwareAggregatedMeasure();
		avgWaitingTime.setId("time");
		avgWaitingTime.setEpsilon(0.1);
		avgWaitingTime.setLowerBounds(0);
		avgWaitingTime.setUpperBound(150);
		avgWaitingTime.setBaseMeasure(lowerThanThreeHours);
		avgWaitingTime.setTarget(180);
		avgWaitingTime.setFalloff(2);
		avgWaitingTime.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		avgWaitingTime.setMinimalSize(50);
		avgWaitingTime.setExtensionFactor(1.2);
		avgWaitingTime.setAggregationFunction(PrivacyAwareAggregator.SUM_LAP);
		
		DerivedMultiInstanceMeasure percentage = new DerivedMultiInstanceMeasure();
		percentage.setId("percentage");
		percentage.setFunction("a/b*100");
		percentage.addUsedMeasure("a", avgWaitingTime);
		percentage.addUsedMeasure("b", totalCases);
		
		
		//also using hack
		PrivacyAwareAggregatedMeasure perc = new PrivacyAwareAggregatedMeasure();
		perc.setEpsilon(0.1);
		perc.setLowerBounds(0);
		perc.setUpperBound(150);
		perc.setBaseMeasure(lowerThanThreeHours);
		perc.setTarget(5.0);
		perc.setFalloff(2);
		perc.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		perc.setMinimalSize(50);
		perc.setExtensionFactor(1.2);
		perc.setAggregationFunction(PrivacyAwareAggregator.PERCENTAGE);
		
		return perc;
		//return percentage;
	}
	
	
	//variants calculating the true result
	private MeasureDefinition buildAvgWaitingTimeUntilAdmissionNoPrivacy() {
		TimeMeasure cycleTime=new TimeMeasure();
		cycleTime.setFrom(new TimeInstantCondition("ER Registration", GenericState.END));
		cycleTime.setTo(new TimeInstantCondition("Admission NC", GenericState.END));
		cycleTime.setUnitOfMeasure(TimeUnit.HOURS);
		
		PrivacyAwareAggregatedMeasure avgWaitingTime = new PrivacyAwareAggregatedMeasure();	
		avgWaitingTime.setEpsilon(0.01);
		avgWaitingTime.setLowerBounds(0);
		avgWaitingTime.setUpperBound(150);
		avgWaitingTime.setBaseMeasure(cycleTime);
		avgWaitingTime.setTarget(24);
		avgWaitingTime.setFalloff(2);
		avgWaitingTime.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		avgWaitingTime.setMinimalSize(0);
		avgWaitingTime.setExtensionFactor(1.2);
		avgWaitingTime.setAggregationFunction(PrivacyAwareAggregator.AVG);
		return avgWaitingTime;
	}
	
	
	private MeasureDefinition buildAvgLengthOfStayNoPrivacy() {
		TimeMeasure cycleTime=new TimeMeasure();
		cycleTime.setFrom(new TimeInstantCondition("Admission NC", GenericState.END));
		cycleTime.setTo(new TimeInstantCondition("Release", GenericState.END));
		cycleTime.setUnitOfMeasure(TimeUnit.DAYS);
		
		PrivacyAwareAggregatedMeasure avgWaitingTime = new PrivacyAwareAggregatedMeasure();	
		avgWaitingTime.setEpsilon(0.1);
		avgWaitingTime.setLowerBounds(0);
		avgWaitingTime.setUpperBound(150);
		avgWaitingTime.setBaseMeasure(cycleTime);
		avgWaitingTime.setTarget(30);
		avgWaitingTime.setFalloff(1);
		avgWaitingTime.setBoundaryEstimation(BoundaryEstimator.EXTEND);
		avgWaitingTime.setMinimalSize(0);
		avgWaitingTime.setExtensionFactor(1.2);
		avgWaitingTime.setAggregationFunction(PrivacyAwareAggregator.AVG);
		return avgWaitingTime;
	}
	
	
	private MeasureDefinition buildMaxLengthOfStayNoPrivacy() {
		TimeMeasure cycleTime=new TimeMeasure();
		cycleTime.setFrom(new TimeInstantCondition("Admission NC", GenericState.END));
		cycleTime.setTo(new TimeInstantCondition("Release", GenericState.END));
		cycleTime.setUnitOfMeasure(TimeUnit.DAYS);
		
		PrivacyAwareAggregatedMeasure avgWaitingTime = new PrivacyAwareAggregatedMeasure();	
		avgWaitingTime.setEpsilon(0.1);
		avgWaitingTime.setLowerBounds(0);
		avgWaitingTime.setUpperBound(150);
		avgWaitingTime.setBaseMeasure(cycleTime);
		avgWaitingTime.setTarget(30);
		avgWaitingTime.setFalloff(2);
		avgWaitingTime.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		avgWaitingTime.setMinimalSize(0);
		avgWaitingTime.setExtensionFactor(1.2);
		avgWaitingTime.setAggregationFunction(PrivacyAwareAggregator.MAX);
		return avgWaitingTime;
	}
	
	
	private MeasureDefinition buildNoOfERReturnsNoPrivacy() {
		CountMeasure instanceCount=new CountMeasure();
		instanceCount.setId("instances");
		instanceCount.setWhen(new TimeInstantCondition("Start",GenericState.END));
		
		PrivacyAwareAggregatedMeasure totalCases = new PrivacyAwareAggregatedMeasure();
		totalCases.setBaseMeasure(instanceCount);
		totalCases.setEpsilon(0.1);
		totalCases.setLowerBounds(0);
		totalCases.setUpperBound(150);
		totalCases.setBaseMeasure(instanceCount);
		totalCases.setTarget(180);
		totalCases.setFalloff(2);
		totalCases.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		totalCases.setMinimalSize(0);
		totalCases.setAggregationFunction(PrivacyAwareAggregator.SUM);
		
		TimeMeasure returnTime=new TimeMeasure();
		returnTime.setFrom(new TimeInstantCondition("Release", GenericState.END));
		returnTime.setTo(new TimeInstantCondition("Return ER", GenericState.END));
		returnTime.setUnitOfMeasure(TimeUnit.DAYS);
		
		DerivedSingleInstanceMeasure returnedIn28Days = new DerivedSingleInstanceMeasure();
		returnedIn28Days.setFunction("r < 28");
		returnedIn28Days.addUsedMeasure("r", returnTime);
		
		PrivacyAwareAggregatedMeasure noOfReturnedPatients = new PrivacyAwareAggregatedMeasure();
		noOfReturnedPatients.setEpsilon(0.1);
		noOfReturnedPatients.setLowerBounds(0);
		noOfReturnedPatients.setUpperBound(150);
		noOfReturnedPatients.setBaseMeasure(returnedIn28Days);
		noOfReturnedPatients.setTarget(180);
		noOfReturnedPatients.setFalloff(2);
		noOfReturnedPatients.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		noOfReturnedPatients.setMinimalSize(0);
		noOfReturnedPatients.setExtensionFactor(1.2);
		noOfReturnedPatients.setAggregationFunction(PrivacyAwareAggregator.SUM);
		
		DerivedMultiInstanceMeasure percentage = new DerivedMultiInstanceMeasure();
		percentage.setFunction("a/b*100");
		percentage.addUsedMeasure("a", noOfReturnedPatients);
		percentage.addUsedMeasure("b", totalCases);
		return noOfReturnedPatients;
	}
	
	private MeasureDefinition buildTimeUntilAntibioticsNoPrivacy() {
		CountMeasure instanceCount=new CountMeasure();
		instanceCount.setId("instances");
		instanceCount.setWhen(new TimeInstantCondition("START",GenericState.END));
		
		PrivacyAwareAggregatedMeasure totalCases = new PrivacyAwareAggregatedMeasure();
		totalCases.setBaseMeasure(instanceCount);
		totalCases.setEpsilon(0.1);
		totalCases.setLowerBounds(0);
		totalCases.setUpperBound(150);
		totalCases.setBaseMeasure(instanceCount);
		totalCases.setTarget(180);
		totalCases.setFalloff(2);
		totalCases.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		totalCases.setMinimalSize(0);
		totalCases.setAggregationFunction(PrivacyAwareAggregator.SUM);
		
		TimeMeasure cycleTime=new TimeMeasure();
		cycleTime.setFrom(new TimeInstantCondition("START", GenericState.END));
		cycleTime.setTo(new TimeInstantCondition("IV Antibiotics", GenericState.END));
		cycleTime.setUnitOfMeasure(TimeUnit.MINUTES);
		
		DerivedSingleInstanceMeasure lowerThanThreeHours = new DerivedSingleInstanceMeasure();
		lowerThanThreeHours.setId("lower");
		lowerThanThreeHours.setFunction("duration<60");
		lowerThanThreeHours.addUsedMeasure("duration", cycleTime);
		
		PrivacyAwareAggregatedMeasure avgWaitingTime = new PrivacyAwareAggregatedMeasure();
		avgWaitingTime.setEpsilon(0.1);
		avgWaitingTime.setLowerBounds(0);
		avgWaitingTime.setUpperBound(150);
		avgWaitingTime.setBaseMeasure(lowerThanThreeHours);
		avgWaitingTime.setTarget(180);
		avgWaitingTime.setFalloff(2);
		avgWaitingTime.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		avgWaitingTime.setMinimalSize(0);
		avgWaitingTime.setExtensionFactor(1.2);
		avgWaitingTime.setAggregationFunction(PrivacyAwareAggregator.SUM);
		
		DerivedMultiInstanceMeasure percentage = new DerivedMultiInstanceMeasure();
		percentage.setFunction("a/b*100");
		percentage.addUsedMeasure("a", avgWaitingTime);
		percentage.addUsedMeasure("b", totalCases);
		return percentage;
	}
	
	private MeasureDefinition buildTimeUntilLacticAcidNoPrivacy() {
		CountMeasure instanceCount=new CountMeasure();
		instanceCount.setId("instances");
		instanceCount.setWhen(new TimeInstantCondition("START",GenericState.END));
		PrivacyAwareAggregatedMeasure totalCases = new PrivacyAwareAggregatedMeasure();
		totalCases.setBaseMeasure(instanceCount);
		totalCases.setEpsilon(0.1);
		totalCases.setLowerBounds(0);
		totalCases.setUpperBound(150);
		totalCases.setBaseMeasure(instanceCount);
		totalCases.setTarget(180);
		totalCases.setFalloff(2);
		totalCases.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		totalCases.setMinimalSize(0);
		totalCases.setAggregationFunction(PrivacyAwareAggregator.SUM);
		
		TimeMeasure cycleTime=new TimeMeasure();
		cycleTime.setFrom(new TimeInstantCondition("START", GenericState.END));
		cycleTime.setTo(new TimeInstantCondition("LacticAcid", GenericState.END));
		cycleTime.setUnitOfMeasure(TimeUnit.MINUTES);
		
		DerivedSingleInstanceMeasure lowerThanThreeHours = new DerivedSingleInstanceMeasure();
		lowerThanThreeHours.setId("lower");
		lowerThanThreeHours.setFunction("duration<180");
		lowerThanThreeHours.addUsedMeasure("duration", cycleTime);
		
		PrivacyAwareAggregatedMeasure avgWaitingTime = new PrivacyAwareAggregatedMeasure();
		avgWaitingTime.setId("time");
		avgWaitingTime.setEpsilon(0.1);
		avgWaitingTime.setLowerBounds(0);
		avgWaitingTime.setUpperBound(150);
		avgWaitingTime.setBaseMeasure(lowerThanThreeHours);
		avgWaitingTime.setTarget(180);
		avgWaitingTime.setFalloff(2);
		avgWaitingTime.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		avgWaitingTime.setMinimalSize(10);
		avgWaitingTime.setExtensionFactor(1.2);
		avgWaitingTime.setAggregationFunction(PrivacyAwareAggregator.SUM);
		
		DerivedMultiInstanceMeasure percentage = new DerivedMultiInstanceMeasure();
		percentage.setId("percentage");
		percentage.setFunction("a/b*100");
		percentage.addUsedMeasure("a", avgWaitingTime);
		percentage.addUsedMeasure("b", totalCases);
		return percentage;
	}
	
	
	
	
	
}
