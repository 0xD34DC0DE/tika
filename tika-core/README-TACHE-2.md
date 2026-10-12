# 1. Sélection des classes à tester

Nous avons utilisé les métriques de couverture générées par JaCoCo pour identifier les classes candidates selon les
critères spécifiés de la tâche 2. Dans les rapports de couverture, plusieurs classes étaient couvertes par les tests
de manière partielle. Nous avons donc réduit l'ensemble de candidats en ne considérant que les classes ayant les
propriétés qualitatives suivantes :

- La classe contient de la logique métier avec une couverture partielle
- La classe n'est pas trop grande et complexe
- Les lignes de code non couvertes ne sont pas des méthodes triviales ou utilitaires (exemple : get, set, etc...)
- Les tests existants n'indiquent pas que la création de tests supplémentaires soit potentiellement compliqué

Avec ces critères supplémentaires, nous avons décidé de mettre de côté les classes des modules `tika-parser-ocr-module`,
`tika-parser-pdf-module` et `tika-parser-microsoft-module` dû à leur complexité et leur taille.
Parmi les candidats restants des modules `tika-core` et `tika-serialization`, nous avons identifié deux classes qui
nous semblaient être d'excellents sujets pour la tâche 2 :

- `ComponentNameResolver` du module `tika-serialization`
- `SafeContentHandler` du module `tika-core`

Ces deux classes sont de taille raisonnable, contiennent de la logique métier, ont une couverture partielle et
contiennent des lignes de code intéressantes pour les tests par mutation.
**Métriques de couverture pour `SafeContentHandler` :**

- Classes: 1/1 (100%)
- Méthodes: 9/13 (69%)
- Lignes: 29/60 (48%)
- Branches: 14/38 (36%)

**Métriques de couverture pour `ComponentNameResolver` :**

- Classes: 1/1 (100%)
- Méthodes: 3/19 (15%)
- Lignes: 43/101 (42%)
- Branches: 3/48 (6%)

Nous avons choisi de commencer par la classe `SafeContentHandler` et ses méthodes non-privées :

```java
public SafeContentHandler(ContentHandler handler);

public void startElement(String uri, String localName, String name, Attributes atts) throws SAXException;

public void endElement(String uri, String localName, String name) throws SAXException;

public void endDocument() throws SAXException;

public void characters(char[] ch, int start, int length) throws SAXException;

public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException;

protected boolean isInvalid(int ch);

protected void writeReplacement(Output output) throws SAXException;

private void filter(char[] ch, int start, int length, Output output) throws SAXException;

private boolean isInvalid(String value);
 ```                                                                                                                                                                                                                                                                                                         

# 2. Tests générés par l'IA

## 2.1. Problèmes rencontrés avec ChatUniTest

Suite à plusieurs difficultés rencontrées avec la mise en place de ChatUniTest, nous sommes parvenus à générer les tests
à l'aide d'un modèle local s'exécutant via Ollama. Un des problèmes rencontrés était que ChatUniTest code en dur une
liste de modèles (GPT-3.5, GPT-4, etc.) que l'outil supporte.

```
No Model with name qwen3:8b
Support models: gpt-3.5-turbo, gpt-3.5-turbo-1106, gpt-4o, gpt-4o-mini, gpt-4o-mini-2024-07-18, code-llama, codeqwen:v1.5-chat  
```

Les modèles GPT ne sont pas disponibles localement, ce qui nous a laissé le choix d'utiliser soit `code-llama` ou
`codeqwen:v1.5-chat`. Cependant, ces identifiants de modèles ne sont plus à jour. Par exemple, `code-llama` a été
remplacé par `codellama`. Pour résoudre ce problème, nous avons créé un alias de modèle dans Ollama pour que
`code-llama` pointe vers le modèle `codellama`. Après cette modification, ChatUniTest a été en mesure de s'exécuter
correctement, mais un autre problème a fait surface : le modèle `codellama` utilise trop de VRAM pour notre système le
plus puissant parmi nos machines (8Go de VRAM). Malgré qu'une exécution par CPU soit possible, elle est extrêmement
lente.
De plus, la taille de la fenêtre de contexte était trop petite pour générer des tests fonctionnels, car les prompts et
les réponses du modèle dépassaient la limite de tokens. Pour remédier à cette limitation, nous avons modifié l'alias
pour pointer vers le modèle `gemma4:e4b`, qui est un modèle "mixture of experts" (MoE). Cette architecture permet de
réduire la demande en VRAM et par conséquent, d'augmenter la taille de la fenêtre de contexte juste assez (14k tokens)
pour générer des tests fonctionnels. Après ces ajustements, nous avons pu générer les tests.

## 2.2. Résultats de la génération de tests

Les tests générés à l'aide de ChatUniTest se trouvent dans le dossier :
`tika-core/src/test/chatunitest-tests/tika-parent/tika-core/org/apache/tika/sax/`

Après avoir corrigé les [problèmes de configuration](#21-problèmes-rencontrés-avec-chatunitest), nous
avons pu générer 7 classes de tests.

Nous avons configuré ChatUniTest pour générer 2 tests par méthode de `SafeContentHandler` avec un maximum de 3 essais
par test. Le choix de générer 2 tests par méthode a été fait pour observer si la couverture de code pouvait être
améliorée en ayant plusieurs tests pour la même méthode et pour donner plus de chances à l'IA de réussir à générer des
tests fonctionnels. Nous avons également configuré ChatUniTest pour continuer à itérer même après le succès d'un test,
afin de donner à l'IA la possibilité de raffiner les tests générés. Finalement, la configuration de fusion des tests a
été désactivée, car la fusion automatique engendrait des erreurs de compilation.

## 2.3 Explication des tests générés

Les tests générés par l'IA utilisent Mockito pour mocker `ContentHandler`:

### `SafeContentHandler_startElement_4_0_Test`

- `testStartElement_NoInvalidAttributes_CallsSuperHandler`: Vérifie que la méthode `startElement` appelle correctement
  la méthode `startElement` de la classe parente lorsque la liste d'attributs contient uniquement des attributs valides.

- `testStartElement_EmptyAttributes_CallsSuperHandler`: Vérifie que la méthode `startElement` appelle correctement la
  méthode `startElement` de la classe parente lorsque la liste d'attributs est vide.

#### **Critique**

En général, ces deux tests sont corrects et augmentent la couverture de code. Les tests sont simples et sont spécifiques
au contrat de l'appel de la méthode parent. Certains commentaires dans le code ne sont pas absolument nécessaires. Par
exemple, le bloc de commentaire dans `testStartElement_NoInvalidAttributes_CallsSuperHandler` porte sur l'impossibilité
de mock la méthode privée `isInvalid` pour contrôler son comportement durant le test, cependant cette méthode est mocké
dans la classe de test par réflexion via `invokeIsInvalid`. Il est donc possible de contrôler le comportement de
`isInvalid`

Ces deux tests ne couvrent pas les cas où la liste d'attributs contient des attributs invalides. Ils ne vérifient pas si
les attributs sont correctement filtrés avant d'être passés à la méthode parente. De plus, ils ne testent pas le
comportement de la méthode lorsque des exceptions sont levées par la méthode parente.

Finalement, l'IA a généré des
méthodes utilitaires par réflexion (`invokeIsInvalid`, `invokeFilter`) ainsi qu'une classe interne `StringOutput` qui ne
sont pas utilisées dans les tests. Cependant, ces méthodes et classes utilitaires auraient pu être utilisées pour tester
le filtrage des attributs invalides. Il est possible que la contrainte du nombre de tests par méthode ait coupé la
génération de tests pour ces cas.

---

### `SafeContentHandler_endElement_5_0_Test`

- `endElement_shouldDelegateSuccessfullyToSuperHandler`: Vérifie que la méthode `endElement` appelle correctement
  la méthode `endElement` de la classe parente avec les arguments attendus.

- `endElement_shouldHandleSAXExceptionFromUnderlyingHandler`: Configure le mock pour lever une `SAXException` et vérifie
  que l'exception est interceptée ou gérée lors de l'appel à `endElement`.

#### **Critique**

Le premier test valide l'appel de `endElement` du parent en vérifiant que le mock a été appelé avec les bons paramètres.
Il est simple et correct.

Le second test présente cependant un défaut logique : la méthode `endElement` est déclarée avec
`throws SAXException` et propage intentionnellement l'exception reçue de l'appel du parent. Le test utilise un bloc
`try-catch` pour capturer `SAXException` afin de satisfaire `verify(...)`, masquant le fait qu'il ne vérifie pas
correctement que l'exception remonte la chaîne d'appel. L'oracle de test est donc incomplet et ne valide pas
correctement le contrat de propagation d'exception. L'utilisation de `assertThrows(SAXException.class, ...)` aurait été
plus appropriée pour vérifier que l'exception est bien propagée.

De plus, dans la méthode `setUp()`, l'IA injecte un mock par réflexion dans le champ privé `handler` de
`ContentHandlerDecorator`, alors que l'appel au constructeur `super(handler)` réalise déjà cette
initialisation.

---

### `SafeContentHandler_endElement_5_1_Test`

- `endElement_shouldCallSuperEndElementWithCorrectParameters_onSuccess`: Vérifie que l'appel à `endElement` se produit
  une seule fois à la classe parente avec les arguments attendus.

- `endElement_shouldPropagateSAXException_whenSuperThrowsIt`: Configure le mock de la classe parente pour lever
  `SAXException` et vérifie avec `assertThrows(SAXException.class, ...)` et `assertEquals(...)` que l'exception est
  correctement propagée avec le même message.

#### **Critique**

Les tests de cette classe de tests est de meilleure qualité que `SafeContentHandler_endElement_5_0_Test`. Elle évite
la réflexion inutile dans `setUp()` et son oracle d'exception est complet : elle valide le type d'exception lancé, son
message et que l'appel au parent a été essayé. On y observe une vérification adéquate du contrat d'appel au parent et de
la propagation des erreurs.

---

### `SafeContentHandler_endDocument_6_0_Test`

- `endDocument_shouldCallSuperEndDocumentOnDelegate`: Vérifie que la méthode `endDocument` appelle correctement la
  méthode `endDocument` de la classe parente.

- `endDocument_shouldPropagateSAXExceptionFromDelegate`: Configure le mock pour lever `SAXException` et vérifie avec
  `assertThrows(SAXException.class, ...)` que l'exception est propagée lors de l'appel à `endDocument`.

#### **Critique**

Ces deux tests sont simples et corrects. Le premier test valide l'appel de `endDocument` du parent et le second test
valide correctement le contrat de propagation d'exception, contrairement à `SafeContentHandler_endElement_5_0_Test`.
Les tests n'utilisent pas de réflexion inutile pour initialiser le mock. De plus, ils couvrent une méthode qui n'était
pas testée par les tests existants de Tika.

---

### `SafeContentHandler_endDocument_6_1_Test`

- `endDocument_shouldCallSuperEndDocumentWhenSuccessful`: Vérifie que la méthode `endDocument` appelle correctement la
  méthode `endDocument` de la classe parente.

- `endDocument_shouldHandleExceptionWhenUnderlyingHandlerThrowsSAXException`: Configure le mock pour lever une
  `SAXException` et vérifie avec `assertDoesNotThrow(...)` que l'exception n'est pas propagée lors de l'appel à
  `endDocument`.

#### **Critique**

Le premier test est identique au premier test de `SafeContentHandler_endDocument_6_0_Test`. Il est simple et correct.

Le deuxième test est incorrect, car celui-ci vérifie que l'exception n'est pas propagée alors que la méthode
`endDocument` est censée propager l'exception reçue. Dans les commentaires du code généré, on peut voir que l'IA
explique que `ContentHandlerDecorator` gère l'exception sans la relancer. Ce raisonnement est partiellement correct,
parce que l'implémentation de la méthode `endDocument` de `SafeContentHandler` utilise en effet un bloc de gestion
d'exception qui ne relance pas l'exception :

```java
public class ContentHandlerDecorator extends DefaultHandler {

//[...]

    @Override
    public void endDocument() throws SAXException {
        try {
            handler.endDocument();
        } catch (SAXException e) {
            handleException(e);
        }
    }

//[...]

}
```

Cependant, la méthode `handleException` de `ContentHandlerDecorator` vérifie si `handler` est une instance de
`ContentHandlerDecorator` et relance l'exception si ce n'est pas le cas.

```java
protected void handleException(SAXException exception) throws SAXException {
    if (handler instanceof ContentHandlerDecorator) {
        ((ContentHandlerDecorator) handler).handleException(exception);
    } else {
        throw exception;
    }
}
```

Puisque dans ce test, `handler` est un mock de `ContentHandler`, la vérification de l'instance échoue et l'exception est
relancée. Étant donné que ce comportement relève de `ContentHandlerDecorator`, nous avons enlevé le test
`endDocument_shouldHandleExceptionWhenUnderlyingHandlerThrowsSAXException`, car l'oracle de test ne devrait pas
vérifier le comportement d'une classe autre que `SafeContentHandler`.

---

### `SafeContentHandler_characters_7_1_Test`

- `characters_shouldProcessFullCharacterSegment_whenInputIsValid`: Vérifie que la méthode `characters` appelle
  correctement la méthode `characters` de la classe parente avec les mêmes paramètres lorsque que tous les caractères
  sont valides.

- `characters_shouldProcessPartialCharacterSegment_whenStartIsNotZero`: Vérifie que la méthode `characters` effectue
  correctement l'appel à la méthode parente tout en conservant les mêmes paramètres `inputChars`, `start` et `length`
  lorsque `start` est différent de zéro.

- `characters_shouldHandleZeroLengthInput_withoutThrowingException`: Vérifie que `characters` ne lance pas d'exception
  lorsque la longueur donnée en paramètre est zéro pour une liste de caractères qui n'est pas vide.

- `characters_shouldHandleFullArrayInput_whenStartIsZero`: Vérifie le même comportement que le premier test.

#### **Critique**

En général, ces tests sont corrects et augmentent la couverture. Les tests vérifient que
les arguments passés à la méthode parente correspondent à ceux passés à la méthode `characters`.
Cependant, `characters_shouldProcessFullCharacterSegment_whenInputIsValid` et
`characters_shouldHandleFullArrayInput_whenStartIsZero` sont redondants, car ils testent le même élément de l'oracle.

Ces tests ne couvrent pas entièrement la logique de métier principale de `SafeContentHandler`, puisque tous les
caractères utilisés sont des caractères ASCII valides. Aucun caractère Unicode invalide n'est utilisé. La logique de
filtrage n'est donc jamais exécutée et l'oracle vérifie seulement que les caractères sont passés tels quels à la méthode
parente, sans tester le remplacement des caractères invalides.

---

### `SafeContentHandler_ignorableWhitespace_8_0_Test`

- `testIgnorableWhitespace_fullLength`: Vérifie que l'appel à `ignorableWhitespace` avec une liste de caractères ne
  lance pas d'exception.

- `testIgnorableWhitespace_partialLength`: Vérifie le comportement avec une liste partielle.

- `testIgnorableWhitespace_zeroLength`: Vérifie le comportement lorsque `length` est égal à zéro.

- `testIgnorableWhitespace_startAtEnd`: Vérifie le comportement lorsque `start` est égal à la taille de la liste de
  caractères.

- `testIgnorableWhitespace_throwsSAXException`: Vérifie le même comportement que `testIgnorableWhitespace_fullLength`.

#### **Critique**

Les oracles de ces tests sont très faibles. Tous les tests utilisent `assertDoesNotThrow` et aucun test ne vérifie avec
`verify(...)` que la méthode parente a été appelée. Les tests vérifient donc seulement que la méthode ne lance pas
d'exception, sans valider son comportement. De plus, le nom du test `testIgnorableWhitespace_throwsSAXException` ne
correspond pas à ce qu'il vérifie, puisqu'il s'attend à ce qu'aucune exception ne soit levée.

Le test `testIgnorableWhitespace_startAtEnd` est erroné : l'indice de début `start` est plus grand que la longueur
de la liste de caractères, cela résulte en un lancement de `IndexOutOfBoundsException` alors que le test vérifie
qu'aucune exception n'est levée malgré que la levée d'exception soit attendue. Une correction manuelle a été apportée
pour que le test vérifie correctement que l'exception est levée.

Finalement, aucun caractère invalide n'est utilisé. Donc, la logique de filtrage de `SafeContentHandler` n'est jamais
testée.

## 2.4 Comparaison des tests générés par l'IA avec les tests existants

En général, les tests générés par l'IA sont pour la plupart corrects et @augmentent la couverture de code. Cependant,
certains tests sont redondants et d'autres ont des oracles incomplets ou incorrects.

Comparativement aux tests existants, ceux générés par l'IA sont plus superficiel et démontrent une compréhension très
en surface. Un bon example est le test `endDocument_shouldHandleExceptionWhenUnderlyingHandlerThrowsSAXException` où
la logique de propagation d'exception est mal comprise. Cela peut être dû à la limitation de la fenêtre de contexte, qui
aurait empêché l'IA d'avoir une vue complète de la classe.

Une autre remarque est que les tests générés utilisent tous des conventions de nommage différentes de celles des
tests existants. Les tests existants utilisent un style simplifié, tandis que les tests générés par l'IA utilisent un
style plus verbeux et descriptif.

Différentes méthodes de tests sont également utilisées :
"Arrange, Act, Assert (AAA)", "Given, When, Then (GWT)" et "Setup, Execute, Verify (SEV)".
Les tests existants n'utilisent pas vraiment de conventions de nommage et sont plus simples. Ils ne font pas usage de
mocks et ne testent pas les exceptions. Cependant, les tests existants se concentrent sur la logique de filtrage des
caractères invalides, ce qui est la responsabilité principale de la classe.

Finalement, les classes de tests générées par l'IA ont des importations, des méthodes utilitaires et des classes
internes inutilisées, ce qui alourdit le code et le rend moins lisible. De plus, le nom de certains tests ne correspond
pas à ce qu'ils testent réellement.

## 3.1. Score de mutation initial (Baseline)

Nous avons d'abord exécuté Pitest uniquement avec les tests originaux fournis par Apache Tika.



- **Couverture de lignes :** 75 % (45/60)



- **Score de mutation :** 58 % (32 mutants tués sur 55)



- **Force des tests (Test strength) :** 82 %



- **Mutants sans couverture (NO_COVERAGE) :** 16




Les statistiques démontrent que malgré une couverture acceptable, plusieurs segments de la logique étaient toujours vulnérables, avec 16 mutants complètement ignorés par l'exécution.



## 3.2. Impact des tests générés par l'IA

Après l'intégration des tests générés par ChatUniTest, nous avons relancé l'analyse :



- **Couverture de lignes :** 74 % (43/58)



- **Score de mutation :** 61 % (35 mutants tués sur 57)



- **Force des tests :** 90 %



- **Mutants sans couverture :** 18

**Mutants détectés par les tests générés (IA)** Les 3 mutants supplémentaires tués par ChatUniTest concernent principalement les appels de délégation à la classe parente.

1. **Mutant : `removed call to org/apache/tika/sax/ContentHandlerDecorator::endDocument`**

- **Pourquoi il a été détecté :** La suite de tests originale de Tika ne testait pas la méthode `endDocument`. Les tests générés par l'IA (ex: `SafeContentHandler_endDocument_6_0_Test`) ont introduit un mock de `ContentHandler` et utilisé `Mockito.verify()` pour s'assurer que la méthode `endDocument()` du parent est bel et bien appelée. Le mutant supprimant cet appel fait donc échouer le test.

2. **Mutant : `removed call to org/apache/tika/sax/ContentHandlerDecorator::endElement`**

- **Pourquoi il a été détecté :** L'IA a généré `SafeContentHandler_endElement_5_1_Test` qui vérifie explicitement avec `verify(...)` que les bons paramètres sont passés au parent lors de la fermeture d'un élément.

3. **Mutant : `removed call to org/apache/tika/sax/ContentHandlerDecorator::startElement`**

- **Pourquoi il a été détecté :** De la même manière, le test `SafeContentHandler_startElement_4_0_Test` vérifie la délégation de l'ouverture d'un élément avec des attributs valides, ce qui tue le mutant qui tente de supprimer cet appel.


L'ajout des tests de l'IA a permis de tuer 3 mutants supplémentaires, augmentant légèrement le score de mutation à 61 %. Cependant, l'IA n'a pas réussi à réduire le nombre d'erreurs `NO_COVERAGE`. Comme mentionné précédemment, l'IA s'est contentée de valider le passage de caractères valides, échouant totalement à générer des caractères Unicode invalides pour déclencher la véritable logique de filtrage de la classe.


## 3.3. Ajout de tests manuels ciblés et renforcement des oracles

Nous avons créé la classe `SafeContentHandlerManualTest` afin d'intégrer des tests qui élimineraient les mutants survivants. On a pu utiliser les outils donné par Pitest afin de cibler les sections de `SafeContentHandler.java` auxquelles les mutants survivaients. On a donc pu s'attaquer aux angles morts laissés par les anciens tests.


**Nom du test :** `testFiltreMutantsLignes86Et87`

- **Intention du test :** Vérifier l'exactitude des limites de la boucle `while` (lignes 86 et 87) lors du filtrage d'un caractère invalide pour s'assurer que le programme n'écrit pas de caractères excédentaires ou ne déclenche pas d'erreurs d'index.

- **Motivation des données :** L'utilisation du caractère invalide immédiat `\u000B` et d'une chaîne combinée `xab\u000B` force l'exécution directe des conditions `i > start` et du calcul `i - start`.

- **Explication de l'oracle :** Un `DefaultHandler` espion intercepte les appels à `characters`. L'oracle vérifie avec `assertEquals` que la taille de la liste des longueurs écrites correspond exactement au comportement attendu, détectant ainsi les mutants modifiant les conditions (`i >= start`) ou les opérations mathématiques (`i + start`).

- **Nom du test :** `testLimitesUnicodeIsInvalid`

- **Intention du test :** Couvrir exhaustivement toutes les frontières conditionnelles (`else if`) de la méthode `isInvalid(int)` qui définit les plages de caractères XML valides.

- **Motivation des données :** Les valeurs testées (`0xD7FF`, `0xD800`, `0xFFFD`, `0xFFFF`, `0x10000`, `0x10FFFF`, `0x110000`) correspondent aux limites exactes dictées par la spécification XML et codées dans la méthode.

- **Explication de l'oracle :** En exposant la méthode protégée via une sous-classe (`TestableSafeContentHandler`), l'oracle utilise des `assertTrue` et `assertFalse` stricts sur les retours booléens. Cela permet d'identifier instantanément tout mutant de type "changed conditional boundary" (ex: `<` changé en `<=`).

- **Nom du test :** `testStartElementAvecAttributInvalide`

- **Intention du test :** Exécuter et valider la logique de remplacement des caractères invalides cachés à l'intérieur des valeurs d'attributs lors de l'ouverture d'un élément (`startElement`).

- **Motivation des données :** L'injection de la chaîne `"valeur\u000Binvalide"` dans un objet `AttributesImpl` force le déclenchement de la condition interne `if (isInvalid(atts.getValue(i)))` qui était jusque-là marquée en `NO_COVERAGE`.

- **Explication de l'oracle :** Un espion capture les attributs filtrés avant leur envoi au parent. L'oracle utilise `assertEquals("valeur\ufffdinvalide", ...)` pour garantir de manière absolue que le caractère invalide a été remplacé par le caractère de substitution Unicode `\ufffd`.




## 3.4. Score de mutation final et conclusion

Avec l'intégration et le raffinement de nos tests manuels, notre dernière exécution Pitest a donné les résultats suivants :



- **Couverture de lignes :** 100 % (58/58)



- **Score de mutation :** 100 % (57 mutants tués sur 57)

- **Force des tests (Test strength) :** 100 %

- **Mutants sans couverture :** 0


**Bilan :**

La combinaison des tests de l'IA et de nos tests manuels a permis de faire passer la couverture de lignes à 100 % et de tuer la totalité des 57 mutants générés. Le score de mutation a fait un bond exceptionnel, passant d'un score initial de 58 % (avec les tests de base de Tika) à un score parfait de 100 %.
