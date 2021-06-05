# ATOCS

ATOCS is a framework that automatically analyses the code of applications and determines the most appropriate encryption schemes for the underlying database system.

## Running ATOCS
To run the ATOCS, you are required to provide two configuration files:
* Application configuration (app_config.yml)
* Cipher configuration (cipher_config.yml)

There are templates for these files on the project resources.

Maven compile and run:
```shell
mvn compile exec:java -Dexec.args="app_config.yml cipher_config.yml"
```

## Plugins
The database systems supported by ATOCS depend on the plugins implemented. Each database system requires a plugin.

ATOCS ships with two plugins. One for HBase and another for SafeNoSQL.

A plugin is responsible for receiving a database operation and determining the required properties of each operand to be preserved, so that the operation is feasible. Example: if a certain operation verifies if column A is equal to column B, then both column A and B require an equility property. 

The plugins must extend the *DatabasePlugin* abstract class, which has the necessary methods to connect them to the other ATOCS components.

They receive a database operation to analyse through the *analyseDbInteraction* method. The *Code Analyser* class offers some public static methods to assit the plugin analysis. For example, they can be used to obtain concrete values for the operation arguments received.
Aside from this, the plugins are responsible for sending all the requirements inferred from the operations analysed to the *Configurator* singleton (which can be accessed by calling `Configurator.getInstance()`).

The *Code Analyser* helper methods are document in the respective class.
 
# Authors
David Ferreira
João Paulo
Miguel Matos
