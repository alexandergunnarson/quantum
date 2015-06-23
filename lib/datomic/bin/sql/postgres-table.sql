-- Table: datomic_kvs

-- DROP TABLE datomic_kvs;

CREATE TABLE datomic_kvs
(
 id text NOT NULL,
 rev integer,
 map text,
 val bytea,
 CONSTRAINT pk_id PRIMARY KEY (id )
)
WITH (
 OIDS=FALSE
);
ALTER TABLE datomic_kvs
 OWNER TO alexandergunnarson;
GRANT ALL ON TABLE datomic_kvs TO alexandergunnarson;
GRANT ALL ON TABLE datomic_kvs TO public;
