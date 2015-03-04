package com.techventus.server.voice.datatypes.records;

import java.util.Date;

import com.techventus.server.voice.datatypes.Contact;


/**
 * 
 * A Record is one entry in a Voice record list(like the Inbox)
 * An implemented Class is a call entry or an SMS or a Voicemail 
 * 
 * TODO give better name?
 * 
 * @author Tobias Eisentraeger
 *
 */
public abstract class Record {
	/** The id of the record **/
	protected String id;
	/** The title of the record **/
	protected String title;
	/** The date of the record **/
	protected Date date;
	/** The Contact of this Record, like who called in  **/
	protected Contact contact;
	/** true if the message is read, false if not **/
	protected boolean read;
	
	public Record(String id, String title, Date date, Contact contact, boolean read) {
		super();
		this.id = id;
		this.title = title;
		this.date = date;
		this.contact = contact;
		this.read = read;
	}
	
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets the id.
	 *
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * Gets a boolean for whether or not the conversation has been read.
	 *
	 * @return the read
	 */
	public boolean getRead(){
		return read;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}
	/**
	 * @param title the title to set
	 */
	public void setTitle(String pTitle) {
		title = pTitle;
	}
	/**
	 * @return the date
	 */
	public Date getDate() {
		return date;
	}
	/**
	 * @param date the date to set
	 */
	public void setDate(Date pDate) {
		date = pDate;
	}
	/**
	 * @return the contact
	 */
	public Contact getContact() {
		return contact;
	}
	/**
	 * @param contact the contact to set
	 */
	public void setContact(Contact pContact) {
		contact = pContact;
	}
	/** Returns a String representation **/
	public abstract String toString();
	
	public boolean isVoicemail() {
		if(this instanceof Voicemail) {
			return true;
		} else {
			return false;
		}
	}
	public boolean isCall() {
		if(this instanceof Call) {
			return true;
		} else {
			return false;
		}
	}
	public boolean isShortMessage() {
		if(this instanceof ShortMessage) {
			return true;
		} else {
			return false;
		}
	}
	
}
