package arvid.thesis.examples.wrapWithTimer;

public class HardWorker2 {

	public void _doHeavyStuff() {
		try {
			Thread.sleep(2);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void doHeavyCall() {
		long start, end;
		start = System.currentTimeMillis();
		this._doHeavyStuff();
		end = System.currentTimeMillis();
		System.out.println(end - start);
	}

	public void doHeavyForLoop1() {
		long start, end;
		start = System.currentTimeMillis();
		for(int i = 0; i < 500; i++) {
			this._doHeavyStuff();
		}
		end = System.currentTimeMillis();
		System.out.println(end - start);
	}

	public void doHeavyForLoop2() {
		long start, end;
		start = System.currentTimeMillis();
		for(int j = 0; j <= 99; j++) {
			this._doHeavyStuff();
		}
		end = System.currentTimeMillis();
		System.out.println(end - start);
	}
	
	public void doHeavyWhile1() {
		long start, end;
		int i = 0;
		start = System.currentTimeMillis();
		while(i < 500) {
			this._doHeavyStuff();
			i += 1;
		}
		end = System.currentTimeMillis();
		System.out.println(end - start);
	}

	public void doHeavyWhile2() {
		long start, end;
		int j = 0;
		start = System.currentTimeMillis();
		while(j <= 99) {
			this._doHeavyStuff();
			j++;
		}
		end = System.currentTimeMillis();
		System.out.println(end - start);
	}

}
