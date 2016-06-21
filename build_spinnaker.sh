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

	push $1

	echo "+++ Building $1..."
	pushd /tmp/$1
	./gradlew -DspringBoot.repackage=true clean build
	popd

	pop $1

}

# Build deck
buildDeck() {

	/bin/mv $1/settings.js $1/settings.js.orig
	/bin/cp settings.js $1/settings.js

	push $1

	pushd /tmp/$1

	echo "+++ Backup up $1's .git/config"
	cp .git/config .git/config.backup

	echo "+++ Filtering out $1's worktree"
	grep -v worktree .git/config > .git/config.new
	mv .git/config.new .git/config

	echo "+++ Building $1..."
	./gradlew -DspringBoot.repackage=true clean build -x test
	/bin/rm -f build/libs/deck-ui-*.jar
	jar cvf build/libs/deck-ui-repackaged.jar -C build/webpack/ .

	echo "+++ Restoring $1's .git/config"
	mv .git/config.backup .git/config

	popd

	pop $1

	/bin/rm -f $1/settings.js
	/bin/mv $1/settings.js.orig $1/settings.js

}

modules="clouddriver echo front50 gate igor orca deck"

if [ "$1" = "" ]; then
	echo
	echo "Usage: ./build_spinnaker.sh [all|clouddriver|echo|front50|gate|igor|orca|deck]"
	echo
	exit
elif [ "$1" = "all" ]; then
	echo
	echo "Building all modules for Spinnaker..."
	echo
	for module in $modules
	do
		echo "Building $module..."
		if [ "$module" = "deck" ]; then
			buildDeck $module
		else
			build $module
		fi
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
