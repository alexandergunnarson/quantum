package com.techventus.server.voice.datatypes;

import com.techventus.server.voice.util.ParsingUtil;

/**
 * 
 * A Contact
 * TODO I think we need to add several subclasses for the different contact infos
 * like phone numbers, Email addresses and such
 * 
 * @author Tobias Eisentraeger
 *
 */
public class Contact implements Comparable<Contact>{
	/** The Display name of the contact **/
	private String name;
	/** Googles id of the contact **/
	private String id;
	/** A phone number **/
	private String number;
	/** The Url of the Contacts image on the web **/
	private String imageUrl;
	
	public Contact(String name, String id, String number, String imageUrl) {
		super();
		this.name = name;
		this.id = id;
		this.number = number;
		this.imageUrl = imageUrl;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * Gets the Display name of the contact
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * Gets the phone number
	 * @return the number
	 */
	public String getNumber() {
		return number;
	}
	/**
	 * @param number the number to set
	 */
	public void setNumber(String number) {
		this.number = number;
	}
	/**
	 * Gets the Url of the Contacts image on the web
	 * @return the imageUrl
	 */
	public String getImageUrl() {
		return imageUrl;
	}
	/**
	 * Sets the Url of the Contacts image on the web
	 * @param imageUrl the imageUrl to set
	 */
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String ret = "{";
		if(id!=null){
			ret+="id="+id+";";
		}
		if(name!=null){
			ret+="name="+name+";";
		}
		if(number!=null){
			ret+="number="+number+";";
		}
		if(imageUrl!=null){
			ret+="imageUrl="+imageUrl+";";
		}
		return ret+"}";
	}
	
	/**
	 * Deprecated Method.  Creates a Contact out of a html
	 * TODO DummyData - Implement.
	 *
	 * @param html the html
	 * @return the contact
	 */
	@Deprecated
	public static Contact extractContact(String html) {
		html = ParsingUtil.removeUninterestingParts(html,"<table class=\"gc-message-tbl\">","</table>", true);
		
		//if it shows a name <a class="gc-under gc-message-name-link" title="Go to contact" href="javascript://">John Doe</a> 
		String lname;
		try {
			lname = ParsingUtil.removeUninterestingParts(html,"<a class=\"gc-under gc-message-name-link\" title=\"Go to contact\" href=\"javascript://\">","</a>",false);
		} catch (Exception e) {
			lname = "Parsing Error (name)";
		}
		
		String lid;
		try {
			String comingCloser = ParsingUtil.removeUninterestingParts(html,"<span class=\"gc-message-name\">","</span>",true);
			lid = ParsingUtil.removeUninterestingParts(comingCloser,"<span style=\"display: none;\">","</span>",false);
		} catch (Exception e) {
			lid = "Parsing Error (id)";
		}
		
		
		// if it shows a phone number <span class="gc-message-type">(912) 230-0029 - mobile</span>
		String lnumber;
		try {
			String comingCloser = ParsingUtil.removeUninterestingParts(html,"<span class=\"gc-message-type\">","</span>",true);
			lnumber= ParsingUtil.removeUninterestingParts(comingCloser,"<span class=\"gc-message-type\">","</span>",false);
		} catch (Exception e) {
			lnumber = "Parsing Error (number)";
		}
		
		//if it shows an image for the contact
		String limageUrl;
		try {
			// Get the correct part: <td class="gc-message-tbl-portrait"><div class="gc-message-portrait"><img src="/s2/photos/private/AIbEiAIAAAAiCJXwgsvT9Le4tAEQ-ImF9fjo7e8OGNP0s7bfv-WnkgEwAfF87xcvfpqmRykC-cC7cRl_?sz=32" width="32" height="32"><div class="gc-message-icon-2"></div></div></td>
			limageUrl = ParsingUtil.removeUninterestingParts(html,"<td class=\"gc-message-tbl-portrait\">","<div class=\"gc-message-icon",false);
			
			// <img alt="Blue_ghost" src="/voice/resources/1366864992-blue_ghost.jpg"> or <img src="/s2/photos/private/AIbEasdAAAiCJXwgsvT9Le4tAEQ-ImF9fjasdOGNP0s7bfv-WnasdEwAfF87kiMETqBfpqmRykC-cC7cRl_?sz=32" width="32" height="32">
			limageUrl=	ParsingUtil.removeUninterestingParts(html,"<img",">",false);  

			// now we have alt="Blue_ghost" src="/voice/resources/1366864992-blue_ghost.jpg"> or <img src="/s2/photos/private/AIbEasdAAAiCJXwgsvT9Le4tAEQ-ImF9fjasdOGNP0s7bfv-WnasdEwAfF87kiMETqBfpqmRykC-cC7cRl_?sz=32" width="32" height="32"
			limageUrl=  ParsingUtil.removeUninterestingParts(html,"src=\"","\"",false); 

		} catch (Exception e) {
			limageUrl = "Parsing Error (imageUrl)";
		}
		
		return new Contact(lname, lid, lnumber, limageUrl);
	}
	
	
	@Override
	public int compareTo(Contact o) {
		if(o.name.compareTo(name) == 0) {
			return o.number.compareTo(number);
		} else {
			return o.name.compareTo(name);
		}
	}
	
	
	
	
	
	
}
