/**
 * 
 */
package com.techventus.server.voice.datatypes;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import gvjava.org.json.JSONException;
import gvjava.org.json.JSONObject;

import com.techventus.server.voice.util.ParsingUtil;

/**
 *
 */
public class ActiveForwardingId {
	String id;
	boolean disabled;
	
	public ActiveForwardingId(JSONObject jsonObject) throws JSONException {
		id = jsonObject.getString("id");
		disabled = jsonObject.getBoolean("disabled");
	}
	public ActiveForwardingId(String pId, boolean pDisabled) {
		id = pId;
		disabled = pDisabled;
	}

	public final static List<ActiveForwardingId> createActiveForwardingIdListFromJsonPartResponse(String jsonPart){ 
		//TODO do with json parser
		List<ActiveForwardingId> activeForwardingIds = new ArrayList<ActiveForwardingId>();
		if(jsonPart!=null &! jsonPart.equals("")) {
			jsonPart = jsonPart.replaceAll(",\"", ",#");
			String[] activeForwardingIdsStrings = jsonPart.split(Pattern.quote(","));
			for (int j = 0; j < activeForwardingIdsStrings.length; j++) {			
				String gId = ParsingUtil.removeUninterestingParts(activeForwardingIdsStrings[j], "\"", "\"", false);
				boolean gState = Boolean.parseBoolean(activeForwardingIdsStrings[j].substring(activeForwardingIdsStrings[j].indexOf(":")+1));
				activeForwardingIds.add(new ActiveForwardingId(gId, gState));
			}
		}
		return activeForwardingIds;
	}
	
	public String toString() {
		try {
			return getAsJsonObject().toString();
		} catch (JSONException e) {
			return null;
		}
	}
	
	public JSONObject getAsJsonObject() throws JSONException {
		JSONObject retO = new JSONObject();
		retO.put("id", id);
		retO.put("disabled", disabled);
		return retO;
	}
	public String getId() {
		return id;
	}
	public boolean isDisabled() {
		return disabled;
	}
	public void setId(String id) {
		this.id = id;
	}
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}
	
}
