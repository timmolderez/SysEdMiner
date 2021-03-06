package arvid.thesis.examples.addLoopCollector;

import java.util.HashSet;
import java.util.Set;

public class Manager1 {

	public void CalculateCalculations(Set<Calculation> calculations) {
		Set<CalculationResult> results = new HashSet<CalculationResult>();
		for (Calculation c : calculations){
			results.add(c.calculate());
		}
	}

	public void EstimateCalculations(Set<Calculation> calculations) {
		Set<CalculationResult> results = new HashSet<CalculationResult>();
		for (Calculation c : calculations){
			results.add(c.estimate());
		}
	}

	public void InitializeTasks(Set<Calculation> calculations) {
		for (Calculation c : calculations) {
			c.initialize();
		}
	}
	
}
