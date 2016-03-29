Solr Log Manager
================

This is an implementation of a Logstash output plugin that delivers log data to Solr.  


### QUICKSTART


1. Copy the `logstash-2.2.2` and `logstash-output-lucidworks_solr` folders to your disk. These contain a working version of Logstash and  the Solr output plugin.

2. Copy provided ManagedIndexSchemaFactory configured `logstash_logs` directory to your Solr collection folder.

3. If you are on a older version of Solr (before 4.4), go to the Solr Admin Page and add a new core.  In the name and instanceDir fields enter: logstash_logs. Accept the defaults for the remaining fields and click on the 'Add Core' button.

4. For a start-up example, we will use syslog:

   Open `lw_solr_syslog.conf` in a text editor. In the '_input_' section of the config file, add the path to your log file[s] by changing
       path => [ "/var/log/system.log" ]
   If your Solr instance is not running on the default localhost:8983, then change the collection_host and collection_port values in the _ouptut_ section of the config file.

5. For the Syslog example, execute the following curl command (making sure to change the port)
        curl 'http://localhost:8983/solr/logstash_logs/schema/fields' -X POST -H 'Content-type: application/json' --data-binary '[{"name":"syslog_program","type":"string","indexed":true,"stored":true}]'
    This will make sure you are faceting correctly on the program field.

6. Run Logstash:
        $ logstash-2.2.2/bin/logstash agent -f lw_solr_syslog.conf

Log data should now be displaying in your shell window and it should be getting stored in Solr.

Happy Logging!


### More Information

The MANUAL.md file contains more details about configuring and running Logstash and writing to Solr.

To build the lucidworks.jar yourself from within the src folder:

	javac *.java
	jar cvf lucidworks.jar *.class
