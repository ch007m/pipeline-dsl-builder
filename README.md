## Pipeline DSL & builder POC

### How to use it

The application can be packaged using this command:

```shell script
./mvnw package
```
and launched:
```bash
java -jar target/quarkus-app/quarkus-run.jar
__  ____  __  _____   ___  __ ____  ______ 
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
2024-07-09 13:10:54,678 WARN  [io.qua.config] (main) Unrecognized configuration key "quarkus.application.main-class" was provided; it will be ignored; verify that the dependency extension for this configuration is set or that you did not make a typo
2024-07-09 13:10:54,968 INFO  [io.quarkus] (main) builder 1.0-SNAPSHOT on JVM (powered by Quarkus 3.12.1) started in 0.535s. Listening on: http://0.0.0.0:8080
2024-07-09 13:10:54,969 INFO  [io.quarkus] (main) Profile prod activated. 
2024-07-09 13:10:54,969 INFO  [io.quarkus] (main) Installed features: [cdi, picocli, rest, smallrye-context-propagation, vertx]
Missing required options: '--configuration=<configuration>', '--output=<output>'
Usage: myapp [-hV] -c=<configuration> -o=<output>
Quarkus CLI example with Picocli
  -c, --configuration=<configuration>
                          The configuration file
  -h, --help              Show this help message and exit.
  -o, --output=<output>   The output file
  -V, --version           Print version information and exit.
```  

If there is a configuration file `conf.yaml` created at the root of this project and that you want to generate the pipelines yaml files under `out/flows`, then execute this command:
```bash
java -jar target/quarkus-app/quarkus-run.jar -c conf.yaml -o out/flows
```
Next, check the pipeline(s) generated under `./out/flows`

