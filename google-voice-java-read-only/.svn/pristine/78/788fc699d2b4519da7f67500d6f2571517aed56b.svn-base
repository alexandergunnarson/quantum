package com.techventus.server.voice.datatypes.records;

import java.util.Date;

import com.techventus.server.voice.datatypes.Contact;

/**
 * Represents a SMS message.
 * 
 * @author Tiago Proenca (tproenca)
 * 
 */
public class SMS implements Comparable<SMS> {

	/** From contact. */
	private Contact from;

	/** Content of the message. */
	private String content;

	/** Timestamp of the message */
	private Date dateTime;

	/**
	 * Creates a SMS instance.
	 * 
	 * @param from
	 *            the contact that sent the message
	 * @param the
	 *            message's content
	 * @param dateTime
	 *            the message's timestamp
	 */
	public SMS(Contact from, String content, Date dateTime) {
		this.from = from;
		this.content = content;
		this.dateTime = dateTime;
	}

	/**
	 * Returns the contact that sent the message.
	 * 
	 * @return the contact that sent the message
	 */
	public Contact getFrom() {
		return from;
	}

	/**
	 * Returns the message's content.
	 * 
	 * @return the message's content
	 */
	public String getContent() {
		return content;
	}

	/**
	 * Returns the message's timestamp.
	 * 
	 * @return the message's timestamp
	 */
	public Date getDateTime() {
		return dateTime;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "SMS [dateTime=" + dateTime + ", from=" + from + ", text="
				+ content + "]";
	}

	@Override
	public int compareTo(SMS o) {
		int dateTimeCompare = o.dateTime.compareTo(dateTime);
		if(dateTimeCompare == 0) {
			int fromCompare = o.from.compareTo(from);
			if(fromCompare == 0) {
				return o.content.compareTo(content);
			} else {
				return fromCompare;
			}
		} else {
			return dateTimeCompare;
		}
	}
}
