#!/bin/bash
# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0
#
# This script packages your project into an open-source solution distributable
# that can be published to sites like GitHub.
#
# Important notes and prereq's:
#   1. This script should be run from the repo's /deployment folder.
#
# This script will perform the following tasks:
#   1. Remove any old dist files from previous runs.
#   2. Package the GitHub contribution and pull request templates (typically
#      found in the /.github folder).
#   3. Package the /source folder along with the necessary root-level
#      open-source artifacts (i.e. CHANGELOG, etc.).
#   4. Remove any unecessary artifacts from the /open-source folder (i.e.
#      node_modules, package-lock.json, etc.).
#   5. Zip up the /open-source folder and create the distributable.
#   6. Remove any temporary files used for staging.
#
# Parameters:
#  - solution-name: name of the solution for consistency

# Check to see if the required parameters have been provided:
if [ -z "$1" ]; then
    echo "Please provide the trademark approved solution name for the open source package."
    echo "For example: ./build-open-source-dist.sh trademarked-solution-name"
    exit 1
fi

# Get reference for all important folders
source_template_dir="$PWD"
dist_dir="$source_template_dir/open-source"
source_dir="$source_template_dir/../"
github_dir="$source_template_dir/../.github"

echo "------------------------------------------------------------------------------"
echo "[Init] ensure open-source folder exists"
echo "------------------------------------------------------------------------------"
rm -rf $dist_dir
mkdir -p $dist_dir

echo "------------------------------------------------------------------------------"
echo "[Packing] all source files"
echo "------------------------------------------------------------------------------"
rsync -av \
  --exclude='../build' \
  --exclude='../.DS_Store' \
  --exclude='../.git' \
  --exclude='../.idea' \
  --exclude='../.gradle' \
  --exclude='../buildspec.yml' \
  --exclude='../clickstream/build' \
  --exclude='../integrationtest/.idea' \
   --exclude='../deployment/open-source' \
  $source_dir $dist_dir

echo "------------------------------------------------------------------------------"
echo "[Packing] Create GitHub (open-source) zip file"
echo "------------------------------------------------------------------------------"

# Create the zip file
echo "cd $dist_dir"
cd $dist_dir
echo "zip -q -r9 ../$1.zip ."
zip -q -r9 ../$1.zip .

# Cleanup any temporary/unnecessary files
echo "Clean up open-source folder"
echo "rm -rf open-source/"
rm -rf * .*

# Place final zip file in $dist_dir
echo "mv ../$1.zip ."
mv ../$1.zip .

echo "Completed building $1.zip dist"
