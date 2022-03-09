package pappi.measureDefinitions;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import pappi.util.FileHandler;


//writers should be own class that extends map
public class OptimizablePrivacyAwareMeasure extends PrivacyAwareAggregatedMeasure{
	public boolean writeToFile;
	FileHandler fileHandler;
	Map<String, FileHandler> writers;
	
	public OptimizablePrivacyAwareMeasure() {
		writers = new HashMap();
		this.writeToFile = false;
	}
	
	public void addFileHandler(String name,FileHandler f) {
		this.writeToFile = true;
		this.writers.put(name, f);
	}
	
	public FileHandler getFileHandler(String name) {
		return this.writers.get(name);
	}
	
	public Set<String> getHandlerKeys(){
		return this.writers.keySet();
	}
}
