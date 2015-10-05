package org.isw;

import java.io.File;
import java.io.FileInputStream;
import java.util.Scanner;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


public class Machine {

	int machineStatus = Macros.MACHINE_IDLE;
	int machineNo;
	public  int shiftCount;
	public  Component[] compList;
	public  double cmCost;
	public double pmCost;
	public	 long downTime;
	public long waitTime;
	public int jobsDone;
	public  int cmJobsDone;
	public  int pmJobsDone;
	public  int compCMJobsDone[];
	public  int compPMJobsDone[];
	public  long procCost;
	public  long penaltyCost;
	public  long cmDownTime;
	public  long pmDownTime;
	public  long runTime;
	public  long idleTime;

	
	public Machine(int machineNo, Scanner in){
		System.out.println("Enter no of components of machine "+(machineNo+1)+":");
		int n = in.nextInt();
		compList = parseExcel(n);
		downTime = 0;
		jobsDone = 0;
		cmJobsDone = pmJobsDone = 0;
		shiftCount = 0;
		cmCost = 0;
		pmCost = 0;
		compCMJobsDone = new int[compList.length];
		compPMJobsDone = new int[compList.length];
		cmDownTime=0;
		pmDownTime=0;
		waitTime=0;
		penaltyCost=0;
		procCost=0;
		runTime =0;
		idleTime = 0;		
		this.machineNo = machineNo;
	}
	
	private static Component[] parseExcel(int n) {
		/**
		 * Parse the component excel file into a list of components.
		 * Total number of components should be 14 for our experiment.
		 * Different component excel file for different machineNo (Stick
		 * to one for now)
		 * 
		 * */
		Component[] c = new Component[n];
		try
		{
			FileInputStream file = new FileInputStream(new File("components.xlsx"));
			XSSFWorkbook workbook = new XSSFWorkbook(file);
			XSSFSheet sheet = workbook.getSheetAt(0);
			XSSFSheet labourSheet = workbook.getSheetAt(1);
			for(int i=5;i<5+n;i++)
			{
				Row row = sheet.getRow(i);
				Component comp = new Component();

				//--------CM data------------
				//0 is assembly name
				comp.compName = row.getCell(1).getStringCellValue();
				comp.initAge = row.getCell(2).getNumericCellValue();
				comp.cmEta = row.getCell(3).getNumericCellValue();
				comp.cmBeta = row.getCell(4).getNumericCellValue();
				comp.cmMuRep = row.getCell(5).getNumericCellValue();
				comp.cmSigmaRep = row.getCell(6).getNumericCellValue();
				comp.cmMuSupp = row.getCell(7).getNumericCellValue();
				comp.cmSigmaSupp = row.getCell(8).getNumericCellValue();
				comp.cmRF = row.getCell(9).getNumericCellValue();
				comp.cmCostSpare = row.getCell(10).getNumericCellValue();
				comp.cmCostOther = row.getCell(11).getNumericCellValue();
				//12 is empty
				//13 is empty

				//--------PM data------------
				//14 is assembly name
				//15 is component name
				//16 is init age
				comp.pmMuRep = row.getCell(17).getNumericCellValue();
				comp.pmSigmaRep = row.getCell(18).getNumericCellValue();
				comp.pmMuSupp = row.getCell(19).getNumericCellValue();
				comp.pmSigmaSupp = row.getCell(20).getNumericCellValue();
				comp.pmRF = row.getCell(21).getNumericCellValue();
				comp.pmCostSpare = row.getCell(22).getNumericCellValue();
				comp.pmCostOther = row.getCell(23).getNumericCellValue();
				row = labourSheet.getRow(i);
				comp.labourCost = new double[]{800,500,300};
				comp.pmLabour = new int[3];
				comp.pmLabour[0] = (int)row.getCell(3).getNumericCellValue();
				comp.pmLabour[1] = (int)row.getCell(5).getNumericCellValue();
				comp.pmLabour[2] = (int)row.getCell(7).getNumericCellValue();
				comp.cmLabour = new int[3];
				comp.cmLabour[0] = (int)row.getCell(2).getNumericCellValue();
				comp.cmLabour[1] = (int)row.getCell(4).getNumericCellValue();
				comp.cmLabour[2] = (int)row.getCell(6).getNumericCellValue();
				comp.initProps(i-5);
				c[i-5] = comp;
			}
			file.close();

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return c;
	}

	public void setStatus(int status)
	{
		machineStatus = status;
	}
	
	public int getStatus()
	{
		return machineStatus;
	}
	

}