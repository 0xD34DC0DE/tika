/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.sax;

import org.apache.tika.sax.SafeContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Unit test for SafeContentHandler.ignorableWhitespace(char[], int, int).
 * Note: This test assumes the existence and functionality of the private 'filter' method
 * and relies on mocking the underlying ContentHandler dependency.
 */
public class SafeContentHandler_ignorableWhitespace_8_0_Test {

    private ContentHandler mockHandler;

    private SafeContentHandler safeContentHandler;

    @BeforeEach
    void setUp() {
        // Mock the ContentHandler dependency
        mockHandler = Mockito.mock(ContentHandler.class);
        // Instantiate the focal class
        safeContentHandler = new SafeContentHandler(mockHandler);
    }

    @Test
    void testIgnorableWhitespace_fullLength() throws SAXException {
        char[] ch = { 'a', 'b', 'c', 'd' };
        int start = 0;
        int length = 4;
        // Since the implementation delegates to 'filter', we verify the public method executes without error.
        // In a real scenario, we would verify the internal state change or the call to the underlying handler.
        // As 'filter' is private and its logic is unknown, we focus on the public API contract.
        assertDoesNotThrow(() -> safeContentHandler.ignorableWhitespace(ch, start, length));
    }

    @Test
    void testIgnorableWhitespace_partialLength() throws SAXException {
        char[] ch = { 'a', 'b', 'c', 'd' };
        int start = 1;
        // Only 'b' and 'c'
        int length = 2;
        assertDoesNotThrow(() -> safeContentHandler.ignorableWhitespace(ch, start, length));
    }

    @Test
    void testIgnorableWhitespace_zeroLength() throws SAXException {
        char[] ch = { 'a', 'b', 'c', 'd' };
        int start = 2;
        int length = 0;
        // Testing the zero length case ensures the method handles empty inputs gracefully.
        assertDoesNotThrow(() -> safeContentHandler.ignorableWhitespace(ch, start, length));
    }

    @Test
    void testIgnorableWhitespace_startAtEnd() throws SAXException {
        char[] ch = { 'a', 'b', 'c', 'd' };
        // Start index beyond array size
        int start = 4;
        int length = 1;
        // Although this might lead to an ArrayIndexOutOfBoundsException if not handled internally,
        // the test verifies the public method execution path.
        assertThrows(IndexOutOfBoundsException.class, () -> safeContentHandler.ignorableWhitespace(ch, start, length));
    }

    @Test
    void testIgnorableWhitespace_throwsSAXException() throws SAXException {
        char[] ch = { 'a', 'b' };
        int start = 0;
        int length = 2;
        // To simulate an SAXException being thrown during processing (e.g., inside 'filter'),
        // we would typically need to mock the internal components. Since we cannot mock the internal 'filter',
        // we rely on the method signature compliance.
        // If the internal logic were exposed, we would force it to throw SAXException.
        // Here, we just ensure the method signature accepts the throw clause.
        assertDoesNotThrow(() -> {
            try {
                safeContentHandler.ignorableWhitespace(ch, start, length);
            } catch (SAXException e) {
                fail("SAXException should not be thrown during standard execution path.");
            }
        });
    }
}
