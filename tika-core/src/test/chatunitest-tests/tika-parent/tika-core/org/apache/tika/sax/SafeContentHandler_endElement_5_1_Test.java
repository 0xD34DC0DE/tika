package org.apache.tika.sax;

import org.apache.tika.sax.ContentHandlerDecorator;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Note: Due to the provided structure where SafeContentHandler calls super.endElement,
 * we assume SafeContentHandler extends a class (like ContentHandlerDecorator)
 * and that the underlying ContentHandler passed to the constructor is the target
 * for verification.
 */
public class SafeContentHandler_endElement_5_1_Test {

    private ContentHandler mockHandler;

    private SafeContentHandler safeContentHandler;

    @BeforeEach
    void setUp() {
        // We mock the underlying ContentHandler that SafeContentHandler should delegate to.
        // Although the provided SafeContentHandler structure is complex, we treat the
        // constructor dependency as the point of control.
        mockHandler = Mockito.mock(ContentHandler.class);
        // Instantiate SafeContentHandler. We must assume SafeContentHandler has a constructor
        // that accepts a ContentHandler, matching the provided signature.
        safeContentHandler = new SafeContentHandler(mockHandler);
    }

    @Test
    void endElement_shouldCallSuperEndElementWithCorrectParameters_onSuccess() throws SAXException {
        String uri = "http://example.com";
        String localName = "tag";
        String name = "element";
        // Act
        assertDoesNotThrow(() -> safeContentHandler.endElement(uri, localName, name));
        // Assert: Verify that the super method (which delegates to the underlying handler)
        // was called exactly once with the provided parameters.
        verify(mockHandler, times(1)).endElement(uri, localName, name);
    }

    @Test
    void endElement_shouldPropagateSAXException_whenSuperThrowsIt() throws SAXException {
        String uri = "http://example.com";
        String localName = "tag";
        String name = "element";
        SAXException expectedException = new SAXException("Test SAX error");
        // Configure the mock handler to throw an exception when endElement is called
        Mockito.doThrow(expectedException).when(mockHandler).endElement(uri, localName, name);
        // Act & Assert: Verify that the SAXException is propagated up
        SAXException thrown = assertThrows(SAXException.class, () -> {
            safeContentHandler.endElement(uri, localName, name);
        });
        // Verify the exception content
        org.junit.jupiter.api.Assertions.assertEquals("Test SAX error", thrown.getMessage());
        // Ensure the call was attempted
        verify(mockHandler, times(1)).endElement(uri, localName, name);
    }
}
