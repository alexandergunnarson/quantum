package test.util;

import com.techventus.server.voice.datatypes.records.SMSThread;
import com.techventus.server.voice.util.SMSParser;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Brett Futral @ Catalyst IT Services
 * 
 */
// Holes in coverage:
// SMSParser.parsePhoneNumber line 258 will always == -1 due to line
// regex filter in line 257)
// Cannot test getSMSThreads due to private method call inside method.
// Cannot test parseContact due to private method call inside method.
// addSMSsToThread is void
@SuppressWarnings("rawtypes")
public class SMSParserTest {

	// TestSMSParser
	final SMSParser testSMSParser = new SMSParser("test", "5030000000");
	private static final long DATE_LONG = 1334348667447L;

	// Reflection Setup buildSMSThreadMap
	private Method bSMSTM;
	private static final String TEST_PPN = "parsePhoneNumber";
	private static final String TEST_BSMSTM = "buildSMSThreadMap";
	private Class[] bSMSTMParamTypes = new Class[1];
	private Object[] bSMSTMParams = new Object[1];

	@Before
	public void setUp() throws NoSuchMethodException, SecurityException {

		// Reflect class/method buildSMSThreadMap
		bSMSTMParamTypes[0] = java.lang.String.class;
		bSMSTM = testSMSParser.getClass().getDeclaredMethod(TEST_BSMSTM,
				bSMSTMParamTypes);
		bSMSTM.setAccessible(true);
	}

	@Test
	public void testParsePhoneNumber() throws NoSuchMethodException,
			SecurityException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {

		// Reflect class/method parsePhoneNumber
		Method pPN;
		Class[] pPNParamTypes = new Class[1];
		Object[] pPNParams = new Object[1];
		pPNParamTypes[0] = java.lang.String.class;
		pPN = testSMSParser.getClass().getDeclaredMethod(TEST_PPN,
				pPNParamTypes);
		pPN.setAccessible(true);
		pPNParams[0] = "5035551212";

		final String testPPNumber = (String) pPN.invoke(testSMSParser,
				pPNParams);

		Assert.assertEquals("+15035551212", testPPNumber);

	}

	@Test
	public void testBuildSMSThreadMap() throws IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {

		// Reflect class/method parsePhoneNumber
		final String testSMSResponse = "<json><![CDATA[{\"messages\":{\"testID\""
				+ ":{\"id\":\"testID\",\"phoneNumber\":\"+15035551212\",\""
				+ "displayNumber\":\"(503) 555-1212\",\"startTime\":\""
				+ "1334348667447\",\"displayStartDateTime\":\"4/13/12 1:24 PM\""
				+ ",\"displayStartTime\":\"1:24 PM\",\"relativeStartTime\":\""
				+ "3 minutes ago\",\"note\":\"test note\",\"isRead\":true,\""
				+ "isSpam\":false,\"isTrash\":false,\"star\":true,\"messageText\""
				+ ":\"testSMS\",\"labels\":[\"sms\",\"all\"],\"type\":11,\""
				+ "children\":\"\"}},\"totalSize\":1,\"unreadCounts\":{\"all\""
				+ ":0,\"inbox\":0,\"sms\":0,\"unread\":0,\"voicemail\":0},\""
				+ "resultsPerPage\":10}]]></json>";
		bSMSTMParams[0] = testSMSResponse;
		final Map testBSMSTMap = (Map) bSMSTM.invoke(testSMSParser,
				bSMSTMParams);

		// Expected object
		final Date validDate = new Date(DATE_LONG);
		final SMSThread expectedSMSThread = new SMSThread("testID",
				"test note", validDate, null, true, true);
		final Map<String, SMSThread> expectedMap = new HashMap<String, SMSThread>();
		expectedMap.put("testID", expectedSMSThread);

		Assert.assertEquals(expectedMap.toString(), testBSMSTMap.toString());

	}

	@Test
	public void testBuildSMSThreadMapNoInputs() throws IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {

		// Reflect class/method parsePhoneNumber
		final String testSMSResponse = "<json><![CDATA[{\"messages\":{\"testID\""
				+ ":{\"phoneNumber\":\"+15035551212\",\"displayNumber\":\""
				+ "(503) 555-1212\",\"displayStartDateTime\":\"4/13/12 1:24 PM\""
				+ ",\"displayStartTime\":\"1:24 PM\",\"relativeStartTime\":\""
				+ "3 minutes ago\",\"isSpam\":false,\"isTrash\":false,\""
				+ "messageText\":\"testSMS\",\"labels\":[\"sms\",\"all\"],\""
				+ "type\":11,\"children\":\"\"}},\"totalSize\":1,\"unreadCounts\""
				+ ":{\"all\":0,\"inbox\":0,\"sms\":0,\"unread\":0,\"voicemail\""
				+ ":0},\"resultsPerPage\":10}]]></json>"; 
		bSMSTMParams[0] = testSMSResponse;
		final Map testNoInputs = (Map) bSMSTM.invoke(testSMSParser,
				bSMSTMParams);

		// Expected object
		final Date epochDate = new Date(0);
		final SMSThread expectedSMSThread = new SMSThread("", "", epochDate,
				null, false, false);
		final Map<String, SMSThread> expectedMap = new HashMap<String, SMSThread>();
		expectedMap.put("", expectedSMSThread);

		Assert.assertEquals(expectedMap.toString(), testNoInputs.toString());

	}

}
