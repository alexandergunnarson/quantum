public static var adminUrl = "http://newmarket.siretechnologies.com/sire/administrator/licenseconnections.aspx";
public static var adminKey = "f0ec6a575a984fb89076442323108f36";
public static var connectionString = "Server=tcp:urb3qb5npi.database.windows.net,1433;Database=NewMarketPublic;User ID=PrimeGov@urb3qb5npi;Password=Prime2014;Encrypt=True;TrustServerCertificate=False;Connection Timeout=30;";
public static String retrieveSessionKey () {
    var response = LHttp.request(adminUrl, "get", null, C.hashMap("sun", adminKey), null);
    var htmlNode = HTML.parse(response);
    Predicate<HtmlNode.Tag> pred = (HtmlNode.Tag node) => {
        return (C.get(node.Attributes, "id") == "Grid_1");
    };
    
    var filtered = HTML.filter(htmlNode, pred);
    HtmlNode.Tag nodeF = (HtmlNode.Tag)C.first(HTML.filter(htmlNode, pred));
    if (C.empty(filtered)) {
        throw (new Exception("No grid tag found."));
    }
    
    return C.get(nodeF.Attributes, "key");
}

protected  void Dump_Click (Object sender, EventArgs e) {
    var sessionKey = retrieveSessionKey();
    var ws = (new SIREService.Service());
    System.Data.DataSet sireReturn = ws.GetMeetings(sessionKey, 2, "Council", "1/1/1900", "1/1/2020");
    Debug.WriteLine("Starting with retrieved session key:", sessionKey);
    Response.Write("<table>");
    foreach (DataRow row in sireReturn.Tables[0].Rows) {
        Response.Write("<tr>");
        Response.Write(("<td>" + C.get(row, "meet_id").ToString() + "</td>"));
        Response.Write(("<td>" + C.get(row, "meet_type").ToString() + "</td>"));
        Response.Write(("<td>" + C.get(row, "meet_date").ToString() + "</td>"));
        Response.Write("</tr>");
        Response.Flush();
    }
    
    Response.Write("</table>");
    
    Response.End();
}

protected  void Import_Click (Object sender, EventArgs e) {
    Debug.WriteLine("Starting");
    var ws = (new SIREService.Service());
    var cn = (new SqlConnection());
    var sk = retrieveSessionKey();
    var headers = C.hashMap("Published_Meetings_Doc", C.vector("doc_id", "vault_id", "security", "date_added", "MEET_ID", "MEET_TYPE", "MEET_DESC", "MEET_DATE", "MEET_TIME", "MEET_LOCATION", "MEET_STATUS", "MEET_PUBLISHED_DATE", "file_count"), "Published_Meetings_Page", C.vector("page_id", "doc_id", "alias", "subdir", "orig_extens", "pagenum", "date_added", "ocr_done", "pdf_done", "fulltext_done", "fulltext_catalog_index", "rev_num", "has_revisions", "checked_out", "pdf_searchable", "page_description", "page_count", "thumb_done", "item_id", "confidential", "item_page_id", "minutes_status", "item_outline", "minutes_confidential"));
    var headersTable = C.hashMap("Published_Meetings_Doc", C.hashMap("security", "VARCHAR(50)", "MEET_STATUS", "VARCHAR(20)", "file_count", "INT", "doc_id", "INT", "MEET_LOCATION", "VARCHAR(50)", "MEET_ID", "INT", "MEET_TYPE", "VARCHAR(50)", "MEET_TIME", "VARCHAR(15)", "MEET_PUBLISHED_DATE", "DATETIME", "MEET_DESC", "VARCHAR(1000)", "date_added", "VARCHAR(10)", "vault_id", "INT", "MEET_DATE", "DATETIME"), "Published_Meetings_Page", C.hashMap("subdir", "INT", "pdf_searchable", "INT", "fulltext_done", "INT", "has_revisions", "INT", "item_outline", "VARCHAR(20)", "page_id", "INT", "pdf_done", "INT", "minutes_status", "INT", "page_count", "INT", "page_description", "VARCHAR(1000)", "doc_id", "INT", "item_page_id", "INT", "confidential", "INT", "checked_out", "INT", "item_id", "INT", "thumb_done", "INT", "minutes_confidential", "INT", "date_added", "VARCHAR(10)", "ocr_done", "INT", "alias", "INT", "rev_num", "INT", "fulltext_catalog_index", "INT", "orig_extens", "VARCHAR(10)", "pagenum", "INT"));
    cn.ConnectionString = connectionString;
    cn.Open();
    
    try {
        foreach (var tableName in headers.Keys) {
            var dataSetN = (new DataSet());
            var sql = ("SELECT * FROM " + tableName);
            var da = (new SqlDataAdapter(sql, cn));
            var cmdBuilder = (new SqlCommandBuilder(da));
            da.Fill(dataSetN);
            Debug.WriteLine("Count before Update:", dataSetN.Tables[0].Rows.Count);
            DB.insertTable(C.get(headers, tableName), C.get(headersTable, tableName), tableName, ws.GetDataset(sk, sql).Tables[0], cn);
        }
    }  finally {
        cn.Close();
        Debug.WriteLine("");
    };
}

public static void insertTable (ImmutableArray<String> headers, ImmutableDictionary<String, String> headersTable, String tableName, DataTable data, SqlConnection conn) {
    var dropStr = ("DROP TABLE [dbo].[" + tableName + "]; ");
    var createStr = "";
    var insertStr = "";
    var valuesStr = "";
    var selectStr = ("SELECT * FROM [dbo].[" + tableName + "];");
    var n = 0;
    // CREATE-STR;
    foreach (var header in headers) {
        createStr = (createStr + "[" + header + "]" + " " + C.get(headersTable, header) + " " + "NULL");
        if ((n != (C.count(headers) - 1))) {
            createStr = (createStr + ", ");
        }
        
        n ++;
    }
    
    createStr = ("CREATE TABLE [dbo].[" + tableName + "] (" + createStr + "); ");
    // INSERT-STR;
    n = 0;
    foreach (var header in headers) {
        insertStr = (insertStr + header);
        if ((n != (C.count(headers) - 1))) {
            insertStr = (insertStr + ", ");
        }
        
        n ++;
    }
    
    insertStr = ("INSERT INTO " + tableName + " (" + insertStr + ") ");
    // VALUES-STR;
    n = 0;
    foreach (var header in headers) {
        valuesStr = (valuesStr + "@" + header);
        if ((n != (C.count(headers) - 1))) {
            valuesStr = (valuesStr + ", ");
        }
        
        n ++;
    }
    
    valuesStr = ("VALUES (" + valuesStr + ");");
    insertStr = (insertStr + valuesStr);
    
    var dropCommand = (new SqlCommand(dropStr, conn));
    var createCommand = (new SqlCommand(createStr, conn));
    var selectCommand = (new SqlCommand(selectStr, conn));
    try {
        dropCommand.ExecuteNonQuery();
    } catch (SqlException) {
        _;
    } ;
    
    createCommand.ExecuteNonQuery();
    foreach (DataRow row in data.Rows) {
        SqlCommand insertCommand = (new SqlCommand(insertStr, conn));
        foreach (var header in headers) {
            insertCommand.Parameters.AddWithValue(("@" + header), row[header]);
        }
        
        Debug.WriteLine("INSERTED ROW");
        insertCommand.ExecuteNonQuery();
    }
    
    Debug.WriteLine("SELECTING FROM DATABASE IS:");
    printResults(selectCommand);
    Debug.WriteLine("========");;
}

protected  void VideoDownload_Click (Object sender, EventArgs e) {
    var conn = (new SqlConnection());
    var tableName = "Published_Meetings_Doc";
    var sql = ("SELECT * FROM " + tableName);
    var command = (new SqlCommand(sql, conn));
    var dataSetN = (new DataSet());
    var sda = (new SqlDataAdapter(sql, conn));
    var cmdBuilder = (new SqlCommandBuilder(sda));
    conn.ConnectionString = connectionString;
    conn.Open();
    sda.Fill(dataSetN);
    foreach (DataRow row in dataSetN.Tables[0].Rows) {
        Response.Write("<tr>");
        Response.Write(("<td>" + C.get(row, "MEET_ID").ToString() + "</td>"));
        Response.Write(("<td>" + C.get(row, "MEET_TYPE").ToString() + "</td>"));
        Response.Write("</tr>");
        Response.Flush();
    }
    
    Response.Write("</table>");
    Response.End();
    
    conn.Close();
}