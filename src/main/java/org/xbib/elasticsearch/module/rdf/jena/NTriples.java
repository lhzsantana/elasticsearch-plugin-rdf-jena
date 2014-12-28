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

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.rdf.model.AnonId;

public class NTriples {
    private static final String START_URI_CHAR = "<";
    private static final String END_URI_CHAR = ">";
    private static final String START_BNODE_CHARS = "_:";
    private static final String START_LITERAL_CHAR = "\"";
    private static final String LANGUAGE_MARKER = "@";
    private static final String DATATYPE_MARKER = "^^";

    static boolean isURI(String nt) {
        return nt != null && nt.startsWith(START_URI_CHAR) && nt.endsWith(END_URI_CHAR);
    }

    static boolean isBlankNode(String nt) {
        return nt != null && nt.startsWith(START_BNODE_CHARS);
    }

    public static Node asNode(String nt) {
        if (isURI(nt)) {
            return internalAsURI(nt);
        } else if (isBlankNode(nt)) {
            return internalAsBlankNode(nt);
        }
        return asLiteral(nt);
    }

    public static Node asURIorBlankNode(String nt) {
        return (isURI(nt)) ? internalAsURI(nt) : internalAsBlankNode(nt);
    }

    public static Node asURI(String nt) {
        if (isURI(nt)) {
            return internalAsURI(nt);
        }
        throw new IllegalArgumentException(nt);
    }

    private static Node internalAsURI(String uriAsString) {
        return NodeFactory.createURI(unescape(uriAsString.substring(1, uriAsString.length() - 1)));
    }

    private static Node internalAsBlankNode(String blankNodeAsString) {
        return NodeFactory.createAnon(AnonId.create(blankNodeAsString.substring(2)));
    }

    public static Node asLiteral(String literal) {
        if (literal.startsWith(START_LITERAL_CHAR)) {
            int endIndexOfValue = endIndexOfValue(literal);
            if (endIndexOfValue != -1) {
                String literalValue = unescape(literal.substring(1, endIndexOfValue));
                int startIndexOfLanguage = literal.indexOf(LANGUAGE_MARKER, endIndexOfValue);
                int startIndexOfDatatype = literal.indexOf(DATATYPE_MARKER, endIndexOfValue);
                if (startIndexOfLanguage != -1) {
                    return NodeFactory.createLiteral(
                            literalValue,
                            literal.substring(startIndexOfLanguage + LANGUAGE_MARKER.length()),
                            null);
                } else if (startIndexOfDatatype != -1) {
                    return NodeFactory.createLiteral(
                            literalValue,
                            null,
                            NodeFactory.getType(literal.substring(startIndexOfDatatype + DATATYPE_MARKER.length())));
                } else {
                    return NodeFactory.createLiteral(literalValue);
                }
            }
        }
        throw new IllegalArgumentException(literal);
    }

    private static int endIndexOfValue(String literalValue) {
        boolean previousWasBackslash = false;
        for (int i = 1; i < literalValue.length(); i++) {
            char c = literalValue.charAt(i);
            if (c == '"' && !previousWasBackslash) {
                return i;
            } else if (c == '\\' && !previousWasBackslash) {
                previousWasBackslash = true;
            } else if (previousWasBackslash) {
                previousWasBackslash = false;
            }
        }
        return -1;
    }

    public static String asNt(Node node) {
        if (node.isURI()) {
            return asNtURI(node);
        } else if (node.isBlank()) {
            return asNtBlankNode(node);
        } else if (node.isLiteral()) {
            return asNtLiteral(node);
        }
        throw new IllegalArgumentException(node.getClass().getName());
    }

    public static String asNtURI(Node uri) {
        StringBuilder buffer = new StringBuilder("<");
        escapeAndAppend(uri.getURI(), buffer);
        return buffer.append(">").toString();
    }

    public static String asNtBlankNode(Node blankNode) {
        return "_:" + blankNode.getBlankNodeLabel();
    }

    public static String asNtLiteral(Node literal) {
        StringBuilder buffer = new StringBuilder("\"");
        escapeAndAppend(String.valueOf(literal.getLiteral().getLexicalForm()), buffer);
        buffer.append("\"");
        String language = literal.getLiteralLanguage();
        if (language != null && !language.isEmpty()) {
            buffer.append("@").append(language);
        }
        String datatypeURI = literal.getLiteralDatatypeURI();
        if (datatypeURI != null) {
            buffer.append("^^");
            escapeAndAppend(datatypeURI, buffer);
        }
        return buffer.toString();
    }

    private static void escapeAndAppend(String value, StringBuilder buffer) {
        int labelLength = value.length();
        for (int i = 0; i < labelLength; i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\':
                    buffer.append("\\\\");
                    break;
                case '\"':
                    buffer.append("\\\"");
                    break;
                case '\n':
                    buffer.append("\\n");
                    break;
                case '\r':
                    buffer.append("\\r");
                    break;
                case '\t':
                    buffer.append("\\t");
                    break;
                default:
                    if ((int) c >= 0x0 && (int) c <= 0x8 || (int) c == 0xB || (int) c == 0xC || (int) c >= 0xE && (int) c <= 0x1F
                            || (int) c >= 0x7F && (int) c <= 0xFFFF) {
                        buffer.append("\\u");
                        buffer.append(hex((int) c, 4));
                    } else if ((int) c >= 0x10000 && (int) c <= 0x10FFFF) {
                        buffer.append("\\U");
                        buffer.append(hex((int) c, 8));
                    } else {
                        buffer.append(c);
                    }
            }
        }
    }

    public static String unescape(String value) {
        int indexOfBackSlash = value.indexOf('\\');
        if (indexOfBackSlash == -1) {
            return value;
        }
        int startIndexOfEscapedSequence = 0;
        int valueLength = value.length();
        StringBuilder builder = new StringBuilder(valueLength);
        while (indexOfBackSlash != -1) {
            builder.append(value.substring(startIndexOfEscapedSequence, indexOfBackSlash));
            if (indexOfBackSlash + 1 >= valueLength) {
                throw new IllegalArgumentException(value);
            }
            char c = value.charAt(indexOfBackSlash + 1);
            switch (c) {
                case 't':
                    builder.append('\t');
                    startIndexOfEscapedSequence = indexOfBackSlash + 2;
                    break;
                case 'b':
                    builder.append('\b');
                    startIndexOfEscapedSequence = indexOfBackSlash + 2;
                    break;
                case 'n':
                    builder.append('\n');
                    startIndexOfEscapedSequence = indexOfBackSlash + 2;
                    break;
                case 'r':
                    builder.append('\r');
                    startIndexOfEscapedSequence = indexOfBackSlash + 2;
                    break;
                case 'f':
                    builder.append('\f');
                    startIndexOfEscapedSequence = indexOfBackSlash + 2;
                    break;
                case '"':
                    builder.append('"');
                    startIndexOfEscapedSequence = indexOfBackSlash + 2;
                    break;
                case '\'':
                    builder.append('\'');
                    startIndexOfEscapedSequence = indexOfBackSlash + 2;
                    break;
                case '\\':
                    builder.append('\\');
                    startIndexOfEscapedSequence = indexOfBackSlash + 2;
                    break;
                case 'u':
                    if (indexOfBackSlash + 5 >= valueLength) {
                        throw new IllegalArgumentException(value);
                    }
                    String hex5 = value.substring(indexOfBackSlash + 2, indexOfBackSlash + 6);
                    try {
                        c = (char) Integer.parseInt(hex5, 16);
                        builder.append(c);
                        startIndexOfEscapedSequence = indexOfBackSlash + 6;
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Bad unicode escape sequence '\\u" + hex5 + "' in: " + value);
                    }
                    break;
                case 'U':
                    if (indexOfBackSlash + 9 >= valueLength) {
                        throw new IllegalArgumentException(value);
                    }
                    String hex9 = value.substring(indexOfBackSlash + 2, indexOfBackSlash + 10);
                    try {
                        c = (char) Integer.parseInt(hex9, 16);
                        builder.append(c);
                        startIndexOfEscapedSequence = indexOfBackSlash + 10;
                    } catch (NumberFormatException exception) {
                        throw new IllegalArgumentException("Bad unicode escape sequence '\\U" + hex9 + "' in: " + value);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unescaped backslash found in: " + value);
            }
            indexOfBackSlash = value.indexOf('\\', startIndexOfEscapedSequence);
        }
        builder.append(value.substring(startIndexOfEscapedSequence));
        return builder.toString();
    }

    private static String hex(int decimal, int length) {
        StringBuilder builder = new StringBuilder(length);
        String hex = Integer.toHexString(decimal).toUpperCase();
        int bound = length - hex.length();
        for (int i = 0; i < bound; i++) {
            builder.append('0');
        }
        return builder.append(hex).toString();
    }
}