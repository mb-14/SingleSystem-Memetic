package org.isw;

import java.io.Serializable;

public class SimulationResult implements Comparable<SimulationResult>,Serializable {
  /**
	 * 
	 */
private static final long serialVersionUID = 1L;
  public long pmAvgTime;
  public int compCombo;
  public int pmOpportunity;
  public double cost;
  public long t; //to be used for calculations by maintenance dept
  public int id; //to be used for calculations by maintenance dept
  public SimulationResult(double cost,long pmAvgTime,int compCombo,int pmOpportunity,long t){
	  this.cost = cost;
	  this.pmAvgTime = pmAvgTime;
	  this.compCombo = compCombo;
	  this.pmOpportunity = pmOpportunity;
	  this.t = t;			 
  }

@Override
public int compareTo(SimulationResult o) {
	return Long.compare(t, o.t);
}


}
