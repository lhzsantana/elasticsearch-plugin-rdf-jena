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
package org.xbib.elasticsearch.module.rdf.jena;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;

public interface Datatypes {

    String XSD_BOOLEAN = XSDDatatype.XSDboolean.getURI();
    String XSD_INT = XSDDatatype.XSDint.getURI();
    String XSD_INTEGER = XSDDatatype.XSDinteger.getURI();
    String XSD_DECIMAL = XSDDatatype.XSDdecimal.getURI();
    String XSD_DOUBLE = XSDDatatype.XSDdouble.getURI();
    String XSD_LONG = XSDDatatype.XSDlong.getURI();
    String XSD_DATE = XSDDatatype.XSDdate.getURI();
    String XSD_DATETIME = XSDDatatype.XSDdateTime.getURI();

}
