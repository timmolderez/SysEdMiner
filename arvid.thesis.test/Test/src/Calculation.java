
public class Calculation {

	private boolean initialized = false;
	
	public boolean initialize() {
		initialized = true;
		return initialized;
	}
	
	public CalculationResult estimate() {
		return new CalculationResult();
	}

	public CalculationResult perform() {
		return new CalculationResult();
	}
	
	public Boolean isInitialized() {
		return initialized;
	}

}
