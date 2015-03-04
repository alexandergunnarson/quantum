package test.datatypes.records;

import java.util.Date;

import junit.framework.Assert;

import org.junit.Test;

import com.techventus.server.voice.datatypes.Contact;
import com.techventus.server.voice.datatypes.records.SMS;

/**
 * 
 * @author Brett Futral @ Catalyst IT Services
 *
 */
public class SMSTest {
	//Params for testSMSs
	final Contact CONTACT = new Contact("testName", "testID", "testNumber",
			"testURL");
	final Contact CONTACT1 = new Contact("testName1", "testID1", "testNumber1",
			"testURL1");
	final Date DATE = new Date(1321038671000l);
	final Date DATE1 = new Date(1320038671000l);
	//testSMSs toCompare
	final SMS testSMS = new SMS(CONTACT, "testContent", DATE);
	final SMS testSMS1 = new SMS(CONTACT1, "testContent1", DATE1);
	final SMS testSMS2 = new SMS(CONTACT1, "testContent1", DATE);
	final SMS testSMS3 = new SMS(CONTACT, "testContent1", DATE);

	@Test
	public void testCompareToOverrideDifferentDate() {
		
		Assert.assertEquals(-1, testSMS.compareTo(testSMS1));
	}
	
	@Test
	public void testCompareToOverrideSameDateDifferentFrom() {
		
		Assert.assertEquals(-1, testSMS.compareTo(testSMS2));
	}
	
	@Test
	public void testCompareToOverrideSameDateSameFrom() {
		
		Assert.assertEquals(1, testSMS.compareTo(testSMS3));
	}
	
	@Test
	public void testCompareToOverrideSameSMS() {
		
		Assert.assertEquals(0, testSMS.compareTo(testSMS));
	}
	
}
