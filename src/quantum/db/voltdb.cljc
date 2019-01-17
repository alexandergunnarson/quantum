(ns quantum.db.voltdb
          (:require
            [com.stuartsierra.component :as comp])
  #?(:clj (:import
            [org.voltdb SQLStmt]
            [org.voltdb.client Client ClientConfig ClientFactory])))

(do

(defrecord VoltDB [^Client client host username password]
  comp/Lifecycle
  (start [this]
    (let [^String username (or username "")
          ^String password (or password "")
          config (ClientConfig. username password)
          _ (.setTopologyChangeAware config true)
          client (ClientFactory/createClient config)]
      (.createConnection client (or host "localhost"))
      (assoc this :client client)))
  (stop [this]
    (.drain client)
    (.close client)
    (assoc this :client nil)))

(let [{:as db :keys [^Client client]} (comp/start (map->VoltDB {}))]
  (try
    ( client)
    (finally (comp/stop db)))))


public final SQLStmt GetSeats = new SQLStmt(
        "SELECT numberofseats FROM Flight WHERE flightid=?;");

voltQueueSQL(GetSeats, EXPECT_ONE_ROW, flightid);
VoltTable[] recordset = voltExecuteSQL();


VoltTable[] results;

try { results = client.callProcedure("LookupFlight",              1
                                     origin,
                                     dest,
                                     departtime).getResults();    2
} catch (Exception e) {                                           3
     e.printStackTrace();
     System.exit(-1);
}


Asynchronous Invocation
To invoke stored procedures asynchronously, use the callProcedure() method with an additional first argument, a callback that will be notified when the procedure completes (or an error occurs). For example, to invoke a NewCustomer() stored procedure asynchronously, the call to callProcedure() might look like the following:

client.callProcedure(new MyCallback(),
                     "NewCustomer",
                     firstname,
                     lastname,
                     custID};
