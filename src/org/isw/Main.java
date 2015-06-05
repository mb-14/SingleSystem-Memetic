package org.isw;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class Main {
	public static ArrayList<Component[]> compList = new ArrayList<Component[]>();
	public static ArrayList<Schedule> mainSchedules = new ArrayList<Schedule>();
	static Random r = new Random(); 
	static ArrayList<Job> jobArray;
	final static long procTimeArr[]={5,5,5,4,4,4,4,4,2,2,1,1};
	final static int procCostArr[]={80,80,70,70,70,60,60,50,50,40,40,40};
	static Map<Long, List<SimulationResult>> result_map;
	static ArrayList<Machine> machines = new ArrayList<Machine>();
	static int noOfMachines =0;
	private static double minCost;
	private static PriorityQueue<CompTTF> ttfList;
	private static long timemax = 0;
	public static void main(String args[])
	{
		Macros.loadMacros();
		parseJobs();
		System.out.println("Enter number of days to simulate:");
		Scanner in = new Scanner(System.in);
		int shiftCount = in.nextInt()*24/Macros.SHIFT_DURATION;
		System.out.println("Enter number of machines:");
		noOfMachines = in.nextInt();
		for(int i=0;i<noOfMachines;i++){
			machines.add(new Machine(i));
			mainSchedules.add(new Schedule());
		}
		int shiftNo = 0;
		//Main loop
		while(shiftNo++ < shiftCount){
			try {
			PriorityQueue<Schedule> pq = new PriorityQueue<Schedule>();
			for(Schedule sched : mainSchedules)
				pq.add(sched);					
			
			System.out.println("Job list: ");
			for(int i=0;i<jobArray.size();i++){
				Schedule min = pq.remove();
				min.addJob(new Job(jobArray.get(i)));
				System.out.print(jobArray.get(i).getJobName()+": "+String.valueOf(jobArray.get(i).getJobTime()/Macros.TIME_SCALE_FACTOR)+" ");
				pq.add(min);
			}
			System.out.println("");
			ExecutorService threadPool = Executors.newFixedThreadPool(noOfMachines);
			CompletionService<ArrayList<SimulationResult>> pool = new ExecutorCompletionService<ArrayList<SimulationResult>>(threadPool);
			int cnt=0;
			minCost = Double.MAX_VALUE;
			ttfList = new PriorityQueue<CompTTF>();
			while(!pq.isEmpty()){
				cnt++;
				Schedule sched = pq.poll();
				pool.submit(new MachineThread(sched,mainSchedules.indexOf(sched)));
			}
			
			for(int i=0; i<noOfMachines;i++){
				for(int j=0;j<machines.get(i).compList.length;j++){
					long ttf = (long)(machines.get(i).compList[j].getCMTTF()*Macros.TIME_SCALE_FACTOR);				
					long ttr = (long)(machines.get(i).compList[j].getCMTTR()*Macros.TIME_SCALE_FACTOR);
				ttfList.add(new CompTTF(ttf,(ttr==0)?1:ttr,i,j));	
				}
			}
			
			result_map = new HashMap<Long, List<SimulationResult>>();
			for(int i=0;i<cnt;i++){
				ArrayList<SimulationResult> results = pool.take().get();
				for(SimulationResult result : results){
					List<SimulationResult> l = result_map.get(result.t);
					if(l == null){
						if(result.t > timemax )
							timemax = result.t;
						  result_map.put(result.t, l=new ArrayList<SimulationResult>());
				    	}
					l.add(result);
					}
				}			
			threadPool.shutdown();
			while(!threadPool.isTerminated());
			threadPool = Executors.newFixedThreadPool(noOfMachines);
			calcCombos(0,mainSchedules,0);
			for(int i=0;i<noOfMachines;i++){
				threadPool.execute(new JobExecThread(mainSchedules.get(i),machines.get(i)));
				}
			threadPool.shutdown();
			while(!threadPool.isTerminated());
			} 
			catch (InterruptedException | ExecutionException | IOException e) {
			
				e.printStackTrace();
			} 
		}
		for(Machine machine : machines)
			writeResults(machine);
		
	}

	private static void parseJobs() {
		jobArray = new ArrayList<Job>();
		try
		{
			FileInputStream file = new FileInputStream(new File("Jobs.xlsx"));
			XSSFWorkbook workbook = new XSSFWorkbook(file);
			XSSFSheet sheet = workbook.getSheetAt(0);
			
			for(int i=1;i<=12;i++)
			{
				Row row = sheet.getRow(i);
				int demand = (int) row.getCell(5).getNumericCellValue();
				String jobName = row.getCell(0).getStringCellValue();
				long jobTime = (long)(row.getCell(1).getNumericCellValue()*Macros.TIME_SCALE_FACTOR);
				double jobCost = row.getCell(3).getNumericCellValue();
				for(int j=0; j<demand ;j++){
					Job job = new Job(jobName,jobTime,jobCost,Job.JOB_NORMAL);
					job.setPenaltyCost(row.getCell(4).getNumericCellValue());
				}
			}
			file.close();
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}		
	}

	private static void writeResults(Machine machine) {

		System.out.println("=========================================");
		System.out.println("Machine "+ (machine.machineNo+1));
		System.out.println("Downtime:" + String.valueOf(machine.downTime*100/(machine.runTime)) +"%");
		System.out.println("CM Downtime: "+ machine.cmDownTime +" hours");
		System.out.println("PM Downtime: "+ machine.pmDownTime +" hours");
		System.out.println("Waiting Downtime: "+ machine.waitTime +" hours");
		System.out.println("Machine Idle time: "+ machine.idleTime+" hours");
		System.out.println("PM Cost: "+ machine.pmCost);
		System.out.println("CM Cost: "+ machine.cmCost);
		System.out.println("Penalty Cost: "+ machine.penaltyCost);
		System.out.println("Processing Cost: "+ machine.procCost);
		System.out.println("Number of jobs:" + machine.jobsDone);
		System.out.println("Number of CM jobs:" + machine.cmJobsDone);
		System.out.println("Number of PM jobs:" + machine.pmJobsDone);
		for(int i=0 ;i<machine.compList.length; i++)
			System.out.println("Component "+String.valueOf(i+1)+": PM "+machine.compPMJobsDone[i]+"|CM"+machine.compCMJobsDone[i]);
		
	}
	
	public static void calcCombos(long time,ArrayList<Schedule> sched, int bitmask) throws IOException, InterruptedException, ExecutionException{
		if(bitmask == ((1<<noOfMachines)-1)|| time> timemax){
			calcCost(sched);
			return;
		}
		List<SimulationResult> l = result_map.get(time);
		if(l==null){
			calcCombos(time+1,sched,bitmask);
		}
		else{
			int cnt=0;
			for(SimulationResult result : l){
				if((bitmask & 1<<result.id) == 0){
					cnt++;
					ArrayList<Schedule> temp = new ArrayList<Schedule>(sched);
					for(int i=0; i< temp.size();i++){
						Schedule schedule = temp.get(i);
						temp.set(i,new Schedule(schedule));
					}
					Job pmJob = new Job("PM",result.pmAvgTime,5000,Job.JOB_PM);
					pmJob.setCompCombo(result.compCombo);
					temp.get(result.id).addPMJob(pmJob, result.pmOpportunity);
					calcCombos(time + result.pmAvgTime,temp,bitmask|(1<<result.id));
				}
			}
			if(cnt==0)
				calcCombos(time+1,sched,bitmask);
			}
		
	}

	private static void calcCost(ArrayList<Schedule> temp) throws InterruptedException, ExecutionException {
		addCMJobs(temp);
		double cost =0;
		ExecutorService threadPool = Executors.newFixedThreadPool(noOfMachines);
		CompletionService<SimulationResult> pool = new ExecutorCompletionService<SimulationResult>(threadPool);
		for(Schedule schedule : temp){
			pool.submit(new SimulationThread(schedule,-1,-1,false,null));
		}
		for(int i = 0;i<temp.size();i++){
			cost += pool.take().get().cost;
		}
		if(cost < minCost){
			mainSchedules = temp;
			minCost = cost;
		}
		threadPool.shutdown();
		while(!threadPool.isTerminated());
		
	}
	private static void addCMJobs(ArrayList<Schedule> temp) {
		while(!ttfList.isEmpty()){
			CompTTF compTTF = ttfList.remove();
			if(compTTF.ttf>= Macros.SHIFT_DURATION*Macros.TIME_SCALE_FACTOR || compTTF.ttf >= temp.get(compTTF.machineID).getSum())
				continue;
			if(temp.get(compTTF.machineID).isEmpty())
				continue;
			//if PM is performed for a component before ttf of that component, ignore.
			ArrayList<Job> pmJobs =	temp.get(compTTF.machineID).getPMJobs();
			boolean flag = false;
			for(Job pmJob : pmJobs){
			int tempIndex = temp.get(compTTF.machineID).indexOf(pmJob);
			int compCombo = pmJob.getCompCombo();
			long time =	temp.get(compTTF.machineID).getFinishingTime(tempIndex-1);
			if(compTTF.ttf>= time && ((1<<compTTF.componentID)&compCombo)!=0)
				flag = true;
			}
			if(flag)
				continue;
			int index = temp.get(compTTF.machineID).jobIndexAt(compTTF.ttf);
			Job job = temp.get(compTTF.machineID).jobAt(index);
			/**If breakdown occurs on a machine while PM/CM is going on shift the ttf to occur after
			 * the PM/CM ends
			 * **/ 
			if(job.getJobType() == Job.JOB_PM || job.getJobType() == Job.JOB_CM || job.getJobType() ==  Job.WAIT_FOR_MT)
			{
				//TODO: Calculate new time
				long newTime = temp.get(compTTF.machineID).getFinishingTime(index);
				if(newTime >= temp.get(compTTF.machineID).getSum() || newTime >= Macros.SHIFT_DURATION*Macros.TIME_SCALE_FACTOR)
					continue;
				ttfList.add(new CompTTF(newTime,compTTF.ttr,compTTF.machineID,compTTF.componentID));
				continue;
			}
			
			boolean pmFlag = false;
			boolean cmFlag = false;
			int cmIndex = 0;
			int cmJobIndex = 0;
			int pmIndex =0;
			int pmJobIndex =0;
			//Loop through all machines and check for overlaps.
			for(int i = 0;i < temp.size();i++){
				if(compTTF.machineID == i || temp.get(i).getSum() <= compTTF.ttf || temp.get(i).isEmpty())
					continue;
				int index1 = temp.get(i).jobIndexAt(compTTF.ttf); 
				Job job1 = temp.get(i).jobAt(index1);
				switch(job1.getJobType()){
					case Job.JOB_CM:
						cmFlag = true;
						cmIndex = i;
						cmJobIndex = index1;
						break;
					case Job.JOB_PM:
						pmFlag = true;
						pmIndex = i;
						pmJobIndex = index1;
						break;
				}		
			
				if(cmFlag){
					/**If CM is being performed on a different machine wait for CM to complete i.e 
					 * shift breakdown time to occur after CM job.
					 * **/
					long newTime = temp.get(cmIndex).getFinishingTime(cmJobIndex);
					if(newTime>=Macros.SHIFT_DURATION*Macros.TIME_SCALE_FACTOR||newTime>=	temp.get(compTTF.machineID).getSum())
						continue;
					int jobIndex =	temp.get(compTTF.machineID).jobIndexAt(compTTF.ttf);
					temp.get(compTTF.machineID).addWaitJob(compTTF.ttf, newTime-compTTF.ttf, jobIndex);
					ttfList.add(new CompTTF(newTime,compTTF.ttr,compTTF.machineID,compTTF.componentID));
				}
				else if(pmFlag){
					/**If PM is being performed on a different machine, interrupt that PM and add a waiting job
					 * and then add the CM job for our machine.
					 **/
					temp.get(pmIndex).addWaitJob(compTTF.ttf, compTTF.ttr,pmJobIndex);
					Job cmJob = new Job("CM",compTTF.ttr,machines.get(compTTF.machineID).compList[compTTF.componentID].getCMCost(),Job.JOB_CM);
					cmJob.setFixedCost(machines.get(compTTF.machineID).compList[compTTF.componentID].getCompCost());
					cmJob.setCompNo(compTTF.componentID);
					temp.get(compTTF.machineID).addCMJob(cmJob, compTTF.ttf);
				}
				else{
					//Since no maintenance is going on add CM job directly
					Job cmJob = new Job("CM",compTTF.ttr,machines.get(compTTF.machineID).compList[compTTF.componentID].getCMCost(),Job.JOB_CM);
					cmJob.setFixedCost(machines.get(compTTF.machineID).compList[compTTF.componentID].getCompCost());
					cmJob.setCompNo(compTTF.componentID);
					temp.get(compTTF.machineID).addCMJob(cmJob, compTTF.ttf);
				}
			}
			
		
		}
		
	}

}
class CompTTF implements Comparable<CompTTF>{
	public long ttf;
	public int machineID;
	public long ttr;
	public int componentID;
	public CompTTF(long ttf2,long ttr2,int count,int compID) {
		ttf = ttf2;
		machineID = count;
		ttr = ttr2;
		componentID = compID;
	}
	@Override
	public int compareTo(CompTTF other) {
		return Long.compare(ttf, other.ttf);
	}
	
}