package org.apache.tika.sax;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Unit tests for SafeContentHandler.characters(char[], int, int).
 *
 * NOTE: Due to the internal reliance on the 'filter' method and the 'Output' delegate
 * which are not fully provided, this test verifies the flow and boundary conditions
 * of the public characters method, ensuring it executes without error and delegates
 * correctly to the underlying handler mechanism.
 */
public class SafeContentHandler_characters_7_1_Test {

    private ContentHandler mockHandler;

    private SafeContentHandler safeContentHandler;

    @BeforeEach
    void setUp() {
        // Mock the ContentHandler dependency required by the constructor
        mockHandler = Mockito.mock(ContentHandler.class);
        // Initialize the focal class
        safeContentHandler = new SafeContentHandler(mockHandler);
    }

    @Test
    void characters_shouldProcessFullCharacterSegment_whenInputIsValid() throws SAXException {
        char[] inputChars = { 'H', 'e', 'l', 'l', 'o' };
        int start = 0;
        int length = 5;
        // Act: Call the focal method
        assertDoesNotThrow(() -> safeContentHandler.characters(inputChars, start, length));
        // Assert: Verify that the underlying handler mechanism was called with the correct segment
        // We verify that the parent ContentHandler's characters method was called with the exact parameters.
        verify(mockHandler).characters(eq(inputChars), eq(start), eq(length));
    }

    @Test
    void characters_shouldProcessPartialCharacterSegment_whenStartIsNotZero() throws SAXException {
        char[] inputChars = { 'A', 'B', 'C', 'D', 'E' };
        int start = 2;
        // Processing 'C', 'D', 'E'
        int length = 3;
        // Act: Call the focal method
        assertDoesNotThrow(() -> safeContentHandler.characters(inputChars, start, length));
        // Assert: Verify delegation
        verify(mockHandler).characters(eq(inputChars), eq(start), eq(length));
    }

    @Test
    void characters_shouldHandleZeroLengthInput_withoutThrowingException() throws SAXException {
        char[] inputChars = { 'A', 'B', 'C' };
        int start = 1;
        int length = 0;
        // Act: Call the focal method with length 0
        assertDoesNotThrow(() -> safeContentHandler.characters(inputChars, start, length));
        // Assert: Verify delegation for zero length
        verify(mockHandler).characters(eq(inputChars), eq(start), eq(length));
    }

    @Test
    void characters_shouldHandleFullArrayInput_whenStartIsZero() throws SAXException {
        char[] inputChars = { 'X', 'Y', 'Z' };
        int start = 0;
        int length = 3;
        // Act: Call the focal method covering the entire array
        assertDoesNotThrow(() -> safeContentHandler.characters(inputChars, start, length));
        // Assert: Verify delegation
        verify(mockHandler).characters(eq(inputChars), eq(start), eq(length));
    }
}
