import java.util.ArrayList;
import java.util.List;


public class Employee {
	
    private String name;
    private Employee boss;
    private List<Employee> subordinates;
    
    public Employee(String name, Employee boss) {
    	this.name = name;
    	this.boss = boss;
    	this.subordinates = new ArrayList<Employee>();
    }
    
    public void addSubordinate(Employee subordinate) {
    	this.subordinates.add(subordinate);
    }
    
}
