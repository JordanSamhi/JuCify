# JuCify

Unifying Android code for enhanced static analysis.

:warning: Please checkout to the following commit to use JuCify: 6897c58350773987da0a2c52a99fd4ebf1edc39e

## Getting started

### Downloading the tool

<pre>
git clone --recursive https://github.com/JordanSamhi/JuCify.git
</pre>

### Installing the tool

<pre>
cd JuCify
mvn clean install
</pre>

### Issues

If you stumble upon a stack overflow error while building JuCify, increase memory available with this command:

<pre>
export MAVEN_OPTS=-Xss32m
</pre>

Then, try to rebuild.

### Using the tool

<pre>
java -jar JuCify/target/JuCify-0.1-jar-with-dependencies.jar <i>options</i>
</pre>

Options:

* ```-a``` : The path to the APK to process.
* ```-p``` : The path to Android platofrms folder.
* ```-f``` :  Provide paths to necessary files for native reconstruciton.
* ```-r``` : Print raw results.
* ```-ta``` : Perform taint analysis.
* ```-c``` : Export call-graph to text file.
* ```-e``` : Export call-graph to dot format.

### Scripts

Better use the following script to use JuCify as described in the paper.

<pre>
./main.sh -f PATH_TO_APK -p PATH_TO_PLATFORMS
</pre>

## Built With

* [Maven](https://maven.apache.org/) - Dependency Management

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details

## Contact

For any question regarding this study, please contact us at:
[Jordan Samhi](mailto:jordan.samhi@uni.lu)
