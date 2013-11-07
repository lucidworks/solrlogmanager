solrlogstash
============

solr-logstash

This is an implementation of a LogStash output plugin that delivers log data to Solr.  

The deploy folder contains a working version of the plugin components and also includes a version of LogStash.  See the 
ReadMe in the deploy folder for instruction on how to install and run.   

To build the lucidworks.jar yourself from within the src folder: 
	javac javac *.java
	jar cvf lucidworks.jar *.class