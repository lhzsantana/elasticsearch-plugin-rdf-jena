/**
 *    Copyright 2014 Jörg Prante
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.xbib.elasticsearch.plugin.rdf.jena;

import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;
import org.xbib.elasticsearch.rest.rdf.jena.RestJenaAction;


public class JenaPlugin extends AbstractPlugin {

    public final static String NAME = "rdf-jena";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "RDF Jena plugin";
    }

    public void onModule(RestModule module) {
        module.addRestAction(RestJenaAction.class);
    }

}
