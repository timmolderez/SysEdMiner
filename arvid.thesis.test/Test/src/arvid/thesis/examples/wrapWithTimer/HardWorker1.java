package arvid.thesis.examples.wrapWithTimer;

public class HardWorker1 {

	public void _doHeavyStuff() {
		try {
			Thread.sleep(2);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void doHeavyCall() {
		long start = System.currentTimeMillis();
		this._doHeavyStuff();
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}

	public void doHeavyForLoop1() {
		long start = System.currentTimeMillis();
		for(int i = 0; i < 500; i++) {
			this._doHeavyStuff();
		}
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}

	public void doHeavyForLoop2() {
		long start = System.currentTimeMillis();
		for(int j = 0; j <= 99; j++) {
			this._doHeavyStuff();
		}
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}
	
	public void doHeavyWhile1() {
		long start = System.currentTimeMillis();
		int i = 0;
		while(i < 500) {
			this._doHeavyStuff();
			i += 1;
		}
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}

	public void doHeavyWhile2() {
		long start = System.currentTimeMillis();
		int j = 0;
		while(j <= 99) {
			this._doHeavyStuff();
			j++;
		}
		long end = System.currentTimeMillis();
		System.out.println(end - start);
	}

}
