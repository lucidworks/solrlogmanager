Instructions
============

This document details how to use Logstash with Solr. It has been tested with the following versions:
- Solr 4.4.0, 4.5.0, 4.6.0, 4.7.0, 4.8.1 and 5.5.0
- Logstash 1.3.3 and 2.2.2
- Java 1.6.0, 1.7.0 and 1.8.0


1. Create a collection to hold the log event data.
  It is now the expectation that the collection uses either a managed_schema or that the user has created an appropriate
  unmanaged schema for receipt of new fields.  With the exception of the two mandatory fields @timestamp and @version
  the application will not itself attempt to directly use the Solr api to explicitly create fields.

	For the mandatory fields @timestamp and @version the program will check for their existence at startup and if these fields
  do not exist then it will attempt to create them with the following types:

	Timestamp
		type: tdate
		name: appends prefix value from config file or logstash_ by default to 'timestamp'. ex: logstash_timestamp
		stored: true
		indexed: true

	Version
		type: long
		name: appends prefix value from config file or logstash_ by default to 'version'.  ex: logstash_version
		stored: true
		indexed: true

	These type definitions can be changed in the file `logstash-output-lucidworks_solr/lib/logstash/outputs/lucidworks_solr.rb`

	Setup a managed schema in `solrconfig.xml` file by including an active ManagedIndexSchemaFactory. The xml file is located
	in the collection's conf directory.

  Example:
	   <schemaFactory class="ManagedIndexSchemaFactory">
	      <bool name="mutable">true</bool>
	      <str name="managedSchemaResourceName">managed-schema</str>
	   </schemaFactory>

   For more information see - http://svn.apache.org/repos/asf/lucene/dev/trunk/solr/example/solr/collection1/conf/solrconfig.xml

   An example collection folder named `logstash_logs` is provided for use as a starter.  Note that it is
   based on Solr version 4.4.0.  If you use a different version then you should use files from your distribution for your starting point.

   Copy the `logstash_logs` folder to the directory where your solr collections are stored.  With this 'skeleton' in place use the Solr admin tool
   to create a new collection.  In the 'instance dir' field enter _logstash_logs_.

   Example admin new core parameters:

       name: WhatEverNameYouLike
       instance_dir: logstash_logs
       dataDir: data
       config: solrconfig.xml
       schema: schema.xml

    Note that later versions of Solr automatically detect cores. Also, if you are running SolrCloud, you will need to either bootstrap the configuration at startup or upload the configuration to zookeeper and then specify it when creating a collection.

2. `lucidworks.jar` is located in `logstash-output-lucidworks_solr/lib/`. It contains the Java class files from `src` directory that the logstash plugin requires.

3. `lucidworks_solr.rb` is located in `logstash-output-lucidworks_solr/lib/logstash/outputs/`. It contains the JRuby source code of the logstash output plugin.

   The _receive_ method converts the event argument received from Logstash into a hash of _fieldname=fieldvalue_ pairs and passes this to lucidworks.jar.

4. Add `lucidworks_solr` output definition to logstash configuration file. An example file `lw_solr_conf` is included
   with the distribution. To use, point the path parameter at your log file location:
  ```
  input {
        file {
            type => "syslog"
            exclude => ["*.gz","*.zip","*.tgz"]
            path => "/logfilePath/**/./*"
            sincedb_path => "/dev/null"
            start_position => "beginning"
        }
  }
  # Create fields for all name=value pairs found in message.  Add a new field and tag just for fun.
  filter {
        kv {
            add_field => [ "User_%{user}_says", "Hello world, from %{src_ip}" ]
            add_tag => [ "tag", "you are it" ]
        }  
  }
  output {
        lucidworks_solr {
            collection_host => "localhost"
            collection_port => "8888"
            collection_name => "logstash_logs"
            field_prefix => "event_"
            force_commit => false
            flush_size => 1000
            idle_flush_time => 1
        }
  }
  ```
  Where:
  ```
  output {
        lucidworks_solr {
          collection_host => ... # string (optional), default: "localhost"
          collection_port => ... # number (optional), default: 8983
          collection_name => ... # string (optional), default: "collection1"
          field_prefix => ... # string (optional), default: "logstash_"
          force_commit => ... # boolean (optional), default: false
          flush_size => ... # number (optional), default: 100
          idle_flush_time => ... # number (optional), default: 1
        }
  }
  ```
  `collection_host` => Address of solr instance.
       Value type is string
       Default is localhost
  `collection_port` => Port for sending rest messages to solr instance.
       Value type is string
       Default is 8983
  `collection_name` => Name of existent collection that will receive new documents.
       Value type is string
       Default is collection1
  `field_prefix` => Logstash @timestamp and @version will be renamed [field_prefix]timestamp and [field_prefix]version.
       Value type is string
       Default is logstash_
  `force_commit` => If true then a commit request will be sent to Solr for each batch of documents uploaded.  If false then
  the documents will be committed per the Solr instance's configured commit policy.
       Value type is boolean
       Default is false

  The lucidworks_solr output plugin uses Logstash's stud buffer to handle buffering events for batched document uploads. The next two
  field values get passed to the buffer manager.

  `flush_size` => Number of events to queue up before writing to Solr.  The implementation uses Logstash's stud event buffering.
       Value type is number
       Default is 100
  `idle_flush_time` => Amount of time in seconds since the last flush before a flush is done even if the number of buffered events is smaller than flush_size.
       Value type is number
       Default is 1

5. To run logstash:  
       $ logstash-2.2.2./bin/logstash agent -f lw_solr_syslog.conf

   Captured log data should now be in your Solr instance.  
