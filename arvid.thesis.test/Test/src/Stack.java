public class Stack {
	
	private int[] stackArray;
	private int top;
	public Stack() {
		stackArray = new int[100];
		top = 0;
	}
	public void clear() {
		top = 0;
	}
	public void push(int value) throws Exception {
		if(top == 100) throw new Exception();
		stackArray[top++] = value;
	}
	public void poke(int value) {
		stackArray[top] = value;
	}
	public void zap() throws Exception {
		if(top == 0) throw new Exception();
		--top;
	}
	public int pop() throws Exception {
		if(top == 0) throw new Exception();
		return stackArray[--top];
	}
	
}
