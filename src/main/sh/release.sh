#!/bin/sh

read -s -p "Secret PGP Passphrase: " PASSPHRASE
mvn release:clean
mvn release:prepare
mvn release:perform -Darguments=-Dgpg.passphrase=PASSPHRASE
