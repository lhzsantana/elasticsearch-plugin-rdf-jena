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

import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QueryDecoder {

    private final static Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    private static final int DEFAULT_MAX_PARAMS = 1024;

    private final Charset charset;

    private final String uri;

    private final String body;

    private final boolean hasPath;

    private final int maxParams;

    private String path;

    private ParameterMap params;

    private int nParams;

    public QueryDecoder(String uri, String body) {
        this(uri, body, DEFAULT_CHARSET);
    }

    public QueryDecoder(String uri, String body, Charset charset) {
        this(uri, body, charset, true);
    }

    public QueryDecoder(String uri, String body, Charset charset, boolean hasPath) {
        this(uri, body, charset, hasPath, DEFAULT_MAX_PARAMS);
    }

    public QueryDecoder(String uri, String body, Charset charset, boolean hasPath, int maxParams) {
        if (uri == null) {
            throw new NullPointerException("uri");
        }
        if (charset == null) {
            throw new NullPointerException("charset");
        }
        if (maxParams <= 0) {
            throw new IllegalArgumentException("maxParams: " + maxParams + " (expected: a positive integer)");
        }
        this.uri = uri;
        this.body = body;
        this.charset = charset;
        this.maxParams = maxParams;
        this.hasPath = hasPath;
    }

    public String uri() {
        return uri;
    }

    public String path() {
        if (path == null) {
            if (!hasPath) {
                return path = "";
            }
            int pathEndPos = uri.indexOf('?');
            if (pathEndPos < 0) {
                path = uri;
            } else {
                return path = uri.substring(0, pathEndPos);
            }
        }
        return path;
    }

    public ParameterMap parameters() {
        if (params == null) {
            this.params = new ParameterMap();
            decodeParams(body);
            if (hasPath) {
                int pathLength = path().length();
                if (uri.length() != pathLength) {
                    decodeParams(uri.substring(pathLength + 1));
                }
            } else {
                decodeParams(uri);
            }
        }
        return params;
    }

    private void decodeParams(String s) {
        if (s == null || s.isEmpty()) {
            return;
        }
        nParams = 0;
        String name = null;
        int pos = 0;
        int i;
        char c;
        for (i = 0; i < s.length(); i++) {
            c = s.charAt(i);
            if (c == '=' && name == null) {
                if (pos != i) {
                    name = decodeComponent(s.substring(pos, i), charset);
                }
                pos = i + 1;
            } else if (c == '&' || c == ';') {
                if (name == null && pos != i) {
                    if (!addParam(params, decodeComponent(s.substring(pos, i), charset), "")) {
                        return;
                    }
                } else if (name != null) {
                    if (!addParam(params, name, decodeComponent(s.substring(pos, i), charset))) {
                        return;
                    }
                    name = null;
                }
                pos = i + 1;
            }
        }

        if (pos != i) {
            if (name == null) {
                addParam(params, decodeComponent(s.substring(pos, i), charset), "");
            } else {
                addParam(params, name, decodeComponent(s.substring(pos, i), charset));
            }
        } else if (name != null) {
            addParam(params, name, "");
        }
    }

    private boolean addParam(Map<String, List<String>> params, String name, String value) {
        if (nParams >= maxParams) {
            return false;
        }
        List<String> values = params.get(name);
        if (values == null) {
            values = new ArrayList<String>(1);
            params.put(name, values);
        }
        values.add(value);
        nParams++;
        return true;
    }

    public static String decodeComponent(final String s) {
        return decodeComponent(s, DEFAULT_CHARSET);
    }

    public static String decodeComponent(final String s, final Charset charset) {
        if (s == null) {
            return "";
        }
        final int size = s.length();
        boolean modified = false;
        for (int i = 0; i < size; i++) {
            final char c = s.charAt(i);
            if (c == '%' || c == '+') {
                modified = true;
                break;
            }
        }
        if (!modified) {
            return s;
        }
        final byte[] buf = new byte[size];
        int pos = 0;
        for (int i = 0; i < size; i++) {
            char c = s.charAt(i);
            switch (c) {
                case '+':
                    buf[pos++] = ' ';
                    break;
                case '%':
                    if (i == size - 1) {
                        throw new IllegalArgumentException("unterminated escape sequence at end of string: " + s);
                    }
                    c = s.charAt(++i);
                    if (c == '%') {
                        buf[pos++] = '%';
                        break;
                    }
                    if (i == size - 1) {
                        throw new IllegalArgumentException("partial escape"
                                + " sequence at end of string: " + s);
                    }
                    c = decodeHexNibble(c);
                    final char c2 = decodeHexNibble(s.charAt(++i));
                    if (c == Character.MAX_VALUE || c2 == Character.MAX_VALUE) {
                        throw new IllegalArgumentException(
                                "invalid escape sequence `%" + s.charAt(i - 1)
                                        + s.charAt(i) + "' at index " + (i - 2)
                                        + " of: " + s);
                    }
                    c = (char) (c * 16 + c2);
                default:
                    buf[pos++] = (byte) c;
                    break;
            }
        }
        return new String(buf, 0, pos, charset);
    }

    private static char decodeHexNibble(final char c) {
        if ('0' <= c && c <= '9') {
            return (char) (c - '0');
        } else if ('a' <= c && c <= 'f') {
            return (char) (c - 'a' + 10);
        } else if ('A' <= c && c <= 'F') {
            return (char) (c - 'A' + 10);
        } else {
            return Character.MAX_VALUE;
        }
    }

    public class ParameterMap extends LinkedHashMap<String, List<String>> {

        public String get(String key, String defaultValue) {
            return containsKey(key) ? get(key).get(0) : defaultValue;
        }

        public Integer getAsInt(String key, Integer defaultValue) {
            String value = containsKey(key) ? get(key).get(0) : Integer.toString(defaultValue);
            try {
                return !value.isEmpty() ? Integer.parseInt(value) : defaultValue;
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        public Map<String, String> asFlatMap() {
            Map<String, String> map = new LinkedHashMap<String, String>();
            for (String key : keySet()) {
                map.put(key, get(key).get(0));
            }
            return map;
        }

    }
}