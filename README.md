#  Assignment

This was once implemented as a small assignment for a job offer.

I implemented the Liquid Democracy.

Beside it was an assignment, I heavily used Java 8+ features, to come back from Kotlin.

### Properties of the Liquid Democracy.
 - There are n voters and m alternatives
 - Voters can delegate their choices to other voters
 - Voters delegating themselves (directly or indirectly) become invalid
 - Result: a list of alternatives with at least one vote
 - Also: Invalid voters should be counted and reported

### The input

The input will be read from `stdin` and should have the following format:
 - `"{Voter's name} pick {alternative}"` (direct pick)
 - or `"{Voter's Name} delegate {Second Voter's Name}"` (delegation)

Where names and alternatives are alphanumeric strings without any space.

##' The output
The output will be printed to `stdout` as "{summed votes} {alternative or invalid count}"
The choices will be in descending order, the count of the invalids always comes last.

There might be *warnings* in the `stderr` stream, hinting that the input was wrong.

<br>

If an additional flag `--open` is set, it will display each voter's alternative
(from direct picks or following delegations) as if it was an open democracy.


#### How I Tested - The Explanation

I mainly tried to cover small unit tests because they make it easy to test
various corner cases for any function.

Additionally I added a simple integration test to test if the overall setup works like intended (end-to-end).

I reached around 90% to 96% of test coverage.
The missing parts are such methods as `toString()` or `equals()` which were implemented (for debugging) but not called.
Or missed IO exception catch branches.

## Run the code

The code uses Gradle (v. 7.0.2) on Linux, the build setup uses Kotlin instead of Groovy.

To build the project, run

``` bash
./gradlew build
```

This will also create a JAR file, locatable in `./app/build/libs/app.jar`.

The could will run with `./gradlew run`, but will probably not allow to insert lines on `System.in` in this wrapped up setup.

So it will be recommended to run the code with the following command:

``` bash
java -jar ./app/build/libs/app.jar de.nox.liquiddemocracy.Main

# text can be piped in

cat example.txt | java -jar ./app/build/libs/app.jar de.nox.liquiddemocracy.Main

# an optional argument can be passed in --open, this will show, who chose what (indirectly)
cat example.txt | java -jar ./app/build/libs/app.jar de.nox.liquiddemocracy.Main --open
```

<br>

To run the tests, run
``` bash
./gradlew test
./gradlew test jacocoTestReport # to show the test coverage with JaCoCo
```

The reports of the test cases and test case coverage can be found in `./app/build/reports/` as:
``` bash
./app/build/reports/jacoco/test/html/de.nox.liquiddemocracy/index.html
./app/build/reports/tests/test/index.html
```


To clean the build directory, run
``` bash
./gradlew clean
```
