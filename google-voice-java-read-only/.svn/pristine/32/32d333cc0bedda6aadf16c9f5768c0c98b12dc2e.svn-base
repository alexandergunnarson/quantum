/**
 * 
 */
package com.techventus.server.voice.datatypes;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import gvjava.org.json.JSONArray;
import gvjava.org.json.JSONException;
import gvjava.org.json.JSONObject;

import com.techventus.server.voice.util.ParsingUtil;

/**
 * Holds the settings of the disabledIdMap json object
 */
public class DisabledId {
	String id;
	boolean disabled;
	
	/**
	 * 
	 * @param pId
	 * @param pDisabled
	 */
	public DisabledId(String pId, boolean pDisabled) {
		id = pId;
		disabled = pDisabled;
	}
	
	/**
	 * 
	 * @param jsonObject
	 * @param saveMode
	 * @throws JSONException
	 */
	public DisabledId(JSONObject jsonObject, boolean saveMode) throws JSONException {
		if(!saveMode || saveMode && jsonObject.has("id")) id = jsonObject.getString("id");
		if(!saveMode || saveMode && jsonObject.has("disabled")) disabled = jsonObject.getBoolean("disabled");
	}
	
	public String toString() {
		String ret="{id="+id+";";
		ret+="disabled="+disabled+"}";	
		return ret;
	}
	
	/**
	 * 
	 * @param jsonPart
	 * @return List<DisabledId>
	 */
	public final static List<DisabledId> createDisabledIdListFromJsonPartResponse(String jsonPart) { 
		List<DisabledId> disabledIds = new ArrayList<DisabledId>();
		if(jsonPart!=null &! jsonPart.equals("")) {
			jsonPart = jsonPart.replaceAll(",\"", ",#");
			String[] disabledIdsStrings = jsonPart.split(Pattern.quote(","));
			for (int j = 0; j < disabledIdsStrings.length; j++) {			
				String gId = ParsingUtil.removeUninterestingParts(disabledIdsStrings[j], "\"", "\"", false);
				boolean gState = Boolean.parseBoolean(disabledIdsStrings[j].substring(disabledIdsStrings[j].indexOf(":")+1));
				disabledIds.add(new DisabledId(gId, gState));
			}
		}
		return disabledIds;
	}
	
	/**
	 * 
	 * @param disabledIdMapJSON
	 * @return List<DisabledId>
	 * @throws JSONException
	 */
	public final static List<DisabledId> createListFromJsonObject(JSONObject disabledIdMapJSON) throws JSONException { 
		List<DisabledId> disabledIds = new ArrayList<DisabledId>();
		JSONArray disabledNames = disabledIdMapJSON.names();
		if(disabledNames!=null) {
			for (int i = 0; i < disabledNames.length(); i++) {
				String id = disabledNames.getString(i);
				boolean booleanValue = disabledIdMapJSON.getBoolean(id);
				disabledIds.add(new DisabledId(id,booleanValue));
			}
		} // if null, then no phones are disabled.
		return disabledIds;
		/*		
		if(settingsJSON.has("disabledIdMap")) {
			JSONArray lArray;
			try {
				lArray = settingsJSON.getJSONArray("disabledIdMap");
//				JSONArray objectNames = lArray.names();
				
				for (int i = 0; i < lArray.length(); i++) {
					String lId = lArray.getJSONObject(i).getString(i+"");
					boolean lDisabled;
					try {
						lDisabled = lArray.getJSONObject(i).getBoolean(lId);
						disabledIds.add(new DisabledId(lId, lDisabled));
					} catch (JSONException e) {
						// Nothing - will not add at exception
					}
				}
			} catch (JSONException e1) {
				// Nothing - will return empty array at exception
			}

		}
		
		return disabledIds;
		*/
	}
	
	/**
	 * 
	 * @param settingsJSON
	 * @return DisabledId[]
	 * @throws JSONException
	 */
	//TODO dotn create list first, direct transform
	public final static DisabledId[] createArrayFromJsonObject(JSONObject settingsJSON) throws JSONException { 
		List<DisabledId> tList = createListFromJsonObject(settingsJSON.getJSONObject("disabledIdMap"));
		return (DisabledId[]) tList.toArray(new DisabledId[tList.size()]);
	}
	
	/**
	 * @return "1":true
	 */
	public String toJson() {
		return "\""+id+"\":"+disabled;
	}
	
	public String getId() {
		return id;
	}
	
	public int getIdAsInt() {
		int ret;
		try {
			ret = Integer.parseInt(id);
		} catch (Exception e) {
			ret = -1;
		}
		return ret;
	}
	
	public boolean isDisabled() {
		return disabled;
	}
	
	public JSONObject toJsonObject(){
		JSONObject resultO = new JSONObject();
		try { 		
			resultO.accumulate(id, disabled);
		} catch (JSONException e) {
			return null;
		}
		
		return resultO;
	}
	
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * @param disabled the disabled to set
	 */
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}
	
}
