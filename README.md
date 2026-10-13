> **Note :** Rapport Tâche 2 par Jaden Che Angelo Keenan-Mbemba et Émerick Poulin

# 1. Sélection des classes à tester

Nous avons utilisé les métriques de couverture générées par JaCoCo pour identifier les classes candidates selon les
critères spécifiés de la tâche 2. Dans les rapports de couverture, plusieurs classes étaient couvertes par les tests
de manière partielle. Nous avons donc réduit l'ensemble de candidats en ne considérant que les classes ayant les
propriétés qualitatives suivantes :

- La classe contient de la logique métier avec une couverture partielle
- La classe n'est pas trop grande et complexe
- Les lignes de code non couvertes ne sont pas des méthodes triviales ou utilitaires (exemple : get, set, etc...)
- Les tests existants n'indiquent pas que la création de tests supplémentaires soit potentiellement compliquée

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
- Méthodes: 10/13 (77%)
- Lignes: 29/60 (48%)
- Branches: 14/38 (36%)

**Métriques de couverture pour `ComponentNameResolver` :**

- Classes: 1/1 (100%)
- Méthodes: 3/19 (15%)
- Lignes: 43/101 (42%)
- Branches: 3/48 (6%)

**Présence de mutants vivants :** la couverture incomplète indique que `SafeContentHandler` a potentiellement des
mutants vivants. Un mutant se retrouvant sur une ligne de code qui n'est pas couverte par un test ne peut donc pas être
tué. À l'aide d'une analyse initiale "à la main" nous avons identifié quelques mutants potentiellement vivants dans
`SafeContentHandler`. Par exemple, les limites des plages de caractères dans `isInvalid(int)` (`<`, `>`) et la
branche de filtrage des attributs invalides dans `startElement` ne sont exécutées par aucun test, donc tout mutant à
ces endroits survit. L'analyse Pitest effectuée par la suite avec les tests originaux confirme que ces mutants sont
vivants.

Au final, nous nous sommes concentrés sur la classe `SafeContentHandler` pour la tâche 2. La classe
`ComponentNameResolver` a été mise de côté, car la mise en place de la génération de tests via ChatUniTest a été plus
compliquée et longue que prévu.

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
`codeqwen:v1.5-chat`. Comme ces identifiants n'étaient plus à jour, nous avons d'abord créé un alias Ollama pour faire
pointer `code-llama` vers `codellama`. ChatUniTest a alors pu s'exécuter, mais `codellama` consommait trop de VRAM
pour notre machine la plus puissante (8 Go) et restait très lent en CPU. Nous avons donc remplacé l'alias par
`gemma4:e4b`, un modèle MoE plus léger, afin de réduire l'usage de VRAM et d'obtenir une fenêtre de contexte assez
grande (14k tokens) pour générer des tests fonctionnels.

## 2.2. Intégration de ChatUniTest dans le pipeline Maven

ChatUniTest est intégré au pipeline Maven du module `tika-core` à l'aide de l'ajout du plugin `chatunitest-maven-plugin`
dans `tika-core/pom.xml`. Les dépendances de test utilisées par les prompts (Mockito, JUnit 5, etc.) ont aussi été
ajoutées au `pom.xml`, en se basant sur le
[pom d'exemple de ChatUniTest](https://github.com/ZJU-ACES-ISE/ChatUniTest/blob/06d164aed21ec4cea29b8ac8661de395bdf50c41/docs/pom-example.xml).

Le plugin `build-helper-maven-plugin` ajoute le répertoire des tests générés comme source de test, afin que Maven puisse
détecter et exécuter les tests générés par ChatUniTest.

Commande utilisée pour générer les tests :

```bash
.\mvnw.cmd compile chatunitest:class -pl tika-core -DselectClass=SafeContentHandler
```

## 2.3. Résultats de la génération de tests

Les tests générés se trouvent dans `tika-core/src/test/chatunitest-tests/tika-parent/tika-core/org/apache/tika/sax/`.
Nous avons généré 7 classes de tests, avec 2 tests par méthode de `SafeContentHandler` (pour observer si plusieurs tests
par méthode améliorent la couverture et donner plus de chances d'obtenir un test fonctionnel), un maximum de 3 essais
par test, `stopWhenSuccess=false` pour laisser l'IA raffiner, et `merge=false` parce que la fusion automatique
produisait du code qui ne compilait pas.

Les 7 classes compilent sans intervention manuelle. Deux méthodes de tests ont nécessité une correction manuelle :

- `SafeContentHandler_endDocument_6_1_Test` : `endDocument_shouldHandleExceptionWhenUnderlyingHandlerThrowsSAXException`
  avait un oracle incorrect. Nous l'avons donc retiré (voir 2.4 - 6_1).
- `SafeContentHandler_ignorableWhitespace_8_0_Test` : `testIgnorableWhitespace_startAtEnd` s'attendait à ce qu'aucune
  exception ne soit levée. Nous l'avons corrigé pour qu'il vérifie le lancement de l'exception.

Certaines classes de test ont été rejetées par ChatUniTest, car il y avait des erreurs de compilation.

## 2.4. Explication des tests générés

Les tests générés par l'IA utilisent Mockito pour mocker `ContentHandler`.

### `SafeContentHandler_startElement_4_0_Test`

Deux tests vérifient que `startElement` appelle la méthode `startElement` du parent, avec des attributs valides
(`testStartElement_NoInvalidAttributes_CallsSuperHandler`) ou une liste vide
(`testStartElement_EmptyAttributes_CallsSuperHandler`). Ils sont corrects et spécifiques au contrat d'appel du parent,
mais aucun attribut invalide n'est utilisé : le filtrage des attributs n'est jamais vérifié et la propagation d'une
exception levée par le parent non plus. L'IA a aussi généré des méthodes utilitaires par réflexion (`invokeIsInvalid`,
`invokeFilter`) et une classe interne `StringOutput` qui ne sont jamais utilisées. Cepedant, elles auraient permis de
tester le filtrage. Un commentaire mentionne qu'il est impossible de contrôler `isInvalid`, alors que `invokeIsInvalid`
le permet. Il est possible que la taille de la fenêtre de contexte ait empêché l'IA d'avoir une vue complète de la
classe.

### `SafeContentHandler_endElement_5_0_Test`

`endElement_shouldDelegateSuccessfullyToSuperHandler` vérifie avec `verify(...)` que le parent est appelé avec les
bons paramètres, ce qui est simple et correct. `endElement_shouldHandleSAXExceptionFromUnderlyingHandler` a un oracle
incomplet : `endElement` propage l'exception du parent, mais le test l'attrape dans un `try-catch` pour satisfaire
`verify(...)`, sans vérifier qu'elle est propagé. Un `assertThrows(SAXException.class, ...)` aurait été plus approprié.
De plus, `setUp()` injecte le mock par réflexion dans le champ privé `handler`, ce que `super(handler)` fait déjà.

### `SafeContentHandler_endElement_5_1_Test`

Mêmes tests que `5_0`, mais de meilleure qualité : pas de réflexion inutile, et le test d'exception utilise
`assertThrows` et `assertEquals` pour valider le type, le message et que l'appel au parent a bien été tenté. Le contrat
d'appel et la propagation des erreurs sont correctement vérifiés.

### `SafeContentHandler_endDocument_6_0_Test`

Deux tests simples et corrects : l'un vérifie l'appel à `endDocument` du parent, l'autre la propagation de la
`SAXException` avec `assertThrows`. Ils couvrent une méthode que `SafeContentHandlerTest` ne testait pas.

### `SafeContentHandler_endDocument_6_1_Test`

Le premier test est identique au premier de `6_0`. Le second
(`endDocument_shouldHandleExceptionWhenUnderlyingHandlerThrowsSAXException`) est incorrect : il vérifie avec
`assertDoesNotThrow` que l'exception n'est pas propagée. L'IA s'appuie sur le fait que
`ContentHandlerDecorator.endDocument` capture `SAXException` et appelle `handleException`, mais cette méthode relance
l'exception lorsque `handler` n'est pas un `ContentHandlerDecorator`, ce qui est le cas avec un mock de
`ContentHandler`. Étant donné que ce comportement relève de `ContentHandlerDecorator` et non de `SafeContentHandler`,
nous avons retiré ce test.

### `SafeContentHandler_characters_7_1_Test`

Quatre tests vérifient que `characters` passe les mêmes `inputChars`, `start` et `length` au parent (segment complet,
`start` non nul, longueur nulle, tableau entier). Corrects et utiles, mais
`characters_shouldProcessFullCharacterSegment_whenInputIsValid` et
`characters_shouldHandleFullArrayInput_whenStartIsZero`
testent la même chose. De plus, tous les caractères utilisés sont des caractères ASCII valides : la logique de filtrage
n'est jamais exécutée et l'oracle ne vérifie que le flux de données.

### `SafeContentHandler_ignorableWhitespace_8_0_Test`

Les cinq tests (`fullLength`, `partialLength`, `zeroLength`, `startAtEnd`, `throwsSAXException`) n'utilisent que
`assertDoesNotThrow` sans aucun `verify(...)` : ils vérifient seulement qu'aucune exception n'est lancée. Le nom
`testIgnorableWhitespace_throwsSAXException` ne correspond pas à ce qu'il fait. `testIgnorableWhitespace_startAtEnd`
était erroné : la valeur de `start` cause l'exception `IndexOutOfBoundsException`. Nous l'avons corrigé pour vérifier
l'exception est lancée. Le filtrage n'est pas testé dans ces tests, car aucun caractère invalide n'est utilisé.

## 2.5. Comparaison des tests générés par l'IA avec les tests existants

En général, les tests générés par l'IA sont pour la plupart corrects et augmentent la couverture de code par rapport à
`SafeContentHandlerTest`. Cependant, certains tests sont redondants et d'autres ont des oracles incomplets ou
incorrects.

Comparativement aux tests existants, ceux générés par l'IA sont plus superficiels et démontrent une compréhension très
en surface. Un bon exemple est le test `endDocument_shouldHandleExceptionWhenUnderlyingHandlerThrowsSAXException` où
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

# 3. Analyse de mutation avec Pitest

## 3.0. Configuration de Pitest

Pitest est intégré au pipeline Maven du module `tika-core` à l'aide de l'ajout du plugin `pitest-maven`
dans `tika-core/pom.xml` ainsi que les dépendances nécessaires pour l'exécution des tests JUnit 5.

Commande d'exécution :

```bash
.\mvnw.cmd test-compile org.pitest:pitest-maven:mutationCoverage -pl :tika-core "-Djacoco.skip=true" "-Dcheckstyle.skip=true"
```

Le rapport HTML est produit dans `tika-core/target/pit-reports/`.
Le paramètre `targetTests` limite l'analyse aux tests de `SafeContentHandler` : `SafeContentHandlerTest` (tests
originaux), les classes `SafeContentHandler_*_Test` (tests générés) et `SafeContentHandlerManualTest` (tests manuels).
Les autres tests du module `org.apache.tika.sax` (`BodyContentHandlerTest`, `XHTMLContentHandlerTest`, etc.) passent
aussi indirectement par `SafeContentHandler`. Les inclure masquerait la réelle contribution des tests générés et
manuels, puisque ces tests tueraient déjà une partie des mutants.

## 3.1. Score de mutation initial (Baseline)

Nous avons d'abord exécuté Pitest uniquement avec les tests originaux de la classe (`SafeContentHandlerTest`).

- **Couverture de lignes :** 48 % (29/60)
- **Score de mutation :** 40 % (22 mutants tués sur 55)
- **Force des tests (Test strength) :** 79 %
- **Mutants sans couverture (NO_COVERAGE) :** 27

La couverture de lignes est la même que celle mesurée par JaCoCo à la section 1. Plus de la moitié des mutants
(27 sur 55) se trouvent dans du code qu'aucun test n'exécute, notamment `startElement`, `endElement`, `endDocument`
et `isInvalid(String)`. Parmi les mutants couverts, 6 survivent.

## 3.2. Impact des tests générés par l'IA

Après l'intégration des tests générés par ChatUniTest, nous avons relancé l'analyse :

- **Couverture de lignes :** 75 % (45/60)
- **Score de mutation :** 58 % (32 mutants tués sur 55)
- **Force des tests :** 82 %
- **Mutants sans couverture :** 16

**Mutants détectés par les tests générés (IA)**

Les tests générés tuent 10 mutants supplémentaires :

| Ligne | Méthode             | Mutateur                                                  | Tué par                                                                                                        |
|-------|---------------------|-----------------------------------------------------------|----------------------------------------------------------------------------------------------------------------|
| 188   | `startElement`      | *removed call to `ContentHandlerDecorator::startElement`* | `SafeContentHandler_startElement_4_0_Test` (les deux tests)                                                    |
| 196   | `endElement`        | *removed call to `ContentHandlerDecorator::endElement`*   | `SafeContentHandler_endElement_5_0_Test`, `SafeContentHandler_endElement_5_1_Test`                             |
| 240   | `endDocument`       | *removed call to `ContentHandlerDecorator::endDocument`*  | `SafeContentHandler_endDocument_6_0_Test`, `SafeContentHandler_endDocument_6_1_Test`                           |
| 169   | `startElement`      | *changed conditional boundary*                            | `testStartElement_NoInvalidAttributes_CallsSuperHandler`, `testStartElement_EmptyAttributes_CallsSuperHandler` |
| 169   | `startElement`      | *negated conditional*                                     | `testStartElement_EmptyAttributes_CallsSuperHandler`                                                           |
| 170   | `startElement`      | *negated conditional*                                     | `testStartElement_NoInvalidAttributes_CallsSuperHandler`                                                       |
| 115   | `isInvalid(String)` | *changed conditional boundary*                            | `testStartElement_NoInvalidAttributes_CallsSuperHandler`                                                       |
| 117   | `isInvalid(String)` | *negated conditional*                                     | `testStartElement_NoInvalidAttributes_CallsSuperHandler`                                                       |
| 120   | `isInvalid(String)` | *Replaced integer addition with subtraction*              | `testStartElement_NoInvalidAttributes_CallsSuperHandler`                                                       |
| 123   | `isInvalid(String)` | *replaced boolean return with true*                       | `testStartElement_NoInvalidAttributes_CallsSuperHandler`                                                       |

Pourquoi ils sont détectés :

- **Appels de délégation (lignes 188, 196, 240) :** `SafeContentHandlerTest` n'appelle jamais `startElement`,
  `endElement` ni `endDocument`. Les tests générés utilisent un mock de `ContentHandler` et vérifient avec
  `Mockito.verify(...)` que la méthode du parent est appelée avec les bons arguments. Le mutant qui supprime cet appel
  fait donc échouer le test.
- **Boucle sur les attributs (ligne 169) :** les deux mutants font lire à la boucle un attribut qui n'existe pas
  (`i <= atts.getLength()`, ou `i >= atts.getLength()` avec une liste vide). `atts.getValue(i)` retourne alors `null`
  et `isInvalid(String)` lance `NullPointerException`, ce qui fait échouer le test. Le mutant de négation n'est tué
  que par le test avec une liste vide : avec au moins un attribut, la condition inversée est fausse dès le départ, la
  boucle est sautée et le comportement observable reste le même.
- **Filtrage déclenché à tort (lignes 115, 117, 120, 123, 170) :** ces mutants font croire à `startElement` qu'un
  attribut valide est invalide, ou font échouer le parcours de la chaîne dans `isInvalid(String)`. Dans la branche de
  filtrage, `startElement` crée un nouvel objet `AttributesImpl` (une copie avec les mêmes valeurs) et le passe au
  parent à la place de l'objet reçu. Le test généré utilise `verify(handler).startElement(..., atts)` avec l'objet
  `atts` d'origine. Comme `AttributesImpl` ne redéfinit pas `equals`, Mockito compare les références : la copie ne
  correspond pas et le test échoue. Ces mutants sont donc détectés grâce à la vérification de l'identité de l'objet
  transmis, et non grâce à une vérification du filtrage en tant que tel.

**Mutants non détectés par les tests générés (IA)**

Il reste 23 mutants non tués après l'ajout des tests de l'IA (55 − 32) : 7 qui survivent et 16 sans couverture.

| Ligne         | Méthode             | Mutateur                                         | Statut        |
|---------------|---------------------|--------------------------------------------------|---------------|
| 86            | `filter`            | *changed conditional boundary*                   | `SURVIVED`    |
| 87            | `filter`            | *Replaced integer subtraction with addition*     | `SURVIVED`    |
| 115           | `isInvalid(String)` | *negated conditional*                            | `SURVIVED`    |
| 118           | `isInvalid(String)` | *replaced boolean return with false*             | `NO_COVERAGE` |
| 140           | `isInvalid(int)`    | *changed conditional boundary*                   | `SURVIVED`    |
| 141           | `isInvalid(int)`    | *replaced boolean return with true*              | `SURVIVED`    |
| 142, 143      | `isInvalid(int)`    | *changed conditional boundary*                   | `SURVIVED`    |
| 144, 145, 147 | `isInvalid(int)`    | *changed conditional boundary*                   | `NO_COVERAGE` |
| 144, 145, 147 | `isInvalid(int)`    | *negated conditional*                            | `NO_COVERAGE` |
| 145, 147      | `isInvalid(int)`    | *replaced boolean return with true*              | `NO_COVERAGE` |
| 173, 175      | `startElement`      | *changed conditional boundary*                   | `NO_COVERAGE` |
| 173, 175, 175 | `startElement`      | *negated conditional*                            | `NO_COVERAGE` |
| 178           | `startElement`      | *removed call to `SafeContentHandler::filter`*   | `NO_COVERAGE` |
| 181           | `startElement`      | *removed call to `AttributesImpl::addAttribute`* | `NO_COVERAGE` |

Pourquoi les tests générés ne les détectent pas :

- **`filter` (lignes 86 et 87) :** ces lignes sont exécutées par `SafeContentHandlerTest`, mais aucun test ne vérifie
  la longueur exacte de chaque écriture. Les mutants `i >= start` et `i + start` produisent des écritures différentes
  que personne n'observe.
- **`isInvalid(int)` (lignes 140 à 147) :** les tests générés n'utilisent que des caractères ASCII valides. Aucun test
  ne vérifie que l'espace, la tabulation, le saut de ligne et le retour chariot sont valides (lignes 140 et 141), et
  les valeurs limites des plages XML (`0xD800`, `0xFFFE`, `0x110000`, etc.) ne sont jamais testées.
- **`isInvalid(String)` (lignes 115 et 118) et branche de filtrage de `startElement` (lignes 173 à 181) :** aucun
  test (original ou généré) n'utilise d'attribut contenant un caractère invalide. Le `return true` de la ligne 118 et
  toute la branche de filtrage ne sont donc jamais exécutés.

L'ajout des tests de l'IA a permis de tuer 10 mutants supplémentaires, faisant passer le score de mutation de 40 % à
58 %. Comme mentionné précédemment, l'IA s'est contentée de valider le passage de caractères valides et n'a généré
aucun caractère invalide pour déclencher la véritable logique de filtrage de la classe.

## 3.3. Ajout de tests manuels ciblés et renforcement des oracles

Nous avons créé la classe `SafeContentHandlerManualTest` afin d'intégrer des tests qui élimineraient les mutants
survivants. Nous avons pu utiliser les outils donnés par Pitest afin de cibler les sections de `SafeContentHandler.java`
dans lesquelles les mutants survivaient. Nous avons donc pu nous attaquer aux angles morts laissés par les anciens
tests.

### `testFiltreMutantsLignes86Et87`

- **Intention du test :** Vérifier l'exactitude des limites de la boucle `while` (lignes 86 et 87) lors du filtrage d'un
  caractère invalide pour s'assurer que le programme n'écrit pas de caractères excédentaires ou ne déclenche pas
  d'erreurs d'index.

- **Motivation des données :** L'utilisation du caractère invalide immédiat `\u000B` et d'une chaîne combinée
  `xab\u000B` force l'exécution directe des conditions `i > start` et du calcul `i - start`.

- **Explication de l'oracle :** Un `DefaultHandler` espion intercepte les appels à `characters`. L'oracle vérifie avec
  `assertEquals` que la taille de la liste des longueurs écrites correspond exactement au comportement attendu,
  détectant ainsi les mutants modifiant les conditions (`i >= start`) ou les opérations mathématiques (`i + start`).

- **Mutants tués (2) :** les deux mutants `SURVIVED` de la méthode `filter` après les tests originaux et les tests IA :
    - ligne 86 : *changed conditional boundary* (`i > start` → `i >= start`)
    - ligne 87 : *Replaced integer subtraction with addition* (`i - start` → `i + start`)

### `testLimitesUnicodeIsInvalid`

- **Intention du test :** Couvrir exhaustivement toutes les frontières conditionnelles (`else if`) de la méthode
  `isInvalid(int)` qui définit les plages de caractères XML valides.

- **Motivation des données :** Les valeurs
  testées (`0xD7FF`, `0xD800`, `0xDFFF`, `0xE000`, `0xFFFD`, `0xFFFE`, `0xFFFF`, `0x10000`, `0x10FFFF`, `0x110000`)
  correspondent aux limites exactes dictées par la spécification XML et codées dans la
  méthode.

- **Explication de l'oracle :** En exposant la méthode protégée via une sous-classe (`TestableSafeContentHandler`),
  l'oracle utilise des `assertTrue` et `assertFalse` stricts sur les retours booléens. Cela permet d'identifier
  instantanément tout mutant de type "changed conditional boundary" (ex: `<` changé en `<=`).

**Mutants tués (10) :** les mutants des lignes 142 à 147 de `isInvalid(int)` qui restaient après les tests
originaux et les tests IA (2 `SURVIVED`, 8 `NO_COVERAGE`) :

- lignes 142, 143 : *changed conditional boundary* (`SURVIVED`)
- lignes 144, 145, 147 : *changed conditional boundary* (`NO_COVERAGE`)
- lignes 144, 145, 147 : *negated conditional* (`NO_COVERAGE`)
- lignes 145, 147 : *replaced boolean return with true* (`NO_COVERAGE`)

### `testStartElementAvecAttributInvalide`

- **Intention du test :** Exécuter et valider la logique de remplacement des caractères invalides cachés à l'intérieur
  des valeurs d'attributs lors de l'ouverture d'un élément (`startElement`).

- **Motivation des données :** L'injection de la chaîne `"valeur\u000Binvalide"` dans un objet `AttributesImpl` force le
  déclenchement de la condition interne `if (isInvalid(atts.getValue(i)))` qui était jusque-là marquée en
  `NO_COVERAGE`. Un deuxième attribut valide (`"valeurValide"`) est ajouté dans le même élément pour vérifier que seuls
  les attributs invalides sont modifiés.

- **Explication de l'oracle :** Un espion capture les attributs filtrés avant leur envoi au parent. L'oracle utilise
  `assertEquals("valeur\ufffdinvalide", ...)` pour garantir de manière absolue que le caractère invalide a été remplacé
  par le caractère de substitution Unicode `\ufffd`.

**Mutants tués (9) :** tous les mutants `NO_COVERAGE` de la branche de filtrage des attributs, plus deux
mutants de `isInvalid(String)` qui ne peuvent être détectés qu'avec une valeur invalide :

- ligne 115 (`isInvalid(String)`) : *negated conditional* (`SURVIVED`)
- ligne 118 (`isInvalid(String)`) : *replaced boolean return with false*
- lignes 173, 175 (`startElement`) : *changed conditional boundary*
- lignes 173, 175, 175 (`startElement`) : *negated conditional* (deux mutants sur la ligne 175)
- ligne 178 : *removed call to `SafeContentHandler::filter`*
- ligne 181 : *removed call to `AttributesImpl::addAttribute`*

### `testCaracteresDeControleIsInvalid`

- **Intention du test :** Vérifier la première plage de `isInvalid(int)` (lignes 140 et 141) : les caractères de
  contrôle sous `0x20` sont invalides, sauf la tabulation, le saut de ligne et le retour chariot.

- **Motivation des données :** `0x00` et `0x1F` sont le premier et le dernier caractère de contrôle invalide. `0x09`,
  `0x0A` et `0x0D` sont les trois exceptions explicitement codées à la ligne 141. `0x20` (espace) est le premier
  caractère après la frontière `ch < 0x20`.

- **Explication de l'oracle :** Selon la spécification XML 1.0, les seuls caractères valides en dessous de `0x20` sont
  `0x09`,
  `0x0A` et `0x0D`. L'oracle utilise `assertTrue` pour `0x00` et `0x1F`, et `assertFalse` pour
  `0x09`, `0x0A`, `0x0D` et `0x20`, en passant par la même sous-classe `TestableSafeContentHandler`.

- **Mutants tués (2) :**
    - ligne 140 : *changed conditional boundary* (`ch < 0x20` → `ch <= 0x20`, l'espace deviendrait invalide)
    - ligne 141 : *replaced boolean return with true* (la tabulation, le saut de ligne et le retour chariot
      deviendraient invalides)

Au total, les quatre tests manuels tuent les 23 mutants restants, ce qui donne un score de 55/55.

## 3.4. Score de mutation final et conclusion

Avec l'intégration et le raffinement de nos tests manuels, notre dernière exécution Pitest a donné les résultats
suivants :

- **Couverture de lignes :** 100 % (60/60)
- **Score de mutation :** 100 % (55 mutants tués sur 55)
- **Force des tests (Test strength) :** 100 %
- **Mutants sans couverture :** 0

**Bilan :**

La combinaison des tests de l'IA et de nos tests manuels a permis de faire passer la couverture de lignes à 100 % et de
tuer la totalité des 55 mutants générés. Le score de mutation a fait un bond exceptionnel, passant d'un score initial de
40 % (avec `SafeContentHandlerTest`) à un score parfait de 100 %.
