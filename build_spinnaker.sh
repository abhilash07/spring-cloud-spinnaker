#!/usr/bin/env bash

# push a module over to /tmp
push() {

	echo "+++ Moving $1 to /tmp..."
	mv $1 /tmp

	echo "+++ Setting aside $1's .git folder..."
	mv /tmp/$1/.git /tmp/$1/backup.git

	echo "+++ Moving over $1's actual .git folder..."
	mv .git/modules/$1 /tmp/$1/.git
}

# pop a module back from /tmp
pop() {

	echo "+++ Moving $1's actual .git folder back..."
	mv /tmp/$1/.git .git/modules/$1

	echo "+++ Restoring $1's submodule .git folder..."
	mv /tmp/$1/backup.git /tmp/$1/.git

	echo "+++ Moving $1 back from /tmp..."
	mv /tmp/$1 .

}

# Build a module
build() {

	BUILD_FAILED="yes"

	push $1

	echo "+++ Building $1..."
	pushd /tmp/$1
	./gradlew -DspringBoot.repackage=true clean build && BUILD_FAILED="no"
	popd

	pop $1

    if [[ "${BUILD_FAILED}" == "yes" ]] ; then
    	echo "--- FAILED to build $1"
    	exit 1
    fi

}

modules="front50 gate igor orca"

if [ "$1" = "" ]; then
	echo
	echo "Usage: ./build_spinnaker.sh [all|front50|gate|igor|orca]"
	echo
	exit
elif [ "$1" = "all" ]; then
	echo
	echo "Building all modules for Spinnaker..."
	echo
	for module in $modules
	do
		echo "Building $module..."
		build $module
	done
	exit
elif [[ $modules =~ $1 ]]; then
	echo
	echo "$1 is a valid member of [$modules]. Building..."
	echo
	if [ "$1" = "deck" ]; then
		buildDeck $1
	else
		build $1
	fi
else
	echo
	echo "'$1' is not a recognized option. Aborting."
	echo
    exit 1
fi;
