package test;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import gvjava.org.json.JSONException;
import gvjava.org.json.JSONObject;

import com.techventus.server.voice.Voice;
import com.techventus.server.voice.datatypes.AllSettings;
import com.techventus.server.voice.datatypes.Contact;
import com.techventus.server.voice.datatypes.DisabledForwardingId;
import com.techventus.server.voice.datatypes.Group;
import com.techventus.server.voice.datatypes.Phone;
import com.techventus.server.voice.datatypes.Greeting;
import com.techventus.server.voice.datatypes.records.SMS;
import com.techventus.server.voice.datatypes.records.SMSThread;
import com.techventus.server.voice.exception.CaptchaRequiredException;
import com.techventus.server.voice.util.ParsingUtil;

@SuppressWarnings("deprecation")
public class test {
	
	static BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	static String userName = null;
	static String pass = null;
	static boolean connectOnStartup = false;
	static Properties testProps = null;
	private static Voice voice;
	private static String jsonData;
	
	public static void main(String[] args){
		
		try {
			testProps = load("test/privateTestData.properties");
			userName = testProps.getProperty("username");
			pass = testProps.getProperty("password");
			connectOnStartup = Boolean.parseBoolean(testProps.getProperty("connectOnStartup"));
			jsonData = testProps.getProperty("jsonData");
		} catch (Exception e) {
			System.out.println("Could not read the testProps, falling back to input. ("+e.getMessage()+")");
			System.out.println("Enter Your Google Voice Username, eg user@gmail.com:");
				//Added this line, otherwise fails all tests.
				connectOnStartup = true;
			try {
				userName = br.readLine();
			} catch (IOException ioe) {
				System.out.println("IO error trying to read your name!");
				System.exit(1);
			}

			System.out.println("Enter Your Password:");


			try {
				pass = br.readLine();
			} catch (IOException ioe) {
				System.out.println("IO error trying to read your name!");
				System.exit(1);
			}
		}
		
		try {
			if(connectOnStartup) voice = new Voice(userName, pass,"GoogleVoiceJava",true,Voice.GOOGLE);
		} catch(CaptchaRequiredException captEx) {
			System.out.println("A captcha is required.");
			System.out.println("Image URL  = "+captEx.getCaptchaUrl());
			System.out.println("Capt Token = "+captEx.getCaptchaToken());
			System.out.println("Goodbye.");
			System.exit(1);
		} catch (IOException e) {
			System.out.println("IO error creating voice! - "+e.getLocalizedMessage());
			System.out.println("Goodbye.");
			System.exit(1);
		}

		listTests();

		/*
	      System.out.println("Enter A \"Source\" for the Log:");
	      String source = null;
	      try {
	    	  source = br.readLine();
	      } catch (IOException ioe) {
	         System.out.println("IO error trying to read your name!");
	         System.exit(1);
	      }

	      System.out.println("Log into Google Voice and find the _rnr_se variable in the page Source. ");
	      System.out.println("Enter rnr_se_ value:");
	      String rnrSee = null;
	      try {
	    	  rnrSee = br.readLine();
	      } catch (IOException ioe) {
	         System.out.println("IO error trying to read your name!");
	         System.exit(1);
	      }
		 */


	}

	/**
	 * Lets the Tester choose the Test to run
	 */
	private static void listTests() {
		System.out.println("Availible Tests for "+userName);
		System.out.println("0: Exit (or any other invalid entry)");  
		System.out.println("1: Multi phone enable / disable");
		System.out.println("2: Inbox paging");
		System.out.println("3: Call Announcement Settings (Presentation)");
		System.out.println("4: Set Default Voicemail Greeting");
		System.out.println("5: Change do not disturb setting.");
		System.out.println("6: Change Group settings.");
		System.out.println("7: Read all settings and print them (cached)");
		System.out.println("8: Read all settings and print them (uncached)");
		System.out.println("9: Read all settings - pure json driven - flat data");
		System.out.println("10: Read all settings - pure json driven - actual account data");
		System.out.println("11: Update Group Settings");
		System.out.println("12: Group settings isPhoneEnabled tests");
		System.out.println("13: List Default Phones and Enabled/Disabled Setting");
		System.out.println("14: Send SMS");
		System.out.println("15: Captcha Test");
		System.out.println("16: List SMS");
		System.out.println("17. List Unread Conversations. Mark Selected Conversations as Read.");
		System.out.println("18. List Starred Conversations. ");
		System.out.println("19. View SMSThread Info");
		System.out.println("20. SMS to Existing Conversation");
		System.out.println("21. Phone SMS enable/disable");
		
		int testNr = 0;
		try {
			testNr = Integer.parseInt(br.readLine());
		} catch (Exception e) {
			System.out.println("Error trying to read the testNr!"+e.getMessage());
			System.exit(1);
		}
		  
		runTest(userName, pass, testNr);
	}

	/**
	 * @param userName
	 * @param pass
	 * @param testNr
	 */
	private static void runTest(String userName, String pass, int testNr) {
	    if(testNr==0) {// 0: Exit							
			System.out.println("Exiting.");
			System.exit(1);
	    }
		try {
	    	  
			
			//Voice voice = new Voice();
//			try {
			if(connectOnStartup) System.out.println(voice.isLoggedIn());
				//Thread.sleep(2000);

					switch (testNr) {
						case 1: // 1: Multi phone enable / disable 
							{
								System.out.println("******** Starting Test "+testNr+" ********");
								Phone[] phones = voice.getSettings(false).getPhones();
								// create int Array from all phone ids
								int[] phonesToChangeStatus = new int[phones.length];
								
								System.out.println("Current phone status:");
								for (int j = 0; j < phones.length; j++) {
									phonesToChangeStatus[j] = phones[j].getId();
									System.out.println(phones[j].getName() + " " + phones[j].getId() + " " + !voice.getSettings(false).isPhoneDisabled(phones[j].getId()) );
								}
								
								//Disable all phones
								for (int j = 0; j < phones.length; j++) {
									voice.phoneDisable(phones[j].getId());
								}
								
								
								phones = voice.getSettings(true).getPhones();
								// Output
								System.out.println("After deactivate multi:");
								for (int j = 0; j < phones.length; j++) {
									System.out.println(phones[j].getName() + " " + phones[j].getId() + " " + !voice.getSettings(false).isPhoneDisabled(phones[j].getId()) );
								}
								
								//Enable all phones 
								for (int j = 0; j < phones.length; j++) {
									voice.phoneEnable(phones[j].getId());
								}
								
								// Output
								phones = voice.getSettings(true).getPhones();
								System.out.println("After activate multi:");
								for (int j = 0; j < phones.length; j++) {
									System.out.println(phones[j].getName() + " " + phones[j].getId() + " " + !voice.getSettings(false).isPhoneDisabled(phones[j].getId()) );
								}
								
								System.out.println("******** Finished Test "+testNr+" ********");
							}
							break;
							
						case 2: // 2: Inbox paging
							{
								System.out.println("******** Starting Test "+testNr+" ********");
								System.out.println(voice.getInboxPage(1000));
								System.out.println("******** Finished Test "+testNr+" ********");
							}
							break;
							
						case 3: // 3: Call Announcement settings (Presentation)
							{
								System.out.println("******** Starting Test "+testNr+" ********");
								System.out.println("Type 'true' for enable, 'false' for disable");
								boolean choice = false;
								try {
									choice = Boolean.parseBoolean(br.readLine());
								} catch (Exception e) {
									System.out.println("Error trying to read the choice!"+e.getMessage());
									System.exit(1);
								}
	//							Thread.sleep(2000);
								System.out.println(voice.setCallPresentation(choice));
								System.out.println("******** Finished Test "+testNr+" ********");
								break;
							}
						case 4: // 4: Caller ID in
						{
							System.out.println("******** Starting Test "+testNr+" ********");
							for (Iterator<Greeting> iterator = voice.getVoicemailList(true).iterator(); iterator.hasNext();) {
								Greeting type = (Greeting) iterator.next();
								System.out.println(type.toString());
							}
							System.out.println("Choose the id of the voicemail greeting to use. ie '0' system standard or '1','2'");
							String voicemailNr = "0";
							try {
								voicemailNr = br.readLine();
							} catch (Exception e) {
								System.out.println("Error trying to read the choice!"+e.getMessage());
								System.exit(1);
							}
//							Thread.sleep(2000);
							voice.setVoicemailGreetingId(voicemailNr);
							System.out.println("******** Finished Test "+testNr+" ********");
							break;
						}
						case 5: // 5: Do not disturb
						{
							System.out.println("******** Starting Test "+testNr+" ********");
							System.out.println("Type 'true' for enable, 'false' for disable");
							boolean dndChoice = false;
							try {
								dndChoice = Boolean.parseBoolean(br.readLine());
							} catch (Exception e) {
								System.out.println("Error trying to read the choice!"+e.getMessage());
								System.exit(1);
							}
							voice.setDoNotDisturb(dndChoice);
							System.out.println("******** Finished Test "+testNr+" ********");
							break;
						}
						case 6: // 6: Group settings
						{
							System.out.println("******** Starting Test "+testNr+" ********");
							Group[] groups1 = voice.getSettings(false).getSettings().getGroups();
							String jsonGroups="";
							for (int i = 0; i < groups1.length; i++) {
								jsonGroups+=groups1[i].toJson();
							}
							System.out.println("All to json:"+jsonGroups);
							System.out.println("******** Finished Test "+testNr+" ********");
							break;
						}
						case 7: // 7: Read all settings - cached
						{
							System.out.println("******** Starting Test "+testNr+" ********");
							System.out.println(voice.getSettings(false).getSettings().toJson());
							System.out.println("******** Finished Test "+testNr+" ********");
							break;
						}
						case 8: // 8: Read all settings - uncached
						{
							System.out.println("******** Starting Test "+testNr+" ********");
							System.out.println(voice.getSettings(true).getSettings().toJson());
							System.out.println("******** Finished Test "+testNr+" ********");
							break;
						}
						case 9: // 9: Read all settings - pure json driven
						{
							System.out.println("******** Starting Test "+testNr+" ********");
							try {
								System.out.println("******** Original JSON Data ********");
								JSONObject origSettings = new JSONObject(jsonData);
								System.out.println(origSettings.toString(4));
								
								System.out.println("******* Parsed back and forth ******");
								AllSettings settings2 = new AllSettings(jsonData);
								System.out.println(settings2.toJsonObject().toString(4));
								
								System.out.println("******* Creating new AllSettings from old JSON ******");
								AllSettings settings4 = new AllSettings(settings2.toJsonObject().toString());
								System.out.println(settings4.toJsonObject().toString(4));
							} catch (JSONException e) {
								System.out.println("Error displaying json:"+e.getLocalizedMessage());
								e.printStackTrace();
							}
							System.out.println("******** Finished Test "+testNr+" ********");
							break;
						}
						case 10: // 10: Read all settings - pure json driven - account data
							System.out.println("******** Starting Test "+testNr+" ********");
//							try {
								System.out.println("******** Test REMOVED FOR NOW ********");
//								System.out.println("******** Original JSON Data ********");
//								String lJson = ParsingUtil.removeUninterestingParts(voice.getONLYFORTEST("https://www.google.com/voice/settings/tab/groups"), "<json><![CDATA[", "]]></json>", false);
//								JSONObject origSettings = new JSONObject(lJson);
//								System.out.println(origSettings.toString(4));
//								
//								System.out.println("******* JsonObject from AllSettings ******");
//								AllSettings settings2 = new AllSettings(lJson);
//								JSONObject objFromAllSettings = settings2.toJsonObject();
//								System.out.println(objFromAllSettings.toString(4));
//								
//								System.out.println("******* Creating new AllSettings from old JSON ******");
//								AllSettings settings3 = new AllSettings(objFromAllSettings.toString());
//								System.out.println(settings3.toJsonObject().toString(4));
//								
//							} catch (JSONException e) {
//								System.out.println("Error displaying json:"+e.getLocalizedMessage());
//								e.printStackTrace();
//							}
							System.out.println("******** Finished Test "+testNr+" ********");
							break;
							
						case 11: // 11: Update Group settings
							System.out.println("******** Starting Test "+testNr+" ********");
							try {
								System.out.println("******** Before ********");
								
								AllSettings settings2 = voice.getSettings(false);
								JSONObject objFromAllSettings = settings2.toJsonObject();
								System.out.println(objFromAllSettings.toString(4));
								
								System.out.println("Choose group to change settings (15)");
								String groupId = "0";
								try {
									groupId = br.readLine();
								} catch (Exception e) {
									System.out.println("Error trying to read the choice!"+e.getMessage());
									System.exit(1);
								}
								
								Group[] groups = voice.getSettings(false).getSettings().getGroups();
								for (int j = 0; j < groups.length; j++) {
								/* New Settings
								"directConnect": true,
				                "disabledForwardingIds": {"1": true},
				                "greetingId": 2,
				                "id": "15",
				                "isCustomDirectConnect": true,
				                "isCustomForwarding": true,
				                "isCustomGreeting": true,	
								 */
									if(groups[j].getId().equals(groupId)) {
										System.out.println("Changing settings for Group: "+groups[j].getName());
										groups[j].setCustomDirectConnect(true);
										groups[j].setDirectConnect(true);
										groups[j].setCustomForwarding(true);
										List<DisabledForwardingId> disList = new ArrayList<DisabledForwardingId>();
										disList.add(new DisabledForwardingId("1", true));
										groups[j].setDisabledForwardingIds(disList);
										groups[j].setCustomGreeting(true);
										groups[j].setGreetingId(2);
										voice.setNewGroupSettings(groups[j]);
									}
								}
								
								System.out.println("******** After  ********");
								AllSettings settings3 = voice.getSettings(true);
								System.out.println(settings3.toJsonObject().toString(4));
								
							} catch (JSONException e) {
								System.out.println("Error displaying json:"+e.getLocalizedMessage());
								e.printStackTrace();
							}
							System.out.println("******** Finished Test "+testNr+" ********");
							break;
							
						case 12: // 12: Group isPhoneEnabled Tests
						{
							System.out.println("******** Starting Test "+testNr+" ********");
							try {
								AllSettings settings3 = voice.getSettings(true);
								Group[] groups = settings3.getSettings().getGroups();
								int[] phoneIds = settings3.getPhoneList();
								for (int j = 0; j < groups.length; j++) {
									System.out.println("+++ Disabled Phones in "+groups[j].getName()+" +++");
									for (int j2 = 0; j2 < phoneIds.length; j2++) {
										System.out.println(phoneIds[j2] + ":" + groups[j].isPhoneDisabled(phoneIds[j2]));
									}

								}
							} catch (JSONException e) {
								System.out.println("Error displaying json:"+e.getLocalizedMessage());
								e.printStackTrace();
							}
							System.out.println("******** Finished Test "+testNr+" ********");
							break;
						}
						case 13: // 13: List Default Phones and Enabled/Disabled Setting
						{
							System.out.println("*********Starting Test "+testNr+" *******");
							
							AllSettings settings3 = voice.getSettings(true);
							List<Integer> phoneList =settings3.getPhoneListAsList();
							Phone[] actualPhoneArray = settings3.getPhones();
							for(Integer phoneint:phoneList){
								inner: for(int ig=0;ig<actualPhoneArray.length;ig++){
											if(actualPhoneArray[ig].getId()==phoneint){
												System.out.println(actualPhoneArray[ig].getId()+ " "+actualPhoneArray[ig].getName()+" enabled:"+!settings3.isPhoneDisabled(actualPhoneArray[ig].getId()));
												break inner;
											}
								}
							}
							
							
//							Group[] groupAr = settings3.getSettings().getGroups()
//							for(String group:groupAr){
//								settings3.getSettings()
//							}
//							
//							
//							settings3.getSettings().getGroups()[0].
//							}
							System.out.println("******** Finished Test "+testNr+" ********");
							
							break;
						}
						case 14:
						{
							System.out.println("*********Starting Test "+testNr+" SEND SMS*******");
							System.out.println("\n\rEnter Number to Send SMS:");
							String number = br.readLine();
							System.out.println("Enter Message:");
							String txt = br.readLine();
							voice.sendSMS(number, txt);
							System.out.println("******** Finished Test "+testNr+" ********");
							break;
						}
						case 15: // 15: Captcha Test
							System.out.println("*********Starting Test "+testNr+" *******");
							System.out.println("Enter Fake Password:");
							String tempPass = br.readLine();
							
							boolean tryAgain = true;
							int counter = 0;
							while(tryAgain) {
								System.out.println("\n\rAttempting new Login... " + counter++);
								try {
									voice = null;
									/* (Toby) added here the counter to the tempPass, so that we actually get the CaptchaEx - bc without 
									 * it I had 300 tries only getting the IO Error, with counter (each time a different password) 
									 * I got the captcha after 30 tries
									 */
									voice = new Voice(userName, tempPass+counter,"GoogleVoiceJavaUnitTests",true,Voice.GOOGLE);
								} catch (CaptchaRequiredException e) {
									System.out.println("Captcha found! - " + e.getLocalizedMessage());
									System.out.println("Token: " + e.getCaptchaToken());
									System.out.println("URL  : " + e.getCaptchaUrl());
									System.out.println("Please enter the captcha challenge response from the URL:");
									String response = br.readLine();
									// create the voice object with the response
									voice = new Voice(userName, pass,"GoogleVoiceJavaUnitTests",true,Voice.GOOGLE,response,e.getCaptchaToken());
									tryAgain = false;
								} catch (IOException e) {
									System.out.println("IO error creating voice! - "+e.getLocalizedMessage());
								}
							
							}
							System.out.println("******** Finished Test "+testNr+" ********");
							
							break;
							
						case 16:
							System.out.println("*********Starting Test "+testNr+" LIST SMS *******");
							String smsString = voice.getSMS();
							  //remove whitespaces
							  //StringTokenizer st = new StringTokenizer(smsString," ",false);
							  //String t="";
							  //while (st.hasMoreElements()) t += st.nextElement();
							  //remove newLine
							  StringTokenizer st2 = new StringTokenizer(smsString,"\n",false);
							  String t2="";
							  while (st2.hasMoreElements()) t2 += st2.nextElement();
							System.out.println(t2);
							break;
							
							
						case 17:
							System.out.println("*********Starting Test "+testNr+" List Unread SMS, Mark one as read. *******");
							
							Collection<SMSThread> collection = voice.getSMSThreads(voice.getUnreadSMS());
							
							int i = 0;
							
							for(SMSThread t:collection){
								System.out.println("index "+i+" id "+t.getId());
								Contact cont = t.getContact();
								//System.out.println(t.getNote());
								Collection<SMS> allsms = t.getAllSMS();
								//t.
								for(SMS smsind:allsms){
									System.out.println(smsind.getFrom()+" "+smsind.getContent());
									
								}
								System.out.println("Contact Name "+cont.getName());
								i++;
		
								
								
							}
							
							List<SMSThread> smsList = new ArrayList<SMSThread>(collection);
							try{
								System.out.println("Please Enter an Integer corresponding to one of the above Indeces to \n\rMark the SMS Conversation as Read");
								int index = Integer.parseInt(br.readLine());
								voice.markAsRead(smsList.get(index).getId());
							}catch(Exception j){
								System.out.println("Error: Did you enter an invalid Non-Negative Integer?");
								j.printStackTrace();
							}
							
							break;
						
						case 18:
							System.out.println("*********Starting Test "+testNr+" List Starred Conversations. *******");

							Collection<SMSThread> collection18 = voice.getSMSThreads(voice.getStarred());
							
							int i18 = 0;
							
							for(SMSThread t:collection18){
								System.out.println("index "+i18+" id "+t.getId());
								Contact cont = t.getContact();
								//System.out.println(t.getNote());
								Collection<SMS> allsms = t.getAllSMS();
								//t.
								for(SMS smsind:allsms){
									System.out.println(smsind.getFrom()+" "+smsind.getContent());
									
								}
								System.out.println("Contact Name "+cont.getName());
								i18++;
		
								
								
							}
							break;
							
						case 19:
							System.out.println("*********Starting Test "+testNr+" View SMSThread Info. *******");
							String unreads = voice.getUnreadSMS();
							Collection<SMSThread> collection19 = voice.getSMSThreads(unreads);
							
							int i19 = 0;
							
							for(SMSThread t:collection19){
								System.out.println("index "+i19+" id "+t.getId());
								Contact cont = t.getContact();
								//System.out.println(t.getNote());
								Collection<SMS> allsms = t.getAllSMS();
								
								for(SMS smsind:allsms){
									System.out.println(smsind.getFrom()+" "+smsind.getContent());
									
								}
								System.out.println("Contact Name "+cont.getName());
								i19++;
							}
							
							

							List<SMSThread> smsList19 = new ArrayList<SMSThread>(collection19);
							try{
								System.out.println("Please Enter an Integer corresponding to one of the above Indeces to \n\rView SMS Conversation (SMSThread) Details");
								int index = Integer.parseInt(br.readLine());
								System.out.println(smsList19.get(index).toString());
								//voice.markAsRead(smsList19.get(index).getId());
							}catch(Exception j){
								System.out.println("Error: Did you enter an invalid Non-Negative Integer?");
								j.printStackTrace();
							}
							break;
							
							
							
						case 20:
							System.out.println("*********Starting Test "+testNr+" Send SMS as Part of Existing Conversation (SMSThread). *******");
							
							Collection<SMSThread> collection20 = voice.getSMSThreads(voice.getUnreadSMS());
							
							int i20 = 0;
							
							for(SMSThread t:collection20){
								System.out.println("index "+i20+" id "+t.getId());
								System.out.println(t.getContact().getName());
								System.out.println(t.getContact().getNumber());
								Contact cont = t.getContact();
								//System.out.println(t.getNote());
								Collection<SMS> allsms = t.getAllSMS();
								
								for(SMS smsind:allsms){
									System.out.println(smsind.getFrom()+" "+smsind.getContent());
									
								}
								System.out.println("Contact Name "+cont.getName());
								i20++;
							}
							
							

							List<SMSThread> smsList20 = new ArrayList<SMSThread>(collection20);
							try{
								System.out.println("Please Enter an Integer corresponding to one of the above Indeces to \n\rAdd to the Conversation:");
								int index20 = Integer.parseInt(br.readLine());
								System.out.println("Please Enter an SMS Text:");
								String txt = br.readLine();
								
								String destinationNumber = smsList20.get(index20).getContact().getNumber();
								
								voice.sendSMS(destinationNumber, txt, smsList20.get(index20));
								
								//voice.markAsRead(smsList19.get(index).getId());
							}catch(Exception j){
								System.out.println("Error: Did you enter an invalid Non-Negative Integer?");
								j.printStackTrace();
							}
							break;
							
						case 21: // 1: phone smsEnable
						{
							System.out.println("******** Starting Test "+testNr+" ********");
							Phone[] phones = voice.getSettings(false).getPhones();
							// create int Array from all phone ids
							int[] phonesToChangeStatus = new int[phones.length];
							
							System.out.println("Current phone status:");
							for (int j = 0; j < phones.length; j++) {
								phonesToChangeStatus[j] = phones[j].getId();
								System.out.println(phones[j].getName() + " " + phones[j].getId() + " " + " SmsEnabled?="+phones[j].getSmsEnabled());
							}
							
							System.out.println("Disable sms all phones:");
							for (int j = 0; j < phones.length; j++) {
								voice.setSmsEnabled(false,phones[j].getId());
							}
							
							
							phones = voice.getSettings(true).getPhones();
							// Output
							System.out.println("After deactivate multi:");
							for (int j = 0; j < phones.length; j++) {
								System.out.println(phones[j].getName() + " " + phones[j].getId() + " " + " SmsEnabled?="+phones[j].getSmsEnabled());
							}
							
							System.out.println("Enable sms all phones:");
							for (int j = 0; j < phones.length; j++) {
								voice.setSmsEnabled(true,phones[j].getId());
							}
							
							// Output
							phones = voice.getSettings(true).getPhones();
							System.out.println("After activate multi:");
							for (int j = 0; j < phones.length; j++) {
								System.out.println(phones[j].getName() + " " + phones[j].getId() + " " + " SmsEnabled?="+phones[j].getSmsEnabled());
							}
							
							System.out.println("******** Finished Test "+testNr+" ********");
						}
						break;
							
							
							
						default: 						
							System.out.println("Test "+testNr+" not found, exiting.");
							System.exit(1);
							break;
					}
					
	
		} catch (IOException e) {	
			e.printStackTrace();
			System.exit(1);
		} catch (JSONException e) {
			e.printStackTrace();
			System.exit(1);
		}
		listTests(); // List the Tests again
	}
	
	/**
     * Load a Properties File
     * @param propsFile
     * @return Properties
     * @throws IOException
     */
    private static Properties load(String propsFile) throws IOException {

    	Properties result = null;
        InputStream in = null;
         
         if (! propsFile.endsWith (".properties"))
        	 propsFile = propsFile.concat (".properties");
                         
         // Returns null on lookup failures:
         in = ClassLoader.getSystemClassLoader().getResourceAsStream (propsFile);
         if (in != null)
         {
             result = new Properties ();
             result.load (in); // Can throw IOException
         }
         testProps = result;
         return result;
    }
	
}
