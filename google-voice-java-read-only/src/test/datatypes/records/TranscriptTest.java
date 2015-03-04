package test.datatypes.records;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.techventus.server.voice.datatypes.records.Transcript;
import com.techventus.server.voice.datatypes.records.TranscriptElement;

/**
 * 
 * @author Brett Futral @ Catalyst IT Services
 *
 */
public class TranscriptTest {
	// testTranscripts
	Transcript testTranscript;
	Transcript testTranscript1;
	// Params for testTranscripts
	final TranscriptElement.RecognitionLevel testLevel = TranscriptElement.RecognitionLevel.HIGH;
	final TranscriptElement.RecognitionLevel testLevel1 = TranscriptElement.RecognitionLevel.MED1;
	final TranscriptElement testElement = new TranscriptElement("testText",
			"testID", testLevel);
	final TranscriptElement testElement1 = new TranscriptElement(null, null,
			null);

	List<TranscriptElement> testList = new ArrayList<TranscriptElement>();
	
	@Test
	public void testToStringOverrideOneNullElement() {
		testList.add(null);
		testTranscript = new Transcript(testList);
		Assert.assertEquals("", testTranscript.toString());
	}
	
	@Test
	public void testToStringOverrideTwoElementsOneNull() {
		testList.add(null);
		testList.add(testElement);
		testTranscript = new Transcript(testList);
		Assert.assertEquals("testText", testTranscript.toString());
	}
	
	@Test
	public void testToStringOverrideOneElement() {
		
		testList.add(testElement);
		testTranscript = new Transcript(testList);

		Assert.assertEquals("testText", testTranscript.toString());
	}

	@Test
	public void testToStringOverrideTwoElements() {
		
		testList.add(testElement);
		testList.add(testElement1);
		
		testTranscript = new Transcript(testList);

		Assert.assertEquals("testText null", testTranscript.toString());

	}

}
