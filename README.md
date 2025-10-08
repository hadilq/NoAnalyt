
# NoAnalyt
This is an automated analytic solution.
By automated, it means it finds all the functions in the Kotlin code, then log them if they get called.
Without any annotation, or any help!
It uses a Kotlin Compiler Plugin to insert calling its logger.
It's far from finish, but the core components are working.
Try building and running tests by running `./gradlew clean test`,
or check the CI build to verify them.
