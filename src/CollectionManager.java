/*
 * Licensed to LucidWorks under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. LucidWorks licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CollectionManager {
	/**
	 * 
	 * @return unique session id.
	 */
	protected String genId() {
		return UUID.randomUUID().toString();
	}
	
	protected String checkURLEncode(String data) {
		// Escape or translate invalid characters
		String result = data;
	    result = replace("<", "&lt;", result);
	    result = replace(">", "&gt;", result);
	    result = replace("@", "%40", result);
	    result = replace("%", "%25", result);
	    result = replace("&", "%26", result);
	    return result;
	}
	
	protected String replace(String target, String replacement, String data) {
	    Pattern pattern = Pattern.compile(target);
	    Matcher match = pattern.matcher(data); 
	    return match.replaceAll(replacement);
	}
}
