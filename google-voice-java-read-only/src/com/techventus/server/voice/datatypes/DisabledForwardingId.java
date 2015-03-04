/**
 * 
 */
package com.techventus.server.voice.datatypes;

import gvjava.org.json.JSONException;
import gvjava.org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class DisabledForwardingId {
	String id;
	boolean disabled;
	public DisabledForwardingId(String pId, boolean pDisabled) {
		id = pId;
		disabled = pDisabled;
	}
	public DisabledForwardingId(JSONObject jsonObject, boolean saveMode) throws JSONException {
		if(!saveMode || saveMode && jsonObject.has("id")) id = jsonObject.getString("id");
		if(!saveMode || saveMode && jsonObject.has("disabled")) disabled = jsonObject.getBoolean("disabled");
	}
	public String toString() {
		String ret="{id="+id+";";
		ret+="disabled="+disabled+"}";	
		return ret;
	}
	public final static List<DisabledForwardingId> createDisabledForwardingIdListFromJsonPartResponse(String jsonPart) { 	
		List<DisabledForwardingId> disabledForwardingIds = new ArrayList<DisabledForwardingId>();
		try {
			String[] disNames = JSONObject.getNames(new JSONObject(jsonPart));
			for (int i = 0; i < disNames.length; i++) {
				DisabledForwardingId dis = new DisabledForwardingId(disNames[i], true);
				disabledForwardingIds.add(dis);
			}
		} catch (Exception e) {
			// nothing on parse error
		}
		return disabledForwardingIds;
		/*
		if(jsonPart!=null &! jsonPart.equals("")) {
			jsonPart = jsonPart.replaceAll(",\"", ",#");
			String[] disabledForwardingIdsStrings = jsonPart.split(Pattern.quote(","));
			for (int j = 0; j < disabledForwardingIdsStrings.length; j++) {			
				try {
					String gId = ParsingUtil.removeUninterestingParts(disabledForwardingIdsStrings[j], "\"", "\"", false);
					boolean gState = Boolean.parseBoolean(disabledForwardingIdsStrings[j].substring(disabledForwardingIdsStrings[j].indexOf(":")+1,disabledForwardingIdsStrings[j].indexOf("}")));
					if(gId!=null) {
						DisabledForwardingId dis = new DisabledForwardingId(gId, gState);
						disabledForwardingIds.add(dis);
					}
				} catch (StringIndexOutOfBoundsException e) {
					// do nothing if exception
				}
			}
		}
		return disabledForwardingIds;
		*/
	}
	
	public final static DisabledForwardingId[] createDisabledForwardingIdArrayFromJsonPartResponse(String jsonPart) {
		List<DisabledForwardingId> list = createDisabledForwardingIdListFromJsonPartResponse(jsonPart);
		DisabledForwardingId[] result = new DisabledForwardingId[list.size()];
		for (int i = 0; i < list.size(); i++) {
			result[i] = list.get(i);
		}
		return result;
	}
	
	public String toJson(){	
		return toJsonObject().toString();
	}
	
	public JSONObject toJsonObject(){
		JSONObject resultO = new JSONObject();
		try { 		
			resultO.putOpt("id", id);
			resultO.putOpt("disabled", disabled);
		} catch (JSONException e) {
			return null;
		}
		
		return resultO;
	}

	// needs to be {"2": true,"3": true}
	public static Object arrayToJsonObject(List<DisabledForwardingId> disabledForwardingIds) throws JSONException {
		JSONObject obj = new JSONObject();
		for (int i = 0; i < disabledForwardingIds.size(); i++) {
			obj.put(disabledForwardingIds.get(i).getId()+"",disabledForwardingIds.get(i).isDisabled());
		}
		return obj;
	}
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	/**
	 * @return the disabled
	 */
	public boolean isDisabled() {
		return disabled;
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
