/*
 * Voice.java
 *
 * Created: Sat Mar  13 14:41:11 2010
 *
 * Copyright (C) 2010-2012 Techventus, LLC
 * 
 * Techventus, LLC is not responsible for any use or misuse of this product.
 * In using this software you agree to hold harmless Techventus, LLC and any other
 * contributors to this project from any damages or liabilities which might result 
 * from its use.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.techventus.server.voice;

import gvjava.org.json.JSONException;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.techventus.server.voice.datatypes.AllSettings;
import com.techventus.server.voice.datatypes.Greeting;
import com.techventus.server.voice.datatypes.Group;
import com.techventus.server.voice.datatypes.Phone;
import com.techventus.server.voice.datatypes.records.SMSThread;
import com.techventus.server.voice.exception.AuthenticationException;
import com.techventus.server.voice.exception.ERROR_CODE;
import com.techventus.server.voice.util.ParsingUtil;
import com.techventus.server.voice.util.SMSParser;

/**
 * The Class Voice. This class is the basis of the entire API and contains all
 * the components necessary to connect and authenticate with Google Voice, place
 * calls and SMS, and pull in the raw data from the account.
 * 
 * @author Techventus, LLC
 */
@SuppressWarnings("deprecation")
public class Voice {

	/** The PRINT to Console FLAG setting. */
	public boolean PRINT_TO_CONSOLE;
	
	/** keeps the list of phones - lazy. */

	private AllSettings settings;
	
	/** The general. */
	String general = null;
	
	/** The phones info. */
	String phonesInfo = null;
	
	/** The rnr see. */
	String rnrSEE = null;
	
	/** The error. */
	private ERROR_CODE error;

	/**
	 * Short string identifying your application, for logging purposes. This string should take the form:
	 * "companyName-applicationName-versionID". See: http://code.google.com/apis/accounts/docs/AuthForInstalledApps.html#Request
	 */
	String source = null;
	/**
	 * User's full email address. It must include the domain (i.e. johndoe@gmail.com).
	 */
	private String user = null;
	/**
	 * User's password.
	 */
	private String pass = null;
	/**
   * Google Voice Phone Number.
   */
  private String phoneNumber = null;
	/**
	 * Once the login information has been successfully authenticated, Google returns a token, which your 
	 * application will reference each time it requests access to the user's account.
	 * This token must be included in all subsequent requests to the Google service for this account. 
	 * Authorization tokens should be closely guarded and should not be given to any other application, 
	 * as they represent access to the user's account. The time limit on the token varies depending on 
	 * which service issued it.
	 */
	private String authToken = null;
	/**
	 * (optional) Token representing the specific CAPTCHA challenge. Google supplies this token and the 
	 * CAPTCHA image URL in a login failed response with the error code "CaptchaRequired".
	 */
	private String captchaToken = null;
	
	/** Url of the image with the captcha - only filled after a captacha response to a login try. */
	private String captchaUrl = null;
	
	/** The captcha url2. */
	private String captchaUrl2 = null;
	
	/** Counts the amount of redirects we are doing in the get(String url) method to avoid infinite loop. */
	private int redirectCounter = 0;
	
	/** Maximum amount of redirects before we throw an exception. */
	private static int MAX_REDIRECTS = 5;
	
	/** The Constant enc. */
	final static String enc = "UTF-8";
	
	/** The Constant USER_AGENT. */
	final static String USER_AGENT = "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/525.13 (KHTML, like Gecko) Chrome/0.A.B.C Safari/525.13";
	
	/** The Constant GOOGLE. */
	public final static String GOOGLE = "GOOGLE";
	
	/** The Constant HOSTED. */
	public final static String HOSTED = "HOSTED";
	
	/** The Constant HOSTED_OR_GOOGLE. */
	public final static String HOSTED_OR_GOOGLE = "HOSTED_OR_GOOGLE";
	/**
	 * Type of account to request authorization for. Possible values are: <br/><br/>
	 * -<b>GOOGLE</b> (get authorization for a Google account only) <br/>
	 * -<b>HOSTED</b> (get authorization for a hosted account only) <br/>
	 * -<b>HOSTED_OR_GOOGLE</b> (get authorization first for a hosted account; if attempt fails, get 
	 * authorization for a Google account)<br/><br/>		
	 * Use <b>HOSTED_OR_GOOGLE</b> if you're not sure which type of account you want authorization for. 
	 * If the user information matches both a hosted and a Google account, only the hosted account is authorized.
	 */
	private String account_type = GOOGLE; 
	
	/**
	 * Name of the Google service you're requesting authorization for. Each service using the Authorization 
	 * service is assigned a name value; for example, the name associated with Google Calendar is 'cl'. 
	 * This parameter is required when accessing services based on Google Data APIs. For specific service 
	 * names, refer to the service documentation.
	 */
	final static String SERVICE = "grandcentral";
	
	/** The Constant generalURLString. */
	final static String generalURLString = "https://www.google.com/voice/b/0";
	
	/** The Constant loginURLString. */
	final static String loginURLString = "https://www.google.com/accounts/ClientLogin";
	
	/** The Constant inboxURLString. */
	final static String inboxURLString = "https://www.google.com/voice/b/0/inbox/recent/inbox/";
	
	/** The Constant starredURLString. */
	final static String starredURLString = "https://www.google.com/voice/b/0/inbox/recent/starred/";
	
	/** The Constant recentAllURLString. */
	final static String recentAllURLString = "https://www.google.com/voice/b/0/inbox/recent/all/";
	
	/** The Constant spamURLString. */
	final static String spamURLString = "https://www.google.com/voice/b/0/inbox/recent/spam/";
	
	/** The Constant trashURLString. */
	final static String trashURLString = "https://www.google.com/voice/b/0/inbox/recent/spam/";
	
	/** The Constant voicemailURLString. */
	final static String voicemailURLString = "https://www.google.com/voice/b/0/inbox/recent/voicemail/";
	
	/** The Constant smsURLString. */
	final static String smsURLString = "https://www.google.com/voice/b/0/inbox/recent/sms/";
	
	/** The Constant recordedURLString. */
	final static String recordedURLString = "https://www.google.com/voice/b/0/inbox/recent/recorded/";
	
	/** The Constant placedURLString. */
	final static String placedURLString = "https://www.google.com/voice/b/0/inbox/recent/placed/";
	
	/** The Constant receivedURLString. */
	final static String receivedURLString = "https://www.google.com/voice/b/0/inbox/recent/received/";
	
	/** The Constant missedURLString. */
	final static String missedURLString = "https://www.google.com/voice/b/0/inbox/recent/missed/";
	
	/** The Constant phoneEnableURLString. */
	final static String phoneEnableURLString = "https://www.google.com/voice/b/0/settings/editDefaultForwarding/";
	
	/** The Constant generalSettingsURLString. */
	final static String generalSettingsURLString = "https://www.google.com/voice/b/0/settings/editGeneralSettings/";
	
	/** The Constant editForwardingSMSURLString. */
	final static String editForwardingSMSURLString = "https://www.google.com/voice/b/0/settings/editForwardingSms/";
	
	/** The Constant phonesInfoURLString. */
	final static String phonesInfoURLString = "https://www.google.com/voice/b/0/settings/tab/phones";
	
	/** The Constant groupsInfoURLString. */
	final static String groupsInfoURLString = "https://www.google.com/voice/b/0/settings/tab/groups";
	
	/** The Constant voicemailInfoURLString. */
	final static String voicemailInfoURLString = "https://www.google.com/voice/b/0/settings/tab/voicemailsettings";
	
	/** The Constant groupsSettingsURLString. */
	final static String groupsSettingsURLString = "https://www.google.com/voice/b/0/settings/editGroup/";
	
	/** The Constant voicemailDownloadURLString. */
	final static String voicemailDownloadURLString = "https://www.google.com/voice/media/send_voicemail/";
	
	/** The Constant markAsReadString. */
	final static String markAsReadString = "https://www.google.com/voice/b/0/inbox/mark/";
	
	/** The Constant unreadSMSString. */
	final static String unreadSMSString = "https://www.google.com/voice/b/0/inbox/recent/sms/unread/";
  
	//Experimental  keyFlag is just there to overload method
	public Voice(String authToken ) throws IOException{
		this.authToken = authToken;
		this.pass = "UNKNOWN";
		this.user = "UNKNOWN";
		
		this.source = "GoogleVoiceJava";
		
	
		this.general = 	getGeneral();
		this.setRNRSEE();
			
		String response = this.getRawPhonesInfo();
		int phoneIndex = response.indexOf("gc-user-number-value\">");
		this.phoneNumber = response.substring(phoneIndex + 22, phoneIndex + 36);
		this.phoneNumber = this.phoneNumber.replaceAll("[^a-zA-Z0-9]", "");
		if (this.phoneNumber.indexOf("+") == -1) {
			this.phoneNumber = "+1" + this.phoneNumber;
		}

	}

	/**
	 * Instantiates a new voice. This constructor is deprecated. Try
	 * Voice(String user, String pass) which automatically determines rnrSee and
	 * assigns a source.
	 * 
	 * @param user
	 *            the user
	 * @param pass
	 *            the pass
	 * @param source
	 *            the source
	 * @param rnrSee
	 *            the rnr see
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Deprecated
	public Voice(String user, String pass, String source, String rnrSee)
			throws IOException {

		this.user = user;
		this.pass = pass;
		this.rnrSEE = rnrSee;
		this.source = source;

		login();
	}

	/**
	 * A constructor which which allows a custom source.
	 * This Constructor enables verbose output.
	 * 
	 * @param user
	 *            the username in the format of user@gmail.com or user@googlemail.com
	 * @param pass
	 *            the password
	 * @param source
	 *            Short string identifying your application, for logging purposes. This string should take the form:
					"companyName-applicationName-versionID". See: http://code.google.com/apis/accounts/docs/AuthForInstalledApps.html#Request
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public Voice(String user, String pass, String source) throws IOException {
		init(user, pass, source, true, GOOGLE, null, null);

	}

	/**
	 * Instantiates a new Voice Object. This is generally the simplest and
	 * preferred constructor. This Constructor enables verbose output.
	 * 
	 * @param user
	 *            the username in the format of user@gmail.com or user@googlemail.com
	 * @param pass
	 *            the pass
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public Voice(String user, String pass) throws IOException {
		init(user, pass, null, true, GOOGLE, null, null);
	}

	/**
	 * Instantiates a new voice. Custom Source Variable allowed, and
	 * printDebugIntoSystemOut which allows for Verbose output.
	 * 
	 * @param user
	 *            the username in the format of user@gmail.com or user@googlemail.com
	 * @param pass
	 *            the password
	 * @param source
	 *            the arbitrary source identifier.  Can be anything.
	 * @param printDebugIntoToSystemOut
	 *            the print debug into to system out
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public Voice(String user, String pass, String source,
			boolean printDebugIntoToSystemOut) throws IOException {
		init(user, pass, source, printDebugIntoToSystemOut, GOOGLE, null, null);
	}
	
	/**
	 * Instantiates a new voice. Custom Source Variable allowed, and
	 * printDebugIntoSystemOut which allows for Verbose output.
	 * 
	 * @param user
	 *            the username in the format of user@gmail.com or user@googlemail.com
	 * @param pass
	 *            the password
	 * @param source
	 *            the arbitrary source identifier.  Can be anything.
	 * @param printDebugIntoToSystemOut
	 *            the print debug into to system out
	 * @param accountType
	 * 			  Type of account to request authorization for. Possible values are:
	 *			Voice.GOOGLE (get authorization for a Google account only) 
	 *			Voice.HOSTED (get authorization for a hosted account only) 
	 *			Voice.HOSTED_OR_GOOGLE (get authorization first for a hosted account; if attempt fails, get authorization for a Google account)
	 *			Use Voice.HOSTED_OR_GOOGLE if you're not sure which type of account you want authorization for. If the user information matches both a hosted and a Google account, only the hosted account is authorized.
	 *
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public Voice(String user, String pass, String source,
			boolean printDebugIntoToSystemOut, String accountType) throws IOException {
		init(user, pass, source, printDebugIntoToSystemOut, accountType, null, null);
	}
	
	/**
	 * Instantiates a new voice. Custom Source Variable allowed, and
	 * printDebugIntoSystemOut which allows for Verbose output.
	 * 
	 * @param user
	 *            the username in the format of user@gmail.com or user@googlemail.com
	 * @param pass
	 *            the password
	 * @param source
	 *            the arbitrary source identifier.  Can be anything.
	 * @param printDebugIntoToSystemOut
	 *            the print debug into to system out
	 * @param accountType
	 * 			  Type of account to request authorization for. Possible values are:
	 *			Voice.GOOGLE (get authorization for a Google account only) 
	 *			Voice.HOSTED (get authorization for a hosted account only) 
	 *			Voice.HOSTED_OR_GOOGLE (get authorization first for a hosted account; if attempt fails, get authorization for a Google account)
	 *			Use Voice.HOSTED_OR_GOOGLE if you're not sure which type of account you want authorization for. If the user information matches both a hosted and a Google account, only the hosted account is authorized.
	 * @param captchaResponse
	 * 				response to a captcha challenge, set to null if normal login
	 * @param captchaToken
	 * 				(optional) token which matches the response/url from the captcha challenge
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public Voice(String user, String pass, String source,
			boolean printDebugIntoToSystemOut, String accountType, String captchaResponse, String captchaToken) throws IOException {
		init(user, pass, source, printDebugIntoToSystemOut, accountType, captchaResponse, captchaToken);
	}

	/**
	 * Internal function used by all constructors to fully initiate the Voice
	 * Object without chaptcha Response.
	 *
	 * @param user the username in the format of user@gmail.com or user@googlemail.com
	 * @param pass the password for the google account
	 * @param source the source
	 * @param printDebugIntoToSystemOut the print debug into to system out
	 * @param accountType Type of account to request authorization for. Possible values are:
	 * Voice.GOOGLE (get authorization for a Google account only)
	 * Voice.HOSTED (get authorization for a hosted account only)
	 * Voice.HOSTED_OR_GOOGLE (get authorization first for a hosted account; if attempt fails, get authorization for a Google account)
	 * Use Voice.HOSTED_OR_GOOGLE if you're not sure which type of account you want authorization for. If the user information matches both a hosted and a Google account, only the hosted account is authorized.
	 * @param captchaResponse response to a captcha challenge, set to null if normal login
	 * @param captchaToken token which matches the response/url from the captcha challenge
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private void init(String user, String pass, String source,
			boolean printDebugIntoToSystemOut, String accountType, String captchaResponse, String captchaToken) throws IOException {
		if(accountType==GOOGLE||accountType==HOSTED||accountType==HOSTED_OR_GOOGLE) {
			this.account_type = accountType;
			this.PRINT_TO_CONSOLE = printDebugIntoToSystemOut;
			this.user = user;
			this.pass = pass;
			// this.rnrSEE = rnrSee;
			if (source != null) {
				this.source = source;
			} else {
				this.source = "GoogleVoiceJava";
			}
			
			login(captchaResponse,captchaToken);
			this.general = getGeneral();
			setRNRSEE();
		} else {
			throw new IOException("AccountType not valid");
		}
	}
	
        /**
         * Returns the username
         * @return username for gvoice account
         */
        public String getUsername()
        {
            return this.user;
        }
	
	/**
	 * Returns the Greeting list - Lazy
	 * @param forceUpdate set to true to force a List update from the server
	 * @return List of Greeting objects
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws JSONException the jSON exception
	 */
	public List<Greeting> getVoicemailList(boolean forceUpdate) throws IOException, JSONException {
		List<Greeting> lGList = new ArrayList<Greeting>();
		Greeting[] lGArray = getSettings(forceUpdate).getSettings().getGreetings();
		for (int i = 0; i < lGArray.length; i++) {
			lGList.add(lGArray[i]);
		}
		return lGList;
	}
	
	/**
	 * Returns the Group list - Lazy.  Not yet Implemented
	 *
	 * @param forceUpdate the force update
	 * @return List of Greeting objects
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public List<String> getGroupSettingsList(boolean forceUpdate) throws IOException {
//		return getSettings(forceUpdate).getGroupSettingsList();
//		List<String> lGList = new ArrayList<Group>();
//		String[] lGArray = getSettings(forceUpdate).getSettings().getGroups().;
//		for (int i = 0; i < lGArray.length; i++) {
//			lGList.add(lGArray[i]);
//		}
//		return lGList;
		//TODO implement getGroupSettingsList
		return null;
	}
	
	/**
	 * returns all users settings - lazy.
	 *
	 * @param forceUpdate the force update
	 * @return the settings
	 * @throws JSONException the jSON exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public AllSettings getSettings(boolean forceUpdate) throws JSONException, IOException {
		if(settings==null || forceUpdate) {
			if(isLoggedIn()==false || forceUpdate) {
				login();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {

					e.printStackTrace();
				}
			}
			if(PRINT_TO_CONSOLE) System.out.println("Fetching Settings.");
			// remove html overhead
			String lJson = ParsingUtil.removeUninterestingParts(get(groupsInfoURLString), "<json><![CDATA[", "]]></json>", false);
			try {
				settings = new AllSettings(lJson);
			} catch (JSONException e) {
				throw new JSONException(e.getMessage()+lJson);
			}
		}
		return settings;
	}

	// public Voice(){
	// authToken = "abcde";
	// }

	/**
	 * Fetches and returns the raw page source code for the Inbox.
	 * 
	 * @return the inbox
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public String getInbox() throws IOException {
		return get(inboxURLString);
	}
	
	/**
	 * Gets the inbox page.
	 *
	 * @param page the page
	 * @return the inbox page
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String getInboxPage(int page) throws IOException {
		return get(inboxURLString,page);
	}

	/**
	 * Fetches the page Source Code for the Voice homepage. This file contains
	 * most of the useful information for the Google Voice Account such as
	 * attached PhoneOld info and Contacts.
	 * 
	 * @return the general
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public String getGeneral() throws IOException {
		return get(generalURLString);
	}
	
	/**
	 * The main Google Voice section is paginated.  Access the raw HTML for 
	 * specific page of the main section.
	 *
	 * @param page the page
	 * @return the general page
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String getGeneralPage(int page) throws IOException {
		return get(generalURLString,page);
	}

	/**
	 * Gets the raw page source code for the starred items.
	 * 
	 * @return the starred item page source
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public String getStarred() throws IOException {
		return get(starredURLString);
	}
	
	/**
	 * Gets the starred page.
	 *
	 * @param page the page
	 * @return the starred page
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String getStarredPage(int page) throws IOException {
		return get(starredURLString,page);
	}


	/**
	 * Gets the raw page source code for the recent items.
	 * 
	 * @return the recent raw source code
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public String getRecent() throws IOException {
		return get(recentAllURLString);
	}
	
	/**
	 * Gets the recent page.
	 *
	 * @param page the page
	 * @return the recent page
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String getRecentPage(int page) throws IOException {
		return get(recentAllURLString,page);
	}

	/**
	 * Gets the page source for the spam.
	 * 
	 * @return the spam
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public String getSpam() throws IOException {
		return get(spamURLString);
	}
	
	/**
	 * Gets the spam page.
	 *
	 * @param page the page
	 * @return the spam page
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String getSpamPage(int page) throws IOException {
		return get(spamURLString,page);
	}

	/**
	 * Gets the page source for the recorded calls.
	 * 
	 * @return the recorded
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public String getRecorded() throws IOException {
		return get(recordedURLString);
	}
	
	/**
	 * Gets the recorded page.
	 *
	 * @param page the page
	 * @return the recorded page
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String getRecordedPage(int page) throws IOException {
		return get(recordedURLString,page);
	}

	/**
	 * Gets the raw source code for the placed calls page.
	 * 
	 * @return the placed calls source code
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public String getPlaced() throws IOException {
		return get(placedURLString);
	}
	
	/**
	 * Gets the placed page.
	 *
	 * @param page the page
	 * @return the placed page
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String getPlacedPage(int page) throws IOException {
		return get(placedURLString,page);
	}

	/**
	 * Gets the received calls source code.
	 * 
	 * @return the received
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public String getReceived() throws IOException {
		return get(receivedURLString);
	}
	
	/**
	 * Gets the received page.
	 *
	 * @param page the page
	 * @return the received page
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String getReceivedPage(int page) throws IOException {
		return get(receivedURLString,page);
	}

	/**
	 * Gets the missed calls source code.
	 * 
	 * @return the missed
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public String getMissed() throws IOException {
		return get(missedURLString);
	}
	
	/**
	 * Gets the missed page.
	 *
	 * @param page the page
	 * @return the missed page
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String getMissedPage(int page) throws IOException {
		return get(missedURLString,page);
	}

	
	/**
	 * Gets the unread sms.
	 *
	 * @return the unread sms
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String getUnreadSMS() throws IOException{
		return get(unreadSMSString);
	}
	
	/**
	 * Gets the unread sms page.
	 *
	 * @param page the page
	 * @return the unread sms page
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String getUnreadSMSPage(int page) throws IOException{
		return get(unreadSMSString,page);
	}
	
	
	
	
	
	
	
	
  /**
	 * Gets the Voicemail page raw source code.
	 *
	 * @return the Voicemail
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public String getVoicemail() throws IOException {
		return get(voicemailURLString);
	}

	/**
	 * Gets the voicemail page.
	 *
	 * @param page the page
	 * @return the voicemail page
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String getVoicemailPage(int page) throws IOException {
		return get(voicemailURLString,page);
	}

  /**
   * Downloads a voicemail.
   *
   * @param msgID the msg id
   * @return byte output stream
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public ByteArrayOutputStream downloadVoicemail(String msgID) throws IOException
  {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    try
    {
      URL u = new URL (voicemailDownloadURLString + msgID);
      HttpURLConnection huc = (HttpURLConnection)u.openConnection () ;
      huc.setRequestProperty("Authorization", "GoogleLogin auth="+authToken);
      huc.setRequestProperty("User-agent", USER_AGENT);
      huc.setRequestMethod ("GET");
      huc.connect() ;
      InputStream is = huc.getInputStream();

      if(huc.getResponseCode() == HttpURLConnection.HTTP_OK)
      {
        byte[] buffer = new byte [4096];
        int bytes = 0;

        while(true)
        {
          bytes = is.read(buffer);
          if(bytes <= 0)
            break;
          outputStream.write(buffer, 0, bytes);
        }

        outputStream.flush();
      }
               
      huc.disconnect ();

      return outputStream;
    }
    catch(IOException e)
    {
      System.out.println ( "Exception\n" + e ) ;
    }

    return null;
  }

	/**
	 * Gets the SMS page raw source code.
	 * 
	 * @return the sMS
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public String getSMS() throws IOException {
		return get(smsURLString);
	}

	
	
	/**
	 * Gets the SMS page.
	 *
	 * @param page the page
	 * @return the sMS page
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String getSMSPage(int page) throws IOException {
		return get(smsURLString,page);
	}
	
	
	
	/**
	 * Gets a collection of SMS threads. Each SMS thread has a collection of SMS
	 * objects which contains contact, text and timestamp information.
	 *
	 * @return a collection of SMS threads.
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public Collection<SMSThread> getSMSThreads() throws IOException {
		SMSParser parser = new SMSParser(get(smsURLString), phoneNumber);
		return parser.getSMSThreads();
	}
	
	/**
	 * Gets a collection of SMS threads. Each SMS thread has a collection of SMS
	 * objects which contains contact, text and timestamp information.
	 *
	 * @return a collection of SMS threads.
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public Collection<SMSThread> getSMSThreads(int page ) throws IOException {
		SMSParser parser = new SMSParser(get(smsURLString, page), phoneNumber);
		return parser.getSMSThreads();
	}
	
	
	/**
	 * Gets the SMS threads from a given Response Page.
	 *
	 * @param response the response
	 * @return the SMS threads
	 */
	public Collection<SMSThread> getSMSThreads(String response){
		SMSParser parser= new SMSParser(response, phoneNumber);
		return parser.getSMSThreads();
	}
	
	
	/**
	 * Gets the rNRSEE.
	 *
	 * @return the rNRSEE
	 */
	public String getRNRSEE(){
		return rnrSEE;
	}
	
	/**
	 * Internal method which parses the Homepage source code to determine the
	 * rnrsee variable, this variable is passed into most fuctions for placing
	 * calls and sms.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private void setRNRSEE() throws IOException {
		if (general != null) {
			if(general.contains("'_rnr_se': '")) {
				String p1 = general.split("'_rnr_se': '", 2)[1];
				rnrSEE = p1.split("',", 2)[0];
				if(PRINT_TO_CONSOLE)
					System.out.println("Successfully Received rnr_se.");
				p1 = null;
			} else if(general.contains("<div class=\"gc-notice\">")) {
				String gcNotice = ParsingUtil.removeUninterestingParts(general, "<div class=\"gc-notice\">", "</div>", false);	
				System.out.println(gcNotice+ "(Answer did not contain rnr_se)");
				throw new IOException(gcNotice + "(Answer did not contain rnr_se)");
			} else {
				System.out.println("Answer did not contain rnr_se! "+ general);
				throw new IOException("Answer did not contain rnr_se! "+ general);
			}
		} else {
			System.out.println("setRNRSEE(): Answer was null!");
			throw new IOException("setRNRSEE(): Answer was null!");
		}
	}

  /**
   * Gets the phone number.
   *
   * @return the phone number
   */
  public String getPhoneNumber()
  {
    return this.phoneNumber;
  }

	//TODO Combine with or replace setPhoneInfo
	/**
	 * Gets the raw phones info.
	 *
	 * @return the raw phones info
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String getRawPhonesInfo() throws IOException{
		return get(phonesInfoURLString);
	}
	
	/**
	 * Place a call.
	 * 
	 * @param originNumber
	 *            the origin number
	 * @param destinationNumber
	 *            the destination number
	 * @param phoneType
	 *            the phone type, this is a number such as 1,2,7 formatted as a String
	 * @return the raw response string received from Google Voice.
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public String call(String originNumber, String destinationNumber,
			String phoneType) throws IOException {
		String out = "";
		StringBuffer calldata = new StringBuffer();
		
		
		// POST /voice/call/connect/ 
		// outgoingNumber=[number to call]
		// &forwardingNumber=[forwarding number]
		// &subscriberNumber=undefined
		// &phoneType=[phone type from google]
		// &remember=0
		// &_rnr_se=[pull from page]
		
		calldata.append("outgoingNumber=");
		calldata.append(URLEncoder.encode(destinationNumber, enc));
		calldata.append("&forwardingNumber=");
		calldata.append(URLEncoder.encode(originNumber, enc));
		calldata.append("&subscriberNumber=undefined");
		calldata.append("&phoneType=");
		calldata.append(URLEncoder.encode(phoneType, enc));
		calldata.append("&remember=0");
		calldata.append("&_rnr_se=");
		calldata.append(URLEncoder.encode(rnrSEE, enc));
		
		
		URL callURL = new URL("https://www.google.com/voice/b/0/call/connect/");

		URLConnection callconn = callURL.openConnection();
		callconn.setRequestProperty("Authorization","GoogleLogin auth="+authToken);
		callconn.setRequestProperty("User-agent",USER_AGENT);

		callconn.setDoOutput(true);
		OutputStreamWriter callwr = new OutputStreamWriter(callconn
				.getOutputStream());

		callwr.write(calldata.toString());
		callwr.flush();

		BufferedReader callrd = new BufferedReader(new InputStreamReader(
				callconn.getInputStream()));

		String line;
		while ((line = callrd.readLine()) != null) {
			out += line + "\n\r";

		}

		callwr.close();
		callrd.close();

		if (out.equals("")) {
			throw new IOException("No Response Data Received.");
		}

		return out;

	}

	/**
	 * Cancel a call that was just placed.
	 * 
	 * @param originNumber
	 *            the origin number
	 * @param destinationNumber
	 *            the destination number
	 * @param phoneType
	 *            the phone type
	 * @return the string
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public String cancelCall(String originNumber, String destinationNumber,
			String phoneType) throws IOException {
		String out = "";
		String calldata = "";
		calldata += URLEncoder.encode("outgoingNumber", enc) + "="
				+ URLEncoder.encode("undefined", enc);
		calldata += "&" + URLEncoder.encode("forwardingNumber", enc) + "="
				+ URLEncoder.encode("undefined", enc);

		calldata += "&" + URLEncoder.encode("cancelType", enc) + "="
				+ URLEncoder.encode("C2C", enc);
		calldata += "&" + URLEncoder.encode("_rnr_se", enc) + "="
				+ URLEncoder.encode(rnrSEE, enc);
		// POST /voice/call/connect/ outgoingNumber=[number to
		// call]&forwardingNumber=[forwarding
		// number]&subscriberNumber=undefined&remember=0&_rnr_se=[pull from
		// page]
		URL callURL = new URL("https://www.google.com/voice/b/0/call/cancel/");

		URLConnection callconn = callURL.openConnection();
		callconn.setRequestProperty( "Authorization",
                "GoogleLogin auth="+authToken );
		callconn
				.setRequestProperty(
						"User-agent",
						USER_AGENT);

		callconn.setDoOutput(true);
		OutputStreamWriter callwr = new OutputStreamWriter(callconn
				.getOutputStream());
		callwr.write(calldata);
		callwr.flush();

		BufferedReader callrd = new BufferedReader(new InputStreamReader(
				callconn.getInputStream()));

		String line;
		while ((line = callrd.readLine()) != null) {
			out += line + "\n\r";

		}

		callwr.close();
		callrd.close();

		if (out.equals("")) {
			throw new IOException("No Response Data Received.");
		}

		return out;

	}
	
	/**
	 * Mark a Conversation with a known Message ID as read.
	 *
	 * @param msgID the msg id
	 * @return the string
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String markAsRead(String msgID) throws IOException
  {
    String out = "";
		StringBuffer calldata = new StringBuffer();

		
        // POST /voice/inbox/mark/ 
        // messages=[messageID]
        // &read=1
        // &_rnr_se=[pull from page]
		
		calldata.append("messages=");
		calldata.append(URLEncoder.encode(msgID, enc));
		calldata.append("&read=1");
		calldata.append("&_rnr_se=");
		calldata.append(URLEncoder.encode(rnrSEE, enc));


		URL callURL = new URL(markAsReadString);

		URLConnection callconn = callURL.openConnection();
		callconn.setRequestProperty("Authorization","GoogleLogin auth="+authToken);
		callconn.setRequestProperty("User-agent",USER_AGENT);

		callconn.setDoOutput(true);
		OutputStreamWriter callwr = new OutputStreamWriter(callconn
				.getOutputStream());

		callwr.write(calldata.toString());
		callwr.flush();

		BufferedReader callrd = new BufferedReader(new InputStreamReader(
				callconn.getInputStream()));

		String line;
		while ((line = callrd.readLine()) != null) {
			out += line + "\n\r";

		}

		callwr.close();
		callrd.close();

		if (out.equals("")) {
			throw new IOException("No Response Data Received.");
		}

		return out;
  }
	
	/**
	 * Mark a Conversation with a known Message ID as unread.
	 *
	 * @param msgID the msg id
	 * @return the string
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String markUnRead(String msgID) throws IOException
	{
	    String out = "";
	        StringBuffer calldata = new StringBuffer();
	
	
	        // POST /voice/inbox/mark/ 
	        // messages=[messageID]
	        // &read=0
	        // &_rnr_se=[pull from page]
	
	        calldata.append("messages=");
	        calldata.append(URLEncoder.encode(msgID, enc));
	        calldata.append("&read=0");
	        calldata.append("&_rnr_se=");
	        calldata.append(URLEncoder.encode(rnrSEE, enc));
	
	
	        URL callURL = new URL("https://www.google.com/voice/b/0/inbox/mark");
	
	        URLConnection callconn = callURL.openConnection();
	        callconn.setRequestProperty("Authorization","GoogleLogin auth="+authToken);
	        callconn.setRequestProperty("User-agent",USER_AGENT);
	
	        callconn.setDoOutput(true);
	        OutputStreamWriter callwr = new OutputStreamWriter(callconn
	                .getOutputStream());
	
	        callwr.write(calldata.toString());
	        callwr.flush();
	
	        BufferedReader callrd = new BufferedReader(new InputStreamReader(
	                callconn.getInputStream()));
	
	        String line;
	        while ((line = callrd.readLine()) != null) {
	            out += line + "\n\r";
	
	        }
	
	        callwr.close();
	        callrd.close();
	
	        if (out.equals("")) {
	            throw new IOException("No Response Data Received.");
	        }
	
	        return out;
	}

	/**
	 * Delete message.
	 *
	 * @param msgID the msg id
	 * @return the string
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String deleteMessage(String msgID) throws IOException
  {
    String out = "";
		StringBuffer calldata = new StringBuffer();


		// POST /voice/inbox/deleteMessages/
		// messages=[messageID]
		// &trash=1
		// &_rnr_se=[pull from page]

		calldata.append("messages=");
		calldata.append(URLEncoder.encode(msgID, enc));
		calldata.append("&trash=1");
		calldata.append("&_rnr_se=");
		calldata.append(URLEncoder.encode(rnrSEE, enc));


		URL callURL = new URL("https://www.google.com/voice/b/0/inbox/deleteMessages/");

		URLConnection callconn = callURL.openConnection();
		callconn.setRequestProperty("Authorization","GoogleLogin auth="+authToken);
		callconn.setRequestProperty("User-agent",USER_AGENT);

		callconn.setDoOutput(true);
		OutputStreamWriter callwr = new OutputStreamWriter(callconn
				.getOutputStream());

		callwr.write(calldata.toString());
		callwr.flush();

		BufferedReader callrd = new BufferedReader(new InputStreamReader(
				callconn.getInputStream()));

		String line;
		while ((line = callrd.readLine()) != null) {
			out += line + "\n\r";

		}

		callwr.close();
		callrd.close();

		if (out.equals("")) {
			throw new IOException("No Response Data Received.");
		}

		return out;
  }
	
	/**
	 * Enables multiple phones in one post
	 * 
	 * TODO Test this with multiple phones in an account
	 * Best would be to be able to construct a url which can switch multiple phones at a time.
	 *
	 * @param IDs Array of Phones to enable
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void phonesEnable(int[] IDs) throws IOException {

		if(IDs.length<1) {
			return;
		} else if(IDs.length==1) {
			//launch single (no thread overhead)	
			phoneEnable(IDs[0]);
		} else {
			for (int i = 0; i < IDs.length; i++) {
				//TODO spawn threads!
				int j = IDs[i];
				String paraString = URLEncoder.encode("enabled", enc) + "="
						+ URLEncoder.encode("1", enc);
				paraString += "&" + URLEncoder.encode("phoneId", enc) + "="
						+ URLEncoder.encode(Integer.toString(j), enc);
				paraString += "&" + URLEncoder.encode("_rnr_se", enc) + "="
					+ URLEncoder.encode(rnrSEE, enc);
			
				phonesEnableDisableApply(paraString);
			}
		}
		
	}
	
	/**
	 * Enables one of the the phones attached to the account from ringing.
	 * Requires the internal ID for that phone, as an integer, usually 1,2,3,
	 * etc.
	 * 
	 * @param ID
	 *            the iD
	 * @return the raw response of the enable action.
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public String phoneEnable(int ID) throws IOException {
		String paraString = URLEncoder.encode("enabled", enc) + "="
				+ URLEncoder.encode("1", enc);
		paraString += "&" + URLEncoder.encode("phoneId", enc) + "="
				+ URLEncoder.encode(Integer.toString(ID), enc);
		paraString += "&" + URLEncoder.encode("_rnr_se", enc) + "="
				+ URLEncoder.encode(rnrSEE, enc);
		return phonesEnableDisableApply(paraString);
	}
	
	/**
	 * Disables multiple phones in one post
	 * 
	 * TODO Test this with multiple phones in an account
	 * Make faster - spawn threads
	 * Best would be to be able to construct a url which can switch multiple phones at a time.
	 *
	 * @param IDs Array of Phones to disable
	 * @return the raw response of the disable action.
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void phonesDisable(int[] IDs) throws IOException {
		
		if(IDs.length<1) {
			return;
		} else if(IDs.length==1) {
			//launch single (no thread overhead)	
			phoneDisable(IDs[0]);
		} else {
			for (int i = 0; i < IDs.length; i++) {
				//TODO spawn threads!
				int j = IDs[i];
				String paraString = URLEncoder.encode("enabled", enc) + "="
						+ URLEncoder.encode("0", enc);
				paraString += "&" + URLEncoder.encode("phoneId", enc) + "="
						+ URLEncoder.encode(Integer.toString(j), enc);
				paraString += "&" + URLEncoder.encode("_rnr_se", enc) + "="
					+ URLEncoder.encode(rnrSEE, enc);
			
				phonesEnableDisableApply(paraString);
			}
		}

	}

	/**
	 * Disable one of the the phones attached to the account from ringing.
	 * Requires the internal ID for that phone, as an integer, usually 1,2,3,
	 * etc.
	 * 
	 * @param ID
	 *            the iD
	 * @return the raw response of the disable action.
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public String phoneDisable(int ID) throws IOException {
		String paraString = URLEncoder.encode("enabled", enc) + "="
				+ URLEncoder.encode("0", enc);
		paraString += "&" + URLEncoder.encode("phoneId", enc) + "="
				+ URLEncoder.encode(Integer.toString(ID), enc);
		paraString += "&" + URLEncoder.encode("_rnr_se", enc) + "="
				+ URLEncoder.encode(rnrSEE, enc);
		return phonesEnableDisableApply(paraString);
	}

	/**
	 * Executes the enable/disable action with the provided url params.
	 *
	 * @param paraString the URL Parameters (encoded), ie ?auth=3248sdf7234&enable=0&phoneId=1&enable=1&phoneId=2&_rnr_se=734682ghdsf
	 * @return the raw response of the disable action.
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private String phonesEnableDisableApply(String paraString) throws IOException {
		String out = "";

		
		// POST /voice/call/connect/ outgoingNumber=[number to
		// call]&forwardingNumber=[forwarding
		// number]&subscriberNumber=undefined&remember=0&_rnr_se=[pull from
		// page]

		//
		if (PRINT_TO_CONSOLE) System.out.println(phoneEnableURLString);
		if (PRINT_TO_CONSOLE) System.out.println(paraString);
		URL requestURL = new URL(phoneEnableURLString);

		URLConnection conn = requestURL.openConnection();
		conn.setRequestProperty( "Authorization",
                "GoogleLogin auth="+authToken );
		conn
				.setRequestProperty(
						"User-agent",
						USER_AGENT);

		conn.setDoOutput(true);
		conn.setDoInput(true);

		OutputStreamWriter callwr = new OutputStreamWriter(conn
				.getOutputStream());
		callwr.write(paraString);
		callwr.flush();

		BufferedReader callrd = new BufferedReader(new InputStreamReader(
				conn.getInputStream()));

		String line;
		while ((line = callrd.readLine()) != null) {
			out += line + "\n\r";

		}

		callwr.close();
		callrd.close();

		if (out.equals("")) {
			throw new IOException("No Response Data Received.");
		}

		return out;

	}
	
	/**
	 * Enables/disables the call Announcement setting (general for all phones).
	 *
	 * @param announceCaller <br/>
	 * true Announces caller's name and gives answering options <br/>
	 * false Directly connects calls when phones are answered
	 * @return the raw response of the disable action.
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String setCallPresentation(boolean announceCaller) throws IOException {
		String out = "";

		URL requestURL = new URL(generalSettingsURLString);
		/** 0 for enable, 1 for disable **/
		String announceCallerStr="";

		if(announceCaller) {
			announceCallerStr = "0";
			if (PRINT_TO_CONSOLE) System.out.println("Turning caller announcement on.");
		}
		else {
			announceCallerStr = "1";
			if (PRINT_TO_CONSOLE) System.out.println("Turning caller announcement off.");
		}
		
		String paraString = "";
		paraString += URLEncoder.encode("directConnect", enc) + "="
				+ URLEncoder.encode(announceCallerStr, enc);
		paraString += "&" + URLEncoder.encode("_rnr_se", enc) + "="
				+ URLEncoder.encode(rnrSEE, enc);


		URLConnection conn = requestURL.openConnection();
		conn.setRequestProperty( "Authorization",
                "GoogleLogin auth="+authToken );
		conn.setRequestProperty("User-agent",
								USER_AGENT);

		conn.setDoOutput(true);
		conn.setDoInput(true);

		OutputStreamWriter callwr = new OutputStreamWriter(conn.getOutputStream());
		callwr.write(paraString);
		callwr.flush();

		BufferedReader callrd = new BufferedReader(new InputStreamReader(conn.getInputStream()));

		String line;
		while ((line = callrd.readLine()) != null) {
			out += line + "\n\r";
		}

		callwr.close();
		callrd.close();

		if (out.equals("")) {
			throw new IOException("No Response Data Received.");
		}

		return out;
	}
	
	/**
	 * This is the general voicemail greeting callers hear.
	 *
	 * @param greetingToSet <br/>
	 * number of the greeting to choose
	 * @return the raw response of the disable action.
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String setVoicemailGreetingId(String greetingToSet) throws IOException {

		URL requestURL = new URL(generalSettingsURLString);

		if (PRINT_TO_CONSOLE) System.out.println("Activating Greeting#"+greetingToSet);

		String paraString = "";
		// URLEncoder.encode("auth", enc) + "="+ URLEncoder.encode(authToken, enc);
		paraString += URLEncoder.encode("greetingId", enc) + "="
				+ URLEncoder.encode(greetingToSet+"", enc);
		paraString += "&" + URLEncoder.encode("_rnr_se", enc) + "="
				+ URLEncoder.encode(rnrSEE, enc);


		return postSettings(requestURL, paraString);
	}
	
	/**
	 * Activated or deactivated the Do Not disturb function.<br>
	 * Enable this to send to voicemail all calls made to your Google number.
	 *
	 * @param dndEnabled true to enable dnd, false to disable it
	 * @return the string
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String setDoNotDisturb(boolean dndEnabled) throws IOException {

		URL requestURL = new URL(generalSettingsURLString);
		
		String enabled;

		if(dndEnabled) {
			if (PRINT_TO_CONSOLE) System.out.println("Enabling dnd");
			enabled = "1";
		} else {
			if (PRINT_TO_CONSOLE) System.out.println("Disabling dnd");
			enabled = "0";
		}

		String paraString = "";
			// URLEncoder.encode("auth", enc) + "="+ URLEncoder.encode(authToken, enc);
		paraString += URLEncoder.encode("doNotDisturb", enc) + "="
				+ URLEncoder.encode(enabled+"", enc);
		paraString += "&" + URLEncoder.encode("_rnr_se", enc) + "="
				+ URLEncoder.encode(rnrSEE, enc);


		return postSettings(requestURL, paraString);
	}
	
	/**
	 * Activated or deactivated the SMS Forwarding for a particular phone
	 *
	 * @param smsEnable true to enable sms forwarding, false to disable it
	 * @param ID The id of the phone to enable/disable
	 * @return the string
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String setSmsEnabled(boolean smsEnable, int ID) throws IOException {

		// only allow editing of type 2 phones
		for (int i = 0; i < settings.getPhones().length; i++) {
			Phone ph = settings.getPhones()[i];
			if(ph.getId() == ID) {
				if(ph.getType() != 2) {
					if (PRINT_TO_CONSOLE) System.out.println("Cannot change sms Enabled on phone of type "+ph.getType() + " only availible on type 2");
					return null;
				}
			}
		}

		String enabled;

		if(smsEnable &! settings.isPhoneSmsEnabled(ID)) {
			if (PRINT_TO_CONSOLE) System.out.println("Enabling sms for phone "+ID);
			enabled = "1";
		} else if(settings.isPhoneSmsEnabled(ID)) {
			if (PRINT_TO_CONSOLE) System.out.println("Disabling sms for phone "+ID);
			enabled = "0";
		} else {
			// do not make changes to phones that are already in the same state
			if (PRINT_TO_CONSOLE) System.out.println("Phone "+ID + " is already in the requested state. "+smsEnable);
			return null;
		}
		
		URL requestURL = new URL(editForwardingSMSURLString);

		String paraString = "";
		paraString += URLEncoder.encode("enabled", enc) + "="
				+ URLEncoder.encode(enabled+"", enc);
		paraString += "&" + URLEncoder.encode("phoneId", enc) + "="
				+ URLEncoder.encode(ID+"", enc);
		paraString += "&" + URLEncoder.encode("_rnr_se", enc) + "="
				+ URLEncoder.encode(rnrSEE, enc);
		
		if (PRINT_TO_CONSOLE) System.out.println(requestURL);
		if (PRINT_TO_CONSOLE) System.out.println(paraString);
		
		return postSettings(requestURL, paraString);

	}
	
	/**
	 * Applies the settings for this group.
	 *
	 * @param group the group
	 * @return the string
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String setNewGroupSettings(Group group) throws IOException {
		URL requestURL = new URL(groupsSettingsURLString);

		String paraString = "";
		// URLEncoder.encode("auth", enc) + "="+ URLEncoder.encode(authToken, enc);;
		
		// 1=true 0=false 
		int isCustomGreeting = 0;
		if(group.isCustomGreeting()) {
			isCustomGreeting = 1;
		}
		paraString += URLEncoder.encode("isCustomGreeting", enc) + "="
			+ URLEncoder.encode(isCustomGreeting+"", enc);
		
		int greetingId = group.getGreetingId();
		paraString += "&" + URLEncoder.encode("greetingId", enc) + "="
			+ URLEncoder.encode(greetingId+"", enc);
		
		for (int i = 0; i < group.getDisabledForwardingIds().size(); i++) {
			paraString += "&" + URLEncoder.encode("disabledPhoneIds", enc) + "="
				+ URLEncoder.encode(group.getDisabledForwardingIds().get(i).getId(), enc);
		}
		
		int directConnect = 0;
		if(group.isDirectConnect()) {
			directConnect = 1;
		}
		paraString += "&" + URLEncoder.encode("directConnect", enc) + "="
			+ URLEncoder.encode(directConnect+"", enc);
		
		int isCustomDirectConnect = 0;
		if(group.isCustomDirectConnect()) {
			isCustomDirectConnect = 1;
		}
		paraString += "&" + URLEncoder.encode("isCustomDirectConnect", enc) + "="
			+ URLEncoder.encode(isCustomDirectConnect+"", enc);
		
		int isCustomForwarding = 0;
		if(group.isCustomForwarding()) {
			isCustomForwarding = 1;
		}
		paraString += "&" + URLEncoder.encode("isCustomForwarding", enc) + "="
			+ URLEncoder.encode(isCustomForwarding+"", enc);
		
		paraString += "&" + URLEncoder.encode("id", enc) + "="
			+ URLEncoder.encode(group.getId(), enc);
		
		paraString += "&" + URLEncoder.encode("_rnr_se", enc) + "="
				+ URLEncoder.encode(rnrSEE, enc);

		return postSettings(requestURL, paraString);
	}

	/**
	 * Posts a settings change.
	 *
	 * @param requestURL the request url
	 * @param paraString the para string
	 * @return the string
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private String postSettings(URL requestURL, String paraString)
			throws IOException {
		String out = "";
		HttpURLConnection conn = (HttpURLConnection) requestURL.openConnection();
		conn.setRequestProperty( "Authorization",
                "GoogleLogin auth="+authToken );
		conn.setRequestProperty("User-agent",
								USER_AGENT);

		conn.setDoOutput(true);
		conn.setDoInput(true);

		OutputStreamWriter callwr = new OutputStreamWriter(conn.getOutputStream());
		callwr.write(paraString);
		callwr.flush();

		// Get the response
		conn.connect();
		int responseCode = conn.getResponseCode();
		if(PRINT_TO_CONSOLE)
			System.out.println(requestURL + " - " + conn.getResponseMessage());
		InputStream is;
		if(responseCode==200) {
			is = conn.getInputStream();
		} else {
			is = conn.getErrorStream();
		}
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader callrd = new BufferedReader(isr);
		
		String line;
		while ((line = callrd.readLine()) != null) {
			out += line + "\n\r";
		}

		callwr.close();
		callrd.close();

		if (out.equals("")) {
			throw new IOException("No Response Data Received.");
		}
		
		if(PRINT_TO_CONSOLE) System.out.println(out);

		return out;
	}

	/**
	 * Send an SMS.
	 *
	 * @param destinationNumber the destination number
	 * @param txt the Text of the message. Messages longer than the allowed
	 * character length will be split into multiple messages.
	 * @param id the Text of the message. Messages longer than the allowed
	 * character length will be split into multiple messages.
	 * @return the string
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String sendSMS(String destinationNumber, String txt, String id)
			throws IOException {
		String out = "";
		String smsdata = "";
		smsdata += URLEncoder.encode("id", enc) + "="
				+ URLEncoder.encode(id, enc);
		smsdata += "&" +URLEncoder.encode("phoneNumber", enc) + "="
				+ URLEncoder.encode(destinationNumber, enc);
		smsdata += "&" +URLEncoder.encode("conversationId", enc) + "="
				+ URLEncoder.encode(id, enc);
		smsdata += "&" + URLEncoder.encode("text", enc) + "="
				+ URLEncoder.encode(txt, enc);
		smsdata += "&" + URLEncoder.encode("_rnr_se", enc) + "="
				+ URLEncoder.encode(rnrSEE, enc);
		System.out.println("smsdata: "+smsdata);
		
		URL smsurl = new URL("https://www.google.com/voice/b/0/sms/send/");

		URLConnection smsconn = smsurl.openConnection();
		smsconn.setRequestProperty( "Authorization",
                "GoogleLogin auth="+authToken );
		smsconn
				.setRequestProperty(
						"User-agent",
						USER_AGENT);

		smsconn.setDoOutput(true);
		OutputStreamWriter callwr = new OutputStreamWriter(smsconn.getOutputStream());
		callwr.write(smsdata);
		callwr.flush();

		BufferedReader callrd = new BufferedReader(new InputStreamReader(
				smsconn.getInputStream()));

		String line;
		while ((line = callrd.readLine()) != null) {
			out += line + "\n\r";

		}

		callwr.close();
		callrd.close();

		if (out.equals("")) {
			throw new IOException("No Response Data Received.");
		}

		return out;
	}
	
	
	/**
	 * Send an SMS.
	 *
	 * @param destinationNumber the destination number
	 * @param txt the Text of the message. Messages longer than the allowed
	 * character length will be split into multiple messages.
	 * @return the string
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String sendSMS(String destinationNumber, String txt, SMSThread thread)
			throws IOException {
		
		String id = thread.getId();
		return sendSMS(destinationNumber,txt,id);
	}
	
	
	
	/**
	 * Send an SMS.
	 *
	 * @param destinationNumber the destination number
	 * @param txt the Text of the message. Messages longer than the allowed
	 * character length will be split into multiple messages.
	 * @return the string
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String sendSMS(String destinationNumber, String txt)
			throws IOException {
		String out = "";
		String smsdata = "";

		smsdata += URLEncoder.encode("phoneNumber", enc) + "="
				+ URLEncoder.encode(destinationNumber, enc);
		smsdata += "&" + URLEncoder.encode("text", enc) + "="
				+ URLEncoder.encode(txt, enc);
		smsdata += "&" + URLEncoder.encode("_rnr_se", enc) + "="
				+ URLEncoder.encode(rnrSEE, enc);
		URL smsurl = new URL("https://www.google.com/voice/b/0/sms/send/");

		URLConnection smsconn = smsurl.openConnection();
		smsconn.setRequestProperty( "Authorization",
                "GoogleLogin auth="+authToken );
		smsconn
				.setRequestProperty(
						"User-agent",
						USER_AGENT);

		smsconn.setDoOutput(true);
		OutputStreamWriter callwr = new OutputStreamWriter(smsconn.getOutputStream());
		callwr.write(smsdata);
		callwr.flush();

		BufferedReader callrd = new BufferedReader(new InputStreamReader(
				smsconn.getInputStream()));

		String line;
		while ((line = callrd.readLine()) != null) {
			out += line + "\n\r";

		}

		callwr.close();
		callrd.close();

		if (out.equals("")) {
			throw new IOException("No Response Data Received.");
		}

		return out;
	}
	

	/**
	 * HTTP GET request for a given URL String.
	 * 
	 * @param urlString
	 *            the url string
	 * @return the string
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	String get(String urlString) throws IOException {
		URL url = new URL(urlString);
		//+ "?auth=" + URLEncoder.encode(authToken, enc));

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestProperty( "Authorization",
                "GoogleLogin auth="+authToken );
		conn.setRequestProperty(
						"User-agent",
						USER_AGENT);
		conn.setInstanceFollowRedirects(false); // will follow redirects of same protocol http to http, but does not follow from http to https for example if set to true

		// Get the response
		conn.connect();
		int responseCode = conn.getResponseCode();
		if(PRINT_TO_CONSOLE)
			System.out.println(urlString + " - " + conn.getResponseMessage());
		InputStream is;
		if(responseCode==200) {
			is = conn.getInputStream();
		} else if(responseCode==HttpURLConnection.HTTP_MOVED_PERM || responseCode==HttpURLConnection.HTTP_MOVED_TEMP || responseCode==HttpURLConnection.HTTP_SEE_OTHER || responseCode==307) {
			redirectCounter++;
			if(redirectCounter > MAX_REDIRECTS) {
				redirectCounter = 0;
				throw new IOException(urlString + " : " + conn.getResponseMessage() + "("+responseCode+") : Too manny redirects. exiting.");
			}
			String location = conn.getHeaderField("Location");
			if(location!=null && !location.equals("")) {
				System.out.println(urlString + " - " + responseCode + " - new URL: " + location);
				return get(location);
			} else {
				throw new IOException(urlString + " : " + conn.getResponseMessage() + "("+responseCode+") : Received moved answer but no Location. exiting.");
			}
		} else {
			is = conn.getErrorStream();
		}
		redirectCounter = 0;
		
		if(is==null) {
			throw new IOException(urlString + " : " + conn.getResponseMessage() + "("+responseCode+") : InputStream was null : exiting.");
		}
		
		String result="";
		try {
			// Get the response
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			
			StringBuffer sb = new StringBuffer();
			String line;
			while ((line = rd.readLine()) != null) {
				sb.append(line + "\n\r");
			}
			rd.close();
			result = sb.toString();
		} catch (Exception e) {
			throw new IOException(urlString + " - " + conn.getResponseMessage() + "("+responseCode+") - " +e.getLocalizedMessage());
		}
		return result;
	}
	
	
	/**
	 * HTTP GET request for a given URL String and a given page number.
	 *
	 * @param urlString the url string
	 * @param page number must be a natural number
	 * @return the string
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String get(String urlString,int page) throws IOException {
		URL url = new URL(urlString + "?page=p"+page);
		//url+="&page="+page;
		URLConnection conn = url.openConnection();
		conn.setRequestProperty( "Authorization",
                "GoogleLogin auth="+authToken );
		conn
				.setRequestProperty(
						"User-agent",
						USER_AGENT);

		// Get the response
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn
				.getInputStream()));
		StringBuffer sb = new StringBuffer();
		String line;
		while ((line = rd.readLine()) != null) {
			sb.append(line + "\n\r");
		}
		rd.close();
		String result = sb.toString();

		return result;
	}
	
	/**
	 * Login Method to refresh authentication with Google Voice.
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void login()  throws IOException {
		login(null,null);
	}
	
	
	
	/**
	 * Use this login method to login - use captchaAnswer to answer a captcha challenge
	 * @param pCaptchaAnswer (optional) String entered by the user as an answer to a CAPTCHA challenge. - null to make a normal login attempt
	 * @param pCaptchaToken (optional) token which matches the response/url from the captcha challenge
	 * @throws IOException if login encounters a connection error
	 */
	public void login(String pCaptchaAnswer, String pCaptchaToken) throws IOException {

		String data = URLEncoder.encode("accountType", enc) + "="
				+ URLEncoder.encode(account_type, enc);
		data += "&" + URLEncoder.encode("Email", enc) + "="
				+ URLEncoder.encode(user, enc);
		data += "&" + URLEncoder.encode("Passwd", enc) + "="
				+ URLEncoder.encode(pass, enc);
		data += "&" + URLEncoder.encode("service", enc) + "="
				+ URLEncoder.encode(SERVICE, enc);
		data += "&" + URLEncoder.encode("source", enc) + "="
				+ URLEncoder.encode(source, enc);
		if(pCaptchaAnswer!=null && pCaptchaToken!=null) {
			data += "&" + URLEncoder.encode("logintoken", enc) + "="
					+ URLEncoder.encode(pCaptchaToken, enc);
			data += "&" + URLEncoder.encode("logincaptcha", enc) + "="
					+ URLEncoder.encode(pCaptchaAnswer, enc);
		}

		// Send data
		URL url = new URL(loginURLString);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn
		.setRequestProperty(
				"User-agent",
				USER_AGENT);
		
		conn.setDoOutput(true);
		OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
		wr.write(data);
		wr.flush();

		// Get the response
		conn.connect();
		int responseCode = conn.getResponseCode();
		if(PRINT_TO_CONSOLE)
			System.out.println(loginURLString + " - " + conn.getResponseMessage());
		InputStream is;
		if(responseCode==200) {
			is = conn.getInputStream();
		} else {
			is = conn.getErrorStream();
		}
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader rd = new BufferedReader(isr);
		String line;
		String completelineDebug="";
		
		/*
		 * A failure response contains an error code and a URL to an error page that can be displayed to the user. 
		 * If the error code is a CAPTCHA challenge, the response also includes a URL to a CAPTCHA image and a special 
		 * token. Your application should be able to solicit an answer from the user and then retry the login request. 
		 * To display the CAPTCHA image to the user, prefix the CaptchaUrl value with "http://www.google.com/accounts/", 
		 * for example: " http://www.google.com/accounts/Captcha?ctoken=HiteT4b0Bk5Xg18_AcVoP6-yFkHPibe7O9EqxeiI7lUSN".
		 */
		String lErrorString = "Unknown Connection Error."; // ex: Error=CaptchaRequired

		// String AuthToken = null;
		while ((line = rd.readLine()) != null) {
			completelineDebug += line+"\n";
			if (line.contains("Auth=")) {
				this.authToken = line.split("=", 2)[1].trim();
				if (PRINT_TO_CONSOLE){
					System.out.println("Logged in to Google - Auth token received");
				}
			} else if (line.contains("Error=")) {
				lErrorString = line.split("=", 2)[1].trim();
				//error = getErrorEnumByCode(lErrorString);
				error = ERROR_CODE.valueOf(lErrorString);
				if (PRINT_TO_CONSOLE)
					System.out.println("Login error - "+lErrorString);
				
				
			}
			if (line.contains("CaptchaToken=")) {
				captchaToken = line.split("=", 2)[1].trim();
			} 
			
			if (line.contains("CaptchaUrl=")) {
				captchaUrl = "http://www.google.com/accounts/" + line.split("=", 2)[1].trim();
			}
			if (line.contains("Url=")) {
				captchaUrl2 = line.split("=", 2)[1].trim();
			}
			

		}
		wr.close();
		rd.close();

//		if (PRINT_TO_CONSOLE){
//			System.out.println(completelineDebug);
//		}
		
		if (this.authToken == null) {
			AuthenticationException.throwProperException(error, captchaToken, captchaUrl);
		}
		
		String response = this.getRawPhonesInfo();
		int phoneIndex = response.indexOf("gc-user-number-value\">");
		this.phoneNumber = response.substring(phoneIndex + 22, phoneIndex + 36);
		this.phoneNumber = this.phoneNumber.replaceAll("[^a-zA-Z0-9]", "");
		if (this.phoneNumber.indexOf("+") == -1) {
			this.phoneNumber = "+1" + this.phoneNumber;
		}
	}

	/**
	 * Gets the error enum by code.
	 *
	 * @param pErrorCodeString the error code string
	 * @return the error enum by code
	 */
	@Deprecated
	private ERROR_CODE getErrorEnumByCode(String pErrorCodeString) {
		if(pErrorCodeString.equals(ERROR_CODE.AccountDeleted.name())) {
			return ERROR_CODE.AccountDeleted;
		} else if(pErrorCodeString.equals(ERROR_CODE.AccountDisabled.name())) {
			return ERROR_CODE.AccountDisabled;
		} else if(pErrorCodeString.equals(ERROR_CODE.BadAuthentication.name())) {
			return ERROR_CODE.BadAuthentication;
		} else if(pErrorCodeString.equals(ERROR_CODE.CaptchaRequired.name())) {
			return ERROR_CODE.CaptchaRequired;
		} else if(pErrorCodeString.equals(ERROR_CODE.NotVerified.name())) {
			return ERROR_CODE.NotVerified;
		} else if(pErrorCodeString.equals(ERROR_CODE.ServiceDisabled.name())) {
			return ERROR_CODE.ServiceDisabled;
		} else if(pErrorCodeString.equals(ERROR_CODE.TermsNotAgreed.name())) {
			return ERROR_CODE.TermsNotAgreed;
		} else {
			return ERROR_CODE.Unknown;
		}
	}
	
	/**
	 * Gets the error.
	 *
	 * @return the error
	 */
	@Deprecated
	public ERROR_CODE getError() {
		return error;
	}
	
	/**
	 * Gets the captcha url.
	 *
	 * @return the captcha url
	 */
	public String getCaptchaUrl() {
		return captchaUrl;
	}
	
	/**
	 * Gets the captcha token.
	 *
	 * @return the captcha token
	 */
	public String getCaptchaToken() {
		return captchaToken;
	}

	/**
	 * Fires a Get request for Recent Items. If the Response requests login
	 * authentication or if an exception is thrown, a false is returned,
	 * otherwise if arbitrary text is contained for a logged in account, a true
	 * is returned.
	 * 
	 *TODO Examine methodology. Perhaps Could Establish greater persistence with 
	 *an option to force an update.  Currently this is an expensive
	 *and slow method.
	 * 
	 * @return true, if is logged in
	 */
	public boolean isLoggedIn() {
		String res;
		try {
			res = getRecent();
		} catch (IOException e) {
			return false;
		}
		if (res
				.contains("<meta name=\"description\" content=\"Google Voice gives you one number")
				&& res
						.contains("action=\"https://www.google.com/accounts/ServiceLoginAuth?service="+SERVICE+"\"")) {
			return false;
		} else {
			if (res.contains("Enter a new or existing contact name")
					|| res.contains("<json><![CDATA[")) {
				return true;
			}
		}
		return false;
	}	
}
