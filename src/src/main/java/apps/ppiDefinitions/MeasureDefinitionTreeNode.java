package apps.ppiDefinitions;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.scalified.tree.TraversalAction;
import com.scalified.tree.TreeNode;
import com.scalified.tree.multinode.ArrayMultiTreeNode;

import es.us.isa.ppinot.model.DataContentSelection;
import es.us.isa.ppinot.model.MeasureDefinition;
import es.us.isa.ppinot.model.aggregated.AggregatedMeasure;
import es.us.isa.ppinot.model.base.CountMeasure;
import es.us.isa.ppinot.model.base.DataMeasure;
import es.us.isa.ppinot.model.base.TimeMeasure;
import es.us.isa.ppinot.model.condition.Condition;
import es.us.isa.ppinot.model.condition.DataPropertyCondition;
import es.us.isa.ppinot.model.condition.TimeInstantCondition;
import es.us.isa.ppinot.model.derived.DerivedMeasure;
import pappi.measureDefinitions.OptimizablePrivacyAwareMeasure;


@SuppressWarnings({ "hiding", "serial" })
public class MeasureDefinitionTreeNode extends ArrayMultiTreeNode<MeasureDefinition> {
	
	private String cardinalForm = "";
	private List<String> childrenCardinalForms = new ArrayList<String>();

	
	public MeasureDefinitionTreeNode(MeasureDefinition data) {
		super(data);
		this.cardinalForm = initializeCardinalForm(this.data());
	}
	
	
	public MeasureDefinitionTreeNode(MeasureDefinition data, int branchingFactor) {
		super(data, branchingFactor);
		this.cardinalForm = initializeCardinalForm(this.data());
	}
	
	
	public String getCardinalForm() {
		return this.cardinalForm;
	}
	
	
	public boolean add(MeasureDefinitionTreeNode subtree) {
		this.childrenCardinalForms.add(subtree.getCardinalForm());
		Collections.sort(this.childrenCardinalForms);
		updateCardinalForm();
		return super.add(subtree);
	}
	
	
	private String initializeCardinalForm(MeasureDefinition measure) {
		this.cardinalForm = "";
		if (measure.getClass() == es.us.isa.ppinot.model.aggregated.AggregatedMeasure.class || 
				measure.getClass() == pappi.measureDefinitions.PrivacyAwareAggregatedMeasure.class) {
			return this.cardinalForm  += AggregatedCardinalForm((AggregatedMeasure) measure);
		
		} else if (measure.getClass() == es.us.isa.ppinot.model.derived.DerivedMeasure.class || 
				measure.getClass() == es.us.isa.ppinot.model.derived.DerivedSingleInstanceMeasure.class || 
				measure.getClass() == es.us.isa.ppinot.model.derived.DerivedMultiInstanceMeasure.class) {
			return this.cardinalForm  += DerivedCardinalForm((DerivedMeasure) measure);
		
		} else if (measure.getClass() == es.us.isa.ppinot.model.base.CountMeasure.class) {
			return this.cardinalForm  += CountCardinalForm((CountMeasure) measure);
		
		} else if (measure.getClass() == es.us.isa.ppinot.model.base.DataMeasure.class) {
			return this.cardinalForm  += DataCardinalForm((DataMeasure) measure);
		
		} else if (measure.getClass() == es.us.isa.ppinot.model.base.TimeMeasure.class) {
			return this.cardinalForm  += TimeCardinalForm((TimeMeasure) measure);
		
		} else if (measure.getClass() == pappi.measureDefinitions.OptimizablePrivacyAwareMeasure.class) {
			return this.cardinalForm  += OptimizableCardinalForm((OptimizablePrivacyAwareMeasure) measure);
		}
		
		return "SPECIAL CASE NOT IMPLEMENTED YET";
	}
	
	
	private void updateCardinalForm() {
		this.cardinalForm = initializeCardinalForm(this.data()) + "("; 
		for(int i = 0; i < childrenCardinalForms.size(); i++) {
			if (i == childrenCardinalForms.size()-1) {
				this.cardinalForm += childrenCardinalForms.get(i) + ")";
			} else {
				this.cardinalForm += childrenCardinalForms.get(i) + ", ";
			}
			
		}
	}
	
	
	private String AggregatedCardinalForm(AggregatedMeasure measure) {
		return "Aggregated_" + measure.getAggregationFunction();
	}
	
	
	private String DerivedCardinalForm(DerivedMeasure measure) {
		return "Derived_" + measure.getFunction();
	}
	
	
	private String CountCardinalForm(CountMeasure measure) {
		String when = measure.getWhen().getAppliesTo();
		return "Count(" + when + ")";
	}
	
	
	private String DataCardinalForm(DataMeasure measure) {
		String when = measure.getDataContentSelection().getSelection();
		String precondition = getPrecondition(measure.getPrecondition());
		return "Data(" + when + ", " + precondition + ")";
	}
	
	
	private String TimeCardinalForm(TimeMeasure measure) {
		String from = measure.getFrom().getAppliesTo();
		String to = measure.getTo().getAppliesTo();
		String precondition = getPrecondition(measure.getPrecondition());
		return "Time(" + from + ", " + to + ", " + precondition +")";
	}
	
	
	private String OptimizableCardinalForm(OptimizablePrivacyAwareMeasure measure) {
		return "Optimizable";
		
	}
	
	
	private String getPrecondition(Condition precondition) {
		if (precondition == null || precondition.getAppliesTo() == null) {
			return "None";
		} else {
			return precondition.getAppliesTo();
		}
	}
	
	
	/**
	 * Indicates whether some object equals to this one, by the data stored in all of the trees leaves
	 *
	 * @param obj the reference object with which to compare
	 * @return {@code true} if this object is the same as the obj
	 *         argument; {@code false} otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null
				|| getClass() != obj.getClass()) {
			return false;
		}
		MeasureDefinition thatMeasure = ((MeasureDefinitionTreeNode) obj).data();
		
		// choose the appropriate comparison method
		return this.callComparisonMethod(thatMeasure);
	}
	
		
	/**
	 * Calls the comparison method for the given MeasureDefintion, as for each type they are unique.
	 * 
	 * @param that the reference object with which to compare
	 * @return {@code true} if this object is the same as that
	 *         argument; {@code false} otherwise
	 */
	private boolean callComparisonMethod(MeasureDefinition that){
		
		if (that.getClass() == es.us.isa.ppinot.model.aggregated.AggregatedMeasure.class || 
				that.getClass() == pappi.measureDefinitions.PrivacyAwareAggregatedMeasure.class) {
			return this.compareAggregationMeasures( (AggregatedMeasure) that );
		
		} else if (that.getClass() == es.us.isa.ppinot.model.derived.DerivedMeasure.class) {
			return this.compareDerivedMeasures( (DerivedMeasure) that);
		
		} else if (that.getClass() == es.us.isa.ppinot.model.base.CountMeasure.class) {
			return this.compareCountMeasures( (CountMeasure) that);
		
		} else if (that.getClass() == es.us.isa.ppinot.model.base.DataMeasure.class) {
			return this.compareDataMeasures( (DataMeasure) that);
		
		} else if (that.getClass() == es.us.isa.ppinot.model.base.TimeMeasure.class) {
			return this.compareTimeMeasures( (TimeMeasure) that);
		}
		return false;
	}
	
	
	/**
	 * @param that the reference object, a node holding a AggregationMeasure, with which to compare
	 * @return {@code true} if this object has the same aggregationFunction as that
	 *         argument; {@code false} otherwise
	 */
	private boolean compareAggregationMeasures(AggregatedMeasure thatMeasure) {
		AggregatedMeasure thisMeasure =  (AggregatedMeasure) this.data();
		if ( thisMeasure.getAggregationFunction() == thatMeasure.getAggregationFunction() ) {
			return true;
		}
		return false;
	}
	
	
	/**
	 * @param that the reference object, a node holding a DerivedMeasure, with which to compare
	 * @return {@code true} if this object has the same function as that
	 *         argument; {@code false} otherwise
	 */
	private boolean compareDerivedMeasures(DerivedMeasure thatMeasure) {
		DerivedMeasure thisMeasure = (DerivedMeasure) this.data();
		if ( thisMeasure.getFunction() == thatMeasure.getFunction() ) {
			return true;
		}
		return false;
	}
	
	
	/**
	 * @param that the reference object, a node holding a CountMeasure, with which to compare
	 * @return {@code true} if this object has the same counting scheme as that
	 *         argument; {@code false} otherwise
	 */
	private boolean compareCountMeasures(CountMeasure thatMeasure) {
		CountMeasure thisMeasure = (CountMeasure) this.data();
		
		// extract the necessary attributes from this and that for equality check
		TimeInstantCondition thisWhen = thisMeasure.getWhen();
		TimeInstantCondition thatWhen = thatMeasure.getWhen();
		
		return this.compareTimeInstantCondition(thisWhen, thatWhen);
	}
	
	
	/**
	 * @param that the reference object, a node holding a DataMeasure, with which to compare
	 * @return {@code true} if this object has the same DataContentSelection as that
	 *         argument; {@code false} otherwise
	 */
	private boolean compareDataMeasures(DataMeasure thatMeasure) {
		DataMeasure thisMeasure = (DataMeasure) this.data();
		
		// extract the necessary attributes from this and that for equality check
		DataContentSelection thisWhen = thisMeasure.getDataContentSelection();
		DataContentSelection thatWhen = thatMeasure.getDataContentSelection();
		
		Condition thisCond = thisMeasure.getPrecondition();
		Condition thatCond = thatMeasure.getPrecondition();
		
		
		// check all cases off condition being (non) null and then compare the values of the DataMeasureS
		if ( (thisCond == null && thatCond != null) || (thisCond != null && thatCond == null) ) {
			return false;
		} else if (thisCond == null && thatCond == null) {
			return (thisWhen.getSelection() == thatWhen.getSelection());
		} else {
			return ((thisWhen.getSelection() == thatWhen.getSelection()) && 
				    (thisCond.getAppliesTo() == thatCond.getAppliesTo()) );
		}
	}
	
	
	/**
	 * @param that the reference object, a node holding a TimeMeasure, with which to compare
	 * @return {@code true} if this object has the same time interval as that
	 *         argument; {@code false} otherwise
	 */
	private boolean compareTimeMeasures(TimeMeasure thatMeasure) {
		TimeMeasure thisMeasure = (TimeMeasure) this.data();
		
		// extract the necessary attributes from this and that for equality check
		TimeInstantCondition thisFrom = thisMeasure.getFrom();
		TimeInstantCondition thisTo = thisMeasure.getTo();
		TimeInstantCondition thatFrom = thatMeasure.getFrom();
		TimeInstantCondition thatTo = thatMeasure.getTo();
		
		DataPropertyCondition thisCond = thisMeasure.getPrecondition();
		DataPropertyCondition thatCond = thatMeasure.getPrecondition();
		
		// check all cases off condition being (non) null and then compare the values of the DataMeasureS
		if ( (thisCond == null && thatCond != null) || (thisCond != null && thatCond == null) ) {
			return false;
		} else if (thisCond == null && thatCond == null) {
			return (this.compareTimeInstantCondition(thisFrom, thatFrom) && this.compareTimeInstantCondition(thisTo, thatTo));
		} else {
			return (((this.compareTimeInstantCondition(thisFrom, thatFrom) && this.compareTimeInstantCondition(thisTo, thatTo))) && 
				    (thisCond.getAppliesTo() == thatCond.getAppliesTo()));
		}
	}
	
	
	/**
	 * @param thisTimeCond the TimeInstantCondition of this obj 
	 * @param thatTimeCond the TimeInstantCondition of the obj to compare to
	 * @return {@code true} if this object has the same appliesTo as that
	 *         argument; {@code false} otherwise
	 */
	private boolean compareTimeInstantCondition(TimeInstantCondition thisTimeCond, TimeInstantCondition thatTimeCond) {
		if (thisTimeCond.getAppliesTo() == thatTimeCond.getAppliesTo()) {
			return true;
		}
		return false;
	}
	
	
	/**
	 * Returns the string representation of this object
	 *
	 * @return string representation of this object
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("\n");
		final int topNodeLevel = level();
		TraversalAction<TreeNode<MeasureDefinition>> action = new TraversalAction<TreeNode<MeasureDefinition>>() {
			@Override
			public void perform(TreeNode<MeasureDefinition> node) {
				int nodeLevel = node.level() - topNodeLevel;
				for (int i = 0; i < nodeLevel; i++) {
					builder.append("|  ");
				}
				builder
						.append("+- ")
						.append(((MeasureDefinition) node.data()).getClass().getSimpleName())
						.append("\n");
			}

			@Override
			public boolean isCompleted() {
				return false;
			}
		};
		traversePreOrder(action);
		return builder.toString();
	}
}
