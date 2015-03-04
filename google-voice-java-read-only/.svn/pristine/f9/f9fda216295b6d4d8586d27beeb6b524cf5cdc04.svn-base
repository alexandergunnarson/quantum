package com.techventus.server.voice.datatypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gvjava.org.json.JSONArray;
import gvjava.org.json.JSONException;
import gvjava.org.json.JSONObject;

import com.techventus.server.voice.util.ParsingUtil;
/**
 * Accountsettings - subtype of AllSettings
 * 
 * @author tobias eisentraeger
 *
 */
public class Setting {
	
	private final static boolean saveMode = true;

	// Settings in order of the json
	private int[] mActiveForwardingList;
	private String baseUrl;
	private int credits;
	private int defaultGreetingId;
	private String[] mDidInfos;
    private boolean directConnect;
    private DisabledId[] mDisabledIdMap;
    private boolean doNotDisturb;
    private EmailAddress[] emailAddresses;
    private boolean emailNotificationActive;
    private String emailNotificationAddress;
    private Greeting[] greetings;
    private String[] groupList;
    private Group[] groups;
	private String language;
	private String primaryDid;
	private int screenBehavior;
	private boolean showTranscripts;
	private String[] smsNotifications;
	private boolean smsToEmailActive;
	private boolean smsToEmailSubject;
	private String spam;
	private String timezone;
	private boolean useDidAsCallerId;
	private boolean useDidAsSource;
	
	public Setting(JSONObject settingsJSON) throws JSONException {
		if(!saveMode || saveMode && settingsJSON.has("activeForwardingIds")) mActiveForwardingList = ParsingUtil.jsonIntArrayToIntArray(settingsJSON.getJSONArray("activeForwardingIds"));
		if(!saveMode || saveMode && settingsJSON.has("baseUrl")) baseUrl = settingsJSON.getString("baseUrl");
		if(!saveMode || saveMode && settingsJSON.has("credits")) credits = settingsJSON.getInt("credits");
		if(!saveMode || saveMode && settingsJSON.has("defaultGreetingId")) defaultGreetingId = settingsJSON.getInt("defaultGreetingId");
		if(!saveMode || saveMode && settingsJSON.has("didInfos")) mDidInfos = ParsingUtil.jsonStringArrayToStringArray(settingsJSON.getJSONArray("didInfos"));
		if(!saveMode || saveMode && settingsJSON.has("directConnect")) directConnect =  settingsJSON.getBoolean("directConnect");
		if(!saveMode || saveMode && settingsJSON.has("disabledIdMap")) mDisabledIdMap = DisabledId.createArrayFromJsonObject(settingsJSON);
		if(!saveMode || saveMode && settingsJSON.has("doNotDisturb")) doNotDisturb =  settingsJSON.getBoolean("doNotDisturb");
		if(!saveMode || saveMode && settingsJSON.has("emailAddresses")) emailAddresses = EmailAddress.createArrayFromJsonObject(settingsJSON);
		if(!saveMode || saveMode && settingsJSON.has("emailNotificationActive")) emailNotificationActive =  settingsJSON.getBoolean("emailNotificationActive");
		if(!saveMode || saveMode && settingsJSON.has("emailNotificationAddress")) emailNotificationAddress = settingsJSON.getString("emailNotificationAddress");
		if(!saveMode || saveMode && settingsJSON.has("greetings")) greetings = Greeting.createArrayFromJsonObject(settingsJSON);
		if(!saveMode || saveMode && settingsJSON.has("groupList")) groupList = ParsingUtil.jsonStringArrayToStringArray(settingsJSON.getJSONArray("groupList"));
		if(!saveMode || saveMode && settingsJSON.has("groups")) groups = Group.createArrayFromJsonObject(settingsJSON.getJSONObject("groups"));
		if(!saveMode || saveMode && settingsJSON.has("language")) language = settingsJSON.getString("language");
		if(!saveMode || saveMode && settingsJSON.has("primaryDid")) primaryDid = settingsJSON.getString("primaryDid");
		if(!saveMode || saveMode && settingsJSON.has("screenBehavior")) screenBehavior = settingsJSON.getInt("screenBehavior");
		if(!saveMode || saveMode && settingsJSON.has("showTranscripts")) showTranscripts = settingsJSON.getBoolean("showTranscripts");
		if(!saveMode || saveMode && settingsJSON.has("smsNotifications")) smsNotifications = ParsingUtil.jsonStringArrayToStringArray(settingsJSON.getJSONArray("smsNotifications"));
		if(!saveMode || saveMode && settingsJSON.has("smsToEmailActive")) smsToEmailActive =  settingsJSON.getBoolean("smsToEmailActive");
		if(!saveMode || saveMode && settingsJSON.has("smsToEmailSubject")) smsToEmailSubject = settingsJSON.getBoolean("smsToEmailSubject");
		if(!saveMode || saveMode && settingsJSON.has("spam")) spam = settingsJSON.getString("spam");
		if(!saveMode || saveMode && settingsJSON.has("timezone")) timezone = settingsJSON.getString("timezone");
		if(!saveMode || saveMode && settingsJSON.has("useDidAsCallerId")) useDidAsCallerId = settingsJSON.getBoolean("useDidAsCallerId");
		if(!saveMode || saveMode && settingsJSON.has("useDidAsSource")) useDidAsSource = settingsJSON.getBoolean("useDidAsSource");
		//TODO webCallButtons
	}
	
	/**
     * Make a JSON text of the Settings. For compactness, no whitespace
     * is added. If this would not result in a syntactically correct JSON text,
     * then null will be returned instead.
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     *
     * @return a printable, displayable, portable, transmittable
     *  representation of the object, beginning
     *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
     *  with <code>}</code>&nbsp;<small>(right brace)</small>.
     */
	public String toJson(){
		return toJsonObject().toString();
	}
	
	/**
	 * Convert this Settings Object to its JsonObject Representation.
	 *
	 * @return the jSON object
	 */
	public JSONObject toJsonObject(){ 
		JSONObject settingsO = new JSONObject();
		try { 
			//JSONObject settingsO = new JSONObject();

			settingsO.putOpt("activeForwardingIds", mActiveForwardingList);
			settingsO.putOpt("baseUrl", baseUrl);
			settingsO.putOpt("credits", credits);
			settingsO.putOpt("defaultGreetingId", defaultGreetingId);
			settingsO.putOpt("didInfos", mDidInfos);
			settingsO.putOpt("directConnect", directConnect);
			if(mDisabledIdMap!=null) {
				Map<String,Boolean> disMap = new HashMap<String,Boolean>();
				for (DisabledId disId : mDisabledIdMap) {
					disMap.put(disId.getId(), disId.isDisabled());
				}
				settingsO.putOpt("disabledIdMap", disMap);
			} else {  
				// need to put this "disabledIdMap": {}
			}
			settingsO.putOpt("doNotDisturb", doNotDisturb);
			if(emailAddresses!=null) {
				for (int i = 0; i < emailAddresses.length; i++) {
					settingsO.accumulate("emailAddresses", emailAddresses[i].getAddress());
				}
			} else {
				// need to put this "emailAddresses": {}
			}
			settingsO.putOpt("emailNotificationActive", emailNotificationActive);
			settingsO.putOpt("emailNotificationAddress", emailNotificationAddress);
			settingsO.putOpt("greetings", greetings); // An Object Array uses the Bean get Methods - no toJson() needed in Greeting
			settingsO.putOpt("groupList", groupList);
			
			settingsO.accumulate("groups", Group.groupsArrayToJsonObject(groups)); 
			
//			JSONObject groupObject = new JSONObject();
//			JSONArray groupNames = groups.names();
//			if(groupNames!=null && groupNames.length()>0) {
//				for (int i = 0; i < groupNames.length(); i++) {
//					JSONObject oneGroupObject = groups;
//					groupObject.putOpt(groupNames.getString(i), oneGroupObject);
//				}
//			}
			
			settingsO.putOpt("language", language);
			settingsO.putOpt("primaryDid", primaryDid);
			settingsO.putOpt("screenBehavior", screenBehavior);
			settingsO.putOpt("showTranscripts", showTranscripts);
			settingsO.putOpt("smsNotifications", smsNotifications);
			settingsO.putOpt("smsToEmailActive", smsToEmailActive);
			settingsO.putOpt("smsToEmailSubject", smsToEmailSubject);
			settingsO.putOpt("spam", spam);
			settingsO.putOpt("timezone", timezone);
			settingsO.putOpt("useDidAsCallerId", useDidAsCallerId);
			settingsO.putOpt("useDidAsSource", useDidAsSource);
			
			//resultO.put("settings", settingsO);
		} catch (JSONException e) {
			System.out.println(e.getLocalizedMessage());
			return null;
		}
		
		return settingsO;
	}

	/**
	 * @return the mActiveForwardingList
	 */
	public int[] getmActiveForwardingList() {
		return mActiveForwardingList;
	}

	/**
	 * @return the baseUrl
	 */
	public String getBaseUrl() {
		return baseUrl;
	}

	/**
	 * @return the credits
	 */
	public int getCredits() {
		return credits;
	}

	/**
	 * @return the defaultGreetingId
	 */
	public int getDefaultGreetingId() {
		return defaultGreetingId;
	}

	/**
	 * @return the mDidInfos
	 */
	public String[] getmDidInfos() {
		return mDidInfos;
	}

	/**
	 * @return the directConnect
	 */
	public boolean isDirectConnect() {
		return directConnect;
	}

	/**
	 * @return the mDisabledIdMap
	 */
	public DisabledId[] getmDisabledIdList() {
		return mDisabledIdMap;
	}

	/**
	 * @return the doNotDisturb
	 */
	public boolean isDoNotDisturb() {
		return doNotDisturb;
	}

	/**
	 * @return the emailAddresses
	 */
	public EmailAddress[] getEmailAddresses() {
		return emailAddresses;
	}

	/**
	 * @return the emailNotificationActive
	 */
	public boolean isEmailNotificationActive() {
		return emailNotificationActive;
	}

	/**
	 * @return the emailNotificationAddress
	 */
	public String getEmailNotificationAddress() {
		return emailNotificationAddress;
	}

	/**
	 * @return the greetings
	 */
	public Greeting[] getGreetings() {
		return greetings;
	}
	/**
	 * @return the greetings
	 */
	public Greeting[] getGreetingsSorted() {
		Arrays.sort(greetings);
		return greetings;
	}

	/**
	 * @return the groupList
	 */
	public String[] getGroupList() {
		return groupList;
	}
	
	/**
	 * @return the groupList as List<String>
	 */
	public List<String> getGroupListAsList() {
		List<String> lresult = new ArrayList<String>();
		for (int i = 0; i < groupList.length; i++) {
			lresult.add(groupList[i]);
		}
		return lresult;
	}

	/**
	 * @return the groups
	 */
	public Group[] getGroups() {
		return groups;
	}
	

	/**
	 * @return the language
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * @return the primaryDid
	 */
	public String getPrimaryDid() {
		return primaryDid;
	}

	/**
	 * @return the screenBehavior
	 */
	public int getScreenBehavior() {
		return screenBehavior;
	}

	/**
	 * @return the showTranscripts
	 */
	public boolean isShowTranscripts() {
		return showTranscripts;
	}

	/**
	 * @return the smsNotifications
	 */
	public String[] getSmsNotifications() {
		return smsNotifications;
	}

	/**
	 * @return the smsToEmailActive
	 */
	public boolean isSmsToEmailActive() {
		return smsToEmailActive;
	}

	/**
	 * @return the smsToEmailSubject
	 */
	public boolean isSmsToEmailSubject() {
		return smsToEmailSubject;
	}

	/**
	 * @return the spam
	 */
	public String getSpam() {
		return spam;
	}

	/**
	 * @return the timezone
	 */
	public String getTimezone() {
		return timezone;
	}

	/**
	 * @return the useDidAsCallerId
	 */
	public boolean isUseDidAsCallerId() {
		return useDidAsCallerId;
	}

	/**
	 * @return the useDidAsSource
	 */
	public boolean isUseDidAsSource() {
		return useDidAsSource;
	}

	/**
	 * @param mActiveForwardingList the mActiveForwardingList to set
	 */
	public void setmActiveForwardingList(int[] mActiveForwardingList) {
		this.mActiveForwardingList = mActiveForwardingList;
	}

	/**
	 * @param baseUrl the baseUrl to set
	 */
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	/**
	 * @param credits the credits to set
	 */
	public void setCredits(int credits) {
		this.credits = credits;
	}

	/**
	 * @param defaultGreetingId the defaultGreetingId to set
	 */
	public void setDefaultGreetingId(int defaultGreetingId) {
		this.defaultGreetingId = defaultGreetingId;
	}

	/**
	 * @param mDidInfos the mDidInfos to set
	 */
	public void setmDidInfos(String[] mDidInfos) {
		this.mDidInfos = mDidInfos;
	}

	/**
	 * @param directConnect the directConnect to set
	 */
	public void setDirectConnect(boolean directConnect) {
		this.directConnect = directConnect;
	}

	/**
	 * @param mDisabledIdMap the mDisabledIdMap to set
	 */
	public void setmDisabledIdList(DisabledId[] mDisabledIdList) {
		this.mDisabledIdMap = mDisabledIdList;
	}

	/**
	 * @param doNotDisturb the doNotDisturb to set
	 */
	public void setDoNotDisturb(boolean doNotDisturb) {
		this.doNotDisturb = doNotDisturb;
	}

	/**
	 * @param emailAddresses the emailAddresses to set
	 */
	public void setEmailAddresses(EmailAddress[] emailAddresses) {
		this.emailAddresses = emailAddresses;
	}

	/**
	 * @param emailNotificationActive the emailNotificationActive to set
	 */
	public void setEmailNotificationActive(boolean emailNotificationActive) {
		this.emailNotificationActive = emailNotificationActive;
	}

	/**
	 * @param emailNotificationAddress the emailNotificationAddress to set
	 */
	public void setEmailNotificationAddress(String emailNotificationAddress) {
		this.emailNotificationAddress = emailNotificationAddress;
	}

	/**
	 * @param greetings the greetings to set
	 */
	public void setGreetings(Greeting[] greetings) {
		this.greetings = greetings;
	}

	/**
	 * @param groupList the groupList to set
	 */
	public void setGroupList(String[] groupList) {
		this.groupList = groupList;
	}

	/**
	 * @param groups the groups to set
	 */
	public void setGroups(Group[] groups) {
		this.groups = groups;
	}

	/**
	 * @param language the language to set
	 */
	public void setLanguage(String language) {
		this.language = language;
	}

	/**
	 * @param primaryDid the primaryDid to set
	 */
	public void setPrimaryDid(String primaryDid) {
		this.primaryDid = primaryDid;
	}

	/**
	 * @param screenBehavior the screenBehavior to set
	 */
	public void setScreenBehavior(int screenBehavior) {
		this.screenBehavior = screenBehavior;
	}

	/**
	 * @param showTranscripts the showTranscripts to set
	 */
	public void setShowTranscripts(boolean showTranscripts) {
		this.showTranscripts = showTranscripts;
	}

	/**
	 * @param smsNotifications the smsNotifications to set
	 */
	public void setSmsNotifications(String[] smsNotifications) {
		this.smsNotifications = smsNotifications;
	}

	/**
	 * @param smsToEmailActive the smsToEmailActive to set
	 */
	public void setSmsToEmailActive(boolean smsToEmailActive) {
		this.smsToEmailActive = smsToEmailActive;
	}

	/**
	 * @param smsToEmailSubject the smsToEmailSubject to set
	 */
	public void setSmsToEmailSubject(boolean smsToEmailSubject) {
		this.smsToEmailSubject = smsToEmailSubject;
	}

	/**
	 * @param spam the spam to set
	 */
	public void setSpam(String spam) {
		this.spam = spam;
	}

	/**
	 * @param timezone the timezone to set
	 */
	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	/**
	 * @param useDidAsCallerId the useDidAsCallerId to set
	 */
	public void setUseDidAsCallerId(boolean useDidAsCallerId) {
		this.useDidAsCallerId = useDidAsCallerId;
	}

	/**
	 * @param useDidAsSource the useDidAsSource to set
	 */
	public void setUseDidAsSource(boolean useDidAsSource) {
		this.useDidAsSource = useDidAsSource;
	}
	
	

}
