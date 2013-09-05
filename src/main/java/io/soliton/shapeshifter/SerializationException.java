/**
 * Copyright 2012, 2013 Turn, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.soliton.shapeshifter;

/**
 * Thrown when an error is encountered upon serializing an Protocol Buffer
 * into JSON.
 *
 * @author jsilland
 */
@SuppressWarnings("serial")
public class SerializationException extends Exception {

  /**
   * Creates a new instance of this class with the given cause.
   *
   * @param cause the root cause
   */
  public SerializationException(Throwable cause) {
    super(cause);
  }
}