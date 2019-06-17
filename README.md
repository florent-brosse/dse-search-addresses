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





