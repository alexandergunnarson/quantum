package com.techventus.server.voice.util;

import java.util.ArrayList;
import java.util.List;

import gvjava.org.json.JSONArray;
import gvjava.org.json.JSONException;
import gvjava.org.json.JSONObject;

/**
 * Collection of useful html parsing methods
 * 
 * @author Tobias Eisentraeger
 *
 */
public abstract class ParsingUtil {

	/**
	 * Strips the text from the uninteresting parts before and after the interesting part.
	 * The return includes borders if includeBorders == true - Returns null when Exception occures<br/><br/>
	 * Example:<br/>
	 * removeUninterestingParts("Hello Toby  , How are you today? Fine.", "How are", "?" , <b>true</b>)<br/>
	 * Returns: "How are you today?"<br/>
	 * <br/>
	 * removeUninterestingParts("Hello Joseph, How are you today? Fine.", "How are", "?" , <b>false</b>)<br/>
	 * Returns: " you today"
	 *
	 * @param text the text
	 * @param startBorder the start border
	 * @param endBorder the end border
	 * @param includeBorders the include borders
	 * @return the string
	 */
	public static final String removeUninterestingParts(String text, String startBorder, String endBorder, boolean includeBorders) {
		String ret="";
		try {
			if(text!=null&&startBorder!=null&&endBorder!=null&&(text.indexOf(startBorder)!=-1)&&(text.indexOf(endBorder)!=-1) ) {
				
				if(includeBorders) {
					text = text.substring(text.indexOf(startBorder));
					if(text!=null) {
						ret = text.substring(0,text.indexOf(endBorder)+endBorder.length());
					} else {
						ret = null;
					}
				} else {
					text = text.substring(text.indexOf(startBorder)+startBorder.length());
					if(text!=null) {
						ret = text.substring(0,text.indexOf(endBorder));
					} else {
						ret = null;
					}
				}
			
			} else {
				ret = null;
			}
		} catch (Exception e) {
			System.out.println("Exception "+e.getMessage());
			System.out.println("Begin:"+startBorder);
			System.out.println("End:"+endBorder);
			System.out.println("Text:"+text);
			e.printStackTrace();
			ret = null;
		}
		return ret;
	}
	
	
	//TODO use Apache commons StringEscapeUtils.unescapeHTML() ?
	/**
	 * Replaces some speciel htmlEntities with a corresponding String.
	 *
	 * @param s the HTML Entity in String format
	 * @return the Decoded HTML in String format
	 */
	public static String htmlEntitiesDecode(String s) {
		s=s.replaceAll("&#39;", "'"); 
		return s;
	}
	
	/**
	 * Json int array to int array.
	 * 
	 * @param array the array
	 * @return the int[]
	 */
	public static final int[] jsonIntArrayToIntArray(JSONArray array) {
		int[] result = new int[array.length()];
		for (int i = 0; i < array.length(); i++) {
			try {
				result[i] = array.getInt(i);
			} catch (JSONException e) {
				return null;
			}
		}
		return result;
	}
	
	/**
	 * Json string array to string array.
	 * 
	 * @param array the array
	 * @return the string[]
	 */
	public static final String[] jsonStringArrayToStringArray(JSONArray array) {
		String[] result = new String[array.length()];
		for (int i = 0; i < array.length(); i++) {
			try {
				result[i] = array.getString(i);
			} catch (JSONException e) {
				return null;
			}
		}
		return result;
	}
	
	/**
	 * Json string array to string list.
	 * 
	 * @param settingsJSON the settings json
	 * @param stringList the string list
	 * @param key the key
	 * @return the list
	 * @throws JSONException the jSON exception
	 */
	public static final List<String> jsonStringArrayToStringList(JSONObject settingsJSON, List<String> stringList, String key) throws JSONException {
		stringList = new ArrayList<String>();
		for (int i = 0; i < ((JSONArray) settingsJSON.get(key)).length(); i++) {
			stringList.add(((JSONArray) settingsJSON.get(key)).getString(i));
		}
		return stringList;
	}
	
	/**
	 * Converts a Json Integer array to an ArrayList of Integers.
	 * 
	 * @param settingsJSON the settings json
	 * @param integerList the integer list
	 * @param key the key corresponding to the JSON formatted integer array
	 * @return the list
	 * @throws JSONException the jSON exception
	 */
	public static final List<Integer> jsonIntArrayToIntegerList(JSONObject settingsJSON, List<Integer> integerList, String key) throws JSONException {
		//TODO Why are we taking integerList as input, if we replace with new one?
		integerList = new ArrayList<Integer>();
		for (int i = 0; i < ((JSONArray) settingsJSON.get(key)).length(); i++) {
			integerList.add(((JSONArray) settingsJSON.get(key)).getInt(i));
		}
		return integerList;
	}
	
	/**
	 * String list to JSON array.
	 * 
	 * @param stringList the string list input
	 * @return the JSON array
	 * @throws JSONException the JSON exception
	 */
	public static final JSONArray stringListToJsonArray(List<String> stringList) throws JSONException {	
		String[] lArray = (String[]) stringList.toArray(new String[stringList.size()]);
		return new JSONArray(lArray);
	}
	
	
}