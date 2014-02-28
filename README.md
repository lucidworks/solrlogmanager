Solr Log Manager
============

This is an implementation of a LogStash output plugin that delivers log data to Solr.  

The logstash_deploy folder contains a working version of the plugin components and also includes a version of LogStash.  See the 
ReadMe in the logstash_deploy folder for instruction on how to install and run.   

To build the lucidworks.jar yourself from within the src folder: 
	javac javac *.java
	jar cvf lucidworks.jar *.class
	
# QUICKSTART


1. Copy the logstash_deploy folder to your disk. You will run LogStash and the Solr Output Writer from this directory.

2. Copy provided ManagedIndexSchemaFactory configured logstash_logs directory to your Solr collection folder.  

3. If you are on a older version of Solr (before 4.4), go to the Solr Admin Page add a new core.  In the name and instanceDir fields enter: logstash_logs. Accept the defaults for the remaining fields and click on the 'Add Core' button.

4. For a start-up example, we will use syslogs:
   Open lw_solr_syslog.conf in a text editor.  In the 'input' section of the config file add the path to your log file[s] by changing 
			path => [ "/var/log/system.log" ]

   If your Solr instance is not running on the default localhost:8983 then change the collection_host and collection_port values in the 
   ouptut section of the config file.
   
4a.Note that we have also included another LogStash config file that uses the kv_filter to automatcially create named fields when it discovers
   name=value pattern in the scanned message.  This simple transform will work as is for many types of logs.  But it can get tripped up by  
   patterns 'a=b=c' for example which can result in invalid Solr field names.  Modify the filters in the config file as necessary to perform 
   LogStash transformations consistent with the data coming from your environment (for details on LogStash configuration see http://logstash.net/docs/1.2.2/).
   
5. Download LogStash to the deploy folder - https://download.elasticsearch.org/logstash/logstash/logstash-1.2.2-flatjar.jar (NO LONGER NECESSARY, it included in this repository)

6. For the Sylog example, execute the following curl command (making sure to change the port)
curl 'http://localhost:8983/solr/logstash_logs/schema/fields' -X POST -H 'Content-type: application/json' --data-binary '
[{"name":"syslog_program","type":"string","indexed":true,"stored":true, "copyFields":["logtext"]}]'
This will make sure you are faceting correctly on the program field.
 
6. Execute LogStash: java -jar logstash-1.2.2-flatjar.jar agent -f lw_solr_syslog.conf -p .

Log data should be displaying in your shell window and it should be getting stored in Solr.

Happy Logging!