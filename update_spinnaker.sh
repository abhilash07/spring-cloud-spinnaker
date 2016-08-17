#!/usr/bin/env bash


# Build a module
update() {

	echo "+++ Updating $1..."
	pushd $1
	git pull $2 $3
	popd

}

update clouddriver origin cfsummit
update deck origin master
update echo origin master
update front50 origin master
update gate origin master
update igor origin master
update orca origin master


