package arvid.thesis.examples.returnZeroIfArgumentsAreEqual;

public class Point {
	
	private int x;
	private int y;
	
	// Constructor
	
	public Point(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	// Getters and setters
	
	public int getX() {
		return x;
	}
	
	public void setX(int x) {
		this.x = x;
	}
	
	public int getY() {
		return y;
	}
	
	public void setY(int y) {
		this.y = y;
	}
	
	// Operations
	
	public double computeDistance(Point p) {
		if (this.equals(p)) return 0;
		double deltaX = this.computeDeltaX(p);
		double deltaY = this.computeDeltaY(p);
		return Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
	}
	
	public double computeDirection(Point otherPoint) {
		if (this.equals(otherPoint)) return 0;
		double deltaX = this.computeDeltaX(otherPoint);
		double deltaY = this.computeDeltaY(otherPoint);
		return Math.atan2(deltaY, deltaX) * 180 / Math.PI;
	}
	
	// Internal helpers
	
	private double computeDeltaX(Point p) {
		return p.getX()-this.getX();
	}
	
	private double computeDeltaY(Point p) {
		return p.getY()-this.getY();
	}
	
}
