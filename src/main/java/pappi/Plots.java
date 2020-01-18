package pappi;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.math3.distribution.LaplaceDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;

public class Plots {
	
	public static void main(String[] args) {
		
		NormalDistribution normal = new NormalDistribution(100, 25);
		System.out.print("normal1=[");
		for(int x=0;x<200;x++) {
		System.out.print(normal.density(x)+",");
		}
		System.out.print("]");
		System.out.println();
		
		normal = new NormalDistribution(100, 27);
		System.out.print("normal2=[");
		for(int x=0;x<200;x++) {
		System.out.print(normal.density(x)+",");
		}
		System.out.print("]");
		System.out.println();
		
		LaplaceDistribution laplace = new LaplaceDistribution(0,1);
		System.out.print("laplace1=[");
		for(double x=-10;x<10;x=x+0.2) {
		System.out.print(laplace.density(x)+",");
		}
		System.out.print("]");
		System.out.println();
		laplace = new LaplaceDistribution(0,5);
		System.out.print("laplace2=[");
		for(double x=-10;x<10;x=x+0.2) {
		System.out.print(laplace.density(x)+",");
		}
		System.out.print("]");
		System.out.println();
		laplace = new LaplaceDistribution(5,1);
		System.out.print("laplace3=[");
		for(double x=-10;x<10;x=x+0.2) {
		System.out.print(laplace.density(x)+",");
		}
		System.out.print("]");
		System.out.println();
		
		normal = new NormalDistribution(100, 25);
		Collection<Double> hundred = new ArrayList<Double>();
		for (int idx = 1; idx <= 200; ++idx){
			double value = normal.sample();
			hundred.add(value);
		}
		
		
	}
}
