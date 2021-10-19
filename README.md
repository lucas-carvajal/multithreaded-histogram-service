## Multithreaded Histogram Service

### What is it and what does it do?
Upon running the main function, you are presented with a GUI to create a Histogram for a folder and all its files using a variety of different multithreaded approaches.  
It creates the histogram by reading through all files in the folder and its subfolders, creating a histogram for all files and then aggregating them.  
When executing one of the options, updates on which files are processed and eventually the final histogram get printed to the console.

### How to use it?
1. Make sure you use Java 11
2. Run the main function in "/Assignment4/src/main/java/de/uniba/wiai/dsg/pks/assignment/Main.java"

### What did I learn?
* Threads, Shared Memory and Locking
* Semaphor, Consumer-Producer Paradigm and Fork/Join Framework
* Java Stream API
* Message Passing with TCP-Sockets
* Actor Model

### Disclaimers
This was a project I did for my Parallel Programming class.  
The work was done in a group of three and the GUI to execute the different multithreaded histogram services was already given by our instructors.
