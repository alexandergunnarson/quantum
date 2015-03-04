package com.techventus.server.voice.datatypes.records;

import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import com.techventus.server.voice.datatypes.Contact;

/**
 * Represents a thread of SMS messages.
 * 
 * @author Tiago Proenca (tproenca)
 * 
 */
public class SMSThread extends Record {

	/** List of SMS messages. */
	private Set<SMS> listSms = new TreeSet<SMS>();

	/** Whether the thread is starred. */
	private boolean starred;

	/**
	 * Creates a SMSThread instance.
	 * 
	 * @param id
	 *            the thread's ID
	 * @param note
	 *            the thread's note
	 * @param date
	 *            the thread's timestamp
	 * @param contact
	 *            the thread's contact
	 * @param read
	 *            whether the thread is starred
	 * @param starred
	 *            whether the thread is read.
	 */
	public SMSThread(String id, String note, Date date, Contact contact,
			boolean read, boolean starred) {
		super(id, note, date, contact, read);
		this.starred = starred;
	}

	/**
	 * Adds a SMS object to this thread.
	 * 
	 * @param sms
	 *            a SMS object.
	 */
	public void addSMS(SMS sms) {
		listSms.add(sms);
	}

	/**
	 * Removes a SMS object from this thread.
	 * 
	 * @param sms
	 *            a SMS object.
	 */
	public void removeSMS(SMS sms) {
		listSms.remove(sms);
	}

	/**
	 * Returns all SMS that exist in this thread.
	 * 
	 * @return all SMS objects.
	 */
	public Collection<SMS> getAllSMS() {
		return listSms;
	}

	/**
	 * Returns the thread's note.
	 * 
	 * @return the thread's note.
	 */
	public String getNote() {
		return title;
	}

	/**
	 * Returns whether this thread is starred.
	 * 
	 * @return whether this thread is starred
	 */
	public boolean isStarred() {
		return starred;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "SMSThread [id=" + id + ", title=" + title + ", date=" + date
				+ ", contact=" + contact + ", read=" + read + ", starred="
				+ starred + ", listSms=" + listSms + "]";
	}

}
