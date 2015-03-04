package test.datatypes.records;

import java.util.Date;

import junit.framework.Assert;

import org.junit.Test;

import com.techventus.server.voice.datatypes.Contact;
import com.techventus.server.voice.datatypes.records.Call;

/**
 * 
 * @author Brett Futral @ Catalyst IT Services
 *
 */
public class CallTest {
	
	private final static Date DATE = new Date(1321038671000l);
	private final static Contact CONTACT = new Contact("testName", "testID", "testNumber", "testURL");
	
	private Call testCall = new Call("testID", "testTitle", DATE, CONTACT, true);
	
	@Test
	public void testCallToStringOverrideFull() {
		
		Assert.assertEquals("id=testID;title=testTitle;date=Fri Nov 11 11:11:11 PST 2011;contact={id=testID;name=testName;number=testNumber;imageUrl=testURL;};read=true;", testCall.toString());	
		
	}
	
	@Test
	public void testCallToStringOverrideNoID() {
		
		testCall.setId(null);
		
		Assert.assertEquals("title=testTitle;date=Fri Nov 11 11:11:11 PST 2011;contact={id=testID;name=testName;number=testNumber;imageUrl=testURL;};read=true;", testCall.toString());

	}
	
	@Test
	public void testCallToStringOverrideNoTitle() {
		
		testCall.setTitle(null);
		
		Assert.assertEquals("id=testID;date=Fri Nov 11 11:11:11 PST 2011;contact={id=testID;name=testName;number=testNumber;imageUrl=testURL;};read=true;", testCall.toString());
	}
	
	@Test
	public void testCallToStringOverrideNoDate() {
		
		testCall.setDate(null);
		
		Assert.assertEquals("id=testID;title=testTitle;contact={id=testID;name=testName;number=testNumber;imageUrl=testURL;};read=true;", testCall.toString());
	}
	
	@Test
	public void testCallToStringOverrideNoContact() {
		
		testCall.setContact(null);
		
		Assert.assertEquals("id=testID;title=testTitle;date=Fri Nov 11 11:11:11 PST 2011;read=true;", testCall.toString());
	}

}
