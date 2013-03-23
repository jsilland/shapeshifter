#!/bin/sh

read -s -p "Secret PGP Passphrase: " PASSPHRASE
read -s -p "Again: " PASSPHRASE_2
if [ $PASSPHRASE != $PASSPHRASE_2 ]
then
  echo "Passphrases don't match, exiting" >&2
  exit 1
fi

echo "Cleaning up before release"
mvn release:clean
if [ $? -ne 0 ] ; then
  echo "mvn release:clean failed" >&2
  exit 1
fi

echo "Preparing for release"
mvn release:prepare
if [ $? -ne 0 ] ; then
  echo "mvn release:prepare failed" >&2
  exit 1
fi

echo "Performing release"
mvn release:perform -Darguments=-Dgpg.passphrase=$PASSPHRASE
if [ $? -ne 0 ] ; then
  echo "mvn release:perform failed" >&2
  exit 1
fi

echo "Updating local master branch"
git fetch
git rebase origin/master
if [ $? -ne 0 ] ; then
  echo "Failed to update local master branch" >&2
  exit 1
fi

echo "Checking out release tag"
git checkout `git tag | tail -n 1`
if [ $? -ne 0 ] ; then
  echo "Failed to check out latest tag" >&2
  exit 1
fi

echo "Generating Javadoc"
mvn clean javadoc:javadoc
if [ $? -ne 0 ] ; then
  echo "Failed to generate Javadoc" >&2
  exit 1
fi

echo "Updating Javadoc"
git checkout gh-pages
git rm -r apidocs
mv target/site/apidocs apidocs
git add apidocs
git commit -m "Updates Javadoc"
git push origin HEAD:gh-pages
if [ $? -ne 0 ] ; then
  echo "Failed to update Javadoc" >&2
  exit 1
fi

echo "Checking out master branch"
git checkout master

echo "All done."
