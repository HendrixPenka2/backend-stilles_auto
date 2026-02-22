# Instructions pour générer catalog-service

## 📋 Étape 1 : Aller sur Spring Initializr

Visite : https://start.spring.io

## ⚙️ Étape 2 : Configuration du projet

Remplis exactement comme suit :

### Project Metadata
- **Project** : Maven
- **Language** : Java
- **Spring Boot** : 3.5.10
- **Group** : `com.Team_Pk`
- **Artifact** : `catalog-service`
- **Name** : `CatalogService`
- **Description** : Microservice de gestion du catalogue pour Stilles Auto
- **Package name** : `com.Team_Pk.car_rental.catalog`
- **Packaging** : Jar
- **Java** : 21

### Dependencies à ajouter

Clique sur "ADD DEPENDENCIES" et ajoute :

1. **Spring Reactive Web** (WebFlux)
2. **Spring Data R2DBC**
3. **PostgreSQL Driver**
4. **Flyway Migration**
5. **Lombok**
6. **Spring Security**
7. **OAuth2 Resource Server**
8. **Validation**

## 📥 Étape 3 : Génération et installation

1. Clique sur **GENERATE** (bouton en bas)
2. Un fichier `catalog-service.zip` sera téléchargé
3. Dézippe le fichier
4. Copie **tout le contenu** du dossier dézippé dans :
   ```
   /home/donpk/Pk_project's/stilles_auto-Backend/catalog-service/
   ```

## ✅ Étape 4 : Vérification

Une fois copié, ta structure devrait être :
```
catalog-service/
├── src/
├── pom.xml
├── mvnw
├── mvnw.cmd
└── ...
```

**Ensuite, reviens me dire "C'est fait" et je configurerai le reste !**
