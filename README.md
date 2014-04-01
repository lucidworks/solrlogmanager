Solr Log Manager
============

This is an implementation of a LogStash output plugin that delivers log data to Solr.  


### QUICKSTART


1. Copy the logstash_deploy folder to your disk. This contains a working version of LogStash and you can run LogStash and the Solr Output Writer from this directory.

2. Copy provided ManagedIndexSchemaFactory configured logstash_logs directory to your Solr collection folder.  If you rename the folder, also appropriately modify the core.properties.

3. If you are on a older version of Solr (before 4.4), go to the Solr Admin Page and add a new core.  In the name and instanceDir fields enter: logstash_logs. Accept the defaults for the remaining fields and click on the 'Add Core' button.

4. For a start-up example, we will use syslogs:
   Open lw_solr_syslog.conf in a text editor.  In the 'input' section of the config file add the path to your log file[s] by changing path => [ "/var/log/system.log" ]
If your Solr instance is not running on the default localhost:8983 then change the collection_host and collection_port values in the 
   ouptut section of the config file.

5. For the Sylog example, execute the following curl command (making sure to change the port)
curl 'http://localhost:8983/solr/logstash_logs/schema/fields' -X POST -H 'Content-type: application/json' --data-binary '
[{"name":"syslog_program","type":"string","indexed":true,"stored":true}]'
This will make sure you are faceting correctly on the program field.
 
6. Execute LogStash: java -jar logstash-1.3.3-flatjar.jar agent -f lw_solr_syslog.conf -p .

Log data should be displaying in your shell window and it should be getting stored in Solr.

Happy Logging!


### More Information

The MANUAL.txt file located in the logstash_deploy folder contains more details about configuring and running LogStash and writing to Solr.   

To build the lucidworks.jar yourself from within the src folder: 
	javac javac *.java
	jar cvf lucidworks.jar *.class
	