package test.datatypes.records;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.techventus.server.voice.datatypes.Contact;
import com.techventus.server.voice.datatypes.records.Call;
import com.techventus.server.voice.datatypes.records.Record;
import com.techventus.server.voice.datatypes.records.ShortMessage;
import com.techventus.server.voice.datatypes.records.Transcript;
import com.techventus.server.voice.datatypes.records.TranscriptElement;
import com.techventus.server.voice.datatypes.records.Voicemail;

/**
 * 
 * @author Brett Futral @ Catalyst IT Services
 *
 */
public class RecordTest {
	//testRecords
	Record testRecord; 
	Record testRecord1; 
	Record testRecord2; 
	
	Transcript testTranscript; 
	
	//params for testRecords
	final static Contact CONTACT = new Contact("testName", "testID", "testNumber", 
			"testURL");
	final static Date DATE = new Date(1321038671000l); 
	final static TranscriptElement.RecognitionLevel TEST_LEVEL = TranscriptElement.RecognitionLevel.HIGH; 
	final static TranscriptElement TEST_ELEMENT = new TranscriptElement("testText", 
			"testID", TEST_LEVEL);
	final List<TranscriptElement> testList = new ArrayList<TranscriptElement>(); 
	
	@Before
	public void setUp() {
		
		testList.add(TEST_ELEMENT);
		
		testTranscript = new Transcript(testList);
		
		testRecord = new Voicemail("ID1", "testTitle", DATE, CONTACT, testTranscript, true);
		testRecord1 = new Call("ID1", "testTitle", DATE, CONTACT, true);
		testRecord2 = new ShortMessage("ID1", "testTitle", DATE, CONTACT, true);
	}

	@Test
	public void testIsVoicMailFalseCall() {

		final boolean isVMCallFalse = testRecord1.isVoicemail();

		Assert.assertEquals(false, isVMCallFalse);
	}

	@Test
	public void testIsVoicMailFalseShortMessage() {

		final boolean isVMSMFalse = testRecord2.isVoicemail();

		Assert.assertEquals(false, isVMSMFalse);
	}
	
	@Test
	public void testIsVoicMailTrue() {

		final boolean isVMTrue = testRecord.isVoicemail();

		Assert.assertEquals(true, isVMTrue);
	}

	@Test
	public void testIsCallFalseVoicemail() {

		final boolean isCallVMFalse = testRecord.isCall();

		Assert.assertEquals(false, isCallVMFalse);
	}
	
	@Test
	public void testIsCallFalseShortMessage() {

		final boolean isCallSMFalse = testRecord2.isCall();

		Assert.assertEquals(false, isCallSMFalse);
	}

	@Test
	public void testIsCallTrue() {

		final boolean isCallTrue = testRecord1.isCall();

		Assert.assertEquals(true, isCallTrue);
	}

	@Test
	public void testIsShortMessageFalseVoicemail() {

		final boolean isSMVMFalse = testRecord.isShortMessage();

		Assert.assertEquals(false, isSMVMFalse);
	}
	
	@Test
	public void testIsShortMessageFalseCall() {

		final boolean isSMCallFalse = testRecord1.isShortMessage();

		Assert.assertEquals(false, isSMCallFalse);
	}

	@Test
	public void testIsShortMessageTrue() {

		final boolean isSMTrue = testRecord2.isShortMessage();

		Assert.assertEquals(true, isSMTrue);
	}

}
