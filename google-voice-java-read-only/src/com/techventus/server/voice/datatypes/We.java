
package com.techventus.server.voice.datatypes;

import java.util.List;
/**
 * 
 * Weekend setting of a phone
 *
 */
public class We{
   	private boolean allDay;
   	private List<String> times;

 	public boolean getAllDay(){
		return this.allDay;
	}
	public void setAllDay(boolean allDay){
		this.allDay = allDay;
	}
 	public List<String> getTimes(){
		return this.times;
	}
	public void setTimes(List<String> times){
		this.times = times;
	}
}
