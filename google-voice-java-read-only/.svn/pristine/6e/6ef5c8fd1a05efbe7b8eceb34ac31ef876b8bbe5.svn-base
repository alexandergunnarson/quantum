package com.techventus.server.voice.util;

import gvjava.org.json.JSONArray;
import gvjava.org.json.JSONException;
import gvjava.org.json.JSONObject;

import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.DOMReader;
import org.w3c.tidy.Tidy;

import com.techventus.server.voice.datatypes.Contact;
import com.techventus.server.voice.datatypes.records.SMS;
import com.techventus.server.voice.datatypes.records.SMSThread;

/**
 * This class parses the SMS messages from the Google Voice XML response.
 * 
 * @author Tiago Proenca (tproenca)
 * 
 */
public class SMSParser {

	/** The server's response. */
	private String xmlResponse;

	/** My contact information. */
	private Contact me;

	/**
	 * Creates a SMSParser instance.
	 * 
	 * @param response
	 *            the server's HTML response.
	 * @param me
	 *            my Contact information.
	 */
	public SMSParser(String response, Contact me) {
		this.xmlResponse = response;
		this.me = me;
	}

	/**
	 * Creates a SMSParser instance.
	 * 
	 * @param response
	 *            the server's HTML response.
	 * @param myPhoneNumber
	 *            my Google Voice phone number.
	 */
	public SMSParser(String response, String myPhoneNumber) {
		this(response, new Contact(GoogleVoice.CONTACT_ME, "", myPhoneNumber,
				""));
	}

	/**
	 * Returns the SMS threads.
	 * 
	 * @return the SMS threads.
	 */
	public Collection<SMSThread> getSMSThreads() {
		Collection<SMSThread> result = new LinkedList<SMSThread>();
		String htmlResponse = ParsingUtil.removeUninterestingParts(xmlResponse,
				FilterResponse.HTML_BEGIN, FilterResponse.HTML_END, false);

		// Use tidy to fix the HTML to a well-formed XML representation.
		Tidy tidy = new Tidy();
		tidy.setXHTML(true);
		// show warnings removes some of the verboseness
		tidy.setShowWarnings(false);
		// However, jtidy is still spewing crap to stderr that is
		// not breaking the parser, but is making it ugly.
		// So we redirect to a bitbucket. This works, but is hacky.
		// It probably shoud go to log4j, if it weren't shakespearean in length.
		tidy.setErrout(new java.io.PrintWriter(new java.io.PrintStream(
				new java.io.OutputStream() {
					public void write(int b) {
					}
				})));
		// TODO: figure out why jtidy generates so much error text and fix
		// the issues if possible
		org.w3c.dom.Document tDOM = tidy.parseDOM(
				new StringReader(htmlResponse), null);

		// Convert the w3c document to the dom4j one (We want to use XPath).
		DOMReader reader = new DOMReader();
		Document doc = reader.read(tDOM);

		Map<String, SMSThread> threadMap = buildSMSThreadMap(xmlResponse);

		// Select the threads using XPath queries.
		@SuppressWarnings("unchecked")
		List<Element> elements = doc.selectNodes(XPathQuery.MESSAGE_ID);
//		int i = -1;
		for (Element element : elements) {
//			System.out.println(element.getStringValue());
//			i++;
				//DEBUG
			if(element==null){
				continue;
			}
//				Contact contact=null;
//					try{
			Contact contact = parseContact(element);
//					}catch(Exception e){
////						System.err.println("Exception element number "+i);
////						System.err.println(element.getStringValue());
//						e.printStackTrace();
//					}
//				if(contact==null){
//					System.err.println("NULL CONTACT "+element);
//					
//				}
			SMSThread smsthread = threadMap.get(element.attribute(
					GoogleVoice.THREAD_ID).getText());
			smsthread.setContact(contact);
			addSMSsToThread(smsthread, element);
			result.add(smsthread);
		}
		return result;
	}

	/**
	 * Parses the contact information from the DOM Element.
	 * 
	 * @param element
	 *            the DOM Element
	 * @return the Contact
	 */
	private Contact parseContact(Element element) {
		String name = element.selectSingleNode(XPathQuery.MESSAGE_NAME_LINK)
				.getText().trim();
		Node phoneNumberNode = element
				.selectSingleNode(XPathQuery.MESSAGE_TYPE);
		String imgURL = parseImgURL((Element) element
				.selectSingleNode(XPathQuery.MESSAGE_PORTRAIT));
		String phoneNumber = phoneNumberNode == null ? name
				: parsePhoneNumber(phoneNumberNode.getText());
		
		
		//TODO TEST SIMPLIFY
//		System.out.println("Parsing Contact...");
		//JLM Phone Number Correction
		List e = element.selectNodes(XPathQuery.MESSAGE_QUICKCALL);
		
		for(Object o:e){
			Node n = (Node)o;
			String res = n.selectSingleNode(XPathQuery.MESSAGE_BOLD).getText();
			//System.out.println(o);
			res =res.replace("(", "").replace(")", "").replace(" ", "").replace("-", "");
			phoneNumber = res;
			if (phoneNumber.indexOf("+") == -1) {
				phoneNumber = "+1" + phoneNumber;
			}
//			System.out.println(phoneNumber);
			
		}	
		
		
		return new Contact(name, "", phoneNumber, imgURL);
	}

	/**
	 * Adds SMSs to their respective thread.
	 * 
	 * @param thread
	 *            the SMSThread object.
	 * @param messages
	 *            the DOM Element that contain all the thread's messages.
	 */
	private void addSMSsToThread(SMSThread thread, Element messages) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				GoogleVoice.DATE_FORMAT);
		SimpleDateFormat dateTimeFormat = new SimpleDateFormat(
				GoogleVoice.DATETIME_FORMAT);
		@SuppressWarnings("unchecked")
		List<Element> elements = messages
				.selectNodes(XPathQuery.MESSAGE_SMS_ROW);
		for (Element element : elements) {
			try {
				String from = element.selectSingleNode(
						XPathQuery.MESSAGE_SMS_FROM).getText().replaceAll(":",
						"").trim();
//				String text = element.selectSingleNode(
//						XPathQuery.MESSAGE_SMS_TEXT).getText().trim();
				//SEE ISSUE 19 Comment 5
				String text = "";
				 if (element.selectSingleNode(XPathQuery.MESSAGE_SMS_TEXT) != null) {
				     text = element.selectSingleNode(XPathQuery.MESSAGE_SMS_TEXT).getText().trim();
				 }
				String dateTime = element.selectSingleNode(
						XPathQuery.MESSAGE_SMS_TIME).getText().trim();
				Contact contact = thread.getContact();
				if (!from.equals(contact.getName())
						&& from.equals(GoogleVoice.CONTACT_ME)) {
					contact = me;
				}
				Date time = dateTimeFormat.parse(dateFormat.format(thread
						.getDate())
						+ " " + dateTime);
				if (!time.before(thread.getDate())) {
					time = new Date(time.getTime()
							- GoogleVoice.DAY_MILLISECONDS);
				}
				thread.addSMS(new SMS(contact, text, time));
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Builds a map between the thread id and a SMSThread object. Since we need
	 * to look for a SMSThread object quite often, this approach is preferable
	 * than a List, since it reduces the lookup operations from O(n) to O(1).
	 * 
	 * @param response
	 *            the server's HTML response.
	 * @return a map between the thread id and a SMSThread object.
	 */
	private Map<String, SMSThread> buildSMSThreadMap(String response) {
		Map<String, SMSThread> result = new HashMap<String, SMSThread>();
		String jsonResponse = ParsingUtil.removeUninterestingParts(response,
				FilterResponse.JSON_BEGIN, FilterResponse.JSON_END, false);
		try {
			JSONObject json = new JSONObject(jsonResponse);
			JSONObject messages = json.getJSONObject(JSONContants.MESSAGES);
			JSONArray names = messages.names();
			for (int i = 0; i < names.length(); i++) {
				JSONObject jsonSmsThread = messages.getJSONObject(names
						.getString(i));
				String id = jsonSmsThread.has(JSONContants.ID) ? jsonSmsThread
						.getString(JSONContants.ID) : "";
				long startTime = jsonSmsThread.has(JSONContants.START_TIME) ? jsonSmsThread
						.getLong(JSONContants.START_TIME)
						: 0;
				String note = jsonSmsThread.has(JSONContants.NOTE) ? jsonSmsThread
						.getString(JSONContants.NOTE)
						: "";
				boolean isRead = jsonSmsThread.has(JSONContants.IS_READ) ? jsonSmsThread
						.getBoolean(JSONContants.IS_READ)
						: false;
				boolean isStarred = jsonSmsThread.has(JSONContants.STARRED) ? jsonSmsThread
						.getBoolean(JSONContants.STARRED)
						: false;
				SMSThread smsThread = new SMSThread(id, note, new Date(
						startTime), null, isRead, isStarred);
				result.put(id, smsThread);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Parses the contact's phone number from the server's HTML response.
	 * 
	 * @param phoneNumber
	 *            the phone number from the server's HTML response.
	 * @return the contact's phone number.
	 */
	private String parsePhoneNumber(String phoneNumber) {
		String result = phoneNumber.replaceAll(GoogleVoice.PHONE_REGEX, "");
		if (result.indexOf("+") == -1) {
			result = GoogleVoice.DEFAULT_COUNTRY_CODE + result;
		}
		return result;
	}

	/**
	 * Parses the contact's image URL from the DOM Element.
	 * 
	 * @param element
	 *            the DOM Element
	 * @return the contact's image URL.
	 */
	private String parseImgURL(Element element) {
		Element img = (Element) element
				.selectSingleNode(XPathQuery.MESSAGE_IMG);
		return GoogleVoice.GOOGLE_URL
				+ img.attribute(GoogleVoice.IMG_SRC).getText();
	}

	/** General Google Voice Constants. */
	private final class GoogleVoice {
		public static final String THREAD_ID = "id";
		public static final String DATE_FORMAT = "MM/dd/yy";
		public static final String TIME_FORMAT = "h:mm a";
		public static final String DATETIME_FORMAT = DATE_FORMAT + " "
				+ TIME_FORMAT;
		public static final long DAY_MILLISECONDS = 24 * 60 * 60 * 1000;
		public static final String GOOGLE_URL = "http://www.google.com";
		public static final String CONTACT_ME = "Me";
		public static final String IMG_SRC = "src";
		public static final String PHONE_REGEX = "[\\s\\p{Punct}]|[a-z]";
		public static final String DEFAULT_COUNTRY_CODE = "+1";
	}

	/** XPath queries constants. */
	private final class XPathQuery {
		public static final String MESSAGE_SMS_FROM = "descendant::span[@class='gc-message-sms-from']";
		public static final String MESSAGE_SMS_TEXT = "descendant::span[@class='gc-message-sms-text']";
		public static final String MESSAGE_SMS_TIME = "descendant::span[@class='gc-message-sms-time']";
		public static final String MESSAGE_SMS_ROW = "descendant::div[@class='gc-message-sms-row']";
//		public static final String MESSAGE_NAME_LINK = "descendant::a[@class='gc-under gc-message-name-link']";
		public static final String MESSAGE_NAME_LINK = "descendant::a[contains(@class,'gc-under')]";
		public static final String MESSAGE_TYPE = "descendant::span[@class='gc-message-type']";
		public static final String MESSAGE_ID = "/*/*/div[@id]";
		public static final String MESSAGE_PORTRAIT = "descendant::div[@class='gc-message-portrait']";
		public static final String MESSAGE_IMG = "descendant::img";
		public static final String MESSAGE_BOLD = "descendant::b";
		public static final String MESSAGE_QUICKCALL = "descendant::form[@name='quickcall']";
	}

	/** Filter responses constants. */
	private final class FilterResponse {
		public static final String HTML_BEGIN = "<html><![CDATA[";
		public static final String HTML_END = "]]></html>";
		public static final String JSON_BEGIN = "<json><![CDATA[";
		public static final String JSON_END = "]]></json>";
	}
}
