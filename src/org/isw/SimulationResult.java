package org.isw;

import java.io.Serializable;
import java.util.ArrayList;

public class SimulationResult implements Serializable,Comparable<SimulationResult> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public double cost; //Intensity Factor
	public double pmAvgTime;
	public long[] compCombo;
	public ArrayList<Integer> pmOpportunity;
	public boolean noPM; // When there is no PM job to do

	public long startTimes[]; //to be set and used for calculations by maintenance dept during planning
	public int id; //to be set and used for calculations by maintenance dept during planning
	public long pmTTRs[][];

	public long chromosomeID;
	
	public SimulationResult(double cost, double pmAvgTime, long[] compCombo,ArrayList<Integer> pmOpportunity, boolean noPM,int id){
		this.cost = cost;
		this.pmAvgTime = pmAvgTime;
		this.compCombo = compCombo;
		this.pmOpportunity = pmOpportunity;
		this.noPM = noPM;
		this.id = id;
		if(!noPM)
			this.startTimes = new long[pmOpportunity.size()];
	}
	public double getCost(){
		return cost;
	}
	public double getPMAvgTime(){
		return pmAvgTime;
	}
	public long[] getCompCombo(){
		return compCombo;
	}
	public ArrayList<Integer> getPMOpportunity(){
		return pmOpportunity;
	}
	public void setCost(double cost) {
		this.cost = cost;

	}

	public int getPMLabourCount(Component[] compList)
	{
		int count = 0;
		for(int pmOpp = 0; pmOpp<pmOpportunity.size(); pmOpp++)
		{
			for(int i=0;i< compList.length;i++)
			{
				int pos = 1<<i;
				if((pos&compCombo[pmOpp])!=0) //for each component in combo
				{
					int[] labour = compList[i].getPMLabour();
					count += labour[0] + labour[1] + labour[2];
				}
			}
		}
		return count;
	}
	public long getChormosome(int length) {
		long combo = compCombo[compCombo.length-1];
		for(int i = compCombo.length-2; i >=0  ;i--){
			combo = (combo<<length);
			combo |= compCombo[i];
		}
		return combo;
	}

	@Override
	public int compareTo(SimulationResult o) {
		return Double.compare(cost, o.cost);
	}


}
