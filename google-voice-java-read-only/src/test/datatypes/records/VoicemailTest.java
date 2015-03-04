package test.datatypes.records;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.techventus.server.voice.datatypes.Contact;
import com.techventus.server.voice.datatypes.records.Transcript;
import com.techventus.server.voice.datatypes.records.TranscriptElement;
import com.techventus.server.voice.datatypes.records.Voicemail;

/**
 * 
 * @author Brett Futral @ Catalyst IT Services
 *
 */
public class VoicemailTest {

	Voicemail testVoicemail;
	// Params for testVoicemail
	final Date DATE = new Date(1321038671000l);
	final Contact CONTACT = new Contact("testName", "testID", "testNumber",
			"testURL");
	final TranscriptElement.RecognitionLevel testLevel = TranscriptElement.RecognitionLevel.HIGH;
	final TranscriptElement testElement = new TranscriptElement("testText",
			"testID", testLevel);

	@Before
	public void setUp() {
		//Params for testVoicemail
		final List<TranscriptElement> testList = new ArrayList<TranscriptElement>();
		testList.add(testElement);
		final Transcript TRANSCRIPT = new Transcript(testList);
		//instantiate testVoicemail
		testVoicemail = new Voicemail("testID", "testTitle", DATE, CONTACT, TRANSCRIPT,
				true);
	}

	@Test
	public void testToStringOverrideFull() {

		Assert.assertEquals(
				"id=testID;title=testTitle;date=Fri Nov 11 11:11:11 PST 2011;contact={id=testID;name=testName;number=testNumber;imageUrl=testURL;};transcript=testText;mp3Url=https://www.google.com/voice/media/send_voicemail/testID;read=true;",
				testVoicemail.toString());
	}
	
	@Test
	public void testToStringOverrideNoID() {
		
		testVoicemail.setId(null);

		Assert.assertEquals(
				"title=testTitle;date=Fri Nov 11 11:11:11 PST 2011;contact={id=testID;name=testName;number=testNumber;imageUrl=testURL;};transcript=testText;mp3Url=https://www.google.com/voice/media/send_voicemail/null;read=true;",
				testVoicemail.toString());
	}

	@Test
	public void testToStringOverrideNoTitle() {
		
		testVoicemail.setTitle(null);

		Assert.assertEquals(
				"id=testID;date=Fri Nov 11 11:11:11 PST 2011;contact={id=testID;name=testName;number=testNumber;imageUrl=testURL;};transcript=testText;mp3Url=https://www.google.com/voice/media/send_voicemail/testID;read=true;",
				testVoicemail.toString());
	}
	
	@Test
	public void testToStringOverrideNoDate() {
		
		testVoicemail.setDate(null);
		
		Assert.assertEquals(
				"id=testID;title=testTitle;contact={id=testID;name=testName;number=testNumber;imageUrl=testURL;};transcript=testText;mp3Url=https://www.google.com/voice/media/send_voicemail/testID;read=true;",
				testVoicemail.toString());
	}
	
	@Test
	public void testToStringOverrideContact() {
		
		testVoicemail.setContact(null);

		Assert.assertEquals(
				"id=testID;title=testTitle;date=Fri Nov 11 11:11:11 PST 2011;transcript=testText;mp3Url=https://www.google.com/voice/media/send_voicemail/testID;read=true;",
				testVoicemail.toString());
	}
	
	@Test
	public void testToStringOverrideNoTranscript() {
		
		testVoicemail.setTransscript(null);

		Assert.assertEquals(
				"id=testID;title=testTitle;date=Fri Nov 11 11:11:11 PST 2011;contact={id=testID;name=testName;number=testNumber;imageUrl=testURL;};mp3Url=https://www.google.com/voice/media/send_voicemail/testID;read=true;",
				testVoicemail.toString());
	}
	
	@Test
	public void testToStringOverrideNoMP3() {
		
		testVoicemail.setMp3Url(null);

		Assert.assertEquals(
				"id=testID;title=testTitle;date=Fri Nov 11 11:11:11 PST 2011;contact={id=testID;name=testName;number=testNumber;imageUrl=testURL;};transcript=testText;read=true;",
				testVoicemail.toString());
	}

}
