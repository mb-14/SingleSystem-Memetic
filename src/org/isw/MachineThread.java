package org.isw;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MachineThread implements Callable<ArrayList<SimulationResult>> {
	Schedule schedule;
	Machine machine;
	
	public MachineThread(Schedule schedule, int machineNo){
		this.schedule = schedule;
		this.machine = Main.machines.get(machineNo);
	}
	@Override
	public ArrayList<SimulationResult> call() throws Exception {
		System.out.println("Machine "+ (machine.machineNo+1)+ ": "+ schedule.printSchedule());
		ArrayList<Integer> pmoList = schedule.getPMOpportunities();
		ArrayList<SimulationResult> results = new ArrayList<>();
		if(pmoList.isEmpty()){
		return results;
		}

		ExecutorService threadPool = Executors.newFixedThreadPool(20);
		CompletionService<SimulationResult> pool = new ExecutorCompletionService<SimulationResult>(threadPool);
		int cnt=0;
		for(Integer i : pmoList){
			for(int j = 1;j<Math.pow(2,machine.compList.length);j++){
				pool.submit(new SimulationThread(schedule,j,i,true,machine));
				cnt++;
			}
		}
		for(int i=0;i<cnt;i++)
				results.add(pool.take().get());
			
		threadPool.shutdown();
		while(!threadPool.isTerminated());
		return results;
	}

}
