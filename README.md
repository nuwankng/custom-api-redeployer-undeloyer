# API Redeployment Client

## Overview
The API Redeployment Client is a Java-based project designed to facilitate the redeployment of APIs in `APIM-4.2.0`. The project includes a set of utilities and a main service class to handle configurations and HTTP requests necessary for the redeployment process.

## Requirements
- Java 11 or higher
- Maven
- Consumer key/secret pair from a tenant specific Oauth Application created for API management.

## Setup and Installation

1. **Clone the repository:**
    ```bash
    git clone https://github.com/yourusername/api-redeployment-client.git
    cd api-redeployment-client
    ```

2. **Build the project using Maven:**
    ```bash
    mvn clean install
    ```
3. **Configure the application:**
    - Ensure the `config.properties`, `tenants.json`, and `logback.xml` files are place along with the Jar file (Reference files can be found in the `src/main/resources` directory). You can place them in a new directory as shown below,
    ```bash
    ├── api-redeployer-client-1.0-jar-with-dependencies.jar
    ├── config.properties
    ├── logback.xml
    └── tenants.json
    └── revisions.json
    
    ```

## Usage

1. **Run the Deployment Service:**
    ```bash
    java -jar api-redeployer-client-1.0-jar-with-dependencies.jar
    ```

2. **Configuration:**
    - The `config.properties` file should include necessary configuration details like API endpoints, credentials, etc.
    - The `tenants.json` file should include information about the tenants to be redeployed.
    - The `revisions.json` file should include information about the revisions to be undeployed.

3. **Logging:**
    - A `logs` directory will be created in the project's root directory.
    - The log file, as specified in the `logback.xml` configuration file, will be created in the `logs` directory.
    - You can monitor the log file for detailed information about the redeployment process and any errors that occur.

## `tenants.json` File Schema
The `tenants.json` file should contain the following schema structure:
```json
{
  "wso2.com": {
    "consumerKey": "64aYA2jy7VhNuZeCu0INJq87kCUa",
    "consumerSecret": "ObJZtL6f3G4kmM3GywEQdc8Vcm8a"
  },
  "abc.com": {
    "consumerKey": "cAlWduHc0qUhDSNdJsh3dBPAfhIa",
    "consumerSecret": "EdBtCj62JHalxd9id_02xavqroAa"
  },
  "sample.com": {
    "consumerKey": "A_9K4FM4XWlxfF9v_2VsnAQy9Wka",
    "consumerSecret": "M5jwZ4zT7HlI7bJGVP6bs6r9Mc8a"
  },
  ...
}
```

## `revisions.json` File Schema
The `revisions.json` file should contain the following schema structure:
```json
[
{"name": "External", "displayOnDevportal": false},
{"name": "Internal", "displayOnDevportal": false}
]
```

## Project Files
  - `DeploymentService.java`: The main service class responsible for initiating the redeployment process.
  - utilities/`ReadConfigFile.java`: Utility class for reading configuration files.
  - utilities/`HttpClientManager.java`: Utility class for managing HTTP clients.
  - utilities/`RestRequests.java`: Utility class for making REST requests.
  - `logback.xml`: Configuration file for logging.
  - `tenants.json`: JSON file containing tenant information.
  - `revisions.json`: JSON file containing to be undeployed revision information.
  - `config.properties`: Properties file for application configuration.
