// Copyright (c) Cognitect, Inc.
// All rights reserved.

/*
 * To compile, from the top-level project directory
 *
 *    javac -cp `bin/classpath` samples/seattle/GettingStarted.java
 *
 * To run:
 *
 *    java -cp `bin/classpath`:samples/seattle GettingStarted
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;

import java.io.Reader;
import java.io.FileReader;

import datomic.Entity;
import datomic.Connection;
import datomic.Database;
import datomic.Peer;
import datomic.Util;

public class GettingStarted {

    public static void main(String[] args) {

        try {
            System.out.println("Creating and connecting to database...");

            String uri = "datomic:mem://seattle";
            Peer.createDatabase(uri);
            Connection conn = Peer.connect(uri);

            pause();


            System.out.println("Parsing schema edn file and running transaction...");

            Reader schema_rdr = new FileReader("samples/seattle/seattle-schema.edn");
            List schema_tx = (List) Util.readAll(schema_rdr).get(0);
            Object txResult = conn.transact(schema_tx).get();
            System.out.println(txResult);

            pause();


            System.out.println("Parsing seed data edn file and running transaction...");

            Reader data_rdr = new FileReader("samples/seattle/seattle-data0.edn");
            List data_tx = (List) Util.readAll(data_rdr).get(0);
            data_rdr.close();
            txResult = conn.transact(data_tx).get();

            pause();


            System.out.println("Finding all communities, counting results...");

            Collection results = Peer.query("[:find ?c :where [?c :community/name]]", conn.db());
            System.out.println(results.size());

            pause();


            System.out.println("Getting first entity id in results, making entity map, displaying keys...");

            results = Peer.query("[:find ?c :where [?c :community/name]]", conn.db());
            Long id = (Long) ((List)results.iterator().next()).get(0);
            Entity entity = conn.db().entity(id);
            System.out.println(entity.keySet());

            pause();


            System.out.println("Displaying the value of the entity's community name...");

            System.out.println(entity.get(":community/name"));

            pause();

            System.out.println("Using a pull expression to get entities' attributes and values, displaying first...");

            Collection pullResults = Peer.query("[:find (pull ?c [*]) :where [?c :community/name]]", conn.db());
            System.out.println( ((List)pullResults.iterator().next()).get(0) );

            pause();


            System.out.println("Getting name of each community (some may appear more than " +
                               "because multiple online communities share the same name)...");

            Database db = conn.db();
            for (Object result : results) {
                entity = db.entity(((List) result).get(0));
                System.out.println(entity.get(":community/name"));
            }

            pause();


            System.out.println("Getting communities' neighborhood names (there are duplicates because " +
                               "multiple communities are in the same neighborhood...");

            db = conn.db();
            for (Object result : results) {
                entity = db.entity(((List) result).get(0));
                Entity neighborhood = (datomic.Entity) entity.get(":community/neighborhood");
                System.out.println(neighborhood.get(":neighborhood/name"));
            }

            pause();


            System.out.println("Getting names of all communities in first community's " +
                               "neighborhood...");

            Entity community = conn.db().entity(((List)results.iterator().next()).get(0));
            Entity neighborhood = (Entity) community.get(":community/neighborhood");
            Collection communities = (Collection) neighborhood.get(":community/_neighborhood");
            for (Object comm : communities) {
                System.out.println(((Entity) comm).get(":community/name"));
            }

            pause();


            System.out.println("Find all communities and specify returning their names...");
            System.out.println("Note: one collection of strings, not a collection of collections.");

            results = Peer.query("[:find [?n ...] :where [_ :community/name ?n]]",
                             conn.db());
            for (Object result : results) System.out.println(((String) result));

            pause();


            System.out.println("Find all community names and pull their urls...");

            results = Peer.query("[:find ?n (pull ?c [:community/url]) :where [?c :community/name ?n]]",
                             conn.db());
            for (Object result : results) System.out.println(result);

            pause();


            System.out.println("Find all categories for community named \"belltown\"...");

            results = Peer.query("[:find [?c ...] :where [?e :community/name \"belltown\"][?e :community/category ?c]]",
                             conn.db());
            for (Object result : results) System.out.println(result);

            pause();


            System.out.println("Find names of all communities that are twitter feeds...");

            results = Peer.query("[:find [?n ...] :where [?c :community/name ?n][?c :community/type :community.type/twitter]]",
                             conn.db());
            for (Object result : results) System.out.println(result);

            pause();


            System.out.println("Find names of all communities that are in a neighborhood " +
                               "in a district in the NE region...");

            results = Peer.query("[:find [?c_name ...] :where " +
                             "[?c :community/name ?c_name]" +
                             "[?c :community/neighborhood ?n]" +
                             "[?n :neighborhood/district ?d]" +
                             "[?d :district/region :region/ne]]",
                             conn.db());
            for (Object result : results) System.out.println(result);

            pause();


            System.out.println("Find community names and region names for of all communities...");

            results = Peer.query("[:find ?c_name ?r_name :where " +
                             "[?c :community/name ?c_name]" +
                             "[?c :community/neighborhood ?n]" +
                             "[?n :neighborhood/district ?d]" +
                             "[?d :district/region ?r]" +
                             "[?r :db/ident ?r_name]]",
                             conn.db());
            for (Object result : results) System.out.println(result);

            pause();


            System.out.println("Find all communities that are twitter feeds and facebook pages using " +
                               "the same query and passing in type as a parameter...");

            String query_by_type =
                "[:find [?n ...] :in $ ?t :where " +
                "[?c :community/name ?n]" +
                "[?c :community/type ?t]]";
            results = Peer.query(query_by_type,
                             conn.db(),
                             ":community.type/twitter");
            for (Object result : results) System.out.println(result);
            results = Peer.query(query_by_type,
                             conn.db(),
                             ":community.type/facebook-page");
            for (Object result : results) System.out.println(result);

            pause();


            System.out.println("Find all communities that are twitter feeds or facebook pages using " +
                               "one query and a list of individual parameters...");

            results = Peer.query("[:find ?n ?t :in $ [?t ...] :where " +
                             "[?c :community/name ?n]" +
                             "[?c :community/type ?t]]",
                             conn.db(),
                             Util.list(":community.type/facebook-page",
                                       ":community.type/twitter"));
            for (Object result : results) System.out.println(result);

            pause();


            System.out.println("Find all communities that are non-commercial email-lists or commercial " +
                               "web-sites using a list of tuple parameters...");

            results = Peer.query("[:find ?n ?t ?ot :in $ [[?t ?ot]] :where " +
                             "[?c :community/name ?n]" +
                             "[?c :community/type ?t]" +
                             "[?c :community/orgtype ?ot]]",
                             conn.db(),
                             Util.list(Util.list(":community.type/email-list",
                                                 ":community.orgtype/community"),
                                       Util.list(":community.type/website",
                                                 ":community.orgtype/commercial")));
            for (Object result : results) System.out.println(result);

            pause();


            System.out.println("Find all community names coming before \"C\" in alphabetical order...");

            results = Peer.query("[:find [?n ...] :where " +
                             "[?c :community/name ?n]" +
                             "[(.compareTo ?n \"C\") ?res]" +
                             "[(< ?res 0)]]",
                             conn.db());
            for (Object result : results) System.out.println(result);

            pause();


            System.out.println("Find all communities whose names include the string \"Wallingford\"...");

            results = Peer.query("[:find [?n ...] :where " +
                             "[(fulltext $ :community/name \"Wallingford\") [[?e ?n]]]]",
                             conn.db());
            for (Object result : results) System.out.println(result);

            pause();


            System.out.println("Find all communities that are websites and that are about " +
                               "food, passing in type and search string as parameters...");

            results = Peer.query("[:find ?name ?cat :in $ ?type ?search :where " +
                             "[?c :community/name ?name]" +
                             "[?c :community/type ?type]" +
                             "[(fulltext $ :community/category ?search) [[?c ?cat]]]]",
                             conn.db(),
                             ":community.type/website",
                             "food");
            for (Object result : results) System.out.println(result);

            pause();


            System.out.println("Find all names of all communities that are twitter feeds, using rules...");

            String rules =
                "[[[twitter ?c]"+
                "  [?c :community/type :community.type/twitter]]]";
            results = Peer.query("[:find [?n ...] :in $ % :where " +
                             "[?c :community/name ?n]" +
                             "(twitter ?c)]",
                             conn.db(),
                             rules);
            for (Object result : results) System.out.println(result);

            pause();


            System.out.println("Find names of all communities in NE and SW regions, using rules " +
                               "to avoid repeating logic...");

            rules =
                "[[[region ?c ?r]" +
                "  [?c :community/neighborhood ?n]" +
                "  [?n :neighborhood/district ?d]" +
                "  [?d :district/region ?re]" +
                "  [?re :db/ident ?r]]]";
            results = Peer.query("[:find [?n ...] :in $ % :where " +
                             "[?c :community/name ?n]" +
                             "(region ?c :region/ne)]",
                             conn.db(),
                             rules);
            for (Object result : results) System.out.println(result);
            results = Peer.query("[:find [?n ...] :in $ % :where " +
                             "[?c :community/name ?n]" +
                             "(region ?c :region/sw)]",
                             conn.db(),
                             rules);
            for (Object result : results) System.out.println(result);

            pause();


            System.out.println("Find names of all communities that are in any of the southern " +
                               "regions and are social-media, using rules for OR logic...");
            rules =
                "[[[region ?c ?r]" +
                "  [?c :community/neighborhood ?n]" +
                "  [?n :neighborhood/district ?d]" +
                "  [?d :district/region ?re]" +
                "  [?re :db/ident ?r]]" +
                " [[social-media ?c]" +
                "  [?c :community/type :community.type/twitter]]" +
                " [[social-media ?c]" +
                "  [?c :community/type :community.type/facebook-page]]" +
                " [[northern ?c] (region ?c :region/ne)]" +
                " [[northern ?c] (region ?c :region/n)]" +
                " [[northern ?c] (region ?c :region/nw)]" +
                " [[southern ?c] (region ?c :region/sw)]" +
                " [[southern ?c] (region ?c :region/s)]" +
                " [[southern ?c] (region ?c :region/se)]]";

            results = Peer.query("[:find [?n ...] :in $ % :where " +
                             "[?c :community/name ?n]" +
                             "(southern ?c)" +
                             "(social-media ?c)]",
                             conn.db(),
                             rules);
            for (Object result : results) System.out.println(result);

            pause();


            System.out.println("Find all database transactions...");

            results = Peer.query("[:find [?when ...] :where [?tx :db/txInstant ?when]]",
                             conn.db());

            pause();

            System.out.println("Sort transactions by time they occurred, then " +
                               "pull out date when seed data load transaction and " +
                               "schema load transactions were executed...");

            List tx_dates = new ArrayList();
            for (Object result : results) tx_dates.add(result);
            Collections.sort(tx_dates);
            Collections.reverse(tx_dates);
            Date data_tx_date = (Date) tx_dates.get(0);
            Date schema_tx_date = (Date) tx_dates.get(1);

            pause();


            System.out.println("Make query to find all communities, use with database " +
                               "values as of and since different points in time...");

            System.out.println("\nFind all communities as of schema transaction...");
            Database db_asOf_schema = conn.db().asOf(schema_tx_date);
            System.out.println(Peer.q("[:find (count ?c) :where [?c :community/name]]", db_asOf_schema));

            System.out.println("\nFind all communities as of seed data transaction...");
            Database db_asOf_data = conn.db().asOf(data_tx_date);
            System.out.println(Peer.q("[:find (count ?c) :where [?c :community/name]]", db_asOf_data));

            System.out.println("\nFind all communities since schema transaction...");
            Database db_since_schema = conn.db().since(schema_tx_date);
            System.out.println(Peer.q("[:find (count ?c) :where [?c :community/name]]", db_since_schema));

            System.out.println("\nFind all communities since seed data transaction...");
            Database db_since_data = conn.db().since(data_tx_date);
            System.out.println(Peer.q("[:find (count ?c) :where [?c :community/name]]", db_since_data));

            pause();

            
            System.out.println("Parse additional seed data file...");
            
            data_rdr = new FileReader("samples/seattle/seattle-data1.edn");
            List new_data_tx = (List) Util.readAll(data_rdr).get(0);
            
            System.out.println("\nFind all communities if new data is loaded...");
            Map report = conn.db().with(new_data_tx);
            Database db_if_new_data = (Database) report.get(Connection.DB_AFTER);
            System.out.println(Peer.q("[:find (count ?c) :where [?c :community/name]]", db_if_new_data));
            
            System.out.println("\nFind all communities currently in database...");
            System.out.println(Peer.q("[:find (count ?c) :where [?c :community/name]]", conn.db()));
            
            System.out.println("\nSubmit new data transaction...");
            txResult = conn.transact(new_data_tx).get();
            
            System.out.println("\nFind all communities currently in database...");
            System.out.println(Peer.q("[:find (count ?c) :where [?c :community/name]]", conn.db()));
            
            System.out.println("\nFind all communities since original seed data load transaction...");
            db_since_data = conn.db().since(data_tx_date);
            System.out.println(Peer.q("[:find (count ?c) :where [?c :community/name]]", db_since_data));
            
            pause();
            
            
            System.out.println("Make a new partition...");
            
            List partition_tx = Util.list(Util.map("db/id", Peer.tempid(":db.part/db"),
                                                   "db/ident", ":communities",
                                                   "db.install/_partition", "db.part/db"));
            txResult = conn.transact(partition_tx).get();
            System.out.println(txResult);
            
            pause();
            
            
            System.out.println("Make a new community...");

            List add_community_tx = Util.list(Util.map(":db/id", Peer.tempid(":communities"),
                                                       ":community/name", "Easton"));
            txResult = conn.transact(add_community_tx).get();
            System.out.println(txResult);

            pause();


            System.out.println("Update data for a community...");

            Long belltown_id = Peer.query("[:find ?id . :where [?id :community/name \"belltown\"]]",
                                      conn.db());

            List update_category_tx = Util.list(Util.map(":db/id", belltown_id,
                                                         ":community/category", "free stuff"));
            txResult = conn.transact(update_category_tx).get();
            System.out.println(txResult);

            pause();


            System.out.println("Retract data for a community...");

            List retract_category_tx = Util.list(Util.list(":db/retract", belltown_id, ":community/category", "free stuff"));
            txResult = conn.transact(retract_category_tx).get();
            System.out.println(txResult);

            pause();


            System.out.println("Retract a community entity...");

            Long easton_id = Peer.query("[:find ?id . :where [?id :community/name \"Easton\"]]",
                                    conn.db());

            List retract_entity_tx = Util.list(Util.list(":db.fn/retractEntity", easton_id));
            txResult = conn.transact(retract_category_tx).get();
            System.out.println(txResult);

            pause();

            System.out.println("Get transaction report queue, add new community again...");

            Queue queue = conn.txReportQueue();

            add_community_tx = Util.list(Util.map(":db/id", Peer.tempid(":communities"),
                                                  ":community/name", "Easton"));

            txResult = conn.transact(add_community_tx).get();
            System.out.println(txResult);

            System.out.println("Poll queue for transaction notification, print data that was added...");

            report = (Map) queue.poll();
            results = Peer.query("[:find ?e ?aname ?v ?added " +
                             ":in $ [[?e ?a ?v _ ?added]] " +
                             ":where " +
                             "[?e ?a ?v _ ?added]" +
                             "[?a :db/ident ?aname]]",
                             report.get(Connection.DB_AFTER),
                             report.get(Connection.TX_DATA));
            for (Object result : results) System.out.println(result);
            Peer.shutdown(true);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static final Scanner scanner = new Scanner(System.in);

    private static void pause() {
        if (System.getProperty("NOPAUSE") == null) {
            System.out.println("\nPress enter to continue...");
            scanner.nextLine();
        }
    }
}
