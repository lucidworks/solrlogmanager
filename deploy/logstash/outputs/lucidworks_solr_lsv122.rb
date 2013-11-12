require "logstash/namespace"
require "logstash/outputs/base"
require "net/http"
require "time"
require "pry"

require "lucidworks.jar"


# LucidWorks output that pushes Logstash collected logs to Solr. 
#
# You can learn more about LucidWorks and Solr at <http://www.lucidworks.com/>
class LogStash::Outputs::LucidWorks < LogStash::Outputs::Base

  config_name "lucidworks_solr_lsv122"
  plugin_status 1
  
  # The config values are set here to default settings.  They are overridden by the 
  # logstash conf file settings.
  
  # Solr host 
  config :collection_host, :validate => :string, :default => "localhost"

  # Port (default solr port = 8983)
  config :collection_port, :validate => :number, :default => 8983
  
  # Collection name 
  config :collection_name, :validate => :string, :default => "collection1"

  # ID of predefined External connector (not used)
  config :external_connector_id, :validate => :string, :default => "-1"

  # Display collection stats (not used)
  config :collection_stats, :validate => :boolean, :default => false
  
  # Prefix will replace the @ in logstash fieldnames @timestash and @version.
  config :field_prefix, :validate => :string, :default => "logstash_"
  
	@lucidworks
	@solrFieldCreationParams
		 
  public
  def register

	  @lucidworks = Java::LWSolrLogCollectionManager.new()

		# If an incoming field does not exist in the Solr collection then addSolrDocument will attempt to create 
		# a new field entry configured per the attributes defined in solrFieldCreationParams.  
		# The format is:
		#   field name - (note that preceding and following spaces will be trimmed.)
		#   Solr field creation parameters for the named field.  Described using json format.
		#
		# Fields not described in the hash will be created with the following default Solr field definition 
		#     "[{\"type\":\"text_en\",\"name\":\"" + fieldName + "\",\"stored\":true,\"indexed\":true}]
		#
		# User organizations can add to the hashmap configurations for LogStash fields that the organization would like
		# defined differently than the default. 
		
		@solrFieldCreationParams = Hash.new
		
		# Tags are multivalued.
		@solrFieldCreationParams["tags"] = "[{\"type\":\"text_en\",\"name\":\"tags\",\"stored\":true,\"indexed\":true,\"multiValued\":true}]"
		
		# Message stored but not indexed.
		@solrFieldCreationParams["message"] = "[{\"type\":\"text_en\",\"name\":\"message\",\"stored\":true,\"indexed\":false}]"
		
		# The event timestamp is typed 'tdate'
		@solrFieldCreationParams[@field_prefix + "timestamp"] = "[{\"type\":\"tdate\",\"name\":\"" + @field_prefix + "timestamp" + "\",\"stored\":true,\"indexed\":true}]"
		
		@solrFieldCreationParams = java.util.HashMap.new(@solrFieldCreationParams)
		
		@lucidworks.init(@collection_host, @collection_port, @collection_name)
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
			when "@timestamp"		# Remove @ from field names because @ is invalid in Solr field names
				solrfields[@field_prefix + "timestamp"] = Time.parse("#{value}").iso8601
			when "@version"
				solrfields[@field_prefix + "version"] = "#{value}"
			else
				solrfields["#{key}"] = "#{value}"
			end
		}
		
		@lucidworks.addSolrDocument(java.util.HashMap.new(solrfields), @solrFieldCreationParams)
		#binding.pry
  end # def receive
end	
