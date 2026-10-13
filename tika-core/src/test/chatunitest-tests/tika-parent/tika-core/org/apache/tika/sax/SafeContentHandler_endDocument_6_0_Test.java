package org.apache.tika.sax;

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

// Note: Since the provided SafeContentHandler code relies on inheritance (calling super.endDocument())
// and uses internal fields (charactersOutput, ignorableWhitespaceOutput) which are not fully defined
// in the snippet, we must assume the structure is a wrapper/decorator.
// We will mock the delegate ContentHandler passed into the constructor.
public class SafeContentHandler_endDocument_6_0_Test {

    private ContentHandler mockDelegate;

    private SafeContentHandler safeContentHandler;

    @BeforeEach
    void setUp() {
        // 1. Setup Mock Delegate
        mockDelegate = mock(ContentHandler.class);
        // 2. Instantiate the Focal Class
        // We assume the constructor takes the delegate ContentHandler
        safeContentHandler = new SafeContentHandler(mockDelegate);
    }

    @Test
    void endDocument_shouldCallSuperEndDocumentOnDelegate() throws SAXException {
        // GIVEN: The delegate ContentHandler is set up
        // WHEN: endDocument is called on SafeContentHandler
        assertDoesNotThrow(() -> safeContentHandler.endDocument());
        // THEN: Verify that the delegate's endDocument method was called exactly once
        verify(mockDelegate, times(1)).endDocument();
    }

    @Test
    void endDocument_shouldPropagateSAXExceptionFromDelegate() throws SAXException {
        // GIVEN: The delegate ContentHandler is set up to throw an exception
        SAXException expectedException = new SAXException("Test Exception");
        doThrow(expectedException).when(mockDelegate).endDocument();
        // WHEN / THEN: Calling endDocument should throw the exception originating from the delegate
        SAXException thrown = org.junit.jupiter.api.Assertions.assertThrows(SAXException.class, () -> {
            safeContentHandler.endDocument();
        });
        // Verify the exception details
        org.junit.jupiter.api.Assertions.assertEquals("Test Exception", thrown.getMessage());
        // Ensure the delegate method was still called
        verify(mockDelegate, times(1)).endDocument();
    }
}
