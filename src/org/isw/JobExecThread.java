package org.isw;

import java.io.IOException;

import org.isw.Component;
import org.isw.Job;
import org.isw.Machine;
import org.isw.Macros;
import org.isw.Schedule;

public class JobExecThread extends Thread{
	Schedule jobList;
	Machine machine;
	public JobExecThread(Schedule jobList, Machine machine){
		this.jobList = jobList;
		this.machine = machine;
	}
	public void run(){
	int sum=0;

	while(!jobList.isEmpty() && sum < Macros.SHIFT_DURATION*Macros.TIME_SCALE_FACTOR){
		
		Job current = jobList.peek(); 
		try{
		jobList.decrement(1);
		}
		catch(IOException e){
			e.printStackTrace();
			System.exit(0);
		}
		switch(current.getJobType()){
			case Job.JOB_NORMAL:
				machine.procCost += current.getJobCost()/Macros.TIME_SCALE_FACTOR;
				for(Component comp : machine.compList)
					comp.initAge++;
				break;
			case Job.JOB_PM:
				machine.pmCost += current.getFixedCost() + current.getJobCost()/Macros.TIME_SCALE_FACTOR;
				machine.pmDownTime++;
				machine.downTime++;
				
				break;
			case Job.JOB_CM:
				machine.cmCost += current.getFixedCost() + current.getJobCost()/Macros.TIME_SCALE_FACTOR;
				current.setFixedCost(0);
				machine.downTime++;
				machine.cmDownTime++;
				break;
			case Job.WAIT_FOR_MT:
				machine.downTime++;
				machine.waitTime++;
		}
		if(current.getJobTime()<=0){
			//Job ends here
			switch(current.getJobType()){
			case Job.JOB_PM:
				for(int i =0; i<machine.compList.length;i++){
					int pos=1<<i;
					int bitmask = current.getCompCombo();
					if((pos&bitmask) != 0){
						Component comp = machine.compList[i];
						comp.initAge = (1-comp.pmRF)*comp.initAge;
						machine.compPMJobsDone[i]++;
					}
				}
				machine.pmJobsDone++;
				break;
			case Job.JOB_CM:	
				Component comp = machine.compList[current.getCompNo()];
				comp.initAge = (1 - comp.cmRF)*comp.initAge;
				machine.cmJobsDone++;
				machine.compCMJobsDone[current.getCompNo()]++;
				break;
			case Job.JOB_NORMAL:
				machine.jobsDone++;
				break;
			}
			try{
			System.out.println("Machine"+ (machine.machineNo+1)+ ": Job "+ jobList.remove().getJobName()+" complete");
			}
			catch(IOException e){
				e.printStackTrace();
				System.exit(0);
			}
			}
		sum++;
		machine.runTime++;
		
	}
	if(jobList.isEmpty()){
		machine.idleTime += Macros.SHIFT_DURATION*Macros.TIME_SCALE_FACTOR - sum;
		return;
		}
	int i = jobList.indexOf(jobList.peek());
	while(i < jobList.getSize()){
		machine.penaltyCost += jobList.jobAt(i++).getPenaltyCost();
	}
	
	}
	

}