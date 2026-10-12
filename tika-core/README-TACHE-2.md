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
