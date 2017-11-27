# Trump Tweet Prediction
Project submission by Michael Janschek

Contents:
- Project description
- Requirements
- Getting started

## Project description
The project descrition is available as notebook at target/tweetAnalysis.ipynb.
This notebook is also available compiled as html at target/tweetAnalysis.html.

## Requirements
Following programs and libraries should be installed and functioning for running this project and its specified application part.

### Stream Application Eclipse Project
* JDK 8
* Eclipse with Gradle Plugin
** Personally, I used "Buildship Gradle Integration" which can be installed via the Eclipse Marketplace
* Spark 2.2.0

### Stream Application only (JAR file is provided in projectfoler/target)
* JDK 8
* Spark 2.2.0

### Batch Analysis Python Script
* Python 3.5
** python libraries:
** pandas
** numpy
** datetime

### Batch Analysis IPython Notebook
Requirements as python script plus:
** matplotlib (for plots)
* Jupyter

## Getting started

### Quickstart
- (optional) Create a new directory, this is your working directory %WORKING_DIR% now
  * Copy all files in %PROJECT_DIR%/target into this folder
- Execute %WORKING_DIR%/startTweetStream.sh

### Building the project
- Open Eclipse
- Import this project as gradle project
- Use gradle plugin to build this project
- all built files should be in %PROJECT_DIR%/target now

### Running the stream project
- Edit %PROJECT_DIR%/src/main/resources/application.properties
  * Set WORKINGDIR to your desired working directory
  * Set boolean options as desired
  * You CAN set change hashtags and filters, BUT this is not recommended due to use of constant hashtags in the code
- You can run this project inside eclipse as Java Application.
- You also can build this project and submit the file to Spark via the script startTweetStream.sh.

### Running the batch analysis
- Edit %PROJECT_DIR%/target/tweetAnalysis.py
  * set workDir to your desired working directory, make sure the csv files from %PROJECT_DIR%/data are there
- run the python script in this directory
  * python3 tweetAnalysis.py

### Running the batch analysis notebook
- Open the terminal and navigate to %PROJECT_DIR%/target
- Run command
  * jupyter notebook
- By default, your webbrowser will open the jupyter GUI
- Open tweetAnalysis.ipynb
- Run the notebook step-by-step