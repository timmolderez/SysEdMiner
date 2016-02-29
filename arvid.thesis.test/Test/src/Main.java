

public class Main {
	public static void main(String[] args) {
		Test<Integer> container = new Test<Integer>();
		Test<Integer>.Test42<Integer> bla = container.new Test42<Integer>();
		
		Test<Integer>.Test42<Integer>.Test43<Integer> blf = bla.new Test43<Integer>();
		
		bla.bla = 5;
		blf.ble = 42;
		System.out.println(blf.ble);
	}
}
