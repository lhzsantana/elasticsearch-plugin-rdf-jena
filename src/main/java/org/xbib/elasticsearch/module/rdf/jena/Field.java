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

/**
 *  Interface for field names
 */
public interface Field {

    String S = "s";
    String P = "p";
    String O = "o";
    String C = "c";
    String LANG = "o_lang";
    String LONG_OBJECT = "o_l";
    String DOUBLE_OBJECT = "o_f";
    String BOOLEAN_OBJECT = "o_b";
    String DATE_OBJECT = "o_d";
    String STRING_OBJECT = "o_s";
}