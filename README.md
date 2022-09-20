# PaPPI: Privacy-aware Process Performance Indicators
PaPPI is an extension of the [Process Performance Indicator Notation](https://github.com/isa-group/ppinot), that adds privacy protection in the form of $\epsilon,0$-differential privacy to the definition and evaluation of Process Performance Indicators. 

PaPPI implements the framework and release mechanisms proposed in the articles "[Privacy-aware Process Performance Indicators: Framework and Release Mechanisms](https://arxiv.org/abs/2103.11740)" and the associated demo paper (under review).

Currently, PaPPI includes the following features:
* Privatization of the common Aggregation Measures (avg, sum, min, max) using the Laplace mechanism and the Interval Mechanism
* Privatization of multi-instance derived Measures using Sample-and-Aggregate
* (Experimental) Automated Selection of the optimal release mechanisms in terms of expected result accuracy
* (Experimental) Re-Use of privatized shared sub-trees to preserve privacy budget during the evaluation of multiple PPIs that partly access the same trace information

## Installation
First, clone or download this repository to your local device. Then, resolve the dependencies specified in the pom.xml file using Maven.

Once, all dependencies have been resolved, you are good to go.

## Defining and Evaluating PPIs
The definition and evaluation of PPIs follows the general structure of basic PPINOT (see [the PPINOT-example repository](https://github.com/isa-group/ppinot-example) for examples on how to define PPIs using PPINOT). For instance, in the following is an example of how to define and evaluate a slightly simplified version of the "Average duration"-PPI from the PPINOT example repository.
```java
//Load Log
LogProvider log = new MXMLLog(new FileInputStream(new File("simulation_logs.mxml")),null);

//PPI Definition
TimeMeasure duration = new TimeMeasure();
duration.setFrom(new TimeInstantCondition("EVENT 2 START MESSAGE", GenericState.START));
duration.setTo(new TimeInstantCondition("FI closed", GenericState.END));
duration.setUnitOfMeasure(TimeUnit.HOURS);

PrivacyAwareAggregatedMeasure privatizedAvg = new PrivacyAwareAggregatedMeasure();
privatizedAvg.setBaseMeasure(duration);
privatizedAvg.setAggregationFunction(PrivacyAwareAggregator.AVG_LAP);
privatizedAvg.setEpsilon(0.1);
privatizedAvg.setBoundaryEstimation(BoundaryEstimator.MINMAX);
privatizedAvg.setId("AvgDuration");

//PPI Evaluation
MeasureEvaluator evaluator = new PrivacyAwareLogMeasureEvaluator(log);
evaluator.eval(privatizedAvg, new SimpleTimeFilter(Period.MONTHLY,1, false));
```
#### Definition
In PaPPI Aggregation Measures are defined using the **_PrivacyAwareAggregatedMeasure_** class. PaPPI adds the mandatory fields _Epsilon_ and _BoundaryEstimation_, and extends the 4 standard aggregation functions with combinations of functions and differentially private release mechanisms. While you can use any of the release mechanisms, we recommend to use the following mechanisms:
* Avg and Sum - Laplace Mechanism (AVG_LAP, SUM_LAP)
* Min and Max - Interval Mechanism (MIN_EXP, MAX_EXP)

#### Evaluation 
For the evaluation of the privacy-aware PPI definitions, use the **_PrivacyAwareLogMeasureEvaluator_** class. 

Note that before evaluating the PPI, PaPPI checks, whether the provided PPI definition is admissible, i.e. if it properly protects all the of trace information accessed by the PPI. In the case, that the provided PPI is not admissible, the evaluation of all PPIs will abort.

By default, PaPPI writes the results of any successfully evaluated PPI to a csv-file located under *src/results/*. For each evaluated PPI such a file is created, named after the user-set ID of the top-level measure definition of the PPI.


## Extending the Framework
If you wish to add your own release mechanisms, you can easily do this by adding it to the [PrivacyAwareComputerFactory](https://github.com/MartinKabierski/privacy-aware-ppinot/blob/master/src/src/main/java/pappi/computers/PrivacyAwareMeasureComputerFactory.java) class, that is responsible for instantiating the appropriate computer for any given measure definition. 

Please, be aware that you are responsible for guaranteeing, that the implemented mechanism actually provides the desired privacy protection.
## Supplementary Data
Additionally, we providethe result files, figures and scripts used for the creation of those figures in the publications. You can find these located in the [Evaluation](https://github.com/MartinKabierski/privacy-aware-ppinot/tree/master/evaluation) directory.

## Citing this work
If you use this repository or the paper in an academic article, please cite it as:
```
@inproceedings{kabierski2021privacy,
  title={Privacy-aware process performance indicators: Framework and release mechanisms},
  author={Kabierski, Martin and Fahrenkrog-Petersen, Stephan A and Weidlich, Matthias},
  booktitle={International Conference on Advanced Information Systems Engineering},
  pages={19--36},
  year={2021},
  organization={Springer}
}
```

## Contact
For any questions, please contact martin.kabierski@hu-berlin.de
