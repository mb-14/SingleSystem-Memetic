package org.isw.test;

import java.util.ArrayList;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.isw.Job;
import org.isw.Machine;
import org.isw.Macros;
import org.isw.Schedule;
import org.isw.SimulationResult;
import org.isw.SimulationThread;

public class SimulationTest {
	static ArrayList<Integer> pmoList;
	static Machine machine;
	public static void main(String[] args) {
		Macros.SHIFT_DURATION = 24*60;
		Schedule schedule = new Schedule();
		schedule.addJob(new Job("J1",10*24,200,Job.JOB_NORMAL));
		schedule.addJob(new Job("J2",10*24,200,Job.JOB_NORMAL));
		schedule.addJob(new Job("J3",10*24,200,Job.JOB_NORMAL));
		schedule.addJob(new Job("J4",10*24,200,Job.JOB_NORMAL));
		schedule.addJob(new Job("J5",10*24,200,Job.JOB_NORMAL));
		schedule.addJob(new Job("J6",10*24,200,Job.JOB_NORMAL));
		//machine = new Machine(0,1);
		pmoList = schedule.getPMOpportunities();
		ExecutorService threadPool = Executors.newSingleThreadExecutor();
		CompletionService<SimulationResult> pool = new ExecutorCompletionService<SimulationResult>(threadPool);
		pool.submit(new SimulationThread(schedule, getCombolist((long)(Math.pow(2, 31)-1)), pmoList,false,machine));
	
		try {
			SimulationResult result =  pool.take().get();
			System.out.println(result.cost);
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static long[] getCombolist(long combo) {
		long combos[] = new long[pmoList.size()];
		for(int i =0;i<pmoList.size();i++){
			combos[i] = (combo>>(machine.compList.length*i))&((int)Math.pow(2,machine.compList.length)-1);
		}
		return combos;
	}
}
