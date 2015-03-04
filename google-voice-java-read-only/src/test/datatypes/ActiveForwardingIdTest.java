package test.datatypes;

import com.techventus.server.voice.datatypes.ActiveForwardingId;

import gvjava.org.json.JSONArray;
import gvjava.org.json.JSONException;
import gvjava.org.json.JSONObject;
import junit.framework.Assert;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Brendon Shih - Catalyst IT Services
 * 
 */
public class ActiveForwardingIdTest {

	List<ActiveForwardingId> testList = new ArrayList<ActiveForwardingId>();
	String testString;

	@Test
	public void testCreateActiveForwardingIdListFromJsonPartResponseEmptyString() {
		testString = "";
		Assert.assertEquals(testList, ActiveForwardingId
				.createActiveForwardingIdListFromJsonPartResponse(testString));
	}

	@Test
	public void testCreateActiveForwardingIdListFromJsonPartResponseNullString() {

		List<ActiveForwardingId> testNullList = new ArrayList<ActiveForwardingId>();

		try {
			testNullList = ActiveForwardingId
					.createActiveForwardingIdListFromJsonPartResponse(testString);
		} catch (Exception e) {
			testNullList = testList;
		}

		Assert.assertEquals(testList, testNullList);

	}

	@Test
	public void testCreateActiveForwardingIdListFromJsonPartResponse()
			throws JSONException {

		final JSONArray testArray = new JSONArray();
		testArray.put("1");
		final JSONObject testObject = testArray.toJSONObject(testArray);
		testString = testObject.toString();

		final JSONObject testAFIDObject = new JSONObject();
		testAFIDObject.put("id", 1);
		testAFIDObject.put("disabled", false);
		final ActiveForwardingId testAFID = new ActiveForwardingId(testAFIDObject);
		testList.add(testAFID);

		Assert.assertEquals(testList.toString(), ActiveForwardingId
				.createActiveForwardingIdListFromJsonPartResponse(testString)
				.toString());

	}

}
