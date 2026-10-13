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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

public class SafeContentHandlerManualTest {

    // 1. Déclaration de la sous-classe pour exposer la méthode protected
    private static class TestableSafeContentHandler extends SafeContentHandler {
        public TestableSafeContentHandler(ContentHandler handler) {
            super(handler);
        }
        public boolean checkInvalid(int ch) {
            return super.isInvalid(ch);
        }
    }

    @Test
    public void testFiltreMutantsLignes86Et87() throws SAXException {
        List<Integer> longueursEcrites = new ArrayList<>();
        DefaultHandler trackerHandler = new DefaultHandler() {
            @Override
            public void characters(char[] ch, int start, int length) {
                longueursEcrites.add(length);
            }
        };
        SafeContentHandler safeHandler = new SafeContentHandler(trackerHandler);

        char[] arrayAvecStart = "xab\u000B".toCharArray(); 
        safeHandler.characters(arrayAvecStart, 1, 3);
        assertEquals(3, longueursEcrites.size());
        assertEquals(2, longueursEcrites.get(0));

        longueursEcrites.clear();
        char[] arrayInvalideImmediat = "\u000B".toCharArray(); 
        safeHandler.characters(arrayInvalideImmediat, 0, 1);
        assertEquals(2, longueursEcrites.size());
    }

    @Test
    public void testLimitesUnicodeIsInvalid() {
        TestableSafeContentHandler safeHandler = new TestableSafeContentHandler(new DefaultHandler());

        // Lignes 143 et 144
        assertFalse(safeHandler.checkInvalid(0xD7FF)); 
        assertTrue(safeHandler.checkInvalid(0xD800));  
        assertTrue(safeHandler.checkInvalid(0xDFFF));  
        assertFalse(safeHandler.checkInvalid(0xE000)); 

        // Ligne 145
        assertFalse(safeHandler.checkInvalid(0xFFFD)); 
        assertTrue(safeHandler.checkInvalid(0xFFFE));  
        assertTrue(safeHandler.checkInvalid(0xFFFF));
        assertFalse(safeHandler.checkInvalid(0x10000));

        // Ligne 147
        assertFalse(safeHandler.checkInvalid(0x10FFFF)); 
        assertTrue(safeHandler.checkInvalid(0x110000));  
    }

    @Test
    public void testCaracteresDeControleIsInvalid() {
        TestableSafeContentHandler safeHandler = new TestableSafeContentHandler(new DefaultHandler());

        // Lignes 140 et 141
        assertTrue(safeHandler.checkInvalid(0x00));
        assertTrue(safeHandler.checkInvalid(0x1F));
        assertFalse(safeHandler.checkInvalid(0x09));
        assertFalse(safeHandler.checkInvalid(0x0A));
        assertFalse(safeHandler.checkInvalid(0x0D));
        assertFalse(safeHandler.checkInvalid(0x20));
    }

    @Test
    public void testStartElementAvecAttributInvalide() throws SAXException {
        // L'espion capture les attributs modifiés qui sont envoyés au ContentHandler parent
        List<AttributesImpl> attributsCaptures = new ArrayList<>();
        DefaultHandler trackerHandler = new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) {
                attributsCaptures.add(new AttributesImpl(attributes));
            }
        };
        
        SafeContentHandler safeHandler = new SafeContentHandler(trackerHandler);
        AttributesImpl atts = new AttributesImpl();
        
        // On teste un attribut valide et un attribut invalide simultanément
        atts.addAttribute("", "attr1", "attr1", "CDATA", "valeurValide");
        atts.addAttribute("", "attr2", "attr2", "CDATA", "valeur\u000Binvalide");
        
        safeHandler.startElement("", "test", "test", atts);
        
        // L'oracle de test : on s'assure que le filtre a fait son travail
        assertEquals(1, attributsCaptures.size());
        AttributesImpl resultAtts = attributsCaptures.get(0);
        
        assertEquals("valeurValide", resultAtts.getValue(0));
        assertEquals("valeur\ufffdinvalide", resultAtts.getValue(1), "Le caractère invalide n'a pas été filtré correctement.");
    }
}
