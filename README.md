
# NoAnalyt
This is an automated analytic solution.
By automated, it means it finds all the functions in the Kotlin code, then log them if they get called.
Without any annotation, or any help!
It uses a Kotlin Compiler Plugin to insert calling its logger.
It probably needs a proper backend implementation to receive the logs and analyze them,
but that needs to be implemented yet.
Try building and running tests by running `./gradlew clean test`,
or check the CI build to verify them.
