# Major Makefile variables.
# - ARCHIVE is the name of the zip archive created by the archive target for
#   source code submission and distribution.
# - JAVAFILES is all of the Java files in the project, including test cases and
#   build tools.

ifeq ($(OS),Windows_NT)
	# assume windows
	PATH_SEPARATOR = ;
else
	# assume Linux
	PATH_SEPARATOR = :
endif

ARCHIVE = project4.zip
JAVAFILES = */*.java */*/*.java
LIBPATH = "$(PWD)/commons-codec-1.11.jar$(PATH_SEPARATOR)$(PWD)/gson-2.8.6.jar"

# Compile all Java files.
.PHONY : all-classes
all-classes :
	javac -Xlint -cp $(LIBPATH) $(JAVAFILES)

# Run checkpoint speed test only.
.PHONY : speed
speed : all-classes
	java -cp .$(PATH_SEPARATOR)$(LIBPATH) test.ConformanceTests speed

# Run checkpoint conformance tests.
.PHONY : checkpoint
checkpoint : all-classes
	java -cp .$(PATH_SEPARATOR)$(LIBPATH) test.ConformanceTests checkpoint

# Run final conformance tests.
.PHONY : test
test : all-classes
	java -cp .$(PATH_SEPARATOR)$(LIBPATH) test.ConformanceTests

# Delete all intermediate and final output and leave only the source.
.PHONY : clean
clean :
	rm -rf $(JAVAFILES:.java=.class) $(ARCHIVE) $(JARFILE)

# Create a source code archive.
.PHONY : archive
archive : clean
	zip -9r $(ARCHIVE) *
