# privacy-aware-ppinot
This repository contains the Java-based source code and the evaluation files for the definition and evaluation of privacy-aware process performance indicators, as presented in the paper "[Privacy-aware Process Performance Indicators: Framework and Release Mechanisms](https://arxiv.org/abs/2103.11740)".
This package builds upon the PPI definition- and evaluation-approaches defined by the "Process Performance Indicator Notation" (PPINOT), publicly available [via Maven](https://mvnrepository.com/artifact/es.us.isa.ppinot/ppinot-model).

The project extends the PPINOT definitions by introducing new aggregation measures and derived measures that guarantee differential privacy.


## Installation
Clone the repository and resolve the depencies defined in pom.xml.
After all these dependencies have been resolved you are ready to go.

## Usage
The general methodology for defining and evaluating PPIs remains the same as for the original PPINOT packages, so please have a look at those first.
Furthermore, the file "CalcPPI.java", that contains the definition and evaluation procedures for multiple privacy-aware PPIs on the basis of the publicly available "Sepsis Cases"-Log file, should give guidance on how to use add privacy-protection to the definition of PPIs.

## Evaluation Results
The result files, figures and scripts used for the creation of those figures, as used throughout the paper, are in the directory "evaluation".

## Contact
martin.kabierski@hu-berlin.de

### License
We provide our code, under the MIT license.
