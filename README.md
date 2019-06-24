dse 6.7.3

dse cassandra -k -s

create KEYSPACE poc WITH replication = {'class': 'SimpleStrategy' , 'replication_factor': 1 };
//CREATE TABLE poc.address2 ( id text PRIMARY KEY , num text, type text, zipcode text, city text, lat text, long text);
 
CREATE TABLE poc.address ( id text PRIMARY KEY , num text, type text, zipcode text, city text, long_lat 'PointType');

dse spark
//val df=spark.read.format("csv").option("header", "false").load("file:///home/florent/Downloads/full.csv").toDF("id","num","type","zipcode","city","source","lat","long")
//df.drop("source").write.cassandraFormat("address2","poc").option("confirm.truncate","true").mode(org.apache.spark.sql.SaveMode.Overwrite).save

val df=spark.read.format("csv").option("header", "false").load("file:///home/florent/Downloads/full.csv").toDF("id","num","type","zipcode","city","source","lat","long")
df.withColumn("long_lat", concat(lit("POINT("),'long,lit(" "),'lat,lit(")"))).drop("source","lat","long").write.cassandraFormat("address","poc").option("confirm.truncate","true").mode(org.apache.spark.sql.SaveMode.Overwrite).save



create SEARCH INDEX on poc.address;


dsetool get_core_schema poc.address > schema.xml
dsetool get_core_config poc.address > config.xml 

vim schema.xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<schema name="autoSolrSchema" version="1.5">
  <types>
    <fieldType class="org.apache.solr.schema.StrField" name="StrField"/>
 <fieldType class="org.apache.solr.schema.SpatialRecursivePrefixTreeFieldType" geo="true" name="SpatialRecursivePrefixTreeFieldType" spatialContextFactory="org.locationtech.spatial4j.context.jts.JtsSpatialContextFactory" useJtsMulti="false"/>
     <fieldType class="org.apache.solr.schema.TextField" name="TextField">
      <analyzer type="index">
        <tokenizer class="solr.StandardTokenizerFactory"/>
        <filter class="solr.ASCIIFoldingFilterFactory"/>
        <filter class="solr.LowerCaseFilterFactory"/>
        <filter class="solr.SynonymFilterFactory" synonyms="synonyms.txt" 
                format="solr" ignoreCase="false" expand="true"/>
</analyzer>
      <analyzer type="query">
        <tokenizer class="solr.StandardTokenizerFactory"/>
        <filter class="solr.ASCIIFoldingFilterFactory"/>
        <filter class="solr.LowerCaseFilterFactory"/>
</analyzer>
    </fieldType>
</types>
<fields>
    <field indexed="false" multiValued="false" name="num" type="StrField"/>
    <field indexed="false" multiValued="false" name="type" type="StrField"/>
    <field indexed="false" multiValued="false" name="city" type="StrField"/>
    <field indexed="false" multiValued="false" name="zipcode" type="StrField"/>
    <field docValues="true" indexed="true" multiValued="false" name="id" type="StrField"/>
    <field docValues="false" indexed="true" multiValued="true" name="full_address" type="TextField"/>
    <field indexed="true" multiValued="false" name="long_lat" type="SpatialRecursivePrefixTreeFieldType"/>
    <copyField source="num" dest="full_address" />
    <copyField source="type" dest="full_address" />
    <copyField source="city" dest="full_address" />
    <copyField source="zipcode" dest="full_address" />
  </fields>
  <uniqueKey>id</uniqueKey>
</schema>



synonyms.txt
av,avenue
r,rue

dsetool write_resource poc.address name=synonyms.txt file=synonyms.txt
dsetool reload_core poc.address solrconfig=config.xml schema=schema.xml deleteAll=true reindex=true

select count(*) from poc.address where solr_query='{"q":"*:*"}';

select * from poc.address where solr_query='{"q":"*:*","fq":"{!geofilt sfield=long_lat pt=48.856613,2.352222 d=20.0 cache=false}"}';

select * from poc.address WHERE solr_query='{"q":"full_address:(73C AND av AND gamb* AND PAris~) "}';

select * from poc.address WHERE solr_query='{"q":"+full_address:(73 AND av AND gambetta) OR {!geofilt sfield=long_lat pt=48.856613,2.352222 d=10.0 score=kilometers}^100"}';

select * from poc.address WHERE solr_query='{"q":"+full_address:(73 AND av ) OR {!geofilt sfield=long_lat pt=43.71613,7.262222 d=50.0 score=recipDistance}","sort":"score desc"}' limit 10;



dse 5.1



drop KEYSPACE poc;
create KEYSPACE poc WITH replication = {'class': 'SimpleStrategy' , 'replication_factor': 1 };
CREATE TABLE poc.address ( id text PRIMARY KEY , num text, type text, zipcode text, city text, long_lat 'PointType');
create SEARCH INDEX on poc.address WITH OPTIONS {lenient:true };
ALTER SEARCH INDEX SCHEMA ON poc.address ADD types.fieldtype[@class='solr.SpatialRecursivePrefixTreeFieldType',
      @name='point', @distanceUnits='kilometers'];

# ALTER SEARCH INDEX SCHEMA ON poc.address drop fields.field[@name='long_lat'] ;
ALTER SEARCH INDEX SCHEMA ON poc.address ADD fields.field[@name='long_lat', @type='point', @indexed='true'];
DESCRIBE ACTIVE SEARCH INDEX SCHEMA ON poc.address ;
RELOAD SEARCH INDEX ON poc.address;
REBUILD SEARCH INDEX ON poc.address;

INSERT INTO poc.address (id, long_lat) VALUES ('Eiffel Tower', 'POINT(2.2945 48.8582)');
INSERT INTO poc.address (id, long_lat) VALUES ('lyon', 'POINT(4.835659 45.764042)');
INSERT INTO poc.address (id, long_lat) VALUES ('marseille', 'POINT(5.369780 43.296482)');

select * from poc.address WHERE solr_query='{"q":"{!geofilt sfield=long_lat pt=48.856613,2.352222 d=1000.0 score=kilometers}","sort":"score asc"}';
id           | city | long_lat                   | num  | solr_query | type | zipcode
--------------+------+----------------------------+------+------------+------+---------
 Eiffel Tower | null |     POINT (2.2945 48.8582) | null |       null | null |    null
         lyon | null | POINT (4.835659 45.764042) | null |       null | null |    null
    marseille | null |  POINT (5.36978 43.296482) | null |       null | null |    null


select * from poc.address WHERE solr_query='{"q":"{!geofilt sfield=long_lat pt=43.856613,5.352222 d=1000.0 score=kilometers}","sort":"score asc"}';


change long_lat -> coords

<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<schema name="autoSolrSchema" version="1.5">
  <types>
    <fieldType class="org.apache.solr.schema.TextField" name="TextField">
      <analyzer>
        <tokenizer class="solr.StandardTokenizerFactory"/>
        <filter class="solr.LowerCaseFilterFactory"/>
      </analyzer>
    </fieldType>
    <fieldType class="org.apache.solr.schema.StrField" name="StrField"/>
    <fieldtype class="solr.SpatialRecursivePrefixTreeFieldType" distanceUnits="kilometers" name="point"/>
  </types>
  <fields>
    <field indexed="true" multiValued="false" name="city" stored="true" type="TextField"/>
    <field indexed="true" multiValued="false" name="type" stored="true" type="TextField"/>
    <field indexed="true" multiValued="false" name="zipcode" stored="true" type="TextField"/>
    <field indexed="true" multiValued="false" name="id" stored="true" type="StrField"/>
    <field indexed="true" multiValued="false" name="num" stored="true" type="TextField"/>
    <field indexed="true" name="long_lat" type="point"/>
  </fields>
  <uniqueKey>id</uniqueKey>
</schema>


<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<schema name="autoSolrSchema" version="1.5">
  <types>
    <fieldType class="org.apache.solr.schema.StrField" name="StrField"/>
 <fieldType class="org.apache.solr.schema.SpatialRecursivePrefixTreeFieldType" geo="true" name="SpatialRecursivePrefixTreeFieldType" spatialContextFactory="org.locationtech.spatial4j.context.jts.JtsSpatialContextFactory" useJtsMulti="false"/>
     <fieldType class="org.apache.solr.schema.TextField" name="TextField">
      <analyzer type="index">
        <tokenizer class="solr.StandardTokenizerFactory"/>
        <filter class="solr.ASCIIFoldingFilterFactory"/>
        <filter class="solr.LowerCaseFilterFactory"/>
        <filter class="solr.SynonymFilterFactory" synonyms="synonyms.txt" 
                format="solr" ignoreCase="false" expand="true"/>
</analyzer>
      <analyzer type="query">
        <tokenizer class="solr.StandardTokenizerFactory"/>
        <filter class="solr.ASCIIFoldingFilterFactory"/>
        <filter class="solr.LowerCaseFilterFactory"/>
</analyzer>
    </fieldType>
</types>
<fields>
    <field indexed="false" multiValued="false" name="num" type="StrField"/>
    <field indexed="false" multiValued="false" name="type" type="StrField"/>
    <field indexed="false" multiValued="false" name="city" type="StrField"/>
    <field indexed="false" multiValued="false" name="zipcode" type="StrField"/>
    <field docValues="true" indexed="true" multiValued="false" name="id" type="StrField"/>
    <field docValues="false" indexed="true" multiValued="true" name="full_address" type="TextField"/>
    <field indexed="true" multiValued="false" name="long_lat" type="SpatialRecursivePrefixTreeFieldType"/>
    <copyField source="num" dest="full_address" />
    <copyField source="type" dest="full_address" />
    <copyField source="city" dest="full_address" />
    <copyField source="zipcode" dest="full_address" />
  </fields>
  <uniqueKey>id</uniqueKey>
</schema>


<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<schema name="autoSolrSchema" version="1.5">
  <types>
    <fieldType class="org.apache.solr.schema.StrField" name="StrField"/>
    <fieldtype class="solr.SpatialRecursivePrefixTreeFieldType" distanceUnits="kilometers" name="point"/>
     <fieldType class="org.apache.solr.schema.TextField" name="TextField">
      <analyzer type="index">
        <tokenizer class="solr.StandardTokenizerFactory"/>
        <filter class="solr.ASCIIFoldingFilterFactory"/>
        <filter class="solr.LowerCaseFilterFactory"/>
        <filter class="solr.SynonymFilterFactory" synonyms="synonyms.txt" 
                format="solr" ignoreCase="false" expand="true"/>
</analyzer>
      <analyzer type="query">
        <tokenizer class="solr.StandardTokenizerFactory"/>
        <filter class="solr.ASCIIFoldingFilterFactory"/>
        <filter class="solr.LowerCaseFilterFactory"/>
</analyzer>
    </fieldType>
</types>
<fields>
    <field indexed="false" multiValued="false" name="num" type="StrField"/>
    <field indexed="false" multiValued="false" name="type" type="StrField"/>
    <field indexed="false" multiValued="false" name="city" type="StrField"/>
    <field indexed="false" multiValued="false" name="zipcode" type="StrField"/>
    <field docValues="true" indexed="true" multiValued="false" name="id" type="StrField"/>
    <field docValues="false" indexed="true" multiValued="true" name="full_address" type="TextField"/>
    <field indexed="true" multiValued="false" name="long_lat" type="point"/>
    <copyField source="num" dest="full_address" />
    <copyField source="type" dest="full_address" />
    <copyField source="city" dest="full_address" />
    <copyField source="zipcode" dest="full_address" />
  </fields>
  <uniqueKey>id</uniqueKey>
</schema>



curl http://localhost:8080/api/search?address=73c%20av%20Gambeta%2075020




