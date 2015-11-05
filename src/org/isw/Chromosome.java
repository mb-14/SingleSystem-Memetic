package org.isw;

import java.util.Random;

public class Chromosome implements Comparable<Chromosome>{
	static Random rand = new Random();
	double pmAvgTime;
	double fitnessValue;
	//Binary representation of the chromosome
	long[] combo;
	public Chromosome(long[] combo){
		this.combo = combo;
		this.fitnessValue = 0;
	}

	public long[] getCombolist(int j) {
		long combos[] = new long[MemeticAlgorithm.pmOs.get(j).size()];
		for(int i =0;i<MemeticAlgorithm.pmOs.get(j).size();i++){
			combos[i] = (combo[j]>>(MemeticAlgorithm.machines.get(j).compList.length*i))&((int)Math.pow(2,MemeticAlgorithm.machines.get(j).compList.length)-1);
		}
		return combos;
	}

	public void applyLocalSearch() {
		for(int j = 0;j < combo.length; j++)
			applyLocalSearch(j);
		
	}



	private void applyLocalSearch(int j) {
		long new_combo = combo[j];
		int cnt = 0;
		while(cnt++<5){
			int limit = MemeticAlgorithm.machines.get(j).compList.length*MemeticAlgorithm.pmOs.get(j).size();
			int mutationPoint = rand.nextInt(limit);
				new_combo = combo[j]^1<<mutationPoint;

			mutationPoint = rand.nextInt(limit);
				new_combo = combo[j]^1<<mutationPoint;
			
			if(heuristic(new_combo , j)>heuristic(combo[j], j)){
				combo[j] = new_combo;
			}
		}		
		
	}

	private long heuristic(long combo, int j) {
		//j = machineIndex
		Component[] temp = new Component[MemeticAlgorithm.machines.get(j).compList.length];
		for(int i=0; i < MemeticAlgorithm.machines.get(j).compList.length; i++)
			temp[i] = new Component(MemeticAlgorithm.machines.get(j).compList[i]);

		int heuristicP=0;
		int heuristicN=0;
		Double fp;
		long comboList[] = getCombolist(combo,j);
		for(int i=0;i<comboList.length;i++){
			for(int k=0;k<temp.length;k++){
				int pos = 1<<k;
				if((comboList[i]&pos)!=0){
					fp = getFailureProbablity(temp[k], MemeticAlgorithm.pmOs.get(j).get(i),j);
					heuristicP += (fp>0.5d)?1:-1;
					temp[k].initAge = (1-temp[k].pmRF)*temp[k].initAge;
				}
				else{
					fp = getFailureProbablity(temp[k], MemeticAlgorithm.pmOs.get(j).get(i),j);
					heuristicN += (fp<0.5d)?1:-1;
				}
			}
		}
		return heuristicN+heuristicP;
	}
	
	
	 

	private Double getFailureProbablity(Component component, int pmO, int j) {
		//j = machineIndex
		int failureCount =0;
		long time = 0;
		time = Math.min(Macros.SHIFT_DURATION, MemeticAlgorithm.schedule.get(j).getSum());
		time -= pmO;
		for(int i=0;i<50;i++){
			Double cmTTF = component.getCMTTF();
			if(cmTTF < time){
				failureCount++;
			}
		}
		return failureCount/50d;
	}

	public static long[] getCombolist(long combo, int j) {
		//J is machineIndex
		long combos[] = new long[MemeticAlgorithm.pmOs.get(j).size()];
		for(int i =0;i< MemeticAlgorithm.pmOs.get(j).size();i++){
			combos[i] = (combo>>(MemeticAlgorithm.machines.get(j).compList.length*i))&((int)Math.pow(2,MemeticAlgorithm.machines.get(j).compList.length)-1);
		}
		return combos;
	}

	@Override
	public int compareTo(Chromosome o) {
		return Double.compare(fitnessValue, o.fitnessValue);
	}
}