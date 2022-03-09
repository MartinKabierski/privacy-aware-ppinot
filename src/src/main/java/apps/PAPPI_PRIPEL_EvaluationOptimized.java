package apps;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import apps.ppiDefinitions.SepsisPPIsNoPrivacy;
import apps.ppiDefinitions.SepsisPPIsOptimized;
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
	
public class PAPPI_PRIPEL_EvaluationOptimized {
		
	static double EPSILON = 0.1;
	
	public static void evaluatePPI(String PPI, BufferedWriter writer) throws Exception{
		PAPPI_PRIPEL_EvaluationOptimized app = new PAPPI_PRIPEL_EvaluationOptimized();
		
		int repetitions = 10;
		
		List<Measure> pripelMeasures = null;
		List<Measure> pappiMeasures = null;
		List<Measure> trueMeasures = null;
		
		LogProvider log = null;
		MXMLLog sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);

		//avg waiting time until admission
	    trueMeasures = app.computePPI(sepsisLog, app.createPPI(PPI, false));
		for(int i=1;i<=repetitions;i++) {	
			System.out.println("Evaluating KPI "+PPI+" - Repetition "+i);
			
			//PaPPI
			sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
			pappiMeasures = app.computePPI(sepsisLog, app.createPPI(PPI, true));
			
			//PRIPEL
			log = new MXMLLog(new FileInputStream(new File(String.join(File.separator, "input", "Sepsis_epsilon_0.1_P20_anonymizied_"+i+".mxml"))),null);
			pripelMeasures = app.computePPI(log, app.createPPI(PPI, false));
			
			writeMeasuresToFile(PPI,pappiMeasures,pripelMeasures,trueMeasures,repetitions, writer);
		}
	}
	
	public MeasureDefinition createPPI(String PPI, boolean privatized) {
		MeasureDefinition measure = null;
		switch(PPI) {
			case "AvgWaitingTimeUntilAdmission":
				measure = privatized?SepsisPPIsOptimized.buildAvgWaitingTimeUntilAdmission(EPSILON):SepsisPPIsNoPrivacy.buildAvgWaitingTimeUntilAdmission();
				break;
			case "AvgLengthOfStay":
				measure = privatized?SepsisPPIsOptimized.buildAvgLengthOfStay(EPSILON):SepsisPPIsNoPrivacy.buildAvgLengthOfStay();
				break;
			case "MaxLengthOfStay":
				measure = privatized?SepsisPPIsOptimized.buildMaxLengthOfStay(EPSILON):SepsisPPIsNoPrivacy.buildMaxLengthOfStay();
				break;
			case "%ReturningPatients<28Days":
				measure = privatized?SepsisPPIsOptimized.buildReturnedIn28days(EPSILON):SepsisPPIsNoPrivacy.buildReturnedIn28days();
				break;
			case "%AntibioticsWithinOneHour":
				measure = privatized?SepsisPPIsOptimized.buildTimeUntilAntibiotics(EPSILON):SepsisPPIsNoPrivacy.buildTimeUntilAntibiotics();
				break;
			case "%LacticAcidWithinThreeHours":
				measure = privatized?SepsisPPIsOptimized.buildTimeUntilLacticAcid(EPSILON):SepsisPPIsNoPrivacy.buildTimeUntilLacticAcid();
				break;
		}
		return measure;
	}
	
	
	//create PPI's based on privatized PRIPEL logs (using epsilon=0.1 and p=20)
	public static void main(String[] args) throws Exception {
		String[] ppis = {"AvgWaitingTimeUntilAdmission","AvgLengthOfStay","MaxLengthOfStay","%ReturningPatients<28Days","%AntibioticsWithinOneHour","%LacticAcidWithinThreeHours"};
		
	    BufferedWriter writer = new BufferedWriter(new FileWriter("evaluation_PAPPI_PRIPEL_optimized.csv"));
	    writer.write("Kpi;From;To;Algorithm;NoOfValues;Result;Orig\n");
		
		for (String ppi : ppis) {
			evaluatePPI(ppi, writer);
		}
		
		writer.close();
		
		/*
		PAPPI_PRIPEL_EvaluationOptimized app = new PAPPI_PRIPEL_EvaluationOptimized();
		List<Measure> pripelMeasures = null;
		List<Measure> pappiMeasures = null;
		List<Measure> trueMeasures = null;
		
		int repetitions = 10;
	    BufferedWriter writer = new BufferedWriter(new FileWriter("evaluation_PAPPI_PRIPEL_optimized.csv"));
	    writer.write("Kpi;From;To;Algorithm;NoOfValues;Result;Orig\n");
		LogProvider log = null;

		MXMLLog sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);

		//avg waiting time until admission
	    trueMeasures = app.computePPI(sepsisLog, SepsisPPIsNoPrivacy.buildAvgWaitingTimeUntilAdmission());
		for(int i=1;i<=repetitions;i++) {
			System.out.println("Evaluating KPI 1 - Repetition "+i);
			//PaPPI
			sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
			pappiMeasures = app.computePPI(sepsisLog, SepsisPPIsOptimized.buildAvgWaitingTimeUntilAdmission(EPSILON));
			
			//PRIPEL
			log = new MXMLLog(new FileInputStream(new File(String.join(File.separator, "input", "Sepsis_epsilon_0.1_P20_anonymizied_"+i+".mxml"))),null);
			pripelMeasures = app.computePPI(log, SepsisPPIsNoPrivacy.buildAvgWaitingTimeUntilAdmission());
			writeMeasuresToFile("AvgWaitingTimeUntilAdmission",pappiMeasures ,pripelMeasures,trueMeasures,repetitions, writer);
		}
		
		//avg length of stay
		sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
	    trueMeasures = app.computePPI(sepsisLog, SepsisPPIsNoPrivacy.buildAvgLengthOfStay());
		for(int i=1;i<repetitions;i++) {
			System.out.println("Evaluating KPI 2 - Repetition "+i);
			
			//PaPPI
			sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
			pappiMeasures = app.computePPI(sepsisLog, SepsisPPIsOptimized.buildAvgLengthOfStay(EPSILON));
			
			//PRIPEL
			log = new MXMLLog(new FileInputStream(new File(String.join(File.separator, "input", "Sepsis_epsilon_0.1_P20_anonymizied_"+i+".mxml"))),null);			
			pripelMeasures = app.computePPI(log, SepsisPPIsNoPrivacy.buildAvgLengthOfStay());
			writeMeasuresToFile("AvgLengthOfStay",pappiMeasures ,pripelMeasures,trueMeasures,repetitions, writer);
		}

		//max length of stay
		sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
	    trueMeasures = app.computePPI(sepsisLog, SepsisPPIsNoPrivacy.buildMaxLengthOfStay());
		for(int i=1;i<repetitions;i++) {
			System.out.println("Evaluating KPI 3 - Repetition "+i);
			
			//PaPPI
			sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
			pappiMeasures = app.computePPI(sepsisLog, SepsisPPIsOptimized.buildMaxLengthOfStay(EPSILON));
			
			//PRIPEL
			log = new MXMLLog(new FileInputStream(new File(String.join(File.separator, "input", "Sepsis_epsilon_0.1_P20_anonymizied_"+i+".mxml"))),null);		
			pripelMeasures = app.computePPI(log, SepsisPPIsNoPrivacy.buildMaxLengthOfStay());
			writeMeasuresToFile("MaxLengthOfStay",pappiMeasures ,pripelMeasures,trueMeasures,repetitions, writer);
		}

		//Fraction of patients that returned to er - total not privatized
		sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
		trueMeasures = app.computePPI(sepsisLog, SepsisPPIsNoPrivacy.buildReturnedIn28days());
		for(int i=1;i<repetitions;i++) {
			System.out.println("Evaluating KPI 4 - Repetition "+i);
			
			//PaPPI
			sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
			pappiMeasures = app.computePPI(sepsisLog, SepsisPPIsOptimized.buildReturnedIn28days(EPSILON));

			//PRIPEL
			log = new MXMLLog(new FileInputStream(new File(String.join(File.separator, "input", "Sepsis_epsilon_0.1_P20_anonymizied_"+i+".mxml"))),null);
			pripelMeasures = app.computePPI(log, SepsisPPIsNoPrivacy.buildReturnedIn28days());
			writeMeasuresToFile("%ReturningPatients<28Days",pappiMeasures ,pripelMeasures,trueMeasures,repetitions, writer);
		}

		//Fraction of cases where antibiotics were given in one hour
		sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
		trueMeasures = app.computePPI(sepsisLog, SepsisPPIsNoPrivacy.buildTimeUntilAntibiotics());
		for(int i=1;i<repetitions;i++) {
			System.out.println("Evaluating KPI 5 - Repetition "+i);
			
			//PaPPI
			sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
			pappiMeasures = app.computePPI(sepsisLog, SepsisPPIsOptimized.buildTimeUntilAntibiotics(EPSILON));
			
			//PRIPEL
			log = new MXMLLog(new FileInputStream(new File(String.join(File.separator, "input", "Sepsis_epsilon_0.1_P20_anonymizied_"+i+".mxml"))),null);		
			pripelMeasures = app.computePPI(log, SepsisPPIsNoPrivacy.buildTimeUntilAntibiotics());
			writeMeasuresToFile("%AntibioticsWithinOneHour",pappiMeasures ,pripelMeasures,trueMeasures,repetitions, writer);
		}

		
		//Fraction of cases where lactic acid check was done in one hour
		sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
		trueMeasures = app.computePPI(sepsisLog, SepsisPPIsNoPrivacy.buildTimeUntilLacticAcid());
		for(int i=1;i<repetitions;i++) {
			System.out.println("Evaluating KPI 6 - Repetition "+i);
			
			//PaPPI
			sepsisLog = new MXMLLog(new FileInputStream(new File(String.join(File.separator,"input","Sepsis Cases - Event Log.mxml"))),null);
			pappiMeasures = app.computePPI(sepsisLog, SepsisPPIsOptimized.buildTimeUntilLacticAcid(EPSILON));

			//PRIPEL
			log = new MXMLLog(new FileInputStream(new File(String.join(File.separator, "input", "Sepsis_epsilon_0.1_P20_anonymizied_"+i+".mxml"))),null);		
			pripelMeasures = app.computePPI(log, SepsisPPIsNoPrivacy.buildTimeUntilLacticAcid());
			writeMeasuresToFile("%LacticAcidWithinThreeHours",pappiMeasures ,pripelMeasures,trueMeasures,repetitions, writer);
		}
		writer.close();*/
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
					"Pripel",
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
}
