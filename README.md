# Réseau de neurones — Examen Design Pattern

Projet Maven contenant le modèle du corrigé en Java et ses deux aspects AspectJ.

## Exécuter dans IntelliJ IDEA

1. Décompresser l’archive. Dans IntelliJ IDEA, choisir **File → Open** et ouvrir le dossier `Reseau_Neurones_IntelliJ` qui contient `pom.xml`. Accepter l’import Maven si IntelliJ le propose.
2. Dans **File → Project Structure → Project → SDK**, sélectionner un **JDK 21**. IntelliJ permet aussi de le télécharger depuis ce menu.
3. Ouvrir **View → Tool Windows → Maven**, puis dans **Réseau de neurones – Examen Design Pattern → Lifecycle**, double-cliquer sur **test**. La configuration **Exécuter l’examen** lance la même commande depuis le menu **Run**.
4. Vérifier le message `BUILD SUCCESS` en fin d’exécution. Maven télécharge les dépendances lors du premier lancement.

La commande équivalente dans un terminal disposant de Maven 3 est `mvn clean test`. La phase `test` compile les sources avec AspectJ puis lance les 16 vérifications du modèle et du contrôle d’accès. La console affiche les résultats et le journal détaillé est écrit dans `target/aspectj.log`.

## Fichiers

| Fichier | Rôle |
| --- | --- |
| `src/main/java/examen/Exam.java` | Réseau de neurones, patrons de conception et vérifications |
| `src/main/aspect/examen/Logging.aj` | Journalisation des appels, réussites et échecs |
| `src/main/aspect/examen/Security.aj` | Vérification des droits de prédiction et d’entraînement |
| `src/main/java/lancement/TestsExamen.java` | Lanceur Maven et écriture du journal AspectJ |

Le modèle de démonstration utilise une architecture 3–5–2. Après l’entraînement, les deux prédictions attendues sont approximativement `[0.1703, 0.1707]` et `[0.8264, 0.8269]`.
