package pappi.evaluators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import es.us.isa.ppinot.evaluation.Measure;
import es.us.isa.ppinot.evaluation.computers.MeasureComputer;
import es.us.isa.ppinot.evaluation.evaluators.LogMeasureEvaluator;
import es.us.isa.ppinot.evaluation.logs.LogConfigurer;
import es.us.isa.ppinot.evaluation.logs.LogProvider;
import es.us.isa.ppinot.model.MeasureDefinition;
import es.us.isa.ppinot.model.ProcessInstanceFilter;
import pappi.computers.PrivacyAwareMeasureComputerFactory;

public class PrivacyAwareLogMeasureEvaluator extends LogMeasureEvaluator {

	    private static final Logger log = Logger.getLogger(PrivacyAwareLogMeasureEvaluator.class.getName());

	    private LogProvider logProvider;
	    private LogConfigurer configurer;
	    private PrivacyAwareMeasureComputerFactory factory;

	    public PrivacyAwareLogMeasureEvaluator(LogProvider logProvider) {
	    	super(logProvider);
	        this.logProvider = logProvider;
	        this.factory = new PrivacyAwareMeasureComputerFactory();
	    }

	    public List<Measure> eval(MeasureDefinition definition, ProcessInstanceFilter filter) {
	        return eval(Arrays.asList(definition), filter).get(definition);
	    }

	    public Map<MeasureDefinition, List<Measure>> eval(List<MeasureDefinition> definitions, ProcessInstanceFilter filter) {
	        LogProvider logToAnalyse;
	        Map<MeasureDefinition, List<Measure>> measures = new HashMap<MeasureDefinition, List<Measure>>();
	        Map<MeasureComputer, MeasureDefinition> computers = new HashMap<MeasureComputer, MeasureDefinition>();

	        if (configurer == null) {
	            logToAnalyse = logProvider;
	        } else {
	            logToAnalyse = configurer.configure(logProvider);
	        }

	        for (MeasureDefinition definition : definitions) {
	            MeasureComputer computer = factory.create(definition, filter);
	            computers.put(computer, definition);
	            logToAnalyse.registerListener(computer);
	            measures.put(definition, new ArrayList<Measure>());
	        }

	        log.info("Processing log...");
	        logToAnalyse.processLog();

	        log.info("Computing measures...");
	        for (MeasureComputer computer : computers.keySet()) {
	            measures.get(computers.get(computer)).addAll(computer.compute());
	        }

	        return measures;
	    }

	}

