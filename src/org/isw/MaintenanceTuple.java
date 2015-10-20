package org.isw;

import java.io.Serializable;

public class MaintenanceTuple implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public long start;
	public long end;
	public int[] labour;
	
	public MaintenanceTuple(long start, long end, int[] labour)
	{
		this.start = start;
		this.end = end;
		this.labour = labour;
	}
	
	public MaintenanceTuple(MaintenanceTuple mt)
	{
		start = mt.start;
		end = mt.end;
		labour = new int[3];
		labour[0] = mt.labour[0];
		labour[1] = mt.labour[1];
		labour[2] = mt.labour[2];
	}
	
	public MaintenanceTuple(int start)
	{
		this.start = -1;
	}

	public void print() {
		System.out.format("Start: %d End %d Laboour %d %d %d\n",start,end,labour[0],labour[1],labour[2]);
		
	}
}
