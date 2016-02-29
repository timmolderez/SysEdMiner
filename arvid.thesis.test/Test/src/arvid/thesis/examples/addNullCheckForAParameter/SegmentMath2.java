package arvid.thesis.examples.addNullCheckForAParameter;

public class SegmentMath2 {

	public double computeDistance(Segment s) {
		if(s == null) return 0;
		double deltaX = s.computeDeltaX();
		double deltaY = s.computeDeltaY();
		return Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
	}
	
	public double computeDirection(Segment s) {
		if(s == null) return 0;
		double deltaX = s.computeDeltaX();
		double deltaY = s.computeDeltaY();
		return Math.atan2(deltaY, deltaX) * 180 / Math.PI;
	}

	public Segment translate(int x, int y, Segment s) {
		Point newPoint1 = new Point(s.getP1().getX()+x, s.getP1().getY()+y);
		Point newPoint2 = new Point(s.getP2().getX()+x, s.getP2().getY()+y);
		return new Segment(newPoint1, newPoint2);
	}

	public void translateDestructive(Segment s, int x, int y) {
		if(s == null) return;
		s.getP1().setX(s.getP1().getX()+x);
		s.getP1().setY(s.getP1().getY()+y);
		s.getP2().setX(s.getP2().getX()+x);
		s.getP2().setY(s.getP2().getY()+y);
    }

	public void resetDestructive(Segment s) {
		if(s == null) return;
		s.getP1().setX(0);
		s.getP1().setY(0);
		s.getP2().setX(0);
		s.getP2().setY(0);
    }
	
}
