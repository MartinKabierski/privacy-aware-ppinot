package pappi;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import es.us.isa.ppinot.model.MeasureDefinition;
import es.us.isa.ppinot.model.derived.DerivedMultiInstanceMeasure;

public class test extends DerivedMultiInstanceMeasure{
	
public boolean valid() {
		Boolean cond = super.valid();
		//System.out.println(cond);
		Iterator<Entry<String, MeasureDefinition>> itInst = this.getUsedMeasureMap().entrySet().iterator();
	    while (cond && itInst.hasNext()) {
	        Map.Entry<String, MeasureDefinition> pairs = itInst.next();
	        MeasureDefinition value = pairs.getValue();
	        cond = cond && value.valid();
	    }
	    
		return cond;
	}
}
