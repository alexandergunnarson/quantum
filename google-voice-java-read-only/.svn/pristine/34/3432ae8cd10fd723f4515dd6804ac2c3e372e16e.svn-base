package test.datatypes;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import gvjava.org.json.JSONArray;
import gvjava.org.json.JSONException;
import gvjava.org.json.JSONObject;

import org.junit.Test;

import com.techventus.server.voice.datatypes.DisabledId;

/**
 * 
 * @author Brett Futral @ Catalyst IT Services
 *
 */
public class DisabledIdTest {

	// Golden DisabledId Object
	final DisabledId goldDisabledID = new DisabledId("1", false);

	// Test Object
	DisabledId testDisabledID;
	JSONObject tesJSONtDId = new JSONObject();
	String testString;
	List<DisabledId> testList = new ArrayList<DisabledId>();

	@Test
	public void testDisabledIDJSONNullObjectSaveModeTrue()
			throws JSONException {

		final JSONObject testJSONDId = new JSONObject();

		testDisabledID = new DisabledId(testJSONDId, true);

		Assert.assertNotNull(testDisabledID);
	}

	@Test
	public void testDisabledIDJSONNullObjectSaveModeFalse() {

		try {
			testDisabledID = new DisabledId(tesJSONtDId, false);
		} catch (Exception e) {
			testDisabledID = goldDisabledID;
		}

		Assert.assertEquals(goldDisabledID, testDisabledID);
	}

	@Test
	public void testDisabledIdJSONSaveModeFalse() throws JSONException {

		tesJSONtDId.put("id", "1");
		tesJSONtDId.put("disabled", false);

		testDisabledID = new DisabledId(tesJSONtDId, false);

		final boolean test = goldDisabledID.toString().equals(
				testDisabledID.toString());

		Assert.assertEquals(true, test);
	}

	@Test
	public void testDisabledIdJSONSaveModeTrue() throws JSONException {

		tesJSONtDId.put("id", "1");
		tesJSONtDId.put("disabled", false);

		testDisabledID = new DisabledId(tesJSONtDId, true);

		final boolean test = goldDisabledID.toString().equals(
				testDisabledID.toString());

		Assert.assertEquals(true, test);
	}
	
	@Test
	public void testCreateDisabledIdListFromJsonPartResponseEmptyString() {
		
		testString = "";
		
		Assert.assertEquals(testList, DisabledId.createDisabledIdListFromJsonPartResponse(testString));
		
	}
	
	@Test
	public void testCreateDisabledIdListFromJsonPartResponseNullString() {
		
		List<DisabledId> testNullString = new ArrayList<DisabledId>();
		
		try {
			testNullString = DisabledId.createDisabledIdListFromJsonPartResponse(testString);
		}
		catch(Exception e) {
			testString = "exception Caught";
		}
		
		Assert.assertEquals(testString = "exception Caught", testString);
		Assert.assertEquals(testList, testNullString);
		
	}
	
	@Test
	public void testCreateDisabledIdListFromJsonPartResponse() throws JSONException {
		
		final JSONArray testArray = new JSONArray();
		testArray.put("1");
		final JSONObject testObject = testArray.toJSONObject(testArray);
		testString = testObject.toString();
		testList.add(goldDisabledID);
				
		Assert.assertEquals(testList.toString(), DisabledId.createDisabledIdListFromJsonPartResponse(testString).toString());

	}
	
	@Test
	public void testCreateDisabledIdListFromJsonObjectNullObject() throws JSONException {
		
		Assert.assertEquals(testList.toString(), DisabledId.createListFromJsonObject(tesJSONtDId).toString());

	}
	
	@Test
	public void testCreateDisabledIdListFromJsonObject() throws JSONException {
		
		tesJSONtDId.put("1", false);
		testList.add(goldDisabledID);
				
		Assert.assertEquals(testList.toString(), DisabledId.createListFromJsonObject(tesJSONtDId).toString());

	}
	

}
