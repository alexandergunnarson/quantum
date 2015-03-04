package com.techventus.server.voice.datatypes.records;

import com.techventus.server.voice.util.ParsingUtil;

/**
 * One Element in a Transcript - normally a word.
 * Each Element has a ReccognitionLevel indicating how sure google is 
 * that the word is recognised correctly.
 * 
 * @author Tobias Eisentraeger
 *
 */
public class TranscriptElement {
	private String text;
	private String id;
	private RecognitionLevel level;
	public enum RecognitionLevel {
		MED1,
		MED2,
		HIGH,
		UNKNOWN;
	}
	
	/**
	 * Standard constructor
	 * @param text
	 * @param id
	 * @param level
	 */
	public TranscriptElement(String text, String id, RecognitionLevel level) {
		this.text = text;
		this.id = id;
		this.level = level;
	}
	
	/**
	 * Creates a TranscriptElement based on the html, for example:
	 * <span id="0-33" class="gc-word-med1">Hello World!</span>
	 * @param html
	 */
	public static TranscriptElement extractTransscriptElement(String html) {
		String lId;
		try {
			lId = ParsingUtil.removeUninterestingParts(html,"id=\"","\"",false);  
		} catch (Exception e) {
			lId = "";
		}
		
		String levelSt;
		try {
			levelSt = ParsingUtil.removeUninterestingParts(html,"class=\"gc-word-","\"",false); 	
		} catch (Exception e) {
			levelSt = "";
		}
		
		String ltext;
		try {
			ltext = ParsingUtil.removeUninterestingParts(html,">","</span>",false); 
			ltext = ParsingUtil.htmlEntitiesDecode(ltext);
		} catch (Exception e) {
			ltext = "";
		}
		
		if(levelSt!=null) {
			if(levelSt.equals("med1")) {
				return new TranscriptElement(ltext, lId, RecognitionLevel.MED1);
			} else if(levelSt.equals("med2")) {
				return new TranscriptElement(ltext, lId, RecognitionLevel.MED2);
			} else if(levelSt.equals("high")) {
				return new TranscriptElement(ltext, lId, RecognitionLevel.HIGH);
			} else {
				return new TranscriptElement(ltext, lId, RecognitionLevel.UNKNOWN);
			}
		} else {
			return null;
		}
	}

	/**
	 * @return the text
	 */
	public String getText() {
		return text;
	}

	/**
	 * @param text the text to set
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the level of recognition - How sure is it that the word is recognized correctly
	 */
	public RecognitionLevel getLevel() {
		return level;
	}

	/**
	 * @param level  of recognition - How sure is it that the word is recognized correctly - usage: RecognitionLevel.MED1
	 */
	public void setLevel(RecognitionLevel level) {
		this.level = level;
	}
	
	
}
