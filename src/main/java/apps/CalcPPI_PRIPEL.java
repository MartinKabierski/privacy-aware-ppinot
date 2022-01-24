package apps;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import es.us.isa.ppinot.evaluation.Measure;
import es.us.isa.ppinot.evaluation.TemporalMeasureScope;
import es.us.isa.ppinot.evaluation.evaluators.MeasureEvaluator;
import es.us.isa.ppinot.evaluation.logs.LogProvider;
import es.us.isa.ppinot.evaluation.logs.MXMLLog;
import es.us.isa.ppinot.model.MeasureDefinition;
import es.us.isa.ppinot.model.TimeUnit;
import es.us.isa.ppinot.model.base.CountMeasure;
import es.us.isa.ppinot.model.base.TimeMeasure;
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
	
public class CalcPPI_PRIPEL {
	
	//private static final Schedule WORKINGHOURS = new Schedule(DateTimeConstants.MONDAY, DateTimeConstants.FRIDAY, new LocalTime())
	
	//create PPI's based on privatized PRIPEL logs (using epsilon=0.1 and p=20)
	public static void main(String[] args) throws Exception {
		CalcPPI_PRIPEL app = new CalcPPI_PRIPEL();
		List<Measure> pripelMeasures = null;
		List<Measure> pappiMeasures = null;
		List<Measure> trueMeasures = null;
		
		int repetitions = 10;
	    BufferedWriter writer = new BufferedWriter(new FileWriter("evaluation_sepsis_PRIPEL_TEST.csv"));
	    writer.write("Kpi;From;To;Algorithm;NoOfValues;Result;Orig\n");
		LogProvider log = null;

		MXMLLog sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);

		//avg waiting time until admission
	    trueMeasures = app.computePPI(sepsisLog, app.buildAvgWaitingTimeUntilAdmissionNoPrivacy());
		for(int i=1;i<=repetitions;i++) {
			System.out.println("Evaluating KPI 1 - Repetition "+i);
			
			//PaPPI
			sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
			pappiMeasures = app.computePPI(sepsisLog, app.buildAvgWaitingTimeUntilAdmission());
			
			//PRIPEL
			log = new MXMLLog(new FileInputStream(new File(String.join(File.separator, "input", "Sepsis_epsilon_0.1_P20_anonymizied_"+i+".mxml"))),null);
			pripelMeasures = app.computePPI(log, app.buildAvgWaitingTimeUntilAdmissionNoPrivacy());
			writeMeasuresToFile("AvgWaitingTimeUntilAdmission",pappiMeasures ,pripelMeasures,trueMeasures,repetitions, writer);
		}
		
		//avg length of stay
		sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
	    trueMeasures = app.computePPI(sepsisLog, app.buildAvgLengthOfStayNoPrivacy());
		for(int i=1;i<repetitions;i++) {
			System.out.println("Evaluating KPI 2 - Repetition "+i);
			
			//PaPPI
			sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
			pappiMeasures = app.computePPI(sepsisLog, app.buildAvgLengthOfStay());
			
			//PRIPEL
			log = new MXMLLog(new FileInputStream(new File(String.join(File.separator, "input", "Sepsis_epsilon_0.1_P20_anonymizied_"+i+".mxml"))),null);			
			pripelMeasures = app.computePPI(log, app.buildAvgLengthOfStayNoPrivacy());
			writeMeasuresToFile("AvgLengthOfStay",pappiMeasures ,pripelMeasures,trueMeasures,repetitions, writer);
		}

		//max length of stay
		sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
	    trueMeasures = app.computePPI(sepsisLog, app.buildMaxLengthOfStayNoPrivacy());
		for(int i=1;i<repetitions;i++) {
			System.out.println("Evaluating KPI 3 - Repetition "+i);
			
			//PaPPI
			sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
			pappiMeasures = app.computePPI(sepsisLog, app.buildMaxLengthOfStay());
			
			//PRIPEL
			log = new MXMLLog(new FileInputStream(new File(String.join(File.separator, "input", "Sepsis_epsilon_0.1_P20_anonymizied_"+i+".mxml"))),null);		
			pripelMeasures = app.computePPI(log, app.buildMaxLengthOfStayNoPrivacy());
			writeMeasuresToFile("MaxLengthOfStay",pappiMeasures ,pripelMeasures,trueMeasures,repetitions, writer);
		}

		//Fraction of patients that returned to er - total not privatized
		sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
		trueMeasures = app.computePPI(sepsisLog, app.buildNoOfERReturnsNoPrivacy());
		for(int i=1;i<repetitions;i++) {
			System.out.println("Evaluating KPI 4.1 - Repetition "+i);
			
			//PaPPI
			sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
			pappiMeasures = app.computePPI(sepsisLog, app.buildNoOfERReturns(false, false));

			//PRIPEL
			log = new MXMLLog(new FileInputStream(new File(String.join(File.separator, "input", "Sepsis_epsilon_0.1_P20_anonymizied_"+i+".mxml"))),null);
			pripelMeasures = app.computePPI(log, app.buildNoOfERReturnsNoPrivacy());
			writeMeasuresToFile("%ReturningPatientsTotalNotPrivatized",pappiMeasures ,pripelMeasures,trueMeasures,repetitions, writer);
		}
		
		//total privatized
		sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
		trueMeasures = app.computePPI(sepsisLog, app.buildNoOfERReturnsNoPrivacy());
		for(int i=1;i<repetitions;i++) {
			System.out.println("Evaluating KPI 4.2 - Repetition "+i);
			
			//PaPPI
			sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
			pappiMeasures = app.computePPI(sepsisLog, app.buildNoOfERReturns(true, false));

			//PRIPEL
			log = new MXMLLog(new FileInputStream(new File(String.join(File.separator, "input", "Sepsis_epsilon_0.1_P20_anonymizied_"+i+".mxml"))),null);
			pripelMeasures = app.computePPI(log, app.buildNoOfERReturnsNoPrivacy());
			writeMeasuresToFile("%ReturningPatientsTotalPrivatized",pappiMeasures ,pripelMeasures,trueMeasures,repetitions, writer);
		}
		
		//sample-and-aggregate
		sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
		trueMeasures = app.computePPI(sepsisLog, app.buildNoOfERReturnsNoPrivacy());
		for(int i=1;i<repetitions;i++) {
			System.out.println("Evaluating KPI 4.3 - Repetition "+i);
			
			//PaPPI
			sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
			pappiMeasures = app.computePPI(sepsisLog, app.buildNoOfERReturns(false, true));

			//PRIPEL
			log = new MXMLLog(new FileInputStream(new File(String.join(File.separator, "input", "Sepsis_epsilon_0.1_P20_anonymizied_"+i+".mxml"))),null);
			pripelMeasures = app.computePPI(log, app.buildNoOfERReturnsNoPrivacy());
			writeMeasuresToFile("%ReturningPatientsSampleAggregate",pappiMeasures ,pripelMeasures,trueMeasures,repetitions, writer);
		}	

		//Fraction of cases where antibiotics were given in one hour
		sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
		trueMeasures = app.computePPI(sepsisLog, app.buildTimeUntilAntibioticsNoPrivacy());
		for(int i=1;i<repetitions;i++) {
			System.out.println("Evaluating KPI 5.1 - Repetition "+i);
			
			//PaPPI
			sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
			pappiMeasures = app.computePPI(sepsisLog, app.buildTimeUntilAntibiotics(false, false));
			
			//PRIPEL
			log = new MXMLLog(new FileInputStream(new File(String.join(File.separator, "input", "Sepsis_epsilon_0.1_P20_anonymizied_"+i+".mxml"))),null);		
			pripelMeasures = app.computePPI(log, app.buildTimeUntilAntibioticsNoPrivacy());
			writeMeasuresToFile("%AntibioticsWithinOneHourTotalNotPrivatized",pappiMeasures ,pripelMeasures,trueMeasures,repetitions, writer);
		}
		
		sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
		trueMeasures = app.computePPI(sepsisLog, app.buildTimeUntilAntibioticsNoPrivacy());
		for(int i=1;i<repetitions;i++) {
			System.out.println("Evaluating KPI 5.2 - Repetition "+i);
			
			//PaPPI
			sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
			pappiMeasures = app.computePPI(sepsisLog, app.buildTimeUntilAntibiotics(true, false));
			
			//PRIPEL
			log = new MXMLLog(new FileInputStream(new File(String.join(File.separator, "input", "Sepsis_epsilon_0.1_P20_anonymizied_"+i+".mxml"))),null);		
			pripelMeasures = app.computePPI(log, app.buildTimeUntilAntibioticsNoPrivacy());
			writeMeasuresToFile("%AntibioticsWithinOneHourTotalPrivatized",pappiMeasures ,pripelMeasures,trueMeasures,repetitions, writer);
		}
		
		sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
		trueMeasures = app.computePPI(sepsisLog, app.buildTimeUntilAntibioticsNoPrivacy());
		for(int i=1;i<repetitions;i++) {
			System.out.println("Evaluating KPI 5.3 - Repetition "+i);
			
			//PaPPI
			sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
			pappiMeasures = app.computePPI(sepsisLog, app.buildTimeUntilAntibiotics(false, true));
			
			//PRIPEL
			log = new MXMLLog(new FileInputStream(new File(String.join(File.separator, "input", "Sepsis_epsilon_0.1_P20_anonymizied_"+i+".mxml"))),null);		
			pripelMeasures = app.computePPI(log, app.buildTimeUntilAntibioticsNoPrivacy());
			writeMeasuresToFile("%AntibioticsWithinOneHourSampleAggregate",pappiMeasures ,pripelMeasures,trueMeasures,repetitions, writer);
		}
		
		//Fraction of cases where lactic acid check was done in one hour
		sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
		trueMeasures = app.computePPI(sepsisLog, app.buildTimeUntilLacticAcidNoPrivacy());
		for(int i=1;i<repetitions;i++) {
			System.out.println("Evaluating KPI 6.1 - Repetition "+i);
			
			//PaPPI
			sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
			pappiMeasures = app.computePPI(sepsisLog, app.buildTimeUntilLacticAcid(false, false));

			//PRIPEL
			log = new MXMLLog(new FileInputStream(new File(String.join(File.separator, "input", "Sepsis_epsilon_0.1_P20_anonymizied_"+i+".mxml"))),null);		
			pripelMeasures = app.computePPI(log, app.buildTimeUntilLacticAcidNoPrivacy());
			writeMeasuresToFile("%LacticAcidWithinThreeHoursTotalNotPrivatized",pappiMeasures ,pripelMeasures,trueMeasures,repetitions, writer);
		}
		
		sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
		trueMeasures = app.computePPI(sepsisLog, app.buildTimeUntilLacticAcidNoPrivacy());
		for(int i=1;i<repetitions;i++) {
			System.out.println("Evaluating KPI 6.2 - Repetition "+i);
			
			//PaPPI
			sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
			pappiMeasures = app.computePPI(sepsisLog, app.buildTimeUntilLacticAcid(true, false));

			//PRIPEL
			log = new MXMLLog(new FileInputStream(new File(String.join(File.separator, "input", "Sepsis_epsilon_0.1_P20_anonymizied_"+i+".mxml"))),null);		
			pripelMeasures = app.computePPI(log, app.buildTimeUntilLacticAcidNoPrivacy());
			writeMeasuresToFile("%LacticAcidWithinThreeHoursTotalPrivatized",pappiMeasures ,pripelMeasures,trueMeasures,repetitions, writer);
		}
		
		sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
		trueMeasures = app.computePPI(sepsisLog, app.buildTimeUntilLacticAcidNoPrivacy());
		for(int i=1;i<repetitions;i++) {
			System.out.println("Evaluating KPI 6.3 - Repetition "+i);
			
			//PaPPI
			sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
			pappiMeasures = app.computePPI(sepsisLog, app.buildTimeUntilLacticAcid(false, true));

			//PRIPEL
			log = new MXMLLog(new FileInputStream(new File(String.join(File.separator, "input", "Sepsis_epsilon_0.1_P20_anonymizied_"+i+".mxml"))),null);		
			pripelMeasures = app.computePPI(log, app.buildTimeUntilLacticAcidNoPrivacy());
			writeMeasuresToFile("%LacticAcidWithinThreeHoursSampleAggregate",pappiMeasures ,pripelMeasures,trueMeasures,repetitions, writer);
		}
		writer.close();
	}


	private static void writeMeasuresToFile(String kpi, List<Measure> pappiMeasures, List<Measure> priepelMeasures, List<Measure> trueMeasures,
			int repetitions, BufferedWriter writer) throws IOException {
		//add all time scopes from the measures
		List<TemporalMeasureScope> timeScopes = new ArrayList<TemporalMeasureScope>();
		pappiMeasures.stream().forEach(x -> timeScopes.add((TemporalMeasureScope)x.getMeasureScope()));
		//priepelMeasures.stream().forEach(x -> timeScopes.add((TemporalMeasureScope)x.getMeasureScope()));
		trueMeasures.stream().forEach(x -> timeScopes.add((TemporalMeasureScope)x.getMeasureScope()));
		
		//order timescopes and remove duplicates
		List<TemporalMeasureScope> orderedTimeScopes = timeScopes.stream().collect(Collectors.toList());
		orderedTimeScopes.sort((a, b) -> a.getStart().compareTo(b.getStart()));
		for (int i=orderedTimeScopes.size()-1;i>0 ;i--) {
			TemporalMeasureScope m = orderedTimeScopes.get(i);
			TemporalMeasureScope prior = orderedTimeScopes.get(i-1);
			if (m.getStart().equals(prior.getStart()) && m.getEnd().equals(prior.getEnd())) {
				orderedTimeScopes.remove(i);
			}
		}
		//orderedTimeScopes.stream().forEach(x -> System.out.println(x.getStart()+" "+x.getEnd()));
		
		//for each timescope, get data from measures and/or trueMeasures
		for(TemporalMeasureScope t : orderedTimeScopes) {
			
			//use Double for explicit modelling of NaN
			Double pappiMeasureSize = Double.NaN;
			Double pappiMeasureValue = Double.NaN;
			
			Double priepelMeasureSize = Double.NaN;
			Double priepelMeasureValue = Double.NaN;
			
			Double trueMeasureValue = Double.NaN;
			//System.out.println(t.getStart()+" -> "+t.getEnd());
			
			for(Measure m : pappiMeasures) {
				if (((TemporalMeasureScope)m.getMeasureScope()).getStart().equals(t.getStart()) && ((TemporalMeasureScope)m.getMeasureScope()).getEnd().equals(t.getEnd())) {
					if (!new Double(m.getValue()).isNaN()) {
						//System.out.println("Pappi: " + m.getValue());
						pappiMeasureSize = (double)m.getInstances().size();
						pappiMeasureValue = m.getValue();
						//System.out.println(pappiMeasureSize);
						break;
					}
				}
			}			
			for(Measure m : priepelMeasures) {
				if (((TemporalMeasureScope)m.getMeasureScope()).getStart().equals(t.getStart()) && ((TemporalMeasureScope)m.getMeasureScope()).getEnd().equals(t.getEnd())) {
					if (!new Double(m.getValue()).isNaN()) {
						//System.out.println("Priepel: "+m.getValue());
						priepelMeasureSize = (double)m.getInstances().size();
						priepelMeasureValue = m.getValue();
						break;
					}
				}
			}
			for(Measure m : trueMeasures) {
				if (((TemporalMeasureScope)m.getMeasureScope()).getStart().equals(t.getStart()) && ((TemporalMeasureScope)m.getMeasureScope()).getEnd().equals(t.getEnd())) {
					if (!new Double(m.getValue()).isNaN()) {
					//System.out.println("Original: "+m.getValue());
					trueMeasureValue = m.getValue();
					break;
					}
				}
			}

			
			//write data into file
			writer.write(String.join(";",
					kpi,
					t.getStart().toString(),
					t.getEnd().toString(),
					"PaPPI",
					pappiMeasureSize.isNaN()?"NaN":pappiMeasureSize.toString(),
					pappiMeasureValue.isNaN()?"NaN":pappiMeasureValue.toString(),
					trueMeasureValue.isNaN()?"NaN":trueMeasureValue.toString()
					));
			writer.write("\n");
			writer.write(String.join(";",
					kpi,
					t.getStart().toString(),
					t.getEnd().toString(),
					"Priepel",
					priepelMeasureSize.isNaN()?"NaN":priepelMeasureSize.toString(),
					priepelMeasureValue.isNaN()?"NaN":priepelMeasureValue.toString(),
					trueMeasureValue.isNaN()?"NaN":trueMeasureValue.toString()
					));
			writer.write("\n");
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
		avgWaitingTime.setBaseMeasure(cycleTime);
		avgWaitingTime.setBoundaryEstimation(BoundaryEstimator.MINMAX);
		avgWaitingTime.setAggregationFunction(PrivacyAwareAggregator.AVG_EXP);
		return avgWaitingTime;
	}
	
	
	private MeasureDefinition buildAvgLengthOfStay() {
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
	
	
	private MeasureDefinition buildMaxLengthOfStay() {
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
	
	private MeasureDefinition buildNoOfERReturns(boolean privatizeTotal, boolean aggregate) {
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
	
	private MeasureDefinition buildTimeUntilAntibiotics(boolean privatizeTotal, boolean aggregate) {
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
	
	private MeasureDefinition buildTimeUntilLacticAcid(boolean privatizeTotal, boolean aggregate) {
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
	private MeasureDefinition buildAvgWaitingTimeUntilAdmissionNoPrivacy() {
		TimeMeasure cycleTime=new TimeMeasure();
		cycleTime.setFrom(new TimeInstantCondition("ER Registration", GenericState.END));
		cycleTime.setTo(new TimeInstantCondition("Admission NC", GenericState.END));
		cycleTime.setUnitOfMeasure(TimeUnit.HOURS);
		
		PrivacyAwareAggregatedMeasure avgWaitingTime = new PrivacyAwareAggregatedMeasure();	
		avgWaitingTime.setBaseMeasure(cycleTime);
		avgWaitingTime.setAggregationFunction(PrivacyAwareAggregator.AVG);	
		
		return avgWaitingTime;
	}
	
	
	private MeasureDefinition buildAvgLengthOfStayNoPrivacy() {
		TimeMeasure cycleTime=new TimeMeasure();
		cycleTime.setFrom(new TimeInstantCondition("Admission NC", GenericState.END));
		cycleTime.setTo(new TimeInstantCondition("Release A", GenericState.END));
		cycleTime.setUnitOfMeasure(TimeUnit.DAYS);
		
		PrivacyAwareAggregatedMeasure avgWaitingTime = new PrivacyAwareAggregatedMeasure();	
		avgWaitingTime.setBaseMeasure(cycleTime);
		avgWaitingTime.setAggregationFunction(PrivacyAwareAggregator.AVG);
		return avgWaitingTime;
	}
	
	
	private MeasureDefinition buildMaxLengthOfStayNoPrivacy() {
		TimeMeasure cycleTime=new TimeMeasure();
		cycleTime.setFrom(new TimeInstantCondition("Admission NC", GenericState.END));
		cycleTime.setTo(new TimeInstantCondition("Release A", GenericState.END));
		cycleTime.setUnitOfMeasure(TimeUnit.DAYS);
		
		PrivacyAwareAggregatedMeasure avgWaitingTime = new PrivacyAwareAggregatedMeasure();	
		avgWaitingTime.setBaseMeasure(cycleTime);
		avgWaitingTime.setAggregationFunction(PrivacyAwareAggregator.MAX);
		return avgWaitingTime;
	}
	
	
	private MeasureDefinition buildNoOfERReturnsNoPrivacy() {
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
	
	private MeasureDefinition buildTimeUntilAntibioticsNoPrivacy() {
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
	
	private MeasureDefinition buildTimeUntilLacticAcidNoPrivacy() {
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
