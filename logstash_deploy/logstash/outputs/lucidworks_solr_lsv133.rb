Dir["jars/*.jar"].each { |jar| require jar }

# for development, easiest to import the JAR that Maven built,
# instead of:
#require "solrlogmanager.jar"
Dir["../target/solrlogmanager*.jar"].each { |jar| require jar }
require "logstash/namespace"
require "logstash/outputs/base"
require "net/http"

require "time"
require 'active_support'
require "pry"



# Lucidworks output that pushes Logstash collected logs to Solr.
#
# You can learn more about Lucidworks and Solr at <http://www.lucidworks.com/>
class LogStash::Outputs::Lucidworks < LogStash::Outputs::Base


  config_name "lucidworks_solr_lsv133"
  milestone 1
  
  # The config values are set here to default settings.  They are overridden by the 
  # logstash conf file settings.

  config :zk_host, :validate => :string, :default => "localhost:2181"
  
  # Solr host 
  config :collection_host, :validate => :string

  # Port (default solr port = 8983)
  config :collection_port, :validate => :number
  
  # Collection name 
  config :collection_name, :validate => :string

  # Prefix will replace the @ in logstash fieldnames @timestash and @version.
  config :field_prefix, :validate => :string, :default => "logstash_"
 
  # Solr solrconfig.xml can be configured to automatically commit documents after a 
  # specified amount of time or after receipt of a maximum number of documents.  For convenience 
  # allow the logger to override this setting for use in cases where the site either isn't 
  # setting the configuration or where it is desired that logs be available for review sooner than the
  # xml configuration allows.
  #
  # If false then rely on the solrconfig setting.  If true then force an immediate commit for each received document. 
  # Note that it takes this module ~twice as long to process each document when this setting is true.
  config :force_commit, :validate => :boolean, :default => false
 
  # Number of events to queue up before writing to Solr
  config :flush_size, :validate => :number, :default => 100

  # Amount of time since the last flush before a flush is done even if
  # the number of buffered events is smaller than flush_size
  config :idle_flush_time, :validate => :number, :default => 1

  config :queue_size, :validate => :number, :default => 500

  config :id_field, :validate => :string, :default => "id"

  @lucidworks
 
  public
  def register
    @lucidworks = Java::LWSolrLogCollectionManager.new()

    # All fields are expected to either already be defined in the collection schema.  Or, expect that a managed-schema is being used.
    # As a convenience we here try to ensure that two mandatory SiLK fields exist and if they do not we will try and
    # have Solr create them.

    default_params = nil # was Hash.new - LWSolrLogCollectionManager does more object creation when params not null

    # public void init(String zkHost, String idField, String collection, boolean forceCommit, SolrParams defaultParams, int queueSize) {
    #@lucidworks.init(@zk_host, @id_field, @collection_name, @force_commit, default_params, @queue_size)

    # public void init(String solrURL, String idField, String collection, int queueSize, int threadCount, boolean forceCommit, SolrParams defaultParams)
    @lucidworks.init("http://localhost:8983/solr/", @id_field, @collection_name, @queue_size, 0, @force_commit, default_params)

  	#@lucidworks.createSchemaField(@field_prefix + "timestamp", "tdate", true, true)
  	#@lucidworks.createSchemaField(@field_prefix + "version", "long", true, true)
   
=begin
    buffer_initialize(
      :max_items => @flush_size,
      :max_interval => @idle_flush_time,
      :logger => @logger
    )
=end
  end # def register

  public
  def receive(event)
    return unless output?(event)

    # NOTES: 
    #   1) Field names must conform to Solr field naming conventions.
    #   2) The tags field is expected to be an collection of name/value pairs.  They are here joined by commas and are
    #      later stored in the collection as multiple select field items.
    #   3) If in the future there are other fields that are collection types then a new case must be added here to either treat them as
    #      tags or to break them out as individual name/value field items as is expected in the final 'else' below.     
    #   4) Each item stored in a Solr index must have a unique ID.  You can manage the ID's by adding your own ID to 
    #      solrfields collection.  If that field is not added here then addSolrDocument call will automatically generate a GUIID 
    #      that will be the record ID.  (Note that if you pass an existing ID then the associated Solr record's data will be 
    #      overwritten.
    solrfields = Hash.new
    lucidfields = event.to_hash
    lucidfields.each { |key,value|
      case key
      when "tags"
        solrfields["#{key}"]= value.join(",")
      when "@timestamp"   
        # @timestamp looks like - 2014-03-25 21:48:35 -0700 
        # Solr's format is 2014-03-25T23:48:35.591 and @ is invalid in Solr field names so fix here.
        solrfields[@field_prefix + "timestamp"] = DateTime.iso8601(Time.parse("#{value}").iso8601).strftime('%Y-%m-%dT%H:%M:%S.%LZ')
      when "@version"
        solrfields[@field_prefix + "version"] = "#{value}"
      else
        solrfields["#{key}"] = "#{value}"
      end
    }
    
    begin
      @lucidworks.addSolrDocument(java.util.HashMap.new(solrfields))
    rescue Exception => e
      puts "Exception occurred constructing new solr document - " + e.message
    end
    #binding.pry
  end # def receive
  
  def flush(events, teardown=false)
    puts "Lucidworks FLUSH: #{solrfields}"

    # TODO: LET SOLR HANDLE THIS STUFF
=begin
  	begin
   		documents = "" 
    	events.each do |event|
    		documents += event
    	end

   		@lucidworks.flushDocs(documents)
 
  	rescue Exception => e
    	@logger.warn("An error occurred while flushing events: #{e.message}")
		end
=end
  end #def flush

  def teardown
    puts "Lucidworks TEARDOWN"
    @lucidworks.close

    super
  end

end 
