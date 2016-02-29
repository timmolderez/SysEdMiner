package arvid.thesis.examples.convertElementToCollection;

import java.util.ArrayList;
import java.util.List;

public class Garage2 {
	
	private List<Car> cars;
	
	public Garage2(List<Car> cars) {
		this.cars = new ArrayList<Car>();
	}
	
	public void addCar(Car car) {
		this.cars.add(car);
	}
	
	public void start() {
		// Start each car in this garage
		for(Car car: cars) {
			car.start();
		}
    }

    public void stop() {
		// Stop each car in this garage
		for(Car car: cars) {
			car.stop();
		}
    }
    
}