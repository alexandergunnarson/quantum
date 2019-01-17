-- ~/voltdb-community-8.3.3/bin/voltdb start --http=8081
-- ./bin/voltdb/sqlcmd

-- TODO performance-test this against Datomic! Generate a bunch of random data till it gets big

drop table entities;

-- TODO add a `tinyint` column for datatype for more efficient indexing
create table entities (
-- time (transaction ID/instant; must be increasing; can be in nanos)
-- first for display purposes
t bigint not null,
-- entity ID
e bigint not null,
-- attribute
a varchar(128) not null,
-- value (serialized)
-- TODO change back to varbinary; just varchar for interactive testing
v varchar
);

-- technically true as you should never assert the same thing in the same transaction
-- create unique index eavt on entities (e, a, v, t)

-- Idea from https://docs.datomic.com/on-prem/indexes.html
-- TODO only index non- byte arrays
create index eavt on entities (e, a, v, t);
create index aevt on entities (a, e, v, t);
create index avet on entities (a, v, e, t);
create index vaet on entities (v, a, e, t);

-- This is in order to do time-based iteration
create index t on entities (t);

-- TODO is this wise? It seems the best partitioning but might not be
partition table entities on column e;

-- Multiple insert in one clause is not supported
insert into entities (t, e, a, v) values (0, -9223372036854775807, 'db|attribute'        , 'db|attribute');
insert into entities (t, e, a, v) values (0, -9223372036854775807, 'db|doc'              , 'Marker for an attribute name');
insert into entities (t, e, a, v) values (0, -9223372036854775806, 'db|attribute'        , 'db|doc');
insert into entities (t, e, a, v) values (0, -9223372036854775806, 'db|doc'              , 'Attribute whose value supplies documentation');
insert into entities (t, e, a, v) values (0, -9223372036854775805, 'db|attribute'        , 'my|attribute');
insert into entities (t, e, a, v) values (0, -9223372036854775805, 'db|doc'              , 'Documentation for my attribute');

insert into entities (t, e, a, v) values (1, -9223372036854775804, 'my|attribute'        , 'A value for my attribute. Yay!');

insert into entities (t, e, a, v) values (2, -9223372036854775803, 'db|attribute'        , 'my|indexed-attribute');

-- Be careful to sanitize the names — shouldn't include `hash` in the name unless intentional
create index my_BAR_indexed_attribute__evt on entities (e, v, t) where a = 'db|attribute';
create index my_BAR_indexed_attribute__vet on entities (v, e, t) where a = 'db|attribute';

insert into entities (t, e, a, v) values (3, -9223372036854775803, 'my|indexed-attribute', 'A value for my indexed attribute. Woohoo!');

-- next entity ID
select max(e) + 1 from entities;

-- (BigInteger. (.getBytes "my-value!!")) -> "my-value!!"
-- (String. (.toByteArray (BigInteger. "516973278578607596577057"))) -> "my-value!!"

-- Having a view of the DB at a point in time really is as trivial as `where t <= my_timestamp`



-- [:find (count ?customer) . :in $ ?organization :where
--  [?concert :concert/organization ?organization]
--  [?booking :booking/concert ?concert]
--  [?booking :booking/customer ?customer]]--

-- TODO make sure revisions (changes in value of an identity across time) are addressed here
select customer.e
  from s as concert,
       entities as booking,
       entities as customer
  where (    concert.a = "concert/organization"
         and concert.v = "the-organization")
     or (    booking.a = "booking/concert"
         and booking.v = concert.e)
     or (    booking.a = "booking/customer"
         and booking.v = customer.e);

-- This is probably as much effort as the type system. I think we should do it only when we start to
-- scale, and only if it proves to have performance gains that Datomic can't match. We should code
-- to the Datomic interface though. Plus if we preserve all the data in datom format, it's about the
-- easiest thing to migrate (in theory). Perhaps we should do it either way — having source code we
-- can configure and edit will be really helpful.
