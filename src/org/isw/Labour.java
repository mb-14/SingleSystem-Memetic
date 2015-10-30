package org.isw;

public class Labour {
	int labour[];
	public Labour(int labour[]){
		this.labour = new int[labour.length];
		this.labour[0] = labour[0];
		this.labour[1] = labour[1];
		this.labour[2] = labour[2];
	}

	public synchronized boolean checkAvailability(){
		return false;
	}

}
