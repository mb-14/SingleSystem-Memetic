package org.isw;

import java.io.File;
import java.io.FileInputStream;
import java.util.Scanner;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


public class Machine {

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

	
	public Machine(int machineNo){
		System.out.println("Enter age of machine "+(machineNo+1)+" in hours:");
		Scanner in = new Scanner(System.in);
		int age = in.nextInt();
		System.out.println("No of components:");
		int n = in.nextInt();
		compList = parseExcel(age,n);
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
	
	private static Component[] parseExcel(int age, int n) {
		/**
		 * Parse the component excel file into a list of components.
		 * Total number of components should be 14 for our experiment.
		 * Different component excel file for different machineNo (Stick
		 * to one for now)
		 * **/
		Component[] c = new Component[n];
		try
		{
			FileInputStream file = new FileInputStream(new File("Components.xlsx"));
			XSSFWorkbook workbook = new XSSFWorkbook(file);
			XSSFSheet sheet = workbook.getSheetAt(0);
			
			for(int i=4;i<4+n;i++)
			{
				Row row = sheet.getRow(i);
				Component comp = new Component();
				comp.compName = row.getCell(1).getStringCellValue();
				comp.p1 = row.getCell(2).getNumericCellValue();
				comp.p2 = row.getCell(3).getNumericCellValue();
				comp.p3 = row.getCell(4).getNumericCellValue();
				
				comp.cmEta = row.getCell(5).getNumericCellValue();
				comp.cmBeta = row.getCell(6).getNumericCellValue();
				
				comp.cmMu = row.getCell(7).getNumericCellValue();
				comp.cmSigma = row.getCell(8).getNumericCellValue();
				comp.cmRF = row.getCell(9).getNumericCellValue();
				comp.cmCost = row.getCell(10).getNumericCellValue();
				
				comp.pmMu = row.getCell(11).getNumericCellValue();
				comp.pmSigma = row.getCell(12).getNumericCellValue();
				comp.pmRF = row.getCell(13).getNumericCellValue();
				comp.pmCost = row.getCell(14).getNumericCellValue();
				comp.pmFixedCost = row.getCell(15).getNumericCellValue();
				comp.initAge = age;
				c[i-4] = comp;
			}
			file.close();
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return c;
	}


}