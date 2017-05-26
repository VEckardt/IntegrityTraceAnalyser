# IntegrityTraceAnalyser
Analyse the traces between your documents 

## Purpose
The TraceAnalyser can be started on one document of your choice and it analyses which traces you have defined from this document  to other documents. 
It will check and list upstream and downstrem traces.
Furthermore, it will check and count any suspect relationships. 

![TraceAnalyser](doc/TraceAnalyser.PNG)

## Use Cases
- Trace status review in general
- To validate the Data Migration when moving documents from one server to another with traces 

## Install
- Put the "dist/IntegrityTraceAnalyser.jar" directly into your IntegrityClient folder
- Add a custom menu entry with 
```
name: Trace Analyser
program:  ../jre/bin/javaw.exe
parameter: -jar ../IntegrityTraceAnalyser.jar
```
- Copy also the files "dits/lib/IntegrityAPI.jar" and "dist/lib/jfxmessagebox-1.1.0.jar" into your IntegrityClient/lib folder

## How to test
- open any document or just stay on one in the query result
- click Custom > Trace Analyser
- Then review the outcome

##  Development environment
- PTC Integrity LM 10.9 (also 11.0 should be fine)
- Netbeans 7.4 (or 8)
- Java 1.7 (or 1.8)

## Known Limitations
- Will not work with Versioned Documents
- Is quite slow when large documents are involved (up to 1 minute to start up with ~ 5000 requirements)
- The colors are internally defined, not configurable
