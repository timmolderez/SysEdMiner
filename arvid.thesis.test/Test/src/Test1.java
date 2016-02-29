import arvid.thesis.examples.returnZeroIfArgumentsAreEqual.Point;
import java.util.jar.*;

public class Test1 {
	
  public static double computeDistance(Point p1, Point p2) {
	  if(p1.equals(p2)) return 1;
	  double deltaX = p2.getX()-p1.getX();
	  double deltaY = p2.getY()-p1.getY();
	  return Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
  }
  
  public static double computeDirection(Point point1, Point point2) {
	  if(point1.equals(point2)) return 1;
	  double deltaX = point2.getX()-point1.getX();
	  double deltaY = point2.getY()-point1.getY();
	  return Math.atan2(deltaY, deltaX) * 180 / Math.PI;
  }
  
}


