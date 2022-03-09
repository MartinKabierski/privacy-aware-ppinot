package apps;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.math3.distribution.LaplaceDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.ParetoDistribution;
import org.apache.commons.math3.distribution.PoissonDistribution;

public class PlotParametricDistribution {
	
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
		System.out.print("gaussian=[");
		for(int x=0;x<200;x++) {
		System.out.print(normal.density(x)+",");
		}
		System.out.print("]");
		System.out.println();

		ParetoDistribution pareto = new ParetoDistribution(10,2);
		System.out.print("pareto=[");
		for(double x=10;x<60;x=x+1) {
		System.out.print(pareto.density(x)+",");
		}
		System.out.print("]");
		System.out.println();
		
		PoissonDistribution poisson = new PoissonDistribution(5);
		System.out.print("poisson=[");
		for(int x=0;x<20;x++) {
		System.out.print(poisson.probability(x)+",");
		}
		System.out.print("]");
		System.out.println();

;
		
		
	}
}
