public class Stack2 {
	
	private int[] stackArray;
	private int top;
	public Stack2() {
		stackArray = new int[100];
		top = 0;
	}
	public Stack2 clear() {
		top = 0;
		return this;
	}
	public Stack2 safePush(int value) throws Exception {
		if(top == 100) throw new Exception();
		stackArray[top++] = value;
		return this;
	}
	public Stack2 poke(int value) {
		stackArray[top] = value;
		return this;
	}
	public Stack2 safeZap() throws Exception {
		if(top == 0) throw new Exception();
		--top;
		return this;
	}
	public int safePop() throws Exception {
		if(top == 0) throw new Exception();
		return stackArray[--top];
	}
	
}
