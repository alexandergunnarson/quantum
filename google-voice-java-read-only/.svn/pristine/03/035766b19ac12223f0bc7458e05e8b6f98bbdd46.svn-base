
package com.techventus.server.voice.datatypes;

import gvjava.org.json.JSONArray;
import gvjava.org.json.JSONException;
import gvjava.org.json.JSONObject;

import com.techventus.server.voice.util.ParsingUtil;

public class Phone implements Comparable<Phone>{
   	//TODO - implement
	
	private boolean saveMode;
	
	private boolean active;
   	private int behaviorOnRedirect;
   	private String carrier;
   	private int customOverrideState;
   	private boolean dEPRECATEDDisabled;
   	private boolean enabledForOthers;
   	private String formattedNumber;
   	private int id;
   	private String incomingAccessNumber;
   	private String name;
   	private String phoneNumber;
   	private int policyBitmask;
   	private boolean redirectToVoicemail;
   	private boolean scheduleSet;
   	private boolean smsEnabled;
   	private boolean telephonyVerified;
   	private int type;
   	private boolean verified;
   	private boolean voicemailForwardingVerified;
   	private Wd wd; //TODO
   	private We we; //TODO
   	private boolean weekdayAllDay;
   	private String[] weekdayTimes;
   	private boolean weekendAllDay;
   	private String[] weekendTimes;
   	
   	public Phone(int id, String name, String phoneNumber) {
   		this.id = id;
   		this.name = name;
   		this.phoneNumber = phoneNumber;
   	}
   	
   	/**
	 * 
	 * saveMode is off
	 * 
	 * @param phonesJSON
	 * 
	 */
   	public Phone(JSONObject phonesJSON) throws JSONException {
   		this(phonesJSON, false);
   	}
   	
   	/**
	    * Instantiates a new Phone Object.
	    *
	    * @param phonesJSON the JSON representation of the Phone object
	    * @param pSaveMode the Save Mode boolean
	    * @throws JSONException the JSONException
	    */
   	public Phone(JSONObject phonesJSON, boolean pSaveMode) throws JSONException {
		if(!saveMode || (saveMode && phonesJSON.has("id")) ) id = phonesJSON.getInt("id");
		if(!saveMode || (saveMode && phonesJSON.has("name")) ) name = phonesJSON.getString("name");
		if(!saveMode || (saveMode && phonesJSON.has("phoneNumber")) ) phoneNumber = phonesJSON.getString("phoneNumber");
		if(!saveMode || (saveMode && phonesJSON.has("active")) ) active = phonesJSON.getBoolean("active");
	   	if(!saveMode || (saveMode && phonesJSON.has("behaviorOnRedirect")) ) behaviorOnRedirect = phonesJSON.getInt("behaviorOnRedirect");
	   	if(!saveMode || (saveMode && phonesJSON.has("carrier")) ) carrier = phonesJSON.getString("carrier");
	   	if(!saveMode || (saveMode && phonesJSON.has("customOverrideState")) ) customOverrideState = phonesJSON.getInt("customOverrideState");
	   	if(!saveMode || (saveMode && phonesJSON.has("dEPRECATEDDisabled")) ) dEPRECATEDDisabled = phonesJSON.getBoolean("dEPRECATEDDisabled");
	   	if(!saveMode || (saveMode && phonesJSON.has("enabledForOthers")) ) enabledForOthers = phonesJSON.getBoolean("enabledForOthers");
	   	if(!saveMode || (saveMode && phonesJSON.has("formattedNumber")) ) formattedNumber = phonesJSON.getString("formattedNumber");
	   	if(!saveMode || (saveMode && phonesJSON.has("incomingAccessNumber")) ) incomingAccessNumber = phonesJSON.getString("incomingAccessNumber");
	   	if(!saveMode || (saveMode && phonesJSON.has("phoneNumber")) ) phoneNumber = phonesJSON.getString("phoneNumber");
	   	if(!saveMode || (saveMode && phonesJSON.has("policyBitmask")) ) policyBitmask = phonesJSON.getInt("policyBitmask");
	   	if(!saveMode || (saveMode && phonesJSON.has("redirectToVoicemail")) ) redirectToVoicemail = phonesJSON.getBoolean("redirectToVoicemail");
	   	if(!saveMode || (saveMode && phonesJSON.has("scheduleSet")) ) {
		   	try {
		   		// if not set, this value is "false", but if active it's 1 !! - this is not true... maybe
		   		 scheduleSet = phonesJSON.getBoolean("scheduleSet");
		   	} catch (JSONException jsE) {
//		   		int positive = phonesJSON.getInt("scheduleSet");
//		   		if(positive==1) scheduleSet = true;
		   		// we will just set false on error
		   		scheduleSet = false;
		   	} 
	   	}
	   	if(!saveMode || (saveMode && phonesJSON.has("smsEnabled")) ) smsEnabled = phonesJSON.getBoolean("smsEnabled");
	   	if(!saveMode || (saveMode && phonesJSON.has("telephonyVerified")) ) telephonyVerified = phonesJSON.getBoolean("telephonyVerified");
	   	if(!saveMode || (saveMode && phonesJSON.has("type")) ) type = phonesJSON.getInt("type");
	   	if(!saveMode || (saveMode && phonesJSON.has("verified")) ) verified = phonesJSON.getBoolean("verified");
	   	if(!saveMode || (saveMode && phonesJSON.has("voicemailForwardingVerified")) ) voicemailForwardingVerified = phonesJSON.getBoolean("voicemailForwardingVerified");
//	   	if(!saveMode || (saveMode && phonesJSON.has("wd")) ) wd = phonesJSON.getInt("id"); //TODO
//	   	if(!saveMode || (saveMode && phonesJSON.has("we")) ) wd = phonesJSON.getInt("id"); //TODO
	   	if(!saveMode || (saveMode && phonesJSON.has("weekdayAllDay")) ) weekdayAllDay = phonesJSON.getBoolean("weekdayAllDay");
	   	if(!saveMode || (saveMode && phonesJSON.has("weekdayTimes")) ) weekdayTimes = ParsingUtil.jsonStringArrayToStringArray(phonesJSON.getJSONArray("weekdayTimes"));//Type correct??
	   	if(!saveMode || (saveMode && phonesJSON.has("weekendAllDay")) ) weekendAllDay = phonesJSON.getBoolean("weekendAllDay");
	   	if(!saveMode || (saveMode && phonesJSON.has("weekendTimes")) ) weekendTimes = ParsingUtil.jsonStringArrayToStringArray(phonesJSON.getJSONArray("weekendTimes"));//Type correct??
   	}
   	
   	public static final Phone[] createArrayFromJsonObject(JSONObject phonesJSON) throws JSONException { 
		JSONArray phoneNames = phonesJSON.names();
		Phone[] result = new Phone[phoneNames.length()];
		for (int i = 0; i < phoneNames.length(); i++) {
			result[i] = new Phone(phonesJSON.getJSONObject(phoneNames.getString(i)));
		}
		return result;
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
	
	public JSONObject toJsonObject(){
		JSONObject resultO = new JSONObject();
		try { 		
			resultO.putOpt("id", id);
			resultO.putOpt("name", name);
			resultO.putOpt("active", active);
		   	resultO.putOpt("behaviorOnRedirect", behaviorOnRedirect);
		   	resultO.putOpt("carrier", carrier);
		   	resultO.putOpt("customOverrideState", customOverrideState);
		   	resultO.putOpt("dEPRECATEDDisabled", dEPRECATEDDisabled);
		   	resultO.putOpt("enabledForOthers", enabledForOthers);
		   	resultO.putOpt("formattedNumber", formattedNumber);
		   	resultO.putOpt("id", id);
		   	resultO.putOpt("incomingAccessNumber", incomingAccessNumber);
		   	resultO.putOpt("name", name);
		   	resultO.putOpt("phoneNumber", phoneNumber);
		   	resultO.putOpt("policyBitmask", policyBitmask);
		   	resultO.putOpt("redirectToVoicemail", redirectToVoicemail);
		   	//Google json expects false or 1 - not anymore
//		   	if(scheduleSet) {
//		   		resultO.putOpt("scheduleSet", 1);
//		   	} else {
//		   		resultO.putOpt("scheduleSet", false);
//		   	}
		   	resultO.putOpt("scheduleSet", scheduleSet);
		   	resultO.putOpt("smsEnabled", smsEnabled);
		   	resultO.putOpt("telephonyVerified", telephonyVerified);
		   	resultO.putOpt("type", type);
		   	resultO.putOpt("verified", verified);
		   	resultO.putOpt("voicemailForwardingVerified", voicemailForwardingVerified);
//		   	private Wd wd", ); //TODO
//		   	private We we", ); //TODO
		   	resultO.putOpt("weekdayAllDay", weekdayAllDay);
		   	resultO.putOpt("weekdayTimes", weekdayTimes);//Type correct??
		   	resultO.putOpt("weekendAllDay", weekendAllDay);
		   	resultO.putOpt("weekendTimes", weekendTimes);//Type correct??
		} catch (JSONException e) {
			return null;
		}
		
		return resultO;
	}

	/**
	 * @param phones
	 * @return Json Object
	 * @throws JSONException 
	 */
	public static Object phonesArrayToJsonObject(Phone[] phones) throws JSONException {
		JSONObject phoneO = new JSONObject();
		for (int i = 0; i < phones.length; i++) {
			phoneO.accumulate(phones[i].getId()+"",phones[i].toJsonObject());
		}
		return phoneO;
	}

	/**
	 * @return the active
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * @return the behaviorOnRedirect
	 */
	public int getBehaviorOnRedirect() {
		return behaviorOnRedirect;
	}

	/**
	 * @return the carrier
	 */
	public String getCarrier() {
		return carrier;
	}

	/**
	 * @return the customOverrideState
	 */
	public int getCustomOverrideState() {
		return customOverrideState;
	}

	/**
	 * @return the dEPRECATEDDisabled
	 */
	public boolean isdEPRECATEDDisabled() {
		return dEPRECATEDDisabled;
	}

	/**
	 * @return the enabledForOthers
	 */
	public boolean isEnabledForOthers() {
		return enabledForOthers;
	}

	/**
	 * @return the formattedNumber
	 */
	public String getFormattedNumber() {
		return formattedNumber;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * returns the value for smsEnabled for this phone
	 * @return
	 */
	public boolean getSmsEnabled() {
		return smsEnabled;
	}

	/**
	 * @return the incomingAccessNumber
	 */
	public String getIncomingAccessNumber() {
		return incomingAccessNumber;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the phoneNumber
	 */
	public String getPhoneNumber() {
		return phoneNumber;
	}

	/**
	 * @return the policyBitmask
	 */
	public int getPolicyBitmask() {
		return policyBitmask;
	}

	/**
	 * @return the redirectToVoicemail
	 */
	public boolean isRedirectToVoicemail() {
		return redirectToVoicemail;
	}

	/**
	 * @return the scheduleSet
	 */
	public boolean isScheduleSet() {
		return scheduleSet;
	}

	/**
	 * @return the smsEnabled
	 */
	public boolean isSmsEnabled() {
		return smsEnabled;
	}

	/**
	 * @return the telephonyVerified
	 */
	public boolean isTelephonyVerified() {
		return telephonyVerified;
	}

	/**
	 * @return the type
	 */
	public int getType() {
		return type;
	}

	/**
	 * @return the verified
	 */
	public boolean isVerified() {
		return verified;
	}

	/**
	 * @return the voicemailForwardingVerified
	 */
	public boolean isVoicemailForwardingVerified() {
		return voicemailForwardingVerified;
	}

	/**
	 * @return the wd
	 */
	public Wd getWd() {
		return wd;
	}

	/**
	 * @return the we
	 */
	public We getWe() {
		return we;
	}

	/**
	 * @return the weekdayAllDay
	 */
	public boolean isWeekdayAllDay() {
		return weekdayAllDay;
	}

	/**
	 * @return the weekdayTimes
	 */
	public String[] getWeekdayTimes() {
		return weekdayTimes;
	}

	/**
	 * @return the weekendAllDay
	 */
	public boolean isWeekendAllDay() {
		return weekendAllDay;
	}

	/**
	 * @return the weekendTimes
	 */
	public String[] getWeekendTimes() {
		return weekendTimes;
	}
	
	

	/**
	 * @param active the active to set
	 */
	public void setActive(boolean active) {
		this.active = active;
	}

	/**
	 * @param behaviorOnRedirect the behaviorOnRedirect to set
	 */
	public void setBehaviorOnRedirect(int behaviorOnRedirect) {
		this.behaviorOnRedirect = behaviorOnRedirect;
	}

	/**
	 * @param carrier the carrier to set
	 */
	public void setCarrier(String carrier) {
		this.carrier = carrier;
	}

	/**
	 * @param customOverrideState the customOverrideState to set
	 */
	public void setCustomOverrideState(int customOverrideState) {
		this.customOverrideState = customOverrideState;
	}

	/**
	 * @param dEPRECATEDDisabled the dEPRECATEDDisabled to set
	 */
	public void setdEPRECATEDDisabled(boolean dEPRECATEDDisabled) {
		this.dEPRECATEDDisabled = dEPRECATEDDisabled;
	}

	/**
	 * @param enabledForOthers the enabledForOthers to set
	 */
	public void setEnabledForOthers(boolean enabledForOthers) {
		this.enabledForOthers = enabledForOthers;
	}

	/**
	 * @param formattedNumber the formattedNumber to set
	 */
	public void setFormattedNumber(String formattedNumber) {
		this.formattedNumber = formattedNumber;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @param incomingAccessNumber the incomingAccessNumber to set
	 */
	public void setIncomingAccessNumber(String incomingAccessNumber) {
		this.incomingAccessNumber = incomingAccessNumber;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param phoneNumber the phoneNumber to set
	 */
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	/**
	 * @param policyBitmask the policyBitmask to set
	 */
	public void setPolicyBitmask(int policyBitmask) {
		this.policyBitmask = policyBitmask;
	}

	/**
	 * @param redirectToVoicemail the redirectToVoicemail to set
	 */
	public void setRedirectToVoicemail(boolean redirectToVoicemail) {
		this.redirectToVoicemail = redirectToVoicemail;
	}

	/**
	 * @param scheduleSet the scheduleSet to set
	 */
	public void setScheduleSet(boolean scheduleSet) {
		this.scheduleSet = scheduleSet;
	}

	/**
	 * @param smsEnabled the smsEnabled to set
	 */
	public void setSmsEnabled(boolean smsEnabled) {
		this.smsEnabled = smsEnabled;
	}

	/**
	 * @param telephonyVerified the telephonyVerified to set
	 */
	public void setTelephonyVerified(boolean telephonyVerified) {
		this.telephonyVerified = telephonyVerified;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * @param verified the verified to set
	 */
	public void setVerified(boolean verified) {
		this.verified = verified;
	}

	/**
	 * @param voicemailForwardingVerified the voicemailForwardingVerified to set
	 */
	public void setVoicemailForwardingVerified(boolean voicemailForwardingVerified) {
		this.voicemailForwardingVerified = voicemailForwardingVerified;
	}

	/**
	 * @param wd the wd to set
	 */
	public void setWd(Wd wd) {
		this.wd = wd;
	}

	/**
	 * @param we the we to set
	 */
	public void setWe(We we) {
		this.we = we;
	}

	/**
	 * @param weekdayAllDay the weekdayAllDay to set
	 */
	public void setWeekdayAllDay(boolean weekdayAllDay) {
		this.weekdayAllDay = weekdayAllDay;
	}

	/**
	 * @param weekdayTimes the weekdayTimes to set
	 */
	public void setWeekdayTimes(String[] weekdayTimes) {
		this.weekdayTimes = weekdayTimes;
	}

	/**
	 * @param weekendAllDay the weekendAllDay to set
	 */
	public void setWeekendAllDay(boolean weekendAllDay) {
		this.weekendAllDay = weekendAllDay;
	}

	/**
	 * @param weekendTimes the weekendTimes to set
	 */
	public void setWeekendTimes(String[] weekendTimes) {
		this.weekendTimes = weekendTimes;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Phone o) {
		if( id < o.getId() )
            return -1;
        if( id > o.getId() )
            return 1;
            
        return 0;
	}
   	
	
}
