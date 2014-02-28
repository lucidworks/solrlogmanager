Instructions 

This document details how to use LogStash with Solr.  This release has been tested with:
 	Solr version 4.4.0, 4.5.0 and 4.6.x
 	LogStash version 1.2.2
 	Java 1.6.0

1) Create a collection to hold the log event data.  
   
   The application creates new fields dynamically.  It requires the solrconfig.xml file 
   contian an active ManagedIndexSchemaFactory.  Insure that a properly configured solrconfig.xml
   exists in the collection's conf directory. 
 
   Example:
	   <schemaFactory class="ManagedIndexSchemaFactory">
	      <bool name="mutable">true</bool>
	      <str name="managedSchemaResourceName">managed-schema</str>
	   </schemaFactory>

   For more information see - http://svn.apache.org/repos/asf/lucene/dev/trunk/solr/example/solr/collection1/conf/solrconfig.xml

   An example collection folder named 'logstash_logs' is provided for use as a starter.  Note that it is 
   based on Solr version 4.4.0.  If you use a different version then you should use files from your distribution for your starting point
   (remember to use a managed solrconfig).  
   
   Copy the logstash_logs folder to the directory where your solr collections are stored.  With this 'skeleton' in place use the Solr admin tool 
   to create a new collection.  In the 'instance dir' field enther logstash_logs.  
   
   Example admin new core parameters:
   
       name: WhatEverNameYouLike
       instance_dir: logstash_logs
       dataDir: data
       config: solrconfig.xml
       schema: schema.xml
    
    Note that later versions of Solr automatically detect cores. Also, if you are running SolrCloud, you will need to either bootstrap the configuration at startup or upload the configuration to zookeeper and then specify it when creating a collection. 
 
2) Copy lucidworks.jar to the directory where you will execute the LogStash jar file (logstash-1.2.2-flatjar.jar or later). If you use the logstash_deploy directory as is this is already done for you.

3) Copy lucidworks_solr_lsv122.rb to your LogStash outputs directory.  This is typically in path "./<locationOfLogstashJar>/logstash/outputs". If you use the logstash_deploy directory as is this is already done for you.
   As the name indicates this version of the Ruby file is targeted at LogStash version 1.2.2.  
   
   The receive method converts the event argument received from LogStash into a hash of fieldname=fieldvalue pairs and passes this to
   the Solr jar.  The user can modify this file to suit their specific needs.  Examples of why the user organization might want 
   to make modifications include:
     
      a) @ is an illeagal Solr fieldname character.  The Ruby module replaces the @ on LogStash @version and @timestamp with a user specified prefix.  
      	 The user sets the desired prefix with an argument in the LogStash config file (see conf settings below).  In the event that LogStash
         makes unexpected schema change in the future that adds illegal Solr fieldname characters to field names then Ruby file needs to adjust this.
            
      b) The user's LogStash config creates other tag-like arrays.  The array items need to be broken out 
         as name value pairs.  
      
      c) Solr documents require a unique ID.  The Solr jar createa a GUIID automatically for each document added.  
         If you want to manage your own ID's or overwrite data in an existing document then you can add a field named 'id' and the appropriate
         value to the collection that gets passed to addSolrDocument() call.
         
      d) If a new field is not already in the collection then the field will be explicitly and automatically defined in the schema before the value 
         is saved.  The parameters for the field definition's are defined in @solrFieldCreationParams variable.  By default fields 'message' 
         and 'tags' use unique confirgurations.  All other fields are given the same default definition:
         
         	tags = "[{\"type\":\"text_en\",\"name\":\"tags\",\"stored\":true,\"indexed\":true,\"multiValued\":true}]"
         	message = "[{\"type\":\"text_en\",\"name\":\"message\",\"stored\":true,\"indexed\":false}]"
         	everythingElse = "[{\"type\":\"text_en\",\"name\":\"" + key + "\",\"stored\":true,\"indexed\":true}]"
         
         See lucidworks_solr_lsv122.rb for instruction about how to add your own specific definitions for one or more of the fields found 
         in your unique environment.
           
                  
4) Add lucidworks_solr output definition to logstash configuration file.  An example file lw_solr_conf is included
   with the distribution.  To use, point the path parameter at your log file location.
   
  input {
    file {
      type => "syslog"
    	exclude => ["*.gz","*.zip","*.tgz"]
			# FYI - LogStash does not always recurse the directory hirearcy correctly on Windows
			# unless the path is all lowercase.
      path => [ "/logfilePath/**/./*" ]
			sincedb_path => "/dev/null"
    	start_position => "beginning"
    }
  }
	# Create fields for all name=value pairs found in message.  Add a new field and tag just for fun.
	filter {
		kv{
			add_field => [ "User_%{user}_says", "Hello world, from %{src_ip}" ]
	    add_tag => [ "tag", "you are it" ]
		}  
	}
	output {
	  stdout { debug => true codec => "rubydebug"}
 		lucidworks_solr_lsv122 { collection_host => "127.0.0.1" collection_port => "8983" collection_name => "logstash_logs" field_prefix => "logstash_"}
  }

  Where:
  	collection_host - address of solr instance.  Defaults is 'localhost'.
	  collection_port - port for sending rest messages to solr instance.  Solr default is 8983.
	  collection_name - name of existent collection that will receive new documents. Default is 'collection1'.
  	field_prefix    - LogStash @timestamp and @version will be renamed [field_prefix]timestamp and [field_prefix]version.  Default is "event_".
  	
  	The other example configuration file that works with MacOS Syslogs is referenced in QuickStart.txt.

5) From within the directory where lucidworks.jar, dist subdirectory and logstash.jar are loacated execute LogStash.  
       Example: java -jar logstash-1.2.2-flatjar.jar agent -f lw_solr_conf -p .  

   Captured log data should now be saved and retrivable from your solr instance.  If debug is true in the config files output then you should see each processed document's data displayed in the console window.

