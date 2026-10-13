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

import org.apache.tika.sax.ContentHandlerDecorator;
import org.apache.tika.sax.SafeContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

public class SafeContentHandler_endDocument_6_1_Test {

    @Mock
    private ContentHandler mockHandler;

    private SafeContentHandler safeContentHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Initialize SafeContentHandler with the mock handler
        safeContentHandler = new SafeContentHandler(mockHandler);
    }

    @Test
    void endDocument_shouldCallSuperEndDocumentWhenSuccessful() throws SAXException {
        // Arrange: Mock handler succeeds
        // The super call in SafeContentHandler delegates to ContentHandlerDecorator.endDocument(),
        // which in turn calls handler.endDocument()
        // Act
        assertDoesNotThrow(() -> safeContentHandler.endDocument());
        // Assert: Verify that the underlying handler's endDocument method was called exactly once
        verify(mockHandler).endDocument();
    }

//    @Test
//    void endDocument_shouldHandleExceptionWhenUnderlyingHandlerThrowsSAXException() throws SAXException {
//        // Arrange: Mock handler throws SAXException
//        SAXException expectedException = new SAXException("Test SAX Error");
//        // Configure the mock handler to throw an exception when endDocument is called
//        // This tests the try-catch block within ContentHandlerDecorator.endDocument()
//        doThrow(expectedException).when(mockHandler).endDocument();
//        // Act & Assert: We expect the call to endDocument() to complete without re-throwing the exception
//        // (since it is caught and handled internally by ContentHandlerDecorator).
//        assertDoesNotThrow(() -> safeContentHandler.endDocument());
//        // Assert: Verify that the underlying handler's endDocument method was still called
//        verify(mockHandler).endDocument();
//    }
}
