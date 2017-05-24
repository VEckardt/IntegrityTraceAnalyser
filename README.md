# IntegrityTraceAnalyser
Analyse the traces between your documents 

## Purpose
The TraceAnalyser can be started on one document of your choice and it analyses whcih traces you have out of this document defined.
It will check upstream and downstrem traces.
Furthermore, it will check and count any suspect relationships. 

![TraceAnalyser](doc/TraceAnalyser.PNG)

## Install
- put the IntegrityTraceAnalyser.jar into your IntegrityClient folder
- add a custom menu with the name: Trace Analyser
-   the program is program:  ../jre/bin/javaw.exe
-   add the parameters as follows:   
-   -jre ../IntegrityTraceAnalyser.jar

## How to test
- open any document or just stay on one in the query result
- click Custom > Trace Analyser
- Then review the outcome

## Known Limitations
- Will not work with Versioned Documents
- Is quite slow when large documents are involved (up to 1 minute to start up with ~ 5000 requirements)
