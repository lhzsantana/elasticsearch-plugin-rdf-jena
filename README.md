![RDF](https://github.com/jprante/elasticsearch-plugin-rdf-jena/raw/master/src/site/resources/jena-logo-large.png)

Image by [Apache Jena](http://jena.apache.org)

# Elasticsearch RDF Jena Plugin

This Elasticsearch plugin stores and retrieves RDF triples by using the Apache Jena API.

[Apache Jena](http://jena.apache.org) is a free and open source Java framework for building semantic web
and Linked Data applications. The framework is composed of different APIs interacting together
to process RDF data.

Each triple will be stored as a single document, and Jena API uses term filter queries to match triples.
Due to the restrictions of such an architecture, do not expect good performance.

## Versions

| Plugin  | Elasticsearch   | Jena    | Release date |
|---------|-----------------|---------|--------------|
| 1.4.0.0 | 1.4.0           | 2.12.1  | Dec 28, 2014 |

## Installation

    ./bin/plugin --install rdf-jena --url http://xbib.org/repository/org/xbib/elasticsearch/plugin/elasticsearch-plugin-rdf-jena/1.4.0.0/elasticsearch-plugin-rdf-jena-1.4.0.0.zip

Do not forget to restart the node after installing.

## Project docs

The Maven project site is available at [Github](http://jprante.github.io/elasticsearch-plugin-rdf-jena)

## Issue

Feedback and issues are most welcome at [Github](http://github.com/jprante/elasticsearch-plugin-rdf-jena/issues)

# EXPERIMENTAL

The implementation of this plugin has just begun. Currently, only a subset of the Jena API is implemented.

# Example

Loading N-Triples

    curl '0:9200/_jena/jena/bsbm' -H 'Content-Type: application/n-triples' --data-binary @/Users/joerg/Projects/github/jprante/elasticsearch-plugin-rdf-jena/src/test/resources/bsbm-generated-dataset.nt
    
or
    
    curl -XPOST 'http://localhost:9200/_jena/jena/bsbm' -H 'Content-Type: application/n-triples' --data-binary @'src/test/resources/bsbm-generated-dataset.nt'


SPARQL Select

    curl '0:9200/_jena/jena/bsbm' --data-urlencode "query=SELECT * WHERE { ?s ?p ?o } LIMIT 10" -H "Accept: application/sparql-results+xml"
    
or
    
    curl -XPOST 'http://localhost:9200/_jena/jena/bsbm' --data-urlencode "query=SELECT * WHERE { ?s ?p ?o } LIMIT 10" -H "Accept: application/sparql-results+xml"
    
returns
    
    <?xml version="1.0"?>
    <sparql xmlns="http://www.w3.org/2005/sparql-results#">
      <head>
        <variable name="s"/>
        <variable name="p"/>
        <variable name="o"/>
      </head>
      <results>
        <result>
          <binding name="s">
            <uri>http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/ProductType1</uri>
          </binding>
          <binding name="p">
            <uri>http://purl.org/dc/elements/1.1/date</uri>
          </binding>
          <binding name="o">
            <literal datatype="http://www.w3.org/2001/XMLSchema#date">2000-07-04</literal>
          </binding>
        </result>
        <result>
          <binding name="s">
            <uri>http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/ProductType2</uri>
          </binding>
          <binding name="p">
            <uri>http://purl.org/dc/elements/1.1/publisher</uri>
          </binding>
          <binding name="o">
            <uri>http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/StandardizationInstitution1</uri>
          </binding>
        </result>
        <result>
          <binding name="s">
            <uri>http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/ProductType3</uri>
          </binding>
          <binding name="p">
            <uri>http://www.w3.org/2000/01/rdf-schema#subClassOf</uri>
          </binding>
          <binding name="o">
            <uri>http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/ProductType1</uri>
          </binding>
        </result>
        <result>
          <binding name="s">
            <uri>http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/ProductType4</uri>
          </binding>
          <binding name="p">
            <uri>http://purl.org/dc/elements/1.1/publisher</uri>
          </binding>
          <binding name="o">
            <uri>http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/StandardizationInstitution1</uri>
          </binding>
        </result>
        <result>
          <binding name="s">
            <uri>http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/ProductType5</uri>
          </binding>
          <binding name="p">
            <uri>http://www.w3.org/2000/01/rdf-schema#subClassOf</uri>
          </binding>
          <binding name="o">
            <uri>http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/ProductType2</uri>
          </binding>
        </result>
        <result>
          <binding name="s">
            <uri>http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/ProductType6</uri>
          </binding>
          <binding name="p">
            <uri>http://www.w3.org/1999/02/22-rdf-syntax-ns#type</uri>
          </binding>
          <binding name="o">
            <uri>http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/ProductType</uri>
          </binding>
        </result>
        <result>
          <binding name="s">
            <uri>http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/ProductType6</uri>
          </binding>
          <binding name="p">
            <uri>http://purl.org/dc/elements/1.1/date</uri>
          </binding>
          <binding name="o">
            <literal datatype="http://www.w3.org/2001/XMLSchema#date">2000-06-28</literal>
          </binding>
        </result>
        <result>
          <binding name="s">
            <uri>http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/ProductType7</uri>
          </binding>
          <binding name="p">
            <uri>http://purl.org/dc/elements/1.1/publisher</uri>
          </binding>
          <binding name="o">
            <uri>http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/StandardizationInstitution1</uri>
          </binding>
        </result>
        <result>
          <binding name="s">
            <uri>http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/ProductFeature1</uri>
          </binding>
          <binding name="p">
            <uri>http://purl.org/dc/elements/1.1/publisher</uri>
          </binding>
          <binding name="o">
            <uri>http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/StandardizationInstitution1</uri>
          </binding>
        </result>
        <result>
          <binding name="s">
            <uri>http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/ProductFeature2</uri>
          </binding>
          <binding name="p">
            <uri>http://www.w3.org/2000/01/rdf-schema#comment</uri>
          </binding>
          <binding name="o">
            <literal>rearms invasiveness foemen inkstand aircrew bravadoes necking enlivenment discolorations pillaging dispossessed pocketknives upsweeps monosyllables slitted secularized visualizer rescheduled graters sheepish airframes ninepin virulence ramshackle packthreads batiste pastured priors ballades tormented towpaths transfused yahweh admonishments insertions afterwards nontemporally scrawlier luxes</literal>
          </binding>
        </result>
      </results>
    </sparql>

SPARQL DESCRIBE

    curl "0:9200/_jena/jena/bsbm" --data-urlencode "query=DESCRIBE <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/dataFromRatingSite1/Reviewer1>"
    {
      "http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/dataFromRatingSite1/Reviewer1" : {
        "http://www.w3.org/1999/02/22-rdf-syntax-ns#type" : [ {
          "type" : "uri" ,
          "value" : "http://xmlns.com/foaf/0.1/Person"
        }
         ] ,
        "http://purl.org/dc/elements/1.1/publisher" : [ {
          "type" : "uri" ,
          "value" : "http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/dataFromRatingSite1/RatingSite1"
        }
         ] ,
        "http://xmlns.com/foaf/0.1/mbox_sha1sum" : [ {
          "type" : "literal" ,
          "value" : "fb3efd92e3c7a8d775a895ba476e11a3e8f3fac"
        }
         ] ,
        "http://xmlns.com/foaf/0.1/name" : [ {
          "type" : "literal" ,
          "value" : "Ruggiero-Delane"
        }
         ] ,
        "http://purl.org/dc/elements/1.1/date" : [ {
          "type" : "literal" ,
          "value" : "2008-09-05" ,
          "datatype" : "http://www.w3.org/2001/XMLSchema#date"
        }
         ] ,
        "http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/country" : [ {
          "type" : "uri" ,
          "value" : "http://downlode.org/rdf/iso-3166/countries#US"
        }
         ]
      }
    }

# Credits

This plugin is heavily based on the work of Andrea Gazzarini's [SolRDF](https://github.com/agazzarini/SolRDF)

# License

Elasticsearch RDF Jena Plugin

Copyright (C) 2014 Jörg Prante

[Follow me on twitter](https://twitter.com/xbib)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
