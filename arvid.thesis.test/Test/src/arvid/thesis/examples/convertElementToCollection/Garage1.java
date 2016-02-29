package arvid.thesis.examples.convertElementToCollection;

import java.util.List;

public class Garage1 {
	
	private List<Car> cars;
	
	public Garage1(List<Car> cars) {
		this.cars = cars;
	}
	
	public void start() {
		for(Car car: cars) {
			car.start();
		}
    }

    public void stop() {
		for(Car car: cars) {
			car.stop();
		}
    }
    
}
